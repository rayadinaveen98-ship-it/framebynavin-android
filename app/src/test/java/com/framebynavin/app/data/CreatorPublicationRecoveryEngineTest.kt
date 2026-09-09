package com.framebynavin.app.data

import org.junit.Assert.*
import org.junit.Test

class CreatorPublicationRecoveryEngineTest {
    private val at = 1_700_000_000_000L
    private fun task(id: String = "p") = CreatorTask(id = id, title = "Film", platform = "YouTube",
        contentType = "Long-form", dueLabel = "Today")
    private fun checkpoint(id: String = "p", kind: PostPublishCheckpointKind = PostPublishCheckpointKind.PERFORMANCE_24H) =
        CreatorPostPublishEngine.build(task(id).copy(publishedAtMillis = at)).first { it.kind == kind }

    @Test fun publicationIsExplicitAndCorrectionsNeverDuplicateKeys() {
        val original = task()
        assertTrue(CreatorRewardReconciliation.reconcile(emptyList(), listOf(original.copy(status = TaskStatus.DONE)), emptyList()).entries.isEmpty())
        val published = original.copy(publishedAtMillis = at)
        val first = CreatorRewardReconciliation.reconcile(emptyList(), listOf(published), emptyList())
        val corrected = CreatorRewardReconciliation.reconcile(first.entries, listOf(published.copy(publishedAtMillis = at + 1000L)), emptyList())
        assertTrue(corrected.newlyCredited.isEmpty())
        assertEquals(1, corrected.entries.size)
        assertEquals(at + 1000L, corrected.entries.single().occurredAtMillis)
        val removed = CreatorRewardReconciliation.reconcile(corrected.entries, listOf(original), emptyList())
        assertTrue(removed.entries.isEmpty())
        val recordedAgain = CreatorRewardReconciliation.reconcile(removed.entries, listOf(published), emptyList())
        assertEquals(1, recordedAgain.entries.size)
        assertEquals(1, recordedAgain.newlyCredited.size)
    }

    @Test fun repeatedRecoveryPreservesExistingCheckpointCreditsAndUnknownHistory() {
        val parent = task().copy(publishedAtMillis = at)
        val done = checkpoint().copy(status = PostPublishCheckpointStatus.DONE, completedAtMillis = at + 90_000L)
        val historical = CreatorRewardEngine.ideaConverted("old-idea", "old-project", at)
        val first = CreatorRewardReconciliation.reconcile(listOf(historical), listOf(parent), listOf(done))
        assertEquals(2, first.newlyCredited.size)
        val second = CreatorRewardReconciliation.reconcile(first.entries, listOf(parent), listOf(done))
        assertEquals(first.entries, second.entries)
        assertTrue(second.newlyCredited.isEmpty())
        val reopened = CreatorRewardReconciliation.reconcile(second.entries, listOf(parent), listOf(done.copy(status = PostPublishCheckpointStatus.PENDING, completedAtMillis = 0L)))
        assertTrue(reopened.newlyCredited.isEmpty())
        assertEquals(3, reopened.entries.size)
        val completedAgain = CreatorRewardReconciliation.reconcile(reopened.entries, listOf(parent), listOf(done.copy(completedAtMillis = at + 180_000L)))
        assertTrue(completedAgain.newlyCredited.isEmpty())
        assertEquals(at + 90_000L, completedAgain.entries.first { it.type == CreatorRewardEventType.POST_PUBLISH_COMPLETED }.occurredAtMillis)
    }

    @Test fun pendingDatesRebaseButCompletedHistoryAndUnrelatedProjectsSurvive() {
        val parent = task().copy(publishedAtMillis = at)
        val existing = CreatorPostPublishEngine.build(parent)
        val done = existing.first().copy(status = PostPublishCheckpointStatus.DONE, completedAtMillis = at + 100L)
        val unrelated = checkpoint("other")
        val corrected = parent.copy(publishedAtMillis = at + 86_400_000L)
        val result = CreatorPostPublishReconciliation.reconcile(listOf(done, existing.last(), unrelated), listOf(corrected))
        assertEquals(done, result.first { it.id == done.id })
        assertEquals(existing.last().dueAtMillis + 86_400_000L, result.first { it.id == existing.last().id }.dueAtMillis)
        assertEquals(unrelated, result.first { it.id == unrelated.id })
        val removed = CreatorPostPublishReconciliation.reconcile(result, listOf(task()))
        assertTrue(removed.any { it.id == done.id && it.status == PostPublishCheckpointStatus.DONE })
        assertFalse(removed.any { it.id == existing.last().id })
        assertEquals(unrelated, removed.first { it.id == unrelated.id })
    }

    @Test fun stageEvidenceRequiresExplicitEventAndNeverReconstructsHistoricalStages() {
        val parent = task().copy(status = TaskStatus.DONE, workflowStageIndex = 4)
        val noEvidence = CreatorRewardReconciliation.reconcile(emptyList(), listOf(parent), emptyList())
        assertTrue(noEvidence.entries.isEmpty())
        val stage = CreatorRewardEngine.stageCompleted("p", "script", "Script", at)
        val withEvidence = CreatorRewardReconciliation.reconcile(emptyList(), listOf(parent), emptyList(), stage)
        assertEquals(1, withEvidence.entries.size)
        assertEquals(10, withEvidence.entries.single().xp)
        assertTrue(CreatorRewardReconciliation.reconcile(withEvidence.entries, listOf(parent), emptyList(), stage).newlyCredited.isEmpty())
    }
}
