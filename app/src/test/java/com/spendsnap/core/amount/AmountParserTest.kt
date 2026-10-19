package com.spendsnap.core.amount

import com.google.common.truth.Truth.assertThat
import java.util.Locale
import org.junit.Test

class AmountParserTest {
    @Test
    fun wholeDollars_areConvertedToCents() {
        assertThat(AmountParser.parse("12", Locale.US)).isEqualTo(1_200)
    }

    @Test
    fun singleDecimalDigit_isPaddedToCents() {
        assertThat(AmountParser.parse("12.5", Locale.US)).isEqualTo(1_250)
    }

    @Test
    fun groupingAndDecimalSeparators_areUnderstood() {
        assertThat(AmountParser.parse("1,234.56", Locale.US)).isEqualTo(123_456)
    }

    @Test
    fun europeanFormat_isUnderstood() {
        assertThat(AmountParser.parse("1.234,56", Locale.GERMANY)).isEqualTo(123_456)
    }

    @Test
    fun europeanDecimalComma_isUnderstood() {
        assertThat(AmountParser.parse("12,50", Locale.GERMANY)).isEqualTo(1_250)
    }

    @Test
    fun text_isRejected() {
        assertThat(AmountParser.parse("abc", Locale.US)).isNull()
    }

    @Test
    fun negativeAmounts_areRejected() {
        assertThat(AmountParser.parse("-3", Locale.US)).isNull()
    }

    @Test
    fun zero_isRejected() {
        assertThat(AmountParser.parse("0", Locale.US)).isNull()
    }

    @Test
    fun oneCent_isRepresentable() {
        assertThat(AmountParser.parse("0.01", Locale.US)).isEqualTo(1)
    }

    @Test
    fun currencySymbolsAndSpaces_areIgnored() {
        assertThat(AmountParser.parse(" $1 234.56 ", Locale.US)).isEqualTo(123_456)
    }

    @Test
    fun tooManyFractionDigits_areRejectedInsteadOfSilentlyRounded() {
        assertThat(AmountParser.parse("12.345", Locale.US)).isNotNull()
        assertThat(AmountParser.parse("12.3456", Locale.US)).isNull()
    }
}
