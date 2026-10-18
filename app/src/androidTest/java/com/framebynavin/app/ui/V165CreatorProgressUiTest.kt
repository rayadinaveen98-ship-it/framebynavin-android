package com.framebynavin.app.ui

import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.framebynavin.app.data.CreatorRewardEngine
import com.framebynavin.app.data.CreatorRewardsV22Engine
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class V165CreatorProgressUiTest {
    @get:Rule val rule = createComposeRule()

    @Test fun emptyProgressOpensRealIdeaVaultAndKeepsOptionalDetailsCollapsed() {
        var ideasOpens = 0
        rule.setContent {
            V23CreatorProgressScreen(
                snapshot = CreatorRewardsV22Engine.snapshot(emptyList()),
                onClose = {},
                onOpenIdeas = { ideasOpens++ },
            )
        }
        rule.onNodeWithText("Your creative work").assertExists()
        rule.onNodeWithText("OPEN IDEA VAULT").performClick()
        rule.runOnIdle { assertEquals(1, ideasOpens) }
        rule.onNodeWithText("VIEW ACHIEVEMENTS").performClick()
        rule.onNodeWithText("First Spark").assertExists()
    }

    @Test fun earnedActionsKeepProjectNavigationAndRecentRewardEvidence() {
        var projectsOpens = 0
        val snapshot = CreatorRewardsV22Engine.snapshot(listOf(
            CreatorRewardEngine.ideaDailyCapture("i", System.currentTimeMillis() - 1000),
            CreatorRewardEngine.projectPublished("p", "Published real content", System.currentTimeMillis() - 700)
        ))
        rule.setContent {
            V23CreatorProgressScreen(
                snapshot = snapshot,
                onClose = {},
                onOpenProjects = { projectsOpens++ },
            )
        }
        rule.onNodeWithText("OPEN PROJECTS").performClick()
        rule.runOnIdle { assertEquals(1, projectsOpens) }
        rule.onNodeWithText("VIEW RECENT REWARDS").performClick()
        rule.onNodeWithText("Published Published real content").assertExists()
    }
}
