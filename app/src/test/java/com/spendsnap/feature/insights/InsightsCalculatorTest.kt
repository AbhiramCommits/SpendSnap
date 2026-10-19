package com.spendsnap.feature.insights

import com.google.common.truth.Truth.assertThat
import com.spendsnap.data.local.DatabaseSeeder
import com.spendsnap.data.local.entity.ExpenseEntity
import java.time.LocalDate
import java.time.YearMonth
import java.time.ZoneId
import org.junit.Test

class InsightsCalculatorTest {
    private val zone = ZoneId.of("UTC")
    private val month = YearMonth.of(2026, 10)

    @Test
    fun calculate_totalsCategoriesMerchantsAndDelta() {
        val expenses = listOf(
            expense("Cafe One", 1_000, "category-meals", month.atDay(2)),
            expense("Cafe One", 500, "category-meals", month.atDay(3)),
            expense("Cloud Co", 2_000, "category-software", month.atDay(4)),
            expense("Airline", 4_000, "category-travel", month.atDay(5)),
            expense("Hotel", 3_000, "category-travel", month.atDay(6)),
            expense("September Cafe", 2_000, "category-meals", month.minusMonths(1).atDay(2)),
        )

        val data = InsightsCalculator.calculate(
            expenses = expenses,
            categories = DatabaseSeeder.DEFAULT_CATEGORIES,
            selectedMonth = month,
            pendingCount = 2,
            zone = zone,
        )

        assertThat(data.totalCents).isEqualTo(10_500)
        assertThat(data.previousMonthTotalCents).isEqualTo(2_000)
        assertThat(data.monthOverMonthPercent).isWithin(0.001).of(425.0)
        assertThat(data.categorySpend.map { it.name })
            .containsExactly("Travel", "Software", "Meals")
            .inOrder()
        assertThat(data.categorySpend.first().totalCents).isEqualTo(7_000)
        assertThat(data.categorySpend.first().fraction).isWithin(0.0001f).of(0.6667f)
        assertThat(data.topMerchants.map { it.merchant })
            .containsExactly("Airline", "Hotel", "Cloud Co")
            .inOrder()
        assertThat(data.topMerchants.first().totalCents).isEqualTo(4_000)
        assertThat(data.pendingCount).isEqualTo(2)
    }

    @Test
    fun noPreviousMonth_hasNoPercentDelta() {
        val data = InsightsCalculator.calculate(
            expenses = listOf(expense("Cafe", 1_000, "category-meals", month.atDay(2))),
            categories = DatabaseSeeder.DEFAULT_CATEGORIES,
            selectedMonth = month,
            pendingCount = 0,
            zone = zone,
        )

        assertThat(data.totalCents).isEqualTo(1_000)
        assertThat(data.previousMonthTotalCents).isEqualTo(0)
        assertThat(data.monthOverMonthPercent).isNull()
    }

    @Test
    fun emptyMonth_returnsZeroedButValidBreakdown() {
        val data = InsightsCalculator.calculate(
            expenses = emptyList(),
            categories = DatabaseSeeder.DEFAULT_CATEGORIES,
            selectedMonth = month,
            pendingCount = 0,
            zone = zone,
        )

        assertThat(data.totalCents).isEqualTo(0)
        assertThat(data.categorySpend).isEmpty()
        assertThat(data.topMerchants).isEmpty()
        assertThat(data.monthOverMonthPercent).isNull()
    }

    private fun expense(
        merchant: String,
        amountCents: Long,
        categoryId: String,
        date: LocalDate,
    ) = ExpenseEntity(
        merchant = merchant,
        amountCents = amountCents,
        categoryId = categoryId,
        occurredAt = date.atStartOfDay(zone).toInstant().toEpochMilli(),
    )
}
