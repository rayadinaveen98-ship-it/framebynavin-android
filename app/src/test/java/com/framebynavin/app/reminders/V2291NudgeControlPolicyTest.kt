package com.framebynavin.app.reminders

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class V2291NudgeControlPolicyTest {
    private val now = 1_800_000_000_000L

    @Test fun offByDefaultAndBlockedStatesRemainTruthful() {
        assertEquals(V2291NudgeControlState.OFF,
            V2291NudgeControlPolicy.status(false, true, 0L, now))
        assertEquals(V2291NudgeControlState.OFF,
            V2291NudgeControlPolicy.status(false, false, now + 1000, now))
        assertEquals(V2291NudgeControlState.BLOCKED,
            V2291NudgeControlPolicy.status(true, false, 0L, now))
        assertEquals(V2291NudgeControlState.ACTIVE,
            V2291NudgeControlPolicy.status(true, true, 0L, now))
    }

    @Test fun pauseSurvivesBackgroundSchedulingUntilBoundary() {
        val until = V205NudgePausePolicy.pauseUntil(now)
        assertEquals(now + V205NudgePausePolicy.PAUSE_MS, until)
        assertEquals(V2291NudgeControlState.PAUSED,
            V2291NudgeControlPolicy.status(true, true, until, now))
        assertEquals(V2291NudgeControlState.PAUSED,
            V2291NudgeControlPolicy.status(true, false, until, now))
        assertEquals(V2291NudgeControlState.ACTIVE,
            V2291NudgeControlPolicy.status(true, true, until, until))
    }

    @Test fun pauseNeverEnablesDisabledOptionalPrompts() {
        val paused = V205NudgePausePolicy.pauseUntil(now)
        assertEquals(V2291NudgeControlState.OFF,
            V2291NudgeControlPolicy.status(false, true, paused, now))
        assertFalse(V205NudgePausePolicy.isPaused(now, 0L))
        assertTrue(V205NudgePausePolicy.isPaused(now, paused))
    }
}
