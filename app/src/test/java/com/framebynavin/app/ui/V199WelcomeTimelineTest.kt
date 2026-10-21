package com.framebynavin.app.ui

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/** Contract tests for the single-authoritative, 3,000ms V199 welcome clock. */
class V199WelcomeTimelineTest {
    @Test fun exactThreeSecondBoundaryIncludesTransition() {
        assertEquals(3_000L, V199WelcomeTimeline.DURATION_MS)
        assertEquals(2_800L, V199WelcomeTimeline.HOLD_END_MS)
        assertEquals(1f, V199WelcomeTimeline.progress(3_000L, 2_800L, 3_000L), 0.0001f)
        assertEquals(0f, V199WelcomeTimeline.progress(2_800L, 2_800L, 3_000L), 0.0001f)
    }

    @Test fun eachMilestoneOrderedAndConvergenceFinishesBeforeTitleLock() {
        assertTrue(V199WelcomeTimeline.AWAKEN_END_MS < V199WelcomeTimeline.CONVERGE_END_MS)
        assertTrue(V199WelcomeTimeline.CONVERGE_END_MS < V199WelcomeTimeline.TITLE_LOCK_END_MS)
        assertTrue(V199WelcomeTimeline.TITLE_LOCK_END_MS < V199WelcomeTimeline.HOLD_END_MS)
        assertTrue(V199WelcomeTimeline.HOLD_END_MS < V199WelcomeTimeline.DURATION_MS)
        for (s in listOf(0f, .2f, .5f, 1f)) {
            val early = V199WelcomeTimeline.converge(0L, s)
            val late = V199WelcomeTimeline.converge(1_900L, s)
            assertEquals(0f, early, 0.0001f)
            assertEquals(1f, late, 0.0001f)
        }
    }

    @Test fun convergenceIsMonotonicAndClamped() {
        val times = (0L..2_000L step 25L).toList()
        for (stagger in listOf(0f, .23f, .48f, .77f, 1f)) {
            var previous = 0f
            for (t in times) {
                val v = V199WelcomeTimeline.converge(t, stagger)
                assertTrue(v in 0f..1f)
                assertTrue("convergence must not go backwards", v >= previous)
                previous = v
            }
        }
        assertEquals(0f, V199WelcomeTimeline.progress(-100L, 0, 350), 0.0001f)
        assertEquals(1f, V199WelcomeTimeline.progress(4_000L, 0, 350), 0.0001f)
    }

    @Test fun widgetAndNotificationEntryRemainsInstant() {
        assertFalse(V151LaunchPolicy.shouldShowCinematicIdent(externalLaunch = true))
        assertTrue(V151LaunchPolicy.shouldShowCinematicIdent(externalLaunch = false))
    }
}
