package com.framebynavin.app.reminders

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class V276ReminderMusicMixPolicyTest {
    @Test fun audibleBaselineAndExactTwentyFivePercentDucking() {
        val before = ReminderMusicMixPolicy.level(speaking = false, hasSpoken = false)
        val during = ReminderMusicMixPolicy.level(speaking = true, hasSpoken = true)
        assertTrue(before > 0.60f)
        assertEquals(before * .75f, during, .00001f)
    }

    @Test fun afterVoiceRisesByFiftyPercentFromDuckedLevelWithoutClipping() {
        val during = ReminderMusicMixPolicy.level(speaking = true, hasSpoken = true)
        val after = ReminderMusicMixPolicy.level(speaking = false, hasSpoken = true)
        assertEquals(during * 1.50f, after, .00001f)
        assertTrue(after > during && after <= 1f)
        assertTrue(ReminderMusicMixPolicy.RETURN_TAIL_MILLIS >= 1_500L)
    }
}
