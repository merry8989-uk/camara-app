package com.camara.app.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.graphics.Color

private fun darkScheme(accent: Color) = darkColorScheme(
    primary = accent,
    onPrimary = Color.White,
    secondary = Accent2,
    onSecondary = Color.White,
    background = Ink,
    onBackground = TextPrimary,
    surface = SurfaceDark,
    onSurface = TextPrimary,
    surfaceVariant = SurfaceElevated,
    onSurfaceVariant = TextSecondary,
    outline = ChipBg,
)

private fun lightScheme(accent: Color) = lightColorScheme(
    primary = accent,
    onPrimary = Color.White,
    secondary = Accent2,
    onSecondary = Color.White,
    background = Color(0xFFFAFAFC),
    onBackground = Color(0xFF111114),
    surface = Color.White,
    onSurface = Color(0xFF111114),
    surfaceVariant = Color(0xFFEDEDF2),
    onSurfaceVariant = Color(0xFF55555E),
    outline = Color(0xFFCFCFD8),
)

/**
 * The app theme. [darkTheme] and [accent] come from Settings, so the Appearance controls
 * actually change the UI (App Factory Rule 4).
 *
 * The camera screen deliberately keeps its own dark chrome regardless of the theme —
 * that is how capture-first apps behave.
 */
@Composable
fun CamaraTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    accent: Color = Accent,
    content: @Composable () -> Unit
) {
    val scheme = if (darkTheme) darkScheme(accent) else lightScheme(accent)

    MaterialTheme(
        colorScheme = scheme,
        typography = CamaraTypography,
    ) {
        // Safety net. Material3 does not set LocalContentColor at the theme level — only
        // Surface and friends do — so a screen that paints a background without wrapping
        // itself in a Surface inherits Compose's default, which is BLACK. On a light
        // background that goes unnoticed; on a dark one the text disappears entirely,
        // which is exactly what happened to Settings. Providing it here means a forgotten
        // Surface degrades to readable text instead of an invisible screen.
        CompositionLocalProvider(LocalContentColor provides scheme.onBackground) {
            content()
        }
    }
}
