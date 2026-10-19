package com.spendsnap.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext

private val LightColors =
    lightColorScheme(
        primary = Forest,
        onPrimary = Sand,
        primaryContainer = Mint,
        onPrimaryContainer = Ink,
        secondary = Sky,
        onSecondary = Sand,
        secondaryContainer = ColorTokens.SkyContainer,
        onSecondaryContainer = Ink,
        tertiary = Amber,
        background = Sand,
        onBackground = Ink,
        surface = ColorTokens.WarmWhite,
        onSurface = Ink,
        surfaceVariant = ColorTokens.SandVariant,
        onSurfaceVariant = ColorTokens.InkMuted,
        error = Coral,
        onError = Sand,
    )

private val DarkColors =
    darkColorScheme(
        primary = ForestLight,
        onPrimary = ColorTokens.DeepForest,
        primaryContainer = ColorTokens.DeepForest,
        onPrimaryContainer = Mint,
        secondary = ColorTokens.SkyLight,
        onSecondary = ColorTokens.DeepForest,
        secondaryContainer = ColorTokens.DeepSky,
        onSecondaryContainer = ColorTokens.SkyLight,
        tertiary = ColorTokens.AmberLight,
        background = DarkSurface,
        onBackground = ColorTokens.NightText,
        surface = ColorTokens.NightSurface,
        onSurface = ColorTokens.NightText,
        surfaceVariant = DarkSurfaceVariant,
        onSurfaceVariant = ColorTokens.NightMuted,
        error = ColorTokens.CoralLight,
        onError = ColorTokens.DeepForest,
    )

@Composable
fun SpendSnapTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit,
) {
    val context = LocalContext.current
    val colorScheme =
        when {
            dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S ->
                if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)

            darkTheme -> DarkColors
            else -> LightColors
        }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = SpendSnapTypography,
        content = content,
    )
}
