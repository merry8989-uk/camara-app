package com.camara.app.beauty

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

    companion object {
        val DEFAULT = BeautySettings()
    }
}
