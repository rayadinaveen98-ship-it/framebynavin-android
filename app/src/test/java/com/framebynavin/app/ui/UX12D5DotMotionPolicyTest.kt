package com.framebynavin.app.ui

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Stage-only policy checks. Do NOT execute until founder authorizes the
 * combined UX12 QA and APK cycle.
 */
class UX12D5DotMotionPolicyTest {

    @Test fun idleFieldMovesWithoutMicrophoneIntensity() {
        val lowWave = UX12D5DotMotionPolicy.idleExpansion(
            wave = 0f, attenuation = 1f, enabled = true,
        )
        val highWave = UX12D5DotMotionPolicy.idleExpansion(
            wave = 1f, attenuation = 1f, enabled = true,
        )
        assertTrue(lowWave > 0f)
        assertTrue(highWave > lowWave)
        assertEquals(0f, UX12D5DotMotionPolicy.reaction(
            speechIntensity = 0f, wave = 1f, attenuation = 1f,
        ), 0f)
    }

    @Test fun reducedMotionRemovesIndependentIdleExpansion() {
        assertEquals(0f, UX12D5DotMotionPolicy.idleExpansion(
            wave = 1f, attenuation = 1f, enabled = false,
        ), 0f)
    }

    @Test fun speechReactionUsesRealRmsAndIsBounded() {
        assertEquals(0f, UX12D5DotMotionPolicy.reaction(
            speechIntensity = 0f, wave = 1f, attenuation = 1f,
        ), 0f)
        assertTrue(UX12D5DotMotionPolicy.reaction(
            speechIntensity = 0.75f, wave = 0.8f, attenuation = 0.7f,
        ) > 0f)
        assertEquals(1f, UX12D5DotMotionPolicy.reaction(
            speechIntensity = 2f, wave = 2f, attenuation = 2f,
        ), 0f)
    }
}
