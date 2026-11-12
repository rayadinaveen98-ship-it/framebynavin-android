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

/** Real Android JSON and portable-backup acceptance for CW4. */
@RunWith(AndroidJUnit4::class)
class CreatorOutputCraftPersistenceAndroidTest {
    private val context get() = InstrumentationRegistry.getInstrumentation().targetContext

    private fun task() = CreatorTask(
        id = "cw4-android-" + UUID.randomUUID(),
        title = "Why Silence Works — తెలుగు",
        platform = "YouTube", contentType = "Long-form", dueLabel = "Tomorrow",
        status = TaskStatus.WORKING, workflowStageIndex = 4,
        workspace = CreatorContentWorkspace(
            revision = 11L,
            scriptStudio = CreatorScriptStudio(projectId = "initial",
                revision = 5L,
                beats = listOf(CreatorScriptBeat(id = "original", label = "Opening",
                    narration = "Original movie review")),
            ),
            deliverables = listOf(
                CreatorDeliverable(id = "main", platform = "YouTube",
                    format = "Long video", title = "Film review"),
                CreatorDeliverable(id = "reel", platform = "Instagram",
                    format = "Reel", title = "Derivative reel",
                    parentDeliverableId = "main"),
            ),
        ),
    )

    private fun withCraft(original: CreatorTask): CreatorTask {
        val firstScript = CreatorOutputCraftPolicy.saveScript(
            original, CreatorOutputScript(deliverableId = "reel",
                hook = "A quiet moment", narration = "Reel narration — తెలుగు",
                visualNotes = "Crop to portrait",
                adaptedFromDeliverableId = "main", adaptedFromScriptRevision = 5L,
            ), 1000L,
        )
        val secondScript = CreatorOutputCraftPolicy.saveScript(
            original.copy(workspace = firstScript),
            requireNotNull(CreatorOutputCraftPolicy.scriptFor(firstScript, "reel"))
                .copy(narration = "Revised derivative narration"), 1500L,
        )
        val withVersion = CreatorOutputCraftPolicy.appendEditVersion(
            original.copy(workspace = secondScript), "reel",
            "Vertical cut v1", "https://example.org/review/reel",
            "Check subtitles", 2000L,
        )
        val versionId = withVersion.editVersions.single().id
        val reviewed = CreatorOutputCraftPolicy.reviewVersion(
            original.copy(workspace = withVersion), versionId,
            CreatorEditReviewStatus.REVIEW, 2500L,
        )
        val approved = CreatorOutputCraftPolicy.reviewVersion(
            original.copy(workspace = reviewed), versionId,
            CreatorEditReviewStatus.APPROVED, 3000L,
        )
        return original.copy(workspace = approved)
    }

    @Test fun fullJsonRoundTripKeepsDerivativeHistoryWithoutEditingMainNarration() {
        val store = TaskStore(context)
        val original = withCraft(task())
        val restored = store.decodeRecoveryTask(store.encodeRecoveryTask(original))
        assertEquals(original.workspace.outputScripts, restored.workspace.outputScripts)
        assertEquals(original.workspace.editVersions, restored.workspace.editVersions)
        assertEquals(original.workspace.scriptStudio?.compiledNarration(),
            restored.workspace.scriptStudio?.compiledNarration())
        assertEquals(2, restored.workspace.outputScripts.single().history.size)
        assertEquals(CreatorEditReviewStatus.APPROVED,
            restored.workspace.editVersions.single().status)
        assertEquals(original.workspace.deliverables, restored.workspace.deliverables)
        assertEquals(original.workflowStageIndex, restored.workflowStageIndex)
    }

    @Test fun oldCw3WorkspaceLoadsWithoutCraftAndCorruptHistoryFailsClosed() {
        val store = TaskStore(context)
        val previous = task()
        val json = JSONArray(store.encodeRecoveryTask(previous))
        val old = json.getJSONObject(0).getJSONObject("workspace")
        old.remove("outputScripts")
        old.remove("editVersions")
        val decoded = store.decodeRecoveryTask(json.toString())
        assertTrue(decoded.workspace.outputScripts.isEmpty())
        assertTrue(decoded.workspace.editVersions.isEmpty())
        assertEquals(previous.workspace.scriptStudio?.compiledNarration(),
            decoded.workspace.scriptStudio?.compiledNarration())

        val withHistory = withCraft(previous)
        val tampered = JSONArray(store.encodeRecoveryTask(withHistory))
        val craft = tampered.getJSONObject(0).getJSONObject("workspace")
        craft.getJSONArray("outputScripts").getJSONObject(0).put("history", JSONArray())
        assertThrows(IllegalArgumentException::class.java) {
            store.decodeRecoveryTask(tampered.toString())
        }
        val wrongType = JSONArray(store.encodeRecoveryTask(withHistory))
        wrongType.getJSONObject(0).getJSONObject("workspace")
            .put("editVersions", "not-an-array")
        assertThrows(IllegalArgumentException::class.java) {
            store.decodeRecoveryTask(wrongType.toString())
        }
    }

    @Test fun guardedWritesAndPortableBackupPreserveOutputCraftAndRejectStaleSaves() = runBlocking {
        HardeningTestEnvironment.requireCiEmulator()
        val store = TaskStore(context)
        val backups = CreatorBackupManager(context)
        val originalBackup = backups.createBackup()
        val starting = task()
        try {
            store.mutate { current -> current + starting }
            val epoch = CreatorDataGate.generation(context)
            val script = requireNotNull(store.saveDerivativeScript(
                starting.id, epoch, 11L,
                CreatorOutputScript(deliverableId = "reel", narration = "Reel narration"),
                4000L,
            ))
            assertEquals(12L, script.workspace.revision)
            assertEquals("Original movie review",
                script.workspace.scriptStudio?.compiledNarration())
            val withCut = requireNotNull(store.addOutputEditVersion(
                starting.id, epoch, 12L, "reel", "Cut 1", "",
                "First external edit", 5000L,
            ))
            val id = withCut.workspace.editVersions.single().id
            val reviewed = requireNotNull(store.reviewOutputEditVersion(
                starting.id, epoch, 13L, id, CreatorEditReviewStatus.REVIEW, 6000L,
            ))
            assertEquals(CreatorEditReviewStatus.REVIEW,
                reviewed.workspace.editVersions.single().status)
            assertEquals(0L, reviewed.publishedAtMillis)
            assertEquals(TaskStatus.WORKING, reviewed.status)
            assertEquals(starting.workflowStageIndex, reviewed.workflowStageIndex)

            var staleRejected = false
            try {
                store.saveDerivativeScript(starting.id, epoch, 11L,
                    CreatorOutputScript(deliverableId = "reel", narration = "Old edit"), 7000L)
            } catch (_: IllegalStateException) {
                staleRejected = true
            }
            assertTrue(staleRejected)
            assertEquals("Reel narration",
                store.load().first { it.id == starting.id }.workspace.outputScripts.single().narration)

            val portable = backups.createBackup()
            backups.validate(portable)
            store.updateTask(starting.id) { latest ->
                latest.copy(workspace = latest.workspace.copy(
                    outputScripts = emptyList(), editVersions = emptyList(),
                    revision = latest.workspace.revision + 1L,
                ))
            }
            assertTrue(store.load().first { it.id == starting.id }.workspace.outputScripts.isEmpty())
            CreatorDataGate.nonCancellable { backups.restore(portable) }
            val restored = store.load().first { it.id == starting.id }
            assertEquals(reviewed.workspace.outputScripts, restored.workspace.outputScripts)
            assertEquals(reviewed.workspace.editVersions, restored.workspace.editVersions)
            assertEquals(reviewed.workspace.scriptStudio?.compiledNarration(),
                restored.workspace.scriptStudio?.compiledNarration())
            assertFalse(restored.workspace.outputScripts.isEmpty())
        } finally {
            CreatorDataGate.nonCancellable { backups.restore(originalBackup) }
        }
    }
}
