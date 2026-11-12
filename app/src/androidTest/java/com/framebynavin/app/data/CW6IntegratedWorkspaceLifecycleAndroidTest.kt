package com.framebynavin.app.data

import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import java.util.UUID

/**
 * CW6 integrated, deterministic Content Workspace journey.
 * Mutating storage is allowed only on disposable, opt-in CI emulators.
 * No remote APIs, project media, owner credentials or real personal data.
 */
@RunWith(AndroidJUnit4::class)
class CW6IntegratedWorkspaceLifecycleAndroidTest {
    private val context get() = InstrumentationRegistry.getInstrumentation().targetContext

    private fun reviewProject(id: String) = CreatorTask(
        id = id,
        title = "Why Silence Works — తెలుగు",
        platform = "YouTube", contentType = "Long-form", dueLabel = "Later",
        status = TaskStatus.WORKING,
        contentDna = CreatorContentDna(
            creatorModeId = "film_entertainment", archetypeId = "review",
            platform = "YouTube", deliveryFormat = "Long-form",
        ),
        workflowStageIndex = 2, // Evidence
        workspace = CreatorContentWorkspace(
            revision = 31L,
            angle = "How silence guides tension",
            references = listOf(CreatorProjectReference(
                id = "reference-1", label = "Director interview",
                url = "https://example.org/interview",
            )),
            assets = listOf(CreatorProjectAsset(
                id = "asset-1", label = "Scene frame",
                location = "content://example/scene",
            )),
            scriptStudio = CreatorScriptStudio(
                projectId = id, revision = 4L,
                beats = listOf(CreatorScriptBeat(
                    id = "opening", label = "Main video intro",
                    narration = "Original long-form analysis narration",
                )),
            ),
            deliverables = listOf(
                CreatorDeliverable(
                    id = "main", platform = "YouTube",
                    format = "Long video", title = "Why Silence Works",
                ),
                CreatorDeliverable(
                    id = "reel", platform = "Instagram",
                    format = "Reel", title = "Silence in 30 seconds",
                    parentDeliverableId = "main",
                ),
            ),
        ),
    )

    private fun anotherProject(id: String) = CreatorTask(
        id = id,
        title = "Independent technical review",
        platform = "YouTube", contentType = "Long-form", dueLabel = "Later",
        status = TaskStatus.WORKING,
        workflowStageIndex = 0,
        workspace = CreatorContentWorkspace(
            revision = 7L, angle = "Tech analysis",
            references = listOf(CreatorProjectReference(
                id = "tech-source", label = "Specification sheet",
                url = "https://example.org/spec",
            )),
        ),
    )

    private fun integratedReview(id: String): CreatorTask {
        val base = reviewProject(id)
        val staged = base.copy(workspace = CreatorStageWorkPolicy.put(
            base,
            CreatorStageWork(
                workflowTemplateId = CreatorWorkflowEngine.templateFor(base).id,
                stageId = "evidence",
                notes = "The long-form video needs scene evidence.",
                linkedReferenceIds = listOf("reference-1"),
                linkedAssetIds = listOf("asset-1"),
                linkedDeliverableIds = listOf("main"),
                evidence = listOf(CreatorStageEvidence(
                    id = "scene-1", claim = "A held silent frame changes rhythm",
                    timecode = "00:21:14", notes = "Check the original scene",
                )),
            ),
        ))
        val firstReel = CreatorOutputCraftPolicy.saveScript(
            staged,
            CreatorOutputScript(
                deliverableId = "reel", hook = "The power of one quiet scene",
                narration = "First short Reel narration",
                adaptedFromDeliverableId = "main",
                adaptedFromScriptRevision = 4L,
            ),
            1_700_000_000_000L,
        )
        val secondReel = CreatorOutputCraftPolicy.saveScript(
            staged.copy(workspace = firstReel),
            requireNotNull(CreatorOutputCraftPolicy.scriptFor(firstReel, "reel"))
                .copy(narration = "Revised independent Reel narration"),
            1_700_000_001_000L,
        )
        val withMainCut = CreatorOutputCraftPolicy.appendEditVersion(
            staged.copy(workspace = secondReel),
            "main", "First rough cut", "https://example.org/cuts/1",
            "Check the silence at 21:14", 1_700_000_002_000L,
        )
        return CreatorWorkflowTransitionPolicy.approveAndAdvance(
            staged.copy(workspace = withMainCut),
            expectedStageId = "evidence", expectedWorkspaceRevision = 31L,
            atMillis = 1_700_000_003_000L,
        )
    }

    @Test fun twoProjectsCanRoundTripWithoutStageScriptOrPublicationCrossContamination() {
        val store = TaskStore(context)
        val review = integratedReview("cw6-review-" + UUID.randomUUID())
        val other = anotherProject("cw6-tech-" + UUID.randomUUID())
        val raw = store.exportSyntheticProjectsForTest(listOf(review, other))
        val decoded = store.decodeSyntheticProjectsForTest(raw)
        assertEquals(2, decoded.size)
        val loadedReview = decoded.single { it.id == review.id }
        val loadedOther = decoded.single { it.id == other.id }
        assertEquals(review.workspace, loadedReview.workspace)
        assertEquals(other.workspace, loadedOther.workspace)
        assertEquals(3, loadedReview.workflowStageIndex)
        assertEquals(0, loadedOther.workflowStageIndex)
        assertEquals(2, loadedReview.workspace.outputScripts.single().history.size)
        assertEquals("Revised independent Reel narration",
            loadedReview.workspace.outputScripts.single().narration)
        assertEquals("Original long-form analysis narration",
            loadedReview.workspace.scriptStudio?.compiledNarration())
        assertTrue(loadedOther.workspace.outputScripts.isEmpty())
        assertTrue(loadedOther.workspace.stageWork.isEmpty())
        assertTrue(loadedOther.workspace.stageTransitions.isEmpty())
        assertEquals(0L, loadedReview.publishedAtMillis)
        assertEquals(0L, loadedReview.completedAtMillis)
        assertEquals(CreatorDeliverableStatus.PLANNED,
            loadedReview.workspace.deliverables.first().status)
    }

    @Test fun publishCorrectionReopenAndRepublishNeverEraseHistoricalReleaseOrDerivativeWork() {
        val review = integratedReview("cw6-publish-" + UUID.randomUUID())
        val originalMain = review.workspace.deliverables.single { it.id == "main" }
        val originalReel = review.workspace.deliverables.single { it.id == "reel" }

        val checkedMain = CreatorPublishWorkflow.ensureGate(originalMain).let { prepared ->
            prepared.copy(
                status = CreatorDeliverableStatus.READY,
                publishGate = prepared.publishGate.map {
                    it.copy(status = CreatorPublishGateStatus.DONE)
                },
            )
        }
        val live = CreatorPublishWorkflow.publish(
            checkedMain, 1_700_000_010_000L, "https://example.org/watch/main"
        )
        val changed = CreatorPublishWorkflow.recordPublishedUpdate(
            live.copy(title = "Corrected Why Silence Works"),
            1_700_000_020_000L, "https://example.org/watch/main",
            "Corrected the public title",
        )
        val afterCorrect = CreatorContentStabilization.syncParentPublication(
            review, review.workspace.copy(deliverables = listOf(changed, originalReel)),
        )
        assertEquals(1_700_000_010_000L, afterCorrect.publishedAtMillis)
        assertEquals("https://example.org/watch/main", afterCorrect.publishedUrl)
        assertEquals(2, changed.publicationHistory.size)
        assertEquals(CreatorDeliverableStatus.PLANNED, originalReel.status)
        assertEquals(review.workspace.outputScripts, afterCorrect.workspace.outputScripts)
        assertEquals(review.workspace.editVersions, afterCorrect.workspace.editVersions)
        assertEquals(review.workspace.stageTransitions, afterCorrect.workspace.stageTransitions)

        val reopened = CreatorPublishWorkflow.reopen(
            changed, 1_700_000_030_000L, "Original export needs a repair"
        )
        val afterReopen = CreatorContentStabilization.syncParentPublication(
            afterCorrect,
            afterCorrect.workspace.copy(deliverables = listOf(reopened, originalReel)),
        )
        assertEquals(0L, afterReopen.publishedAtMillis)
        assertEquals("", afterReopen.publishedUrl)
        assertEquals(CreatorDeliverableStatus.READY, reopened.status)
        assertEquals(3, reopened.publicationHistory.size)
        assertEquals(
            CreatorPublicationEventKind.PUBLISHED,
            reopened.publicationHistory[0].kind,
        )
        assertEquals(
            CreatorPublicationEventKind.REOPENED,
            reopened.publicationHistory[2].kind,
        )

        val liveAgain = CreatorPublishWorkflow.publish(
            reopened, 1_700_000_040_000L, "https://example.org/watch/final"
        )
        val restored = CreatorContentStabilization.syncParentPublication(
            afterReopen,
            afterReopen.workspace.copy(deliverables = listOf(liveAgain, originalReel)),
        )
        val newRecord = restored.workspace.deliverables.single { it.id == "main" }
        assertEquals(CreatorDeliverableStatus.PUBLISHED, newRecord.status)
        assertEquals(4, newRecord.publicationHistory.size)
        assertEquals(
            CreatorPublicationEventKind.PUBLISHED,
            newRecord.publicationHistory.last().kind,
        )
        assertEquals(1_700_000_040_000L, restored.publishedAtMillis)
        assertEquals("https://example.org/watch/final", restored.publishedUrl)
        assertEquals(review.workspace.outputScripts, restored.workspace.outputScripts)
        assertEquals(review.workspace.editVersions, restored.workspace.editVersions)
        assertEquals(review.workspace.stageWork, restored.workspace.stageWork)
        assertEquals(review.workspace.stageTransitions, restored.workspace.stageTransitions)
        assertEquals(review.workflowStageIndex, restored.workflowStageIndex)
        assertEquals(TaskStatus.WORKING, restored.status)
    }

    @Test fun trueBackupRestoreRetainsBothProjectsAndRejectsPreRestoreEpoch() = runBlocking {
        HardeningTestEnvironment.requireCiEmulator()
        val store = TaskStore(context)
        val backups = CreatorBackupManager(context)
        val originalBackup = backups.createBackup()
        val review = integratedReview("cw6-review-" + UUID.randomUUID())
        val other = anotherProject("cw6-other-" + UUID.randomUUID())
        try {
            store.mutate { tasks -> tasks + review + other }
            val oldEpoch = CreatorDataGate.generation(context)
            val backup = backups.createBackup()
            backups.validate(backup)
            store.updateTask(review.id) { current ->
                current.copy(workspace = current.workspace.copy(
                    stageWork = emptyList(), outputScripts = emptyList(),
                    editVersions = emptyList(), stageTransitions = emptyList(),
                    revision = current.workspace.revision + 1L,
                ))
            }
            assertTrue(store.load().first { it.id == review.id }.workspace.stageWork.isEmpty())
            CreatorDataGate.nonCancellable { backups.restore(backup) }
            val restored = store.load()
            val returnedReview = restored.single { it.id == review.id }
            val returnedOther = restored.single { it.id == other.id }
            assertEquals(review.workspace, returnedReview.workspace)
            assertEquals(other.workspace, returnedOther.workspace)
            assertEquals(review.workflowStageIndex, returnedReview.workflowStageIndex)
            assertEquals(0L, returnedReview.publishedAtMillis)
            assertEquals("Original long-form analysis narration",
                returnedReview.workspace.scriptStudio?.compiledNarration())
            assertFalse(returnedReview.workspace.stageTransitions.isEmpty())
            assertTrue(returnedOther.workspace.stageTransitions.isEmpty())
            assertNotEquals(oldEpoch, CreatorDataGate.generation(context))
            var staleRejected = false
            try {
                store.saveStageWork(review.id, oldEpoch, review.workspace.revision,
                    CreatorStageWork(
                        workflowTemplateId = "v150_film_review", stageId = "evidence",
                        notes = "Stale edit after restore",
                    ))
            } catch (_: IllegalStateException) {
                staleRejected = true
            }
            assertTrue("Pre-restore editor must never overwrite saved stage notes", staleRejected)
        } finally {
            CreatorDataGate.nonCancellable { backups.restore(originalBackup) }
        }
    }
}
