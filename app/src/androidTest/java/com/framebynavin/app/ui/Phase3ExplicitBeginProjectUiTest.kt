package com.framebynavin.app.ui

import android.graphics.Bitmap
import android.content.ContextWrapper
import android.content.Intent
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.platform.LocalContext
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.framebynavin.app.data.CreatorTask
import com.framebynavin.app.data.CreatorWorkflowEngine
import com.framebynavin.app.data.HardeningTestEnvironment
import com.framebynavin.app.data.TaskStatus
import com.framebynavin.app.ui.theme.FrameByNavinTheme
import java.io.File
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/** Owner preview: Home Continue is read-only; Begin inside Projects is an explicit action. */
@RunWith(AndroidJUnit4::class)
class Phase3ExplicitBeginProjectUiTest {
    @get:Rule val rule = createComposeRule()

    private fun project(status: TaskStatus) = CreatorTask(
        id = "phase3-script", title = "A cinematic scene · తెలుగు",
        platform = "YouTube", contentType = "Long-form",
        dueLabel = "Tomorrow", status = status,
    )

    @Test fun plannedProjectShowsExplicitBeginAndDoesNotAutoCompleteWorkflow() {
        HardeningTestEnvironment.requireCiEmulator()
        var startCount = 0
        var advanceCount = 0
        rule.setContent {
            FrameByNavinTheme {
                Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState())) {
                    V071WorkflowInlineContent(
                        task = project(TaskStatus.PLANNED),
                        onAdvance = { advanceCount++ },
                        onBack = {}, onFocus = {}, onStart = { startCount++ },
                    )
                }
            }
        }
        rule.onNodeWithTag("phase3-project-begin").performScrollTo().assertExists()
        capture("planned-explicit-begin")
        rule.onNodeWithTag("phase3-project-begin").performClick()
        rule.runOnIdle {
            assertEquals(1, startCount)
            assertEquals(0, advanceCount)
        }
    }

    @Test fun workingProjectHasNoRedundantBeginAction() {
        HardeningTestEnvironment.requireCiEmulator()
        var startCount = 0
        rule.setContent {
            FrameByNavinTheme {
                Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState())) {
                    V071WorkflowInlineContent(
                        task = project(TaskStatus.WORKING),
                        onAdvance = {}, onBack = {}, onFocus = {},
                        onStart = { startCount++ },
                    )
                }
            }
        }
        rule.onNodeWithTag("phase3-project-begin").assertDoesNotExist()
        rule.runOnIdle { assertEquals(0, startCount) }
    }

    @Test fun workOnAtScriptStageOpensActualScriptWorkspaceWithoutStartingTimer() {
        HardeningTestEnvironment.requireCiEmulator()
        val original = project(TaskStatus.WORKING)
        val scriptIndex = CreatorWorkflowEngine.templateFor(original).stages.indexOfFirst { it.id == "script" }
        assertTrue(scriptIndex >= 0)
        val workingScript = original.copy(workflowStageIndex = scriptIndex)
        var focusCount = 0
        var openedIntent: Intent? = null
        val testContext = object : ContextWrapper(InstrumentationRegistry.getInstrumentation().targetContext) {
            override fun startActivity(intent: Intent) {
                openedIntent = intent
            }
        }
        rule.setContent {
            CompositionLocalProvider(LocalContext provides testContext) {
                FrameByNavinTheme {
                    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState())) {
                        V071WorkflowInlineContent(
                            task = workingScript,
                            onAdvance = {}, onBack = {}, onFocus = { focusCount++ },
                        )
                    }
                }
            }
        }
        rule.onNodeWithTag("phase3-project-work-on").performScrollTo().assertExists()
        capture("working-work-on-and-focus")
        rule.onNodeWithTag("phase3-project-work-on").performClick()
        rule.runOnIdle {
            assertEquals(0, focusCount)
            assertEquals(workingScript.id,
                openedIntent?.getStringExtra(ContentWorkspaceActivity.EXTRA_PROJECT_ID))
            assertEquals("script",
                openedIntent?.getStringExtra(ContentWorkspaceActivity.EXTRA_HOME_CONTINUE_STAGE_ID))
        }
        openedIntent = null
        rule.onNodeWithTag("phase3-project-focus").performScrollTo().performClick()
        rule.runOnIdle {
            assertEquals(1, focusCount)
            assertEquals(null, openedIntent)
        }
    }

    @Test fun plannedWorkOnOpensHubWithoutImplicitlyStartingProjectOrTimer() {
        HardeningTestEnvironment.requireCiEmulator()
        val planned = project(TaskStatus.PLANNED)
        var startCount = 0
        var focusCount = 0
        var openedIntent: Intent? = null
        val testContext = object : ContextWrapper(InstrumentationRegistry.getInstrumentation().targetContext) {
            override fun startActivity(intent: Intent) { openedIntent = intent }
        }
        rule.setContent {
            CompositionLocalProvider(LocalContext provides testContext) {
                FrameByNavinTheme {
                    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState())) {
                        V071WorkflowInlineContent(
                            task = planned,
                            onAdvance = {}, onBack = {},
                            onStart = { startCount++ }, onFocus = { focusCount++ },
                        )
                    }
                }
            }
        }
        rule.onNodeWithTag("phase3-project-work-on").performScrollTo().performClick()
        rule.runOnIdle {
            assertEquals(planned.id,
                openedIntent?.getStringExtra(ContentWorkspaceActivity.EXTRA_PROJECT_ID))
            assertEquals(null,
                openedIntent?.getStringExtra(ContentWorkspaceActivity.EXTRA_HOME_CONTINUE_STAGE_ID))
            assertEquals(0, focusCount)
            assertEquals(0, startCount)
        }
    }

    private fun capture(scene: String) {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        rule.waitForIdle()
        val folder = File(context.filesDir, "phase3-project-begin")
        assertTrue(folder.isDirectory || folder.mkdirs())
        val image = rule.onRoot().captureToImage().asAndroidBitmap()
        File(folder, "$scene.png").outputStream().use {
            assertTrue(image.compress(Bitmap.CompressFormat.PNG, 100, it))
        }
        assertTrue(File(folder, "$scene.png").length() > 1000L)
    }
}
