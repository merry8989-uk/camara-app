package com.camara.app.lenses

/**
 * Where a lens piece attaches on the face. Anchoring to landmarks (rather than the
 * bounding box) is what makes a lens sit correctly as the head moves and tilts.
 */
enum class Anchor {
    EYE_MID,      // between the eyes — glasses, lashes, symmetric masks
    EYE_LEFT,
    EYE_RIGHT,
    NOSE,         // base of the nose — moustaches, animal noses, whiskers
    MOUTH,        // lips
    CHIN,
    FOREHEAD,     // above the eyes — hats, crowns, halos, ears
    FACE_CENTRE,
    EAR_LEFT,
    EAR_RIGHT,
    CHEEK_LEFT,
    CHEEK_RIGHT
}

/** One piece of a lens. */
sealed interface LensArt

/**
 * An emoji placed on an anchor. [dx]/[dy] are offsets in units of the inter-ocular
 * distance (IOD) and [size] is the glyph size in IOD units, so the piece keeps its
 * proportion as the face moves closer or further away.
 */
data class EmojiArt(
    val emoji: String,
    val anchor: Anchor,
    val dx: Float = 0f,
    val dy: Float = 0f,
    val size: Float = 0.8f
) : LensArt

/** Hand-drawn pieces — crisper than emoji, and can follow the real face contours. */
enum class DrawKind {
    GLASSES,          // rectangular shades
    ROUND_GLASSES,
    HEART_GLASSES,
    CAT_EARS,
    MUSTACHE,
    BLUSH,
    CROWN,
    LIPS,             // lipstick that follows the lip contour
    LASHES,           // follows the eye contours
    FACE_PAINT,       // translucent colour over the face oval
    HALO,
    PARTY_HAT,
    ANIMAL_NOSE,
    WHISKERS
}

data class DrawnArt(
    val kind: DrawKind,
    val anchor: Anchor,
    val dx: Float = 0f,
    val dy: Float = 0f,
    val size: Float = 1f
) : LensArt

/** A face lens: a set of pieces anchored to the detected face. */
data class Lens(
    val id: String,
    val name: String,
    val art: List<LensArt>
)
