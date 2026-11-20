package com.framebynavin.app.reminders

import com.framebynavin.app.personal.Phase1HabitAlertKind
import com.framebynavin.app.personal.RX01ChallengePresentationPolicy
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Founder ALL DONE integration tests: fail-closed occurrence gating,
 * presentation reversibility and Android Full-Screen Intent policy.
 * Pure policy tests; no Android context, backend or personal record mutations.
 */
class RX01IntegrationPolicyTest {
    private val key = "creator:VOICE:task-1:occurrence-1"

    @Test fun entranceRequiresLiveOwnerTokenAndFreshSourceVerification() {
        assertTrue(RX01EntryGatePolicy.canStart(
            key, true, RX01GatePhase.ENTRANCE, true
        ))
        assertFalse(RX01EntryGatePolicy.canStart(
            "", true, RX01GatePhase.ENTRANCE, true
        ))
        assertFalse(RX01EntryGatePolicy.canStart(
            key, false, RX01GatePhase.ENTRANCE, true
        ))
        assertFalse(RX01EntryGatePolicy.canStart(
            key, true, RX01GatePhase.ENTRANCE, false
        ))
        for (phase in listOf(RX01GatePhase.VERIFYING,
                RX01GatePhase.EXITING, RX01GatePhase.MAIN)) {
            assertFalse(RX01EntryGatePolicy.canStart(key, true, phase, true))
        }
    }

    @Test fun mainRemainsLockedUntilFinalVerifiedExitingFence() {
        assertTrue(RX01EntryGatePolicy.canFinish(
            key, true, RX01GatePhase.EXITING, true
        ))
        assertFalse(RX01EntryGatePolicy.canFinish(
            key, true, RX01GatePhase.EXITING, false
        ))
        assertFalse(RX01EntryGatePolicy.canFinish(
            "", true, RX01GatePhase.EXITING, true
        ))
        assertFalse(RX01EntryGatePolicy.canFinish(
            key, false, RX01GatePhase.EXITING, true
        ))
        for (phase in listOf(RX01GatePhase.ENTRANCE,
                RX01GatePhase.VERIFYING, RX01GatePhase.MAIN)) {
            assertFalse(RX01EntryGatePolicy.canFinish(key, true, phase, true))
        }
    }

    @Test fun powerSaveOrReducedMotionSkipsTransitionOnly() {
        assertEquals(320, RX01EntryGatePolicy.exitDurationMillis(
            motionEnabled = true, lowPower = false
        ))
        assertEquals(0, RX01EntryGatePolicy.exitDurationMillis(
            motionEnabled = false, lowPower = false
        ))
        assertEquals(0, RX01EntryGatePolicy.exitDurationMillis(
            motionEnabled = true, lowPower = true
        ))
    }

    @Test fun visualPreferencePrecedenceNeverErasesLegacyChoices() {
        assertEquals(RX01ScenePresentation.FLUID,
            RX01ReminderScenePolicy.creatorOrPersonal(
                rx01Enabled = true, premiereEnabled = true))
        assertEquals(RX01ScenePresentation.PREMIERE,
            RX01ReminderScenePolicy.creatorOrPersonal(
                rx01Enabled = false, premiereEnabled = true))
        assertEquals(RX01ScenePresentation.LEGACY,
            RX01ReminderScenePolicy.creatorOrPersonal(
                rx01Enabled = false, premiereEnabled = false))
        assertTrue(RX01ReminderScenePolicy.challengeVisualEnabled(true, true))
        assertFalse(RX01ReminderScenePolicy.challengeVisualEnabled(false, true))
        assertFalse(RX01ReminderScenePolicy.challengeVisualEnabled(true, false))
        assertFalse(RX01ReminderScenePolicy.challengeFullScreenAllowed(false, true))
        assertFalse(RX01ReminderScenePolicy.challengeFullScreenAllowed(true, false))
        assertTrue(RX01ReminderScenePolicy.challengeFullScreenAllowed(true, true))
    }

    @Test fun challengeOnlyVoiceAndAlarmAreEligibleForVisualEntrance() {
        assertTrue(RX01ChallengePresentationPolicy.eligibleForVisual(
            Phase1HabitAlertKind.ALARM, 1784500000000L))
        assertTrue(RX01ChallengePresentationPolicy.eligibleForVisual(
            Phase1HabitAlertKind.VOICE, 1784500000000L))
        assertFalse(RX01ChallengePresentationPolicy.eligibleForVisual(
            Phase1HabitAlertKind.QUIET, 1784500000000L))
        assertFalse(RX01ChallengePresentationPolicy.eligibleForVisual(
            Phase1HabitAlertKind.ALARM, 0L))
    }

    @Test fun noChallengeFullScreenWithoutAllFiveEligibilityGates() {
        val valid = { kind: Phase1HabitAlertKind,
                owner: Boolean, os: Boolean, channel: Boolean, at: Long ->
            RX01ChallengePresentationPolicy.automaticFullScreen(
                kind = kind,
                isOwnerOptedIn = owner,
                hasOsEntitlement = os,
                alarmChannelHigh = channel,
                scheduledAt = at
            )
        }
        val due = 1784500000000L
        assertTrue(valid(Phase1HabitAlertKind.ALARM, true, true, true, due))
        assertFalse(valid(Phase1HabitAlertKind.QUIET, true, true, true, due))
        assertFalse(valid(Phase1HabitAlertKind.VOICE, true, true, true, due))
        assertFalse(valid(Phase1HabitAlertKind.ALARM, false, true, true, due))
        assertFalse(valid(Phase1HabitAlertKind.ALARM, true, false, true, due))
        assertFalse(valid(Phase1HabitAlertKind.ALARM, true, true, false, due))
        assertFalse(valid(Phase1HabitAlertKind.ALARM, true, true, true, 0L))
    }

    @Test fun allThreeReminderMainBackgroundsAreDifferentFromEntrance() {
        for (domain in RX01ReminderDomain.entries) {
            val entry = RX01FluidTheme.palette(domain, RX01SurfacePhase.ENTRANCE)
            val main = RX01FluidTheme.palette(domain, RX01SurfacePhase.MAIN)
            assertFalse(domain.name + " must have separate main backdrop",
                entry.baseTop == main.baseTop && entry.baseBottom == main.baseBottom)
        }
    }
}
