package com.framebynavin.app.ui

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.*
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.framebynavin.app.reminders.ReminderMusicMode
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class V277VoiceMusicPreviewUiTest {
    @get:Rule val rule = createComposeRule()

    @Test fun musicAndVoicePreviewActionsCanBeStoppedWithoutSavingReminder() {
        var selected = ReminderMusicMode.PIANO
        rule.setContent {
            var mode by remember { mutableStateOf(ReminderMusicMode.PIANO) }
            MaterialTheme {
                V273ReminderMusicPicker(
                    mode = mode,
                    uri = "",
                    onChanged = { next, _ -> mode = next; selected = next },
                )
            }
        }
        rule.onNodeWithTag("v277-voice-music-test").assertExists().assertHasClickAction()
        rule.onNodeWithTag("v273-music-preview").performClick()
        rule.onNodeWithTag("v277-preview-stop").performClick()
        rule.onNodeWithTag("v273-music-off").performClick()
        rule.onNodeWithTag("v273-music-preview").assertIsNotEnabled()
        rule.onNodeWithTag("v277-voice-music-test").assertIsEnabled()
        rule.onNodeWithTag("v277-preview-stop").performClick()
        rule.runOnIdle { assertEquals(ReminderMusicMode.OFF, selected) }
    }
}
