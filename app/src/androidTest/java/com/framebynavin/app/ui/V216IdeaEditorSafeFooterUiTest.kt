package com.framebynavin.app.ui

import android.graphics.Bitmap
import android.os.SystemClock
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.unit.dp
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.framebynavin.app.data.CreatorIdea
import com.framebynavin.app.ui.theme.FrameByNavinTheme
import java.io.File
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Real existing-idea editing through V09IdeaVaultScreen, including its
 * full-window Dialog and saved-item edit route. No real ideas are persisted:
 * callbacks are synthetic, and all screenshot evidence is synthetic.
 */
@RunWith(AndroidJUnit4::class)
class V216IdeaEditorSafeFooterUiTest {
    @get:Rule val rule = createComposeRule()
    private val source = CreatorIdea(
        id = "v216-owner-safe-test", title = "Cinema breakdown · తెలుగు",
        notes = "Original notes retained", topic = "Film craft",
    )

    private fun openEdit(onSave: (CreatorIdea) -> String? = { "accepted" }) {
        rule.setContent {
            FrameByNavinTheme {
                V09IdeaVaultScreen(
                    ideas = listOf(source),
                    onClose = null,
                    onSave = onSave,
                    onDelete = {},
                    onArchive = {},
                    onConvert = { _, _, _, _ -> null },
                    externalIdeaId = source.id,
                    externalIdeaMode = "",
                    externalLaunchNonce = 216L,
                )
            }
        }
        rule.waitForIdle()
        rule.onNodeWithTag("v216-edit-editor").assertIsDisplayed()
        rule.onNodeWithTag("v216-edit-save").assertIsDisplayed()
        rule.onNodeWithTag("v216-edit-cancel").assertIsDisplayed()
    }

    private fun assertFooterInSafeWindow() {
        rule.waitForIdle()
        val root = rule.onNodeWithTag("v216-edit-editor").getUnclippedBoundsInRoot()
        val save = rule.onNodeWithTag("v216-edit-save").getUnclippedBoundsInRoot()
        val cancel = rule.onNodeWithTag("v216-edit-cancel").getUnclippedBoundsInRoot()
        assertTrue("Save clipped on bottom or right: $save vs $root",
            save.bottom <= root.bottom && save.right <= root.right)
        assertTrue("Save clipped at top or left: $save vs $root",
            save.top >= root.top && save.left >= root.left)
        assertTrue("Cancel clipped: $cancel vs $root",
            cancel.bottom <= root.bottom && cancel.left >= root.left)
        assertTrue("Save/cancel do not have reachable vertical touch area: $save $cancel",
            save.bottom - save.top >= 48.dp && cancel.bottom - cancel.top >= 48.dp)
        rule.onNodeWithTag("v216-edit-save").assertIsDisplayed()
        rule.onNodeWithTag("v216-edit-cancel").assertIsDisplayed()
    }

    private fun capture(scenario: String) {
        val profile = InstrumentationRegistry.getArguments().getString("backlotVisualProfile")
        check(profile in setOf("default", "compact")) { "Explicit viewport profile required" }
        rule.waitForIdle()
        SystemClock.sleep(300)
        val instrument = InstrumentationRegistry.getInstrumentation()
        val bitmap = requireNotNull(instrument.uiAutomation.takeScreenshot()) {
            "Could not capture Android window and keyboard"
        }
        val dir = File(instrument.targetContext.filesDir, "v216-idea-editor-visual")
        check(dir.isDirectory || dir.mkdirs())
        val output = File(dir, "v216-" + scenario + "-" + profile + ".png")
        try {
            output.outputStream().use {
                check(bitmap.compress(Bitmap.CompressFormat.PNG, 100, it))
            }
        } finally {
            bitmap.recycle()
        }
        assertTrue("Invalid screenshot: $output", output.length() > 256)
    }

    @Test fun existingIdeaSaveAndCancelAlwaysFit() {
        openEdit()
        assertFooterInSafeWindow()
        rule.onNodeWithTag("v216-edit-title").assertIsDisplayed()
        rule.onNodeWithTag("v216-edit-notes").assertIsDisplayed()
        capture("editor-top")
    }

    @Test fun longTeluguNotesKeepSaveReachableWithKeyboard() {
        openEdit()
        rule.onNodeWithTag("v216-edit-notes").performTextInput(
            "తెలుగు సినిమాలో కెమెరా కదలికలు, వెలుతురు, ఎడిటింగ్ విశ్లేషణ. ".repeat(7)
        )
        assertFooterInSafeWindow()
        capture("editor-keyboard")
    }

    @Test fun rejectedSaveRetainsEditableOriginalFields() {
        openEdit(onSave = { null })
        rule.onNodeWithTag("v216-edit-save").performClick()
        rule.onNodeWithTag("v216-edit-editor").assertExists()
        rule.onNodeWithTag("v216-edit-save").assertIsDisplayed()
        assertFooterInSafeWindow()
        capture("editor-rejected")
    }

    @Test fun successfulSavePreservesIdeaIdentityAndOriginalNotes() {
        var saved: CreatorIdea? = null
        openEdit(onSave = { saved = it; "accepted" })
        assertFooterInSafeWindow()
        capture("editor-save")
        rule.onNodeWithTag("v216-edit-save").performClick()
        rule.waitForIdle()
        assertEquals(source.id, saved?.id)
        assertEquals(source.notes, saved?.notes)
        assertEquals(source.topic, saved?.topic)
        rule.onNodeWithTag("v216-edit-editor").assertDoesNotExist()
    }
}
