package com.framebynavin.app.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Test

class CreatorFocusSessionPolicyTest {
    private fun session() = CreatorFocusSessionPolicy.create(
        projectId = "creator-project",
        stageId = "script",
        targetMinutes = 25,
        atWallMillis = 1_700_000_000_000L,
        id = "focus-test",
    )

    @Test fun newSessionDoesNotStartUntilExplicitAction() {
        val created = session()
        assertEquals(CreatorFocusState.READY, created.state)
        assertEquals(25L * 60_000L, CreatorFocusSessionPolicy.remainingMillis(created, 400L, 2))
        assertEquals(0L, CreatorFocusSessionPolicy.activeMillis(created, 400L, 2))
        assertEquals("script", created.stageId)
    }

    @Test fun startPauseResumeAndFinishCountOnlyRealMonotonicActiveTime() {
        val running = CreatorFocusSessionPolicy.start(session(), 100_000L, 7)
        assertEquals(90_000L, CreatorFocusSessionPolicy.activeMillis(running, 190_000L, 7))
        val paused = CreatorFocusSessionPolicy.pause(running, 190_000L, 7)
        assertEquals(CreatorFocusState.PAUSED, paused.state)
        assertEquals(90_000L, CreatorFocusSessionPolicy.activeMillis(paused, 999_000L, 7))
        val resumed = CreatorFocusSessionPolicy.start(paused, 2_000_000L, 7)
        assertEquals(120_000L, CreatorFocusSessionPolicy.activeMillis(resumed, 2_030_000L, 7))
        val finished = CreatorFocusSessionPolicy.finish(resumed, 2_030_000L, 7, 1_700_000_030_000L)
        assertEquals(120_000L, finished.accruedActiveMillis)
        assertEquals(CreatorFocusState.FINISHED, finished.state)
        assertEquals("creator-project", finished.projectId)
    }

    @Test fun sameBootProcessRecreationUsesMonotonicAnchorWithoutFakeNewStart() {
        val running = CreatorFocusSessionPolicy.start(session(), 1_000L, 5)
        val serializedEquivalent = running.copy()
        assertEquals(serializedEquivalent,
            CreatorFocusSessionPolicy.recover(serializedEquivalent, 95_000L, 5))
        assertEquals(94_000L, CreatorFocusSessionPolicy.activeMillis(running, 95_000L, 5))
    }

    @Test fun rebootRecoveryPausesAtLastVerifiedCheckpointWithoutUsingWallClock() {
        val running = CreatorFocusSessionPolicy.start(session(), 100L, 5)
        val checkpoint = CreatorFocusSessionPolicy.checkpoint(running, 90_100L, 5)
        val restored = CreatorFocusSessionPolicy.recover(checkpoint, 500L, 6)
        assertEquals(CreatorFocusState.PAUSED, restored.state)
        assertEquals(90_000L, restored.accruedActiveMillis)
        assertTrue(restored.recoveredAfterReboot)
        assertEquals(90_000L, CreatorFocusSessionPolicy.activeMillis(restored, 2_000_000L, 6))
    }

    @Test fun elapsedResetWithinBootFailsClosedAndDoesNotOvercount() {
        val running = CreatorFocusSessionPolicy.start(session(), 40_000L, 9)
        val recovered = CreatorFocusSessionPolicy.recover(running, 5_000L, 9)
        assertEquals(CreatorFocusState.PAUSED, recovered.state)
        assertEquals(0L, recovered.accruedActiveMillis)
        assertTrue(recovered.recoveredAfterReboot)
    }

    @Test fun checkpointPreventsDoubleCountingAcrossRepeatedLifecycleCalls() {
        val started = CreatorFocusSessionPolicy.start(session(), 100L, 9)
        val once = CreatorFocusSessionPolicy.checkpoint(started, 20_100L, 9)
        val twice = CreatorFocusSessionPolicy.checkpoint(once, 20_100L, 9)
        val three = CreatorFocusSessionPolicy.checkpoint(twice, 30_100L, 9)
        assertEquals(30_000L, three.accruedActiveMillis)
        assertEquals(30_000L, CreatorFocusSessionPolicy.activeMillis(three, 30_100L, 9))
    }

    @Test fun timerNeverCountsBeyondTargetAndCannotResumeAfterTarget() {
        val running = CreatorFocusSessionPolicy.start(session(), 1L, 1)
        assertEquals(1_500_000L,
            CreatorFocusSessionPolicy.activeMillis(running, Long.MAX_VALUE, 1))
        val atTarget = CreatorFocusSessionPolicy.pause(running, 3_000_000L, 1)
        assertEquals(1_500_000L, atTarget.accruedActiveMillis)
        try {
            CreatorFocusSessionPolicy.start(atTarget, 3_001_000L, 1)
            fail("Target reached: cannot resume automatically")
        } catch (_: IllegalArgumentException) { }
    }

    @Test fun completionAndDiscardDoNotChangeProjectIdentityOrCreateStageReceipts() {
        val created = session()
        val discarded = CreatorFocusSessionPolicy.discard(created, 1_700_000_003_000L)
        assertEquals(CreatorFocusState.DISCARDED, discarded.state)
        assertEquals(0L, discarded.accruedActiveMillis)
        assertEquals("script", discarded.stageId)
        val finished = CreatorFocusSessionPolicy.finish(created, 0L, 1, 1_700_000_003_000L)
        assertEquals(CreatorFocusState.FINISHED, finished.state)
        assertEquals(0L, finished.accruedActiveMillis)
        assertFalse(finished.recoveredAfterReboot)
    }

    @Test fun malformedSessionsAndUnsupportedDurationsAreRejected() {
        listOf(0, 4, 26, 121).forEach { minutes ->
            try {
                CreatorFocusSessionPolicy.create("p", "script", minutes, 100L)
                fail("Unexpected duration accepted: $minutes")
            } catch (_: IllegalArgumentException) { }
        }
        try {
            session().copy(state = CreatorFocusState.RUNNING).checked()
            fail("Running timer without monotonic anchor was accepted")
        } catch (_: IllegalArgumentException) { }
    }
}
