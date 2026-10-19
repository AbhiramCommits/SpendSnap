package com.spendsnap.feature.expenses

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.spendsnap.data.local.entity.CategoryEntity
import com.spendsnap.data.local.entity.ExpenseEntity
import com.spendsnap.data.repository.ExpenseRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import java.time.Instant
import java.time.LocalDate
import java.time.YearMonth
import java.time.ZoneId
import java.util.Locale
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update

@HiltViewModel
class ExpenseListViewModel @Inject constructor(
    private val repository: ExpenseRepository,
) : ViewModel() {
    private val selectedMonth = MutableStateFlow(YearMonth.now())
    private val error = MutableStateFlow<String?>(null)

    val uiState = selectedMonth
        .flatMapLatest { month ->
            combine(
                repository.observeExpenses(),
                repository.observeCategories(),
                repository.observeUnsyncedCount(),
                error,
            ) { expenses, categories, pendingCount, currentError ->
                val categoriesById = categories.associateBy { it.id }
                val zone = ZoneId.systemDefault()
                val monthlyExpenses = expenses.filter {
                    YearMonth.from(
                        Instant.ofEpochMilli(it.occurredAt).atZone(zone).toLocalDate(),
                    ) == month
                }
                val groups = monthlyExpenses
                    .groupBy {
                        Instant.ofEpochMilli(it.occurredAt).atZone(zone).toLocalDate()
                    }
                    .toSortedMap(compareByDescending<LocalDate> { it })
                    .map { (date, items) ->
                        ExpenseDayGroup(
                            date = date,
                            totalCents = items.sumOf { it.amountCents },
                            items = items.map { expense ->
                                expense.toUi(categoriesById[expense.categoryId])
                            },
                        )
                    }

                ExpenseListUiState(
                    isLoading = false,
                    selectedMonth = month,
                    groups = groups,
                    monthTotalCents = monthlyExpenses.sumOf { it.amountCents },
                    pendingCount = pendingCount,
                    error = currentError,
                )
            }
        }
        .catch { throwable ->
            error.value = throwable.message ?: "Could not load expenses"
            emit(
                ExpenseListUiState(
                    isLoading = false,
                    selectedMonth = selectedMonth.value,
                    error = error.value,
                ),
            )
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = ExpenseListUiState(selectedMonth = selectedMonth.value),
        )

    fun onEvent(event: ExpenseListEvent) {
        when (event) {
            is ExpenseListEvent.MonthChanged -> selectedMonth.value = event.month
            ExpenseListEvent.PreviousMonth -> selectedMonth.update { it.minusMonths(1) }
            ExpenseListEvent.NextMonth -> selectedMonth.update { it.plusMonths(1) }
            ExpenseListEvent.DismissError -> error.value = null
        }
    }

    private fun ExpenseEntity.toUi(category: CategoryEntity?) = ExpenseListItemUi(
        id = id,
        merchant = merchant,
        amountCents = amountCents,
        currency = currency,
        categoryId = categoryId,
        categoryName = category?.name?.uppercase(Locale.getDefault()) ?: "OTHER",
        categoryColorHex = category?.colorHex ?: "#6C757D",
        occurredAt = occurredAt,
        syncState = syncState,
    )
}
