package com.camara.app.filters

/**
 * The built-in filter library, grouped into ten families.
 *
 * Every look is assembled from colour-matrix primitives, so there are no shader
 * assets to ship and every filter works on every device. Filter *strength* is a
 * separate control — see [Filter.matrixAt], which lerps a look toward identity.
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

        // ------------------------------------------------------------------ Portrait
        // Soft, skin-friendly looks: gentle contrast, warm mid-tones, no clipping.
        f("softglow", "Soft Glow", FilterCategory.PORTRAIT, M.saturation(1.05f), M.contrast(0.96f), M.brightness(0.07f), M.temperature(0.06f)),
        f("porcelain", "Porcelain", FilterCategory.PORTRAIT, M.saturation(0.92f), M.contrast(0.94f), M.brightness(0.11f), M.temperature(0.03f)),
        f("peach", "Peach", FilterCategory.PORTRAIT, M.saturation(1.06f), M.brightness(0.08f), M.temperature(0.14f), M.tint(0.04f)),
        f("honey", "Honey", FilterCategory.PORTRAIT, M.saturation(1.08f), M.contrast(1.02f), M.brightness(0.07f), M.temperature(0.18f)),
        f("silk", "Silk", FilterCategory.PORTRAIT, M.saturation(0.96f), M.contrast(0.97f), M.brightness(0.09f), M.tint(0.05f)),
        f("airbrush", "Airbrush", FilterCategory.PORTRAIT, M.saturation(0.95f), M.contrast(0.92f), M.brightness(0.13f), M.temperature(0.05f)),
        f("glow", "Glow", FilterCategory.PORTRAIT, M.saturation(1.10f), M.contrast(0.94f), M.brightness(0.10f), M.temperature(0.10f), M.tint(0.05f)),
        f("natural", "Natural", FilterCategory.PORTRAIT, M.saturation(1.02f), M.contrast(1.01f), M.brightness(0.03f)),

        // ------------------------------------------------------------------- Classic
        f("clarendon", "Clarendon", FilterCategory.CLASSIC, M.saturation(1.25f), M.contrast(1.15f), M.brightness(0.02f), M.temperature(-0.05f)),
        f("gingham", "Gingham", FilterCategory.CLASSIC, M.saturation(0.85f), M.contrast(1.05f), M.brightness(0.06f), M.temperature(-0.10f)),
        f("juno", "Juno", FilterCategory.CLASSIC, M.saturation(1.35f), M.contrast(1.10f), M.temperature(0.08f)),
        f("lark", "Lark", FilterCategory.CLASSIC, M.saturation(1.10f), M.contrast(1.05f), M.brightness(0.08f), M.temperature(0.05f)),
        f("reyes", "Reyes", FilterCategory.CLASSIC, M.saturation(0.75f), M.contrast(0.95f), M.brightness(0.12f), M.sepia(0.15f)),
        f("xpro", "X-Pro", FilterCategory.CLASSIC, M.saturation(1.10f), M.contrast(1.20f), M.temperature(0.05f)),
        f("lofi", "Lo-Fi", FilterCategory.CLASSIC, M.saturation(1.40f), M.contrast(1.25f)),
        f("amaro", "Amaro", FilterCategory.CLASSIC, M.saturation(1.10f), M.contrast(1.10f), M.brightness(0.08f)),
        f("sierra", "Sierra", FilterCategory.CLASSIC, M.saturation(0.90f), M.contrast(1.05f), M.brightness(0.10f), M.sepia(0.10f)),
        f("crema", "Crema", FilterCategory.CLASSIC, M.saturation(0.90f), M.brightness(0.08f), M.temperature(0.05f), M.sepia(0.10f)),

        // ---------------------------------------------------------------------- Warm
        f("valencia", "Valencia", FilterCategory.WARM, M.saturation(1.20f), M.contrast(1.08f), M.temperature(0.15f), M.brightness(0.05f)),
        f("mayfair", "Mayfair", FilterCategory.WARM, M.saturation(1.15f), M.contrast(1.05f), M.brightness(0.05f), M.temperature(0.08f)),
        f("rise", "Rise", FilterCategory.WARM, M.saturation(1.05f), M.brightness(0.10f), M.temperature(0.10f)),
        f("hefe", "Hefe", FilterCategory.WARM, M.saturation(1.30f), M.contrast(1.15f), M.temperature(0.12f)),
        f("toaster", "Toaster", FilterCategory.WARM, M.saturation(1.15f), M.contrast(1.20f), M.brightness(0.05f), M.temperature(0.15f)),
        f("aden", "Aden", FilterCategory.WARM, M.saturation(0.85f), M.temperature(0.12f), M.brightness(0.10f)),
        f("nashville", "Nashville", FilterCategory.WARM, M.saturation(1.10f), M.contrast(1.05f), M.temperature(0.20f), M.brightness(0.05f), M.tint(-0.05f)),
        f("golden", "Golden Hour", FilterCategory.WARM, M.saturation(1.18f), M.contrast(1.06f), M.brightness(0.06f), M.temperature(0.26f)),
        f("sunset", "Sunset", FilterCategory.WARM, M.saturation(1.28f), M.contrast(1.10f), M.temperature(0.30f), M.tint(-0.06f)),
        f("amber", "Amber", FilterCategory.WARM, M.saturation(1.12f), M.contrast(1.04f), M.brightness(0.05f), M.temperature(0.22f), M.sepia(0.08f)),

        // ---------------------------------------------------------------------- Cool
        f("hudson", "Hudson", FilterCategory.COOL, M.saturation(1.10f), M.contrast(1.15f), M.temperature(-0.10f), M.brightness(0.02f)),
        f("perpetua", "Perpetua", FilterCategory.COOL, M.contrast(1.05f), M.brightness(0.05f), M.temperature(-0.05f), M.tint(0.05f)),
        f("coldcinema", "Cold Cinema", FilterCategory.COOL, M.temperature(-0.22f), M.contrast(1.15f), M.saturation(0.90f)),
        f("arctic", "Arctic", FilterCategory.COOL, M.temperature(-0.32f), M.contrast(1.08f), M.saturation(0.92f), M.brightness(0.05f)),
        f("steel", "Steel", FilterCategory.COOL, M.temperature(-0.20f), M.saturation(0.70f), M.contrast(1.14f)),
        f("indigo", "Indigo", FilterCategory.COOL, M.channelGain(0.95f, 0.98f, 1.12f), M.contrast(1.10f), M.saturation(1.05f)),
        f("mint", "Mint", FilterCategory.COOL, M.channelGain(0.95f, 1.08f, 1.02f), M.temperature(-0.12f), M.saturation(1.05f)),

        // ---------------------------------------------------------------------- Mono
        f("moon", "Moon", FilterCategory.MONO, M.saturation(0f), M.contrast(1.10f), M.brightness(0.05f)),
        f("monohigh", "Mono High", FilterCategory.MONO, M.saturation(0f), M.contrast(1.30f)),
        f("silver", "Silver", FilterCategory.MONO, M.saturation(0.10f), M.contrast(1.10f), M.brightness(0.05f)),
        f("sepia", "Sepia", FilterCategory.MONO, M.sepia(1f)),
        f("willow", "Willow", FilterCategory.MONO, M.saturation(0.30f), M.brightness(0.10f), M.sepia(0.40f)),
        f("slumber", "Slumber", FilterCategory.MONO, M.saturation(0.85f), M.contrast(1.05f), M.brightness(0.05f), M.sepia(0.25f)),
        f("noir", "Noir", FilterCategory.MONO, M.saturation(0f), M.contrast(1.45f), M.brightness(-0.04f)),
        f("charcoal", "Charcoal", FilterCategory.MONO, M.saturation(0f), M.contrast(1.20f), M.brightness(-0.02f), M.channelGain(1.02f, 1.0f, 0.98f)),

        // ----------------------------------------------------------------- Cinematic
        f("tealorange", "Teal & Orange", FilterCategory.CINEMATIC, M.channelGain(1.06f, 1.00f, 1.03f), M.contrast(1.12f), M.saturation(1.05f), M.temperature(0.06f)),
        f("bleach", "Bleach Bypass", FilterCategory.CINEMATIC, M.saturation(0.60f), M.contrast(1.30f)),
        f("fade", "Fade", FilterCategory.CINEMATIC, M.contrast(0.85f), M.brightness(0.12f), M.saturation(0.95f)),
        f("warmcinema", "Warm Cinema", FilterCategory.CINEMATIC, M.temperature(0.25f), M.contrast(1.10f)),
        f("vesper", "Vesper", FilterCategory.CINEMATIC, M.saturation(1.10f), M.contrast(1.10f), M.brightness(0.05f), M.temperature(0.10f), M.sepia(0.15f)),
        f("dogpatch", "Dogpatch", FilterCategory.CINEMATIC, M.saturation(0.80f), M.contrast(1.20f), M.sepia(0.20f)),
        f("blockbuster", "Blockbuster", FilterCategory.CINEMATIC, M.channelGain(1.04f, 1.0f, 1.06f), M.contrast(1.22f), M.saturation(0.95f)),
        f("noirfilm", "Noir Film", FilterCategory.CINEMATIC, M.saturation(0f), M.contrast(1.35f), M.brightness(0.03f)),
        f("anamorphic", "Anamorphic", FilterCategory.CINEMATIC, M.channelGain(1.05f, 0.98f, 1.08f), M.contrast(1.16f), M.saturation(1.02f), M.temperature(-0.05f)),

        // ------------------------------------------------------------------- Vintage
        f("fadedfilm", "Faded Film", FilterCategory.VINTAGE, M.contrast(0.88f), M.brightness(0.10f), M.saturation(0.88f), M.sepia(0.22f)),
        f("oldphoto", "Old Photo", FilterCategory.VINTAGE, M.saturation(0.45f), M.contrast(0.92f), M.brightness(0.10f), M.sepia(0.55f)),
        f("retro", "Retro", FilterCategory.VINTAGE, M.saturation(1.05f), M.contrast(1.05f), M.temperature(0.16f), M.sepia(0.25f)),
        f("kodak", "Kodak", FilterCategory.VINTAGE, M.saturation(1.18f), M.contrast(1.06f), M.temperature(0.12f), M.sepia(0.12f)),
        f("polaroid", "Polaroid", FilterCategory.VINTAGE, M.contrast(0.90f), M.brightness(0.13f), M.saturation(0.92f), M.temperature(-0.06f)),
        f("lomo", "Lomo", FilterCategory.VINTAGE, M.saturation(1.40f), M.contrast(1.28f), M.temperature(0.08f), M.tint(-0.05f)),
        f("dust", "Dusty", FilterCategory.VINTAGE, M.contrast(0.90f), M.brightness(0.08f), M.saturation(0.72f), M.sepia(0.35f)),
        f("super8", "Super 8", FilterCategory.VINTAGE, M.saturation(1.12f), M.contrast(1.10f), M.temperature(0.20f), M.sepia(0.30f), M.brightness(0.04f)),

        // ---------------------------------------------------------------------- Snap
        f("snapwarm", "Snap Warm", FilterCategory.SNAP, M.temperature(0.30f), M.saturation(1.20f), M.brightness(0.05f)),
        f("snapcool", "Snap Cool", FilterCategory.SNAP, M.temperature(-0.30f), M.saturation(1.15f), M.contrast(1.05f)),
        f("snappop", "Snap Pop", FilterCategory.SNAP, M.saturation(1.50f), M.contrast(1.20f)),
        f("snapfade", "Snap Fade", FilterCategory.SNAP, M.contrast(0.85f), M.brightness(0.15f), M.saturation(0.90f)),
        f("snapvivid", "Snap Vivid", FilterCategory.SNAP, M.saturation(1.60f), M.contrast(1.10f)),
        f("snappunch", "Snap Punch", FilterCategory.SNAP, M.saturation(1.45f), M.contrast(1.32f), M.brightness(0.02f)),
        f("snapdream", "Snap Dream", FilterCategory.SNAP, M.saturation(1.12f), M.contrast(0.90f), M.brightness(0.14f), M.tint(0.07f)),

        // -------------------------------------------------------------------- Social
        f("fbclassic", "Feed Classic", FilterCategory.SOCIAL, M.saturation(1.10f), M.contrast(1.05f), M.brightness(0.03f)),
        f("fbwarm", "Feed Warm", FilterCategory.SOCIAL, M.temperature(0.15f), M.saturation(1.05f)),
        f("fbvivid", "Feed Vivid", FilterCategory.SOCIAL, M.saturation(1.30f), M.contrast(1.10f)),
        f("fbnoir", "Feed Noir", FilterCategory.SOCIAL, M.saturation(0f), M.contrast(1.20f)),
        f("fbvintage", "Feed Vintage", FilterCategory.SOCIAL, M.sepia(0.30f), M.saturation(0.85f), M.contrast(1.05f)),
        f("fbbright", "Feed Bright", FilterCategory.SOCIAL, M.brightness(0.12f), M.contrast(1.04f), M.saturation(1.08f)),
        f("fbsoft", "Feed Soft", FilterCategory.SOCIAL, M.contrast(0.94f), M.brightness(0.09f), M.saturation(0.96f), M.temperature(0.05f))
    )

    fun byId(id: String): Filter = ALL.firstOrNull { it.id == id } ?: NONE

    fun categories(): List<FilterCategory> = FilterCategory.entries.toList()

    fun inCategory(category: FilterCategory): List<Filter> =
        if (category == FilterCategory.ORIGINAL) listOf(NONE) else ALL.filter { it.category == category }

    /** Categories that actually have filters, in declaration order. */
    fun activeCategories(): List<FilterCategory> =
        FilterCategory.entries.filter { inCategory(it).isNotEmpty() }
}
