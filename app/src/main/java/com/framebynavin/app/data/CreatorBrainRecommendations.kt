package com.framebynavin.app.data

enum class CreatorBrainRecommendationKind {
    LEAN_IN,
    TEST_MORE,
    WATCH,
    AVOID_FOR_NOW,
    REFRESH_EVIDENCE,
}

data class CreatorBrainRecommendation(
    val id: String,
    val kind: CreatorBrainRecommendationKind,
    val title: String,
    val body: String,
    val confidence: Int,
    val patternIds: List<String>,
    val taskId: String = "",
)

/**
 * Alpha 1.4D turns qualified Creator Brain memory into explainable creator guidance.
 *
 * Guidance is still deterministic and evidence-backed. It never invents a topic, never treats a
 * single upload as a rule, and never replaces the Opportunity Engine. Project-specific guidance is
 * emitted only when an active project matches a qualified, creator-specific Brain pattern. Global
 * guidance can lean into proven memory, suggest a controlled test for emerging memory, warn about a
 * weakening/caution pattern, or ask for a fresh retest when formerly useful evidence has gone stale.
 */
object CreatorBrainRecommendationEngine {
    fun build(
        brain: CreatorBrainSnapshot,
        tasks: List<CreatorTask>,
    ): List<CreatorBrainRecommendation> {
        if (brain.patterns.isEmpty()) return emptyList()

        val recommendations = mutableListOf<ScoredRecommendation>()
        val projectPatternIds = mutableSetOf<String>()

        tasks.asSequence()
            .filter(::isActiveProject)
            .forEach { task ->
                val match = CreatorBrainEngine.matchForTask(task, brain)
                val specificPatterns = match.patterns.filter(::isActionablePattern)
                if (specificPatterns.isEmpty()) return@forEach

                val guidance = projectGuidance(task, match.rankingDelta, specificPatterns) ?: return@forEach
                projectPatternIds += guidance.patternIds
                recommendations += ScoredRecommendation(
                    recommendation = guidance,
                    priority = 110 + kotlin.math.abs(match.rankingDelta),
                )
            }

        brain.patterns
            .asSequence()
            .filter(::isActionablePattern)
            .filter { it.id !in projectPatternIds }
            .mapNotNull(::patternGuidance)
            .forEach { guidance ->
                recommendations += ScoredRecommendation(
                    recommendation = guidance,
                    priority = globalPriority(guidance.kind),
                )
            }

        return recommendations
            .sortedWith(
                compareByDescending<ScoredRecommendation> { it.priority }
                    .thenByDescending { it.recommendation.confidence }
                    .thenBy { it.recommendation.title }
            )
            .map { it.recommendation }
            .distinctBy { it.id }
            .take(MAX_GUIDANCE)
    }

    private fun projectGuidance(
        task: CreatorTask,
        rankingDelta: Int,
        patterns: List<CreatorBrainPattern>,
    ): CreatorBrainRecommendation? {
        val strongest = patterns.maxByOrNull { kotlin.math.abs(it.rankingDelta) } ?: return null
        val weakening = patterns.any { it.trajectory == CreatorBrainTrajectory.WEAKENING }
        val aging = patterns.any { it.freshness == CreatorBrainFreshness.AGING }
        val labels = patterns.take(2).joinToString(" + ") { cleanLabel(it.label) }
        val evaluated = patterns.maxOf { it.evaluatedCount }

        val kind = when {
            rankingDelta <= -4 -> CreatorBrainRecommendationKind.AVOID_FOR_NOW
            weakening -> CreatorBrainRecommendationKind.WATCH
            rankingDelta >= 7 && patterns.any { it.state == CreatorBrainPatternState.PROVEN } && !aging -> CreatorBrainRecommendationKind.LEAN_IN
            rankingDelta >= 4 -> CreatorBrainRecommendationKind.TEST_MORE
            else -> return null
        }

        val title = when (kind) {
            CreatorBrainRecommendationKind.LEAN_IN -> "Strong Brain fit · ${task.title}"
            CreatorBrainRecommendationKind.TEST_MORE -> "Promising fit · ${task.title}"
            CreatorBrainRecommendationKind.WATCH -> "Refresh the recipe · ${task.title}"
            CreatorBrainRecommendationKind.AVOID_FOR_NOW -> "Rework before repeating · ${task.title}"
            CreatorBrainRecommendationKind.REFRESH_EVIDENCE -> "Retest · ${task.title}"
        }
        val body = when (kind) {
            CreatorBrainRecommendationKind.LEAN_IN ->
                "This active project matches $labels, backed by $evaluated evaluated outcomes. Use that pattern as a tailwind, not a guarantee; keep the new video distinct."
            CreatorBrainRecommendationKind.TEST_MORE ->
                "This project matches $labels, but the Brain still needs more proof. Keep the idea and test the pattern deliberately rather than treating it as a formula."
            CreatorBrainRecommendationKind.WATCH ->
                "This project matches $labels, but the memory is aging or recent outcomes are weaker than earlier ones. Keep the core idea, refresh the hook or angle, then test again."
            CreatorBrainRecommendationKind.AVOID_FOR_NOW ->
                "This project overlaps with $labels, where repeated evaluated outcomes currently carry caution. Rework the hook, angle or format before repeating the same recipe."
            CreatorBrainRecommendationKind.REFRESH_EVIDENCE ->
                "The matching pattern needs fresh evidence before it should influence this project again."
        }

        return CreatorBrainRecommendation(
            id = "brain-project-${task.id}-${strongest.id}",
            kind = kind,
            title = title,
            body = body,
            confidence = guidanceConfidence(strongest),
            patternIds = patterns.map { it.id },
            taskId = task.id,
        )
    }

    private fun patternGuidance(pattern: CreatorBrainPattern): CreatorBrainRecommendation? {
        if (pattern.state == CreatorBrainPatternState.LEARNING) return null

        val kind = when {
            pattern.freshness == CreatorBrainFreshness.STALE -> CreatorBrainRecommendationKind.REFRESH_EVIDENCE
            pattern.state == CreatorBrainPatternState.CAUTION -> CreatorBrainRecommendationKind.AVOID_FOR_NOW
            pattern.trajectory == CreatorBrainTrajectory.WEAKENING -> CreatorBrainRecommendationKind.WATCH
            pattern.freshness == CreatorBrainFreshness.AGING -> CreatorBrainRecommendationKind.WATCH
            pattern.state == CreatorBrainPatternState.PROVEN && pattern.rankingDelta > 0 -> CreatorBrainRecommendationKind.LEAN_IN
            pattern.state == CreatorBrainPatternState.EMERGING && pattern.rankingDelta > 0 -> CreatorBrainRecommendationKind.TEST_MORE
            else -> return null
        }

        val clean = cleanLabel(pattern.label)
        val title = when (kind) {
            CreatorBrainRecommendationKind.LEAN_IN -> "Lean into $clean"
            CreatorBrainRecommendationKind.TEST_MORE -> "Test $clean again"
            CreatorBrainRecommendationKind.WATCH -> when (pattern.trajectory) {
                CreatorBrainTrajectory.WEAKENING -> "$clean is weakening"
                else -> "Revalidate $clean"
            }
            CreatorBrainRecommendationKind.AVOID_FOR_NOW -> "Avoid repeating $clean unchanged"
            CreatorBrainRecommendationKind.REFRESH_EVIDENCE -> "Old memory · retest $clean"
        }
        val body = when (kind) {
            CreatorBrainRecommendationKind.LEAN_IN ->
                "${pattern.positiveCount}/${pattern.evaluatedCount} evaluated outcomes were positive and the evidence is ${pattern.freshness.name.lowercase()}. Reuse the underlying pattern, not the exact execution."
            CreatorBrainRecommendationKind.TEST_MORE ->
                "${pattern.positiveCount}/${pattern.evaluatedCount} evaluated outcomes are positive so far. This is emerging memory, so one more deliberate test is more useful than calling it a rule."
            CreatorBrainRecommendationKind.WATCH -> when (pattern.trajectory) {
                CreatorBrainTrajectory.WEAKENING ->
                    "Recent outcomes are weaker than the previous run for this pattern. Keep it available, but change the hook, angle or execution before leaning on it again."
                else ->
                    "This pattern still has history, but its evidence is aging. Treat it as a hypothesis until a fresh upload confirms it still works."
            }
            CreatorBrainRecommendationKind.AVOID_FOR_NOW ->
                "Only ${pattern.positiveCount}/${pattern.evaluatedCount} evaluated outcomes were positive. Do not copy the same recipe right now; change at least one major creative variable first."
            CreatorBrainRecommendationKind.REFRESH_EVIDENCE ->
                "This pattern used to have enough evidence to matter, but its latest evaluated result is stale. It is preserved as memory and no longer changes ranking until fresh evidence returns."
        }

        return CreatorBrainRecommendation(
            id = "brain-pattern-${pattern.id}",
            kind = kind,
            title = title,
            body = body,
            confidence = guidanceConfidence(pattern),
            patternIds = listOf(pattern.id),
        )
    }

    private fun guidanceConfidence(pattern: CreatorBrainPattern): Int {
        val stateBase = when (pattern.state) {
            CreatorBrainPatternState.PROVEN -> 82
            CreatorBrainPatternState.CAUTION -> 80
            CreatorBrainPatternState.EMERGING -> 68
            CreatorBrainPatternState.LEARNING -> 50
        }
        val evidenceBoost = (pattern.evaluatedCount - 3).coerceIn(0, 5) * 2
        val freshnessAdjustment = when (pattern.freshness) {
            CreatorBrainFreshness.FRESH -> 4
            CreatorBrainFreshness.AGING -> -6
            CreatorBrainFreshness.STALE -> -14
        }
        return (stateBase + evidenceBoost + freshnessAdjustment).coerceIn(50, 94)
    }

    private fun isActionablePattern(pattern: CreatorBrainPattern): Boolean = when (pattern.dimension) {
        CreatorBrainDimension.PLATFORM,
        CreatorBrainDimension.CREATOR_MODE -> false
        else -> true
    }

    private fun isActiveProject(task: CreatorTask): Boolean =
        task.archivedAtMillis <= 0L && task.status != TaskStatus.DONE && task.status != TaskStatus.SKIPPED

    private fun cleanLabel(label: String): String = label
        .substringAfter(" · ", label)
        .replaceFirstChar { if (it.isUpperCase()) it.lowercase() else it.toString() }

    private fun globalPriority(kind: CreatorBrainRecommendationKind): Int = when (kind) {
        CreatorBrainRecommendationKind.AVOID_FOR_NOW -> 94
        CreatorBrainRecommendationKind.LEAN_IN -> 92
        CreatorBrainRecommendationKind.WATCH -> 88
        CreatorBrainRecommendationKind.TEST_MORE -> 82
        CreatorBrainRecommendationKind.REFRESH_EVIDENCE -> 70
    }

    private data class ScoredRecommendation(
        val recommendation: CreatorBrainRecommendation,
        val priority: Int,
    )

    private const val MAX_GUIDANCE = 4
}
