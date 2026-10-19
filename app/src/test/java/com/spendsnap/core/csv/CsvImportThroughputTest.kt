package com.spendsnap.core.csv

import com.google.common.truth.Truth.assertThat
import com.spendsnap.data.local.DatabaseSeeder
import org.junit.Test

class CsvImportThroughputTest {
    @Test
    fun oneThousandRows_parsesWithMeasuredThroughput() {
        val categories = DatabaseSeeder.DEFAULT_CATEGORIES
        val categoryNames = listOf("Meals", "Travel", "Software", "Office")
        val csv =
            buildString {
                appendLine("date,merchant,amount,category")
                repeat(1_000) { index ->
                    val amount = if (index % 200 == 0) "not-an-amount" else "12.34"
                    val category = categoryNames[index % categoryNames.size]
                    val day = ((index % 28) + 1).toString().padStart(2, '0')
                    appendLine("2026-10-$day,Merchant $index,$amount,$category")
                }
            }
        val importer = CsvExpenseImporter()

        val start = System.nanoTime()
        val result = importer.parse(csv, categories)
        val elapsedMillis = (System.nanoTime() - start) / 1_000_000.0

        assertThat(result.validCount).isEqualTo(995)
        assertThat(result.invalidCount).isEqualTo(5)
        println(
            "CSV_THROUGHPUT rows=1000 valid=${result.validCount} " +
                "invalid=${result.invalidCount} elapsed_ms=$elapsedMillis",
        )
    }
}
