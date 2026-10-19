package com.spendsnap.data.local.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Query
import androidx.room.Upsert
import com.spendsnap.data.local.SyncState
import com.spendsnap.data.local.entity.ExpenseEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface ExpenseDao {
    @Query("SELECT * FROM expenses ORDER BY occurredAt DESC, updatedAt DESC")
    fun observeAll(): Flow<List<ExpenseEntity>>

    @Query("SELECT * FROM expenses WHERE id = :id")
    fun observeById(id: String): Flow<ExpenseEntity?>

    @Query("SELECT * FROM expenses WHERE syncState = 'PENDING' ORDER BY updatedAt ASC")
    fun observePending(): Flow<List<ExpenseEntity>>

    @Query("SELECT COUNT(*) FROM expenses WHERE syncState != 'SYNCED'")
    fun observeUnsyncedCount(): Flow<Int>

    @Query(
        """
        SELECT categoryId,
               SUM(amountCents) AS totalCents,
               COUNT(*) AS expenseCount
        FROM expenses
        WHERE occurredAt >= :startInclusive AND occurredAt < :endExclusive
        GROUP BY categoryId
        ORDER BY totalCents DESC
        """,
    )
    fun observeMonthlyBreakdown(
        startInclusive: Long,
        endExclusive: Long,
    ): Flow<List<MonthlyCategoryTotal>>

    @Query(
        """
        SELECT COALESCE(SUM(amountCents), 0)
        FROM expenses
        WHERE occurredAt >= :startInclusive AND occurredAt < :endExclusive
        """,
    )
    fun observeMonthlyTotal(
        startInclusive: Long,
        endExclusive: Long,
    ): Flow<Long>

    @Query(
        """
        SELECT * FROM expenses
        WHERE syncState IN ('PENDING', 'FAILED')
        ORDER BY updatedAt ASC
        """,
    )
    suspend fun getSyncable(): List<ExpenseEntity>

    @Query("SELECT * FROM expenses WHERE id = :id")
    suspend fun getById(id: String): ExpenseEntity?

    @Query("SELECT COUNT(*) FROM expenses")
    suspend fun count(): Int

    @Query("SELECT COUNT(*) FROM expenses WHERE syncState != 'SYNCED'")
    suspend fun countUnsynced(): Int

    @Upsert
    suspend fun upsert(expense: ExpenseEntity)

    @Upsert
    suspend fun upsertAll(expenses: List<ExpenseEntity>)

    @Delete
    suspend fun delete(expense: ExpenseEntity)

    @Query("DELETE FROM expenses WHERE id = :id")
    suspend fun deleteById(id: String)

    @Query(
        """
        UPDATE expenses
        SET syncState = 'SYNCED', remoteId = :remoteId, updatedAt = :updatedAt
        WHERE id = :id AND syncState != 'SYNCED'
        """,
    )
    suspend fun markSynced(
        id: String,
        remoteId: String,
        updatedAt: Long,
    )

    @Query(
        """
        UPDATE expenses
        SET syncState = :state, updatedAt = :updatedAt
        WHERE id = :id AND syncState != 'SYNCED'
        """,
    )
    suspend fun markSyncState(
        id: String,
        state: SyncState,
        updatedAt: Long,
    )
}
