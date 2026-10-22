package com.camara.app.video

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/** When an overlay is on screen, which drives both the preview and the export. */
class VideoOverlayTest {

    private fun overlay(startMs: Float, endMs: Float) = VideoOverlay(
        id = 1L,
        kind = VideoOverlay.Kind.TEXT,
        text = "Hello",
        startMs = startMs,
        endMs = endMs,
    )

    @Test
    fun `an overlay with no range is always visible`() {
        val o = overlay(0f, Float.MAX_VALUE)
        assertTrue(o.isVisibleAt(0f))
        assertTrue(o.isVisibleAt(5_000f))
        assertTrue(o.isVisibleAt(600_000f))
    }

    @Test
    fun `a ranged overlay is visible only inside its window, edges included`() {
        val o = overlay(1_000f, 3_000f)
        assertFalse("before", o.isVisibleAt(999f))
        assertTrue("at the start", o.isVisibleAt(1_000f))
        assertTrue("in the middle", o.isVisibleAt(2_000f))
        assertTrue("at the end", o.isVisibleAt(3_000f))
        assertFalse("after", o.isVisibleAt(3_001f))
    }

    @Test
    fun `a zero length range still shows at its instant`() {
        val o = overlay(2_000f, 2_000f)
        assertTrue(o.isVisibleAt(2_000f))
        assertFalse(o.isVisibleAt(2_001f))
    }

    @Test
    fun `full length is reported only when it covers the whole clip`() {
        assertTrue(overlay(0f, Float.MAX_VALUE).isFullLength(10_000f))
        assertTrue(overlay(0f, 10_000f).isFullLength(10_000f))
        assertFalse("starts late", overlay(500f, 10_000f).isFullLength(10_000f))
        assertFalse("ends early", overlay(0f, 9_000f).isFullLength(10_000f))
    }

    @Test
    fun `defaults place a new overlay centred and full length`() {
        val o = VideoOverlay(id = 2L, kind = VideoOverlay.Kind.STICKER, glyph = "🔥")
        assertTrue(o.isFullLength(5_000f))
        assertTrue(o.x in 0f..1f)
        assertTrue(o.y in 0f..1f)
        assertTrue("a default size should be visible but not fill the frame", o.sizeFraction in 0.02f..0.4f)
    }
}
