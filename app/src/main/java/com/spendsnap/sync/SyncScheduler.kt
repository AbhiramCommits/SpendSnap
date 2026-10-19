package com.spendsnap.sync

interface SyncScheduler {
    fun requestImmediateSync()
    fun ensurePeriodicSync()
}

/**
 * Temporary binding while the worker is wired in the background-sync milestone.
 * It is replaced by [WorkManagerSyncScheduler] once WorkManager is configured.
 */
class NoOpSyncScheduler @javax.inject.Inject constructor() : SyncScheduler {
    override fun requestImmediateSync() = Unit
    override fun ensurePeriodicSync() = Unit
}
