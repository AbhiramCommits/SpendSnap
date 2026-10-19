package com.spendsnap.core.ui

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import java.math.BigDecimal
import java.text.NumberFormat
import java.util.Currency
import java.util.Locale

@Composable
fun MoneyText(
    amountCents: Long,
    currency: String = "USD",
    modifier: Modifier = Modifier,
    style: TextStyle = MaterialTheme.typography.titleMedium,
    color: Color = Color.Unspecified,
) {
    Text(
        text = formatMoney(amountCents, currency),
        modifier = modifier,
        style = style,
        color = color,
    )
}

fun formatMoney(
    amountCents: Long,
    currency: String = "USD",
    locale: Locale = Locale.getDefault(),
): String {
    val format = NumberFormat.getCurrencyInstance(locale)
    runCatching { format.currency = Currency.getInstance(currency) }
    return format.format(BigDecimal.valueOf(amountCents, 2))
}
