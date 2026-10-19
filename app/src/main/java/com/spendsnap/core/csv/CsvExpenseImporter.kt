package com.spendsnap.core.csv

import com.spendsnap.core.amount.AmountParser
import com.spendsnap.data.local.DatabaseSeeder
import com.spendsnap.data.local.entity.CategoryEntity
import com.spendsnap.data.local.entity.ExpenseEntity
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeParseException
import java.util.Locale
import javax.inject.Inject

data class CsvImportError(
    val lineNumber: Int,
    val message: String,
    val row: String,
)

data class CsvImportResult(
    val expenses: List<ExpenseEntity>,
    val errors: List<CsvImportError>,
) {
    val validCount: Int get() = expenses.size
    val invalidCount: Int get() = errors.count { it.lineNumber > 0 }

    val summary: String
        get() = "$validCount imported, ${errors.size} row error(s)"
}

/**
 * Pure CSV parser. It accepts the header `date,merchant,amount,category`,
 * quoted merchant names, ISO dates, and falls back to Other for unknown categories.
 */
class CsvExpenseImporter @Inject constructor() {
    fun parse(
        content: String,
        categories: List<CategoryEntity>,
        now: Long = System.currentTimeMillis(),
    ): CsvImportResult {
        if (content.isBlank()) {
            return CsvImportResult(
                expenses = emptyList(),
                errors = listOf(CsvImportError(0, "CSV file is empty", "")),
            )
        }

        val lines = content.removePrefix("\uFEFF").lineSequence().toList()
        val categoryLookup = categories.associateBy { it.name.lowercase(Locale.US) }
        val otherCategory = categoryLookup["other"]?.id ?: DatabaseSeeder.OTHER_CATEGORY_ID
        val validExpenses = mutableListOf<ExpenseEntity>()
        val errors = mutableListOf<CsvImportError>()

        lines.forEachIndexed { index, rawLine ->
            val lineNumber = index + 1
            if (rawLine.isBlank()) return@forEachIndexed

            val columns = splitCsvLine(rawLine)
            if (index == 0 && columns.firstOrNull()?.trim().equals("date", ignoreCase = true)) {
                return@forEachIndexed
            }
            if (columns.size != 4) {
                errors += CsvImportError(
                    lineNumber = lineNumber,
                    message = "Expected 4 columns (date,merchant,amount,category), found ${columns.size}",
                    row = rawLine,
                )
                return@forEachIndexed
            }

            val dateText = columns[0].trim()
            val merchant = columns[1].trim()
            val amountText = columns[2].trim()
            val categoryText = columns[3].trim()

            val date = try {
                LocalDate.parse(dateText)
            } catch (_: DateTimeParseException) {
                errors += CsvImportError(lineNumber, "Invalid date '$dateText' (use yyyy-MM-dd)", rawLine)
                return@forEachIndexed
            }
            if (merchant.isEmpty()) {
                errors += CsvImportError(lineNumber, "Merchant is required", rawLine)
                return@forEachIndexed
            }
            val amountCents = AmountParser.parse(amountText, Locale.US)
            if (amountCents == null) {
                errors += CsvImportError(lineNumber, "Invalid amount '$amountText'", rawLine)
                return@forEachIndexed
            }

            val categoryId = categoryLookup[categoryText.lowercase(Locale.US)]?.id ?: otherCategory
            validExpenses += ExpenseEntity(
                merchant = merchant,
                amountCents = amountCents,
                categoryId = categoryId,
                occurredAt = date.atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli(),
                note = "Imported from CSV",
                updatedAt = now,
            )
        }

        return CsvImportResult(validExpenses, errors)
    }

    private fun splitCsvLine(line: String): List<String> {
        val columns = mutableListOf<String>()
        val current = StringBuilder()
        var quoted = false
        var index = 0
        while (index < line.length) {
            val character = line[index]
            when {
                character == '"' && quoted && index + 1 < line.length && line[index + 1] == '"' -> {
                    current.append('"')
                    index += 1
                }
                character == '"' -> quoted = !quoted
                character == ',' && !quoted -> {
                    columns += current.toString()
                    current.clear()
                }
                else -> current.append(character)
            }
            index += 1
        }
        columns += current.toString()
        return columns
    }
}
