package com.camara.app.core

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * The ownership rule behind bitmap recycling. A mistake here is either a leak (OOM on a
 * large export) or a crash (drawing a recycled bitmap), so it is pinned down by tests.
 */
class ReleasingChainTest {

    /** Plain class on purpose: identity equality is what the chain relies on. */
    private class Item(val id: Int)

    private class Recorder {
        val released = mutableListOf<Item>()
        fun release(item: Item) {
            assertFalse("released the same item twice", released.any { it === item })
            released += item
        }
    }

    @Test
    fun `the original is never released`() {
        val rec = Recorder()
        val original = Item(0)
        val chain = ReleasingChain(original, rec::release)

        chain.advance(Item(1))
        chain.advance(Item(2))
        chain.advance(Item(3))

        assertFalse("original must survive", rec.released.any { it === original })
        assertEquals(listOf(1, 2), rec.released.map { it.id })
    }

    @Test
    fun `the final value is returned and never released`() {
        val rec = Recorder()
        val chain = ReleasingChain(Item(0), rec::release)
        val last = Item(9)

        chain.advance(Item(1))
        chain.advance(last)

        assertSame(last, chain.value)
        assertFalse("the result must survive", rec.released.any { it === last })
    }

    @Test
    fun `a stage that returns its input changes nothing`() {
        val rec = Recorder()
        val original = Item(0)
        val chain = ReleasingChain(original, rec::release)

        // Nothing has been produced yet, so nothing may be released.
        chain.advance(original)
        assertTrue(rec.released.isEmpty())
        assertSame(original, chain.value)

        // After a real stage, re-passing the current value still releases nothing.
        val produced = Item(1)
        chain.advance(produced)
        chain.advance(produced)
        assertTrue("re-passing the current value must release nothing", rec.released.isEmpty())
        assertSame(produced, chain.value)
    }

    @Test
    fun `every intermediate is released exactly once`() {
        val rec = Recorder()
        val chain = ReleasingChain(Item(0), rec::release)
        val stages = (1..6).map { Item(it) }

        stages.forEach { chain.advance(it) }

        // 0 is the caller's, 6 is the result: five intermediates released.
        assertEquals(5, rec.released.size)
        assertEquals(listOf(1, 2, 3, 4, 5), rec.released.map { it.id })
        assertSame(stages.last(), chain.value)
    }

    @Test
    fun `a no-op stage in the middle does not break the chain`() {
        val rec = Recorder()
        val chain = ReleasingChain(Item(0), rec::release)

        val a = Item(1)
        val b = Item(2)
        chain.advance(a)   // 1 produced
        chain.advance(a)   // no-op
        chain.advance(b)   // 2 produced, 1 released

        assertEquals(listOf(1), rec.released.map { it.id })
        assertSame(b, chain.value)
    }
}
