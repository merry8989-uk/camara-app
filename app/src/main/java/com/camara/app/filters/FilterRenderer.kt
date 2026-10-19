package com.camara.app.filters

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.ColorMatrixColorFilter
import android.graphics.Paint

/** Renders a [Filter] onto a still [Bitmap] using the platform colour pipeline. */
object FilterRenderer {

    fun apply(src: Bitmap, filter: Filter): Bitmap {
        if (filter.isIdentity) return src
        val out = Bitmap.createBitmap(src.width, src.height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(out)
        val paint = Paint(Paint.FILTER_BITMAP_FLAG or Paint.ANTI_ALIAS_FLAG)
        paint.colorFilter = ColorMatrixColorFilter(filter.matrix)
        canvas.drawBitmap(src, 0f, 0f, paint)
        return out
    }

    /** Convenience: apply to an in-place canvas paint, used by the editor preview. */
    fun paintFor(filter: Filter?): Paint {
        val paint = Paint(Paint.FILTER_BITMAP_FLAG or Paint.ANTI_ALIAS_FLAG)
        if (filter != null && !filter.isIdentity) {
            paint.colorFilter = ColorMatrixColorFilter(filter.matrix)
        }
        return paint
    }
}
