package com.camara.app.util

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.PorterDuff
import android.graphics.PorterDuffXfermode

/** Composites one bitmap over another through an alpha mask. */
object MaskCompositor {

    /**
     * Draws [overlay] on top of [base], but only where [mask] has alpha — with the
     * mask's soft edges producing a gradual blend. This is how face-only beauty and
     * background bokeh both work.
     */
    fun maskOver(base: Bitmap, overlay: Bitmap, mask: Bitmap): Bitmap {
        val out = base.copy(Bitmap.Config.ARGB_8888, true)
        val layer = Bitmap.createBitmap(base.width, base.height, Bitmap.Config.ARGB_8888)

        val layerCanvas = Canvas(layer)
        layerCanvas.drawBitmap(overlay, 0f, 0f, null)
        val maskPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            xfermode = PorterDuffXfermode(PorterDuff.Mode.DST_IN)
        }
        layerCanvas.drawBitmap(mask, 0f, 0f, maskPaint)

        Canvas(out).drawBitmap(layer, 0f, 0f, null)
        layer.recycle()
        return out
    }
}
