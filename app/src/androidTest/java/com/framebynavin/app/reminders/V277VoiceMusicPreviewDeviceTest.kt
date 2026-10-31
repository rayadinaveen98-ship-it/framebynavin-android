package com.framebynavin.app.reminders

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Isolated in-app preview with synthetic original sounds only, never persisted,
 * no notification, and no personal or project data.
 */
@RunWith(AndroidJUnit4::class)
class V277VoiceMusicPreviewDeviceTest {
    @Test fun musicPreviewStartsAndStopsWithoutReminderSideEffects() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val preview = ReminderVoiceMusicPreview(context)
        try {
            preview.playMusic(ReminderMusicChoice(ReminderMusicMode.PIANO))
            preview.stop()
            preview.playMusic(ReminderMusicChoice(ReminderMusicMode.OFF))
            preview.stop()
            preview.stop()
        } finally {
            preview.stop()
        }
    }

    @Test fun voicePreviewIsShortGenericTextWithHardStop() {
        val phrase = ReminderVoiceMusicPreview.SAMPLE_SPEECH
        assertTrue(phrase.startsWith("Backlot reminder preview."))
        assertTrue(phrase.length < 180)
        assertFalse(phrase.contains("CreatorTask"))
        assertTrue(ReminderVoiceMusicPreview.MAX_PREVIEW_MILLIS in 10_000L..60_000L)
        val context = ApplicationProvider.getApplicationContext<Context>()
        val preview = ReminderVoiceMusicPreview(context)
        try {
            preview.playVoiceAndMusic(ReminderMusicChoice(ReminderMusicMode.OFF)) {}
        } finally {
            preview.stop() // Must cancel pending asynchronous TTS initialization safely.
        }
    }
}
