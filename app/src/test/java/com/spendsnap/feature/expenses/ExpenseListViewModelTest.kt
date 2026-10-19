package com.spendsnap.feature.expenses

import app.cash.turbine.ReceiveTurbine
import app.cash.turbine.test
import com.google.common.truth.Truth.assertThat
import com.spendsnap.data.local.SyncState
import com.spendsnap.data.local.entity.ExpenseEntity
import com.spendsnap.testutil.FakeExpenseRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Before
import org.junit.Test
import java.time.LocalDate
import java.time.YearMonth
import java.time.ZoneId

@OptIn(ExperimentalCoroutinesApi::class)
class ExpenseListViewModelTest {
    private val dispatcher = StandardTestDispatcher()

    @Before
    fun setUp() {
        Dispatchers.setMain(dispatcher)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun state_groupsByDay_andReportsMonthlyTotalAndPendingCount() =
        runTest(dispatcher) {
            val repository = FakeExpenseRepository(initialExpenses = samples())
            val viewModel = ExpenseListViewModel(repository)

            viewModel.uiState.test {
                val loaded = awaitMatching { !it.isLoading }

                assertThat(loaded.groups).hasSize(1)
                assertThat(loaded.groups.single().items).hasSize(2)
                assertThat(loaded.monthTotalCents).isEqualTo(2_200)
                assertThat(loaded.pendingCount).isEqualTo(1)
                assertThat(loaded.error).isNull()
                assertThat(loaded.groups.single().items.first().categoryName).isEqualTo("MEALS")
                cancelAndIgnoreRemainingEvents()
            }
        }

    @Test
    fun previousMonthEvent_loadsOlderExpenses() =
        runTest(dispatcher) {
            val repository = FakeExpenseRepository(initialExpenses = samples())
            val viewModel = ExpenseListViewModel(repository)
            val previous = YearMonth.now().minusMonths(1)

            viewModel.uiState.test {
                awaitMatching { !it.isLoading }
                viewModel.onEvent(ExpenseListEvent.PreviousMonth)

                val earlier = awaitMatching { it.selectedMonth == previous }
                assertThat(earlier.monthTotalCents).isEqualTo(500)
                assertThat(earlier.groups.single().items.single().merchant).isEqualTo("Old Train")
                cancelAndIgnoreRemainingEvents()
            }
        }

    private fun samples(): List<ExpenseEntity> {
        val zone = ZoneId.systemDefault()
        val current = LocalDate.now().atTime(12, 0).atZone(zone).toInstant().toEpochMilli()
        val old =
            LocalDate.now().minusMonths(1).atTime(12, 0)
                .atZone(zone).toInstant().toEpochMilli()
        return listOf(
            ExpenseEntity(
                id = "one",
                merchant = "Corner Cafe",
                amountCents = 1_200,
                categoryId = "category-meals",
                occurredAt = current,
                syncState = SyncState.PENDING,
            ),
            ExpenseEntity(
                id = "two",
                merchant = "Bus Pass",
                amountCents = 1_000,
                categoryId = "category-travel",
                occurredAt = current + 1,
                syncState = SyncState.SYNCED,
            ),
            ExpenseEntity(
                id = "old",
                merchant = "Old Train",
                amountCents = 500,
                categoryId = "category-travel",
                occurredAt = old,
                syncState = SyncState.SYNCED,
            ),
        )
    }
}

internal suspend fun <T> ReceiveTurbine<T>.awaitMatching(predicate: (T) -> Boolean): T {
    while (true) {
        val item = awaitItem()
        if (predicate(item)) return item
    }
}
