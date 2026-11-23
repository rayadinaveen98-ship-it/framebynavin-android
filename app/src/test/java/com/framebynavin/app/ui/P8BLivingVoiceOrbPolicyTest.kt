package com.framebynavin.app.ui

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/** Prepared source only. Do NOT run before founder's consolidated QA approval. */
class P8BLivingVoiceOrbPolicyTest {
    @Test fun idleNeverPretendsToContainLiveMicrophoneEnergy() {
        assertEquals(0f, P8BVoiceOrbPolicy.intensity(false, 9f), .001f)
    }

    @Test fun ordinarySpeechProducesVisibleReaction() {
        val weak = P8BVoiceOrbPolicy.intensity(true, 1.0f)
        val speaking = P8BVoiceOrbPolicy.intensity(true, 5.0f)
        val loud = P8BVoiceOrbPolicy.intensity(true, 9f)
        assertTrue(weak >= 0f)
        assertTrue(speaking > .40f)
        assertTrue(loud <= 1f)
        assertTrue(loud >= speaking)
    }

    @Test fun invalidAndOutOfRangeRmsCannotBreakGraphics() {
        assertEquals(0f, P8BVoiceOrbPolicy.intensity(true, Float.NaN), .001f)
        assertEquals(0f, P8BVoiceOrbPolicy.intensity(true, Float.NEGATIVE_INFINITY), .001f)
        assertEquals(0f, P8BVoiceOrbPolicy.intensity(true, Float.POSITIVE_INFINITY), .001f)
        assertEquals(0f, P8BVoiceOrbPolicy.intensity(true, -1000f), .001f)
        assertEquals(1f, P8BVoiceOrbPolicy.intensity(true, 10000f), .001f)
    }

    @Test fun voiceModeChipsAreMutuallyExclusiveAndToggleOff() {
        assertEquals(P8BVoiceMode.DICTATE,
            P8BVoiceModePolicy.choose(P8BVoiceMode.OFF,
                P8BVoiceMode.DICTATE, false))
        assertEquals(P8BVoiceMode.RECORD,
            P8BVoiceModePolicy.choose(P8BVoiceMode.DICTATE,
                P8BVoiceMode.RECORD, false))
        assertEquals(P8BVoiceMode.OFF,
            P8BVoiceModePolicy.choose(P8BVoiceMode.RECORD,
                P8BVoiceMode.RECORD, false))
    }

    @Test fun recordingCannotBeAbandonedByModeSwitch() {
        assertEquals(P8BVoiceMode.RECORD,
            P8BVoiceModePolicy.choose(P8BVoiceMode.RECORD,
                P8BVoiceMode.DICTATE, true))
        assertEquals(P8BVoiceMode.RECORD,
            P8BVoiceModePolicy.choose(P8BVoiceMode.RECORD,
                P8BVoiceMode.OFF, true))
    }
}
