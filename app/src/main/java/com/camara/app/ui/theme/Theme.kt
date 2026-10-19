package com.camara.app.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable

private val CamaraColors = darkColorScheme(
    primary = Accent,
    onPrimary = TextPrimary,
    secondary = Accent2,
    onSecondary = TextPrimary,
    background = Ink,
    onBackground = TextPrimary,
    surface = SurfaceDark,
    onSurface = TextPrimary,
    surfaceVariant = SurfaceElevated,
    onSurfaceVariant = TextSecondary,
    outline = ChipBg
)

@Composable
fun CamaraTheme(
    @Suppress("UNUSED_PARAMETER") darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    // Camara is dark-first by design, like most capture-first camera apps.
    MaterialTheme(
        colorScheme = CamaraColors,
        typography = CamaraTypography,
        content = content
    )
}
