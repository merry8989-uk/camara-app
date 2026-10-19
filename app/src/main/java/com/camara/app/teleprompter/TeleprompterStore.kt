package com.camara.app.teleprompter

import android.content.Context
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue

/**
 * Shared, persisted teleprompter state.
 *
 * A singleton so the on-camera overlay and the full-screen editor always agree.
 * Values are Compose state, so any screen reading them recomposes on change.
 */
object TeleprompterStore {

    private const val PREFS = "camara_teleprompter"

    const val DEFAULT_SCRIPT =
        "Paste your script here.\n\n" +
            "Hit play and the words scroll at your pace while you keep an eye on " +
            "yourself in the preview. Drag this panel anywhere, change the speed and " +
            "text size, or flip it with mirror mode when using a beam-splitter rig."

    var script by mutableStateOf(DEFAULT_SCRIPT)
    var speed by mutableFloatStateOf(3f)      // scroll units per frame
    var fontSize by mutableFloatStateOf(30f)
    var mirror by mutableStateOf(false)
    var opacity by mutableFloatStateOf(0.7f)  // panel background opacity

    private var loaded = false

    fun load(context: Context) {
        if (loaded) return
        val p = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        script = p.getString("script", DEFAULT_SCRIPT) ?: DEFAULT_SCRIPT
        speed = p.getFloat("speed", 3f)
        fontSize = p.getFloat("fontSize", 30f)
        mirror = p.getBoolean("mirror", false)
        opacity = p.getFloat("opacity", 0.7f)
        loaded = true
    }

    fun persist(context: Context) {
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .edit()
            .putString("script", script)
            .putFloat("speed", speed)
            .putFloat("fontSize", fontSize)
            .putBoolean("mirror", mirror)
            .putFloat("opacity", opacity)
            .apply()
    }

    /** Cycles the panel opacity so the text can be made more or less transparent. */
    fun cycleOpacity(): Float {
        opacity = when {
            opacity >= 0.8f -> 0.35f
            opacity >= 0.5f -> 0.85f
            else -> 0.6f
        }
        return opacity
    }
}
