package com.spendsnap.data.repository

import com.spendsnap.data.local.dao.MonthlyCategoryTotal
import com.spendsnap.data.local.entity.CategoryEntity
import com.spendsnap.data.local.entity.ExpenseEntity
import java.time.YearMonth
import kotlinx.coroutines.flow.Flow

interface ExpenseRepository {
    fun observeExpenses(): Flow<List<ExpenseEntity>>
    fun observeExpense(id: String): Flow<ExpenseEntity?>
    fun observeCategories(): Flow<List<CategoryEntity>>
    fun observeUnsyncedCount(): Flow<Int>
    fun observeMonthlyBreakdown(yearMonth: YearMonth): Flow<List<MonthlyCategoryTotal>>
    fun observeMonthlyTotal(yearMonth: YearMonth): Flow<Long>

    suspend fun upsert(expense: ExpenseEntity)
    suspend fun upsertAll(expenses: List<ExpenseEntity>)
    suspend fun delete(id: String)
    suspend fun syncPending(): SyncResult
}
