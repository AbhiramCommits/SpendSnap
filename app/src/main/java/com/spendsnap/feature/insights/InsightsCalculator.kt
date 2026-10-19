package com.spendsnap.feature.insights

import com.spendsnap.data.local.entity.CategoryEntity
import com.spendsnap.data.local.entity.ExpenseEntity
import java.time.Instant
import java.time.YearMonth
import java.time.ZoneId

data class CategorySpendUi(
    val categoryId: String,
    val name: String,
    val colorHex: String,
    val totalCents: Long,
    val fraction: Float,
)

data class MerchantSpendUi(
    val merchant: String,
    val totalCents: Long,
)

data class InsightsData(
    val totalCents: Long,
    val previousMonthTotalCents: Long,
    val monthOverMonthPercent: Double?,
    val categorySpend: List<CategorySpendUi>,
    val topMerchants: List<MerchantSpendUi>,
    val pendingCount: Int,
)

object InsightsCalculator {
    fun calculate(
        expenses: List<ExpenseEntity>,
        categories: List<CategoryEntity>,
        selectedMonth: YearMonth,
        pendingCount: Int,
        zone: ZoneId = ZoneId.systemDefault(),
    ): InsightsData {
        val current = expenses.filter { it.isInMonth(selectedMonth, zone) }
        val previous = expenses.filter { it.isInMonth(selectedMonth.minusMonths(1), zone) }
        val totalCents = current.sumOf { it.amountCents }
        val previousTotalCents = previous.sumOf { it.amountCents }
        val categoryLookup = categories.associateBy { it.id }

        val categorySpend = current
            .groupBy { it.categoryId }
            .map { (categoryId, rows) ->
                val category = categoryLookup[categoryId]
                val categoryTotal = rows.sumOf { it.amountCents }
                CategorySpendUi(
                    categoryId = categoryId,
                    name = category?.name ?: "Other",
                    colorHex = category?.colorHex ?: "#6C757D",
                    totalCents = categoryTotal,
                    fraction = if (totalCents == 0L) 0f else {
                        categoryTotal.toFloat() / totalCents.toFloat()
                    },
                )
            }
            .sortedByDescending { it.totalCents }

        val topMerchants = current
            .groupBy { it.merchant.trim() }
            .map { (merchant, rows) -> MerchantSpendUi(merchant, rows.sumOf { it.amountCents }) }
            .sortedByDescending { it.totalCents }
            .take(3)

        val monthOverMonthPercent = if (previousTotalCents == 0L) {
            null
        } else {
            ((totalCents - previousTotalCents).toDouble() / previousTotalCents.toDouble()) * 100.0
        }

        return InsightsData(
            totalCents = totalCents,
            previousMonthTotalCents = previousTotalCents,
            monthOverMonthPercent = monthOverMonthPercent,
            categorySpend = categorySpend,
            topMerchants = topMerchants,
            pendingCount = pendingCount,
        )
    }

    private fun ExpenseEntity.isInMonth(month: YearMonth, zone: ZoneId): Boolean =
        YearMonth.from(Instant.ofEpochMilli(occurredAt).atZone(zone).toLocalDate()) == month
}
