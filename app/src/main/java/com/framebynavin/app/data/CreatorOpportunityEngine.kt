package com.framebynavin.app.data

import com.framebynavin.app.youtube.CreatorOpportunityAlert

enum class CreatorOpportunityKind {
    CHANNEL_MOMENTUM,
    VIDEO_MOMENTUM,
    MATCHED_IDEA,
    READY_IDEA,
    PROJECT_MOMENTUM,
}

enum class CreatorOpportunitySource { LOCAL, YOUTUBE, GEMINI }
enum class CreatorOpportunityTargetKind { INSIGHTS, IDEA_VAULT, PROJECT }

data class CreatorOpportunityEvidence(
    val id: String,
    val label: String,
    val source: CreatorOpportunitySource,
)

data class CreatorPlatformOpportunitySignal(
    val videoId: String,
    val title: String,
    val periodViews: Long,
    val baselineMultiple: Double,
    val viewSharePercent: Int,
)

data class CreatorAiOpportunitySignal(
    val videoId: String,
    val citedEvidenceCount: Int,
)

data class CreatorOpportunity(
    val id: String,
    val kicker: String,
    val title: String,
    val body: String,
    val actionLabel: String,
    val kind: CreatorOpportunityKind,
    val targetKind: CreatorOpportunityTargetKind,
    val targetId: String = "",
    val taskId: String? = null,
    val ideaId: String? = null,
    val videoId: String? = null,
    val confidence: Int,
    val score: Int,
    val evidence: List<CreatorOpportunityEvidence>,
) {
    val usesAiEvidence: Boolean get() = evidence.any { it.source == CreatorOpportunitySource.GEMINI }
}

data class CreatorOpportunitySnapshot(
    val primary: CreatorOpportunity?,
    val alternatives: List<CreatorOpportunity>,
) {
    val aiEvidenceUsed: Boolean get() = listOfNotNull(primary).plus(alternatives).any { it.usesAiEvidence }
    val evidenceSourceCount: Int get() = listOfNotNull(primary)
        .plus(alternatives)
        .flatMap { it.evidence }
        .map { it.source }
        .distinct()
        .size
}

/**
 * Evidence-backed creator opportunity ranking.
 *
 * Local creator state and YouTube evidence are sufficient to produce recommendations. Optional
 * Gemini reports may strengthen an already-grounded video opportunity, but AI never becomes a
 * prerequisite for ranking or silently creates a recommendation on its own.
 */
object CreatorOpportunityEngine {
    fun build(
        tasks: List<CreatorTask>,
        ideas: List<CreatorIdea>,
        youtubeAlerts: List<CreatorOpportunityAlert> = emptyList(),
        performanceSignals: List<CreatorPlatformOpportunitySignal> = emptyList(),
        aiSignals: List<CreatorAiOpportunitySignal> = emptyList(),
        nowMillis: Long = System.currentTimeMillis(),
    ): CreatorOpportunitySnapshot {
        val candidates = mutableListOf<CreatorOpportunity>()

        youtubeAlerts.forEachIndexed { index, alert ->
            val idea = alert.ideaId?.let { id -> ideas.firstOrNull { it.id == id } }
            val matchedIdea = idea != null
            val videoMomentum = alert.kicker.contains("VIDEO", ignoreCase = true)
            val kind = when {
                matchedIdea -> CreatorOpportunityKind.MATCHED_IDEA
                videoMomentum -> CreatorOpportunityKind.VIDEO_MOMENTUM
                else -> CreatorOpportunityKind.CHANNEL_MOMENTUM
            }
            val baseScore = when (kind) {
                CreatorOpportunityKind.MATCHED_IDEA -> 960
                CreatorOpportunityKind.VIDEO_MOMENTUM -> 920
                CreatorOpportunityKind.CHANNEL_MOMENTUM -> 870
                else -> 0
            }
            val ideaBoost = idea?.let(::ideaBoost) ?: 0
            val evidence = buildList {
                add(CreatorOpportunityEvidence("youtube.pulse.24h", "Recent YouTube momentum", CreatorOpportunitySource.YOUTUBE))
                if (idea != null) {
                    add(CreatorOpportunityEvidence("idea.${idea.id}", "Saved idea: ${idea.title}", CreatorOpportunitySource.LOCAL))
                }
            }
            candidates += CreatorOpportunity(
                id = "youtube-${alert.kicker.lowercase().replace(' ', '-')}-${alert.ideaId ?: index}",
                kicker = if (matchedIdea) "MATCHED OPPORTUNITY" else alert.kicker,
                title = alert.title,
                body = alert.body,
                actionLabel = if (matchedIdea) "OPEN MATCHED IDEA" else "REVIEW THE SIGNAL",
                kind = kind,
                targetKind = if (matchedIdea) CreatorOpportunityTargetKind.IDEA_VAULT else CreatorOpportunityTargetKind.INSIGHTS,
                targetId = alert.ideaId.orEmpty(),
                ideaId = alert.ideaId,
                confidence = (if (matchedIdea) 88 else 82) + (ideaBoost / 20),
                score = baseScore + ideaBoost,
                evidence = evidence,
            )
        }

        performanceSignals
            .filter { it.videoId.isNotBlank() && (it.baselineMultiple >= 1.20 || it.viewSharePercent >= 25) }
            .take(3)
            .forEach { signal ->
                val lift = ((signal.baselineMultiple - 1.0) * 100.0).toInt()
                val body = when {
                    signal.baselineMultiple >= 1.20 -> "This video is running about ${lift.coerceAtLeast(0)}% above your usual performance and represents ${signal.viewSharePercent}% of views in this period. Treat that as a signal to study or follow up, not proof of causation."
                    else -> "This video represents ${signal.viewSharePercent}% of views in this period. Review what you can reuse before choosing the next topic."
                }
                candidates += CreatorOpportunity(
                    id = "performance-${signal.videoId}",
                    kicker = "PERFORMANCE SIGNAL",
                    title = "Follow the signal from “${signal.title}”",
                    body = body,
                    actionLabel = "OPEN INSIGHTS",
                    kind = CreatorOpportunityKind.VIDEO_MOMENTUM,
                    targetKind = CreatorOpportunityTargetKind.INSIGHTS,
                    targetId = signal.videoId,
                    videoId = signal.videoId,
                    confidence = if (signal.baselineMultiple >= 1.5) 86 else 78,
                    score = 850 + ((signal.baselineMultiple * 30).toInt()).coerceAtMost(90) + signal.viewSharePercent.coerceAtMost(40),
                    evidence = listOf(
                        CreatorOpportunityEvidence("youtube.video.${signal.videoId}", "Current YouTube performance", CreatorOpportunitySource.YOUTUBE),
                    ),
                )
            }

        ideas.asSequence()
            .filter { it.status != IdeaStatus.ARCHIVED && it.status != IdeaStatus.CONVERTED }
            .sortedByDescending(::ideaScore)
            .take(2)
            .forEach { idea ->
                val ready = idea.status == IdeaStatus.READY_TO_PRODUCE
                val high = idea.potential == IdeaPotential.HIGH
                if (!ready && !high) return@forEach
                candidates += CreatorOpportunity(
                    id = "idea-${idea.id}",
                    kicker = if (ready) "READY IDEA" else "HIGH-POTENTIAL IDEA",
                    title = idea.title,
                    body = when {
                        ready && high -> "You marked this idea High potential and Ready to Produce. It is the strongest creator-owned opportunity waiting in your vault."
                        ready -> "This idea is already Ready to Produce, so it can become a project without more triage."
                        else -> "You marked this idea High potential. Develop the angle or move it toward production when current commitments allow."
                    },
                    actionLabel = "OPEN IDEA VAULT",
                    kind = CreatorOpportunityKind.READY_IDEA,
                    targetKind = CreatorOpportunityTargetKind.IDEA_VAULT,
                    targetId = idea.id,
                    ideaId = idea.id,
                    confidence = if (ready && high) 76 else 69,
                    score = 700 + ideaScore(idea),
                    evidence = listOf(
                        CreatorOpportunityEvidence("idea.${idea.id}", "Creator-owned Idea Vault signal", CreatorOpportunitySource.LOCAL),
                    ),
                )
            }

        CreatorPriorityEngine.rankActive(tasks, nowMillis).firstOrNull()?.let { task ->
            val recommendation = CreatorPriorityEngine.recommendation(task, nowMillis)
            candidates += CreatorOpportunity(
                id = "project-${task.id}",
                kicker = "EXECUTION OPPORTUNITY",
                title = task.title,
                body = "${recommendation.action}. ${recommendation.reason}",
                actionLabel = "OPEN PROJECT",
                kind = CreatorOpportunityKind.PROJECT_MOMENTUM,
                targetKind = CreatorOpportunityTargetKind.PROJECT,
                targetId = task.id,
                taskId = task.id,
                confidence = 64,
                score = 620 + recommendation.score.coerceIn(0, 180),
                evidence = listOf(
                    CreatorOpportunityEvidence("project.${task.id}", "Current workflow, deadline and priority", CreatorOpportunitySource.LOCAL),
                ),
            )
        }

        val aiByVideo = aiSignals.associateBy { it.videoId }
        val enriched = candidates.map { opportunity ->
            val videoId = opportunity.videoId ?: return@map opportunity
            val ai = aiByVideo[videoId] ?: return@map opportunity
            if (ai.citedEvidenceCount <= 0) return@map opportunity
            opportunity.copy(
                confidence = (opportunity.confidence + 4).coerceAtMost(95),
                evidence = opportunity.evidence + CreatorOpportunityEvidence(
                    id = "gemini.$videoId",
                    label = "Saved Gemini autopsy · ${ai.citedEvidenceCount} cited evidence item${if (ai.citedEvidenceCount == 1) "" else "s"}",
                    source = CreatorOpportunitySource.GEMINI,
                ),
            )
        }

        val ranked = enriched
            .distinctBy { it.kind to (it.targetId.ifBlank { it.title }) }
            .sortedWith(compareByDescending<CreatorOpportunity> { it.score }.thenByDescending { it.confidence })

        return CreatorOpportunitySnapshot(
            primary = ranked.firstOrNull(),
            alternatives = ranked.drop(1).take(2),
        )
    }

    private fun ideaBoost(idea: CreatorIdea): Int = when (idea.potential) {
        IdeaPotential.HIGH -> 50
        IdeaPotential.MEDIUM -> 25
        IdeaPotential.LOW -> 0
    } + when (idea.status) {
        IdeaStatus.READY_TO_PRODUCE -> 35
        IdeaStatus.RESEARCHING -> 20
        IdeaStatus.WORTH_EXPLORING -> 10
        else -> 0
    }

    private fun ideaScore(idea: CreatorIdea): Int = ideaBoost(idea) + when (idea.status) {
        IdeaStatus.READY_TO_PRODUCE -> 30
        IdeaStatus.RESEARCHING -> 20
        IdeaStatus.WORTH_EXPLORING -> 10
        else -> 0
    }
}
