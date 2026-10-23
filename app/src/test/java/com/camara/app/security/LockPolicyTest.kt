package com.camara.app.security

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/** When the app re-locks. Getting this wrong means either a lock that does nothing or one
 *  that fires while the user is in the middle of something. */
class LockPolicyTest {

    private val grace = 20_000L

    @Test
    fun `a cold start locks`() {
        assertTrue(LockPolicy.shouldRelock(nowMs = 1_000L, backgroundedAtMs = null, graceMs = grace))
    }

    @Test
    fun `coming straight back does not lock`() {
        assertTrue(
            "well inside the grace period",
            !LockPolicy.shouldRelock(nowMs = 1_000L + 2_000L, backgroundedAtMs = 1_000L, graceMs = grace)
        )
    }

    @Test
    fun `coming back after the grace period locks`() {
        assertTrue(
            LockPolicy.shouldRelock(nowMs = 1_000L + grace + 1L, backgroundedAtMs = 1_000L, graceMs = grace)
        )
    }

    @Test
    fun `exactly at the boundary it does not lock`() {
        // The boundary belongs to the grace period: the app has not been away *longer*
        // than the grace.
        assertFalse(
            LockPolicy.shouldRelock(nowMs = 1_000L + grace, backgroundedAtMs = 1_000L, graceMs = grace)
        )
    }

    @Test
    fun `a zero grace period always locks`() {
        assertTrue(LockPolicy.shouldRelock(nowMs = 1_000L, backgroundedAtMs = 1_000L, graceMs = 0L))
    }

    @Test
    fun `the default grace is a sensible length`() {
        // Long enough to survive a permission dialog, short enough to be a lock.
        assertTrue(LockPolicy.DEFAULT_GRACE_MS in 5_000L..120_000L)
        // A prompt that takes a few seconds must not re-lock.
        assertFalse(
            LockPolicy.shouldRelock(
                nowMs = LockPolicy.DEFAULT_GRACE_MS / 2,
                backgroundedAtMs = 0L,
            )
        )
    }
}
