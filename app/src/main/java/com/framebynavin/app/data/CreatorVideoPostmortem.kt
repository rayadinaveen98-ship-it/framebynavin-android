package com.framebynavin.app.data

import com.framebynavin.app.youtube.YouTubePublishCheckpoint
import com.framebynavin.app.youtube.YouTubeReachVideoSummary
import com.framebynavin.app.youtube.YouTubeVideoRetentionSnapshot
import com.framebynavin.app.youtube.YouTubeVideoSnapshot
import kotlin.math.abs

/** How trustworthy a project-to-publication duration is. */
enum class CreatorPostmortemTimingQuality { EXACT_FROM_PROJECT_CREATION, LOWER_BOUND_FROM_FIRST_OBSERVATION, UNAVAILABLE }

data class CreatorPostmortemStageSpan(
    val stageId: String,
    val stageLabel: String,
    val enteredAtMillis: Long,
    val exitedAtMillis: Long,
    val residenceMillis: Long,
    val lowerBound: Boolean,
)

data class CreatorPostmortemCreation(
    val creatorModeId: String,
    val archetypeId: String,
    val productionStyles: List<String>,
    val platform: String,
    val deliveryFormat: String,
    val audience: String,
    val viewerProblem: String,
    val promise: String,
    val angle: String,
    val hook: String,
    val scriptPresent: Boolean,
    val scriptCharacterCount: Int,
    val referenceCount: Int,
)

data class CreatorPostmortemPackaging(
    val titleSnapshot: String,
    val thumbnailSnapshot: String,
    val descriptionPresent: Boolean,
    val tagsPresent: Boolean,
    val publicationRevisionCount: Int,
    val matchedPublishedDeliverable: Boolean,
)

data class CreatorPostmortemPerformance(
    val periodViews: Long,
    val lifetimeViews: Long,
    val watchMinutes: Long,
    val averageViewDurationSeconds: Long,
    val netSubscribers: Long,
    val likes: Long,
    val comments: Long,
    val impressions: Long?,
    val ctrPercent: Double?,
    val retentionPointCount: Int,
    val retentionAt10Percent: Double?,
    val retentionAt50Percent: Double?,
    val retentionAt90Percent: Double?,
)

data class CreatorPostmortemEvidence(
    val creationBrief: Boolean,
    val contentDna: Boolean,
    val workflowTimeline: Boolean,
    val packagingHistory: Boolean,
    val releaseCurve: Boolean,
    val retention: Boolean,
    val reach: Boolean,
    val creatorLearning: Boolean,
) {
    val capturedCount: Int get() = listOf(
        creationBrief,
        contentDna,
        workflowTimeline,
        packagingHistory,
        releaseCurve,
        retention,
        reach,
        creatorLearning,
    ).count { it }
    val totalCount: Int get() = 8
}

data class CreatorVideoPostmortem(
    val taskId: String,
    val projectTitle: String,
    val videoId: String,
    val videoTitle: String,
    val publishedAtMillis: Long,
    val creation: CreatorPostmortemCreation,
    val packaging: CreatorPostmortemPackaging,
    val stageSpans: List<CreatorPostmortemStageSpan>,
    val projectToPublishMillis: Long,
    val timingQuality: CreatorPostmortemTimingQuality,
    val performance: CreatorPostmortemPerformance,
    val checkpoints: List<YouTubePublishCheckpoint>,
    val creatorLearning: String,
    val evidence: CreatorPostmortemEvidence,
)

/**
 * Joins creator-owned creation history to platform outcomes without inventing missing evidence.
 *
 * This is intentionally a pure derived record. Project/workspace/timeline data remain authoritative
 * creator history; YouTube data remains rebuildable platform evidence; post-publish checkpoints are
 * durable release-curve observations. No metric is inferred when its source is absent.
 */
object CreatorVideoPostmortemEngine {
    fun build(
        task: CreatorTask,
        video: YouTubeVideoSnapshot,
        timeline: List<CreatorWorkflowTimelineEvent> = emptyList(),
        retention: YouTubeVideoRetentionSnapshot? = null,
        reach: YouTubeReachVideoSummary? = null,
        checkpoints: List<YouTubePublishCheckpoint> = emptyList(),
    ): CreatorVideoPostmortem {
        require(task.id.isNotBlank()) { "A linked creator project is required" }
        require(video.videoId.isNotBlank()) { "A linked YouTube video is required" }

        val workspace = task.workspace
        val dna = task.contentDna
        val deliverable = chooseDeliverable(task, video.videoId)
        val publication = deliverable?.publicationHistory
            ?.filter { it.kind == CreatorPublicationEventKind.PUBLISHED || it.kind == CreatorPublicationEventKind.UPDATED }
            ?.maxByOrNull { it.atMillis }

        val titleSnapshot = publication?.titleSnapshot?.takeIf { it.isNotBlank() }
            ?: deliverable?.title?.takeIf { it.isNotBlank() }
            ?: video.title
        val thumbnailSnapshot = publication?.thumbnailSnapshot?.takeIf { it.isNotBlank() }
            ?: deliverable?.thumbnailConcept.orEmpty()
        val description = publication?.descriptionSnapshot?.takeIf { it.isNotBlank() }
            ?: deliverable?.description.orEmpty()
        val tags = publication?.tagsSnapshot?.takeIf { it.isNotBlank() }
            ?: deliverable?.tags.orEmpty()

        val stageSpans = stageSpans(task.id, timeline)
        val publishedAt = task.publishedAtMillis.takeIf { it > 0L } ?: video.publishedAtMillis
        val firstEntry = timeline
            .filter { it.taskId == task.id && it.type == CreatorWorkflowTimelineType.ENTERED && it.atMillis > 0L }
            .minByOrNull { it.atMillis }
        val timingQuality = when {
            firstEntry == null || publishedAt <= firstEntry.atMillis -> CreatorPostmortemTimingQuality.UNAVAILABLE
            firstEntry.source == CreatorWorkflowTimelineSource.PROJECT_CREATED -> CreatorPostmortemTimingQuality.EXACT_FROM_PROJECT_CREATION
            else -> CreatorPostmortemTimingQuality.LOWER_BOUND_FROM_FIRST_OBSERVATION
        }
        val projectToPublish = if (timingQuality == CreatorPostmortemTimingQuality.UNAVAILABLE) 0L
            else publishedAt - requireNotNull(firstEntry).atMillis

        val sortedCheckpoints = checkpoints
            .filter { it.videoId == video.videoId }
            .distinctBy { it.targetMinutes }
            .sortedBy { it.targetMinutes }
        val creationBriefPresent = listOf(
            workspace.audience,
            workspace.viewerProblem,
            workspace.promise,
            workspace.angle,
            workspace.hook,
            workspace.script,
        ).any { it.isNotBlank() } || workspace.references.isNotEmpty()
        val dnaPresent = !dna.isEmpty || task.platform.isNotBlank() || task.contentType.isNotBlank()
        val packagingPresent = deliverable != null || task.publishedUrl.isNotBlank()

        return CreatorVideoPostmortem(
            taskId = task.id,
            projectTitle = task.title,
            videoId = video.videoId,
            videoTitle = video.title,
            publishedAtMillis = publishedAt,
            creation = CreatorPostmortemCreation(
                creatorModeId = dna.creatorModeId,
                archetypeId = dna.archetypeId.ifBlank { task.contentType },
                productionStyles = dna.productionStyles.toList().sorted(),
                platform = dna.platform.ifBlank { task.platform },
                deliveryFormat = dna.deliveryFormat.ifBlank { task.contentType },
                audience = workspace.audience,
                viewerProblem = workspace.viewerProblem,
                promise = workspace.promise,
                angle = workspace.angle,
                hook = workspace.scriptStudio?.selectedHook().orEmpty().ifBlank { workspace.hook },
                scriptPresent = workspace.script.isNotBlank() || workspace.scriptStudio?.isEmpty() == false,
                scriptCharacterCount = workspace.script.length + (workspace.scriptStudio?.let(::scriptStudioCharacters) ?: 0),
                referenceCount = workspace.references.size,
            ),
            packaging = CreatorPostmortemPackaging(
                titleSnapshot = titleSnapshot,
                thumbnailSnapshot = thumbnailSnapshot,
                descriptionPresent = description.isNotBlank(),
                tagsPresent = tags.isNotBlank(),
                publicationRevisionCount = deliverable?.publicationHistory?.size ?: 0,
                matchedPublishedDeliverable = deliverable != null,
            ),
            stageSpans = stageSpans,
            projectToPublishMillis = projectToPublish,
            timingQuality = timingQuality,
            performance = CreatorPostmortemPerformance(
                periodViews = video.periodViews,
                lifetimeViews = video.lifetimeViews,
                watchMinutes = video.watchMinutes,
                averageViewDurationSeconds = video.averageViewDurationSeconds,
                netSubscribers = video.netSubscribers,
                likes = video.likes,
                comments = video.comments,
                impressions = reach?.impressions,
                ctrPercent = reach?.ctrPercent,
                retentionPointCount = retention?.points?.size ?: 0,
                retentionAt10Percent = retentionValue(retention, .10),
                retentionAt50Percent = retentionValue(retention, .50),
                retentionAt90Percent = retentionValue(retention, .90),
            ),
            checkpoints = sortedCheckpoints,
            creatorLearning = workspace.learnings.trim(),
            evidence = CreatorPostmortemEvidence(
                creationBrief = creationBriefPresent,
                contentDna = dnaPresent,
                workflowTimeline = timeline.any { it.taskId == task.id },
                packagingHistory = packagingPresent,
                releaseCurve = sortedCheckpoints.isNotEmpty(),
                retention = retention?.points?.isNotEmpty() == true,
                reach = reach != null,
                creatorLearning = workspace.learnings.isNotBlank(),
            ),
        )
    }

    private fun chooseDeliverable(task: CreatorTask, videoId: String): CreatorDeliverable? {
        val youtube = task.workspace.deliverables.filter {
            it.platform.equals("YouTube", ignoreCase = true) ||
                it.publishedUrl.contains("youtu", ignoreCase = true) ||
                it.publicationHistory.any { event -> event.url.contains("youtu", ignoreCase = true) }
        }
        return youtube.firstOrNull { deliverable ->
            deliverable.publishedUrl.contains(videoId) ||
                deliverable.publicationHistory.any { it.url.contains(videoId) }
        } ?: youtube.maxByOrNull { deliverable ->
            maxOf(
                deliverable.publishedAtMillis,
                deliverable.publicationHistory.maxOfOrNull { it.atMillis } ?: 0L,
            )
        }
    }

    private fun stageSpans(taskId: String, timeline: List<CreatorWorkflowTimelineEvent>): List<CreatorPostmortemStageSpan> {
        val events = timeline.filter { it.taskId == taskId && it.atMillis > 0L }.sortedBy { it.atMillis }
        val open = mutableMapOf<String, CreatorWorkflowTimelineEvent>()
        val result = mutableListOf<CreatorPostmortemStageSpan>()
        events.forEach { event ->
            when (event.type) {
                CreatorWorkflowTimelineType.ENTERED -> open[event.stageId] = event
                CreatorWorkflowTimelineType.EXITED -> {
                    val entered = open.remove(event.stageId) ?: return@forEach
                    if (event.atMillis <= entered.atMillis) return@forEach
                    result += CreatorPostmortemStageSpan(
                        stageId = event.stageId,
                        stageLabel = entered.stageLabel.ifBlank { event.stageLabel },
                        enteredAtMillis = entered.atMillis,
                        exitedAtMillis = event.atMillis,
                        residenceMillis = event.atMillis - entered.atMillis,
                        lowerBound = entered.source == CreatorWorkflowTimelineSource.OBSERVED_CURRENT,
                    )
                }
            }
        }
        return result.sortedBy { it.enteredAtMillis }
    }

    private fun retentionValue(snapshot: YouTubeVideoRetentionSnapshot?, target: Double): Double? {
        val points = snapshot?.points.orEmpty()
        if (points.isEmpty()) return null
        return points.minByOrNull { abs(it.elapsedVideoTimeRatio - target) }?.audienceWatchRatio
    }

    private fun scriptStudioCharacters(studio: CreatorScriptStudio): Int = studio.compiledNarration().length
}
