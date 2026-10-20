package com.camara.app.portrait

import android.graphics.Bitmap
import com.camara.app.util.Blur
import com.camara.app.util.MaskCompositor
import kotlin.math.min

/**
 * Real portrait depth: the background is blurred and the subject stays sharp, with
 * the soft mask producing a natural transition between the two.
 */
object PortraitBokeh {

    fun apply(src: Bitmap, subjectMask: Bitmap?, strength: Float): Bitmap {
        if (subjectMask == null || strength <= 0.01f) return src

        val minDim = min(src.width, src.height)
        val radius = (minDim * 0.035f * strength).toInt().coerceIn(2, 90)
        val blurred = Blur.blur(src, radius)

        val out = MaskCompositor.maskOver(blurred, src, subjectMask)
        if (blurred !== src) blurred.recycle()
        return out
    }
}
