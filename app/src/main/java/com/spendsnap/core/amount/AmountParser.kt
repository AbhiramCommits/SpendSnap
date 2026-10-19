package com.spendsnap.core.amount

import java.math.BigDecimal
import java.math.RoundingMode
import java.text.DecimalFormatSymbols
import java.util.Locale

/**
 * Parses human-entered money into integer cents. Never performs money math in
 * Double, and understands both "1,234.56" and "1.234,56" style input.
 */
object AmountParser {
    private val allowedCurrencyMarks = setOf('$', '€', '£', '¥', '₹', '₽', '₩')
    private val whitespace = setOf(' ', '\u00A0', '\u202F', '\t')

    fun parse(raw: String, locale: Locale = Locale.getDefault()): Long? {
        if (raw.isBlank()) return null

        val symbols = DecimalFormatSymbols.getInstance(locale)
        val cleaned = buildString {
            raw.trim().forEach { original ->
                val character = normalizeDigit(original)
                when {
                    character == symbols.decimalSeparator -> append('.')
                    character == symbols.groupingSeparator -> append(',')
                    character in whitespace -> Unit
                    character in allowedCurrencyMarks -> Unit
                    character == '\'' -> Unit
                    else -> append(character)
                }
            }
        }
        if (cleaned.isEmpty() || cleaned.any { it == '-' || it == '+' }) return null
        if (cleaned.any { !it.isDigit() && it != '.' && it != ',' }) return null
        if (cleaned.none(Char::isDigit)) return null

        val decimalSymbol = symbols.decimalSeparator
        val groupingSymbol = symbols.groupingSeparator

        val dotCount = cleaned.count { it == '.' }
        val commaCount = cleaned.count { it == ',' }
        val lastDot = cleaned.lastIndexOf('.')
        val lastComma = cleaned.lastIndexOf(',')

        val decimalSeparator: Char? = when {
            dotCount > 0 && commaCount > 0 -> if (lastDot > lastComma) '.' else ','
            dotCount == 1 -> resolveSingleSeparator(
                value = cleaned,
                separator = '.',
                decimalSymbol = decimalSymbol,
                groupingSymbol = groupingSymbol,
            )
            commaCount == 1 -> resolveSingleSeparator(
                value = cleaned,
                separator = ',',
                decimalSymbol = decimalSymbol,
                groupingSymbol = groupingSymbol,
            )
            dotCount > 1 || commaCount > 1 -> null
            else -> null
        }

        val normalized = if (decimalSeparator == null) {
            val separators = cleaned.filter { it == '.' || it == ',' }
            if (separators.isNotEmpty() && !hasValidGrouping(cleaned)) return null
            cleaned.filter(Char::isDigit)
        } else {
            if (cleaned.count { it == decimalSeparator } != 1) return null
            val pieces = cleaned.split(decimalSeparator)
            if (pieces.size != 2) return null
            if (!hasValidGrouping(pieces[0])) return null
            val whole = pieces[0].filter(Char::isDigit)
            val fraction = pieces[1]
            if (fraction.length !in 1..2 || fraction.any { !it.isDigit() }) return null
            "${whole.ifEmpty { "0" }}.${fraction.padEnd(2, '0')}"
        }

        if (normalized.isEmpty()) return null

        return runCatching {
            val amount = BigDecimal(normalized)
            val cents = amount.movePointRight(2).setScale(0, RoundingMode.UNNECESSARY)
            cents.longValueExact().takeIf { it > 0 }
        }.getOrNull()
    }

    /**
     * Group marks must separate groups of three digits, except the first group
     * which may have one to three. This rejects inputs such as 12.3456 while
     * still accepting 1,234.56 and 1.234.567.
     */
    private fun normalizeDigit(character: Char): Char {
        val digit = Character.digit(character, 10)
        return if (digit in 0..9) ('0' + digit) else character
    }

    private fun hasValidGrouping(integerPart: String): Boolean {
        if (integerPart.none { it == '.' || it == ',' }) {
            return integerPart.all(Char::isDigit)
        }
        val groups = integerPart.split('.', ',')
        if (groups.first().length !in 1..3 || groups.first().any { !it.isDigit() }) return false
        return groups.drop(1).all { group ->
            group.length == 3 && group.all(Char::isDigit)
        }
    }

    private fun resolveSingleSeparator(
        value: String,
        separator: Char,
        decimalSymbol: Char,
        groupingSymbol: Char,
    ): Char? {
        val digitsAfter = value.length - value.lastIndexOf(separator) - 1
        if (digitsAfter == 0) return null
        if (separator == decimalSymbol) {
            return if (digitsAfter <= 2) separator else null
        }
        if (separator == groupingSymbol) {
            return if (digitsAfter == 3) null else separator
        }
        return if (digitsAfter == 3) null else separator
    }
}
