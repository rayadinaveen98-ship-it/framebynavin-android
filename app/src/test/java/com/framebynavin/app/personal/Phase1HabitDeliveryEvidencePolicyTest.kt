package com.framebynavin.app.personal

import org.junit.Assert.assertEquals
import org.junit.Test

/** Pure classification checks: never infer an Android or owner action that was not recorded. */
class Phase1HabitDeliveryEvidencePolicyTest {
    private val due = 1_900_000_000_000L
    private val id = "challenge-a"

    private fun evidence(
        stage: Phase1HabitDeliveryStage,
        challengeId: String = id,
        at: Long = due,
        observedAt: Long = due + 50L,
    ) = Phase1HabitDeliveryEvidence(challengeId, at, stage, observedAt)

    private fun health(
        now: Long,
        vararg events: Phase1HabitDeliveryEvidence,
    ) = Phase1HabitDeliveryEvidencePolicy.health(events.toList(), id, due, now)

    @Test fun noRecordMeansNoClaimOfDelivery() {
        assertEquals(Phase1HabitDeliveryHealth.NOT_RECORDED, health(due + 5000L))
    }

    @Test fun exactAndInexactSchedulesDoNotProveTheOsFired() {
        assertEquals(Phase1HabitDeliveryHealth.ARMED,
            health(due - 10L, evidence(Phase1HabitDeliveryStage.ARMED_EXACT)))
        assertEquals(Phase1HabitDeliveryHealth.OS_DELIVERY_UNCONFIRMED,
            health(due + 10L, evidence(Phase1HabitDeliveryStage.ARMED_INEXACT)))
    }

    @Test fun firedDoesNotMeanNotificationWasPosted() {
        assertEquals(Phase1HabitDeliveryHealth.FIRED_PRESENTATION_UNCONFIRMED,
            health(due + 10L, evidence(Phase1HabitDeliveryStage.OS_FIRED)))
    }

    @Test fun appOrChannelPermissionBlocksAreExplicit() {
        for (stage in listOf(Phase1HabitDeliveryStage.APP_NOTIFICATIONS_BLOCKED,
                Phase1HabitDeliveryStage.CHANNEL_BLOCKED)) {
            assertEquals(Phase1HabitDeliveryHealth.PERMISSION_BLOCKED,
                health(due + 10L, evidence(Phase1HabitDeliveryStage.OS_FIRED),
                    evidence(stage)))
        }
    }

    @Test fun postingIsNotVoicePlayback() {
        assertEquals(Phase1HabitDeliveryHealth.NOTIFICATION_POSTED,
            health(due + 10L, evidence(Phase1HabitDeliveryStage.NOTIFICATION_POSTED)))
        assertEquals(Phase1HabitDeliveryHealth.VOICE_REQUESTED,
            health(due + 10L, evidence(Phase1HabitDeliveryStage.NOTIFICATION_POSTED),
                evidence(Phase1HabitDeliveryStage.VOICE_REQUESTED)))
        assertEquals(Phase1HabitDeliveryHealth.VOICE_UNAVAILABLE,
            health(due + 10L, evidence(Phase1HabitDeliveryStage.VOICE_REQUESTED),
                evidence(Phase1HabitDeliveryStage.VOICE_TTS_UNAVAILABLE)))
        assertEquals(Phase1HabitDeliveryHealth.VOICE_SPEAKING,
            health(due + 10L, evidence(Phase1HabitDeliveryStage.VOICE_SPEAK_STARTED)))
    }

    @Test fun unrelatedChallengeAndUnrelatedDayCannotContaminateThisOccurrence() {
        assertEquals(Phase1HabitDeliveryHealth.NOT_RECORDED,
            health(due + 10L, evidence(Phase1HabitDeliveryStage.NOTIFICATION_POSTED, "other"),
                evidence(Phase1HabitDeliveryStage.OS_FIRED, id, due + 86_400_000L)))
    }

    @Test fun stalePendingIntentDoesNotCountAsSuccessfulDelivery() {
        assertEquals(Phase1HabitDeliveryHealth.STALE_IGNORED,
            health(due + 10L, evidence(Phase1HabitDeliveryStage.OS_FIRED),
                evidence(Phase1HabitDeliveryStage.STALE_IGNORED)))
    }
    @Test fun laterSpeechFailureOverridesEarlierSpeakingEvidence() {
        assertEquals(Phase1HabitDeliveryHealth.VOICE_UNAVAILABLE,
            health(due + 400L,
                evidence(Phase1HabitDeliveryStage.VOICE_REQUESTED, observedAt = due + 50L),
                evidence(Phase1HabitDeliveryStage.VOICE_SPEAK_STARTED, observedAt = due + 100L),
                evidence(Phase1HabitDeliveryStage.VOICE_SPEAK_FAILED, observedAt = due + 200L)))
    }

    @Test fun laterSuccessfulStartCanSupersedeEarlierFailedAttempt() {
        assertEquals(Phase1HabitDeliveryHealth.VOICE_SPEAKING,
            health(due + 400L,
                evidence(Phase1HabitDeliveryStage.VOICE_SPEAK_FAILED, observedAt = due + 100L),
                evidence(Phase1HabitDeliveryStage.VOICE_SPEAK_STARTED, observedAt = due + 200L)))
    }

    @Test fun tiedVoiceEvidenceTimestampsFavorRecordedFailure() {
        assertEquals(Phase1HabitDeliveryHealth.VOICE_UNAVAILABLE,
            health(due + 400L,
                evidence(Phase1HabitDeliveryStage.VOICE_SPEAK_STARTED, observedAt = due + 100L),
                evidence(Phase1HabitDeliveryStage.VOICE_TTS_UNAVAILABLE, observedAt = due + 100L)))
    }

    @Test fun otherChallengeVoiceFailureCannotMaskThisOccurrence() {
        assertEquals(Phase1HabitDeliveryHealth.VOICE_SPEAKING,
            health(due + 400L,
                evidence(Phase1HabitDeliveryStage.VOICE_SPEAK_STARTED, observedAt = due + 100L),
                evidence(Phase1HabitDeliveryStage.VOICE_SPEAK_FAILED, "unrelated", observedAt = due + 200L),
                evidence(Phase1HabitDeliveryStage.VOICE_TTS_UNAVAILABLE, id,
                    at = due + 86_400_000L, observedAt = due + 200L)))
    }

}
