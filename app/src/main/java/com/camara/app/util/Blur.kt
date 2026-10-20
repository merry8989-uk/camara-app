package com.camara.app.util

import android.graphics.Bitmap
import kotlin.math.max
import kotlin.math.min

/** Shared blur helpers used by beauty smoothing and portrait bokeh. */
object Blur {

    /**
     * Downscale → box blur → upscale. Much cheaper than a full-resolution
     * convolution and visually indistinguishable for a soft blur.
     */
    fun blur(src: Bitmap, radius: Int): Bitmap {
        if (radius <= 0) return src
        val factor = when {
            radius > 18 -> 0.12f
            radius > 10 -> 0.18f
            radius > 5 -> 0.28f
            else -> 0.5f
        }
        val sw = max(1, (src.width * factor).toInt())
        val sh = max(1, (src.height * factor).toInt())
        val small = Bitmap.createScaledBitmap(src, sw, sh, true)
        val boxed = boxBlur(small, max(1, radius / 4))
        val out = Bitmap.createScaledBitmap(boxed, src.width, src.height, true)
        if (small !== src) small.recycle()
        if (boxed !== small) boxed.recycle()
        return out
    }

    fun boxBlur(src: Bitmap, radius: Int): Bitmap {
        val w = src.width
        val h = src.height
        if (radius <= 0 || w < 2 || h < 2) return src
        val px = IntArray(w * h)
        src.getPixels(px, 0, w, 0, 0, w, h)
        val out = IntArray(w * h)
        for (y in 0 until h) {
            for (x in 0 until w) {
                var r = 0; var g = 0; var b = 0; var a = 0; var n = 0
                for (dy in -radius..radius) {
                    val yy = (y + dy).coerceIn(0, h - 1)
                    val row = yy * w
                    for (dx in -radius..radius) {
                        val xx = (x + dx).coerceIn(0, w - 1)
                        val p = px[row + xx]
                        a += (p ushr 24) and 0xFF
                        r += (p shr 16) and 0xFF
                        g += (p shr 8) and 0xFF
                        b += p and 0xFF
                        n++
                    }
                }
                out[y * w + x] = ((a / n) shl 24) or ((r / n) shl 16) or ((g / n) shl 8) or (b / n)
            }
        }
        return Bitmap.createBitmap(out, w, h, Bitmap.Config.ARGB_8888)
    }

    /** Softens the edge of a white-on-transparent alpha mask. */
    fun featherMask(mask: Bitmap, radius: Int): Bitmap = blur(mask, radius)

    fun clamp(v: Int): Int = min(255, max(0, v))
}
