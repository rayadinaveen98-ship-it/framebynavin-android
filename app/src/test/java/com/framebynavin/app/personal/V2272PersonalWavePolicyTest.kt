package com.framebynavin.app.personal

import com.framebynavin.app.reminders.V195ParticleGeometry
import com.framebynavin.app.reminders.V195ReminderGlyph
import com.framebynavin.app.reminders.V202PremiumParticleGeometry
import com.framebynavin.app.reminders.V218UnifiedWavePolicy
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class V2272PersonalWavePolicyTest {
    @Test fun personalVoiceAndAlarmUseSameNaturalGlyphGeometryAsCreator() {
        assertEquals(V195ReminderGlyph.MICROPHONE, V2272PersonalWavePolicy.icon(PersonalAlert.VOICE))
        assertEquals(V195ReminderGlyph.CLOCK, V2272PersonalWavePolicy.icon(PersonalAlert.ALARM))
        assertEquals(V195ReminderGlyph.CLOCK, V2272PersonalWavePolicy.icon(PersonalAlert.NOTIFICATION))
    }

    @Test fun reducedPowerUsesLessThanHalfTheNormalDotBudget() {
        assertEquals(728, V2272PersonalWavePolicy.sampledCount(false))
        assertEquals(329, V2272PersonalWavePolicy.sampledCount(true))
        assertEquals(243, V2272PersonalWavePolicy.glyphCount(false))
        assertEquals(110, V2272PersonalWavePolicy.glyphCount(true))
        assertTrue(V2272PersonalWavePolicy.sampledCount(true) <
            V2272PersonalWavePolicy.sampledCount(false) / 2)
        assertEquals(33_333_333L, V2272PersonalWavePolicy.frameIntervalNs(false))
        assertEquals(50_000_000L, V2272PersonalWavePolicy.frameIntervalNs(true))
    }

    @Test fun sameFieldFormsRecognizableGlyphWithoutExtraOverlayGeometry() {
        for (lowPower in listOf(false, true)) {
            val base = V202PremiumParticleGeometry.build(lowPower)
                .filterIndexed { index, _ -> index % V2272PersonalWavePolicy.SAMPLE_EVERY == 0 }
            assertEquals(V2272PersonalWavePolicy.sampledCount(lowPower), base.size)
            for (alert in listOf(PersonalAlert.VOICE, PersonalAlert.ALARM)) {
                val target = V195ParticleGeometry.build(
                    V2272PersonalWavePolicy.icon(alert),
                    V2272PersonalWavePolicy.glyphCount(lowPower),
                )
                assertEquals(V2272PersonalWavePolicy.glyphCount(lowPower), target.size)
                assertTrue(target.all { it.x.isFinite() && it.y.isFinite() })
            }
        }
    }

    @Test fun reducedMotionShowsStableFormedIconInsteadOfSpinningForever() {
        val time = V2272PersonalWavePolicy.elapsedWhenMotionDisabled()
        assertTrue(time > V218UnifiedWavePolicy.FORM_SECONDS)
        assertEquals(1f, V218UnifiedWavePolicy.glyphBlend(time, 0f), 0.0001f)
        assertFalse(time.isNaN())
    }
}
