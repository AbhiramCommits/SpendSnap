package com.spendsnap.sync

import com.google.common.truth.Truth.assertThat
import com.spendsnap.data.repository.SyncResult
import com.spendsnap.telemetry.TelemetryEvents
import com.spendsnap.testutil.FakeExpenseRepository
import com.spendsnap.testutil.FakeTelemetry
import java.io.IOException
import kotlinx.coroutines.test.runTest
import org.junit.Test

class ExpenseSyncWorkerTest {
    @Test
    fun success_returnsCountsWithoutRetry() = runTest {
        val repository = FakeExpenseRepository()
        repository.queuedSyncResults += SyncResult(
            syncedCount = 3,
            failedCount = 0,
            remainingCount = 0,
        )
        val telemetry = FakeTelemetry()

        val execution = ExpenseSyncWorker.executeSync(repository, telemetry)

        assertThat(execution.shouldRetry).isFalse()
        assertThat(execution.result.syncedCount).isEqualTo(3)
        assertThat(execution.result.failedCount).isEqualTo(0)
        assertThat(execution.summary).contains("Synced 3")
        assertThat(telemetry.events.map { it.name })
            .containsExactly(TelemetryEvents.SYNC_COMPLETED)
        assertThat(telemetry.nonFatals).isEmpty()
    }

    @Test
    fun allFailures_requestRetry() = runTest {
        val repository = FakeExpenseRepository()
        repository.queuedSyncResults += SyncResult(
            syncedCount = 0,
            failedCount = 2,
            remainingCount = 2,
        )
        val telemetry = FakeTelemetry()

        val execution = ExpenseSyncWorker.executeSync(repository, telemetry)

        assertThat(execution.shouldRetry).isTrue()
        assertThat(execution.result.failedCount).isEqualTo(2)
        assertThat(execution.result.remainingCount).isEqualTo(2)
        assertThat(telemetry.nonFatals).hasSize(1)
        val event = telemetry.events.single()
        assertThat(event.params["failure_count"]).isEqualTo(2)
        assertThat(event.params["success_count"]).isEqualTo(0)
    }

    @Test
    fun partialFailure_retriesWhileReportingSuccessfulRecords() = runTest {
        val repository = FakeExpenseRepository()
        repository.queuedSyncResults += SyncResult(
            syncedCount = 2,
            failedCount = 1,
            remainingCount = 1,
        )
        val telemetry = FakeTelemetry()

        val execution = ExpenseSyncWorker.executeSync(repository, telemetry)

        assertThat(execution.shouldRetry).isTrue()
        assertThat(execution.result.syncedCount).isEqualTo(2)
        assertThat(execution.result.failedCount).isEqualTo(1)
        assertThat(execution.result.attemptedCount).isEqualTo(3)
        val event = telemetry.events.single()
        assertThat(event.params["success_count"]).isEqualTo(2)
        assertThat(event.params["failure_count"]).isEqualTo(1)
    }

    @Test
    fun unexpectedTransportFailure_requestsRetryAndRecordsNonFatal() = runTest {
        val repository = FakeExpenseRepository()
        repository.syncError = IOException("offline")
        val telemetry = FakeTelemetry()

        val execution = ExpenseSyncWorker.executeSync(repository, telemetry)

        assertThat(execution.shouldRetry).isTrue()
        assertThat(execution.result.syncedCount).isEqualTo(0)
        assertThat(execution.result.failedCount).isEqualTo(1)
        assertThat(telemetry.nonFatals.single()).isInstanceOf(IOException::class.java)
    }
}
