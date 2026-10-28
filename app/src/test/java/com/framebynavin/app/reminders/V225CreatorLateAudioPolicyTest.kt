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
}
