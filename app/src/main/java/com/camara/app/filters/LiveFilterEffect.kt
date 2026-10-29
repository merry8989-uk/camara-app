package com.camara.app.filters

import android.graphics.ColorMatrixColorFilter
import android.graphics.RenderEffect
import android.os.Build
import android.view.View
import androidx.annotation.RequiresApi
import com.camara.app.beauty.BeautySettings

/**
 * The live **colour** look on the camera preview.
 *
 * This used to also chain a blur for skin smoothing, applied to the whole `PreviewView`.
 * That was wrong twice over: a View-level `RenderEffect` cannot be masked, so turning on
 * smoothing blurred the entire picture; and it only worked on API 31+, so older devices
 * got no live look at all.
 *
 * Skin smoothing now belongs to [com.camara.app.face.FaceCompositor], which works on
 * pixels, applies the effect through a feathered face mask, and runs on every supported
 * API level. What is left here is the part that genuinely is a whole-frame colour
 * transform, which is cheap and correct to do on the View.
 *
 * IMPORTANT: this only works when the preview is a TextureView, i.e. the
 * [androidx.camera.view.PreviewView] must be created with
 * `ImplementationMode.COMPATIBLE`. A SurfaceView (`PERFORMANCE`, the default) is
 * composited as a separate layer and ignores RenderEffect entirely.
 */
object LiveFilterEffect {

    fun applyColour(view: View, filter: Filter, intensity: Float, beauty: BeautySettings) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            applyColourApi31(view, filter, intensity, beauty)
        }
    }

    @RequiresApi(Build.VERSION_CODES.S)
    private fun applyColourApi31(view: View, filter: Filter, intensity: Float, beauty: BeautySettings) {
        val hasFilter = !filter.isIdentity && intensity > 0.01f
        if (!hasFilter && !beauty.hasGrade) {
            view.setRenderEffect(null)
            return
        }

        val matrix = when {
            hasFilter && beauty.hasGrade ->
                ColorMatrixMath.compose(filter.matrixAt(intensity), beauty.gradeMatrix())
            hasFilter -> filter.matrixAt(intensity)
            else -> beauty.gradeMatrix()
        }
        view.setRenderEffect(RenderEffect.createColorFilterEffect(ColorMatrixColorFilter(matrix)))
    }
}
