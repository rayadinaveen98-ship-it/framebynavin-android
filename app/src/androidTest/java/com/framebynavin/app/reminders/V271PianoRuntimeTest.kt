package com.framebynavin.app.reminders

import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Uses synthetic emulator audio only; this is NOT physical-speaker acceptance.
 * Runtime test catches AudioTrack initialization and infinite-loop setup errors
 * that pure waveform unit tests cannot detect.
 */
@RunWith(AndroidJUnit4::class)
class V271PianoRuntimeTest {
    @Test fun originalPianoTrackInitializesDucksAndReleases() {
        val piano = ReminderPianoLoop()
        try {
            assertTrue("Offline Android audio track must initialize", piano.start())
            piano.setSpeaking(true)
            piano.setSpeaking(false)
        } finally {
            piano.stop()
            piano.stop() // Duplicate teardown from Done + Activity close must be harmless.
        }
    }
}
