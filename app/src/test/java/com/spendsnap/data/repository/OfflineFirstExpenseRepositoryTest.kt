package com.spendsnap.data.repository

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.google.common.truth.Truth.assertThat
import com.spendsnap.data.local.DatabaseSeeder
import com.spendsnap.data.local.SpendSnapDatabase
import com.spendsnap.data.local.SyncState
import com.spendsnap.data.local.entity.ExpenseEntity
import com.spendsnap.data.remote.FakeSpendSnapApi
import com.spendsnap.sync.SyncScheduler
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.time.YearMonth
import java.time.ZoneId

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class OfflineFirstExpenseRepositoryTest {
    private lateinit var database: SpendSnapDatabase
    private lateinit var repository: OfflineFirstExpenseRepository
    private lateinit var api: FakeSpendSnapApi
    private lateinit var scheduler: RecordingSyncScheduler

    @Before
    fun setUp() =
        runBlocking {
            val context = ApplicationProvider.getApplicationContext<Context>()
            database =
                Room.inMemoryDatabaseBuilder(context, SpendSnapDatabase::class.java)
                    .allowMainThreadQueries()
                    .addCallback(DatabaseSeeder())
                    .build()
            database.categoryDao().upsertAll(DatabaseSeeder.DEFAULT_CATEGORIES)
            api = FakeSpendSnapApi(delayMillis = 0)
            scheduler = RecordingSyncScheduler()
            repository =
                OfflineFirstExpenseRepository(
                    expenseDao = database.expenseDao(),
                    categoryDao = database.categoryDao(),
                    api = api,
                    syncScheduler = scheduler,
                )
        }

    @After
    fun tearDown() {
        database.close()
    }

    @Test
    fun offlineWrite_staysPending_andEnqueuesSync() =
        runBlocking {
            val expense = expense(merchant = "Corner Cafe", amountCents = 1299)

            repository.upsert(expense)

            val stored = database.expenseDao().getById(expense.id)
            assertThat(stored).isNotNull()
            assertThat(stored?.syncState).isEqualTo(SyncState.PENDING)
            assertThat(stored?.remoteId).isNull()
            assertThat(api.allExpenses()).isEmpty()
            assertThat(scheduler.immediateRequests).isEqualTo(1)
        }

    @Test
    fun successfulSync_marksSynced_andStoresRemoteId() =
        runBlocking {
            val expense = expense(merchant = "Northwind Books", amountCents = 2400)
            repository.upsert(expense)

            val result = repository.syncPending()

            val stored = database.expenseDao().getById(expense.id)
            assertThat(result.syncedCount).isEqualTo(1)
            assertThat(result.failedCount).isEqualTo(0)
            assertThat(result.remainingCount).isEqualTo(0)
            assertThat(stored?.syncState).isEqualTo(SyncState.SYNCED)
            assertThat(stored?.remoteId).startsWith("remote-")
            assertThat(api.allExpenses()).hasSize(1)
        }

    @Test
    fun failedSync_marksFailed_thenRetrySucceeds() =
        runBlocking {
            api.failNextRequests(1)
            val expense = expense(merchant = "Metro Card", amountCents = 5500)
            repository.upsert(expense)

            val first = repository.syncPending()
            val afterFailure = database.expenseDao().getById(expense.id)
            assertThat(first.syncedCount).isEqualTo(0)
            assertThat(first.failedCount).isEqualTo(1)
            assertThat(first.remainingCount).isEqualTo(1)
            assertThat(afterFailure?.syncState).isEqualTo(SyncState.FAILED)

            val second = repository.syncPending()
            val afterRetry = database.expenseDao().getById(expense.id)
            assertThat(second.syncedCount).isEqualTo(1)
            assertThat(second.failedCount).isEqualTo(0)
            assertThat(afterRetry?.syncState).isEqualTo(SyncState.SYNCED)
            assertThat(api.allExpenses()).hasSize(1)
        }

    @Test
    fun monthlyBreakdown_aggregatesCentsWithoutFloatingPointRounding() =
        runBlocking {
            val month = YearMonth.of(2026, 10)
            val day = month.atDay(10).atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli()

            repository.upsertAll(
                listOf(
                    expense("Meals", 1_001, day),
                    expense("Meals", 2_002, day + 1),
                    expense("Travel", 1_234, day + 2, categoryId = "category-travel"),
                    expense("Meals", 9_999, day - 40L * 24 * 60 * 60 * 1_000),
                ),
            )

            val breakdown = repository.observeMonthlyBreakdown(month).first()
            val meals = breakdown.single { it.categoryId == "category-meals" }
            val travel = breakdown.single { it.categoryId == "category-travel" }

            assertThat(meals.totalCents).isEqualTo(3_003)
            assertThat(meals.expenseCount).isEqualTo(2)
            assertThat(travel.totalCents).isEqualTo(1_234)
            assertThat(repository.observeMonthlyTotal(month).first()).isEqualTo(4_237)
        }

    @Test
    fun concurrentSync_uploadsExactlyOnce_andLeavesOneKeyedRow() =
        runBlocking {
            val expense = expense(merchant = "Concurrent Cafe", amountCents = 1_500)
            repository.upsert(expense)

            val results =
                coroutineScope {
                    listOf(
                        async { repository.syncPending() },
                        async { repository.syncPending() },
                    ).awaitAll()
                }

            assertThat(results.sumOf { it.syncedCount }).isAtLeast(1)
            assertThat(api.allExpenses()).hasSize(1)
            assertThat(database.expenseDao().count()).isEqualTo(1)
            val stored = database.expenseDao().getById(expense.id)
            assertThat(stored?.syncState).isEqualTo(SyncState.SYNCED)
            assertThat(stored?.remoteId).isEqualTo(api.allExpenses().single().remoteId)

            val thirdRun = repository.syncPending()
            assertThat(thirdRun.syncedCount).isEqualTo(0)
            assertThat(api.allExpenses()).hasSize(1)
        }

    @Test
    fun repeatedUpsertForKeyedUuid_updatesInsteadOfDuplicating() =
        runBlocking {
            val expense = expense(merchant = "Draft Name", amountCents = 1_000)
            repository.upsert(expense)
            repository.upsert(expense.copy(merchant = "Final Name", amountCents = 2_000))

            assertThat(database.expenseDao().count()).isEqualTo(1)
            val stored = database.expenseDao().getById(expense.id)
            assertThat(stored?.merchant).isEqualTo("Final Name")
            assertThat(stored?.amountCents).isEqualTo(2_000)
        }

    private fun expense(
        merchant: String,
        amountCents: Long,
        occurredAt: Long = System.currentTimeMillis(),
        categoryId: String = "category-meals",
    ) = ExpenseEntity(
        merchant = merchant,
        amountCents = amountCents,
        categoryId = categoryId,
        occurredAt = occurredAt,
    )

    private class RecordingSyncScheduler : SyncScheduler {
        var immediateRequests = 0

        override fun requestImmediateSync() {
            immediateRequests += 1
        }

        override fun ensurePeriodicSync() = Unit
    }
}
