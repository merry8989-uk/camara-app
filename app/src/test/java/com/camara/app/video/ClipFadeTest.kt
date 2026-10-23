package com.camara.app.video

import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * The fade ramp. A wrong factor here is a video that darkens in the wrong place, so the
 * shape of the ramp is pinned down at both ends and in the middle.
 */
class ClipFadeTest {

    // Two clips: 0–2000 and 2000–5000.
    private val spans = listOf(
        ClipSpan(id = 1, startMs = 0f, endMs = 2_000f),
        ClipSpan(id = 2, startMs = 2_000f, endMs = 5_000f),
    )

    private fun fade(ms: Float) = ClipFade(spans, ms)

    @Test
    fun `no fade leaves everything untouched`() {
        val f = fade(0f)
        listOf(0f, 500f, 2_000f, 4_999f).forEach {
            assertEquals("at $it", 1f, f.factorAt(it), 0.001f)
        }
    }

    @Test
    fun `the first clip starts black and reaches full`() {
        val f = fade(500f)
        assertEquals("black at the very start", 0f, f.factorAt(0f), 0.001f)
        assertEquals("half way through the fade", 0.5f, f.factorAt(250f), 0.001f)
        assertEquals("full once the fade is done", 1f, f.factorAt(500f), 0.001f)
        assertEquals("still full mid-clip", 1f, f.factorAt(1_000f), 0.001f)
    }

    @Test
    fun `the last clip ends black`() {
        val f = fade(500f)
        assertEquals("full before the fade starts", 1f, f.factorAt(4_500f), 0.001f)
        assertEquals("half way out", 0.5f, f.factorAt(4_750f), 0.001f)
        assertEquals("black at the very end", 0f, f.factorAt(5_000f), 0.001f)
    }

    @Test
    fun `the join is black on both sides`() {
        val f = fade(500f)
        // The end of clip 1 and the start of clip 2 are both the join.
        assertEquals("clip 1 fades out into the join", 0f, f.factorAt(2_000f), 0.001f)
        assertEquals("clip 2 fades in out of the join", 0f, f.factorAt(2_000f), 0.001f)
        assertEquals("just before the join", 0.1f, f.factorAt(1_950f), 0.001f)
        assertEquals("just after the join", 0.1f, f.factorAt(2_050f), 0.001f)
    }

    @Test
    fun `the middle of a clip is untouched`() {
        val f = fade(500f)
        assertEquals(1f, f.factorAt(1_000f), 0.001f)
        assertEquals(1f, f.factorAt(3_000f), 0.001f)
    }

    @Test
    fun `a clip shorter than two fades dips and recovers without going negative`() {
        // One 400 ms clip with a 1000 ms fade: both ramps overlap, so the peak is 0.2.
        val tiny = ClipFade(listOf(ClipSpan(1, 0f, 400f)), 1_000f)
        assertEquals("black at the start", 0f, tiny.factorAt(0f), 0.001f)
        assertEquals("the peak is the shorter ramp", 0.2f, tiny.factorAt(200f), 0.001f)
        assertEquals("and it comes back down", 0f, tiny.factorAt(400f), 0.001f)
        (0..400 step 10).forEach { t ->
            val v = tiny.factorAt(t.toFloat())
            assertEquals("in range at $t", v.coerceIn(0f, 1f), v, 0.0001f)
        }
    }

    @Test
    fun `outside the timeline nothing is darkened`() {
        val f = fade(500f)
        assertEquals(1f, f.factorAt(-100f), 0.001f)
        assertEquals(1f, f.factorAt(9_999f), 0.001f)
    }

    @Test
    fun `an empty timeline is untouched`() {
        assertEquals(1f, ClipFade(emptyList(), 500f).factorAt(0f), 0.001f)
    }

    @Test
    fun `the matrix scales rgb and leaves alpha alone`() {
        val m = fade(500f).matrixAt(250f) // factor 0.5
        assertEquals("red", 0.5f, m[0], 0.001f)
        assertEquals("green", 0.5f, m[5], 0.001f)
        assertEquals("blue", 0.5f, m[10], 0.001f)
        assertEquals("alpha untouched", 1f, m[15], 0.001f)
        assertEquals("16 values for a 4x4", 16, m.size)
        // Every off-diagonal term must be zero, or the fade would mix the channels.
        listOf(1, 2, 3, 4, 6, 7, 8, 9, 11, 12, 13, 14).forEach {
            assertEquals("m[$it] must be 0", 0f, m[it], 0.001f)
        }
    }

    @Test
    fun `every choice has a label`() {
        ClipFade.CHOICES.forEach {
            assertEquals(true, ClipFade.label(it).isNotBlank())
        }
        assertEquals("Off", ClipFade.label(0f))
    }
}
