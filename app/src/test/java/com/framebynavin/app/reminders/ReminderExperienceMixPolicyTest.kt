package com.framebynavin.app.reminders

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Staged for the consolidated owner-approved R2 QA campaign; never triggers
 * a per-edit JVM/Gradle run.
 */
class ReminderExperienceMixPolicyTest {
    @Test fun speechHasStrongDuckAndPostSpeechReturnsToOriginalGain() {
        assertEquals(0.68f, ReminderMusicMixPolicy.BEFORE_VOICE, 0.0001f)
        assertEquals(0.18f, ReminderMusicMixPolicy.level(true, false), 0.0001f)
        assertEquals(0.18f, ReminderMusicMixPolicy.level(true, true), 0.0001f)
        assertEquals(ReminderMusicMixPolicy.BEFORE_VOICE,
            ReminderMusicMixPolicy.level(false, true), 0.0001f)
        assertEquals(ReminderMusicMixPolicy.BEFORE_VOICE,
            ReminderMusicMixPolicy.level(false, false), 0.0001f)
        assertTrue(ReminderMusicMixPolicy.DURING_VOICE <
            ReminderMusicMixPolicy.BEFORE_VOICE * 0.30f)
    }

    @Test fun fadeFitsInsideExistingCreatorPersonalAndChallengeMusicTails() {
        assertEquals(850L, ReminderMusicMixPolicy.tailFadeDelay(1_500L))
        assertEquals(1_250L,
            ReminderMusicMixPolicy.tailFadeDelay(ReminderMusicMixPolicy.RETURN_TAIL_MILLIS))
        assertEquals(0L, ReminderMusicMixPolicy.tailFadeDelay(250L))
    }

    @Test fun musicAndAlarmGainAreBoundedAndDoNotRequireStreamVolume() {
        assertEquals(0f, ReminderMusicMixPolicy.interpolatedGain(0f, 1f, -1f), 0f)
        assertEquals(1f, ReminderMusicMixPolicy.interpolatedGain(0f, 1f, 2f), 0f)
        assertEquals(0.5f, ReminderMusicMixPolicy.interpolatedGain(0f, 1f, 0.5f), 0f)
        assertEquals(0.42f, ReminderAlarmMixPolicy.SPEECH_GAIN, 0.0001f)
        assertEquals(1f, ReminderAlarmMixPolicy.NORMAL_GAIN, 0.0001f)
        assertTrue(ReminderAlarmMixPolicy.SPEECH_GAIN > 0f)
        assertTrue(ReminderAlarmMixPolicy.NATURAL_TIMEOUT_FADE_MILLIS in 250L..400L)
        assertEquals(0f, ReminderAlarmMixPolicy.interpolate(1f, 0f, 1f), 0f)
    }
}
