package com.camara.app.face

import com.camara.app.lenses.FaceState
import kotlin.math.max
import kotlin.math.min

/** A rectangle in frame pixels. Deliberately not android.graphics.Rect: this is pure. */
data class FaceBox(
    val left: Float,
    val top: Float,
    val right: Float,
    val bottom: Float,
) {
    val width: Float get() = (right - left).coerceAtLeast(0f)
    val height: Float get() = (bottom - top).coerceAtLeast(0f)
    val isEmpty: Boolean get() = width < 2f || height < 2f
}

/**
 * Where the face is, in frame pixels — the geometry the live compositor needs.
 *
 * Kept pure and separate from the drawing so it can be tested: an off-by-one here shows
 * up as the face effect sliding off the face.
 */
object FaceGeometry {

    /**
     * The face box, grown by [expand] so the feather has room to fade inside the crop
     * instead of being cut off at its edge.
     */
    fun faceBox(
        face: FaceState,
        frameWidth: Int,
        frameHeight: Int,
        expand: Float = 0.28f,
    ): FaceBox {
        if (frameWidth <= 0 || frameHeight <= 0) return FaceBox(0f, 0f, 0f, 0f)
        val cx = face.cx * frameWidth
        val cy = face.cy * frameHeight
        val halfW = face.w * frameWidth * (0.5f + expand)
        val halfH = face.h * frameHeight * (0.5f + expand)
        return FaceBox(
            left = max(0f, cx - halfW),
            top = max(0f, cy - halfH),
            right = min(frameWidth.toFloat(), cx + halfW),
            bottom = min(frameHeight.toFloat(), cy + halfH),
        )
    }

    /** Face outline in frame pixels. */
    fun outlinePixels(face: FaceState, frameWidth: Int, frameHeight: Int): List<Pair<Float, Float>> =
        face.outline().map { (it.x * frameWidth) to (it.y * frameHeight) }

    /**
     * A coarse key for the mask cache. Sub-pixel movement does not change the mask enough
     * to be worth rebuilding, and rebuilding it every frame is the expensive part.
     */
    fun maskKey(face: FaceState, frameWidth: Int, frameHeight: Int): Long {
        fun q(v: Float) = (v * 200f).toLong()
        return q(face.cx) * 31 + q(face.cy) * 17 + q(face.w) * 7 + q(face.h) + frameWidth * 3 + frameHeight * 5
    }
}
