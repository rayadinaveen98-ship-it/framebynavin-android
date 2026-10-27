package com.framebynavin.app.reminders

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.math.abs

class V218UnifiedWavePolicyTest {
    @Test fun oneFieldUsesExistingScreenParticleCountNotSecondGlyphOverlay() {
        val normal = V202PremiumParticleGeometry.build(false)
        val reduced = V202PremiumParticleGeometry.build(true)
        assertEquals(2184, normal.size)
        assertEquals(986, reduced.size)
        assertEquals(650, V218UnifiedWavePolicy.NORMAL_GLYPH_DOTS)
        assertEquals(340, V218UnifiedWavePolicy.LOW_POWER_GLYPH_DOTS)
        for ((low, field) in listOf(false to normal, true to reduced)) {
            val stride = if (low) 2 else 3
            val slots = (field.size + stride - 1) / stride
            val glyphCount = if (low) 340 else 650
            val assigned = field.indices.filter { index ->
                index % stride == 0 && ((index / stride * 137) % slots) < glyphCount
            }
            assertEquals(glyphCount, assigned.size)
            // Icon particles originate from the whole field, not a small strip.
            val ys = assigned.map { field[it].y }
            val xs = assigned.map { field[it].x }
            assertTrue(ys.min() < .1f && ys.max() > .85f)
            assertTrue(xs.min() < .1f && xs.max() > .85f)
            assertTrue(glyphCount < field.size) // many dots continue their ambient flow
        }
    }

    @Test fun bothShapesRetainRecognizablePureTargetGeometry() {
        for (glyph in V195ReminderGlyph.entries) {
            for (count in listOf(340, 650)) {
                val points = V195ParticleGeometry.build(glyph, count)
                assertEquals(count, points.size)
                assertTrue(points.all { it.x in -1f..1f && it.y in -1f..1f })
                assertTrue(points.distinctBy { it.x to it.y }.size >= count / 2)
            }
        }
    }

    @Test fun formationFadesInAndOutWithoutSnappingOrAutomaticControlReveal() {
        val p = V218UnifiedWavePolicy
        assertEquals(0f, p.entrance(0.0), 0f)
        assertEquals(1f, p.entrance(3.0), 0f)
        assertEquals(0f, p.glyphBlend(0.0, 0f), 0f)
        assertEquals(p.PEAK_GLYPH_STRENGTH, p.glyphBlend(3.0, 0f), 1e-5f)
        assertEquals(0f, p.glyphBlend(3.0, 1f), 0f)
        for (i in 1..200) {
            val before = p.entrance(i * .01 - .01)
            val after = p.entrance(i * .01)
            assertTrue(after >= before)
            assertTrue(abs(after - before) < .015f)
        }
        for (reveal in 0..10) {
            val value = p.glyphBlend(3.0, reveal / 10f)
            assertTrue(value in 0f..1f)
        }
    }

    @Test fun floatingMotionIsFiniteAndContinuousAfterLongRunningSessions() {
        val p = V218UnifiedWavePolicy
        val points = V195ParticleGeometry.build(V195ReminderGlyph.CLOCK, 650)
        for (t in listOf(0.0, .25, 1.3, 4.2, 1000.0, 86400.0)) {
            val dot = points[35]
            val xy = p.iconDrift(t, dot)
            assertTrue(xy.first.isFinite() && xy.second.isFinite())
            assertTrue(abs(xy.first) <= p.GLYPH_FLOW_AMPLITUDE + 1e-5f)
            assertTrue(abs(xy.second) <= p.GLYPH_FLOW_AMPLITUDE + 1e-5f)
            val next = p.iconDrift(t + .0001, dot)
            assertTrue(abs(xy.first - next.first) < .0001f)
            assertTrue(abs(xy.second - next.second) < .0001f)
        }
    }
}
