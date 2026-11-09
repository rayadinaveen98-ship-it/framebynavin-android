package com.framebynavin.app.personal

import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * Pure policy checks only. Full Android TTS/MediaPlayer, locked-screen and
 * process-death behavior belongs to the deferred release integration QA.
 */
class FC13PersonalPlaybackVisualStateTest {
    @After fun clear() {
        FC13PersonalPlaybackVisualState.receipts.value.let { current ->
            FC13PersonalPlaybackVisualState.clear(current.reminderId, current.occurrenceToken)
        }
    }

    @Test fun realPlaybackStageVisibleOnlyForExactOccurrence() {
        FC13PersonalPlaybackVisualState.show("alarm-1", "token-a", FC13PlaybackStage.ALARM_RINGING)
        val actual = FC13PersonalPlaybackVisualState.receipts.value
        assertEquals(FC13PlaybackStage.ALARM_RINGING, actual.stageFor("alarm-1", "token-a"))
        assertEquals(FC13PlaybackStage.SILENT, actual.stageFor("alarm-1", "token-b"))
        assertEquals(FC13PlaybackStage.SILENT, actual.stageFor("other", "token-a"))
        FC13PersonalPlaybackVisualState.clear("alarm-1", "token-b")
        assertEquals(FC13PlaybackStage.ALARM_RINGING,
            FC13PersonalPlaybackVisualState.receipts.value.stageFor("alarm-1", "token-a"))
    }

    @Test fun endOfSpeechAndPreemptionClearMatchingVisualOnly() {
        FC13PersonalPlaybackVisualState.show("voice-1", "old", FC13PlaybackStage.VOICE_SPEAKING)
        FC13PersonalPlaybackVisualState.show("voice-1", "new", FC13PlaybackStage.VOICE_SPEAKING)
        FC13PersonalPlaybackVisualState.clear("voice-1", "old")
        assertEquals(FC13PlaybackStage.VOICE_SPEAKING,
            FC13PersonalPlaybackVisualState.receipts.value.stageFor("voice-1", "new"))
        FC13PersonalPlaybackVisualState.clear("voice-1", "new")
        assertEquals(FC13PlaybackStage.SILENT,
            FC13PersonalPlaybackVisualState.receipts.value.stageFor("voice-1", "new"))
    }

    @Test fun blankOrSilentValuesCannotFabricatePlayback() {
        FC13PersonalPlaybackVisualState.show("", "token", FC13PlaybackStage.ALARM_RINGING)
        FC13PersonalPlaybackVisualState.show("x", "", FC13PlaybackStage.VOICE_SPEAKING)
        FC13PersonalPlaybackVisualState.show("x", "token", FC13PlaybackStage.SILENT)
        assertEquals(FC13PlaybackStage.SILENT,
            FC13PersonalPlaybackVisualState.receipts.value.stageFor("x", "token"))
    }
}
