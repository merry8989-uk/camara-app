package com.camara.app.filters

/**
 * The built-in filter library. Every look is assembled from colour-matrix
 * primitives, so there are no shader assets to ship and every filter works on
 * every device. Presets are grouped into families reminiscent of the looks you
 * find in popular camera and social apps, but each value here is tuned from
 * scratch.
 */
object FilterPresets {

    private fun f(
        id: String,
        name: String,
        category: FilterCategory,
        vararg parts: FloatArray
    ) = Filter(id, name, category, ColorMatrixMath.chain(*parts))

    private val M = ColorMatrixMath

    val NONE = Filter("none", "Original", FilterCategory.ORIGINAL, M.IDENTITY.copyOf())

    val ALL: List<Filter> = listOf(
        NONE,

        // --- Classic / editorial ---
        f("clarendon", "Clarendon", FilterCategory.CLASSIC, M.saturation(1.25f), M.contrast(1.15f), M.brightness(0.02f), M.temperature(-0.05f)),
        f("gingham", "Gingham", FilterCategory.CLASSIC, M.saturation(0.85f), M.contrast(1.05f), M.brightness(0.06f), M.temperature(-0.10f)),
        f("juno", "Juno", FilterCategory.CLASSIC, M.saturation(1.35f), M.contrast(1.10f), M.temperature(0.08f)),
        f("lark", "Lark", FilterCategory.CLASSIC, M.saturation(1.10f), M.contrast(1.05f), M.brightness(0.08f), M.temperature(0.05f)),
        f("reyes", "Reyes", FilterCategory.CLASSIC, M.saturation(0.75f), M.contrast(0.95f), M.brightness(0.12f), M.sepia(0.15f)),
        f("xpro", "X-Pro", FilterCategory.CLASSIC, M.saturation(1.10f), M.contrast(1.20f), M.temperature(0.05f)),
        f("lofi", "Lo-Fi", FilterCategory.CLASSIC, M.saturation(1.40f), M.contrast(1.25f)),
        f("amaro", "Amaro", FilterCategory.CLASSIC, M.saturation(1.10f), M.contrast(1.10f), M.brightness(0.08f)),

        // --- Warm ---
        f("valencia", "Valencia", FilterCategory.WARM, M.saturation(1.20f), M.contrast(1.08f), M.temperature(0.15f), M.brightness(0.05f)),
        f("mayfair", "Mayfair", FilterCategory.WARM, M.saturation(1.15f), M.contrast(1.05f), M.brightness(0.05f), M.temperature(0.08f)),
        f("rise", "Rise", FilterCategory.WARM, M.saturation(1.05f), M.brightness(0.10f), M.temperature(0.10f)),
        f("hefe", "Hefe", FilterCategory.WARM, M.saturation(1.30f), M.contrast(1.15f), M.temperature(0.12f)),
        f("toaster", "Toaster", FilterCategory.WARM, M.saturation(1.15f), M.contrast(1.20f), M.brightness(0.05f), M.temperature(0.15f)),
        f("aden", "Aden", FilterCategory.WARM, M.saturation(0.85f), M.temperature(0.12f), M.brightness(0.10f)),
        f("crema", "Crema", FilterCategory.WARM, M.saturation(0.90f), M.brightness(0.08f), M.temperature(0.05f), M.sepia(0.10f)),
        f("nashville", "Nashville", FilterCategory.WARM, M.saturation(1.10f), M.contrast(1.05f), M.temperature(0.20f), M.brightness(0.05f), M.tint(-0.05f)),

        // --- Cool ---
        f("hudson", "Hudson", FilterCategory.COOL, M.saturation(1.10f), M.contrast(1.15f), M.temperature(-0.10f), M.brightness(0.02f)),
        f("perpetua", "Perpetua", FilterCategory.COOL, M.contrast(1.05f), M.brightness(0.05f), M.temperature(-0.05f), M.tint(0.05f)),
        f("sierra", "Sierra", FilterCategory.COOL, M.saturation(0.90f), M.contrast(1.05f), M.brightness(0.10f), M.sepia(0.10f)),
        f("coldcinema", "Cold Cinema", FilterCategory.COOL, M.temperature(-0.22f), M.contrast(1.15f), M.saturation(0.90f)),

        // --- Mono ---
        f("moon", "Moon", FilterCategory.MONO, M.saturation(0f), M.contrast(1.10f), M.brightness(0.05f)),
        f("monohigh", "Mono High", FilterCategory.MONO, M.saturation(0f), M.contrast(1.30f)),
        f("silver", "Silver", FilterCategory.MONO, M.saturation(0.10f), M.contrast(1.10f), M.brightness(0.05f)),
        f("sepia", "Sepia", FilterCategory.MONO, M.sepia(1f)),
        f("willow", "Willow", FilterCategory.MONO, M.saturation(0.30f), M.brightness(0.10f), M.sepia(0.40f)),
        f("slumber", "Slumber", FilterCategory.MONO, M.saturation(0.85f), M.contrast(1.05f), M.brightness(0.05f), M.sepia(0.25f)),

        // --- Cinematic ---
        f("tealorange", "Teal & Orange", FilterCategory.CINEMATIC, M.channelGain(1.06f, 1.00f, 1.03f), M.contrast(1.12f), M.saturation(1.05f), M.temperature(0.06f)),
        f("bleach", "Bleach Bypass", FilterCategory.CINEMATIC, M.saturation(0.60f), M.contrast(1.30f)),
        f("fade", "Fade", FilterCategory.CINEMATIC, M.contrast(0.85f), M.brightness(0.12f), M.saturation(0.95f)),
        f("warmcinema", "Warm Cinema", FilterCategory.CINEMATIC, M.temperature(0.25f), M.contrast(1.10f)),
        f("vesper", "Vesper", FilterCategory.CINEMATIC, M.saturation(1.10f), M.contrast(1.10f), M.brightness(0.05f), M.temperature(0.10f), M.sepia(0.15f)),
        f("dogpatch", "Dogpatch", FilterCategory.CINEMATIC, M.saturation(0.80f), M.contrast(1.20f), M.sepia(0.20f)),

        // --- Snap-style pop ---
        f("snapwarm", "Snap Warm", FilterCategory.SNAP, M.temperature(0.30f), M.saturation(1.20f), M.brightness(0.05f)),
        f("snapcool", "Snap Cool", FilterCategory.SNAP, M.temperature(-0.30f), M.saturation(1.15f), M.contrast(1.05f)),
        f("snappop", "Snap Pop", FilterCategory.SNAP, M.saturation(1.50f), M.contrast(1.20f)),
        f("snapfade", "Snap Fade", FilterCategory.SNAP, M.contrast(0.85f), M.brightness(0.15f), M.saturation(0.90f)),
        f("snapvivid", "Snap Vivid", FilterCategory.SNAP, M.saturation(1.60f), M.contrast(1.10f)),

        // --- Social feed ---
        f("fbclassic", "Feed Classic", FilterCategory.SOCIAL, M.saturation(1.10f), M.contrast(1.05f), M.brightness(0.03f)),
        f("fbwarm", "Feed Warm", FilterCategory.SOCIAL, M.temperature(0.15f), M.saturation(1.05f)),
        f("fbvivid", "Feed Vivid", FilterCategory.SOCIAL, M.saturation(1.30f), M.contrast(1.10f)),
        f("fbnoir", "Feed Noir", FilterCategory.SOCIAL, M.saturation(0f), M.contrast(1.20f)),
        f("fbvintage", "Feed Vintage", FilterCategory.SOCIAL, M.sepia(0.30f), M.saturation(0.85f), M.contrast(1.05f))
    )

    fun byId(id: String): Filter = ALL.firstOrNull { it.id == id } ?: NONE

    fun categories(): List<FilterCategory> = FilterCategory.values().toList()

    fun inCategory(category: FilterCategory): List<Filter> =
        if (category == FilterCategory.ORIGINAL) listOf(NONE) else ALL.filter { it.category == category }
}
