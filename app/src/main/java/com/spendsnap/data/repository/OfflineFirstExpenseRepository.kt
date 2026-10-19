package com.spendsnap.data.repository

import com.spendsnap.data.local.SyncState
import com.spendsnap.data.local.dao.ExpenseDao
import com.spendsnap.data.local.dao.MonthlyCategoryTotal
import com.spendsnap.data.local.entity.CategoryEntity
import com.spendsnap.data.local.entity.ExpenseEntity
import com.spendsnap.data.remote.ExpenseDto
import com.spendsnap.data.remote.SpendSnapApi
import com.spendsnap.sync.SyncScheduler
import java.time.YearMonth
import java.time.ZoneId
import java.util.concurrent.CancellationException
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.Flow

@Singleton
class OfflineFirstExpenseRepository @Inject constructor(
    private val expenseDao: ExpenseDao,
    private val categoryDao: com.spendsnap.data.local.dao.CategoryDao,
    private val api: SpendSnapApi,
    private val syncScheduler: SyncScheduler,
) : ExpenseRepository {

    override fun observeExpenses(): Flow<List<ExpenseEntity>> = expenseDao.observeAll()

    override fun observeExpense(id: String): Flow<ExpenseEntity?> = expenseDao.observeById(id)

    override fun observeCategories(): Flow<List<CategoryEntity>> = categoryDao.observeAll()

    override fun observeUnsyncedCount(): Flow<Int> = expenseDao.observeUnsyncedCount()

    override fun observeMonthlyBreakdown(yearMonth: YearMonth): Flow<List<MonthlyCategoryTotal>> {
        val (start, end) = monthBounds(yearMonth)
        return expenseDao.observeMonthlyBreakdown(start, end)
    }

    override fun observeMonthlyTotal(yearMonth: YearMonth): Flow<Long> {
        val (start, end) = monthBounds(yearMonth)
        return expenseDao.observeMonthlyTotal(start, end)
    }

    override suspend fun upsert(expense: ExpenseEntity) {
        val now = System.currentTimeMillis()
        expenseDao.upsert(
            expense.copy(
                syncState = SyncState.PENDING,
                remoteId = null,
                updatedAt = now,
            ),
        )
        syncScheduler.requestImmediateSync()
    }

    override suspend fun upsertAll(expenses: List<ExpenseEntity>) {
        if (expenses.isEmpty()) return
        val now = System.currentTimeMillis()
        expenseDao.upsertAll(
            expenses.map {
                it.copy(
                    syncState = SyncState.PENDING,
                    remoteId = null,
                    updatedAt = now,
                )
            },
        )
        syncScheduler.requestImmediateSync()
    }

    override suspend fun delete(id: String) {
        expenseDao.deleteById(id)
    }

    override suspend fun syncPending(): SyncResult {
        val queue = expenseDao.getSyncable()
        var synced = 0
        var failed = 0

        queue.forEach { expense ->
            try {
                val remote = api.createExpense(
                    clientUuid = expense.id,
                    expense = expense.toDto(),
                )
                val remoteId = requireNotNull(remote.remoteId) {
                    "Remote accepted expense without an id"
                }
                expenseDao.markSynced(
                    id = expense.id,
                    remoteId = remoteId,
                    updatedAt = System.currentTimeMillis(),
                )
                synced += 1
            } catch (cancellation: CancellationException) {
                throw cancellation
            } catch (error: Exception) {
                expenseDao.markSyncState(
                    id = expense.id,
                    state = SyncState.FAILED,
                    updatedAt = System.currentTimeMillis(),
                )
                failed += 1
            }
        }

        return SyncResult(
            syncedCount = synced,
            failedCount = failed,
            remainingCount = expenseDao.countUnsynced(),
        )
    }

    private fun ExpenseEntity.toDto() = ExpenseDto(
        clientUuid = id,
        merchant = merchant,
        amountCents = amountCents,
        currency = currency,
        categoryId = categoryId,
        occurredAt = occurredAt,
        note = note,
        receiptUri = receiptUri,
        updatedAt = updatedAt,
        remoteId = remoteId,
    )

    private fun monthBounds(yearMonth: YearMonth): Pair<Long, Long> {
        val zone = ZoneId.systemDefault()
        val start = yearMonth.atDay(1).atStartOfDay(zone).toInstant().toEpochMilli()
        val end = yearMonth.plusMonths(1).atDay(1).atStartOfDay(zone).toInstant().toEpochMilli()
        return start to end
    }
}
