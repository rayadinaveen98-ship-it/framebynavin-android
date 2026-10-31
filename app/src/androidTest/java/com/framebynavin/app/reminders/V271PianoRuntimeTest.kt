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
        val piano = ReminderPianoLoop(androidx.test.core.app.ApplicationProvider.getApplicationContext())
        try {
            assertTrue("Offline piano MediaPlayer must initialize", piano.start())
            val deadline = android.os.SystemClock.elapsedRealtime() + 6_000L
            while (!piano.isPlayingForQa() &&
                android.os.SystemClock.elapsedRealtime() < deadline) {
                Thread.sleep(100)
            }
            assertTrue("Offline WAV must actually begin audible playback", piano.isPlayingForQa())
            piano.setSpeaking(true)
            piano.setSpeaking(false)
        } finally {
            piano.stop()
            piano.stop() // Duplicate teardown from Done + Activity close must be harmless.
        }
    }
}
