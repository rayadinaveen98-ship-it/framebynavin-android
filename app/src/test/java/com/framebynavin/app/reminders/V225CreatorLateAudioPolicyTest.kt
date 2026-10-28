package com.framebynavin.app.reminders

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class V225CreatorLateAudioPolicyTest {
    private val original = 1_800_000_000_000L

    @Test fun exactTimelyReminderMayStartAudibleService() {
        assertTrue(V225CreatorLateAudioPolicy.allowAudible(original, original, true))
        assertTrue(V225CreatorLateAudioPolicy.allowAudible(original, original + 30_000L, true))
        assertTrue(V225CreatorLateAudioPolicy.allowAudible(
            original, original + V225CreatorLateAudioPolicy.MAX_AUDIBLE_DELAY_MILLIS, true,
        ))
    }

    @Test fun exactPermissionCannotReviveAnHoursOldAlarmOrVoice() {
        assertFalse(V225CreatorLateAudioPolicy.allowAudible(
            original, original + V225CreatorLateAudioPolicy.MAX_AUDIBLE_DELAY_MILLIS + 1L, true,
        ))
        assertFalse(V225CreatorLateAudioPolicy.allowAudible(original, original + 3_600_000L, true))
        assertFalse(V225CreatorLateAudioPolicy.allowAudible(original, original + 86_400_000L, true))
    }

    @Test fun inexactOrInvalidTimestampsNeverStartAudiblePlayback() {
        assertFalse(V225CreatorLateAudioPolicy.allowAudible(original, original, false))
        assertFalse(V225CreatorLateAudioPolicy.allowAudible(0L, original, true))
        assertFalse(V225CreatorLateAudioPolicy.allowAudible(original, original - 5_001L, true))
        assertTrue(V225CreatorLateAudioPolicy.allowAudible(original, original - 5_000L, true))
    }

    @Test fun expiredProjectDeadlineSuppressesVeryOldAndroidBroadcast() {
        val deadline = original + 60_000L
        val staleAt = deadline + 6L * 60L * 60L * 1000L + 1L
        assertFalse(V225CreatorLateAudioPolicy.shouldDeliverAtAll(original, deadline, staleAt))
        assertTrue(V225CreatorLateAudioPolicy.shouldDeliverAtAll(
            original, deadline, original + 90_000L,
        ))
    }

    @Test fun oldReminderRemainsRelevantUntilFutureProjectDeadline() {
        val deadline = original + 7L * 24L * 60L * 60L * 1000L
        val caughtUp = original + 2L * 24L * 60L * 60L * 1000L
        assertTrue(V225CreatorLateAudioPolicy.shouldDeliverAtAll(original, deadline, caughtUp))
        assertFalse(V225CreatorLateAudioPolicy.allowAudible(original, caughtUp, true))
        assertFalse(V225CreatorLateAudioPolicy.shouldDeliverAtAll(original, deadline, original - 5_001L))
    }

}
