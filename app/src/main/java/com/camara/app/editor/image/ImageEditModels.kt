package com.camara.app.editor.image

import com.camara.app.filters.ColorMatrixMath
import com.camara.app.filters.Filter
import com.camara.app.filters.FilterPresets
import com.camara.app.stickers.PlacedSticker

/** Normalised point (0..1) so drawing survives preview-to-export scaling. */
data class Pt(val x: Float, val y: Float)

/** A freehand brush stroke. */
data class DrawStroke(
    val points: List<Pt>,
    val colorArgb: Int,
    val widthFraction: Float
)

/** A draggable text overlay. */
data class TextItem(
    val id: Long,
    val text: String,
    val x: Float,
    val y: Float,
    val scale: Float = 1f,
    val rotation: Float = 0f,
    val colorArgb: Int = 0xFFFFFFFF.toInt(),
    val sizeFraction: Float = 0.09f
)

/** A normalised crop rectangle over the (rotated, flipped) image. */
data class CropRect(
    val left: Float,
    val top: Float,
    val right: Float,
    val bottom: Float
) {
    val width: Float get() = right - left
    val height: Float get() = bottom - top

    companion object {
        val FULL = CropRect(0f, 0f, 1f, 1f)

        /** Largest centred rect of the given width/height [aspect] inside the frame. */
        fun centered(aspect: Float): CropRect {
            if (aspect <= 0f) return FULL
            return if (aspect >= 1f) {
                val h = 1f / aspect
                CropRect(0f, (1f - h) / 2f, 1f, (1f + h) / 2f)
            } else {
                val w = aspect
                CropRect((1f - w) / 2f, 0f, (1f + w) / 2f, 1f)
            }
        }
    }
}

/** Photo frame styles that are baked on export. */
enum class FrameStyle(val label: String) {
    NONE("None"),
    WHITE("White"),
    ROUNDED("Rounded"),
    FILM("Film"),
    VIGNETTE("Vignette")
}

/** Colour adjustments from the Adjust panel. */
data class Adjustments(
    val brightness: Float = 0f,   // -1..1
    val contrast: Float = 1f,     // 0.5..2
    val saturation: Float = 1f,   // 0..2
    val warmth: Float = 0f        // -1..1
) {
    val isDefault: Boolean
        get() = brightness == 0f && contrast == 1f && saturation == 1f && warmth == 0f

    fun matrix(): FloatArray = ColorMatrixMath.chain(
        ColorMatrixMath.saturation(saturation),
        ColorMatrixMath.contrast(contrast),
        ColorMatrixMath.brightness(brightness),
        ColorMatrixMath.temperature(warmth)
    )

    companion object { val DEFAULT = Adjustments() }
}

/** The complete description of an edit session. */
data class ImageEditState(
    val rotationDeg: Float = 0f,
    val flipH: Boolean = false,
    val flipV: Boolean = false,
    val crop: CropRect = CropRect.FULL,
    val cropAspectLock: Float? = null,
    val filter: Filter = FilterPresets.NONE,
    val adjustments: Adjustments = Adjustments.DEFAULT,
    val stickers: List<PlacedSticker> = emptyList(),
    val texts: List<TextItem> = emptyList(),
    val strokes: List<DrawStroke> = emptyList(),
    val frame: FrameStyle = FrameStyle.NONE
) {
    val hasEdits: Boolean
        get() = rotationDeg != 0f || flipH || flipV || crop != CropRect.FULL ||
            !filter.isIdentity || !adjustments.isDefault ||
            stickers.isNotEmpty() || texts.isNotEmpty() ||
            strokes.isNotEmpty() || frame != FrameStyle.NONE
}
