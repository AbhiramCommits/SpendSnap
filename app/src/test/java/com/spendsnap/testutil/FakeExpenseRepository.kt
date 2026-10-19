package com.spendsnap.testutil

import com.spendsnap.data.local.SyncState
import com.spendsnap.data.local.dao.MonthlyCategoryTotal
import com.spendsnap.data.local.entity.CategoryEntity
import com.spendsnap.data.local.entity.ExpenseEntity
import com.spendsnap.data.repository.ExpenseRepository
import com.spendsnap.data.repository.SyncResult
import java.time.Instant
import java.time.YearMonth
import java.time.ZoneId
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update

class FakeExpenseRepository(
    initialExpenses: List<ExpenseEntity> = emptyList(),
    initialCategories: List<CategoryEntity> = DEFAULT_CATEGORIES,
) : ExpenseRepository {
    val expenses = MutableStateFlow(initialExpenses)
    val categories = MutableStateFlow(initialCategories)
    val upserted = mutableListOf<ExpenseEntity>()
    val deletedIds = mutableListOf<String>()
    val queuedSyncResults = ArrayDeque<SyncResult>()
    var syncError: Exception? = null
    var syncCalls: Int = 0

    override fun observeExpenses(): Flow<List<ExpenseEntity>> = expenses

    override fun observeExpense(id: String): Flow<ExpenseEntity?> =
        expenses.map { rows -> rows.firstOrNull { it.id == id } }

    override fun observeCategories(): Flow<List<CategoryEntity>> = categories

    override fun observeUnsyncedCount(): Flow<Int> = expenses.map { rows ->
        rows.count { it.syncState != SyncState.SYNCED }
    }

    override fun observeMonthlyBreakdown(
        yearMonth: YearMonth,
    ): Flow<List<MonthlyCategoryTotal>> = expenses.map { rows ->
        rows.filter { it.inMonth(yearMonth) }
            .groupBy { it.categoryId }
            .map { (categoryId, monthRows) ->
                MonthlyCategoryTotal(
                    categoryId = categoryId,
                    totalCents = monthRows.sumOf { it.amountCents },
                    expenseCount = monthRows.size,
                )
            }
    }

    override fun observeMonthlyTotal(yearMonth: YearMonth): Flow<Long> = expenses.map { rows ->
        rows.filter { it.inMonth(yearMonth) }.sumOf { it.amountCents }
    }

    override suspend fun upsert(expense: ExpenseEntity) {
        upserted += expense
        expenses.update { rows ->
            rows.filterNot { it.id == expense.id } + expense
        }
    }

    override suspend fun upsertAll(expensesToInsert: List<ExpenseEntity>) {
        upserted += expensesToInsert
        expenses.update { rows ->
            rows.filterNot { existing -> expensesToInsert.any { it.id == existing.id } } +
                expensesToInsert
        }
    }

    override suspend fun delete(id: String) {
        deletedIds += id
        expenses.update { rows -> rows.filterNot { it.id == id } }
    }

    override suspend fun syncPending(): SyncResult {
        syncCalls += 1
        syncError?.let { throw it }
        return queuedSyncResults.removeFirstOrNull()
            ?: SyncResult(syncedCount = 0, failedCount = 0, remainingCount = 0)
    }

    private fun ExpenseEntity.inMonth(yearMonth: YearMonth): Boolean = YearMonth.from(
        Instant.ofEpochMilli(occurredAt).atZone(ZoneId.systemDefault()).toLocalDate(),
    ) == yearMonth

    companion object {
        val DEFAULT_CATEGORIES = com.spendsnap.data.local.DatabaseSeeder.DEFAULT_CATEGORIES
    }
}
