package com.framebynavin.app.ui

import android.graphics.Bitmap
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.framebynavin.app.data.*
import com.framebynavin.app.ui.theme.FrameByNavinTheme
import java.io.File
import java.util.UUID
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/** CW4 native Android acceptance using isolated synthetic project records. */
@RunWith(AndroidJUnit4::class)
class CW4OutputCraftDeskUiTest {
    @get:Rule val rule = createComposeRule()
    private val context get() = InstrumentationRegistry.getInstrumentation().targetContext

    private fun project() = CreatorTask(
        id = "cw4-ui-" + UUID.randomUUID(),
        title = "Why Silence Works",
        platform = "YouTube", contentType = "Long-form", dueLabel = "",
        status = TaskStatus.WORKING,
        workspace = CreatorContentWorkspace(
            revision = 18L,
            scriptStudio = CreatorScriptStudio(projectId = "main", revision = 3L,
                beats = listOf(CreatorScriptBeat(id = "beat", label = "Original",
                    narration = "The main script should not change"))),
            deliverables = listOf(
                CreatorDeliverable(id = "main", platform = "YouTube",
                    format = "Long video", title = "Full Film Analysis"),
                CreatorDeliverable(id = "reel", platform = "Instagram",
                    format = "Reel", title = "30 second edit",
                    parentDeliverableId = "main"),
            ),
        ),
    )

    @Test fun realOutputDeskSavesIndependentReelScriptAndMainRemainsUnchanged() {
        HardeningTestEnvironment.requireCiEmulator()
        val original = project()
        var capturedScript: CreatorOutputScript? = null
        var capturedRevision = -1L
        try {
            rule.setContent {
                FrameByNavinTheme {
                    CW4OutputCraftDesk(
                        task = original, onBack = {}, onOpenMainScript = {},
                        onSaveDerivativeScript = { rev, script ->
                            capturedRevision = rev; capturedScript = script
                        },
                        onAddEditVersion = { _, _, _, _, _ -> },
                        onReviewEditVersion = { _, _, _ -> },
                    )
                }
            }
            rule.onNodeWithTag("cw4-output-desk").assertExists()
            rule.onNodeWithText("Instagram · Reel").performScrollTo().performClick()
            rule.onNodeWithTag("cw4-narration").performScrollTo()
                .performTextInput("An independent vertical narration.")
            rule.onNodeWithTag("cw4-save-script").performScrollTo().performClick()
            rule.runOnIdle {
                assertEquals(18L, capturedRevision)
                val draft = requireNotNull(capturedScript)
                assertEquals("reel", draft.deliverableId)
                assertEquals("An independent vertical narration.", draft.narration)
                assertEquals(0L, draft.revision)
                val output = CreatorOutputCraftPolicy.saveScript(original, draft, 100L)
                assertEquals("The main script should not change",
                    output.scriptStudio?.compiledNarration())
                assertEquals(original.workspace.deliverables, output.deliverables)
                assertEquals("An independent vertical narration.",
                    output.outputScripts.single().narration)
            }
            rule.waitForIdle()
            val image = rule.onRoot().captureToImage().asAndroidBitmap()
            val dir = File(context.filesDir, "cw4-qa").apply { mkdirs() }
            val output = File(dir, "cw4-reel-script.png")
            output.outputStream().use {
                assertTrue(image.compress(Bitmap.CompressFormat.PNG, 100, it))
            }
            assertTrue(output.length() > 4_000L)
        } finally {
            CreatorOutputCraftDraftStore(context).clear(
                original.id, "reel", explicitDiscard = true,
            )
        }
    }

    @Test fun editCutIsLoggedWithoutPublishingAndWithoutReplacingScript() {
        HardeningTestEnvironment.requireCiEmulator()
        val original = project()
        var savedOutputId = ""
        var capturedLabel = ""
        try {
            rule.setContent {
                FrameByNavinTheme {
                    CW4OutputCraftDesk(
                        task = original, onBack = {}, onOpenMainScript = {},
                        onSaveDerivativeScript = { _, _ -> },
                        onAddEditVersion = { rev, outputId, label, _, _ ->
                            assertEquals(18L, rev)
                            savedOutputId = outputId
                            capturedLabel = label
                        },
                        onReviewEditVersion = { _, _, _ -> },
                    )
                }
            }
            rule.onNodeWithTag("cw4-new-cut-label").performScrollTo()
                .performTextInput("Main Rough Cut v1")
            rule.onNodeWithTag("cw4-save-cut").performScrollTo().performClick()
            rule.runOnIdle {
                assertEquals("main", savedOutputId)
                assertEquals("Main Rough Cut v1", capturedLabel)
                assertTrue(original.workspace.editVersions.isEmpty())
                assertEquals(0L, original.publishedAtMillis)
                assertEquals("The main script should not change",
                    original.workspace.scriptStudio?.compiledNarration())
            }
        } finally {
            CreatorOutputCraftDraftStore(context).clear(
                original.id, "main", explicitDiscard = true,
            )
        }
    }
}
