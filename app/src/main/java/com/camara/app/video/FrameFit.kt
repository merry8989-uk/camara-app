package com.camara.app.video

/**
 * Where a video actually sits inside a container.
 *
 * The preview is letterboxed — a 9:16 clip in a wider box leaves bars at the sides — and
 * overlays are positioned as fractions of the *frame*, not of the container. Getting this
 * wrong puts the text in the bar instead of on the picture, so the maths lives here on its
 * own and is unit tested.
 */
object FrameFit {

    /** Left, top, right, bottom of the video inside a [boxW] x [boxH] container. */
    fun rect(boxW: Float, boxH: Float, videoW: Float, videoH: Float): FloatArray {
        if (boxW <= 0f || boxH <= 0f || videoW <= 0f || videoH <= 0f) {
            val w = boxW.coerceAtLeast(0f)
            val h = boxH.coerceAtLeast(0f)
            return floatArrayOf(0f, 0f, w, h)
        }
        val videoAspect = videoW / videoH
        val boxAspect = boxW / boxH
        return if (videoAspect > boxAspect) {
            // Wider than the box: full width, bars top and bottom.
            val h = boxW / videoAspect
            val top = (boxH - h) / 2f
            floatArrayOf(0f, top, boxW, top + h)
        } else {
            // Taller than the box: full height, bars left and right.
            val w = boxH * videoAspect
            val left = (boxW - w) / 2f
            floatArrayOf(left, 0f, left + w, boxH)
        }
    }
}
