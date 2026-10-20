package com.camara.app.filters

import android.graphics.ColorMatrixColorFilter
import android.graphics.RenderEffect
import android.graphics.Shader
import android.os.Build
import android.view.View
import androidx.annotation.RequiresApi
import com.camara.app.beauty.BeautySettings

/**
 * Applies a live look to the camera preview.
 *
 * This is the piece that makes filters and beauty *visible while you shoot*. It
 * combines the selected colour-matrix filter with the colour part of the beauty
 * grade into one colour filter, and optionally chains a blur for skin smoothing,
 * then applies the result to the preview view as a [RenderEffect].
 *
 * IMPORTANT: this only works when the preview is a TextureView, i.e. the
 * [androidx.camera.view.PreviewView] must be created with
 * `ImplementationMode.COMPATIBLE`. A SurfaceView (`PERFORMANCE`, the default) is
 * composited as a separate layer and ignores RenderEffect entirely — which is why
 * live filters previously appeared to do nothing.
 *
 * The spatial beauty steps (face slimming, eye enlargement, detail sharpen) cannot
 * be expressed as a RenderEffect and are baked into the captured photo instead.
 */
object LiveFilterEffect {

    fun apply(view: View, filter: Filter, intensity: Float, beauty: BeautySettings) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            applyApi31(view, filter, intensity, beauty)
        }
    }

    @RequiresApi(Build.VERSION_CODES.S)
    private fun applyApi31(view: View, filter: Filter, intensity: Float, beauty: BeautySettings) {
        val hasColour = (!filter.isIdentity && intensity > 0.01f) || beauty.hasGrade
        val hasBlur = beauty.smooth > 0.01f

        if (!hasColour && !hasBlur) {
            view.setRenderEffect(null)
            return
        }

        var effect: RenderEffect? = null

        if (hasColour) {
            val matrix = ColorMatrixMath.compose(filter.matrixAt(intensity), beauty.gradeMatrix())
            effect = RenderEffect.createColorFilterEffect(ColorMatrixColorFilter(matrix))
        }

        if (hasBlur) {
            val radius = (beauty.smooth * 8f).coerceIn(0.5f, 12f)
            val blur = RenderEffect.createBlurEffect(radius, radius, Shader.TileMode.CLAMP)
            effect = if (effect != null) {
                // Inner applied first: blur, then colour grade.
                RenderEffect.createChainEffect(effect, blur)
            } else {
                blur
            }
        }

        view.setRenderEffect(effect)
    }
}
