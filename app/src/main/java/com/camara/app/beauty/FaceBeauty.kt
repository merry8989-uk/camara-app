package com.camara.app.beauty

import android.graphics.Bitmap
import com.camara.app.lenses.FaceState
import com.camara.app.util.MaskCompositor

/**
 * Applies retouch **to the face only**.
 *
 * The full beauty pipeline runs over the whole frame, then the result is composited back
 * through a feathered face mask — so smoothing and grading land on the face while hair,
 * clothing and background stay untouched.
 *
 * When no face is found, only the **colour grade** is applied, over the whole frame. The
 * spatial steps — smoothing, slimming, eye enlargement, sharpening — are skipped entirely.
 *
 * That distinction matters: the old behaviour fell back to the full result, which smeared
 * skin smoothing across the whole picture. "Beauty on the face only" turning into "beauty
 * everywhere" exactly when detection is least reliable is the opposite of what was asked
 * for. A colour grade is defensible full-frame; a blur is not.
 */
object FaceBeauty {

    fun apply(src: Bitmap, settings: BeautySettings, face: FaceState?): Bitmap {
        if (settings.isDefault) return src

        if (face == null) {
            return if (settings.hasGrade) {
                BeautyRenderer.applyGradeOnly(src, settings)
            } else {
                src
            }
        }

        val full = BeautyRenderer.apply(src, settings)
        val mask = FaceMasker.buildMask(src, face)
        val result = MaskCompositor.maskOver(src, full, mask)
        mask.recycle()
        // `full` was only an input to the compositor — release it unless the compositor
        // handed it straight back (in which case it is the result and must survive).
        if (result !== full) full.recycle()
        return result
    }
}
