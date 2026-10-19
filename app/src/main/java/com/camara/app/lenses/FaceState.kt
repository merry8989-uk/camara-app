package com.camara.app.lenses

import androidx.compose.ui.geometry.Offset

/**
 * A detected face, normalised to the upright analysis image (0..1 on both axes).
 *
 * [imageAspect] is width/height of that upright image, which the overlay needs to
 * map the face onto the preview correctly under a FILL_CENTER scale type.
 */
data class FaceState(
    val cx: Float,
    val cy: Float,
    val w: Float,
    val h: Float,
    val rollDegrees: Float,
    val imageAspect: Float,
    val leftEye: Offset? = null,
    val rightEye: Offset? = null,
    val nose: Offset? = null,
    val mouth: Offset? = null
)
