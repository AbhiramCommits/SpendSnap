package com.spendsnap.feature.expenses

import com.spendsnap.data.local.SyncState
import java.time.LocalDate
import java.time.YearMonth

data class ExpenseListItemUi(
    val id: String,
    val merchant: String,
    val amountCents: Long,
    val currency: String,
    val categoryId: String,
    val categoryName: String,
    val categoryColorHex: String,
    val occurredAt: Long,
    val syncState: SyncState,
)

data class ExpenseDayGroup(
    val date: LocalDate,
    val totalCents: Long,
    val items: List<ExpenseListItemUi>,
)

data class ExpenseListUiState(
    val isLoading: Boolean = true,
    val selectedMonth: YearMonth = YearMonth.now(),
    val groups: List<ExpenseDayGroup> = emptyList(),
    val monthTotalCents: Long = 0,
    val pendingCount: Int = 0,
    val error: String? = null,
)

sealed interface ExpenseListEvent {
    data class MonthChanged(val month: YearMonth) : ExpenseListEvent

    data object PreviousMonth : ExpenseListEvent

    data object NextMonth : ExpenseListEvent

    data object DismissError : ExpenseListEvent
}
