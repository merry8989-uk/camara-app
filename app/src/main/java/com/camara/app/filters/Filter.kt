package com.camara.app.filters

/** Grouping used by the filter carousel in the UI. */
enum class FilterCategory(val label: String) {
    ORIGINAL("Original"),
    CLASSIC("Classic"),
    WARM("Warm"),
    COOL("Cool"),
    MONO("Mono"),
    CINEMATIC("Cinematic"),
    SNAP("Snap"),
    SOCIAL("Social")
}

/**
 * A single filter, stored as a 4x5 colour matrix (20 floats, row-major) applied to
 * RGBA. The same matrix drives both the live preview (via RenderEffect) and the
 * still-image renderer, so what you see is what you export.
 */
data class Filter(
    val id: String,
    val name: String,
    val category: FilterCategory,
    val matrix: FloatArray
) {
    val isIdentity: Boolean get() = matrix.contentEquals(ColorMatrixMath.IDENTITY)

    // FloatArray needs explicit equals/hashCode; data-class defaults compare references.
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is Filter) return false
        return id == other.id && matrix.contentEquals(other.matrix)
    }

    override fun hashCode(): Int = 31 * id.hashCode() + matrix.contentHashCode()
}
