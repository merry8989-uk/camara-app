package com.camara.app.beauty

import android.graphics.Bitmap
import com.camara.app.lenses.FaceState
import com.camara.app.util.MaskCompositor

/**
 * Applies retouch **to the face only**.
 *
 * The full beauty pipeline runs over the whole frame, then the result is composited
 * back through a feathered face mask — so smoothing and grading land on the face
 * while hair, clothing and background stay untouched. When no face is found the
 * caller can fall back to the full-frame result.
 */
object FaceBeauty {

    fun apply(src: Bitmap, settings: BeautySettings, face: FaceState?): Bitmap {
        if (settings.isDefault) return src
        val full = BeautyRenderer.apply(src, settings)
        if (face == null) return full
        val mask = FaceMasker.buildMask(src, face)
        val result = MaskCompositor.maskOver(src, full, mask)
        mask.recycle()
        return result
    }
}
