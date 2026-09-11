package com.framebynavin.app.data

import org.junit.Assert.*
import org.junit.Test

class CreatorAiEvidencePackTest {
    private fun postmortem() = CreatorVideoPostmortem(
        taskId = "project-1",
        projectTitle = "Private project title",
        videoId = "abc123",
        videoTitle = "Published title",
        publishedAtMillis = 1_000L,
        creation = CreatorPostmortemCreation(
            creatorModeId = "film-analyst",
            archetypeId = "analysis",
            productionStyles = listOf("cinematic"),
            platform = "YouTube",
            deliveryFormat = "Long-form",
            audience = "Telugu film audience",
            viewerProblem = "Wants deeper craft context",
            promise = "Explain why the scene works",
            angle = "Cinematography + emotion",
            hook = "Why does this scene stay with us?",
            scriptPresent = true,
            scriptCharacterCount = 4200,
            referenceCount = 3,
        ),
        packaging = CreatorPostmortemPackaging(
            titleSnapshot = "Published title",
            thumbnailSnapshot = "One face, minimal copy",
            descriptionPresent = true,
            tagsPresent = true,
            publicationRevisionCount = 2,
            matchedPublishedDeliverable = true,
        ),
        stageSpans = listOf(
            CreatorPostmortemStageSpan("edit", "Editing", 100L, 500L, 400L, false),
        ),
        projectToPublishMillis = 900L,
        timingQuality = CreatorPostmortemTimingQuality.EXACT_FROM_PROJECT_CREATION,
        performance = CreatorPostmortemPerformance(
            periodViews = 1000,
            lifetimeViews = 1200,
            watchMinutes = 4800,
            averageViewDurationSeconds = 220,
            netSubscribers = 12,
            likes = 100,
            comments = 20,
            impressions = 15000,
            ctrPercent = 6.5,
            retentionPointCount = 3,
            retentionAt10Percent = .82,
            retentionAt50Percent = .55,
            retentionAt90Percent = .31,
        ),
        checkpoints = emptyList(),
        creatorLearning = "Shorter setup worked better.",
        evidence = CreatorPostmortemEvidence(true, true, true, true, false, true, true, true),
    )

    @Test fun disabledConsentEmitsNothing() {
        val pack = CreatorAiEvidencePackBuilder.build(postmortem(), CreatorAiConsent())
        assertTrue(pack.evidence.isEmpty())
        assertNull(pack.publicVideoUrl)
    }

    @Test fun publicVideoOnlyDoesNotLeakProjectContext() {
        val pack = CreatorAiEvidencePackBuilder.build(
            postmortem(),
            CreatorAiConsent(enabled = true, sharePublicVideo = true),
        )
        assertEquals("https://www.youtube.com/watch?v=abc123", pack.publicVideoUrl)
        assertEquals(0, pack.privateEvidenceCount)
        assertFalse(pack.evidence.any { it.value.contains("Private project title") })
        assertFalse(pack.evidence.any { it.value.contains("Telugu film audience") })
    }

    @Test fun projectContextSharesScriptSizeButNeverFullScript() {
        val pack = CreatorAiEvidencePackBuilder.build(
            postmortem(),
            CreatorAiConsent(enabled = true, shareProjectContext = true),
        )
        val script = pack.evidence.first { it.id == "project.script_size" }
        assertTrue(script.value.contains("4200 characters"))
        assertTrue(script.value.contains("full script withheld"))
        assertTrue(pack.privateEvidenceCount > 0)
    }

    @Test fun promptRequiresEvidenceCitationsAndNoInventedMetrics() {
        val pack = CreatorAiEvidencePackBuilder.build(
            postmortem(),
            CreatorAiConsent(enabled = true, sharePublicVideo = true, sharePerformance = true),
        )
        val prompt = CreatorAiEvidencePackBuilder.videoAutopsyPrompt(pack)
        assertTrue(prompt.contains("Never invent missing metrics"))
        assertTrue(prompt.contains("cite the relevant evidence id"))
        assertTrue(prompt.contains("[performance.ctr]"))
        assertTrue(prompt.contains("https://www.youtube.com/watch?v=abc123"))
    }
}
