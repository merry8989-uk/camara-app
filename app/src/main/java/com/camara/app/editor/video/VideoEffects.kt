package com.camara.app.editor.video

import androidx.media3.common.Effect
import androidx.media3.effect.Presentation
import androidx.media3.effect.RgbMatrix
import com.camara.app.filters.Filter

/**
 * Bridges a colour-matrix [Filter] into Media3 video effects.
 *
 * Media3's [RgbMatrix] wants a 4x4 matrix in column-major order, so we take the
 * linear (non-offset) part of our 4x5 matrix and transpose it.
 */
object VideoEffects {

    fun forFilter(filter: Filter, height: Int = 1080): List<Effect> {
        val effects = mutableListOf<Effect>()
        effects.add(Presentation.createForHeight(height))
        if (!filter.isIdentity) {
            val m = filter.matrix
            val matrix4x4 = floatArrayOf(
                m[0], m[5], m[10], 0f,
                m[1], m[6], m[11], 0f,
                m[2], m[7], m[12], 0f,
                0f, 0f, 0f, 1f
            )
            effects.add(RgbMatrix { _, _ -> matrix4x4 })
        }
        return effects
    }
}
