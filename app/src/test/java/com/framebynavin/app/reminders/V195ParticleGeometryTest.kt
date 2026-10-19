package com.framebynavin.app.reminders

import org.junit.Assert.*
import org.junit.Test
import kotlin.math.hypot

class V195ParticleGeometryTest {
    @Test fun allScenesHaveExactDeterministicCountAndBoundedTargets() {
        for (glyph in V195ReminderGlyph.entries) {
            val first = V195ParticleGeometry.build(glyph)
            val again = V195ParticleGeometry.build(glyph)
            assertEquals(V195ParticleGeometry.DEFAULT_DOTS, first.size)
            assertEquals(first, again)
            assertTrue(first.all { it.x in -1f..1f && it.y in -1f..1f })
            assertTrue(first.all { it.scatterX in -1.4f..1.4f && it.scatterY in -1.4f..1.4f })
            assertTrue(first.all { it.phase.isFinite() && it.speed in .8f..3.0f })
            assertTrue(first.map { it.x to it.y }.distinct().size > first.size / 2)
        }
    }

    @Test fun clockContainsCircularRingAndTwoDifferentHands() {
        val p = V195ParticleGeometry.build(V195ReminderGlyph.CLOCK)
        assertTrue(p.count { hypot(it.x, it.y) > .86f } >= 200)
        assertTrue(p.any { it.y < -.65f && kotlin.math.abs(it.x) < .04f })
        assertTrue(p.any { it.x > .45f && it.y > .23f })
    }

    @Test fun microphoneHasCapsuleArchStemAndBase() {
        val p = V195ParticleGeometry.build(V195ReminderGlyph.MICROPHONE)
        assertTrue(p.any { it.y < -.75f })
        assertTrue(p.any { it.x > .49f && it.y > .1f })
        assertTrue(p.any { it.y > .88f && kotlin.math.abs(it.x) > .25f })
        assertTrue(p.any { it.y > .72f && kotlin.math.abs(it.x) < .01f })
    }

    @Test fun lowerPowerModeRemainsIdentifiableWithoutChangingGlyph() {
        for (glyph in V195ReminderGlyph.entries) {
            val p = V195ParticleGeometry.build(glyph, V195ParticleGeometry.LOW_POWER_DOTS)
            assertEquals(160, p.size)
            assertTrue(p.all { it.x.isFinite() && it.y.isFinite() })
        }
    }

    @Test fun invalidParticleBudgetsAreRejected() {
        for (count in listOf(0, 10, 501)) {
            try {
                V195ParticleGeometry.build(V195ReminderGlyph.CLOCK, count)
                fail("invalid count accepted: $count")
            } catch (_: IllegalArgumentException) {
                // Desired bound on draw workloads.
            }
        }
    }
}
