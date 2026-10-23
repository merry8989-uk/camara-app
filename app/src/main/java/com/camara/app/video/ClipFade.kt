package com.camara.app.video

import kotlin.math.min

/**
 * Fade-to-black at the joins, and at the very start and end.
 *
 * This is a **transition you can actually ship**: Media3's `RgbMatrix` is evaluated per
 * frame with that frame's timestamp, so multiplying the colour by a factor that ramps
 * over time gives a real fade with no custom GL shader. A crossfade (both clips visible
 * at once) would need overlapping sequences and a time-varying *alpha*, which Media3 only
 * exposes through a hand-written shader — deliberately not attempted.
 *
 * The maths is pure and lives here so it can be tested: everything downstream just asks
 * [factorAt] for a timestamp.
 */
class ClipFade(private val spans: List<ClipSpan>, private val fadeMs: Float) {

    /** 1 = untouched, 0 = fully black. */
    fun factorAt(timeMs: Float): Float {
        if (fadeMs <= 0f || spans.isEmpty()) return 1f
        val span = spans.firstOrNull { timeMs >= it.startMs && timeMs <= it.endMs } ?: return 1f
        // A clip shorter than two fades just gets the smaller of the two ramps, so it
        // dips toward black and back rather than going negative or wrapping.
        val fromStart = (timeMs - span.startMs) / fadeMs
        val fromEnd = (span.endMs - timeMs) / fadeMs
        return min(fromStart, fromEnd).coerceIn(0f, 1f)
    }

    /** The RGB matrix that darkens a frame by [factor]. Alpha is left alone. */
    fun matrixAt(timeMs: Float): FloatArray {
        val f = factorAt(timeMs)
        return floatArrayOf(
            f, 0f, 0f, 0f,
            0f, f, 0f, 0f,
            0f, 0f, f, 0f,
            0f, 0f, 0f, 1f
        )
    }

    companion object {
        /** Fade lengths offered in the editor, in milliseconds. 0 means no transition. */
        val CHOICES = listOf(0f, 300f, 600f, 1000f)

        fun label(fadeMs: Float): String = when (fadeMs) {
            0f -> "Off"
            else -> "${(fadeMs / 100f).toInt() / 10f}s"
        }
    }
}
