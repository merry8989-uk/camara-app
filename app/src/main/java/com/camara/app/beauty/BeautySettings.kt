package com.camara.app.beauty

import com.camara.app.filters.ColorMatrixMath

/**
 * Live beauty / retouch parameters. All strengths are 0f..1f (or -1f..1f where a
 * direction is meaningful) so the UI sliders map directly onto them.
 */
data class BeautySettings(
    val smooth: Float = 0f,        // skin smoothing (blur-blend)
    val brighten: Float = 0f,      // lift overall exposure
    val warmth: Float = 0f,        // -1 cool .. +1 warm
    val skinTone: Float = 0f,      // -1 cool .. +1 rosy
    val sharpen: Float = 0f,       // detail recovery
    val slimFace: Float = 0f,      // horizontal centre pull
    val enlargeEyes: Float = 0f    // local magnification
) {
    val isDefault: Boolean
        get() = smooth == 0f && brighten == 0f && warmth == 0f &&
            skinTone == 0f && sharpen == 0f && slimFace == 0f && enlargeEyes == 0f

    /** True when there is a colour-only component, which can be previewed live. */
    val hasGrade: Boolean
        get() = brighten != 0f || warmth != 0f || skinTone != 0f

    /**
     * The colour part of the retouch as a 4x5 matrix. This is the portion that can
     * be previewed live on the camera (via RenderEffect); the spatial parts
     * (smoothing, slimming, eye enlargement) are baked into the capture.
     */
    fun gradeMatrix(): FloatArray = ColorMatrixMath.chain(
        ColorMatrixMath.saturation(1f + skinTone * 0.08f),
        ColorMatrixMath.brightness(brighten * 0.25f),
        ColorMatrixMath.temperature(warmth)
    )

    companion object {
        val DEFAULT = BeautySettings()

        /** One-tap natural retouch: "Enhance face". */
        fun enhance(): BeautySettings = BeautySettings(
            smooth = 0.55f,
            brighten = 0.16f,
            warmth = 0.10f,
            skinTone = 0.15f,
            sharpen = 0.22f
        )
    }
}
