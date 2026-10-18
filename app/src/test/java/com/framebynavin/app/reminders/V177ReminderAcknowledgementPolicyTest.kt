package com.framebynavin.app.reminders

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class V177ReminderAcknowledgementPolicyTest {
    @Test fun responseIsFinalEvenAfterDelayedAudioTimeout() {
        assertFalse(ReminderAckTransitions.allowed(ReminderAckState.RESPONDED, ReminderAckState.UNANSWERED))
        assertFalse(ReminderAckTransitions.allowed(ReminderAckState.RESPONDED, ReminderAckState.NOTIFICATION_POSTED))
    }

    @Test fun lateExplicitResponseCanResolveAnUnansweredOccurrence() {
        assertTrue(ReminderAckTransitions.allowed(ReminderAckState.UNANSWERED, ReminderAckState.RESPONDED))
        assertFalse(ReminderAckTransitions.allowed(ReminderAckState.UNANSWERED, ReminderAckState.ALARM_ACTIVE))
    }

    @Test fun failedVoicePlaybackMayRecoverThroughNotificationButNotBlindlyRetry() {
        assertTrue(ReminderAckTransitions.allowed(ReminderAckState.DELIVERY_FAILED, ReminderAckState.NOTIFICATION_POSTED))
        assertFalse(ReminderAckTransitions.allowed(ReminderAckState.DELIVERY_FAILED, ReminderAckState.VOICE_ACTIVE))
        assertTrue(ReminderAckTransitions.allowed(ReminderAckState.NOTIFICATION_POSTED, ReminderAckState.UNANSWERED))
    }

    @Test fun anObservationIsNotAReadReceiptOrNewAttempt() {
        assertTrue(ReminderAckTransitions.allowed(ReminderAckState.ATTEMPTED, ReminderAckState.NOTIFICATION_POSTED))
        assertTrue(ReminderAckTransitions.allowed(ReminderAckState.VOICE_ACTIVE, ReminderAckState.VOICE_SPEAKING))
        assertTrue(ReminderAckTransitions.allowed(ReminderAckState.VOICE_SPEAKING, ReminderAckState.VOICE_PLAYBACK_FINISHED))
        assertFalse(ReminderAckTransitions.allowed(ReminderAckState.ALARM_ACTIVE, ReminderAckState.ATTEMPTED))
        assertFalse(ReminderAckTransitions.allowed(ReminderAckState.NOTIFICATION_POSTED, ReminderAckState.NOTIFICATION_POSTED))
    }
}
