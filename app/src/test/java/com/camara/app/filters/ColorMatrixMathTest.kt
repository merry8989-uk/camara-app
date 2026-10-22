package com.camara.app.filters

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Tests for the colour-matrix engine — the core of every filter, the filter strength
 * slider and the beauty grade. This maths had no coverage at all before.
 */
class ColorMatrixMathTest {

    /** Applies a 4x5 row-major matrix the same way Android's ColorMatrix does. */
    private fun apply(m: FloatArray, r: Float, g: Float, b: Float, a: Float = 255f): FloatArray {
        val v = floatArrayOf(r, g, b, a)
        return FloatArray(4) { i ->
            var sum = 0f
            for (k in 0 until 4) sum += m[i * 5 + k] * v[k]
            sum + m[i * 5 + 4]
        }
    }

    private fun assertColorEquals(expected: FloatArray, actual: FloatArray, delta: Float = 0.01f) {
        assertEquals("r", expected[0], actual[0], delta)
        assertEquals("g", expected[1], actual[1], delta)
        assertEquals("b", expected[2], actual[2], delta)
        assertEquals("a", expected[3], actual[3], delta)
    }

    @Test
    fun `identity leaves a colour untouched`() {
        val out = apply(ColorMatrixMath.IDENTITY, 12f, 200f, 77f, 255f)
        assertColorEquals(floatArrayOf(12f, 200f, 77f, 255f), out)
    }

    @Test
    fun `compose applies the second matrix after the first`() {
        val before = ColorMatrixMath.brightness(0.2f)   // +51 to every channel
        val after = ColorMatrixMath.contrast(0.5f)      // halve, recentre
        val composed = ColorMatrixMath.compose(after, before)

        val direct = apply(before, 100f, 100f, 100f)
        val staged = apply(after, direct[0], direct[1], direct[2])
        val single = apply(composed, 100f, 100f, 100f)

        assertColorEquals(staged, single)
    }

    @Test
    fun `chain applies matrices in the order given`() {
        val a = ColorMatrixMath.saturation(1.5f)
        val b = ColorMatrixMath.temperature(0.4f)
        val c = ColorMatrixMath.brightness(0.1f)

        val composed = ColorMatrixMath.chain(a, b, c)
        val step1 = apply(a, 90f, 140f, 210f)
        val step2 = apply(b, step1[0], step1[1], step1[2])
        val step3 = apply(c, step2[0], step2[1], step2[2])

        assertColorEquals(step3, apply(composed, 90f, 140f, 210f))
    }

    @Test
    fun `lerp at zero is identity and at one is the original matrix`() {
        val m = ColorMatrixMath.chain(ColorMatrixMath.saturation(1.6f), ColorMatrixMath.contrast(1.3f))

        val atZero = ColorMatrixMath.lerp(ColorMatrixMath.IDENTITY, m, 0f)
        val atOne = ColorMatrixMath.lerp(ColorMatrixMath.IDENTITY, m, 1f)

        assertColorEquals(apply(ColorMatrixMath.IDENTITY, 33f, 120f, 240f), apply(atZero, 33f, 120f, 240f))
        assertColorEquals(apply(m, 33f, 120f, 240f), apply(atOne, 33f, 120f, 240f))
    }

    @Test
    fun `full desaturation produces a grey pixel`() {
        val out = apply(ColorMatrixMath.saturation(0f), 255f, 0f, 0f)
        assertEquals("channels should match", out[0], out[1], 0.5f)
        assertEquals("channels should match", out[1], out[2], 0.5f)
    }

    @Test
    fun `brightness shifts every channel by the same offset`() {
        val out = apply(ColorMatrixMath.brightness(0.5f), 10f, 20f, 30f)
        assertEquals(10f + 127.5f, out[0], 0.01f)
        assertEquals(20f + 127.5f, out[1], 0.01f)
        assertEquals(30f + 127.5f, out[2], 0.01f)
    }

    @Test
    fun `warmth raises red and lowers blue`() {
        val out = apply(ColorMatrixMath.temperature(1f), 100f, 100f, 100f)
        assertTrue("red should increase", out[0] > 100f)
        assertTrue("blue should decrease", out[2] < 100f)
    }

    // ------------------------------------------------------------- filter strength

    @Test
    fun `matrixAt zero is a no-op and one is full strength`() {
        val filter = FilterPresets.byId("clarendon")
        assertFalse("test filter should not be identity", filter.isIdentity)

        val off = apply(filter.matrixAt(0f), 40f, 160f, 90f)
        assertColorEquals(floatArrayOf(40f, 160f, 90f, 255f), off)

        val full = apply(filter.matrixAt(1f), 40f, 160f, 90f)
        assertColorEquals(apply(filter.matrix, 40f, 160f, 90f), full)
    }

    @Test
    fun `matrixAt half sits between the original and the filtered colour`() {
        val filter = FilterPresets.byId("clarendon")
        val original = floatArrayOf(40f, 160f, 90f)
        val full = apply(filter.matrix, original[0], original[1], original[2])
        val half = apply(filter.matrixAt(0.5f), original[0], original[1], original[2])

        for (i in 0..2) {
            assertEquals("channel $i", (original[i] + full[i]) / 2f, half[i], 0.05f)
        }
    }

    // ------------------------------------------------------------------ catalogue

    @Test
    fun `every preset is well formed`() {
        assertTrue("catalogue should not be empty", FilterPresets.ALL.size > 50)

        val ids = HashSet<String>()
        FilterPresets.ALL.forEach { f ->
            assertEquals("${f.id} must be a 4x5 matrix", 20, f.matrix.size)
            assertTrue("${f.id} must have a unique id", ids.add(f.id))
            assertTrue("${f.id} must have a name", f.name.isNotBlank())
            f.matrix.forEach { v ->
                assertFalse("${f.id} contains a non-finite value", v.isNaN() || v.isInfinite())
            }
        }
    }

    @Test
    fun `the default filter is the identity`() {
        assertTrue(FilterPresets.NONE.isIdentity)
    }

    @Test
    fun `every active category has at least one filter`() {
        FilterPresets.activeCategories().forEach { category ->
            assertTrue(
                "$category is listed as active but has no filters",
                FilterPresets.inCategory(category).isNotEmpty()
            )
        }
    }

    @Test
    fun `lookup by id falls back to the default filter`() {
        assertTrue(FilterPresets.byId("does-not-exist").isIdentity)
    }
}
