package com.spendsnap.core.csv

import com.google.common.truth.Truth.assertThat
import com.spendsnap.data.local.DatabaseSeeder
import org.junit.Test

class CsvExpenseImporterTest {
    private val importer = CsvExpenseImporter()
    private val categories = DatabaseSeeder.DEFAULT_CATEGORIES

    @Test
    fun validRows_areParsedWithCategoriesAndAmounts() {
        val csv = """
            date,merchant,amount,category
            2026-10-01,Corner Cafe,12.50,Meals
            2026-10-02,"Northwind, Inc.","1,234.56",Software
        """.trimIndent()

        val result = importer.parse(csv, categories, now = 1_000)

        assertThat(result.validCount).isEqualTo(2)
        assertThat(result.errors).isEmpty()
        assertThat(result.expenses[0].merchant).isEqualTo("Corner Cafe")
        assertThat(result.expenses[0].amountCents).isEqualTo(1_250)
        assertThat(result.expenses[0].categoryId).isEqualTo("category-meals")
        assertThat(result.expenses[1].merchant).isEqualTo("Northwind, Inc.")
        assertThat(result.expenses[1].amountCents).isEqualTo(123_456)
        assertThat(result.expenses[1].categoryId).isEqualTo("category-software")
    }

    @Test
    fun malformedDate_reportsThePhysicalLine() {
        val csv = """
            date,merchant,amount,category
            not-a-date,Cafe,10.00,Meals
        """.trimIndent()

        val result = importer.parse(csv, categories)

        assertThat(result.validCount).isEqualTo(0)
        assertThat(result.errors).hasSize(1)
        assertThat(result.errors.single().lineNumber).isEqualTo(2)
        assertThat(result.errors.single().message).contains("Invalid date")
    }

    @Test
    fun badAmount_reportsThePhysicalLine() {
        val csv = """
            date,merchant,amount,category
            2026-10-01,Cafe,abc,Meals
        """.trimIndent()

        val result = importer.parse(csv, categories)

        assertThat(result.validCount).isEqualTo(0)
        assertThat(result.errors.single().message).contains("Invalid amount")
    }

    @Test
    fun unknownCategory_fallsBackToOther() {
        val csv = """
            date,merchant,amount,category
            2026-10-01,Cafe,10.00,Space Travel
        """.trimIndent()

        val result = importer.parse(csv, categories)

        assertThat(result.validCount).isEqualTo(1)
        assertThat(result.expenses.single().categoryId)
            .isEqualTo(DatabaseSeeder.OTHER_CATEGORY_ID)
    }

    @Test
    fun emptyFile_isReportedWithoutCrashing() {
        val result = importer.parse("", categories)

        assertThat(result.validCount).isEqualTo(0)
        assertThat(result.errors).hasSize(1)
        assertThat(result.errors.single().lineNumber).isEqualTo(0)
    }

    @Test
    fun malformedColumnCount_reportsRowError() {
        val csv = """
            date,merchant,amount,category
            2026-10-01,Cafe,10.00
        """.trimIndent()

        val result = importer.parse(csv, categories)

        assertThat(result.validCount).isEqualTo(0)
        assertThat(result.errors.single().message).contains("Expected 4 columns")
    }
}
