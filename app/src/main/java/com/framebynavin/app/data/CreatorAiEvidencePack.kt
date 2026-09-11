package com.framebynavin.app.data

import java.util.Locale

enum class CreatorAiEvidenceClass {
    PUBLIC_VIDEO,
    PROJECT_CONTEXT,
    PLATFORM_ANALYTICS,
    CREATOR_LEARNING,
}

enum class CreatorAiEvidenceVisibility { PUBLIC, PRIVATE_CREATOR_DATA }

data class CreatorAiConsent(
    val enabled: Boolean = false,
    val sharePublicVideo: Boolean = false,
    val shareProjectContext: Boolean = false,
    val sharePerformance: Boolean = false,
    val shareCreatorLearning: Boolean = false,
)

data class CreatorAiEvidence(
    val id: String,
    val evidenceClass: CreatorAiEvidenceClass,
    val visibility: CreatorAiEvidenceVisibility,
    val label: String,
    val value: String,
)

data class CreatorAiEvidencePack(
    val schemaVersion: Int,
    val projectId: String,
    val videoId: String,
    val publicVideoUrl: String?,
    val evidence: List<CreatorAiEvidence>,
) {
    val privateEvidenceCount: Int get() = evidence.count { it.visibility == CreatorAiEvidenceVisibility.PRIVATE_CREATOR_DATA }
    val publicEvidenceCount: Int get() = evidence.size - privateEvidenceCount
}

/**
 * Privacy-first evidence builder for the optional Gemini layer.
 *
 * Nothing is emitted while AI is disabled. Full scripts/research notes are deliberately not part of
 * this v104 schema. A later feature must add them as a separate, explicit consent surface rather
 * than silently widening an existing permission.
 */
object CreatorAiEvidencePackBuilder {
    const val SCHEMA_VERSION = 1

    fun build(postmortem: CreatorVideoPostmortem, consent: CreatorAiConsent): CreatorAiEvidencePack {
        if (!consent.enabled) {
            return CreatorAiEvidencePack(SCHEMA_VERSION, postmortem.taskId, postmortem.videoId, null, emptyList())
        }

        val evidence = buildList {
            if (consent.sharePublicVideo) {
                add(public("video.title", CreatorAiEvidenceClass.PUBLIC_VIDEO, "Published video title", postmortem.videoTitle))
                add(public("video.youtube_url", CreatorAiEvidenceClass.PUBLIC_VIDEO, "Public YouTube URL", youtubeUrl(postmortem.videoId)))
                if (postmortem.publishedAtMillis > 0L) {
                    add(public("video.published_at", CreatorAiEvidenceClass.PUBLIC_VIDEO, "Published at millis", postmortem.publishedAtMillis.toString()))
                }
            }

            if (consent.shareProjectContext) {
                val creation = postmortem.creation
                addPrivateIfPresent("project.title", "Project title", postmortem.projectTitle)
                addPrivateIfPresent("project.creator_mode", "Creator mode", creation.creatorModeId)
                addPrivateIfPresent("project.archetype", "Content archetype", creation.archetypeId)
                addPrivateIfPresent("project.production_styles", "Production styles", creation.productionStyles.joinToString(", "))
                addPrivateIfPresent("project.platform", "Platform", creation.platform)
                addPrivateIfPresent("project.delivery_format", "Delivery format", creation.deliveryFormat)
                addPrivateIfPresent("project.audience", "Intended audience", creation.audience)
                addPrivateIfPresent("project.viewer_problem", "Viewer problem", creation.viewerProblem)
                addPrivateIfPresent("project.promise", "Promise", creation.promise)
                addPrivateIfPresent("project.angle", "Angle", creation.angle)
                addPrivateIfPresent("project.selected_hook", "Selected hook", creation.hook)
                addPrivateIfPresent("package.title", "Published title snapshot", postmortem.packaging.titleSnapshot)
                addPrivateIfPresent("package.thumbnail", "Thumbnail concept snapshot", postmortem.packaging.thumbnailSnapshot)
                if (creation.scriptPresent) {
                    add(privateEvidence("project.script_size", CreatorAiEvidenceClass.PROJECT_CONTEXT, "Captured script size", "${creation.scriptCharacterCount} characters; full script withheld in v104"))
                }
                if (postmortem.projectToPublishMillis > 0L) {
                    add(privateEvidence("workflow.project_to_publish_ms", CreatorAiEvidenceClass.PROJECT_CONTEXT, "Project-to-publish elapsed residence", postmortem.projectToPublishMillis.toString()))
                }
                postmortem.stageSpans.forEachIndexed { index, span ->
                    add(privateEvidence(
                        "workflow.stage.$index",
                        CreatorAiEvidenceClass.PROJECT_CONTEXT,
                        "Stage residence ${span.stageLabel}",
                        "${span.residenceMillis} ms${if (span.lowerBound) "; lower bound" else ""}",
                    ))
                }
            }

            if (consent.sharePerformance) {
                val p = postmortem.performance
                add(public("performance.period_views", CreatorAiEvidenceClass.PLATFORM_ANALYTICS, "Period views", p.periodViews.toString()))
                add(public("performance.lifetime_views", CreatorAiEvidenceClass.PLATFORM_ANALYTICS, "Lifetime views", p.lifetimeViews.toString()))
                add(public("performance.watch_minutes", CreatorAiEvidenceClass.PLATFORM_ANALYTICS, "Watch minutes", p.watchMinutes.toString()))
                add(public("performance.avg_view_seconds", CreatorAiEvidenceClass.PLATFORM_ANALYTICS, "Average view duration seconds", p.averageViewDurationSeconds.toString()))
                add(public("performance.net_subscribers", CreatorAiEvidenceClass.PLATFORM_ANALYTICS, "Net subscribers", p.netSubscribers.toString()))
                add(public("performance.likes", CreatorAiEvidenceClass.PLATFORM_ANALYTICS, "Likes", p.likes.toString()))
                add(public("performance.comments", CreatorAiEvidenceClass.PLATFORM_ANALYTICS, "Comments", p.comments.toString()))
                p.impressions?.let { add(public("performance.impressions", CreatorAiEvidenceClass.PLATFORM_ANALYTICS, "Thumbnail impressions", it.toString())) }
                p.ctrPercent?.let { add(public("performance.ctr", CreatorAiEvidenceClass.PLATFORM_ANALYTICS, "Thumbnail CTR percent", String.format(Locale.US, "%.4f", it))) }
                p.retentionAt10Percent?.let { add(public("retention.10", CreatorAiEvidenceClass.PLATFORM_ANALYTICS, "Audience watch ratio near 10%", String.format(Locale.US, "%.5f", it))) }
                p.retentionAt50Percent?.let { add(public("retention.50", CreatorAiEvidenceClass.PLATFORM_ANALYTICS, "Audience watch ratio near 50%", String.format(Locale.US, "%.5f", it))) }
                p.retentionAt90Percent?.let { add(public("retention.90", CreatorAiEvidenceClass.PLATFORM_ANALYTICS, "Audience watch ratio near 90%", String.format(Locale.US, "%.5f", it))) }
                postmortem.checkpoints.forEach { checkpoint ->
                    add(public(
                        "release.${checkpoint.targetMinutes}",
                        CreatorAiEvidenceClass.PLATFORM_ANALYTICS,
                        "Release checkpoint ${checkpoint.targetMinutes} minutes",
                        "views=${checkpoint.lifetimeViews}; watchMinutes=${checkpoint.periodWatchMinutes}; avgViewSeconds=${checkpoint.averageViewDurationSeconds}; netSubscribers=${checkpoint.netSubscribers}; actualAgeMinutes=${checkpoint.actualVideoAgeMinutes}; quality=${checkpoint.captureQuality.name}",
                    ))
                }
            }

            if (consent.shareCreatorLearning) {
                addPrivateIfPresent("creator.learning", "Creator learning note", postmortem.creatorLearning, CreatorAiEvidenceClass.CREATOR_LEARNING)
            }
        }

        return CreatorAiEvidencePack(
            schemaVersion = SCHEMA_VERSION,
            projectId = postmortem.taskId,
            videoId = postmortem.videoId,
            publicVideoUrl = youtubeUrl(postmortem.videoId).takeIf { consent.sharePublicVideo },
            evidence = evidence,
        )
    }

    fun videoAutopsyPrompt(pack: CreatorAiEvidencePack): String = buildString {
        appendLine("You are analyzing one creator's published video using a FrameByNavin evidence pack.")
        appendLine("Use only the supplied evidence and the public YouTube video when a URL is present.")
        appendLine("Separate direct observation from inference. Never invent missing metrics, timestamps, creator intent, or causation.")
        appendLine("When making a claim, cite the relevant evidence id in square brackets, for example [retention.50].")
        appendLine("Treat correlations as hypotheses, not proof. If evidence is insufficient, say so explicitly.")
        appendLine("Return: 1) strongest opening observations, 2) retention/pacing moments, 3) packaging-to-content alignment, 4) likely strengths, 5) possible weaknesses, 6) three evidence-grounded lessons for the next video.")
        appendLine("Evidence schema: ${pack.schemaVersion}")
        pack.publicVideoUrl?.let { appendLine("Public video: $it") }
        pack.evidence.forEach { appendLine("[${it.id}] ${it.label}: ${it.value}") }
    }.trim()

    private fun MutableList<CreatorAiEvidence>.addPrivateIfPresent(
        id: String,
        label: String,
        value: String,
        klass: CreatorAiEvidenceClass = CreatorAiEvidenceClass.PROJECT_CONTEXT,
    ) {
        value.trim().takeIf { it.isNotBlank() }?.let { add(privateEvidence(id, klass, label, it)) }
    }

    private fun public(id: String, klass: CreatorAiEvidenceClass, label: String, value: String) =
        CreatorAiEvidence(
            id,
            klass,
            if (klass == CreatorAiEvidenceClass.PLATFORM_ANALYTICS) CreatorAiEvidenceVisibility.PRIVATE_CREATOR_DATA
            else CreatorAiEvidenceVisibility.PUBLIC,
            label,
            value,
        )

    private fun privateEvidence(id: String, klass: CreatorAiEvidenceClass, label: String, value: String) =
        CreatorAiEvidence(id, klass, CreatorAiEvidenceVisibility.PRIVATE_CREATOR_DATA, label, value)

    private fun youtubeUrl(videoId: String): String = "https://www.youtube.com/watch?v=$videoId"
}
