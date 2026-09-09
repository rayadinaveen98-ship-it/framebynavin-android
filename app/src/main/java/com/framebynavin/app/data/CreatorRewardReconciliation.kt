package com.framebynavin.app.data

/** Only explicit creator-owned publication and completed-review evidence is authoritative. */
data class CreatorRewardReconciliationResult(
    val entries: List<CreatorRewardLedgerEntry>,
    val newlyCredited: List<CreatorRewardLedgerEntry>,
)

object CreatorRewardReconciliation {
    fun reconcile(
        current: List<CreatorRewardLedgerEntry>,
        tasks: List<CreatorTask>,
        checkpoints: List<PostPublishCheckpoint>,
        stageEvidence: CreatorRewardLedgerEntry? = null,
    ): CreatorRewardReconciliationResult {
        val byKey = linkedMapOf<String, CreatorRewardLedgerEntry>()
        current.forEach { byKey[it.eventKey] = it }
        val added = mutableListOf<CreatorRewardLedgerEntry>()
        val parents = tasks.filterNot(CreatorPostPublishEngine::isLegacyTask).associateBy { it.id }
        parents.values.forEach { task ->
            val key = "published:${task.id}"
            if (task.publishedAtMillis <= 0L) {
                byKey.remove(key) // Explicit removal revokes only this publication credit.
            } else {
                val expected = CreatorRewardEngine.projectPublished(task.id, task.title, task.publishedAtMillis)
                if (key !in byKey) added += expected
                // Corrected dates/labels replace the existing event, never add another key.
                byKey[key] = expected
            }
        }
        checkpoints.forEach { checkpoint ->
            val parent = parents[checkpoint.projectId] ?: return@forEach
            if (parent.publishedAtMillis <= 0L ||
                checkpoint.status != PostPublishCheckpointStatus.DONE || checkpoint.completedAtMillis <= 0L) return@forEach
            val expected = CreatorRewardEngine.postPublishCompleted(checkpoint, checkpoint.completedAtMillis)
            if (expected.eventKey !in byKey) {
                byKey[expected.eventKey] = expected
                added += expected
            }
            // Existing credits survive reopening/correction. A review cannot be farmed.
        }
        stageEvidence?.let { evidence ->
            require(evidence.type == CreatorRewardEventType.PROJECT_STAGE_COMPLETED)
            require(evidence.eventKey == "stage:${evidence.projectId}:${evidence.subjectId}")
            require(evidence.occurredAtMillis > 0L)
            if (evidence.eventKey !in byKey) {
                byKey[evidence.eventKey] = evidence
                added += evidence
            }
        }
        // Unknown historical rewards are not deleted or reconstructed.
        return CreatorRewardReconciliationResult(byKey.values.sortedByDescending { it.occurredAtMillis }, added)
    }
}
