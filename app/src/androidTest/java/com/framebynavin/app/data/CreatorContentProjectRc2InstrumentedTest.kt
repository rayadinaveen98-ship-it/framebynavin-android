package com.framebynavin.app.data

import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import java.util.UUID

@RunWith(AndroidJUnit4::class)
class CreatorContentProjectRc2InstrumentedTest {

    @Test
    fun deliverablePublicationDrivesLegacyLearningWithoutCompletingProject() = runBlocking {
        HardeningTestEnvironment.requireCiEmulator()
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val backup = CreatorBackupManager(context)
        val store = TaskStore(context)
        val postPublish = CreatorPostPublishStore(context)
        val rewards = CreatorRewardStore(context)
        val recovery = CreatorPublicationRecovery(context)
        val original = backup.createBackup()
        val projectId = "rc2-authority-${UUID.randomUUID()}"
        val publishedAt = System.currentTimeMillis() - 120_000L

        val studio = CreatorScriptStudio(
            projectId = projectId,
            revision = 2L,
            hooks = listOf(CreatorHookIdea(id = "hook", text = "Structured hook", selected = true)),
            beats = listOf(CreatorScriptBeat(id = "beat", label = "Beat", narration = "Structured narration")),
        )
        val ready = CreatorPublishWorkflow.ensureGate(
            CreatorDeliverable(
                id = "youtube-main",
                platform = "YouTube",
                format = "Long video",
                title = "RC2 Authority Test",
            )
        ).let { deliverable ->
            deliverable.copy(
                publishGate = deliverable.publishGate.map { it.copy(status = CreatorPublishGateStatus.DONE) },
                status = CreatorDeliverableStatus.READY,
            )
        }
        val workspace = CreatorContentWorkspace(
            revision = 6L,
            hook = studio.selectedHook(),
            script = studio.compiledNarration(),
            scriptStudio = studio,
            deliverables = listOf(ready),
        )
        val task = CreatorTask(
            id = projectId,
            title = "RC2 Authority Test",
            platform = "YouTube",
            contentType = "Long-form",
            status = TaskStatus.WORKING,
            progress = 55,
            workspace = workspace,
        )

        try {
            store.mutate { current -> current.filterNot { it.id == projectId } + task }

            // Simulate the old Alpha3 editor attempting to overwrite Script Studio and publish.
            val unsafeDraft = workspace.copy(
                hook = "Wrong legacy hook",
                script = "Wrong legacy narration",
                scriptStudio = null,
                deliverables = listOf(
                    ready.copy(
                        status = CreatorDeliverableStatus.PUBLISHED,
                        publishedAtMillis = publishedAt,
                        publishedUrl = "https://example.test/bypass",
                    )
                ),
            )
            val protected = CreatorContentStabilization.prepareProjectEditorSave(workspace, unsafeDraft)
            assertEquals("Structured hook", protected.hook)
            assertEquals(studio, protected.scriptStudio)
            assertEquals(CreatorDeliverableStatus.READY, protected.deliverables.single().status)

            // Publish only through the hardened Publish Studio workflow.
            val live = CreatorPublishWorkflow.publish(
                ready,
                publishedAt,
                "https://example.test/live",
                "Creator confirmed live output",
            )
            val liveWorkspace = protected.copy(deliverables = listOf(live))
            store.updateTask(projectId) { current ->
                CreatorContentStabilization.syncParentPublication(current, liveWorkspace)
            }
            recovery.recoverAll()

            val publishedProject = store.load().first { it.id == projectId }
            assertEquals(TaskStatus.WORKING, publishedProject.status)
            assertEquals(55, publishedProject.progress)
            assertEquals(publishedAt, publishedProject.publishedAtMillis)
            assertEquals("https://example.test/live", publishedProject.publishedUrl)
            assertEquals(CreatorDeliverableStatus.PUBLISHED, publishedProject.workspace.deliverables.single().status)
            assertEquals(CreatorPublicationEventKind.PUBLISHED, publishedProject.workspace.deliverables.single().publicationHistory.last().kind)

            val publicationReward = rewards.load().firstOrNull { it.eventKey == "published:$projectId" }
            assertNotNull(publicationReward)
            assertEquals(publishedAt, publicationReward!!.occurredAtMillis)
            val checkpoints = postPublish.load().filter { it.projectId == projectId }
            assertTrue(checkpoints.any { it.kind == PostPublishCheckpointKind.PERFORMANCE_24H })
            assertTrue(checkpoints.any { it.kind == PostPublishCheckpointKind.PERFORMANCE_7D })

            // Reopening the only live output clears only derived publication state, not project work.
            val reopened = CreatorPublishWorkflow.reopen(live, System.currentTimeMillis(), "Needs correction")
            store.updateTask(projectId) { current ->
                CreatorContentStabilization.syncParentPublication(
                    current,
                    current.workspace.copy(deliverables = listOf(reopened)),
                )
            }
            recovery.recoverAll()

            val reopenedProject = store.load().first { it.id == projectId }
            assertEquals(TaskStatus.WORKING, reopenedProject.status)
            assertEquals(0L, reopenedProject.publishedAtMillis)
            assertEquals("", reopenedProject.publishedUrl)
            assertFalse(reopenedProject.workspace.deliverables.single().publicationHistory.isEmpty())
            assertTrue(rewards.load().none { it.eventKey == "published:$projectId" })
            assertTrue(postPublish.load().filter { it.projectId == projectId }
                .none { it.status == PostPublishCheckpointStatus.PENDING })
        } finally {
            CreatorDataGate.nonCancellable { backup.restore(original) }
        }
    }
}
