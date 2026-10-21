package com.framebynavin.app.reminders

import org.junit.Assert.assertFalse
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class V205NudgePausePolicyTest {
    private val hour = 60L * 60L * 1000L
    private val now = 1_800_000_000_000L

    @Test fun pauseStartsOnlyAfterExplicitUserAction() {
        assertFalse(V205NudgePausePolicy.isPaused(now, 0L))
        assertFalse(V205NudgePausePolicy.isPaused(now, now))
    }

    @Test fun sevenDaysAreExactlySevenDaysAndAutomaticallyExpire() {
        val until = V205NudgePausePolicy.pauseUntil(now)
        assertEquals(now + 7L * 24L * hour, until)
        assertTrue(V205NudgePausePolicy.isPaused(now, until))
        assertTrue(V205NudgePausePolicy.isPaused(until - 1, until))
        assertFalse(V205NudgePausePolicy.isPaused(until, until))
        assertFalse(V205NudgePausePolicy.isPaused(until + 1, until))
    }

    @Test fun badAndOverflowingClocksNeverCrashOrWrap() {
        assertEquals(0L, V205NudgePausePolicy.pauseUntil(0L))
        assertEquals(0L, V205NudgePausePolicy.pauseUntil(-20L))
        assertEquals(Long.MAX_VALUE, V205NudgePausePolicy.pauseUntil(Long.MAX_VALUE - 1L))
        assertFalse(V205NudgePausePolicy.isPaused(-1L, Long.MAX_VALUE))
    }
}
