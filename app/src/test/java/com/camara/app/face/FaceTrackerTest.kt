package com.camara.app.face

import androidx.compose.ui.geometry.Offset
import com.camara.app.lenses.FaceState
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * The tracking engine: does it steady the pose, does it hold through a dropout, and does
 * it stop holding when the face is really gone.
 */
class FaceTrackerTest {

    private fun face(cx: Float, cy: Float = 0.5f, w: Float = 0.4f, h: Float = 0.5f) = FaceState(
        cx = cx,
        cy = cy,
        w = w,
        h = h,
        imageAspect = 0.75f,
        leftEye = Offset(cx - 0.06f, cy - 0.08f),
        rightEye = Offset(cx + 0.06f, cy - 0.08f)
    )

    @Test
    fun `the first detection is passed straight through`() {
        val tracker = FaceTracker()
        val result = tracker.update(face(0.5f), nowMs = 0L)
        assertNotNull(result)
        assertEquals(0.5f, result!!.cx, 0.0001f)
    }

    @Test
    fun `a jittering pose is smoothed`() {
        val tracker = FaceTracker(smoothing = 0.5f)
        tracker.update(face(0.50f), nowMs = 0L)
        // ML Kit wobbles by a couple of percent each frame.
        tracker.update(face(0.54f), nowMs = 33L)
        val after = tracker.update(face(0.46f), nowMs = 66L)!!

        // Raw would be 0.46. Half-blended from 0.52 it should sit between, and much
        // closer to the previous pose than to the new one.
        assertTrue("smoothed value ${after.cx} should be > 0.46", after.cx > 0.46f)
        assertTrue("smoothed value ${after.cx} should be < 0.52", after.cx < 0.52f)
    }

    @Test
    fun `smoothing of 1 follows the detection exactly`() {
        val tracker = FaceTracker(smoothing = 1f)
        tracker.update(face(0.5f), nowMs = 0L)
        val after = tracker.update(face(0.52f), nowMs = 33L)!!
        assertEquals(0.52f, after.cx, 0.0001f)
    }

    @Test
    fun `smoothing of 0 never moves`() {
        val tracker = FaceTracker(smoothing = 0f)
        tracker.update(face(0.5f), nowMs = 0L)
        val after = tracker.update(face(0.51f), nowMs = 33L)!!
        assertEquals(0.5f, after.cx, 0.0001f)
    }

    @Test
    fun `landmarks are smoothed too`() {
        val tracker = FaceTracker(smoothing = 0.5f)
        tracker.update(face(0.5f), nowMs = 0L)
        val after = tracker.update(face(0.5f, cy = 0.6f), nowMs = 33L)!!
        val left = after.leftEye!!
        // Raw eye y would be 0.52; half-blended from 0.42 it lands near 0.47.
        assertTrue("eye y ${left.y} should be between the two", left.y > 0.42f && left.y < 0.52f)
    }

    @Test
    fun `a dropout holds the last pose`() {
        val tracker = FaceTracker(holdMs = 300L)
        tracker.update(face(0.5f), nowMs = 0L)
        val held = tracker.update(null, nowMs = 100L)
        assertNotNull("a short dropout must not blink the sticker out", held)
        assertEquals(0.5f, held!!.cx, 0.0001f)
        assertTrue(tracker.holding)
    }

    @Test
    fun `a long dropout releases the face`() {
        val tracker = FaceTracker(holdMs = 300L)
        tracker.update(face(0.5f), nowMs = 0L)
        assertNull(tracker.update(null, nowMs = 301L))
        assertNull("and it stays gone", tracker.update(null, nowMs = 400L))
        assertTrue(!tracker.holding)
    }

    @Test
    fun `the hold window is inclusive at the boundary`() {
        val tracker = FaceTracker(holdMs = 300L)
        tracker.update(face(0.5f), nowMs = 0L)
        assertNotNull(tracker.update(null, nowMs = 300L))
        assertNull(tracker.update(null, nowMs = 301L))
    }

    @Test
    fun `a detection after a dropout resumes without a jump`() {
        val tracker = FaceTracker(smoothing = 0.5f, holdMs = 300L)
        tracker.update(face(0.5f), nowMs = 0L)
        tracker.update(null, nowMs = 50L)
        val resumed = tracker.update(face(0.5f), nowMs = 100L)!!
        assertEquals(0.5f, resumed.cx, 0.0001f)
        assertTrue(!tracker.holding)
    }

    @Test
    fun `a big jump snaps instead of gliding`() {
        val tracker = FaceTracker(smoothing = 0.2f)
        tracker.update(face(0.2f), nowMs = 0L)
        // 0.6 away with a face 0.4 wide is well past the snap threshold.
        val after = tracker.update(face(0.8f), nowMs = 33L)!!
        assertEquals("must snap, not drag across the screen", 0.8f, after.cx, 0.0001f)
    }

    @Test
    fun `a small move is not treated as a jump`() {
        val tracker = FaceTracker(smoothing = 0.5f)
        tracker.update(face(0.50f), nowMs = 0L)
        val after = tracker.update(face(0.55f), nowMs = 33L)!!
        assertTrue("should blend, not snap", after.cx < 0.55f)
    }

    @Test
    fun `reset forgets everything`() {
        val tracker = FaceTracker()
        tracker.update(face(0.5f), nowMs = 0L)
        tracker.reset()
        assertNull(tracker.current)
        assertNull(tracker.update(null, nowMs = 10L))
    }

    @Test
    fun `contours blend point by point`() {
        val a = face(0.5f).copy(faceContour = listOf(Offset(0.1f, 0.1f), Offset(0.2f, 0.2f)))
        val b = face(0.5f).copy(faceContour = listOf(Offset(0.3f, 0.1f), Offset(0.4f, 0.2f)))
        val blended = FaceTracker.blend(a, b, 0.5f)
        assertEquals(0.2f, blended.faceContour[0].x, 0.0001f)
        assertEquals(0.3f, blended.faceContour[1].x, 0.0001f)
    }

    @Test
    fun `contours of different lengths take the new shape whole`() {
        val a = face(0.5f).copy(faceContour = listOf(Offset(0.1f, 0.1f)))
        val b = face(0.5f).copy(faceContour = listOf(Offset(0.3f, 0.1f), Offset(0.4f, 0.2f)))
        val blended = FaceTracker.blend(a, b, 0.5f)
        assertEquals("must not blend two different shapes", 2, blended.faceContour.size)
        assertEquals(0.3f, blended.faceContour[0].x, 0.0001f)
    }

    @Test
    fun `a missing landmark is taken from whichever pose has it`() {
        val withEye = face(0.5f)
        val without = face(0.5f).copy(leftEye = null)
        assertNotNull(FaceTracker.blend(without, withEye, 0.5f).leftEye)
        assertNotNull(FaceTracker.blend(withEye, without, 0.5f).leftEye)
    }

    @Test
    fun `distance is measured in normalised units`() {
        assertEquals(0.5f, FaceTracker.distance(face(0f), face(0.5f)), 0.0001f)
    }
}
