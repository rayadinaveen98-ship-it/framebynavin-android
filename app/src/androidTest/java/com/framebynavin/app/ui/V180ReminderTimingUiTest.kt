package com.framebynavin.app.ui

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.ui.test.assertExists
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onFirst
import androidx.compose.ui.test.performClick
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test

class V180ReminderTimingUiTest {
    @get:Rule val rule = createComposeRule()

    @Test fun creatorCanChooseShortAlarmAndVoiceRepetitionWithoutLosingValues() {
        val count = mutableIntStateOf(3)
        val gap = mutableIntStateOf(20)
        val duration = mutableIntStateOf(120)
        rule.setContent {
            MaterialTheme {
                V180ReminderTimingControls(
                    repeatCount = count.intValue,
                    onRepeatCount = { count.intValue = it },
                    repeatGapSeconds = gap.intValue,
                    onRepeatGapSeconds = { gap.intValue = it },
                    alarmTimeoutSeconds = duration.intValue,
                    onAlarmTimeoutSeconds = { duration.intValue = it },
                )
            }
        }
        rule.onNodeWithTag("v180-reminder-timing-controls").assertExists()
        rule.onNodeWithText("Once").performClick()
        rule.onAllNodesWithText("30s").onFirst().performClick()
        rule.onNodeWithText("45s").performClick()
        rule.runOnIdle {
            assertEquals(1, count.intValue)
            assertEquals(30, gap.intValue)
            assertEquals(45, duration.intValue)
        }
    }
}
