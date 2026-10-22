package com.camara.app.editor

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Undo/redo semantics, including the coalescing window that makes a slider drag undo in
 * one step.
 *
 * Every test drives the clock explicitly. With the real clock, two consecutive `record`
 * calls land in the same millisecond and coalesce — which is the intended behaviour, but
 * it makes a test that expects two separate steps flaky. Hence the injected clock.
 */
class EditHistoryTest {

    private class Clock {
        var now = 1_000L
        fun advance(ms: Long) {
            now += ms
        }
    }

    private class Fixture(val clock: Clock = Clock(), limit: Int = 60, coalesceMs: Long = 700L) {
        val history = EditHistory<String>(limit, coalesceMs, clock::now)

        /** Records a value, then moves the clock past the coalescing window. */
        fun recordSeparately(value: String) {
            history.record(value)
            clock.advance(5_000)
        }
    }

    @Test
    fun `nothing to undo or redo on a fresh history`() {
        val h = EditHistory<String>()
        assertFalse(h.canUndo)
        assertFalse(h.canRedo)
        assertNull(h.undo("a"))
        assertNull(h.redo("a"))
    }

    @Test
    fun `undo returns the state before the change`() {
        val f = Fixture()
        f.recordSeparately("a")
        f.recordSeparately("b")

        assertEquals("b", f.history.undo("c"))
        assertEquals("a", f.history.undo("b"))
        assertFalse(f.history.canUndo)
    }

    @Test
    fun `redo walks forward again`() {
        val f = Fixture()
        f.recordSeparately("a")
        f.recordSeparately("b")

        assertEquals("b", f.history.undo("c"))
        assertTrue(f.history.canRedo)
        assertEquals("c", f.history.redo("b"))
        assertEquals("b", f.history.redo("a"))
        assertFalse(f.history.canRedo)
    }

    @Test
    fun `a new change after an undo drops the redo branch`() {
        val f = Fixture()
        f.recordSeparately("a")
        f.recordSeparately("b")
        f.history.undo("c")
        assertTrue(f.history.canRedo)

        f.history.record("b2")
        assertFalse("redo must be cleared by a new edit", f.history.canRedo)
    }

    @Test
    fun `rapid changes coalesce into a single undo step`() {
        val clock = Clock()
        val h = EditHistory<String>(coalesceMs = 700L, clock = clock::now)

        h.record("start")     // first entry
        clock.advance(50)
        h.record("mid-1")     // same drag: coalesced, not pushed
        clock.advance(50)
        h.record("mid-2")     // coalesced
        clock.advance(50)
        h.record("mid-3")     // coalesced

        // One undo lands on the state before the whole drag.
        assertEquals("start", h.undo("end"))
        assertFalse("the drag must undo in one step", h.canUndo)
    }

    @Test
    fun `a pause starts a new undo step`() {
        val clock = Clock()
        val h = EditHistory<String>(coalesceMs = 700L, clock = clock::now)

        h.record("first")
        clock.advance(50)
        h.record("during-drag")

        clock.advance(5_000)   // user let go and did something else
        h.record("second")

        assertEquals("second", h.undo("current"))
        assertEquals("first", h.undo("current"))
        assertFalse(h.canUndo)
    }

    @Test
    fun `the very first record is always pushed`() {
        val h = EditHistory<String>(coalesceMs = 700L, clock = Clock().let { c -> { c.now } })
        h.record("only")
        assertTrue(h.canUndo)
        assertEquals("only", h.undo("now"))
    }

    @Test
    fun `history is capped at the limit`() {
        val f = Fixture(limit = 3)
        listOf("a", "b", "c", "d", "e").forEach { f.recordSeparately(it) }

        // The last three records survive: e, d, c.
        assertEquals("e", f.history.undo("f"))
        assertEquals("d", f.history.undo("e"))
        assertEquals("c", f.history.undo("d"))
        assertFalse(f.history.canUndo)
    }

    @Test
    fun `clear forgets everything`() {
        val f = Fixture()
        f.recordSeparately("a")
        f.recordSeparately("b")
        f.history.undo("c")

        f.history.clear()

        assertFalse(f.history.canUndo)
        assertFalse(f.history.canRedo)
        assertNull(f.history.undo("c"))
    }
}
