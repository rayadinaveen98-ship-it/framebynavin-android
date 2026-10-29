package com.framebynavin.app.ui

import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.framebynavin.app.data.CreatorIdea
import com.framebynavin.app.data.CreatorTask
import com.framebynavin.app.data.TaskStatus
import com.framebynavin.app.ui.theme.FrameByNavinTheme
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class V2292SparkConversationUiTest {
    @get:Rule val rule = createComposeRule()

    private fun project() = CreatorTask(
        id = "spark-active", title = "Why silence works",
        platform = "YouTube", contentType = "Long-form",
        dueLabel = "This week", status = TaskStatus.WORKING,
    )

    @Test fun quickNextStepUsesSavedProjectAndOpensExistingProject() {
        var routed = ""
        rule.setContent {
            FrameByNavinTheme {
                V2292SparkConversationPanel(
                    tasks = listOf(project()), ideas = emptyList(),
                    onBack = {}, onOpenProject = { routed = it },
                    onOpenIdeas = { routed = "ideas" },
                    onOpenReminders = { routed = "reminders" },
                )
            }
        }
        rule.onNodeWithTag("v2292-spark-conversation").assertExists()
        rule.onNodeWithTag("v2292-spark-chip-0").performClick()
        rule.onNodeWithText("Why silence works", substring = true).assertExists()
        rule.onNodeWithTag("v2292-spark-action").performClick()
        rule.runOnIdle { assertEquals("spark-active", routed) }
    }

    @Test fun unknownQuestionGetsHonestLimitedAnswer() {
        rule.setContent {
            FrameByNavinTheme {
                V2292SparkConversationPanel(
                    tasks = emptyList(), ideas = emptyList(),
                    onBack = {}, onOpenProject = {},
                    onOpenIdeas = {}, onOpenReminders = {},
                )
            }
        }
        rule.onNodeWithTag("v2292-spark-input").performTextInput("Tell me the weather in Mars")
        rule.onNodeWithTag("v2292-spark-send").performClick()
        rule.onNodeWithText("not a full AI chat model", substring = true).assertExists()
    }

    @Test fun reminderQuestionRoutesToExistingRemindersWithoutDeliveryClaim() {
        var route = ""
        rule.setContent {
            FrameByNavinTheme {
                V2292SparkConversationPanel(
                    tasks = emptyList(), ideas = listOf(CreatorIdea(id = "test", title = "Idea")),
                    onBack = {}, onOpenProject = { route = it },
                    onOpenIdeas = { route = "ideas" },
                    onOpenReminders = { route = "reminders" },
                )
            }
        }
        rule.onNodeWithTag("v2292-spark-chip-2").performClick()
        rule.onNodeWithText("can't verify", substring = true).assertExists()
        rule.onNodeWithTag("v2292-spark-action").performClick()
        rule.runOnIdle { assertEquals("reminders", route) }
    }
}
