package com.camara.app.filters

import android.graphics.ColorMatrixColorFilter
import android.graphics.RenderEffect
import android.os.Build
import android.view.View
import androidx.annotation.RequiresApi

/**
 * Applies a live filter to a preview surface.
 *
 * On Android 12+ (API 31) we use [RenderEffect] with a colour-matrix filter, which
 * runs on the GPU with no extra buffers — so the preview shows the filtered look at
 * full frame rate. Below API 31 the preview is unfiltered (the filter is still baked
 * into the captured photo, so nothing is lost).
 */
object LiveFilterEffect {

    fun apply(view: View, filter: Filter?) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            applyApi31(view, filter)
        }
    }

    @RequiresApi(Build.VERSION_CODES.S)
    private fun applyApi31(view: View, filter: Filter?) {
        if (filter == null || filter.isIdentity) {
            view.setRenderEffect(null)
        } else {
            val colorFilter = ColorMatrixColorFilter(filter.matrix)
            view.setRenderEffect(RenderEffect.createColorFilterEffect(colorFilter))
        }
    }
}
