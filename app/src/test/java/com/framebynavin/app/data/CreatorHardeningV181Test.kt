package com.framebynavin.app.data

import org.junit.Assert.*
import org.junit.Test

class CreatorHardeningV181Test {
    private fun task(id: String = "p1", type: String = "Long-form") = CreatorTask(
        id = id, title = "A real project", platform = "YouTube", contentType = type, dueLabel = "Today",
    )

    @Test fun publishingPrecedesPromotionAndPreservesActualTime() {
        val now = System.currentTimeMillis() - 3_600_000L
        val original = task().copy(status = TaskStatus.WORKING, workflowStageIndex = 7)
        val published = CreatorPublicationEngine.advance(original, now)
        assertEquals(TaskStatus.WORKING, published.status)
        assertEquals(8, published.workflowStageIndex)
        assertEquals(now, published.publishedAtMillis)
        assertEquals(0L, published.completedAtMillis)
        assertEquals(40, CreatorPublicationEngine.publicationReward(original, published)?.xp)
        val finished = CreatorPublicationEngine.advance(published, now + 60_000L)
        assertEquals(TaskStatus.DONE, finished.status)
        assertEquals(now, finished.publishedAtMillis)
        assertEquals(now + 60_000L, finished.completedAtMillis)
        assertNull(CreatorPublicationEngine.publicationReward(published, finished))
        assertEquals(now + 24L * 60L * 60_000L, CreatorPostPublishEngine.build(finished).first().dueAtMillis)
    }

    @Test fun newsletterSendIsNotFinalReview() {
        val original = task(type = "Newsletter").copy(platform = "Blog / Newsletter", status = TaskStatus.WORKING, workflowStageIndex = 4)
        val now = System.currentTimeMillis() - 60_000L
        val sent = CreatorPublicationEngine.advance(original, now)
        assertEquals(now, sent.publishedAtMillis)
        assertEquals(5, sent.workflowStageIndex)
        assertEquals(TaskStatus.WORKING, sent.status)
        assertEquals("FINISH PROJECT", CreatorWorkflowEngine.stageActionLabel(sent))
    }

    @Test fun completionDoesNotInventHistoricalPublication() {
        val completed = CreatorPublicationEngine.finish(task(), System.currentTimeMillis())
        assertEquals(TaskStatus.DONE, completed.status)
        assertEquals(0L, completed.publishedAtMillis)
        assertTrue(CreatorPostPublishEngine.build(completed).isEmpty())
        assertNull(CreatorPublicationEngine.publicationReward(task(), completed))
    }

    @Test fun publicationCorrectionAndRemovalAreExplicit() {
        val original = task().copy(status = TaskStatus.DONE, completedAtMillis = 100L)
        val at = System.currentTimeMillis() - 60_000L
        val corrected = CreatorPublicationEngine.correct(original, at, "https://example.com/video")
        assertEquals(at, corrected.publishedAtMillis)
        assertEquals(100L, corrected.completedAtMillis)
        assertEquals("https://example.com/video", corrected.publishedUrl)
        val cleared = CreatorPublicationEngine.correct(corrected, 0L)
        assertEquals(0L, cleared.publishedAtMillis)
        assertTrue(cleared.publishedUrl.isEmpty())
        assertTrue(CreatorPostPublishEngine.build(cleared).isEmpty())
    }

    @Test fun invalidPublicationInputIsRejected() {
        assertThrows(IllegalArgumentException::class.java) { CreatorPublicationEngine.correct(task(), -1L) }
        assertThrows(IllegalArgumentException::class.java) { CreatorPublicationEngine.correct(task(), System.currentTimeMillis() + 86_400_000L) }
        assertThrows(IllegalArgumentException::class.java) { CreatorPublicationEngine.correct(task(), 1L, "http://example.com") }
    }

    @Test fun unrelatedConcurrentEditsArePreserved() {
        val a = task("a")
        val b = task("b")
        val updated = CreatorDeltaEngine.merge(listOf(a,b), listOf(a.copy(title="new"),b), listOf(a,b.copy(notes="other"))) { it.id }
        assertEquals("new", updated.first { it.id == "a" }.title)
        assertEquals("other", updated.first { it.id == "b" }.notes)
    }

    @Test fun staleEditAndStaleDeleteAreRejected() {
        val a = task()
        val newer = a.copy(notes="newer")
        assertThrows(CreatorWriteConflict::class.java) {
            CreatorDeltaEngine.merge(listOf(a), listOf(a.copy(notes="old")), listOf(newer)) { it.id }
        }
        assertThrows(CreatorWriteConflict::class.java) {
            CreatorDeltaEngine.merge(listOf(a), emptyList(), listOf(newer)) { it.id }
        }
    }

    @Test fun unrelatedDeletionDoesNotEraseNewRecords() {
        val a=task("a"); val b=task("b"); val c=task("c")
        val result=CreatorDeltaEngine.merge(listOf(a,b), listOf(b), listOf(a,b,c)) { it.id }
        assertEquals(setOf("b","c"), result.map { it.id }.toSet())
        assertThrows(IllegalArgumentException::class.java) {
            CreatorDeltaEngine.merge(listOf(a,a), listOf(a), listOf(a)) { it.id }
        }
    }
}
