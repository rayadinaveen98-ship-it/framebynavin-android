package com.framebynavin.app.ui

import androidx.compose.runtime.*
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.framebynavin.app.data.CreatorProfile
import com.framebynavin.app.data.CreatorTask
import com.framebynavin.app.data.HardeningTestEnvironment
import com.framebynavin.app.ui.theme.FrameByNavinTheme
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Simulates a real cold launch with a stable mutable StateList object.
 * No TaskStore writes, backups or creator data are touched.
 */
@RunWith(AndroidJUnit4::class)
class V230ColdStartLoadingUiTest {
    @get:Rule val rule = createComposeRule()
    private val profile = CreatorProfile(
        displayName = "Test Creator",
        primaryCreatorMode = "Filmmaker",
        platforms = setOf("YouTube"),
        primaryGoal = "Make cinema",
    )

    @Test fun initialEmptySnapshotShowsLoadingNotEmptyAccount() {
        HardeningTestEnvironment.requireCiEmulator()
        val liveTasks = mutableStateListOf<CreatorTask>()
        var loaded by mutableStateOf(false)
        rule.setContent {
            FrameByNavinTheme {
                PTodayScreen(
                    creatorProfile = profile,
                    tasks = liveTasks,
                    projectsLoaded = loaded,
                    onAdd = {}, onStart = {}, onAdvance = {},
                    onViewAllReminders = {}, onFocus = {},
                )
            }
        }
        rule.onNodeWithTag("v230-projects-loading").assertExists()
        rule.onNodeWithText("Opening saved projects…").assertExists()
        rule.onNodeWithText("CREATE PROJECT").assertDoesNotExist()
        rule.runOnIdle { loaded = true }
        rule.waitForIdle()
        rule.onNodeWithTag("v230-projects-loading").assertDoesNotExist()
        rule.onNodeWithText("CREATE PROJECT").assertExists()
    }

    @Test fun savedProjectsReplaceLoadingWithoutNavigationOrListReplacement() {
        HardeningTestEnvironment.requireCiEmulator()
        val liveTasks = mutableStateListOf<CreatorTask>()
        var loaded by mutableStateOf(false)
        rule.setContent {
            FrameByNavinTheme {
                PTodayScreen(
                    creatorProfile = profile,
                    tasks = liveTasks,
                    projectsLoaded = loaded,
                    onAdd = {}, onStart = {}, onAdvance = {},
                    onViewAllReminders = {}, onFocus = {},
                )
            }
        }
        rule.onNodeWithTag("v230-projects-loading").assertExists()
        rule.runOnIdle {
            liveTasks.add(CreatorTask(
                id = "real-cold-start", title = "Saved film project",
                platform = "YouTube", contentType = "Long-form", dueLabel = "Today",
            ))
            loaded = true
        }
        rule.waitForIdle()
        rule.onNodeWithTag("v215-up-next-header").assertExists()
        rule.onNodeWithText("Saved film project").assertExists()
        rule.onNodeWithTag("v230-projects-loading").assertDoesNotExist()
        rule.onNodeWithText("CREATE PROJECT").assertDoesNotExist()
    }
}
