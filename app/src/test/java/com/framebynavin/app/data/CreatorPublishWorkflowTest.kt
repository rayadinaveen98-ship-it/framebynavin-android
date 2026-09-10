package com.framebynavin.app.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class CreatorPublishWorkflowTest {
    @Test
    fun legacyDeliverableGetsGateAndCannotPublishUntilResolved() {
        val legacy = CreatorDeliverable(
            id = "yt",
            platform = "YouTube",
            format = "Long video",
            title = "A finished title",
        )
        val normalized = CreatorPublishWorkflow.ensureGate(legacy)

        assertTrue(normalized.publishGate.isNotEmpty())
        assertFalse(CreatorPublishWorkflow.isReadyToPublish(normalized))
        assertTrue(CreatorPublishWorkflow.unresolvedRequired(normalized).isNotEmpty())
    }

    @Test
    fun resolvedGatePublishesOnlyThatDeliverableAndCreatesSnapshot() {
        val deliverable = CreatorPublishWorkflow.ensureGate(
            CreatorDeliverable(
                id = "yt",
                platform = "YouTube",
                format = "Long video",
                title = "Why this scene works",
                description = "Description",
                tags = "cinema, analysis",
                thumbnailConcept = "Face + frame",
            )
        ).copy(
            publishGate = CreatorPublishWorkflow.ensureGate(
                CreatorDeliverable(platform = "YouTube", format = "Long video")
            ).publishGate.map { it.copy(status = CreatorPublishGateStatus.DONE) }
        )

        val published = CreatorPublishWorkflow.publish(deliverable, 1234L, "https://example.com/video")

        assertEquals(CreatorDeliverableStatus.PUBLISHED, published.status)
        assertEquals(1234L, published.publishedAtMillis)
        assertEquals("https://example.com/video", published.publishedUrl)
        assertEquals(1, published.publicationHistory.size)
        assertEquals(CreatorPublicationEventKind.PUBLISHED, published.publicationHistory.single().kind)
        assertEquals("Why this scene works", published.publicationHistory.single().titleSnapshot)
    }

    @Test
    fun publicationUpdateAndReopenAppendHistoryWithoutDeletingPastRecord() {
        val base = CreatorDeliverable(
            id = "ig",
            platform = "Instagram",
            format = "Reel",
            title = "Final reel",
            status = CreatorDeliverableStatus.PUBLISHED,
            publishedAtMillis = 100L,
            publishedUrl = "https://example.com/first",
            publicationHistory = listOf(
                CreatorPublicationEvent(
                    id = "event-1",
                    kind = CreatorPublicationEventKind.PUBLISHED,
                    atMillis = 100L,
                    titleSnapshot = "Final reel",
                    url = "https://example.com/first",
                )
            ),
        )

        val updated = CreatorPublishWorkflow.recordPublishedUpdate(
            deliverable = base.copy(title = "Corrected reel"),
            atMillis = 200L,
            url = "https://example.com/corrected",
            note = "Fixed title",
        )
        val reopened = CreatorPublishWorkflow.reopen(updated, 300L, "Needs another export")

        assertEquals(3, reopened.publicationHistory.size)
        assertEquals(CreatorPublicationEventKind.PUBLISHED, reopened.publicationHistory[0].kind)
        assertEquals(CreatorPublicationEventKind.UPDATED, reopened.publicationHistory[1].kind)
        assertEquals(CreatorPublicationEventKind.REOPENED, reopened.publicationHistory[2].kind)
        assertEquals(CreatorDeliverableStatus.READY, reopened.status)
        assertEquals(0L, reopened.publishedAtMillis)
        assertTrue(reopened.publishedUrl.isBlank())
    }

    @Test
    fun derivativeKeepsParentRelationshipButStartsUnpublished() {
        val parent = CreatorDeliverable(
            id = "main",
            platform = "YouTube",
            format = "Long video",
            title = "Main analysis",
            status = CreatorDeliverableStatus.PUBLISHED,
            publishedAtMillis = 100L,
            tags = "analysis",
        )

        val derivative = CreatorPublishWorkflow.createDerivative(parent, "Instagram", "Reel")

        assertEquals("main", derivative.parentDeliverableId)
        assertEquals(CreatorDeliverableStatus.PLANNED, derivative.status)
        assertEquals(0L, derivative.publishedAtMillis)
        assertEquals("analysis", derivative.tags)
        assertTrue(derivative.publishGate.isNotEmpty())
        assertEquals(CreatorDeliverableStatus.PUBLISHED, parent.status)
    }

    @Test
    fun variantsRemainAlternativesUntilExplicitlyChosenAsFinalMetadata() {
        val deliverable = CreatorDeliverable(
            platform = "YouTube",
            format = "Long video",
            title = "Current final",
            thumbnailConcept = "Current cover",
            titleVariants = listOf(
                CreatorVariantIdea(id = "a", text = "Alternative A"),
                CreatorVariantIdea(id = "b", text = "Alternative B"),
            ),
            thumbnailVariants = listOf(CreatorVariantIdea(id = "c", text = "Alternative cover")),
        )

        assertEquals("Current final", deliverable.title)
        assertEquals(2, deliverable.titleVariants.size)
        assertEquals("Current cover", deliverable.thumbnailConcept)
        assertEquals(1, deliverable.thumbnailVariants.size)
    }
}
