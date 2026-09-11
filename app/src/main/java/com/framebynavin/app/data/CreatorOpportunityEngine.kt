package com.framebynavin.app.data

import com.framebynavin.app.youtube.CreatorOpportunityAlert

enum class CreatorOpportunityKind {
    CHANNEL_MOMENTUM,
    VIDEO_MOMENTUM,
    MATCHED_IDEA,
    READY_IDEA,
    PROJECT_MOMENTUM,
}

enum class CreatorOpportunitySource { LOCAL, YOUTUBE, GEMINI, PLAYBOOK, CREATOR_BRAIN }
enum class CreatorOpportunityTargetKind { INSIGHTS, IDEA_VAULT, PROJECT }
enum class CreatorOpportunityHorizon { NOW, NEXT, LATER }

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

data class CreatorOpportunityScorecard(
    val urgency: Int = 50,
    val momentum: Int = 50,
    val readiness: Int = 50,
    val evidenceStrength: Int = 50,
    val strategicValue: Int = 50,
) {
    val weightedScore: Int
        get() = (
            urgency.coerceIn(0, 100) * 30 +
                momentum.coerceIn(0, 100) * 25 +
                readiness.coerceIn(0, 100) * 20 +
                evidenceStrength.coerceIn(0, 100) * 15 +
                strategicValue.coerceIn(0, 100) * 10
            ) / 100
}

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
    val horizon: CreatorOpportunityHorizon = CreatorOpportunityHorizon.NEXT,
    val scorecard: CreatorOpportunityScorecard = CreatorOpportunityScorecard(),
) {
    val usesAiEvidence: Boolean get() = evidence.any { it.source == CreatorOpportunitySource.GEMINI }
    val usesPlaybookEvidence: Boolean get() = evidence.any { it.source == CreatorOpportunitySource.PLAYBOOK }
    val usesCreatorBrainEvidence: Boolean get() = evidence.any { it.source == CreatorOpportunitySource.CREATOR_BRAIN }
}

data class CreatorOpportunitySnapshot(
    val primary: CreatorOpportunity?,
    val alternatives: List<CreatorOpportunity>,
    val now: List<CreatorOpportunity> = emptyList(),
    val next: List<CreatorOpportunity> = emptyList(),
    val later: List<CreatorOpportunity> = emptyList(),
) {
    private val visible: List<CreatorOpportunity>
        get() = (now + next + later).ifEmpty { listOfNotNull(primary).plus(alternatives) }

    val aiEvidenceUsed: Boolean get() = visible.any { it.usesAiEvidence }
    val playbookEvidenceUsed: Boolean get() = visible.any { it.usesPlaybookEvidence }
    val creatorBrainEvidenceUsed: Boolean get() = visible.any { it.usesCreatorBrainEvidence }
    val evidenceSourceCount: Int get() = visible
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
 *
 * Alpha 1.4A adds the first Creator Brain memory across Content DNA, content type, platform, hook
 * style and workflow pace. Brain patterns require repeated evaluated outcomes and only add a tightly
 * capped adjustment to recommendations that can be linked to a real project. Current urgency,
 * readiness, YouTube evidence and the broader Creator Playbook remain dominant.
 */
object CreatorOpportunityEngine {
    fun build(
        tasks: List<CreatorTask>,
        ideas: List<CreatorIdea>,
        youtubeAlerts: List<CreatorOpportunityAlert> = emptyList(),
        performanceSignals: List<CreatorPlatformOpportunitySignal> = emptyList(),
        aiSignals: List<CreatorAiOpportunitySignal> = emptyList(),
        playbook: CreatorPlaybookSnapshot = CreatorPlaybookSnapshot(),
        brain: CreatorBrainSnapshot = CreatorBrainSnapshot(),
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
            val scorecard = when (kind) {
                CreatorOpportunityKind.MATCHED_IDEA -> CreatorOpportunityScorecard(
                    urgency = 95,
                    momentum = 92,
                    readiness = idea?.let(::ideaReadiness) ?: 70,
                    evidenceStrength = 92,
                    strategicValue = 90,
                )
                CreatorOpportunityKind.VIDEO_MOMENTUM -> CreatorOpportunityScorecard(
                    urgency = 88,
                    momentum = 90,
                    readiness = 62,
                    evidenceStrength = 80,
                    strategicValue = 82,
                )
                else -> CreatorOpportunityScorecard(
                    urgency = 76,
                    momentum = 82,
                    readiness = 52,
                    evidenceStrength = 72,
                    strategicValue = 74,
                )
            }
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
                confidence = ((if (matchedIdea) 88 else 82) + (ideaBoost / 20)).coerceAtMost(95),
                score = baseScore + ideaBoost + scorecard.weightedScore,
                evidence = evidence,
                horizon = when (kind) {
                    CreatorOpportunityKind.MATCHED_IDEA,
                    CreatorOpportunityKind.VIDEO_MOMENTUM -> CreatorOpportunityHorizon.NOW
                    else -> CreatorOpportunityHorizon.NEXT
                },
                scorecard = scorecard,
            )
        }

        performanceSignals
            .filter { it.videoId.isNotBlank() && (it.baselineMultiple >= 1.20 || it.viewSharePercent >= 25) }
            .sortedByDescending { it.baselineMultiple * 100 + it.viewSharePercent }
            .take(3)
            .forEach { signal ->
                val lift = ((signal.baselineMultiple - 1.0) * 100.0).toInt()
                val momentumScore = (
                    58 + ((signal.baselineMultiple - 1.0) * 42.0).toInt() + (signal.viewSharePercent / 3)
                    ).coerceIn(55, 100)
                val scorecard = CreatorOpportunityScorecard(
                    urgency = if (signal.baselineMultiple >= 1.5 || signal.viewSharePercent >= 40) 92 else 80,
                    momentum = momentumScore,
                    readiness = 58,
                    evidenceStrength = if (signal.periodViews > 0L) 82 else 74,
                    strategicValue = if (signal.viewSharePercent >= 30) 84 else 76,
                )
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
                    taskId = tasks.firstOrNull { task ->
                        CreatorRecommendationOutcomeEngine.extractYouTubeVideoId(task.publishedUrl) == signal.videoId
                    }?.id,
                    videoId = signal.videoId,
                    confidence = if (signal.baselineMultiple >= 1.5) 86 else 78,
                    score = 850 + ((signal.baselineMultiple * 30).toInt()).coerceAtMost(90) + signal.viewSharePercent.coerceAtMost(40) + scorecard.weightedScore,
                    evidence = listOf(
                        CreatorOpportunityEvidence("youtube.video.${signal.videoId}", "Current YouTube performance", CreatorOpportunitySource.YOUTUBE),
                    ),
                    horizon = if (signal.baselineMultiple >= 1.35 || signal.viewSharePercent >= 35) CreatorOpportunityHorizon.NOW else CreatorOpportunityHorizon.NEXT,
                    scorecard = scorecard,
                )
            }

        ideas.asSequence()
            .filter { it.status != IdeaStatus.ARCHIVED && it.status != IdeaStatus.CONVERTED }
            .sortedByDescending(::ideaScore)
            .take(3)
            .forEach { idea ->
                val ready = idea.status == IdeaStatus.READY_TO_PRODUCE
                val high = idea.potential == IdeaPotential.HIGH
                if (!ready && !high) return@forEach
                val scorecard = CreatorOpportunityScorecard(
                    urgency = if (ready) 62 else 38,
                    momentum = 34,
                    readiness = ideaReadiness(idea),
                    evidenceStrength = 60,
                    strategicValue = when (idea.potential) {
                        IdeaPotential.HIGH -> 86
                        IdeaPotential.MEDIUM -> 70
                        IdeaPotential.LOW -> 52
                    },
                )
                candidates += CreatorOpportunity(
                    id = "idea-${idea.id}",
                    kicker = if (ready) "READY IDEA" else "HIGH-POTENTIAL IDEA",
                    title = idea.title,
                    body = when {
                        ready && high -> "You marked this idea High potential and Ready to Produce. It is a strong creator-owned option once the current NOW move is handled."
                        ready -> "This idea is already Ready to Produce, so it can become a project without more triage."
                        else -> "You marked this idea High potential. Develop the angle before it competes with work that is already ready or showing live momentum."
                    },
                    actionLabel = "OPEN IDEA VAULT",
                    kind = CreatorOpportunityKind.READY_IDEA,
                    targetKind = CreatorOpportunityTargetKind.IDEA_VAULT,
                    targetId = idea.id,
                    ideaId = idea.id,
                    confidence = if (ready && high) 76 else 69,
                    score = 700 + ideaScore(idea) + scorecard.weightedScore,
                    evidence = listOf(
                        CreatorOpportunityEvidence("idea.${idea.id}", "Creator-owned Idea Vault signal", CreatorOpportunitySource.LOCAL),
                    ),
                    horizon = if (ready) CreatorOpportunityHorizon.NEXT else CreatorOpportunityHorizon.LATER,
                    scorecard = scorecard,
                )
            }

        CreatorPriorityEngine.rankActive(tasks, nowMillis).firstOrNull()?.let { task ->
            val recommendation = CreatorPriorityEngine.recommendation(task, nowMillis)
            val urgentProject = recommendation.score >= 100
            val scorecard = CreatorOpportunityScorecard(
                urgency = (55 + recommendation.score / 4).coerceIn(55, 96),
                momentum = if (task.status == TaskStatus.WORKING) 72 else 48,
                readiness = if (task.status == TaskStatus.WORKING) 92 else 78,
                evidenceStrength = 70,
                strategicValue = 72,
            )
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
                confidence = if (urgentProject) 74 else 66,
                score = 620 + recommendation.score.coerceIn(0, 180) + scorecard.weightedScore,
                evidence = listOf(
                    CreatorOpportunityEvidence("project.${task.id}", "Current workflow, deadline and priority", CreatorOpportunitySource.LOCAL),
                ),
                horizon = if (urgentProject) CreatorOpportunityHorizon.NOW else CreatorOpportunityHorizon.NEXT,
                scorecard = scorecard,
            )
        }

        val aiByVideo = aiSignals.associateBy { it.videoId }
        val aiEnriched = candidates.map { opportunity ->
            val videoId = opportunity.videoId ?: return@map opportunity
            val ai = aiByVideo[videoId] ?: return@map opportunity
            if (ai.citedEvidenceCount <= 0) return@map opportunity
            opportunity.copy(
                confidence = (opportunity.confidence + 4).coerceAtMost(95),
                score = opportunity.score + 12,
                scorecard = opportunity.scorecard.copy(
                    evidenceStrength = (opportunity.scorecard.evidenceStrength + 8).coerceAtMost(100),
                ),
                evidence = opportunity.evidence + CreatorOpportunityEvidence(
                    id = "gemini.$videoId",
                    label = "Saved Gemini autopsy · ${ai.citedEvidenceCount} cited evidence item${if (ai.citedEvidenceCount == 1) "" else "s"}",
                    source = CreatorOpportunitySource.GEMINI,
                ),
            )
        }

        val outcomeWeighted = aiEnriched.map { opportunity ->
            val pattern = playbook.patternFor(opportunity.kind) ?: return@map opportunity
            if (pattern.state == CreatorPlaybookState.LEARNING || pattern.rankingDelta == 0) return@map opportunity

            val positive = pattern.rankingDelta > 0
            val strategicDelta = (pattern.rankingDelta / 2).coerceIn(-9, 14)
            opportunity.copy(
                confidence = if (positive) {
                    (opportunity.confidence + 2).coerceAtMost(95)
                } else {
                    (opportunity.confidence - 1).coerceAtLeast(45)
                },
                score = opportunity.score + pattern.rankingDelta,
                scorecard = opportunity.scorecard.copy(
                    strategicValue = (opportunity.scorecard.strategicValue + strategicDelta).coerceIn(0, 100),
                ),
                evidence = opportunity.evidence + CreatorOpportunityEvidence(
                    id = "playbook.${opportunity.kind.name.lowercase()}",
                    label = if (positive) {
                        "Creator Playbook · ${pattern.positiveCount}/${pattern.evaluatedCount} ${pattern.label} outcomes positive"
                    } else {
                        "Creator Playbook caution · ${pattern.positiveCount}/${pattern.evaluatedCount} ${pattern.label} outcomes positive"
                    },
                    source = CreatorOpportunitySource.PLAYBOOK,
                ),
            )
        }

        val brainWeighted = outcomeWeighted.map { opportunity ->
            val linkedTask = opportunity.taskId?.let { id -> tasks.firstOrNull { it.id == id } }
                ?: opportunity.videoId?.let { videoId ->
                    tasks.firstOrNull { task ->
                        CreatorRecommendationOutcomeEngine.extractYouTubeVideoId(task.publishedUrl) == videoId
                    }
                }
                ?: return@map opportunity
            val match = CreatorBrainEngine.matchForTask(linkedTask, brain)
            if (match.rankingDelta == 0 || match.patterns.isEmpty()) return@map opportunity

            val positive = match.rankingDelta > 0
            val strategicDelta = (match.rankingDelta / 2).coerceIn(-7, 8)
            opportunity.copy(
                confidence = if (positive) {
                    (opportunity.confidence + 1).coerceAtMost(95)
                } else {
                    (opportunity.confidence - 1).coerceAtLeast(45)
                },
                score = opportunity.score + match.rankingDelta,
                scorecard = opportunity.scorecard.copy(
                    strategicValue = (opportunity.scorecard.strategicValue + strategicDelta).coerceIn(0, 100),
                ),
                evidence = opportunity.evidence + match.patterns.map { pattern ->
                    CreatorOpportunityEvidence(
                        id = "brain.${pattern.id}",
                        label = "Creator Brain ${pattern.state.name.lowercase()} · ${pattern.label} · ${pattern.positiveCount}/${pattern.evaluatedCount} positive",
                        source = CreatorOpportunitySource.CREATOR_BRAIN,
                    )
                },
            )
        }

        val ranked = brainWeighted
            .distinctBy { it.kind to (it.targetId.ifBlank { it.title }) }
            .sortedWith(
                compareByDescending<CreatorOpportunity> { it.score }
                    .thenByDescending { it.scorecard.weightedScore }
                    .thenByDescending { it.confidence }
            )

        val balanced = balanceHorizons(ranked)
        val primary = balanced.firstOrNull { it.horizon == CreatorOpportunityHorizon.NOW } ?: balanced.firstOrNull()

        return CreatorOpportunitySnapshot(
            primary = primary,
            alternatives = balanced.filterNot { it.id == primary?.id }.take(2),
            now = balanced.filter { it.horizon == CreatorOpportunityHorizon.NOW }.take(2),
            next = balanced.filter { it.horizon == CreatorOpportunityHorizon.NEXT }.take(2),
            later = balanced.filter { it.horizon == CreatorOpportunityHorizon.LATER }.take(2),
        )
    }

    private fun balanceHorizons(ranked: List<CreatorOpportunity>): List<CreatorOpportunity> {
        if (ranked.isEmpty()) return ranked
        var balanced = ranked
        if (balanced.none { it.horizon == CreatorOpportunityHorizon.NOW }) {
            balanced = balanced.mapIndexed { index, opportunity ->
                if (index == 0) opportunity.copy(horizon = CreatorOpportunityHorizon.NOW) else opportunity
            }
        }
        if (balanced.size > 1 && balanced.none { it.horizon == CreatorOpportunityHorizon.NEXT }) {
            val candidateId = balanced.firstOrNull { it.horizon != CreatorOpportunityHorizon.NOW }?.id
            if (candidateId != null) {
                balanced = balanced.map { opportunity ->
                    if (opportunity.id == candidateId) opportunity.copy(horizon = CreatorOpportunityHorizon.NEXT) else opportunity
                }
            }
        }
        return balanced
    }

    private fun ideaReadiness(idea: CreatorIdea): Int = when (idea.status) {
        IdeaStatus.READY_TO_PRODUCE -> 96
        IdeaStatus.RESEARCHING -> 72
        IdeaStatus.WORTH_EXPLORING -> 58
        else -> 42
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
