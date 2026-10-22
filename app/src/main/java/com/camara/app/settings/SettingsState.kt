package com.camara.app.settings

/** Everything the Settings screen can control. */
data class SettingsState(
    val themeMode: ThemeMode = ThemeMode.SYSTEM,
    val accentColor: Long = 0xFFFF2D78,
    val textScale: Float = 1.0f,
    val density: Float = 1.0f,
    val language: String = "system",
    val notificationsEnabled: Boolean = true,
    /** Log collection. On by default (Rule 7). */
    val diagnosticsEnabled: Boolean = true,
    /** Hide the system bars while using the app (Rule 4: full-screen by default). */
    val immersiveMode: Boolean = true,
    val mirrorSelfie: Boolean = true,
    val gridByDefault: Boolean = false,
    val timerSeconds: Int = 0,
    /** Per-feature on/off switches, persisted individually. */
    val featureFlags: Map<String, Boolean> = FeatureFlag.defaults(),
) {
    fun isEnabled(flag: FeatureFlag): Boolean = featureFlags[flag.id] ?: flag.defaultOn
}

enum class ThemeMode { SYSTEM, LIGHT, DARK }

/**
 * The app's major features, each of which can be switched off from Settings. These are
 * real gates — turning one off removes it from the camera UI (Rule 4: no dead toggles).
 */
enum class FeatureFlag(val id: String, val label: String, val defaultOn: Boolean = true) {
    FILTERS("filters", "Filters"),
    STICKERS("stickers", "Stickers"),
    BEAUTY("beauty", "Beauty / retouch"),
    LENSES("lenses", "AR lenses"),
    PORTRAIT("portrait", "Portrait depth"),
    TELEPROMPTER("teleprompter", "Teleprompter");

    companion object {
        fun defaults(): Map<String, Boolean> = entries.associate { it.id to it.defaultOn }
    }
}
