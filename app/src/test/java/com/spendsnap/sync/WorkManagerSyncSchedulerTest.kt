package com.spendsnap.sync

import android.content.Context
import android.util.Log
import androidx.test.core.app.ApplicationProvider
import androidx.work.Configuration
import androidx.work.WorkManager
import androidx.work.testing.SynchronousExecutor
import androidx.work.testing.WorkManagerTestInitHelper
import com.google.common.truth.Truth.assertThat
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class WorkManagerSyncSchedulerTest {
    private lateinit var context: Context
    private lateinit var scheduler: WorkManagerSyncScheduler

    @Before
    fun setUp() {
        context = ApplicationProvider.getApplicationContext()
        WorkManagerTestInitHelper.initializeTestWorkManager(
            context,
            Configuration.Builder()
                .setExecutor(SynchronousExecutor())
                .setMinimumLoggingLevel(Log.DEBUG)
                .build(),
        )
        scheduler = WorkManagerSyncScheduler(context)
    }

    @Test
    fun repeatedImmediateSync_keepsOneUniqueWorkRequest() {
        scheduler.requestImmediateSync()
        val firstId = WorkManager.getInstance(context)
            .getWorkInfosForUniqueWork(SyncWorkNames.ONE_SHOT)
            .get()
            .single()
            .id

        scheduler.requestImmediateSync()
        val work = WorkManager.getInstance(context)
            .getWorkInfosForUniqueWork(SyncWorkNames.ONE_SHOT)
            .get()

        assertThat(work).hasSize(1)
        assertThat(work.single().id).isEqualTo(firstId)
    }

    @Test
    fun periodicSync_isAlsoUnique() {
        scheduler.ensurePeriodicSync()
        scheduler.ensurePeriodicSync()

        val work = WorkManager.getInstance(context)
            .getWorkInfosForUniqueWork(SyncWorkNames.PERIODIC)
            .get()

        assertThat(work).hasSize(1)
    }
}
