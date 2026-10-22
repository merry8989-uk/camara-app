package com.camara.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Density
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import com.camara.app.diagnostics.DiagnosticsLogger
import com.camara.app.settings.SettingsRepository
import com.camara.app.settings.SettingsState
import com.camara.app.settings.ThemeMode
import com.camara.app.ui.navigation.CamaraRoot
import com.camara.app.ui.theme.CamaraTheme

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        WindowCompat.setDecorFitsSystemWindows(window, false)

        val settingsRepo = SettingsRepository(applicationContext)

        setContent {
            val settings by settingsRepo.settings.collectAsState(initial = SettingsState())

            val dark = when (settings.themeMode) {
                ThemeMode.SYSTEM -> isSystemInDarkTheme()
                ThemeMode.LIGHT -> false
                ThemeMode.DARK -> true
            }

            // Text size and density are applied here, so the Appearance sliders take
            // effect immediately rather than only after a restart.
            val base = LocalDensity.current
            CompositionLocalProvider(
                LocalDensity provides Density(
                    density = base.density * settings.density,
                    fontScale = settings.textScale,
                )
            ) {
                LaunchedEffect(settings.diagnosticsEnabled) {
                    DiagnosticsLogger.setEnabled(settings.diagnosticsEnabled)
                }
                LaunchedEffect(settings.immersiveMode) {
                    applyImmersiveMode(settings.immersiveMode)
                }

                CamaraTheme(darkTheme = dark, accent = Color(settings.accentColor)) {
                    CamaraRoot()
                }
            }
        }
    }

    /**
     * Hides or restores the system bars (Rule 4: full-screen by default).
     *
     * Named "apply…" rather than "set…" because Activity already has a hidden
     * `setImmersive` member, which a plain override would clash with.
     */
    private fun applyImmersiveMode(enabled: Boolean) {
        val controller = WindowCompat.getInsetsController(window, window.decorView)
        if (enabled) {
            controller.hide(WindowInsetsCompat.Type.systemBars())
            controller.systemBarsBehavior =
                WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
        } else {
            controller.show(WindowInsetsCompat.Type.systemBars())
        }
    }
}
