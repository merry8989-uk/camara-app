package com.camara.app.portrait

import androidx.compose.ui.geometry.Offset

/**
 * Portrait depth settings.
 *
 * [subjectPoint] is where the user tapped to choose what stays sharp, normalised
 * (0..1) to the image. When null, the largest subject in the frame is kept.
 */
data class PortraitSettings(
    val enabled: Boolean = false,
    val strength: Float = 0.6f,
    val subjectPoint: Offset? = null
) {
    companion object { val DEFAULT = PortraitSettings() }
}
