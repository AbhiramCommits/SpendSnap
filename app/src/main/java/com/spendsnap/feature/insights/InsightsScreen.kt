package com.spendsnap.feature.insights

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.TrendingDown
import androidx.compose.material.icons.automirrored.outlined.TrendingFlat
import androidx.compose.material.icons.automirrored.outlined.TrendingUp
import androidx.compose.material.icons.outlined.ChevronLeft
import androidx.compose.material.icons.outlined.ChevronRight
import androidx.compose.material.icons.outlined.CloudUpload
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.spendsnap.core.ui.CategoryDot
import com.spendsnap.core.ui.EmptyState
import com.spendsnap.core.ui.ErrorBanner
import com.spendsnap.core.ui.LoadingState
import com.spendsnap.core.ui.MoneyText
import com.spendsnap.core.ui.parseColor
import com.spendsnap.ui.theme.SpendSnapTheme
import java.time.YearMonth
import java.time.format.DateTimeFormatter
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun InsightsScreen(viewModel: InsightsViewModel = hiltViewModel()) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    InsightsContent(
        state = state,
        onPreviousMonth = { viewModel.onEvent(InsightsEvent.PreviousMonth) },
        onNextMonth = { viewModel.onEvent(InsightsEvent.NextMonth) },
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun InsightsContent(
    state: InsightsUiState,
    onPreviousMonth: () -> Unit,
    onNextMonth: () -> Unit,
) {
    Scaffold(
        modifier = Modifier.testTag("insights_screen"),
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text("Insights", style = MaterialTheme.typography.titleLarge)
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
                            contentDescription = "Previous month insights",
                        )
                    }
                    IconButton(onClick = onNextMonth) {
                        Icon(
                            Icons.Outlined.ChevronRight,
                            contentDescription = "Next month insights",
                        )
                    }
                },
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
                    label = "Loading insights",
                )

            state.error != null ->
                Column(
                    modifier =
                        Modifier
                            .fillMaxSize()
                            .padding(padding)
                            .padding(16.dp),
                ) {
                    ErrorBanner(message = state.error)
                }

            state.data == null || state.data?.totalCents == 0L ->
                EmptyState(
                    modifier =
                        Modifier
                            .fillMaxSize()
                            .padding(padding)
                            .testTag("insights_empty"),
                    title = "No insights this month",
                    message = "Add expenses or import a CSV to unlock category and merchant trends.",
                )

            else ->
                InsightsBody(
                    state = state,
                    modifier =
                        Modifier
                            .fillMaxSize()
                            .padding(padding),
                )
        }
    }
}

@Composable
private fun InsightsBody(
    state: InsightsUiState,
    modifier: Modifier = Modifier,
) {
    val data = state.data ?: return
    Column(
        modifier =
            modifier
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors =
                CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                ),
            shape = RoundedCornerShape(20.dp),
        ) {
            Column(modifier = Modifier.padding(20.dp)) {
                Text(
                    "TOTAL SPEND",
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                )
                Spacer(Modifier.height(4.dp))
                MoneyText(
                    amountCents = data.totalCents,
                    modifier = Modifier.testTag("insights_total"),
                    style = MaterialTheme.typography.displaySmall,
                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                )
                Spacer(Modifier.height(8.dp))
                MonthDelta(data)
                Spacer(Modifier.height(8.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        Icons.Outlined.CloudUpload,
                        contentDescription = "Pending sync",
                        modifier = Modifier.size(18.dp),
                        tint = MaterialTheme.colorScheme.onPrimaryContainer,
                    )
                    Spacer(Modifier.width(6.dp))
                    Text(
                        if (data.pendingCount == 0) {
                            "Everything is synced"
                        } else {
                            "${data.pendingCount} record(s) waiting to sync"
                        },
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onPrimaryContainer,
                        modifier = Modifier.testTag("insights_pending"),
                    )
                }
            }
        }

        Text("Spend by category", style = MaterialTheme.typography.titleLarge)
        data.categorySpend.forEach { category ->
            CategoryBar(category)
        }

        Text("Top merchants", style = MaterialTheme.typography.titleLarge)
        data.topMerchants.forEachIndexed { index, merchant ->
            Row(
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .testTag("top_merchant_${index + 1}"),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        "${index + 1}.",
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.primary,
                    )
                    Spacer(Modifier.width(8.dp))
                    Text(merchant.merchant, style = MaterialTheme.typography.bodyLarge)
                }
                MoneyText(merchant.totalCents, style = MaterialTheme.typography.bodyLarge)
            }
        }
        Spacer(Modifier.height(24.dp))
    }
}

@Composable
private fun MonthDelta(data: InsightsData) {
    val delta = data.monthOverMonthPercent
    val icon =
        when {
            delta == null -> Icons.AutoMirrored.Outlined.TrendingFlat
            delta > 0 -> Icons.AutoMirrored.Outlined.TrendingUp
            delta < 0 -> Icons.AutoMirrored.Outlined.TrendingDown
            else -> Icons.AutoMirrored.Outlined.TrendingFlat
        }
    val text =
        when {
            delta == null -> "No prior-month data"
            delta > 0 -> "+${"%.1f".format(Locale.getDefault(), delta)}% vs last month"
            delta < 0 -> "${"%.1f".format(Locale.getDefault(), delta)}% vs last month"
            else -> "Flat vs last month"
        }
    Row(verticalAlignment = Alignment.CenterVertically) {
        Icon(
            icon,
            contentDescription = "Month over month trend",
            modifier = Modifier.size(20.dp),
            tint = MaterialTheme.colorScheme.onPrimaryContainer,
        )
        Spacer(Modifier.width(6.dp))
        Text(
            text,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onPrimaryContainer,
            modifier = Modifier.testTag("month_over_month"),
        )
    }
}

@Composable
private fun CategoryBar(category: CategorySpendUi) {
    Column(
        modifier =
            Modifier
                .fillMaxWidth()
                .testTag("category_bar_${category.categoryId}"),
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                CategoryDot(category.colorHex)
                Spacer(Modifier.width(8.dp))
                Text(category.name, style = MaterialTheme.typography.bodyLarge)
            }
            MoneyText(category.totalCents, style = MaterialTheme.typography.bodyLarge)
        }
        Box(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .height(10.dp)
                    .background(
                        MaterialTheme.colorScheme.surfaceVariant,
                        RoundedCornerShape(50),
                    )
                    .semantics {
                        contentDescription = "${category.name}: ${(category.fraction * 100).toInt()} percent"
                    },
        ) {
            Box(
                modifier =
                    Modifier
                        .fillMaxWidth(category.fraction.coerceIn(0f, 1f))
                        .height(10.dp)
                        .background(parseColor(category.colorHex), RoundedCornerShape(50)),
            )
        }
        Text(
            "${"%.0f".format(Locale.getDefault(), category.fraction * 100)}% of month",
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

private fun YearMonth.formatMonth(): String = format(DateTimeFormatter.ofPattern("MMMM yyyy", Locale.getDefault()))

@Preview(name = "Insights light", showBackground = true)
@Composable
fun InsightsLightPreview() {
    SpendSnapTheme(darkTheme = false) {
        InsightsContent(
            state = previewInsightsState(),
            onPreviousMonth = {},
            onNextMonth = {},
        )
    }
}

@Preview(name = "Insights dark", showBackground = true)
@Composable
fun InsightsDarkPreview() {
    SpendSnapTheme(darkTheme = true) {
        InsightsContent(
            state = previewInsightsState(),
            onPreviousMonth = {},
            onNextMonth = {},
        )
    }
}

private fun previewInsightsState() =
    InsightsUiState(
        isLoading = false,
        selectedMonth = YearMonth.now(),
        data =
            InsightsData(
                totalCents = 123_456,
                previousMonthTotalCents = 100_000,
                monthOverMonthPercent = 23.456,
                categorySpend =
                    listOf(
                        CategorySpendUi("category-travel", "Travel", "#2F6F8F", 70_000, 0.57f),
                        CategorySpendUi("category-meals", "Meals", "#C8553D", 33_456, 0.27f),
                        CategorySpendUi("category-software", "Software", "#6B5CA5", 20_000, 0.16f),
                    ),
                topMerchants =
                    listOf(
                        MerchantSpendUi("Airline", 70_000),
                        MerchantSpendUi("Corner Cafe", 33_456),
                        MerchantSpendUi("Cloud Co", 20_000),
                    ),
                pendingCount = 2,
            ),
    )
