package com.camara.app.lenses

import androidx.compose.ui.geometry.Offset
import kotlin.math.atan2
import kotlin.math.hypot

/**
 * A detected face, normalised to the upright analysis image (0..1 on both axes).
 *
 * Carries landmarks *and* contours. Contours make lenses follow the real face
 * shape, and they give a far more stable anchor than the bounding box: lenses are
 * placed using the eye midpoint, scaled by the inter-ocular distance and rotated
 * by the eye-line angle.
 */
data class FaceState(
    val cx: Float,
    val cy: Float,
    val w: Float,
    val h: Float,
    val imageAspect: Float,
    val leftEye: Offset? = null,
    val rightEye: Offset? = null,
    val nose: Offset? = null,
    val mouth: Offset? = null,
    val leftEar: Offset? = null,
    val rightEar: Offset? = null,
    val leftCheek: Offset? = null,
    val rightCheek: Offset? = null,
    val faceContour: List<Offset> = emptyList(),
    val leftEyeContour: List<Offset> = emptyList(),
    val rightEyeContour: List<Offset> = emptyList(),
    val upperLipTop: List<Offset> = emptyList(),
    val lowerLipBottom: List<Offset> = emptyList()
) {
    /** Midpoint between the eyes — the natural centre for glasses and masks. */
    val eyeMid: Offset?
        get() = if (leftEye != null && rightEye != null) {
            Offset((leftEye.x + rightEye.x) / 2f, (leftEye.y + rightEye.y) / 2f)
        } else null

    /**
     * Distance between the eyes, normalised by image width. Used as the scale
     * reference so a lens keeps its proportion as the face moves closer or further.
     */
    val interOcular: Float
        get() = if (leftEye != null && rightEye != null) {
            hypot((rightEye.x - leftEye.x).toDouble(), (rightEye.y - leftEye.y).toDouble()).toFloat()
        } else w * 0.5f

    /** In-plane tilt of the eye line, in degrees (positive = clockwise on screen). */
    val eyeAngle: Float
        get() = if (leftEye != null && rightEye != null) {
            Math.toDegrees(
                atan2((rightEye.y - leftEye.y).toDouble(), (rightEye.x - leftEye.x).toDouble())
            ).toFloat()
        } else 0f

    /** Fallback ellipse for the face oval when no contour is available. */
    fun fallbackOval(): List<Offset> {
        val rx = w / 2f
        val ry = h / 2f
        return (0 until 32).map { i ->
            val a = (i / 32.0) * 2.0 * Math.PI
            Offset(cx + (rx * Math.cos(a)).toFloat(), cy + (ry * Math.sin(a)).toFloat())
        }
    }

    /** The outline used for face masking: the real contour, else an ellipse. */
    fun outline(): List<Offset> =
        if (faceContour.size >= 6) faceContour else fallbackOval()
}
