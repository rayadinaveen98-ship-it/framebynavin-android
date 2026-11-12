package com.framebynavin.app.data

import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import kotlinx.coroutines.runBlocking
import org.json.JSONArray
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import java.util.UUID

/**
 * CW2 storage acceptance: the pure JSON tests never change local projects.
 * The transactional test is restricted to disposable CI emulators and restores
 * its original Creator backup even if a check fails.
 */
@RunWith(AndroidJUnit4::class)
class CreatorStageWorkPersistenceAndroidTest {
    private val context get() = InstrumentationRegistry.getInstrumentation().targetContext
    private fun project() = CreatorTask(
        id = "cw2-test-" + UUID.randomUUID(),
        title = "Why Silence Works",
        platform = "YouTube", contentType = "Long-form", dueLabel = "",
        status = TaskStatus.WORKING, workflowStageIndex = 2,
        reminderEnabled = false,
        contentDna = CreatorContentDna(
            creatorModeId = "film_entertainment", archetypeId = "review",
            platform = "YouTube", deliveryFormat = "Long-form",
        ),
        workspace = CreatorContentWorkspace(
            revision = 23L,
            angle = "Why silence works",
            references = listOf(CreatorProjectReference(
                id = "source", label = "Reference", url = "https://example.org/reference")),
            assets = listOf(CreatorProjectAsset(
                id = "image", label = "Reference image", location = "content://provider/image")),
            deliverables = listOf(CreatorDeliverable(
                id = "yt", platform = "YouTube", format = "Long video", title = "Review")),
        ),
    )

    private fun stage() = CreatorStageWork(
        workflowTemplateId = "v150_film_review",
        stageId = "evidence",
        notes = "Stage notes — do not trim, rewrite or lose. తెలుగు",
        checklist = listOf(CreatorStageChecklistItem(
            id = "task", title = "Verify timestamp", status = CreatorChecklistStatus.DONE)),
        evidence = listOf(CreatorStageEvidence(
            id = "moment", claim = "A quiet scene creates tension",
            timecode = "01:21:09", notes = "Check audio mix",
            referenceId = "source", assetId = "image")),
        decisions = listOf(CreatorStageDecision(
            id = "decision", text = "Keep first example spoiler-safe", recordedAtMillis = 42L)),
        linkedReferenceIds = listOf("source"),
        linkedAssetIds = listOf("image"),
        linkedDeliverableIds = listOf("yt"),
        reviewedAtMillis = 99L,
    )

    @Test fun stageJsonRoundTripPreservesAllTextLinksAndSharedScript() {
        val store = TaskStore(context)
        val project = project().let { it.copy(workspace = CreatorStageWorkPolicy.put(it, stage())) }
        val decoded = store.decodeRecoveryTask(store.encodeRecoveryTask(project))
        assertEquals(project.id, decoded.id)
        assertEquals(project.workspace.stageWork, decoded.workspace.stageWork)
        assertEquals(project.workspace.references, decoded.workspace.references)
        assertEquals(project.workspace.assets, decoded.workspace.assets)
        assertEquals(project.workspace.deliverables, decoded.workspace.deliverables)
        assertEquals(project.workflowStageIndex, decoded.workflowStageIndex)
        assertFalse(decoded.workspace.stageWork.single().notes.isBlank())
    }

    @Test fun preCw2LegacyTaskLoadsWithEmptyStageWorkAndNoNewRequirements() {
        val store = TaskStore(context)
        val current = project()
        val original = JSONArray(store.encodeRecoveryTask(current))
        original.getJSONObject(0).getJSONObject("workspace").remove("stageWork")
        val decoded = store.decodeRecoveryTask(original.toString())
        assertTrue(decoded.workspace.stageWork.isEmpty())
        assertEquals(current.workspace, decoded.workspace)
    }

    @Test fun malformedStageFieldRejectsRestoreRatherThanDroppingCreatorWork() {
        val store = TaskStore(context)
        val current = project().let { it.copy(workspace = CreatorStageWorkPolicy.put(it, stage())) }
        val json = JSONArray(store.encodeRecoveryTask(current))
        json.getJSONObject(0).getJSONObject("workspace").put("stageWork", "not-an-array")
        assertThrows(IllegalArgumentException::class.java) {
            store.decodeRecoveryTask(json.toString())
        }
        val withDuplicate = JSONArray(store.encodeRecoveryTask(current))
        val stages = withDuplicate.getJSONObject(0).getJSONObject("workspace").getJSONArray("stageWork")
        stages.put(stages.getJSONObject(0))
        assertThrows(IllegalArgumentException::class.java) {
            store.decodeRecoveryTask(withDuplicate.toString())
        }
    }

    @Test fun guardedUpdateAndPortableExportKeepStageWorkWithoutAdvancingWorkflow() = runBlocking {
        HardeningTestEnvironment.requireCiEmulator()
        val backup = CreatorBackupManager(context)
        val store = TaskStore(context)
        val original = backup.createBackup()
        val task = project()
        try {
            store.mutate { it + task }
            val epoch = CreatorDataGate.generation(context)
            val saved = requireNotNull(store.saveStageWork(
                projectId = task.id,
                expectedGeneration = epoch,
                expectedWorkspaceRevision = task.workspace.revision,
                stage = stage(),
            ))
            assertEquals(24L, saved.workspace.revision)
            assertEquals(task.workflowStageIndex, saved.workflowStageIndex)
            assertEquals(task.status, saved.status)
            assertEquals(task.workspace.deliverables, saved.workspace.deliverables)
            assertEquals(stage(), saved.workspace.stageWork.single())

            val exported = store.exportJson()
            val restored = store.decodeRecoveryTask(
                JSONArray(exported).let { array ->
                    (0 until array.length())
                        .map { array.getJSONObject(it) }
                        .first { it.getString("id") == task.id }
                        .let { JSONArray().put(it).toString() }
                }
            )
            assertEquals(saved.workspace.stageWork, restored.workspace.stageWork)
            assertEquals(saved.workspace.assets, restored.workspace.assets)

            // Also verify the actual Creator backup envelope and restore path.
            val stageBackup = backup.createBackup()
            backup.validate(stageBackup)
            store.updateTask(task.id) { current ->
                current.copy(workspace = current.workspace.copy(stageWork = emptyList(),
                    revision = current.workspace.revision + 1L))
            }
            assertTrue(store.load().first { it.id == task.id }.workspace.stageWork.isEmpty())
            CreatorDataGate.nonCancellable { backup.restore(stageBackup) }
            val returned = store.load().first { it.id == task.id }
            assertEquals(saved.workspace.stageWork, returned.workspace.stageWork)
            assertEquals(saved.workspace.assets, returned.workspace.assets)
            assertEquals(saved.workflowStageIndex, returned.workflowStageIndex)
            assertEquals(saved.reminderEnabled, returned.reminderEnabled)
            val postRestoreEpoch = CreatorDataGate.generation(context)

            // A stale editor cannot overwrite the newest stage revision.
            var failed = false
            try {
                store.saveStageWork(task.id, postRestoreEpoch, 23L, stage().copy(notes = "Old draft"))
            } catch (_: IllegalStateException) {
                failed = true
            }
            assertTrue("Stale revision must not overwrite saved stage notes", failed)
            assertEquals(stage(), store.load().first { it.id == task.id }.workspace.stageWork.single())
        } finally {
            CreatorDataGate.nonCancellable { backup.restore(original) }
        }
    }
}
