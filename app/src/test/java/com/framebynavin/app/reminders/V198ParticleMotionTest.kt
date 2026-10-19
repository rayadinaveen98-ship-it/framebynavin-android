package com.framebynavin.app.reminders

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class V198ParticleMotionTest {
    @Test fun animationLoopsWithoutPositionOrColorDiscontinuities() {
        for (harmonic in 1..3) for (offset in listOf(0f, .7f, 1.9f, 3.8f)) {
            val n = harmonic.toFloat()
            assertEquals(
                V198ParticleMotion.wave(0f, n, offset),
                V198ParticleMotion.wave(V198ParticleMotion.PHASE_END, n, offset),
                .00001f,
            )
            assertEquals(
                V198ParticleMotion.colorMix(0f, n, offset),
                V198ParticleMotion.colorMix(V198ParticleMotion.PHASE_END, n, offset),
                .00001f,
            )
        }
    }

    @Test fun vibrantTransitionsRemainWithinColorBlendBounds() {
        for (speed in 1..3) for (i in 0..80) {
            val t = i * V198ParticleMotion.PHASE_END / 80f
            assertTrue(V198ParticleMotion.colorMix(t, speed.toFloat(), .71f) in 0f..1f)
        }
    }

    @Test fun iconAlwaysVisibleButVisiblyPulses() {
        val pulses = (0..100).map {
            V198ParticleMotion.iconPulse(it * V198ParticleMotion.PHASE_END / 100f)
        }
        assertTrue(pulses.minOrNull()!! >= .66f)
        assertTrue(pulses.maxOrNull()!! <= 1.001f)
        assertTrue(pulses.maxOrNull()!! - pulses.minOrNull()!! >= .31f)
        assertEquals(3_800, V198ParticleMotion.CYCLE_MILLIS)
    }

    @Test fun richerDensityRetainsLowPowerCeiling() {
        assertEquals(1_092, V197AmbientGeometry.NORMAL_COUNT)
        assertEquals(493, V197AmbientGeometry.LOW_POWER_COUNT)
        assertEquals(840, V195ParticleGeometry.DEFAULT_DOTS)
        assertEquals(440, V195ParticleGeometry.LOW_POWER_DOTS)
        assertTrue(V197AmbientGeometry.LOW_POWER_COUNT < V197AmbientGeometry.NORMAL_COUNT / 2)
    }
}
