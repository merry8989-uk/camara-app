package com.camara.app.filters

/** Grouping used by the filter carousel in the UI. */
enum class FilterCategory(val label: String) {
    ORIGINAL("Original"),
    PORTRAIT("Portrait"),
    CLASSIC("Classic"),
    WARM("Warm"),
    COOL("Cool"),
    MONO("Mono"),
    CINEMATIC("Cinematic"),
    VINTAGE("Vintage"),
    SNAP("Snap"),
    SOCIAL("Social")
}

/**
 * A single filter, stored as a 4x5 colour matrix (20 floats, row-major) applied to
 * RGBA. The same matrix drives the live preview, the still-image renderer and the
 * video exporter, so what you see is what you export.
 */
data class Filter(
    val id: String,
    val name: String,
    val category: FilterCategory,
    val matrix: FloatArray
) {
    val isIdentity: Boolean get() = matrix.contentEquals(ColorMatrixMath.IDENTITY)

    /**
     * The matrix blended toward identity by [intensity] (0..1). This is what powers
     * the filter strength slider — a full-strength matrix lerped toward no-op gives
     * a real, artifact-free blend.
     */
    fun matrixAt(intensity: Float): FloatArray {
        val t = intensity.coerceIn(0f, 1f)
        return if (t >= 0.999f) matrix else ColorMatrixMath.lerp(ColorMatrixMath.IDENTITY, matrix, t)
    }

    // FloatArray needs explicit equals/hashCode; data-class defaults compare references.
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is Filter) return false
        return id == other.id && matrix.contentEquals(other.matrix)
    }

    override fun hashCode(): Int = 31 * id.hashCode() + matrix.contentHashCode()
}
