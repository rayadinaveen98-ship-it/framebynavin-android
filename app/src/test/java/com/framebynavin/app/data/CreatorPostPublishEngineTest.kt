package com.framebynavin.app.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class CreatorPostPublishEngineTest {
    @Test
    fun youtubeCreatesProjectOwnedPostPublishCheckpoints() {
        val task = CreatorTask(
            id = "video-1",
            title = "OG Analysis",
            platform = "YouTube",
            contentType = "Long-form",
            dueLabel = "Today",
            status = TaskStatus.DONE,
            completedAtMillis = 2_000_000L,
            publishedAtMillis = 1_000_000L,
        )
        val checkpoints = CreatorPostPublishEngine.build(task)
        assertEquals(listOf("24h-review", "7d-review"), checkpoints.map { it.kind.key })
        assertEquals("video-1", checkpoints.first().projectId)
        assertEquals(1_000_000L + 24L * 60L * 60_000L, checkpoints.first().dueAtMillis)
        assertTrue(checkpoints.all { it.status == PostPublishCheckpointStatus.PENDING })
    }

    @Test
    fun legacyFakeTaskMigratesIntoCheckpointWithoutWorkflowMeaning() {
        val followUp = CreatorTask(
            id = "legacy-followup",
            title = "24h performance check · OG Analysis",
            platform = "YouTube",
            contentType = "Update",
            dueLabel = "Tomorrow",
            dueAtMillis = 2_000_000L,
            status = TaskStatus.DONE,
            sourceRefId = "post-publish:video-1:24h-review",
            completedAtMillis = 2_100_000L,
        )
        val migrated = CreatorPostPublishEngine.fromLegacyTask(followUp)!!
        assertEquals("post-publish:video-1:24h-review", migrated.id)
        assertEquals("video-1", migrated.projectId)
        assertEquals(PostPublishCheckpointKind.PERFORMANCE_24H, migrated.kind)
        assertEquals(PostPublishCheckpointStatus.DONE, migrated.status)
        assertEquals(2_100_000L, migrated.completedAtMillis)
    }


    @Test
    fun legacyCrossPromoteProjectIsDroppedBecausePromotionAlreadyLivesInWorkflow() {
        val legacy = CreatorTask(
            id = "legacy-cross",
            title = "Cross-promote · OG Analysis",
            platform = "X",
            contentType = "Post",
            dueLabel = "Today",
            sourceRefId = "post-publish:video-1:cross-promote",
        )
        assertTrue(CreatorPostPublishEngine.fromLegacyTask(legacy) == null)
    }

    @Test
    fun legacySourceParsingUsesFinalSeparatorSoProjectIdsRemainSafe() {
        val parsed = CreatorPostPublishEngine.parseLegacySource("post-publish:project:with:colon:7d-review")!!
        assertEquals("project:with:colon", parsed.first)
        assertEquals(PostPublishCheckpointKind.PERFORMANCE_7D, parsed.second)
    }
}
