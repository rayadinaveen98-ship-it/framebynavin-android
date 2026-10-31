package com.framebynavin.app.personal

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/** Pure playback schedule assertions. Actual TTS and speakers need device acceptance. */
class V271PersonalVoiceRepeatTest {
    @Test fun personalVoiceHasThreeCompleteUtterances() {
        assertEquals(3, PersonalPlaybackPolicy.VOICE_REPEAT_COUNT)
        assertEquals(1, PersonalPlaybackPolicy.nextVoiceIndex(0))
        assertEquals(2, PersonalPlaybackPolicy.nextVoiceIndex(1))
        assertNull(PersonalPlaybackPolicy.nextVoiceIndex(2))
        assertEquals(2_500L, PersonalPlaybackPolicy.VOICE_REPEAT_GAP_MILLIS)
    }
    @Test fun personalWatchdogCanCoverThreeSpokenAnnouncements() {
        assertTrue(PersonalPlaybackPolicy.VOICE_MAX_MILLIS >= 60_000L)
        assertTrue(PersonalPlaybackPolicy.VOICE_MAX_MILLIS <= 120_000L)
    }
}
