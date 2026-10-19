package com.spendsnap.data.repository

data class SyncResult(
    val syncedCount: Int,
    val failedCount: Int,
    val remainingCount: Int,
) {
    val isComplete: Boolean get() = failedCount == 0
    val attemptedCount: Int get() = syncedCount + failedCount
}
