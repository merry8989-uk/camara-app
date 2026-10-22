package com.camara.app.util

import android.graphics.Bitmap
import kotlin.math.max

/**
 * Blur helpers used by beauty smoothing and portrait bokeh.
 *
 * The box blur is **separable**: a horizontal pass followed by a vertical pass, each
 * using a sliding window sum. That makes it O(w·h) regardless of radius, instead of the
 * O(w·h·r²) of a naive 2D convolution — at radius 20 on a 1024² image that is roughly a
 * 40× reduction. The pixel maths lives in [boxBlurPixels] so it can be unit tested
 * without an Android Bitmap.
 */
object Blur {

    /** Downscale → box blur → upscale. Cheap, and visually indistinguishable from a full-res blur. */
    fun blur(src: Bitmap, radius: Int): Bitmap {
        if (radius <= 0) return src

        val factor = when {
            radius > 24 -> 0.25f
            radius > 10 -> 0.40f
            else -> 0.60f
        }
        val sw = max(1, (src.width * factor).toInt())
        val sh = max(1, (src.height * factor).toInt())

        val small = Bitmap.createScaledBitmap(src, sw, sh, true)
        val boxed = boxBlur(small, max(1, (radius * factor).toInt()))
        val out = Bitmap.createScaledBitmap(boxed, src.width, src.height, true)

        if (small !== src) small.recycle()
        if (boxed !== small) boxed.recycle()
        return out
    }

    fun boxBlur(src: Bitmap, radius: Int): Bitmap {
        if (radius <= 0 || src.width < 2 || src.height < 2) return src
        val w = src.width
        val h = src.height
        val px = IntArray(w * h)
        src.getPixels(px, 0, w, 0, 0, w, h)
        return Bitmap.createBitmap(boxBlurPixels(px, w, h, radius), w, h, Bitmap.Config.ARGB_8888)
    }

    /**
     * Separable box blur over packed ARGB pixels. Edges are handled by clamping, so
     * border pixels are not darkened — a constant image comes back unchanged.
     */
    fun boxBlurPixels(px: IntArray, w: Int, h: Int, radius: Int): IntArray {
        if (radius <= 0 || w <= 0 || h <= 0) return px.copyOf()

        val horizontal = IntArray(w * h)
        val out = IntArray(w * h)
        pass(px, horizontal, w, h, radius, horizontal = true)
        pass(horizontal, out, w, h, radius, horizontal = false)
        return out
    }

    private fun pass(src: IntArray, dst: IntArray, w: Int, h: Int, radius: Int, horizontal: Boolean) {
        val n = 2 * radius + 1
        val outer = if (horizontal) h else w
        val inner = if (horizontal) w else h
        val last = inner - 1

        for (o in 0 until outer) {
            var a = 0
            var r = 0
            var g = 0
            var b = 0

            // Prime the window centred on the first pixel (edges clamp to the border).
            for (i in -radius..radius) {
                val p = src[index(o, i.coerceIn(0, last), w, horizontal)]
                a += (p ushr 24) and 0xFF
                r += (p shr 16) and 0xFF
                g += (p shr 8) and 0xFF
                b += p and 0xFF
            }

            for (i in 0..last) {
                dst[index(o, i, w, horizontal)] =
                    ((a / n) shl 24) or ((r / n) shl 16) or ((g / n) shl 8) or (b / n)

                // Slide the window one pixel along.
                val leaving = src[index(o, (i - radius).coerceIn(0, last), w, horizontal)]
                a -= (leaving ushr 24) and 0xFF
                r -= (leaving shr 16) and 0xFF
                g -= (leaving shr 8) and 0xFF
                b -= leaving and 0xFF

                val entering = src[index(o, (i + radius + 1).coerceIn(0, last), w, horizontal)]
                a += (entering ushr 24) and 0xFF
                r += (entering shr 16) and 0xFF
                g += (entering shr 8) and 0xFF
                b += entering and 0xFF
            }
        }
    }

    private fun index(o: Int, i: Int, w: Int, horizontal: Boolean): Int =
        if (horizontal) o * w + i else i * w + o

    /** Softens the edge of a white-on-transparent alpha mask. */
    fun featherMask(mask: Bitmap, radius: Int): Bitmap = blur(mask, radius)
}
