package com.framebynavin.app.reminders

import org.junit.Assert.*
import org.junit.Test
import kotlin.math.abs
import kotlin.math.hypot

class V202PremiumParticleMotionTest {
    @Test fun geometryReallyDoublesBothAmbientAndGlyphCount() {
        for (low in listOf(false, true)) {
            val field = V202PremiumParticleGeometry.build(low)
            assertEquals(if (low) 986 else 2184, field.size)
            assertEquals(field, V202PremiumParticleGeometry.build(low))
            assertEquals(8, field.map { it.colorIndex }.distinct().size)
            assertEquals(3, field.map { it.depth }.distinct().size)
            assertTrue(field.all { it.x in 0f..1f && it.y in 0f..1f })
            assertTrue(field.all { it.velocityX.isFinite() && it.velocityY.isFinite() })
            assertTrue(field.all { hypot(it.velocityX, it.velocityY) in .05f..0.25f })
            assertEquals(4, field.map { (it.velocityX > 0f) to (it.velocityY > 0f) }.distinct().size)
        }
        assertEquals(840, V195ParticleGeometry.DEFAULT_DOTS)
        assertEquals(440, V195ParticleGeometry.LOW_POWER_DOTS)
        assertEquals(3024, V202PremiumParticleGeometry.NORMAL_COUNT + V195ParticleGeometry.DEFAULT_DOTS)
        assertEquals(1426, V202PremiumParticleGeometry.LOW_POWER_COUNT + V195ParticleGeometry.LOW_POWER_DOTS)
    }

    @Test fun allScreenEdgesReflectContinuouslyAndStopSmoothly() {
        for (velocity in listOf(.12f, -.12f)) {
            val initial = .21f
            val hit = if (velocity > 0f) (1.0 - initial) / velocity else -initial / velocity
            val at = V202InfiniteBounce.position(initial, velocity, hit)
            val before = V202InfiniteBounce.position(initial, velocity, hit - .001)
            val after = V202InfiniteBounce.position(initial, velocity, hit + .001)
            assertEquals(if (velocity > 0f) 1f else 0f, at, .00001f)
            assertTrue(abs(at - before) < .00001f)
            assertTrue(abs(at - after) < .00001f)
            assertTrue(abs(before - after) < .00001f)
            val oneSecLater = V202InfiniteBounce.position(initial, velocity, hit + 1.0)
            assertTrue(if (velocity > 0f) oneSecLater < at else oneSecLater > at)
        }
    }

    @Test fun motionNeverTeleportsEvenAfterDays() {
        val field = V202PremiumParticleGeometry.build(false)
        for (p in field.take(90)) {
            for (seconds in listOf(0.0, .11, 3.8, 9.0, 604.125, 86400.75, 604800.125)) {
                val x = V202InfiniteBounce.position(p.x, p.velocityX, seconds)
                val y = V202InfiniteBounce.position(p.y, p.velocityY, seconds)
                assertTrue(x in 0f..1f && y in 0f..1f)
                assertTrue(V202InfiniteBounce.colorBlend(seconds, p.colorPhase, p.colorRate) in 0f..1f)
            }
        }
        val p = field[20]
        val a = V202InfiniteBounce.position(p.x, p.velocityX, 3.8 - .0001)
        val b = V202InfiniteBounce.position(p.x, p.velocityX, 3.8 + .0001)
        assertTrue(abs(a - b) < .0002f)
    }
}
