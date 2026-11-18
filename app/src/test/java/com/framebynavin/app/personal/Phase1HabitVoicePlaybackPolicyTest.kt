package com.framebynavin.app.personal

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/** Pure timing contracts; real Android TTS still requires owner-device QA. */
class Phase1HabitVoicePlaybackPolicyTest {
    @Test fun completesExactlyThreeAnnouncementsAndStops() {
        assertEquals(3, Phase1HabitVoicePlaybackPolicy.REPEAT_COUNT)
        assertEquals(1, Phase1HabitVoicePlaybackPolicy.nextIndex(0))
        assertEquals(2, Phase1HabitVoicePlaybackPolicy.nextIndex(1))
        assertNull(Phase1HabitVoicePlaybackPolicy.nextIndex(2))
        assertNull(Phase1HabitVoicePlaybackPolicy.nextIndex(-1))
        assertNull(Phase1HabitVoicePlaybackPolicy.nextIndex(3))
    }

    @Test fun insertsAudibleGapInsteadOfImmediateRepeat() {
        assertEquals(5_000L, Phase1HabitVoicePlaybackPolicy.REPEAT_GAP_MILLIS)
        assertTrue(Phase1HabitVoicePlaybackPolicy.REPEAT_GAP_MILLIS >=
            PersonalPlaybackPolicy.VOICE_REPEAT_GAP_MILLIS)
    }

    @Test fun watchdogAllowsColdStartThreeLongSentencesAndTwoGaps() {
        val necessary = 10_000L + 3L * 45_000L +
            2L * Phase1HabitVoicePlaybackPolicy.REPEAT_GAP_MILLIS
        assertTrue(Phase1HabitVoicePlaybackPolicy.WATCHDOG_MILLIS >= necessary)
        assertTrue(Phase1HabitVoicePlaybackPolicy.WATCHDOG_MILLIS > 30_000L)
    }

    @Test fun explicitlySelectedFiveAnnouncementsUseFourFullGaps() {
        assertEquals(1, Phase1HabitVoicePlaybackPolicy.nextIndex(0, 5))
        assertEquals(4, Phase1HabitVoicePlaybackPolicy.nextIndex(3, 5))
        assertNull(Phase1HabitVoicePlaybackPolicy.nextIndex(4, 5))
        val fiveLongSentences = 10_000L + 5L * 45_000L + 4L * 60_000L
        assertTrue(Phase1HabitVoicePlaybackPolicy.watchdogMillis(5, 60_000L) >=
            fiveLongSentences)
        assertEquals(5_000L, Phase1HabitVoicePlaybackPolicy.gapMillis(2_500L))
        assertEquals(60_000L, Phase1HabitVoicePlaybackPolicy.gapMillis(60_000L))
    }

    @Test fun ordinaryPersonalSettingsRemainUntouched() {
        assertEquals(3, PersonalPlaybackPolicy.VOICE_REPEAT_COUNT)
        assertEquals(2_500L, PersonalPlaybackPolicy.VOICE_REPEAT_GAP_MILLIS)
    }
}
