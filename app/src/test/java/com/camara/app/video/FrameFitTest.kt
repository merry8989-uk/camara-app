package com.camara.app.video

import org.junit.Assert.assertEquals
import org.junit.Test

/** Letterbox maths for overlay positioning. */
class FrameFitTest {

    private fun assertRect(expected: FloatArray, actual: FloatArray, delta: Float = 0.01f) {
        assertEquals("left", expected[0], actual[0], delta)
        assertEquals("top", expected[1], actual[1], delta)
        assertEquals("right", expected[2], actual[2], delta)
        assertEquals("bottom", expected[3], actual[3], delta)
    }

    @Test
    fun `a matching aspect ratio fills the box exactly`() {
        assertRect(
            floatArrayOf(0f, 0f, 400f, 300f),
            FrameFit.rect(400f, 300f, 1920f, 1440f)
        )
    }

    @Test
    fun `a taller video is pillarboxed with equal bars`() {
        // 9:16 into a 1:1 box: full height, bars left and right.
        val r = FrameFit.rect(400f, 400f, 1080f, 1920f)
        assertRect(floatArrayOf(87.5f, 0f, 312.5f, 400f), r)
        assertEquals("bars must match", r[0], 400f - r[2], 0.01f)
    }

    @Test
    fun `a wider video is letterboxed with equal bars`() {
        // 16:9 into a 1:1 box: full width, bars top and bottom.
        val r = FrameFit.rect(400f, 400f, 1920f, 1080f)
        assertRect(floatArrayOf(0f, 87.5f, 400f, 312.5f), r)
        assertEquals("bars must match", r[1], 400f - r[3], 0.01f)
    }

    @Test
    fun `the fitted rect always keeps the source aspect ratio`() {
        listOf(
            Triple(1080f, 1920f, 0.5625f),
            Triple(1920f, 1080f, 1.7778f),
            Triple(1080f, 1080f, 1f),
        ).forEach { (vw, vh, expectedAspect) ->
            val r = FrameFit.rect(360f, 640f, vw, vh)
            val aspect = (r[2] - r[0]) / (r[3] - r[1])
            assertEquals("aspect for ${vw}x$vh", expectedAspect, aspect, 0.01f)
        }
    }

    @Test
    fun `the fitted rect never escapes the box`() {
        listOf(1080f to 1920f, 1920f to 1080f, 4000f to 1000f).forEach { (vw, vh) ->
            val r = FrameFit.rect(360f, 640f, vw, vh)
            assertEquals("left", 0f, r[0], 0.01f)
            assertEquals("top", 0f, r[1], 0.01f)
            assertEquals("right", 360f, r[2], 0.01f)
            assertEquals("bottom", 640f, r[3], 0.01f)
        }
    }

    @Test
    fun `degenerate sizes fall back to the whole box rather than dividing by zero`() {
        assertRect(floatArrayOf(0f, 0f, 100f, 50f), FrameFit.rect(100f, 50f, 0f, 0f))
        assertRect(floatArrayOf(0f, 0f, 0f, 0f), FrameFit.rect(0f, 0f, 1080f, 1920f))
    }
}
