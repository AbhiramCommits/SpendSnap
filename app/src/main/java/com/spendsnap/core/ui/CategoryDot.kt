package com.spendsnap.core.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.core.graphics.ColorUtils

@Composable
fun CategoryDot(
    colorHex: String,
    modifier: Modifier = Modifier,
) {
    androidx.compose.foundation.layout.Box(
        modifier =
            modifier
                .size(10.dp)
                .background(parseColor(colorHex), CircleShape),
    )
}

fun parseColor(hex: String): Color =
    runCatching {
        Color(android.graphics.Color.parseColor(hex))
    }.getOrElse { Color(0xFF6C757D) }

fun contrastTextColor(backgroundHex: String): Color {
    val background = android.graphics.Color.parseColor(backgroundHex)
    return if (ColorUtils.calculateLuminance(background) > 0.5) Color.Black else Color.White
}
