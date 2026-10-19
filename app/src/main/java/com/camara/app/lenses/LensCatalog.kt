package com.camara.app.lenses

/**
 * The built-in lens catalogue. Lenses mix hand-drawn pieces (glasses, ears,
 * moustaches) with emoji accents, all anchored to the detected face box.
 */
object LensCatalog {

    val ALL: List<Lens> = listOf(
        Lens(
            "none", "None", emptyList()
        ),
        Lens(
            "sunglasses", "Shades",
            listOf(DrawnArt(DrawKind.GLASSES, 0f, -0.10f, 1.05f))
        ),
        Lens(
            "cat", "Cat",
            listOf(
                DrawnArt(DrawKind.CAT_EARS, 0f, -0.62f, 1.1f),
                EmojiArt("🐾", -0.55f, 0.45f, 0.30f)
            )
        ),
        Lens(
            "coolcat", "Cool Cat",
            listOf(
                DrawnArt(DrawKind.CAT_EARS, 0f, -0.62f, 1.1f),
                DrawnArt(DrawKind.GLASSES, 0f, -0.10f, 1.05f)
            )
        ),
        Lens(
            "moustache", "Moustache",
            listOf(DrawnArt(DrawKind.MUSTACHE, 0f, 0.28f, 0.85f))
        ),
        Lens(
            "crown", "Crown",
            listOf(DrawnArt(DrawKind.CROWN, 0f, -0.62f, 1.0f))
        ),
        Lens(
            "blush", "Blush",
            listOf(DrawnArt(DrawKind.BLUSH, 0f, 0.10f, 1.5f))
        ),
        Lens(
            "flower", "Flower Crown",
            listOf(
                EmojiArt("🌸", -0.42f, -0.55f, 0.34f),
                EmojiArt("🌺", 0f, -0.66f, 0.36f),
                EmojiArt("🌼", 0.42f, -0.55f, 0.34f),
                EmojiArt("🌸", -0.62f, -0.34f, 0.28f),
                EmojiArt("🌸", 0.62f, -0.34f, 0.28f)
            )
        ),
        Lens(
            "party", "Party",
            listOf(
                EmojiArt("🎉", -0.72f, -0.30f, 0.40f),
                EmojiArt("🥳", 0.72f, -0.30f, 0.40f),
                EmojiArt("🎊", 0f, -0.78f, 0.40f)
            )
        ),
        Lens(
            "heart", "Hearts",
            listOf(
                EmojiArt("❤️", -0.24f, -0.06f, 0.34f),
                EmojiArt("❤️", 0.24f, -0.06f, 0.34f),
                EmojiArt("💕", 0f, -0.70f, 0.40f)
            )
        ),
        Lens(
            "bunny", "Bunny",
            listOf(
                EmojiArt("🐰", 0f, -0.72f, 0.55f),
                DrawnArt(DrawKind.BLUSH, 0f, 0.12f, 1.5f)
            )
        ),
        Lens(
            "dog", "Puppy",
            listOf(
                EmojiArt("🐶", 0f, -0.72f, 0.60f),
                EmojiArt("🐾", -0.62f, 0.42f, 0.30f)
            )
        ),
        Lens(
            "star", "Star",
            listOf(
                EmojiArt("⭐", -0.70f, -0.50f, 0.36f),
                EmojiArt("⭐", 0.70f, -0.50f, 0.36f),
                EmojiArt("✨", 0f, 0.55f, 0.36f)
            )
        ),
        Lens(
            "angel", "Angel",
            listOf(
                EmojiArt("😇", 0f, -0.74f, 0.55f),
                EmojiArt("✨", -0.60f, 0.30f, 0.32f),
                EmojiArt("✨", 0.60f, 0.30f, 0.32f)
            )
        ),
        Lens(
            "fire", "On Fire",
            listOf(
                EmojiArt("🔥", -0.72f, -0.34f, 0.42f),
                EmojiArt("🔥", 0.72f, -0.34f, 0.42f),
                DrawnArt(DrawKind.GLASSES, 0f, -0.10f, 1.05f)
            )
        )
    )

    val NONE: Lens = ALL.first()

    fun byId(id: String): Lens = ALL.firstOrNull { it.id == id } ?: NONE
}
