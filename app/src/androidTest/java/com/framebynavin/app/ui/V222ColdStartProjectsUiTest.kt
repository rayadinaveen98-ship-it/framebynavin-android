package com.framebynavin.app.ui

import androidx.compose.runtime.mutableStateListOf
import androidx.compose.ui.test.assertDoesNotExist
import androidx.compose.ui.test.assertExists
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.framebynavin.app.data.CreatorProfile
import com.framebynavin.app.data.CreatorTask
import com.framebynavin.app.data.HardeningTestEnvironment
import com.framebynavin.app.ui.theme.FrameByNavinTheme
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Simulates the actual cold start: Home is first composed while CreatorViewModel's
 * mutableStateListOf has no tasks; DataStore emits by mutating that SAME list
 * instance. Immutable-list tests alone miss this production regression.
 * No real project store or user data is written.
 */
@RunWith(AndroidJUnit4::class)
class V222ColdStartProjectsUiTest {
    @get:Rule val rule = createComposeRule()

    @Test fun coldStartMutableTaskListLoadsHomeWithoutSwitchingTabs() {
        HardeningTestEnvironment.requireCiEmulator()
        val liveTasks = mutableStateListOf<CreatorTask>()
        val profile = CreatorProfile(
            displayName = "Demo Creator",
            primaryCreatorMode = "Filmmaker",
            platforms = setOf("YouTube"),
            primaryGoal = "Create meaningful cinema",
        )
        rule.setContent {
            FrameByNavinTheme {
                PTodayScreen(
                    creatorProfile = profile,
                    tasks = liveTasks,
                    onAdd = {}, onStart = {}, onAdvance = {},
                    onViewAllReminders = {}, onFocus = {},
                )
            }
        }

        rule.onNodeWithText("CREATE PROJECT").assertExists()
        rule.onNodeWithTag("v215-up-next-header").assertDoesNotExist()

        // The reference object is never replaced, exactly as in CreatorViewModel.
        rule.runOnIdle {
            liveTasks.add(
                CreatorTask(
                    id = "cold-load-1",
                    title = "Cold-start project · తెలుగు",
                    platform = "YouTube",
                    contentType = "Film",
                    dueLabel = "Today",
                )
            )
        }
        rule.waitForIdle()
        rule.onNodeWithTag("v215-up-next-header").assertExists()
        rule.onNodeWithText("Cold-start project · తెలుగు").assertExists()
        rule.onNodeWithText("CREATE PROJECT").assertDoesNotExist()

        // Subsequent same-instance updates must not leave stale project cards.
        rule.runOnIdle {
            liveTasks[0] = liveTasks[0].copy(title = "Edited after loading")
        }
        rule.waitForIdle()
        rule.onNodeWithText("Edited after loading").assertExists()
        rule.onNodeWithText("Cold-start project · తెలుగు").assertDoesNotExist()
    }
}
