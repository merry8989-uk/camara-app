package com.camara.app.lenses

/** One piece of a lens. */
sealed interface LensArt

/**
 * An emoji placed relative to the face. [rx] is an offset in units of face width,
 * [ry] in units of face height, measured from the face centre. [size] is the glyph
 * size as a fraction of face width.
 */
data class EmojiArt(
    val emoji: String,
    val rx: Float,
    val ry: Float,
    val size: Float
) : LensArt

/** A hand-drawn piece — crisper than an emoji for glasses, ears, moustaches. */
data class DrawnArt(
    val kind: DrawKind,
    val rx: Float,
    val ry: Float,
    val size: Float
) : LensArt

enum class DrawKind { GLASSES, CAT_EARS, MUSTACHE, BLUSH, CROWN }

/** A face lens: a set of pieces anchored to the detected face. */
data class Lens(
    val id: String,
    val name: String,
    val art: List<LensArt>
)
