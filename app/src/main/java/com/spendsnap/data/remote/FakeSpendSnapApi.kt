package com.spendsnap.data.remote

import java.io.IOException
import java.util.UUID
import kotlinx.coroutines.delay
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

/**
 * In-process stand-in for the receipt service. It is idempotent on client UUID,
 * which lets the retry tests prove that a repeated upload cannot duplicate a row.
 */
class FakeSpendSnapApi(
    @Volatile var delayMillis: Long = 20,
) : SpendSnapApi {
    private val mutex = Mutex()
    private val remoteExpenses = linkedMapOf<String, ExpenseDto>()
    private var failuresRemaining = 0

    fun failNextRequests(count: Int = 1) {
        synchronized(this) {
            failuresRemaining = count
        }
    }

    fun clearFailures() {
        synchronized(this) {
            failuresRemaining = 0
        }
    }

    suspend fun allExpenses(): List<ExpenseDto> = mutex.withLock {
        remoteExpenses.values.toList()
    }

    override suspend fun createExpense(
        clientUuid: String,
        expense: ExpenseDto,
    ): ExpenseDto {
        if (delayMillis > 0) delay(delayMillis)
        val shouldFail = synchronized(this) {
            if (failuresRemaining > 0) {
                failuresRemaining -= 1
                true
            } else {
                false
            }
        }
        if (shouldFail) throw IOException("Simulated network failure")

        return mutex.withLock {
            remoteExpenses[clientUuid] ?: expense.copy(
                remoteId = "remote-${UUID.randomUUID()}",
            ).also { remoteExpenses[clientUuid] = it }
        }
    }

    override suspend fun getExpenses(): ExpenseListResponse {
        if (delayMillis > 0) delay(delayMillis)
        return mutex.withLock { ExpenseListResponse(remoteExpenses.values.toList()) }
    }
}
