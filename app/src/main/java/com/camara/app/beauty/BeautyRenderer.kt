package com.camara.app.beauty

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.ColorMatrixColorFilter
import android.graphics.Paint
import com.camara.app.filters.ColorMatrixMath
import kotlin.math.exp
import kotlin.math.max
import kotlin.math.min

/**
 * Applies [BeautySettings] to a bitmap.
 *
 * Pipeline: colour grade -> skin smoothing (blur blend) -> face slimming ->
 * eye enlargement -> optional sharpen. The warp steps use [Canvas.drawBitmapMesh],
 * a real vertex-mesh warp rather than a crop or scale, so the result keeps its
 * framing.
 */
object BeautyRenderer {

    fun apply(src: Bitmap, settings: BeautySettings): Bitmap {
        if (settings.isDefault) return src

        var bmp = src

        // 1. Colour grade (brightness / warmth / skin tone).
        val grade = ColorMatrixMath.chain(
            ColorMatrixMath.saturation(1f + settings.skinTone * 0.08f),
            ColorMatrixMath.brightness(settings.brighten * 0.25f),
            ColorMatrixMath.temperature(settings.warmth)
        )
        if (settings.brighten != 0f || settings.warmth != 0f || settings.skinTone != 0f) {
            bmp = applyMatrix(bmp, grade)
        }

        // 2. Skin smoothing: blend the image with a blurred copy.
        if (settings.smooth > 0f) {
            bmp = smooth(bmp, settings.smooth)
        }

        // 3. Face slimming (horizontal centre pull).
        if (settings.slimFace > 0f) {
            bmp = warp(bmp, slim = settings.slimFace, eyes = 0f)
        }

        // 4. Eye enlargement (local magnification).
        if (settings.enlargeEyes > 0f) {
            bmp = warp(bmp, slim = 0f, eyes = settings.enlargeEyes)
        }

        // 5. Sharpen (unsharp-style detail recovery).
        if (settings.sharpen > 0f) {
            bmp = sharpen(bmp, settings.sharpen)
        }

        return bmp
    }

    private fun applyMatrix(src: Bitmap, matrix: FloatArray): Bitmap {
        val out = Bitmap.createBitmap(src.width, src.height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(out)
        val paint = Paint(Paint.FILTER_BITMAP_FLAG or Paint.ANTI_ALIAS_FLAG)
        paint.colorFilter = ColorMatrixColorFilter(matrix)
        canvas.drawBitmap(src, 0f, 0f, paint)
        return out
    }

    /** Downscale-upscale blur, then alpha-blend with the original. Fast and soft. */
    private fun smooth(src: Bitmap, amount: Float): Bitmap {
        val factor = (1f - 0.85f * amount).coerceIn(0.15f, 1f)
        val sw = max(1, (src.width * factor).toInt())
        val sh = max(1, (src.height * factor).toInt())
        val small = Bitmap.createScaledBitmap(src, sw, sh, true)
        val blurred = Bitmap.createScaledBitmap(small, src.width, src.height, true)
        if (small !== src) small.recycle()

        val out = Bitmap.createBitmap(src.width, src.height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(out)
        canvas.drawBitmap(src, 0f, 0f, null)
        val paint = Paint(Paint.FILTER_BITMAP_FLAG)
        paint.alpha = (amount * 255f * 0.7f).toInt().coerceIn(0, 255)
        canvas.drawBitmap(blurred, 0f, 0f, paint)
        if (blurred !== src) blurred.recycle()
        return out
    }

    /**
     * Vertex-mesh warp. [slim] squeezes horizontally around the centre; [eyes]
     * magnifies two eye regions.
     */
    private fun warp(src: Bitmap, slim: Float, eyes: Float): Bitmap {
        val cols = 24
        val rows = 24
        val w = src.width.toFloat()
        val h = src.height.toFloat()
        val cx = w / 2f
        val cy = h / 2f

        val verts = FloatArray((cols + 1) * (rows + 1) * 2)
        var idx = 0
        for (r in 0..rows) {
            for (c in 0..cols) {
                var x = c * w / cols
                val y = r * h / rows

                if (slim > 0f) {
                    val dx = x - cx
                    val fy = exp(-((y - cy) / (h * 0.45f)).let { it * it })
                    val fx = exp(-((dx) / (w * 0.40f)).let { it * it })
                    x = cx + dx * (1f - slim * 0.22f * fx * fy)
                }

                if (eyes > 0f) {
                    x = magnify(x, y, w * 0.34f, h * 0.42f, w * 0.14f, eyes)
                    x = magnify(x, y, w * 0.66f, h * 0.42f, w * 0.14f, eyes)
                }

                verts[idx++] = x
                verts[idx++] = y
            }
        }

        val out = Bitmap.createBitmap(src.width, src.height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(out)
        val paint = Paint(Paint.FILTER_BITMAP_FLAG or Paint.ANTI_ALIAS_FLAG)
        canvas.drawBitmapMesh(src, cols, rows, verts, 0, null, 0, paint)
        return out
    }

    private fun magnify(x: Float, y: Float, ex: Float, ey: Float, radius: Float, strength: Float): Float {
        val dx = x - ex
        val dy = y - ey
        val dist = kotlin.math.sqrt(dx * dx + dy * dy)
        if (dist >= radius || dist == 0f) return x
        val t = 1f - dist / radius
        val scale = 1f + strength * 0.35f * t * t
        return ex + dx * scale
    }

    /** Lightweight unsharp mask on a downscaled proxy for speed. */
    private fun sharpen(src: Bitmap, amount: Float): Bitmap {
        val w = src.width
        val h = src.height
        if (w < 3 || h < 3) return src

        // Build a blurred proxy at reduced resolution, then upscale to compare.
        val f = 0.5f
        val sw = max(1, (w * f).toInt())
        val sh = max(1, (h * f).toInt())
        val small = Bitmap.createScaledBitmap(src, sw, sh, true)
        val blurSmall = boxBlur(small, 1)
        val blur = Bitmap.createScaledBitmap(blurSmall, w, h, true)
        if (small !== src) small.recycle()
        if (blurSmall !== small) blurSmall.recycle()

        val srcPx = IntArray(w * h)
        val blurPx = IntArray(w * h)
        src.getPixels(srcPx, 0, w, 0, 0, w, h)
        blur.getPixels(blurPx, 0, w, 0, 0, w, h)
        blur.recycle()

        val k = amount * 1.4f
        val outPx = IntArray(w * h)
        for (i in srcPx.indices) {
            val s = srcPx[i]
            val b = blurPx[i]
            val sr = (s shr 16) and 0xFF
            val sg = (s shr 8) and 0xFF
            val sb = s and 0xFF
            val br = (b shr 16) and 0xFF
            val bg = (b shr 8) and 0xFF
            val bb = b and 0xFF
            val r = clamp(sr + ((sr - br) * k).toInt())
            val g = clamp(sg + ((sg - bg) * k).toInt())
            val bl = clamp(sb + ((sb - bb) * k).toInt())
            outPx[i] = (s and 0xFF000000.toInt()) or (r shl 16) or (g shl 8) or bl
        }
        return Bitmap.createBitmap(outPx, w, h, Bitmap.Config.ARGB_8888)
    }

    private fun boxBlur(src: Bitmap, radius: Int): Bitmap {
        val w = src.width
        val h = src.height
        val px = IntArray(w * h)
        src.getPixels(px, 0, w, 0, 0, w, h)
        val out = IntArray(w * h)
        for (y in 0 until h) {
            for (x in 0 until w) {
                var r = 0; var g = 0; var b = 0; var n = 0
                for (dy in -radius..radius) {
                    val yy = (y + dy).coerceIn(0, h - 1)
                    for (dx in -radius..radius) {
                        val xx = (x + dx).coerceIn(0, w - 1)
                        val p = px[yy * w + xx]
                        r += (p shr 16) and 0xFF
                        g += (p shr 8) and 0xFF
                        b += p and 0xFF
                        n++
                    }
                }
                out[y * w + x] = (px[y * w + x] and 0xFF000000.toInt()) or
                    ((r / n) shl 16) or ((g / n) shl 8) or (b / n)
            }
        }
        return Bitmap.createBitmap(out, w, h, Bitmap.Config.ARGB_8888)
    }

    private fun clamp(v: Int): Int = min(255, max(0, v))
}
