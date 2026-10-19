package com.camara.app.filters

/**
 * Small linear-algebra helpers for building 4x5 colour matrices.
 *
 * A colour matrix transforms a pixel `v = [r, g, b, a]` (each 0..255) into
 * `v' = M * v + offset`. Matrices compose by multiplication, so filters are
 * assembled by chaining primitives (saturation, contrast, warmth, ...).
 */
object ColorMatrixMath {

    val IDENTITY: FloatArray = floatArrayOf(
        1f, 0f, 0f, 0f, 0f,
        0f, 1f, 0f, 0f, 0f,
        0f, 0f, 1f, 0f, 0f,
        0f, 0f, 0f, 1f, 0f
    )

    /** Returns [after] applied to the output of [before], i.e. after(before(x)). */
    fun compose(after: FloatArray, before: FloatArray): FloatArray {
        val out = FloatArray(20)
        for (i in 0 until 4) {
            for (j in 0 until 5) {
                var sum = 0f
                for (k in 0 until 4) {
                    sum += after[i * 5 + k] * before[k * 5 + j]
                }
                if (j == 4) sum += after[i * 5 + 4]
                out[i * 5 + j] = sum
            }
        }
        return out
    }

    /** Chains matrices left-to-right: chain(a, b, c) == a ∘ b ∘ c. */
    fun chain(vararg matrices: FloatArray): FloatArray {
        if (matrices.isEmpty()) return IDENTITY.copyOf()
        var acc = matrices[0]
        for (i in 1 until matrices.size) acc = compose(matrices[i], acc)
        return acc
    }

    fun lerp(a: FloatArray, b: FloatArray, t: Float): FloatArray {
        val out = FloatArray(20)
        for (i in 0 until 20) out[i] = a[i] + (b[i] - a[i]) * t
        return out
    }

    /** s = 0 fully desaturated, 1 unchanged, >1 boosted. */
    fun saturation(s: Float): FloatArray {
        val lr = 0.213f; val lg = 0.715f; val lb = 0.072f
        val sr = (1f - s) * lr; val sg = (1f - s) * lg; val sb = (1f - s) * lb
        return floatArrayOf(
            sr + s, sg, sb, 0f, 0f,
            sr, sg + s, sb, 0f, 0f,
            sr, sg, sb + s, 0f, 0f,
            0f, 0f, 0f, 1f, 0f
        )
    }

    /** c = 1 unchanged; <1 flatter, >1 punchier. */
    fun contrast(c: Float): FloatArray {
        val t = (1f - c) * 127.5f
        return floatArrayOf(
            c, 0f, 0f, 0f, t,
            0f, c, 0f, 0f, t,
            0f, 0f, c, 0f, t,
            0f, 0f, 0f, 1f, 0f
        )
    }

    /** b in -1..1, added as a flat offset. */
    fun brightness(b: Float): FloatArray {
        val t = b * 255f
        return floatArrayOf(
            1f, 0f, 0f, 0f, t,
            0f, 1f, 0f, 0f, t,
            0f, 0f, 1f, 0f, t,
            0f, 0f, 0f, 1f, 0f
        )
    }

    /** warmth in -1 (cool/blue) .. +1 (warm/amber). */
    fun temperature(warmth: Float): FloatArray {
        val r = warmth * 38f
        val b = -warmth * 38f
        return floatArrayOf(
            1f, 0f, 0f, 0f, r,
            0f, 1f, 0f, 0f, 0f,
            0f, 0f, 1f, 0f, b,
            0f, 0f, 0f, 1f, 0f
        )
    }

    /** t in -1 (magenta) .. +1 (green). */
    fun tint(t: Float): FloatArray {
        val g = t * 30f
        return floatArrayOf(
            1f, 0f, 0f, 0f, 0f,
            0f, 1f, 0f, 0f, g,
            0f, 0f, 1f, 0f, 0f,
            0f, 0f, 0f, 1f, 0f
        )
    }

    /** Per-channel multipliers, for split-tone / teal-and-orange looks. */
    fun channelGain(r: Float, g: Float, b: Float): FloatArray = floatArrayOf(
        r, 0f, 0f, 0f, 0f,
        0f, g, 0f, 0f, 0f,
        0f, 0f, b, 0f, 0f,
        0f, 0f, 0f, 1f, 0f
    )

    private val SEPIA = floatArrayOf(
        0.393f, 0.769f, 0.189f, 0f, 0f,
        0.349f, 0.686f, 0.168f, 0f, 0f,
        0.272f, 0.534f, 0.131f, 0f, 0f,
        0f, 0f, 0f, 1f, 0f
    )

    /** amount 0..1 blends toward sepia. */
    fun sepia(amount: Float): FloatArray = lerp(IDENTITY, SEPIA, amount)
}
