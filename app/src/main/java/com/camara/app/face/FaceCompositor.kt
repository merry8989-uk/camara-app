package com.camara.app.face

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.ColorMatrixColorFilter
import android.graphics.Paint
import android.graphics.Path
import com.camara.app.beauty.BeautySettings
import com.camara.app.filters.ColorMatrixMath
import com.camara.app.filters.Filter
import com.camara.app.lenses.FaceState
import com.camara.app.util.Blur
import com.camara.app.util.MaskCompositor

/**
 * Applies the look to the **face only**, for the live preview.
 *
 * The old preview path put a `RenderEffect` on the whole `PreviewView`, and a View-level
 * effect cannot be masked — so turning on skin smoothing blurred the entire picture while
 * the saved photo was correctly masked. That is fixed here by doing the work on pixels
 * instead of on the View: build a feathered face mask, apply the effect through it, and
 * hand back a layer that is transparent everywhere the face is not.
 *
 * Doing it in software also means it works on **every** supported API level, not just 31+.
 *
 * The result is drawn over the camera preview, so the untouched preview is always behind
 * it: if this returns null, or the face is lost, the preview simply shows through.
 */
object FaceCompositor {

    /**
     * @return a frame-sized ARGB bitmap whose only non-transparent pixels are the face,
     *   or null when there is nothing to apply.
     */
    fun faceLayer(
        frame: Bitmap,
        face: FaceState,
        filter: Filter,
        intensity: Float,
        beauty: BeautySettings,
    ): Bitmap? {
        val matrix = buildMatrix(filter, intensity, beauty)
        val smooth = beauty.smooth
        if (matrix == null && smooth <= 0.01f) return null

        val box = FaceGeometry.faceBox(face, frame.width, frame.height)
        if (box.isEmpty) return null

        val left = box.left.toInt()
        val top = box.top.toInt()
        val w = box.width.toInt().coerceAtLeast(2)
        val h = box.height.toInt().coerceAtLeast(2)
        if (left + w > frame.width || top + h > frame.height) return null

        val crop = Bitmap.createBitmap(frame, left, top, w, h)
        val own = if (crop === frame) frame.copy(Bitmap.Config.ARGB_8888, false) else crop

        val mask = buildMask(own, face, left, top, frame.width, frame.height)
        if (mask == null) {
            if (own !== frame) own.recycle()
            return null
        }

        var effected: Bitmap? = null
        try {
            var work = own
            if (matrix != null) {
                val graded = applyMatrix(work, matrix)
                work = graded
                effected = graded
            }
            if (smooth > 0.01f) {
                val blurred = Blur.blur(work, (smooth * 18f).toInt().coerceAtLeast(1))
                val mixed = blendBlurred(work, blurred, smooth)
                if (blurred !== work) blurred.recycle()
                // The graded copy has served its purpose. Without this it is leaked once
                // per frame, which at 8 fps is a few megabytes a second.
                if (effected != null && effected !== mixed) effected.recycle()
                work = mixed
                effected = mixed
            }
            if (work === own) return null

            val masked = MaskCompositor.maskOver(own, work, mask)
            val out = Bitmap.createBitmap(frame.width, frame.height, Bitmap.Config.ARGB_8888)
            Canvas(out).drawBitmap(masked, left.toFloat(), top.toFloat(), null)
            if (masked !== own && masked !== work) masked.recycle()
            return out
        } finally {
            mask.recycle()
            if (effected != null && effected !== own) effected.recycle()
            if (own !== frame) own.recycle()
        }
    }

    /** The colour part of the look, or null when there is no colour change at all. */
    private fun buildMatrix(filter: Filter, intensity: Float, beauty: BeautySettings): FloatArray? {
        val hasFilter = !filter.isIdentity && intensity > 0.01f
        val hasGrade = beauty.hasGrade
        if (!hasFilter && !hasGrade) return null
        return when {
            hasFilter && hasGrade -> ColorMatrixMath.compose(filter.matrixAt(intensity), beauty.gradeMatrix())
            hasFilter -> filter.matrixAt(intensity)
            else -> beauty.gradeMatrix()
        }
    }

    private fun applyMatrix(src: Bitmap, matrix: FloatArray): Bitmap {
        val out = Bitmap.createBitmap(src.width, src.height, Bitmap.Config.ARGB_8888)
        val paint = Paint(Paint.FILTER_BITMAP_FLAG or Paint.ANTI_ALIAS_FLAG)
        paint.colorFilter = ColorMatrixColorFilter(matrix)
        Canvas(out).drawBitmap(src, 0f, 0f, paint)
        return out
    }

    /** Blends the smoothed copy in at [amount], leaving the rest of the detail intact. */
    private fun blendBlurred(sharp: Bitmap, blurred: Bitmap, amount: Float): Bitmap {
        val out = Bitmap.createBitmap(sharp.width, sharp.height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(out)
        canvas.drawBitmap(sharp, 0f, 0f, null)
        val paint = Paint(Paint.FILTER_BITMAP_FLAG)
        paint.alpha = (amount.coerceIn(0f, 1f) * 255f * 0.75f).toInt().coerceIn(0, 255)
        canvas.drawBitmap(blurred, 0f, 0f, paint)
        return out
    }

    /**
     * A soft-edged mask for the crop: white over the face, feathered out to nothing.
     * Coordinates are converted from the normalised frame space into crop space.
     */
    private fun buildMask(
        crop: Bitmap,
        face: FaceState,
        left: Int,
        top: Int,
        frameWidth: Int,
        frameHeight: Int,
    ): Bitmap? {
        val mask = Bitmap.createBitmap(crop.width, crop.height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(mask)
        val path = Path()
        val pts = FaceGeometry.outlinePixels(face, frameWidth, frameHeight)
        if (pts.size < 3) {
            mask.recycle()
            return null
        }
        pts.forEachIndexed { i, (x, y) ->
            val px = x - left
            val py = y - top
            if (i == 0) path.moveTo(px, py) else path.lineTo(px, py)
        }
        path.close()
        val fill = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.WHITE }
        canvas.drawPath(path, fill)

        // Feather roughly in proportion to the face size, so the fade looks the same
        // whether the person is close to the lens or across the room.
        val feather = (minOf(crop.width, crop.height) * 0.06f).toInt().coerceIn(2, 40)
        val soft = Blur.featherMask(mask, feather)
        if (soft !== mask) mask.recycle()
        return soft
    }
}
