package com.framebynavin.app.data

import android.content.Context
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import java.util.UUID

@RunWith(AndroidJUnit4::class)
class CreatorPublicationRecoveryV183Test {
    private fun task() = CreatorTask(id = "recovery-${UUID.randomUUID()}", title = "Synthetic film",
        platform = "YouTube", contentType = "Long-form", dueLabel = "Today")

    private suspend fun fixture(block: suspend (Context, TaskStore, CreatorPublicationRecovery) -> Unit) {
        HardeningTestEnvironment.requireCiEmulator()
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val backup = CreatorBackupManager(context)
        val original = backup.createBackup()
        val recovery = CreatorPublicationRecovery(context)
        try {
            recovery.recoverAll()
            block(context, TaskStore(context), recovery)
        } finally {
            backup.restore(original)
        }
    }

    @Test fun processDeathAfterProjectSaveRepairsOnlyAuthoritativePublication(): Unit = runBlocking {
        fixture { context, tasks, recovery ->
            val project = task()
            tasks.mutate { it + project }
            val epoch = CreatorDataGate.generation(context)
            val at = System.currentTimeMillis() - 120_000L
            recovery.begin(epoch, project.id)
            tasks.updateTask(project.id, epoch) { it.copy(publishedAtMillis = at) }
            // Simulate a process ending before reward/checkpoint follow-up writes.
            val first = recovery.recoverAll()
            assertTrue(first.any { it.eventKey == "published:${project.id}" })
            assertEquals(1, CreatorRewardStore(context).load().count { it.eventKey == "published:${project.id}" })
            assertEquals(2, CreatorPostPublishStore(context).load().count { it.projectId == project.id })
            assertTrue(recovery.recoverAll().isEmpty())
            assertFalse(context.getSharedPreferences("creator_publication_recovery_v183", 0).contains("pending"))
        }
    }

    @Test fun intentBeforeSourceSaveCannotInventPublicationOrFinalStageReward(): Unit = runBlocking {
        fixture { context, tasks, recovery ->
            val project = task()
            val template = CreatorWorkflowEngine.templateFor(project)
            val last = template.stages.lastIndex
            val working = project.copy(status = TaskStatus.WORKING, workflowStageIndex = last)
            tasks.mutate { it + working }
            val epoch = CreatorDataGate.generation(context)
            val at = System.currentTimeMillis()
            val stage = template.stages[last]
            recovery.begin(epoch, project.id, CreatorRewardEngine.stageCompleted(project.id, stage.id, stage.label, at),
                last, at, last, TaskStatus.DONE.name)
            assertTrue(recovery.recoverAll().isEmpty())
            val ledger = CreatorRewardStore(context).load()
            assertFalse(ledger.any { it.eventKey == "stage:${project.id}:${stage.id}" })
            assertFalse(ledger.any { it.eventKey == "published:${project.id}" })
            assertTrue(CreatorPostPublishStore(context).load().none { it.projectId == project.id })
        }
    }

    @Test fun interruptedFirstStageAdvanceRecoversOnlyItsExplicitEvent(): Unit = runBlocking {
        fixture { context, tasks, recovery ->
            val project = task()
            tasks.mutate { it + project }
            val epoch = CreatorDataGate.generation(context)
            val stage = CreatorWorkflowEngine.templateFor(project).stages.first()
            val at = System.currentTimeMillis()
            val evidence = CreatorRewardEngine.stageCompleted(project.id, stage.id, stage.label, at)
            recovery.begin(epoch, project.id, evidence, 1, 0L, 0, TaskStatus.WORKING.name)
            tasks.updateTask(project.id, epoch) { it.copy(status = TaskStatus.WORKING, workflowStageIndex = 1) }
            val first = recovery.recoverAll()
            assertEquals(1, first.count { it.eventKey == evidence.eventKey })
            assertTrue(recovery.recoverAll().isEmpty())
            assertEquals(1, CreatorRewardStore(context).load().count { it.eventKey == evidence.eventKey })
            assertFalse(CreatorRewardStore(context).load().any { it.eventKey == "published:${project.id}" })
        }
    }

    @Test fun failedRecoveryKeepsMarkerAndCanBeRetried(): Unit = runBlocking {
        fixture { context, tasks, recovery ->
            val project = task()
            tasks.mutate { it + project }
            val epoch = CreatorDataGate.generation(context)
            recovery.begin(epoch, project.id)
            tasks.updateTask(project.id, epoch) { it.copy(publishedAtMillis = System.currentTimeMillis() - 60_000L) }
            val prefs = context.getSharedPreferences("creator_publication_recovery_v183", 0)
            val marker = org.json.JSONObject(prefs.getString("pending", null)!!)
            assertTrue(prefs.edit().putString("pending", marker.put("version", 2).toString()).commit())
            assertTrue(runCatching { recovery.recoverAll() }.isFailure)
            assertTrue(prefs.contains("pending"))
            assertTrue(prefs.edit().putString("pending", marker.put("version", 1).toString()).commit())
            assertEquals(1, recovery.recoverAll().count { it.eventKey == "published:${project.id}" })
            assertFalse(prefs.contains("pending"))
            assertTrue(recovery.recoverAll().isEmpty())
        }
    }

    @Test fun interruptedCheckpointSaveIsRecoveredWithoutFarmingRewards(): Unit = runBlocking {
        fixture { context, tasks, recovery ->
            val project = task().copy(publishedAtMillis = System.currentTimeMillis() - 172_800_000L)
            tasks.mutate { it + project }
            recovery.recoverAll()
            val checkpoints = CreatorPostPublishStore(context)
            val id = checkpoints.load().first { it.projectId == project.id }.id
            val epoch = CreatorDataGate.generation(context)
            val at = System.currentTimeMillis() - 1_000L
            recovery.begin(epoch, project.id)
            checkpoints.updateStatus(id, PostPublishCheckpointStatus.DONE, at)
            val first = recovery.recoverAll()
            assertEquals(1, first.count { it.eventKey == "post-publish:$id" })
            assertEquals(at, checkpoints.load().first { it.id == id }.completedAtMillis)
            val (again, added) = recovery.updateCheckpoint(id, PostPublishCheckpointStatus.DONE, epoch, at + 1000L)
            assertEquals(at, again!!.completedAtMillis)
            assertTrue(added.isEmpty())
            assertEquals(1, CreatorRewardStore(context).load().count { it.eventKey == "post-publish:$id" })
            assertTrue(recovery.recoverAll().isEmpty())
        }
    }

    @Test fun correctionRemovalAndRerecordPreserveCompletedReviewHistory(): Unit = runBlocking {
        fixture { context, tasks, recovery ->
            val project = task().copy(publishedAtMillis = System.currentTimeMillis() - 259_200_000L)
            tasks.mutate { it + project }
            recovery.recoverAll()
            val checkpoints = CreatorPostPublishStore(context)
            val done = checkpoints.load().first { it.projectId == project.id }
            val epoch = CreatorDataGate.generation(context)
            recovery.updateCheckpoint(done.id, PostPublishCheckpointStatus.DONE, epoch)
            val originalReview = checkpoints.load().first { it.id == done.id }
            val correctedAt = System.currentTimeMillis() - 86_400_000L
            recovery.begin(epoch, project.id)
            tasks.updateTask(project.id, epoch) { it.copy(publishedAtMillis = correctedAt) }
            recovery.finish(epoch)
            assertEquals(originalReview, checkpoints.load().first { it.id == done.id })
            val pending = checkpoints.load().first { it.projectId == project.id && it.id != done.id }
            assertEquals(correctedAt + 7L * 24L * 60L * 60_000L, pending.dueAtMillis)
            recovery.begin(epoch, project.id)
            tasks.updateTask(project.id, epoch) { it.copy(publishedAtMillis = 0L, publishedUrl = "") }
            recovery.finish(epoch)
            assertFalse(CreatorRewardStore(context).load().any { it.eventKey == "published:${project.id}" })
            assertEquals(originalReview, checkpoints.load().first { it.id == done.id })
            recovery.begin(epoch, project.id)
            tasks.updateTask(project.id, epoch) { it.copy(publishedAtMillis = correctedAt) }
            val added = recovery.finish(epoch)
            assertEquals(1, added.count { it.eventKey == "published:${project.id}" })
            assertEquals(1, CreatorRewardStore(context).load().count { it.eventKey == "published:${project.id}" })
            assertEquals(originalReview, checkpoints.load().first { it.id == done.id })
        }
    }

    @Test fun restoreInvalidatesOldIntentAndRejectsQueuedCheckpointMutation(): Unit = runBlocking {
        fixture { context, tasks, recovery ->
            val project = task().copy(publishedAtMillis = System.currentTimeMillis() - 100_000L)
            tasks.mutate { it + project }
            recovery.recoverAll()
            val checkpointId = CreatorPostPublishStore(context).load().first { it.projectId == project.id }.id
            val backup = CreatorBackupManager(context)
            val snapshot = backup.createBackup()
            val oldEpoch = CreatorDataGate.generation(context)
            recovery.begin(oldEpoch, project.id)
            backup.restore(snapshot)
            val stale = runCatching {
                recovery.updateCheckpoint(checkpointId, PostPublishCheckpointStatus.DONE, oldEpoch)
            }.exceptionOrNull()
            assertTrue(stale is CreatorWriteConflict)
            assertTrue(recovery.recoverAll().isEmpty())
            assertTrue(CreatorPostPublishStore(context).load().first { it.id == checkpointId }.status == PostPublishCheckpointStatus.PENDING)
            assertEquals(1, CreatorRewardStore(context).load().count { it.eventKey == "published:${project.id}" })
        }
    }
}
