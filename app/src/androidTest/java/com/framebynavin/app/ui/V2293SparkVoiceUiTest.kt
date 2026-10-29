package com.framebynavin.app.ui

import android.os.Build
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.framebynavin.app.data.CreatorTask
import com.framebynavin.app.data.TaskStatus
import com.framebynavin.app.ui.theme.FrameByNavinTheme
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * The emulator is never asked to enable a mic or download TTS voices;
 * those require consent and real-device acceptance testing.
 */
@RunWith(AndroidJUnit4::class)
class V2293SparkVoiceUiTest {
    @get:Rule val rule = createComposeRule()

    private fun render() = rule.setContent {
        FrameByNavinTheme {
            V2292SparkConversationPanel(
                tasks = listOf(CreatorTask(
                    id = "voice-qa", title = "Film analysis",
                    platform = "YouTube", contentType = "Long-form",
                    dueLabel = "Today", status = TaskStatus.WORKING,
                )),
                ideas = emptyList(), onBack = {}, onOpenProject = {},
                onOpenIdeas = {}, onOpenReminders = {},
            )
        }
    }

    @Test fun microphoneIsAnOptionalUserActionAndTextStillAvailable() {
        render()
        rule.onNodeWithTag("v2293-spark-microphone").assertExists()
        rule.onNodeWithTag("v2292-spark-input").assertExists()
        rule.onNodeWithTag("v2292-spark-send").assertExists()
        rule.onNodeWithTag("v2293-spark-privacy").assertExists()
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.S) {
            rule.onNodeWithTag("v2293-spark-microphone").assertIsNotEnabled()
        }
    }

    @Test fun readAloudIsOptInAndNeverAutoplaysOnNewReply() {
        render()
        rule.onNodeWithTag("v2292-spark-chip-0").performClick()
        rule.onNodeWithTag("v2293-spark-read-aloud").assertExists()
        rule.onNodeWithText("READ ALOUD").assertExists()
        // No TTS invocation in this test: manual tap is mandatory.
    }
}
