package com.camara.app.util

import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Tests for the separable box blur. These run on the JVM against the pure pixel maths,
 * so they need no Android Bitmap.
 */
class BlurTest {

    private fun argb(a: Int, r: Int, g: Int, b: Int) =
        (a shl 24) or (r shl 16) or (g shl 8) or b

    private fun channel(p: Int, shift: Int) = (p shr shift) and 0xFF

    @Test
    fun `radius zero returns an unchanged copy`() {
        val src = IntArray(16) { argb(255, it * 3, 100, 50) }
        val out = Blur.boxBlurPixels(src, 4, 4, 0)
        assertArrayEquals(src, out)
    }

    @Test
    fun `a constant image is left exactly as it was`() {
        // Clamped edges are what make this hold: a naive blur darkens the border.
        val src = IntArray(64) { argb(255, 120, 60, 200) }
        val out = Blur.boxBlurPixels(src, 8, 8, 3)
        out.forEach { p ->
            assertEquals("alpha", 255, channel(p, 24))
            assertEquals("red", 120, channel(p, 16))
            assertEquals("green", 60, channel(p, 8))
            assertEquals("blue", 200, channel(p, 0))
        }
    }

    @Test
    fun `alpha is preserved on a constant-alpha image`() {
        val src = IntArray(25) { argb(128, it % 256, 0, 0) }
        Blur.boxBlurPixels(src, 5, 5, 2).forEach { p ->
            assertEquals("alpha should not drift", 128, channel(p, 24))
        }
    }

    @Test
    fun `a single bright pixel spreads to its neighbours`() {
        val w = 9
        val h = 9
        val src = IntArray(w * h) { argb(255, 0, 0, 0) }
        src[4 * w + 4] = argb(255, 255, 255, 255)

        val out = Blur.boxBlurPixels(src, w, h, 1)

        assertTrue("centre should be bright", channel(out[4 * w + 4], 16) > 0)
        assertTrue("left neighbour should receive light", channel(out[4 * w + 3], 16) > 0)
        assertTrue("right neighbour should receive light", channel(out[4 * w + 5], 16) > 0)
        assertTrue("vertical neighbour should receive light", channel(out[3 * w + 4], 16) > 0)
        assertTrue("far pixel should stay dark", channel(out[0], 16) == 0)
    }

    @Test
    fun `the centre of a single bright pixel is dimmer than the source`() {
        val w = 9
        val src = IntArray(w * w) { argb(255, 0, 0, 0) }
        src[4 * w + 4] = argb(255, 255, 255, 255)
        val out = Blur.boxBlurPixels(src, w, w, 2)
        assertTrue(
            "blurring must reduce the peak",
            channel(out[4 * w + 4], 16) < 255
        )
    }

    @Test
    fun `a larger radius spreads further`() {
        val w = 21
        val src = IntArray(w * w) { argb(255, 0, 0, 0) }
        src[10 * w + 10] = argb(255, 255, 255, 255)

        val small = Blur.boxBlurPixels(src, w, w, 1)
        val large = Blur.boxBlurPixels(src, w, w, 6)

        assertTrue(
            "radius 6 should reach a pixel radius 1 cannot",
            channel(large[10 * w + 15], 16) > channel(small[10 * w + 15], 16)
        )
    }

    @Test
    fun `output size always matches the input`() {
        val w = 7
        val h = 5
        val src = IntArray(w * h) { argb(255, 10, 20, 30) }
        assertEquals(w * h, Blur.boxBlurPixels(src, w, h, 2).size)
    }
}
