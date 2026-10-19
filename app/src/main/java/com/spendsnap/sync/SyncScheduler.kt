package com.spendsnap.sync

interface SyncScheduler {
    fun requestImmediateSync()

    fun ensurePeriodicSync()
}
