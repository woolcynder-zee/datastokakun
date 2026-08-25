package com.stokakun.app.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val WhiteText = Color(0xFFFFFFFF)

private val StokAkunDarkColors = darkColorScheme(
    primary = RedAccent,
    onPrimary = WhiteText,
    secondary = RedAccentDark,
    onSecondary = WhiteText,
    background = BackgroundBlack,
    onBackground = TextPrimary,
    surface = SurfaceDark,
    onSurface = TextPrimary,
    surfaceVariant = SurfaceDarkElevated,
    onSurfaceVariant = TextSecondary,
    error = RedAccent,
    onError = WhiteText
)

@Composable
fun StokAkunTheme(content: @Composable () -> Unit) {
    // App is always dark mode per design requirement, regardless of system theme.
    MaterialTheme(
        colorScheme = StokAkunDarkColors,
        typography = StokAkunTypography,
        content = content
    )
}

