package com.camara.app.face

import androidx.compose.ui.geometry.Offset
import com.camara.app.lenses.FaceState
import kotlin.math.abs

/**
 * The live face-tracking engine.
 *
 * ML Kit hands back a raw detection per frame, and raw detections are not usable for
 * anything that has to sit still on a face:
 *
 *  - the pose **jitters** by a few pixels frame to frame, which makes a sticker shake;
 *  - it **drops out** for a frame or two on a blink, a head turn or a hand passing over,
 *    which makes a sticker blink out of existence and back;
 *  - the contours are noisy at the edges, so a lipstick or lash piece crawls.
 *
 * This smooths the pose over time, holds it briefly through dropouts, and snaps instead of
 * gliding when the face genuinely jumps somewhere else (a new person, or the camera
 * cutting). Everything downstream renders the pose this returns, so the improvement lands
 * in one place.
 *
 * Deliberately pure — no Android graphics, no clock of its own — so the behaviour can be
 * tested frame by frame.
 */
class FaceTracker(
    /** 0 = never move, 1 = use the raw detection. Lower is steadier but laggier. */
    private val smoothing: Float = 0.4f,
    /** How long a lost face keeps its last pose before it is dropped. */
    private val holdMs: Long = 300L,
    /** A jump bigger than this fraction of the face width is treated as a new face. */
    private val snapDistance: Float = 0.35f,
) {
    private var smoothed: FaceState? = null
    private var lastSeenAt = 0L

    /** The pose to render right now, or null when there is no face to draw on. */
    val current: FaceState? get() = smoothed

    /** True while the pose is being held through a dropout rather than freshly detected. */
    var holding: Boolean = false
        private set

    /**
     * Feed one frame. [detection] is null when ML Kit found nothing this frame.
     *
     * @return the pose to render, or null if the face is gone for good.
     */
    fun update(detection: FaceState?, nowMs: Long): FaceState? {
        if (detection == null) {
            val previous = smoothed
            if (previous != null && nowMs - lastSeenAt <= holdMs) {
                // Hold the last good pose rather than making the sticker flicker.
                holding = true
                return previous
            }
            smoothed = null
            holding = false
            return null
        }

        val previous = smoothed
        val next = when {
            previous == null -> detection
            // A jump means a different face (or a cut); gliding across the gap would drag
            // the sticker over the whole screen on the way.
            isJump(previous, detection) -> detection
            else -> blend(previous, detection, smoothing.coerceIn(0f, 1f))
        }

        smoothed = next
        lastSeenAt = nowMs
        holding = false
        return next
    }

    fun reset() {
        smoothed = null
        lastSeenAt = 0L
        holding = false
    }

    private fun isJump(a: FaceState, b: FaceState): Boolean {
        val moved = hypotApprox(b.cx - a.cx, b.cy - a.cy)
        val scale = maxOf(a.w, 0.01f)
        return moved > scale * snapDistance
    }

    private fun hypotApprox(dx: Float, dy: Float): Float =
        kotlin.math.sqrt(dx * dx + dy * dy)

    companion object {

        /** Linear blend of two poses. Exposed for testing. */
        internal fun blend(from: FaceState, to: FaceState, t: Float): FaceState {
            val k = t.coerceIn(0f, 1f)
            fun f(a: Float, b: Float) = a + (b - a) * k
            fun p(a: Offset?, b: Offset?): Offset? = when {
                a == null -> b
                b == null -> a
                else -> Offset(f(a.x, b.x), f(a.y, b.y))
            }

            return to.copy(
                cx = f(from.cx, to.cx),
                cy = f(from.cy, to.cy),
                w = f(from.w, to.w),
                h = f(from.h, to.h),
                imageAspect = to.imageAspect,
                leftEye = p(from.leftEye, to.leftEye),
                rightEye = p(from.rightEye, to.rightEye),
                nose = p(from.nose, to.nose),
                mouth = p(from.mouth, to.mouth),
                leftEar = p(from.leftEar, to.leftEar),
                rightEar = p(from.rightEar, to.rightEar),
                leftCheek = p(from.leftCheek, to.leftCheek),
                rightCheek = p(from.rightCheek, to.rightCheek),
                faceContour = blendPoints(from.faceContour, to.faceContour, k),
                leftEyeContour = blendPoints(from.leftEyeContour, to.leftEyeContour, k),
                rightEyeContour = blendPoints(from.rightEyeContour, to.rightEyeContour, k),
                upperLipTop = blendPoints(from.upperLipTop, to.upperLipTop, k),
                lowerLipBottom = blendPoints(from.lowerLipBottom, to.lowerLipBottom, k)
            )
        }

        /**
         * Contour point counts are stable in practice, but not guaranteed. A mismatch
         * takes the new contour whole rather than blending two different shapes.
         */
        private fun blendPoints(a: List<Offset>, b: List<Offset>, k: Float): List<Offset> {
            if (a.isEmpty()) return b
            if (b.isEmpty()) return a
            if (a.size != b.size) return b
            return List(b.size) { i ->
                val pa = a[i]
                val pb = b[i]
                Offset(pa.x + (pb.x - pa.x) * k, pa.y + (pb.y - pa.y) * k)
            }
        }

        /** Distance in normalised units, used by the jump test. */
        internal fun distance(a: FaceState, b: FaceState): Float =
            kotlin.math.sqrt(
                (b.cx - a.cx) * (b.cx - a.cx) + (b.cy - a.cy) * (b.cy - a.cy)
            )
    }
}
