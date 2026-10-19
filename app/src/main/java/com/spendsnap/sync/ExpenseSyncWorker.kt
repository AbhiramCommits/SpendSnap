package com.spendsnap.sync

import android.content.Context
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import androidx.work.workDataOf
import com.spendsnap.data.repository.ExpenseRepository
import com.spendsnap.data.repository.SyncResult
import com.spendsnap.telemetry.Telemetry
import com.spendsnap.telemetry.TelemetryEvents
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import java.util.concurrent.CancellationException

data class SyncExecution(
    val result: SyncResult,
    val shouldRetry: Boolean,
) {
    val summary: String
        get() = "Synced ${result.syncedCount}, failed ${result.failedCount}, " +
            "${result.remainingCount} remaining"
}

@HiltWorker
class ExpenseSyncWorker @AssistedInject constructor(
    @Assisted appContext: Context,
    @Assisted workerParams: WorkerParameters,
    private val repository: ExpenseRepository,
    private val telemetry: Telemetry,
) : CoroutineWorker(appContext, workerParams) {

    override suspend fun doWork(): Result {
        val execution = executeSync(repository, telemetry)
        return if (execution.shouldRetry) {
            Result.retry()
        } else {
            Result.success(
                workDataOf(
                    KEY_SYNCED to execution.result.syncedCount,
                    KEY_FAILED to execution.result.failedCount,
                    KEY_REMAINING to execution.result.remainingCount,
                ),
            )
        }
    }

    companion object {
        const val KEY_SYNCED = "synced"
        const val KEY_FAILED = "failed"
        const val KEY_REMAINING = "remaining"

        /**
         * Pure execution seam used by unit tests. WorkManager applies the
         * exponential retry policy configured on the request.
         */
        suspend fun executeSync(
            repository: ExpenseRepository,
            telemetry: Telemetry,
        ): SyncExecution {
            return try {
                val result = repository.syncPending()
                telemetry.logEvent(
                    TelemetryEvents.SYNC_COMPLETED,
                    mapOf(
                        "success_count" to result.syncedCount,
                        "failure_count" to result.failedCount,
                        "remaining_count" to result.remainingCount,
                    ),
                )
                if (result.failedCount > 0) {
                    telemetry.recordNonFatal(
                        IllegalStateException(
                            "Sync incomplete: ${result.failedCount} record(s) failed",
                        ),
                    )
                }
                SyncExecution(
                    result = result,
                    shouldRetry = result.failedCount > 0,
                )
            } catch (cancellation: CancellationException) {
                throw cancellation
            } catch (error: Exception) {
                telemetry.recordNonFatal(error)
                telemetry.logEvent(
                    TelemetryEvents.SYNC_COMPLETED,
                    mapOf(
                        "success_count" to 0,
                        "failure_count" to 1,
                        "remaining_count" to -1,
                    ),
                )
                SyncExecution(
                    result = SyncResult(syncedCount = 0, failedCount = 1, remainingCount = -1),
                    shouldRetry = true,
                )
            }
        }
    }
}
