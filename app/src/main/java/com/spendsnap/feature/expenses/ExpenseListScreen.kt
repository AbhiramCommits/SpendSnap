package com.spendsnap.feature.expenses

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ReceiptLong
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.outlined.ChevronLeft
import androidx.compose.material.icons.outlined.ChevronRight
import androidx.compose.material.icons.outlined.CloudDone
import androidx.compose.material.icons.outlined.CloudUpload
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.spendsnap.core.ui.CategoryDot
import com.spendsnap.core.ui.EmptyState
import com.spendsnap.core.ui.ErrorBanner
import com.spendsnap.core.ui.LoadingState
import com.spendsnap.core.ui.MoneyText
import com.spendsnap.core.ui.SyncBadge
import com.spendsnap.data.local.SyncState
import com.spendsnap.ui.theme.SpendSnapTheme
import java.time.LocalDate
import java.time.YearMonth
import java.time.format.DateTimeFormatter
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ExpenseListScreen(
    onAddExpense: () -> Unit,
    onExpenseClick: (String) -> Unit,
    viewModel: ExpenseListViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    ExpenseListContent(
        state = state,
        onAddExpense = onAddExpense,
        onExpenseClick = onExpenseClick,
        onPreviousMonth = { viewModel.onEvent(ExpenseListEvent.PreviousMonth) },
        onNextMonth = { viewModel.onEvent(ExpenseListEvent.NextMonth) },
        onDismissError = { viewModel.onEvent(ExpenseListEvent.DismissError) },
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ExpenseListContent(
    state: ExpenseListUiState,
    onAddExpense: () -> Unit,
    onExpenseClick: (String) -> Unit,
    onPreviousMonth: () -> Unit,
    onNextMonth: () -> Unit,
    onDismissError: () -> Unit,
) {
    Scaffold(
        modifier = Modifier.testTag("expense_list_screen"),
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text("SpendSnap", style = MaterialTheme.typography.titleLarge)
                        Text(
                            state.selectedMonth.formatMonth(),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                },
                actions = {
                    IconButton(onClick = onPreviousMonth) {
                        Icon(
                            Icons.Outlined.ChevronLeft,
                            contentDescription = "Previous month",
                        )
                    }
                    IconButton(onClick = onNextMonth) {
                        Icon(
                            Icons.Outlined.ChevronRight,
                            contentDescription = "Next month",
                        )
                    }
                },
            )
        },
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = onAddExpense,
                modifier = Modifier.testTag("add_expense_fab"),
                icon = { Icon(Icons.Filled.Add, contentDescription = "Add expense") },
                text = { Text("Add expense") },
            )
        },
    ) { padding ->
        when {
            state.isLoading ->
                LoadingState(
                    modifier =
                        Modifier
                            .fillMaxSize()
                            .padding(padding),
                    label = "Loading expenses",
                )

            else ->
                Column(
                    modifier =
                        Modifier
                            .fillMaxSize()
                            .padding(padding),
                ) {
                    if (state.error != null) {
                        ErrorBanner(
                            message = state.error,
                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                            onRetry = onDismissError,
                        )
                    }
                    MonthSummary(state)
                    if (state.groups.isEmpty()) {
                        EmptyState(
                            modifier =
                                Modifier
                                    .weight(1f)
                                    .testTag("expense_empty_state"),
                            title = "No expenses yet",
                            message = "Capture a receipt or import a CSV to see your monthly spend.",
                            actionLabel = "Add your first expense",
                            onAction = onAddExpense,
                        )
                    } else {
                        LazyColumn(
                            modifier =
                                Modifier
                                    .weight(1f)
                                    .fillMaxWidth(),
                            contentPadding =
                                androidx.compose.foundation.layout.PaddingValues(
                                    start = 16.dp,
                                    end = 16.dp,
                                    bottom = 100.dp,
                                ),
                            verticalArrangement = Arrangement.spacedBy(8.dp),
                        ) {
                            state.groups.forEach { group ->
                                item(key = "header-${group.date}") {
                                    DayHeader(group)
                                }
                                items(group.items, key = { it.id }) { item ->
                                    ExpenseRow(item = item, onClick = { onExpenseClick(item.id) })
                                }
                            }
                        }
                    }
                }
        }
    }
}

@Composable
private fun MonthSummary(state: ExpenseListUiState) {
    Card(
        modifier =
            Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp),
        colors =
            CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.primaryContainer,
            ),
        shape = RoundedCornerShape(20.dp),
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            Text(
                "MONTH TOTAL",
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onPrimaryContainer,
            )
            Spacer(Modifier.height(4.dp))
            MoneyText(
                amountCents = state.monthTotalCents,
                modifier = Modifier.testTag("monthly_total"),
                style = MaterialTheme.typography.displaySmall,
                color = MaterialTheme.colorScheme.onPrimaryContainer,
            )
            Spacer(Modifier.height(8.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                val allSynced = state.pendingCount == 0
                Icon(
                    imageVector = if (allSynced) Icons.Outlined.CloudDone else Icons.Outlined.CloudUpload,
                    contentDescription = if (allSynced) "Synced" else "Pending sync",
                    modifier = Modifier.size(18.dp),
                    tint = MaterialTheme.colorScheme.onPrimaryContainer,
                )
                Spacer(Modifier.width(6.dp))
                Text(
                    text =
                        if (allSynced) {
                            "All changes synced"
                        } else {
                            "${state.pendingCount} waiting to sync"
                        },
                    modifier = Modifier.testTag("pending_count"),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                )
            }
        }
    }
}

@Composable
private fun DayHeader(group: ExpenseDayGroup) {
    Row(
        modifier =
            Modifier
                .fillMaxWidth()
                .padding(top = 8.dp, bottom = 2.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = group.date.formatDayHeader(),
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        MoneyText(
            amountCents = group.totalCents,
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
private fun ExpenseRow(
    item: ExpenseListItemUi,
    onClick: () -> Unit,
) {
    Card(
        modifier =
            Modifier
                .fillMaxWidth()
                .clickable(onClick = onClick)
                .testTag("expense_row_${item.id}"),
        shape = RoundedCornerShape(14.dp),
    ) {
        Row(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Surface(
                color = MaterialTheme.colorScheme.surfaceVariant,
                shape = RoundedCornerShape(12.dp),
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Outlined.ReceiptLong,
                    contentDescription = "Receipt",
                    modifier =
                        Modifier
                            .padding(10.dp)
                            .size(20.dp),
                    tint = MaterialTheme.colorScheme.primary,
                )
            }
            Spacer(Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    item.merchant,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                )
                Row(verticalAlignment = Alignment.CenterVertically) {
                    CategoryDot(item.categoryColorHex)
                    Spacer(Modifier.width(6.dp))
                    Text(
                        item.categoryName,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
            Column(horizontalAlignment = Alignment.End) {
                MoneyText(item.amountCents, item.currency)
                Spacer(Modifier.height(4.dp))
                SyncBadge(
                    state = item.syncState,
                    modifier = Modifier.testTag("sync_badge_${item.id}"),
                )
            }
        }
    }
}

private fun YearMonth.formatMonth(): String = format(DateTimeFormatter.ofPattern("MMMM yyyy", Locale.getDefault()))

private fun LocalDate.formatDayHeader(): String {
    val today = LocalDate.now()
    return when (this) {
        today -> "Today"
        today.minusDays(1) -> "Yesterday"
        else -> format(DateTimeFormatter.ofPattern("EEE, MMM d", Locale.getDefault()))
    }
}

@Preview(name = "Expenses light", showBackground = true)
@Composable
fun ExpenseListLightPreview() {
    SpendSnapTheme(darkTheme = false) {
        ExpenseListContent(
            state = previewExpenseListState(),
            onAddExpense = {},
            onExpenseClick = {},
            onPreviousMonth = {},
            onNextMonth = {},
            onDismissError = {},
        )
    }
}

@Preview(name = "Expenses dark", showBackground = true)
@Composable
fun ExpenseListDarkPreview() {
    SpendSnapTheme(darkTheme = true) {
        ExpenseListContent(
            state = previewExpenseListState(),
            onAddExpense = {},
            onExpenseClick = {},
            onPreviousMonth = {},
            onNextMonth = {},
            onDismissError = {},
        )
    }
}

private fun previewExpenseListState(): ExpenseListUiState {
    val today = LocalDate.now()
    return ExpenseListUiState(
        isLoading = false,
        selectedMonth = YearMonth.now(),
        groups =
            listOf(
                ExpenseDayGroup(
                    date = today,
                    totalCents = 4_298,
                    items =
                        listOf(
                            ExpenseListItemUi(
                                id = "preview-1",
                                merchant = "Corner Cafe",
                                amountCents = 1_298,
                                currency = "USD",
                                categoryId = "category-meals",
                                categoryName = "MEALS",
                                categoryColorHex = "#C8553D",
                                occurredAt = System.currentTimeMillis(),
                                syncState = SyncState.PENDING,
                            ),
                            ExpenseListItemUi(
                                id = "preview-2",
                                merchant = "Northwind Books",
                                amountCents = 3_000,
                                currency = "USD",
                                categoryId = "category-office",
                                categoryName = "OFFICE",
                                categoryColorHex = "#A06A2C",
                                occurredAt = System.currentTimeMillis(),
                                syncState = SyncState.SYNCED,
                            ),
                        ),
                ),
            ),
        monthTotalCents = 4_298,
        pendingCount = 1,
    )
}
