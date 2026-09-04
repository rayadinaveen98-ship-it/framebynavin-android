package com.framebynavin.app.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class CreatorPostPublishEngineTest {
    @Test
    fun youtubeCreatesCrossPromoteAndPerformanceChecks() {
        val task = CreatorTask(
            id = "video-1",
            title = "OG Analysis",
            platform = "YouTube",
            contentType = "Long-form",
            dueLabel = "Today",
        )
        val specs = CreatorPostPublishEngine.specs(task)
        assertEquals(listOf("cross-promote", "24h-review", "7d-review"), specs.map { it.key })
        assertEquals(30L, specs.first().dueOffsetMinutes)
        assertEquals(24L * 60L, specs[1].dueOffsetMinutes)
        assertEquals(7L * 24L * 60L, specs[2].dueOffsetMinutes)
    }

    @Test
    fun generatedFollowUpDoesNotGenerateAnotherFollowUpSet() {
        val followUp = CreatorTask(
            id = "followup",
            title = "24h performance check",
            platform = "YouTube",
            contentType = "Update",
            dueLabel = "Tomorrow",
            sourceRefId = "post-publish:video-1:24h-review",
        )
        assertTrue(CreatorPostPublishEngine.specs(followUp).isEmpty())
    }

    @Test
    fun sourceReferenceIsStableForDedupe() {
        assertEquals(
            "post-publish:abc:7d-review",
            CreatorPostPublishEngine.sourceRef("abc", "7d-review"),
        )
    }
}
