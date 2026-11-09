package com.framebynavin.app.ui

import android.graphics.Bitmap
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.framebynavin.app.data.CreatorFocusLedger
import com.framebynavin.app.data.CreatorFocusSessionPolicy
import com.framebynavin.app.data.CreatorFocusSessionStore
import com.framebynavin.app.data.CreatorFocusState
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

@RunWith(AndroidJUnit4::class)
class CreatorFocusSessionScreenUiTest {
    @get:Rule val rule = createComposeRule()

    private fun project() = CreatorTask(
        id = "focus-owner-review", title = "Why This Scene Works · తెలుగు",
        platform = "YouTube", contentType = "Long-form", dueLabel = "Tomorrow",
        status = TaskStatus.WORKING,
    )

    @Test fun focusScreenStartsOnlyByTapAndFinishingDoesNotAdvanceProject(): Unit {
        HardeningTestEnvironment.requireCiEmulator()
        val ctx = InstrumentationRegistry.getInstrumentation().targetContext
        val store = CreatorFocusSessionStore(ctx)
        val original = store.exportJson()
        try {
            store.importAfterApprovedRestore(com.framebynavin.app.data.CreatorFocusSessionCodec
                .encode(CreatorFocusLedger()))
            val project = project()
            rule.setContent {
                FrameByNavinTheme {
                    CreatorFocusSessionScreen(task = project, onClose = {})
                }
            }
            rule.onNodeWithTag("phase3-focus-start").assertExists()
            rule.runOnIdle { assertEquals(null, store.load().active) }
            capture("focus-ready")
            rule.onNodeWithTag("phase3-focus-duration-15").performClick()
            rule.onNodeWithTag("phase3-focus-start").performClick()
            rule.onNodeWithTag("phase3-focus-pause").assertExists()
            capture("focus-running")
            rule.onNodeWithTag("phase3-focus-pause").performClick()
            rule.onNodeWithTag("phase3-focus-resume").assertExists()
            capture("focus-paused")
            rule.onNodeWithTag("phase3-focus-finish").performClick()
            rule.runOnIdle {
                assertEquals(null, store.load().active)
                assertEquals(1, store.load().history.size)
                assertEquals(CreatorFocusState.FINISHED, store.load().history.single().state)
                assertEquals("focus-owner-review", store.load().history.single().projectId)
                assertEquals(TaskStatus.WORKING, project.status)
            }
            rule.onNodeWithText("SESSION FINISHED").assertExists()
            capture("focus-finished")
        } finally {
            store.importAfterApprovedRestore(original)
        }
    }

    @Test fun savedSessionIsRecoveredWithoutAutoStartingAfterScreenRecreation(): Unit {
        HardeningTestEnvironment.requireCiEmulator()
        val ctx = InstrumentationRegistry.getInstrumentation().targetContext
        val store = CreatorFocusSessionStore(ctx)
        val original = store.exportJson()
        try {
            val new = CreatorFocusSessionPolicy.create(
                projectId = "focus-owner-review", stageId = "idea",
                targetMinutes = 25, atWallMillis = 1_700_000_000_000L,
                id = "focus-restart-ui-test"
            )
            store.importAfterApprovedRestore(com.framebynavin.app.data.CreatorFocusSessionCodec
                .encode(CreatorFocusLedger(active = new)))
            rule.setContent { FrameByNavinTheme {
                CreatorFocusSessionScreen(task = project(), onClose = {})
            } }
            rule.onNodeWithTag("phase3-focus-resume").assertExists()
            rule.onNodeWithTag("phase3-focus-start").assertDoesNotExist()
            rule.runOnIdle {
                assertEquals(CreatorFocusState.READY, store.load().active?.state)
                assertEquals("focus-restart-ui-test", store.load().active?.id)
            }
        } finally {
            store.importAfterApprovedRestore(original)
        }
    }

    private fun capture(name: String) {
        rule.waitForIdle()
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val dir = File(context.filesDir, "focus-v2-native").apply { mkdirs() }
        val img = rule.onRoot().captureToImage().asAndroidBitmap()
        File(dir, "$name.png").outputStream().use {
            assertTrue(img.compress(Bitmap.CompressFormat.PNG, 100, it))
        }
        assertTrue(File(dir, "$name.png").length() > 1000L)
    }
}
