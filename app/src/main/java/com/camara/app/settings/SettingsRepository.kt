package com.camara.app.settings

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.floatPreferencesKey
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "camara_settings")

/**
 * Persists every setting to DataStore. Reads are a Flow, so the UI reacts the moment a
 * value changes — no dead toggles, and everything survives a restart (Rule 4).
 */
class SettingsRepository(private val context: Context) {

    private object Keys {
        val THEME_MODE = stringPreferencesKey("theme_mode")
        val ACCENT_COLOR = longPreferencesKey("accent_color")
        val TEXT_SCALE = floatPreferencesKey("text_scale")
        val DENSITY = floatPreferencesKey("density")
        val LANGUAGE = stringPreferencesKey("language")
        val NOTIFICATIONS = booleanPreferencesKey("notifications_enabled")
        val DIAGNOSTICS = booleanPreferencesKey("diagnostics_enabled")
        val IMMERSIVE = booleanPreferencesKey("immersive_mode")
        val MIRROR_SELFIE = booleanPreferencesKey("mirror_selfie")
        val GRID = booleanPreferencesKey("grid_by_default")
        val TIMER = intPreferencesKey("timer_seconds")
        fun flag(id: String) = booleanPreferencesKey("flag_$id")
    }

    val settings: Flow<SettingsState> = context.dataStore.data.map { p ->
        SettingsState(
            themeMode = p[Keys.THEME_MODE]
                ?.let { runCatching { ThemeMode.valueOf(it) }.getOrNull() }
                ?: ThemeMode.SYSTEM,
            accentColor = p[Keys.ACCENT_COLOR] ?: 0xFFFF2D78,
            textScale = p[Keys.TEXT_SCALE] ?: 1.0f,
            density = p[Keys.DENSITY] ?: 1.0f,
            language = p[Keys.LANGUAGE] ?: "system",
            notificationsEnabled = p[Keys.NOTIFICATIONS] ?: true,
            diagnosticsEnabled = p[Keys.DIAGNOSTICS] ?: true,
            immersiveMode = p[Keys.IMMERSIVE] ?: true,
            mirrorSelfie = p[Keys.MIRROR_SELFIE] ?: true,
            gridByDefault = p[Keys.GRID] ?: false,
            timerSeconds = p[Keys.TIMER] ?: 0,
            featureFlags = FeatureFlag.entries.associate { flag ->
                flag.id to (p[Keys.flag(flag.id)] ?: flag.defaultOn)
            }
        )
    }

    suspend fun setThemeMode(mode: ThemeMode) = edit { it[Keys.THEME_MODE] = mode.name }
    suspend fun setAccentColor(color: Long) = edit { it[Keys.ACCENT_COLOR] = color }
    suspend fun setTextScale(scale: Float) = edit { it[Keys.TEXT_SCALE] = scale }
    suspend fun setDensity(value: Float) = edit { it[Keys.DENSITY] = value }
    suspend fun setLanguage(language: String) = edit { it[Keys.LANGUAGE] = language }
    suspend fun setNotifications(enabled: Boolean) = edit { it[Keys.NOTIFICATIONS] = enabled }
    suspend fun setDiagnosticsEnabled(enabled: Boolean) = edit { it[Keys.DIAGNOSTICS] = enabled }
    suspend fun setImmersive(enabled: Boolean) = edit { it[Keys.IMMERSIVE] = enabled }
    suspend fun setMirrorSelfie(enabled: Boolean) = edit { it[Keys.MIRROR_SELFIE] = enabled }
    suspend fun setGridByDefault(enabled: Boolean) = edit { it[Keys.GRID] = enabled }
    suspend fun setTimerSeconds(seconds: Int) = edit { it[Keys.TIMER] = seconds }

    suspend fun setFeatureFlag(id: String, enabled: Boolean) = edit { it[Keys.flag(id)] = enabled }

    suspend fun reset() = edit { it.clear() }

    private suspend fun edit(block: (androidx.datastore.preferences.core.MutablePreferences) -> Unit) {
        context.dataStore.edit(block)
    }
}
