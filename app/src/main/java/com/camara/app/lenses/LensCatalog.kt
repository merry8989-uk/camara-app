package com.camara.app.lenses

/**
 * The built-in lens catalogue.
 *
 * Every piece is anchored to a facial landmark and sized in units of the
 * inter-ocular distance, so lenses track the face rather than the bounding box.
 * Sizes: a typical pair of glasses is about 2.4 IOD wide; cat ears about 1.8.
 */
object LensCatalog {

    private fun emoji(e: String, a: Anchor, dx: Float = 0f, dy: Float = 0f, size: Float = 0.8f) =
        EmojiArt(e, a, dx, dy, size)

    private fun drawn(k: DrawKind, a: Anchor, dx: Float = 0f, dy: Float = 0f, size: Float = 1f) =
        DrawnArt(k, a, dx, dy, size)

    val ALL: List<Lens> = listOf(
        Lens("none", "None", emptyList()),

        // ---------------------------------------------------------- Eyewear
        Lens("shades", "Shades", listOf(drawn(DrawKind.GLASSES, Anchor.EYE_MID, 0f, 0f, 2.5f))),
        Lens("round", "Round", listOf(drawn(DrawKind.ROUND_GLASSES, Anchor.EYE_MID, 0f, 0f, 2.5f))),
        Lens("heartspecs", "Heart Specs", listOf(drawn(DrawKind.HEART_GLASSES, Anchor.EYE_MID, 0f, 0f, 2.6f))),
        Lens("lashes", "Lashes", listOf(drawn(DrawKind.LASHES, Anchor.EYE_MID, 0f, 0f, 2.6f))),
        Lens("shades_lash", "Shades + Lashes", listOf(
            drawn(DrawKind.LASHES, Anchor.EYE_MID, 0f, 0f, 2.6f),
            drawn(DrawKind.GLASSES, Anchor.EYE_MID, 0f, 0f, 2.5f)
        )),

        // ---------------------------------------------------------- Animals
        Lens("cat", "Cat", listOf(
            drawn(DrawKind.CAT_EARS, Anchor.FOREHEAD, 0f, -0.45f, 1.9f),
            drawn(DrawKind.ANIMAL_NOSE, Anchor.NOSE, 0f, 0.06f, 0.85f),
            drawn(DrawKind.WHISKERS, Anchor.NOSE, 0f, 0.10f, 2.2f)
        )),
        Lens("coolcat", "Cool Cat", listOf(
            drawn(DrawKind.CAT_EARS, Anchor.FOREHEAD, 0f, -0.45f, 1.9f),
            drawn(DrawKind.GLASSES, Anchor.EYE_MID, 0f, 0f, 2.5f)
        )),
        Lens("fox", "Fox", listOf(
            emoji("🦊", Anchor.FOREHEAD, 0f, -0.55f, 1.6f),
            drawn(DrawKind.ANIMAL_NOSE, Anchor.NOSE, 0f, 0.06f, 0.85f),
            drawn(DrawKind.WHISKERS, Anchor.NOSE, 0f, 0.10f, 2.2f)
        )),
        Lens("puppy", "Puppy", listOf(
            emoji("🐶", Anchor.FOREHEAD, 0f, -0.60f, 1.7f),
            drawn(DrawKind.ANIMAL_NOSE, Anchor.NOSE, 0f, 0.08f, 0.9f)
        )),
        Lens("bunny", "Bunny", listOf(
            emoji("🐰", Anchor.FOREHEAD, 0f, -0.62f, 1.7f),
            drawn(DrawKind.BLUSH, Anchor.EYE_MID, 0f, 0.85f, 2.9f)
        )),
        Lens("panda", "Panda", listOf(
            emoji("🐼", Anchor.FOREHEAD, 0f, -0.60f, 1.7f),
            drawn(DrawKind.ANIMAL_NOSE, Anchor.NOSE, 0f, 0.08f, 0.9f)
        )),

        // ---------------------------------------------------------- Makeup
        Lens("lips", "Red Lips", listOf(drawn(DrawKind.LIPS, Anchor.MOUTH, 0f, 0f, 1.3f))),
        Lens("blush", "Blush", listOf(drawn(DrawKind.BLUSH, Anchor.EYE_MID, 0f, 0.85f, 2.9f))),
        Lens("glam", "Glam", listOf(
            drawn(DrawKind.LIPS, Anchor.MOUTH, 0f, 0f, 1.3f),
            drawn(DrawKind.BLUSH, Anchor.EYE_MID, 0f, 0.85f, 2.9f),
            drawn(DrawKind.LASHES, Anchor.EYE_MID, 0f, 0f, 2.6f)
        )),
        Lens("paint", "Face Paint", listOf(drawn(DrawKind.FACE_PAINT, Anchor.FACE_CENTRE, 0f, 0f, 0f))),

        // ---------------------------------------------------------- Headwear
        Lens("crown", "Crown", listOf(drawn(DrawKind.CROWN, Anchor.FOREHEAD, 0f, -0.35f, 2.1f))),
        Lens("halo", "Halo", listOf(drawn(DrawKind.HALO, Anchor.FOREHEAD, 0f, -0.80f, 1.8f))),
        Lens("party", "Party Hat", listOf(
            drawn(DrawKind.PARTY_HAT, Anchor.FOREHEAD, 0f, -0.55f, 1.7f),
            emoji("🎉", Anchor.EYE_LEFT, -0.95f, -0.55f, 0.9f),
            emoji("🎊", Anchor.EYE_RIGHT, 0.95f, -0.55f, 0.9f)
        )),
        Lens("flower", "Flower Crown", listOf(
            emoji("🌸", Anchor.FOREHEAD, -0.95f, -0.20f, 0.85f),
            emoji("🌺", Anchor.FOREHEAD, 0f, -0.40f, 0.95f),
            emoji("🌼", Anchor.FOREHEAD, 0.95f, -0.20f, 0.85f),
            emoji("🌸", Anchor.FOREHEAD, -1.45f, 0.10f, 0.70f),
            emoji("🌸", Anchor.FOREHEAD, 1.45f, 0.10f, 0.70f)
        )),
        Lens("beanie", "Beanie", listOf(emoji("🧢", Anchor.FOREHEAD, 0f, -0.50f, 2.4f))),

        // ---------------------------------------------------------- Fun
        Lens("hearts", "Hearts", listOf(
            emoji("❤️", Anchor.EYE_LEFT, 0f, 0f, 0.85f),
            emoji("❤️", Anchor.EYE_RIGHT, 0f, 0f, 0.85f),
            emoji("💕", Anchor.FOREHEAD, 0f, -0.55f, 1.1f)
        )),
        Lens("fire", "On Fire", listOf(
            emoji("🔥", Anchor.EYE_LEFT, -0.85f, -0.35f, 1.0f),
            emoji("🔥", Anchor.EYE_RIGHT, 0.85f, -0.35f, 1.0f),
            drawn(DrawKind.GLASSES, Anchor.EYE_MID, 0f, 0f, 2.5f)
        )),
        Lens("angel", "Angel", listOf(
            drawn(DrawKind.HALO, Anchor.FOREHEAD, 0f, -0.80f, 1.8f),
            emoji("✨", Anchor.CHEEK_LEFT, -0.30f, 0.30f, 0.75f),
            emoji("✨", Anchor.CHEEK_RIGHT, 0.30f, 0.30f, 0.75f)
        )),
        Lens("star", "Star", listOf(
            emoji("⭐", Anchor.EYE_LEFT, -0.80f, -0.40f, 0.85f),
            emoji("⭐", Anchor.EYE_RIGHT, 0.80f, -0.40f, 0.85f),
            emoji("✨", Anchor.MOUTH, 0f, 0.55f, 0.85f)
        )),
        Lens("moustache", "Moustache", listOf(drawn(DrawKind.MUSTACHE, Anchor.NOSE, 0f, 0.42f, 1.5f)))
    )

    val NONE: Lens = ALL.first()

    fun byId(id: String): Lens = ALL.firstOrNull { it.id == id } ?: NONE
}
