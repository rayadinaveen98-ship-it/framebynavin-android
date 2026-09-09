package com.framebynavin.app.data

object CreatorPostPublishReconciliation {
    fun reconcile(current: List<PostPublishCheckpoint>, parents: List<CreatorTask>): List<PostPublishCheckpoint> {
        val known = parents.associateBy { it.id }
        val expected = parents.flatMap(CreatorPostPublishEngine::build).associateBy { it.id }
        val updated = current.filterNot { checkpoint ->
            checkpoint.projectId in known && checkpoint.status == PostPublishCheckpointStatus.PENDING && checkpoint.id !in expected
        }.map { checkpoint ->
            if (checkpoint.projectId !in known || checkpoint.status != PostPublishCheckpointStatus.PENDING) checkpoint
            else expected[checkpoint.id]?.let { checkpoint.copy(dueAtMillis = it.dueAtMillis) } ?: checkpoint
        }.toMutableList()
        val existingIds = updated.mapTo(mutableSetOf()) { it.id }
        expected.values.forEach { if (existingIds.add(it.id)) updated += it }
        return updated
    }
}
