package com.framebynavin.app.data

import com.framebynavin.app.youtube.YouTubeCheckpointQuality
import com.framebynavin.app.youtube.YouTubePublishCheckpoint
import com.framebynavin.app.youtube.YouTubeReachVideoSummary
import com.framebynavin.app.youtube.YouTubeRetentionPoint
import com.framebynavin.app.youtube.YouTubeVideoRetentionSnapshot
import com.framebynavin.app.youtube.YouTubeVideoSnapshot
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class CreatorVideoPostmortemTest {
    private val hour = 60L * 60L * 1000L
    private val video = YouTubeVideoSnapshot(
        videoId = "video-1",
        title = "Published title",
        publishedAtMillis = 20 * hour,
        periodViews = 1_500,
        lifetimeViews = 2_400,
        watchMinutes = 900,
        averageViewDurationSeconds = 180,
        subscribersGained = 12,
        subscribersLost = 2,
        likes = 110,
        comments = 19,
    )

    @Test
    fun `project created entry gives exact project to publish timing`() {
        val task = task(publishedAtMillis = 20 * hour)
        val timeline = listOf(
            event("e1", CreatorWorkflowTimelineType.ENTERED, CreatorWorkflowTimelineSource.PROJECT_CREATED, 2 * hour, "idea", "Idea"),
            event("x1", CreatorWorkflowTimelineType.EXITED, CreatorWorkflowTimelineSource.STAGE_TRANSITION, 5 * hour, "idea", "Idea"),
            event("e2", CreatorWorkflowTimelineType.ENTERED, CreatorWorkflowTimelineSource.STAGE_TRANSITION, 5 * hour, "edit", "Edit"),
            event("x2", CreatorWorkflowTimelineType.EXITED, CreatorWorkflowTimelineSource.PROJECT_FINISHED, 19 * hour, "edit", "Edit"),
        )

        val result = CreatorVideoPostmortemEngine.build(task, video, timeline)

        assertEquals(CreatorPostmortemTimingQuality.EXACT_FROM_PROJECT_CREATION, result.timingQuality)
        assertEquals(18 * hour, result.projectToPublishMillis)
        assertEquals(2, result.stageSpans.size)
        assertFalse(result.stageSpans.first().lowerBound)
        assertEquals(14 * hour, result.stageSpans.last().residenceMillis)
    }

    @Test
    fun `legacy observed entry is labeled lower bound rather than exact`() {
        val task = task(publishedAtMillis = 20 * hour)
        val timeline = listOf(
            event("e1", CreatorWorkflowTimelineType.ENTERED, CreatorWorkflowTimelineSource.OBSERVED_CURRENT, 10 * hour, "edit", "Edit"),
            event("x1", CreatorWorkflowTimelineType.EXITED, CreatorWorkflowTimelineSource.PROJECT_FINISHED, 19 * hour, "edit", "Edit"),
        )

        val result = CreatorVideoPostmortemEngine.build(task, video, timeline)

        assertEquals(CreatorPostmortemTimingQuality.LOWER_BOUND_FROM_FIRST_OBSERVATION, result.timingQuality)
        assertEquals(10 * hour, result.projectToPublishMillis)
        assertTrue(result.stageSpans.single().lowerBound)
    }

    @Test
    fun `matching publication history is preferred for packaging evidence`() {
        val publication = CreatorPublicationEvent(
            kind = CreatorPublicationEventKind.PUBLISHED,
            atMillis = 20 * hour,
            titleSnapshot = "Final locked title",
            thumbnailSnapshot = "Red face + minimal copy",
            descriptionSnapshot = "Published description",
            tagsSnapshot = "cinema,analysis",
            url = "https://youtube.com/watch?v=video-1",
        )
        val deliverable = CreatorDeliverable(
            platform = "YouTube",
            format = "Long-form",
            title = "Draft title",
            publishedAtMillis = 20 * hour,
            publishedUrl = "https://youtube.com/watch?v=video-1",
            publicationHistory = listOf(publication),
        )
        val task = task(workspace = CreatorContentWorkspace(deliverables = listOf(deliverable)))

        val result = CreatorVideoPostmortemEngine.build(task, video)

        assertEquals("Final locked title", result.packaging.titleSnapshot)
        assertEquals("Red face + minimal copy", result.packaging.thumbnailSnapshot)
        assertTrue(result.packaging.descriptionPresent)
        assertTrue(result.packaging.tagsPresent)
        assertTrue(result.packaging.matchedPublishedDeliverable)
        assertTrue(result.evidence.packagingHistory)
    }

    @Test
    fun `missing platform datasets remain absent rather than fabricated`() {
        val result = CreatorVideoPostmortemEngine.build(task(), video)

        assertNull(result.performance.impressions)
        assertNull(result.performance.ctrPercent)
        assertNull(result.performance.retentionAt50Percent)
        assertEquals(0, result.performance.retentionPointCount)
        assertTrue(result.checkpoints.isEmpty())
        assertFalse(result.evidence.reach)
        assertFalse(result.evidence.retention)
        assertFalse(result.evidence.releaseCurve)
    }

    @Test
    fun `reach retention and release curve join the linked video`() {
        val retention = YouTubeVideoRetentionSnapshot(
            videoId = "video-1",
            title = video.title,
            points = listOf(
                YouTubeRetentionPoint(.10, .88, null),
                YouTubeRetentionPoint(.48, .61, null),
                YouTubeRetentionPoint(.90, .32, null),
            ),
            fetchedAtMillis = 22 * hour,
        )
        val reach = YouTubeReachVideoSummary("video-1", impressions = 12_000, ctrPercent = 5.25)
        val checkpoints = listOf(
            checkpoint(target = 1_440, views = 2_000),
            checkpoint(target = 30, views = 300),
            checkpoint(target = 30, views = 310),
        )

        val result = CreatorVideoPostmortemEngine.build(task(), video, retention = retention, reach = reach, checkpoints = checkpoints)

        assertEquals(12_000L, result.performance.impressions)
        assertEquals(5.25, result.performance.ctrPercent!!, 0.001)
        assertEquals(.61, result.performance.retentionAt50Percent!!, 0.001)
        assertEquals(listOf(30, 1_440), result.checkpoints.map { it.targetMinutes })
        assertTrue(result.evidence.reach)
        assertTrue(result.evidence.retention)
        assertTrue(result.evidence.releaseCurve)
    }

    @Test
    fun `creation record keeps dna hook script references and creator learning`() {
        val workspace = CreatorContentWorkspace(
            audience = "Cinema lovers",
            promise = "Understand why the scene works",
            angle = "Craft over hype",
            hook = "This shot changes the whole scene",
            script = "Narration draft",
            references = listOf(CreatorProjectReference(label = "Interview", url = "https://example.com")),
            learnings = "Shorter opening worked better.",
        )
        val dna = CreatorContentDna(
            creatorModeId = "film_entertainment",
            archetypeId = "analysis",
            productionStyles = setOf("Voiceover"),
            platform = "YouTube",
            deliveryFormat = "Long-form",
        )

        val result = CreatorVideoPostmortemEngine.build(task(workspace = workspace, dna = dna), video)

        assertEquals("analysis", result.creation.archetypeId)
        assertTrue(result.creation.scriptPresent)
        assertEquals(1, result.creation.referenceCount)
        assertEquals("Shorter opening worked better.", result.creatorLearning)
        assertTrue(result.evidence.creationBrief)
        assertTrue(result.evidence.contentDna)
        assertTrue(result.evidence.creatorLearning)
    }

    private fun task(
        publishedAtMillis: Long = 20 * hour,
        workspace: CreatorContentWorkspace = CreatorContentWorkspace(),
        dna: CreatorContentDna = CreatorContentDna(),
    ) = CreatorTask(
        id = "project-1",
        title = "OG Analysis",
        platform = "YouTube",
        contentType = "Long-form",
        dueLabel = "Today",
        publishedAtMillis = publishedAtMillis,
        publishedUrl = "https://youtube.com/watch?v=video-1",
        contentDna = dna,
        workspace = workspace,
    )

    private fun event(
        id: String,
        type: CreatorWorkflowTimelineType,
        source: CreatorWorkflowTimelineSource,
        at: Long,
        stageId: String,
        label: String,
    ) = CreatorWorkflowTimelineEvent(
        id = id,
        taskId = "project-1",
        stageId = stageId,
        stageLabel = label,
        type = type,
        source = source,
        atMillis = at,
    )

    private fun checkpoint(target: Int, views: Long) = YouTubePublishCheckpoint(
        videoId = "video-1",
        targetMinutes = target,
        capturedAtMillis = 21 * hour,
        actualVideoAgeMinutes = target.toLong(),
        captureQuality = YouTubeCheckpointQuality.NEAR_TARGET,
        lifetimeViews = views,
        likes = 0,
        comments = 0,
        periodWatchMinutes = 0,
        averageViewDurationSeconds = 0,
        periodSubscribersGained = 0,
        periodSubscribersLost = 0,
        sourceWindowDays = 7,
    )
}
