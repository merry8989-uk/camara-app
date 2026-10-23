package com.camara.app.video

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * The timeline model behind multi-clip editing: trimming, ordering, and mapping a
 * position on the whole timeline back to a clip.
 */
class VideoTimelineTest {

    private fun clip(id: Long, durationMs: Float, start: Float = 0f, end: Float = Float.MAX_VALUE) =
        TimelineClip(id = id, uri = "file://clip$id", sourceDurationMs = durationMs, trimStartMs = start, trimEndMs = end)

    private val three = VideoTimeline(
        listOf(clip(1, 1_000f), clip(2, 2_000f), clip(3, 500f))
    )

    // ------------------------------------------------------------------ trimming

    @Test
    fun `an untrimmed clip runs to its source duration`() {
        val c = clip(1, 4_000f)
        assertEquals(0f, c.startMs(), 0.01f)
        assertEquals(4_000f, c.endMs(), 0.01f)
        assertEquals(4_000f, c.durationMs, 0.01f)
    }

    @Test
    fun `trimming shortens the clip`() {
        val c = clip(1, 4_000f, start = 1_000f, end = 3_000f)
        assertEquals(1_000f, c.startMs(), 0.01f)
        assertEquals(3_000f, c.endMs(), 0.01f)
        assertEquals(2_000f, c.durationMs, 0.01f)
    }

    @Test
    fun `trim is clamped to the source and cannot invert`() {
        val past = clip(1, 1_000f).trimmed(startMs = -500f, endMs = 5_000f)
        assertEquals(0f, past.startMs(), 0.01f)
        assertEquals(1_000f, past.endMs(), 0.01f)

        // An end before the start collapses to a zero-length clip rather than going negative.
        val inverted = clip(1, 1_000f).trimmed(startMs = 800f, endMs = 200f)
        assertEquals(800f, inverted.startMs(), 0.01f)
        assertEquals(800f, inverted.endMs(), 0.01f)
        assertEquals(0f, inverted.durationMs, 0.01f)
    }

    // ---------------------------------------------------------------- total length

    @Test
    fun `total duration sums the trimmed clips`() {
        assertEquals(3_500f, three.totalDurationMs, 0.01f)

        val trimmed = three.replace(clip(2, 2_000f, start = 500f, end = 1_500f))
        assertEquals(2_000f, trimmed.totalDurationMs, 0.01f)
    }

    @Test
    fun `an empty timeline has no duration`() {
        assertEquals(0f, VideoTimeline().totalDurationMs, 0.01f)
        assertTrue(VideoTimeline().isEmpty)
    }

    // ------------------------------------------------------------------- locating

    @Test
    fun `a position maps to the right clip and offset`() {
        // clip 1 covers 0–1000, clip 2 covers 1000–3000, clip 3 covers 3000–3500.
        val first = three.locate(0f)!!
        assertEquals(1L, first.clip.id)
        assertEquals(0f, first.localMs, 0.01f)

        val boundary = three.locate(1_000f)!!
        assertEquals("the boundary belongs to the next clip", 2L, boundary.clip.id)
        assertEquals(0f, boundary.localMs, 0.01f)

        val middle = three.locate(2_000f)!!
        assertEquals(2L, middle.clip.id)
        assertEquals(1_000f, middle.localMs, 0.01f)

        val last = three.locate(3_200f)!!
        assertEquals(3L, last.clip.id)
        assertEquals(200f, last.localMs, 0.01f)
    }

    @Test
    fun `locating accounts for the trim`() {
        val trimmed = VideoTimeline(listOf(clip(1, 1_000f, start = 400f, end = 900f)))
        val pos = trimmed.locate(100f)!!
        assertEquals(500f, pos.localMs, 0.01f)
    }

    @Test
    fun `locating past the end clamps to the last frame`() {
        val pos = three.locate(99_999f)!!
        assertEquals(3L, pos.clip.id)
        assertEquals(500f, pos.localMs, 0.01f)
    }

    @Test
    fun `locating on an empty timeline gives nothing`() {
        assertNull(VideoTimeline().locate(0f))
    }

    @Test
    fun `a zero length clip does not break locating`() {
        val withEmpty = VideoTimeline(listOf(clip(1, 0f), clip(2, 1_000f)))
        val pos = withEmpty.locate(0f)!!
        assertEquals(2L, pos.clip.id)
    }

    // ------------------------------------------------------------------ arranging

    @Test
    fun `adding appends`() {
        val t = VideoTimeline().add(clip(1, 100f)).add(clip(2, 200f))
        assertEquals(listOf(1L, 2L), t.clips.map { it.id })
    }

    @Test
    fun `removing takes the clip out`() {
        val t = three.remove(2L)
        assertEquals(listOf(1L, 3L), t.clips.map { it.id })
        assertEquals(1_500f, t.totalDurationMs, 0.01f)
    }

    @Test
    fun `removing an unknown id changes nothing`() {
        assertEquals(three.clips, three.remove(99L).clips)
    }

    @Test
    fun `moving shifts a clip and stops at the ends`() {
        assertEquals(listOf(2L, 1L, 3L), three.moved(1L, +1).clips.map { it.id })
        assertEquals(listOf(1L, 3L, 2L), three.moved(2L, +1).clips.map { it.id })
        assertEquals(listOf(1L, 2L, 3L), three.moved(1L, -1).clips.map { it.id })
        assertEquals(listOf(1L, 2L, 3L), three.moved(3L, +1).clips.map { it.id })
    }

    @Test
    fun `replacing a clip keeps its place`() {
        val t = three.replace(clip(2, 2_000f, start = 1_000f, end = 2_000f))
        assertEquals(listOf(1L, 2L, 3L), t.clips.map { it.id })
        assertEquals(2_500f, t.totalDurationMs, 0.01f)
    }

    @Test
    fun `startOf reports where each clip begins`() {
        assertEquals(0f, three.startOf(1L), 0.01f)
        assertEquals(1_000f, three.startOf(2L), 0.01f)
        assertEquals(3_000f, three.startOf(3L), 0.01f)
    }
}
