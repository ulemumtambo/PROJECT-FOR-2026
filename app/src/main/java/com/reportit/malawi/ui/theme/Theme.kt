package com.reportit.malawi.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val LightColors = lightColorScheme(
    primary = GreenPrimary,
    onPrimary = ColorWhite,
    secondary = Mint,
    tertiary = GreenDark,

    background = BackgroundLight,
    surface = ColorWhite,

    onBackground = TextPrimary,
    onSurface = TextPrimary,
    onSurfaceVariant = TextSecondary,

    outline = Color(0xFFD0D5D2),
)

private val DarkColors = darkColorScheme(
    primary = GreenPrimary,
    onPrimary = ColorWhite,
    secondary = Mint,
    tertiary = GreenDark,

    background = ColorWhite,
    surface = ColorWhite,

    onBackground = TextPrimary,
    onSurface = TextPrimary,
    onSurfaceVariant = TextSecondary,

    outline = Color(0xFFD0D5D2),
)

@Composable
fun ReportItMalawiTheme(
    darkTheme: Boolean = false,
    content: @Composable () -> Unit
) {
    val colors = LightColors

    MaterialTheme(
        colorScheme = colors,
        typography = Typography,
        content = content,
    )
}
