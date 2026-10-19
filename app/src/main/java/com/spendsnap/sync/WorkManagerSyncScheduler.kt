package com.spendsnap.sync

import android.content.Context
import androidx.work.BackoffPolicy
import androidx.work.Constraints
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.ExistingWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import dagger.hilt.android.qualifiers.ApplicationContext
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Singleton

object SyncWorkNames {
    const val ONE_SHOT = "spendsnap-sync-one-shot"
    const val PERIODIC = "spendsnap-sync-periodic"
}

@Singleton
class WorkManagerSyncScheduler
    @Inject
    constructor(
        @ApplicationContext private val context: Context,
    ) : SyncScheduler {
        override fun requestImmediateSync() {
            val request =
                OneTimeWorkRequestBuilder<ExpenseSyncWorker>()
                    .setConstraints(connectedConstraint())
                    .setBackoffCriteria(BackoffPolicy.EXPONENTIAL, 30, TimeUnit.SECONDS)
                    .build()
            // KEEP is the deduplication guard: a write during an in-flight sync
            // cannot start a second worker with the same queue snapshot.
            WorkManager.getInstance(context).enqueueUniqueWork(
                SyncWorkNames.ONE_SHOT,
                ExistingWorkPolicy.KEEP,
                request,
            )
        }

        override fun ensurePeriodicSync() {
            val request =
                PeriodicWorkRequestBuilder<ExpenseSyncWorker>(6, TimeUnit.HOURS)
                    .setConstraints(connectedConstraint())
                    .setBackoffCriteria(BackoffPolicy.EXPONENTIAL, 30, TimeUnit.SECONDS)
                    .build()
            WorkManager.getInstance(context).enqueueUniquePeriodicWork(
                SyncWorkNames.PERIODIC,
                ExistingPeriodicWorkPolicy.KEEP,
                request,
            )
        }

        private fun connectedConstraint() =
            Constraints.Builder()
                .setRequiredNetworkType(NetworkType.CONNECTED)
                .build()
    }
