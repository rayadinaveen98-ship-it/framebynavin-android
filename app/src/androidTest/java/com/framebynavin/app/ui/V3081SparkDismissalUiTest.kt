package com.framebynavin.app.ui

import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.framebynavin.app.data.HardeningTestEnvironment
import com.framebynavin.app.ui.theme.FrameByNavinTheme
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/** Synthetic-only presentation test: no real stores or alarm actions touched. */
@RunWith(AndroidJUnit4::class)
class V3081SparkDismissalUiTest {
    @get:Rule val rule = createComposeRule()

    @Test fun perItemCloseReflowsToNextRealActivityWithoutCompletingReminder() {
        HardeningTestEnvironment.requireCiEmulator()
        val feed = listOf(
            V306ActivityItem("creator:action:a:1", V306ActivityDomain.CREATOR,
                V306ActivityBucket.NEEDS_ACTION, "Film project A", "Awaiting response", 10L, "a"),
            V306ActivityItem("creator:action:b:2", V306ActivityDomain.CREATOR,
                V306ActivityBucket.NEEDS_ACTION, "Film project B", "Awaiting response", 9L, "b"),
            V306ActivityItem("personal:action:c:3", V306ActivityDomain.PERSONAL,
                V306ActivityBucket.NEEDS_ACTION, "My private sleep diary", "Awaiting response", 8L, "c"),
            V306ActivityItem("creator:action:d:4", V306ActivityDomain.CREATOR,
                V306ActivityBucket.NEEDS_ACTION, "Film project D", "Awaiting response", 7L, "d"),
        )
        val dismissed = mutableStateOf(emptySet<String>())
        var allOpens = 0
        var projectOpen = ""
        rule.setContent {
            FrameByNavinTheme {
                V308SparkActivityDigest(
                    feed = feed,
                    dismissed = dismissed.value,
                    onDismiss = { item ->
                        dismissed.value = dismissed.value + V308SparkActivityPolicy.dismissalId(item)
                    },
                    onOpenActivity = { allOpens++ },
                    onOpenCreator = { projectOpen = it },
                    onOpenPersonal = {},
                )
            }
        }
        rule.onNodeWithText("Film project A").assertExists()
        rule.onNodeWithText("Film project D").assertDoesNotExist()
        rule.onNodeWithText("My private sleep diary").assertDoesNotExist()
        rule.onAllNodesWithTag("v308-spark-dismiss").onFirst().performClick()
        rule.onNodeWithText("Film project A").assertDoesNotExist()
        rule.onNodeWithText("Film project D").assertExists()
        rule.onNodeWithText("Personal reminder needs attention").assertExists()
        rule.onNodeWithTag("v308-spark-see-all").performClick()
        rule.runOnIdle {
            assertEquals(1, allOpens)
            assertTrue(feed.any { it.title == "Film project A" })
            assertEquals(4, feed.size)
            assertEquals(1, dismissed.value.size)
        }
        rule.onNodeWithText("Film project B").performClick()
        rule.runOnIdle { assertEquals("b", projectOpen) }
    }
}
