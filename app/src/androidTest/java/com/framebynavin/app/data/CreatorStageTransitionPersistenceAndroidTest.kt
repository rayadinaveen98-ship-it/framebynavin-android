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

/** Creator stage approvals survive genuine TaskStore JSON and portable backup. */
@RunWith(AndroidJUnit4::class)
class CreatorStageTransitionPersistenceAndroidTest {
    private val context get() = InstrumentationRegistry.getInstrumentation().targetContext

    private fun plannedProject(): CreatorTask = CreatorTask(
        id = "cw5-android-" + UUID.randomUUID(),
        title = "Stage receipts — తెలుగు",
        platform = "YouTube", contentType = "Long-form", dueLabel = "Tomorrow",
        status = TaskStatus.WORKING, workflowStageIndex = 1,
        workspace = CreatorContentWorkspace(
            revision = 8L,
            audience = "Film students",
            stageWork = listOf(CreatorStageWork(
                workflowTemplateId = "youtube_longform",
                stageId = "research",
                notes = "Source notes",
                reviewedAtMillis = 0L,
            )),
        ),
    )

    private fun afterApproval(): CreatorTask {
        val project = plannedProject()
        return CreatorWorkflowTransitionPolicy.approveAndAdvance(
            project, "research", 8L, 1234L,
        )
    }

    @Test fun stageReceiptRoundTripsWithProjectMetadataAndOlderJsonStillLoads() {
        val store = TaskStore(context)
        val approved = afterApproval()
        val result = store.decodeRecoveryTask(store.encodeRecoveryTask(approved))
        assertEquals(approved.workspace.stageTransitions, result.workspace.stageTransitions)
        assertEquals(approved.workspace.stageWork, result.workspace.stageWork)
        assertEquals(approved.workflowStageIndex, result.workflowStageIndex)
        assertEquals(0L, result.publishedAtMillis)
        assertEquals(0L, result.completedAtMillis)
        assertEquals(listOf("stage_notes_not_reviewed"),
            result.workspace.stageTransitions.single().acknowledgedWarningCodes)

        val legacy = JSONArray(store.encodeRecoveryTask(plannedProject()))
        legacy.getJSONObject(0).getJSONObject("workspace").remove("stageTransitions")
        assertTrue(store.decodeRecoveryTask(legacy.toString()).workspace.stageTransitions.isEmpty())
    }

    @Test fun corruptedOrDuplicateStageApprovalCannotBeSilentlyDropped() {
        val store = TaskStore(context)
        val approved = afterApproval()
        val brokenType = JSONArray(store.encodeRecoveryTask(approved))
        brokenType.getJSONObject(0).getJSONObject("workspace")
            .put("stageTransitions", "not-an-array")
        assertThrows(IllegalArgumentException::class.java) {
            store.decodeRecoveryTask(brokenType.toString())
        }
        val duplicate = JSONArray(store.encodeRecoveryTask(approved))
        val approvals = duplicate.getJSONObject(0).getJSONObject("workspace")
            .getJSONArray("stageTransitions")
        approvals.put(approvals.getJSONObject(0))
        assertThrows(IllegalArgumentException::class.java) {
            store.decodeRecoveryTask(duplicate.toString())
        }
        val noIdentity = JSONArray(store.encodeRecoveryTask(approved))
        noIdentity.getJSONObject(0).getJSONObject("workspace")
            .getJSONArray("stageTransitions").getJSONObject(0)
            .put("workflowTemplateId", "")
        assertThrows(IllegalArgumentException::class.java) {
            store.decodeRecoveryTask(noIdentity.toString())
        }
    }

    @Test fun actualCreatorBackupRestoreKeepsApprovalAndCannotManufactureRelease() = runBlocking {
        HardeningTestEnvironment.requireCiEmulator()
        val backups = CreatorBackupManager(context)
        val store = TaskStore(context)
        val previousBackup = backups.createBackup()
        val project = plannedProject()
        try {
            store.mutate { it + project }
            val generation = CreatorDataGate.generation(context)
            val saved = requireNotNull(store.updateTask(project.id, expectedGeneration = generation) {
                CreatorWorkflowTransitionPolicy.approveAndAdvance(
                    it, "research", 8L, 2000L,
                )
            })
            assertEquals(1, saved.workspace.stageTransitions.size)
            assertEquals(0L, saved.publishedAtMillis)
            assertEquals(TaskStatus.WORKING, saved.status)

            val backup = backups.createBackup()
            backups.validate(backup)
            store.updateTask(project.id) {
                it.copy(workspace = it.workspace.copy(
                    stageTransitions = emptyList(), revision = it.workspace.revision + 1L,
                ))
            }
            assertTrue(store.load().first { it.id == project.id }.workspace.stageTransitions.isEmpty())
            CreatorDataGate.nonCancellable { backups.restore(backup) }
            val restored = store.load().first { it.id == project.id }
            assertEquals(saved.workspace.stageTransitions, restored.workspace.stageTransitions)
            assertEquals(saved.workspace.stageWork, restored.workspace.stageWork)
            assertEquals(saved.workflowStageIndex, restored.workflowStageIndex)
            assertFalse(restored.workspace.stageTransitions.isEmpty())
            assertEquals(0L, restored.publishedAtMillis)
        } finally {
            CreatorDataGate.nonCancellable { backups.restore(previousBackup) }
        }
    }
}
