package com.framebynavin.app.reminders

import org.junit.Assert.*
import org.junit.Test

class V197ImmersiveParticlePolicyTest {
    @Test fun backgroundCoversEntireScreenInBothPerformanceModes() {
        for (lowPower in listOf(false, true)) {
            val dots = V197AmbientGeometry.build(lowPower)
            assertEquals(
                if (lowPower) V197AmbientGeometry.LOW_POWER_COUNT else V197AmbientGeometry.NORMAL_COUNT,
                dots.size,
            )
            assertTrue(dots.all { it.x in 0f..1f && it.y in 0f..1f })
            assertTrue(dots.all { it.phase.isFinite() && it.speed > 0f && it.palette in 0..3 })
            assertEquals(4, dots.map { it.palette }.distinct().size)
            // Background must occupy top/bottom and left/right, unlike V195's icon-only dots.
            assertTrue(dots.any { it.x < .06f && it.y < .06f })
            assertTrue(dots.any { it.x > .94f && it.y > .94f })
            assertTrue(dots.any { it.x < .06f && it.y > .94f })
            assertTrue(dots.any { it.x > .94f && it.y < .06f })
        }
    }

    @Test fun ambientGeometryIsDeterministicAndNonOverlapping() {
        val first = V197AmbientGeometry.build()
        assertEquals(first, V197AmbientGeometry.build())
        assertEquals(first.size, first.map { it.x to it.y }.distinct().size)
        assertEquals(4, first.map { it.palette }.distinct().size)
    }

    @Test fun aNormalSceneNeverAutoRevealsWhileReducedMotionOpensControlsDirectly() {
        assertFalse(V197ReminderRevealPolicy.startRevealed(motionEnabled = true))
        assertTrue(V197ReminderRevealPolicy.startRevealed(motionEnabled = false))
    }
}
