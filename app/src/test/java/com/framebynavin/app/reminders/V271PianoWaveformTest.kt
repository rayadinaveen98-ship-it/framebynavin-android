package com.framebynavin.app.reminders

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/** Verify offline synthesized soundtrack shape; don't require emulator speakers. */
class V271PianoWaveformTest {
    @Test fun originalPianoLoopIsAudibleFiniteAndShort() {
        val samples = ReminderPianoWaveform.render()
        assertTrue(samples.size in 48_000..192_000)
        assertEquals(0, samples[0].toInt())
        assertTrue(samples.any { it > 1_000 })
        assertTrue(samples.any { it < -1_000 })
        assertTrue(samples.all { it.toInt() in Short.MIN_VALUE..Short.MAX_VALUE })
    }
    @Test fun originalPianoIsStableAcrossRebuilds() {
        val first = ReminderPianoWaveform.render()
        val second = ReminderPianoWaveform.render()
        assertTrue(first.contentEquals(second))
        assertEquals(16_000, ReminderPianoWaveform.SAMPLE_RATE)
    }
}
