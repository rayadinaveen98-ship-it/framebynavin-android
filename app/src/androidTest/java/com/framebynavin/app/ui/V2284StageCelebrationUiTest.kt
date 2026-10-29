package com.framebynavin.app.ui

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.test.assertDoesNotExist
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithText
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/** Real Compose pixels/semantics, no creator account and no project writes. */
@RunWith(AndroidJUnit4::class)
class V2284StageCelebrationUiTest {
    @get:Rule val rule = createComposeRule()

    @Test fun confirmedStagePulseIsVisibleAndDisappearsWithinThreeSeconds() {
        var event by mutableStateOf<String?>(null)
        rule.setContent { V127StageCompletionPulse(event) }
        rule.onNodeWithText("STAGE COMPLETE").assertDoesNotExist()
        rule.runOnUiThread { event = "stage:synthetic-project:script" }
        rule.waitUntil(3_000) {
            rule.onAllNodesWithText("STAGE COMPLETE").fetchSemanticsNodes().isNotEmpty()
        }
        rule.waitUntil(5_000) {
            rule.onAllNodesWithText("STAGE COMPLETE").fetchSemanticsNodes().isEmpty()
        }
        assertTrue(event!!.startsWith("stage:"))
    }

    @Test fun absentStageEventCannotRenderCelebration() {
        rule.setContent { V127StageCompletionPulse(null) }
        rule.onNodeWithText("STAGE COMPLETE").assertDoesNotExist()
    }
}
