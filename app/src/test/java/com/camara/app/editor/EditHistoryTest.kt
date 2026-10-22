package com.camara.app.editor

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Undo/redo semantics, including the coalescing window that makes a slider drag undo in
 * one step. The clock is driven by the test, so nothing here waits on wall time.
 */
class EditHistoryTest {

    private class Clock {
        var now = 1_000L
        fun advance(ms: Long) {
            now += ms
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
        val h = EditHistory<String>()
        h.record("a")
        h.record("b")

        assertEquals("b", h.undo("c"))
        assertEquals("a", h.undo("b"))
        assertFalse(h.canUndo)
    }

    @Test
    fun `redo walks forward again`() {
        val h = EditHistory<String>()
        h.record("a")
        h.record("b")

        assertEquals("b", h.undo("c"))
        assertTrue(h.canRedo)
        assertEquals("c", h.redo("b"))
        assertEquals("b", h.redo("a"))
        assertFalse(h.canRedo)
    }

    @Test
    fun `a new change after an undo drops the redo branch`() {
        val h = EditHistory<String>()
        h.record("a")
        h.record("b")
        h.undo("c")
        assertTrue(h.canRedo)

        h.record("b2")
        assertFalse("redo must be cleared by a new edit", h.canRedo)
    }

    @Test
    fun `rapid changes coalesce into a single undo step`() {
        val clock = Clock()
        val h = EditHistory<String>(coalesceMs = 700L, clock = clock::now)

        h.record("start")      // first entry
        clock.advance(50)
        h.record("mid-1")      // same drag: coalesced, not pushed
        clock.advance(50)
        h.record("mid-2")      // coalesced
        clock.advance(50)
        h.record("mid-3")      // coalesced

        // One undo should land on the state before the whole drag.
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
        val clock = Clock()
        val h = EditHistory<String>(coalesceMs = 700L, clock = clock::now)
        h.record("only")
        assertTrue(h.canUndo)
        assertEquals("only", h.undo("now"))
    }

    @Test
    fun `history is capped at the limit`() {
        val clock = Clock()
        val h = EditHistory<String>(limit = 3, coalesceMs = 0L, clock = clock::now)

        listOf("a", "b", "c", "d", "e").forEach {
            h.record(it)
            clock.advance(10)
        }

        // The last three records survive: e, d, c.
        assertEquals("e", h.undo("f"))
        assertEquals("d", h.undo("e"))
        assertEquals("c", h.undo("d"))
        assertFalse(h.canUndo)
    }

    @Test
    fun `clear forgets everything`() {
        val h = EditHistory<String>()
        h.record("a")
        h.record("b")
        h.undo("c")

        h.clear()

        assertFalse(h.canUndo)
        assertFalse(h.canRedo)
        assertNull(h.undo("c"))
    }
}
