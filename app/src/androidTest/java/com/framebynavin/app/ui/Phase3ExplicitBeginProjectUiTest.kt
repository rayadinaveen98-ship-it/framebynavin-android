package com.framebynavin.app.ui

import android.graphics.Bitmap
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.test.assertDoesNotExist
import androidx.compose.ui.test.assertExists
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.captureToImage
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.framebynavin.app.data.CreatorTask
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
