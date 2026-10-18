package com.framebynavin.app.ui

import androidx.compose.runtime.*
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.test.platform.app.InstrumentationRegistry
import com.framebynavin.app.data.HardeningTestEnvironment
import com.framebynavin.app.data.QuickCaptureDraftStore
import org.junit.Assert.*
import org.junit.Before
import org.junit.Rule
import org.junit.Test

class V184QuickCaptureRecoveryUiTest {
    @get:Rule val rule = createComposeRule()
    @Before fun reset() {
        HardeningTestEnvironment.requireCiEmulator()
        InstrumentationRegistry.getInstrumentation().targetContext
            .getSharedPreferences("quick_capture_draft_v184", 0).edit().clear().commit()
    }

    private fun waitForCaptureReady() {
        try {
            rule.waitUntil(30_000) {
                !rule.onNodeWithTag("quick-capture-title").fetchSemanticsNode().config.contains(SemanticsProperties.Disabled)
            }
        } catch (error: Throwable) {
            throw AssertionError("Capture stayed disabled. UI state:\n" + rule.onAllNodes(isRoot(), useUnmergedTree = true).printToString(), error)
        }
    }

    @Test fun keepCloseAndReopenRecoversTextWhileDiscardClearsIt() {
        var visible by mutableStateOf(true)
        rule.setContent {
            if (visible) V14QuickCaptureDialog(onDismiss = { visible = false }, onSave = { false })
        }
        waitForCaptureReady()
        rule.onNodeWithTag("quick-capture-title").performTextInput("Camera movement idea")
        rule.waitUntil(30_000) {
            !rule.onNodeWithTag("quick-capture-save").fetchSemanticsNode().config.contains(SemanticsProperties.Disabled)
        }
        rule.onNodeWithTag("quick-capture-close").performClick()
        rule.onNodeWithTag("quick-capture-keep-draft").performClick()
        rule.waitUntil(30_000) { !visible }
        rule.onAllNodesWithTag("quick-capture-title").assertCountEquals(0)
        rule.waitForIdle()
        rule.runOnIdle { visible = true }
        waitForCaptureReady()
        rule.onNodeWithText("Your unfinished idea was recovered.").assertIsDisplayed()
        rule.onNodeWithTag("quick-capture-title").assertTextContains("Camera movement idea")
        try {
            rule.waitUntil(30_000) {
                !rule.onNodeWithTag("quick-capture-save").fetchSemanticsNode().config.contains(SemanticsProperties.Disabled)
            }
        } catch (error: Throwable) {
            throw AssertionError("Recovered capture stayed disabled. UI state:\n" + rule.onAllNodes(isRoot(), useUnmergedTree = true).printToString(), error)
        }
        rule.onNodeWithTag("quick-capture-close").performClick()
        rule.onNodeWithTag("quick-capture-discard-draft").performClick()
        rule.waitUntil(30_000) { !visible }
        assertNull(QuickCaptureDraftStore(InstrumentationRegistry.getInstrumentation().targetContext).load())
    }

    @Test fun failedDurableSaveKeepsDraftForRetryAndSuccessClearsIt() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        var attempts = 0
        rule.setContent {
            V14QuickCaptureDialog(onDismiss = {}, onSave = { error("must use durable writer") },
                onSaveDurably = { _, _ -> ++attempts > 1 })
        }
        waitForCaptureReady()
        rule.onNodeWithTag("quick-capture-title").performTextInput("Keep this thought")
        rule.waitUntil(30_000) {
            !rule.onNodeWithTag("quick-capture-save").fetchSemanticsNode().config.contains(SemanticsProperties.Disabled)
        }
        rule.onNodeWithTag("quick-capture-save").performClick()
        rule.onNodeWithTag("quick-capture-save-error").assertIsDisplayed()
        assertEquals("Keep this thought", QuickCaptureDraftStore(context).load()?.title)
        rule.onNodeWithTag("quick-capture-save").performClick()
        rule.waitUntil(30_000) { attempts == 2 && QuickCaptureDraftStore(context).load() == null }
    }
}
