package com.framebynavin.app.ui

import android.graphics.Bitmap
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.framebynavin.app.data.*
import com.framebynavin.app.ui.theme.FrameByNavinTheme
import java.io.File
import java.util.UUID
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/** Native, editable CW3 screen on synthetic projects only. */
@RunWith(AndroidJUnit4::class)
class CW3StageDeskUiTest {
    @get:Rule val rule = createComposeRule()
    private val context get() = InstrumentationRegistry.getInstrumentation().targetContext

    private fun project() = CreatorTask(
        id = "cw3-qa-" + UUID.randomUUID(),
        title = "Why Silence Works",
        platform = "YouTube", contentType = "Long-form",
        dueLabel = "Tomorrow", status = TaskStatus.WORKING, workflowStageIndex = 2,
        contentDna = CreatorContentDna(
            creatorModeId = "film_entertainment", archetypeId = "review",
            platform = "YouTube", deliveryFormat = "Long-form",
        ),
        workspace = CreatorContentWorkspace(
            revision = 14L,
            angle = "Silence and emotional immersion",
            references = listOf(CreatorProjectReference(
                id = "source-1", label = "Source interview",
                url = "https://example.org/interview")),
            assets = listOf(CreatorProjectAsset(
                id = "asset-1", label = "Scene still",
                location = "content://provider/scene")),
            deliverables = listOf(CreatorDeliverable(
                id = "main", platform = "YouTube", format = "Long video",
                title = "Why Silence Works")),
        ),
    )

    @Test fun evidenceDeskEditsOnlyItsStageAndNeverAdvancesProject() {
        HardeningTestEnvironment.requireCiEmulator()
        val task = project()
        var observedRevision = -1L
        var proposed: CreatorStageWork? = null
        val evidence = CreatorWorkflowEngine.templateFor(task).stages.first { it.id == "evidence" }
        try {
            rule.setContent {
                FrameByNavinTheme {
                    CW3StageDesk(
                        task = task, stage = evidence,
                        onSave = { rev, draft -> observedRevision = rev; proposed = draft },
                        onBack = {},
                    )
                }
            }
            rule.onNodeWithTag("cw3-stage-desk").assertExists()
            rule.onNodeWithText("Evidence desk").assertExists()
            rule.onNodeWithTag("cw3-stage-notes").performTextInput(
                "Scene evidence and creative decisions are independent from the narration."
            )
            rule.onNodeWithTag("cw3-new-task").performScrollTo()
                .performTextInput("Check original scene")
            rule.onNodeWithText("ADD", useUnmergedTree = true).performClick()
            rule.onNodeWithTag("cw3-evidence-claim").performScrollTo()
                .performTextInput("A long silence increases tension")
            rule.onNodeWithTag("cw3-evidence-timecode").performScrollTo()
                .performTextInput("00:21:14")
            rule.onNodeWithText("ADD EVIDENCE").performScrollTo().performClick()
            rule.onNodeWithTag("cw3-save-stage").performScrollTo().performClick()
            rule.runOnIdle {
                assertEquals(14L, observedRevision)
                val saved = requireNotNull(proposed)
                assertEquals("v150_film_review", saved.workflowTemplateId)
                assertEquals("evidence", saved.stageId)
                assertEquals(1, saved.checklist.size)
                assertEquals(1, saved.evidence.size)
                assertEquals("00:21:14", saved.evidence.single().timecode)
                val isolated = CreatorStageWorkPolicy.put(task, saved)
                assertEquals(task.workspace.angle, isolated.angle)
                assertEquals(task.workspace.deliverables, isolated.deliverables)
                assertEquals(task.workspace.references, isolated.references)
                assertEquals(task.workspace.assets, isolated.assets)
                assertEquals(task.workflowStageIndex, 2)
                assertEquals(TaskStatus.WORKING, task.status)
            }
            val image = rule.onRoot().captureToImage().asAndroidBitmap()
            val out = File(context.filesDir, "cw3-visuals").apply { mkdirs() }
            val f = File(out, "cw3-evidence-desk.png")
            f.outputStream().use { stream -> assertTrue(image.compress(Bitmap.CompressFormat.PNG, 100, stream)) }
            assertTrue(f.length() > 4_000L)
        } finally {
            CreatorStageDraftStore(context).clear(task.id, "v150_film_review", "evidence",
                explicitDiscard = true)
        }
    }

    @Test fun editStageIsClearlyAProductionLogNotAVideoEditor() {
        HardeningTestEnvironment.requireCiEmulator()
        val task = project()
        val edit = CreatorWorkflowEngine.templateFor(task).stages.first { it.id == "edit" }
        try {
            rule.setContent {
                FrameByNavinTheme {
                    CW3StageDesk(task, edit, onSave = { _, _ -> }, onBack = {})
                }
            }
            rule.onNodeWithText("Production and cut log").assertExists()
            rule.onNodeWithText("Review rough cut", substring = true).assertExists()
            rule.onNodeWithTag("cw3-stage-notes").assertIsDisplayed()
        } finally {
            CreatorStageDraftStore(context).clear(task.id, "v150_film_review", "edit",
                explicitDiscard = true)
        }
    }
}
