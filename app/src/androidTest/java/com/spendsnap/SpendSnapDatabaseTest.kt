package com.spendsnap

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.google.common.truth.Truth.assertThat
import com.spendsnap.data.local.DatabaseSeeder
import com.spendsnap.data.local.SpendSnapDatabase
import com.spendsnap.data.local.SyncState
import com.spendsnap.data.local.entity.ExpenseEntity
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import java.time.YearMonth
import java.time.ZoneId

@RunWith(AndroidJUnit4::class)
class SpendSnapDatabaseTest {
    private lateinit var database: SpendSnapDatabase

    @Before
    fun setUp() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        database =
            Room.inMemoryDatabaseBuilder(context, SpendSnapDatabase::class.java)
                .allowMainThreadQueries()
                .addCallback(DatabaseSeeder())
                .build()
    }

    @After
    fun tearDown() {
        database.close()
    }

    @Test
    fun crudAndMonthlyQuery_workTogether() =
        runBlocking {
            val categoryCount = database.categoryDao().count()
            assertThat(categoryCount).isEqualTo(8)

            val month = YearMonth.of(2026, 10)
            val date = month.atDay(12).atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli()
            val expense =
                ExpenseEntity(
                    id = "android-test-expense",
                    merchant = "Instrumented Cafe",
                    amountCents = 1_275,
                    categoryId = "category-meals",
                    occurredAt = date,
                    syncState = SyncState.PENDING,
                )

            database.expenseDao().upsert(expense)
            assertThat(database.expenseDao().getById(expense.id)).isEqualTo(expense)
            assertThat(database.expenseDao().observePending().first()).hasSize(1)

            database.expenseDao().upsertAll(
                listOf(
                    expense.copy(id = "second", amountCents = 2_000, merchant = "Second Cafe"),
                    expense.copy(
                        id = "old",
                        occurredAt =
                            month.minusMonths(1).atDay(1)
                                .atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli(),
                    ),
                ),
            )

            val start = month.atDay(1).atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli()
            val end =
                month.plusMonths(1).atDay(1)
                    .atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli()
            val breakdown = database.expenseDao().observeMonthlyBreakdown(start, end).first()
            assertThat(breakdown.single().categoryId).isEqualTo("category-meals")
            assertThat(breakdown.single().totalCents).isEqualTo(3_275)
            assertThat(database.expenseDao().observeMonthlyTotal(start, end).first()).isEqualTo(3_275)

            database.expenseDao().markSynced(expense.id, "remote-123", System.currentTimeMillis())
            val synced = database.expenseDao().getById(expense.id)
            assertThat(synced?.syncState).isEqualTo(SyncState.SYNCED)
            assertThat(synced?.remoteId).isEqualTo("remote-123")

            database.expenseDao().deleteById("second")
            assertThat(database.expenseDao().count()).isEqualTo(2)
            Unit
        }

    @Test
    fun seededCategories_areStableAndUnique() =
        runBlocking {
            val categories = database.categoryDao().getAll()
            assertThat(categories).hasSize(8)
            assertThat(categories.map { it.name }).containsExactly(
                "Meals",
                "Travel",
                "Software",
                "Office",
                "Marketing",
                "Hardware",
                "Fees",
                "Other",
            )
            Unit
        }
}
