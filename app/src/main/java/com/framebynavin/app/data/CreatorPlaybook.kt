package com.framebynavin.app.data

enum class CreatorPlaybookState {
    LEARNING,
    PROVEN,
    CAUTION,
}

data class CreatorPlaybookPattern(
    val opportunityKind: CreatorOpportunityKind,
    val evaluatedCount: Int,
    val positiveCount: Int,
    val strongCount: Int,
    val weakCount: Int,
    val averageBaselineMultiple: Double,
    val averageViewSharePercent: Int,
    val state: CreatorPlaybookState,
    val rankingDelta: Int,
) {
    val positiveRatePercent: Int
        get() = if (evaluatedCount <= 0) 0 else ((positiveCount * 100.0) / evaluatedCount).toInt().coerceIn(0, 100)

    val label: String
        get() = when (opportunityKind) {
            CreatorOpportunityKind.CHANNEL_MOMENTUM -> "channel momentum"
            CreatorOpportunityKind.VIDEO_MOMENTUM -> "video momentum follow-ups"
            CreatorOpportunityKind.MATCHED_IDEA -> "matched ideas"
            CreatorOpportunityKind.READY_IDEA -> "ready ideas"
            CreatorOpportunityKind.PROJECT_MOMENTUM -> "execution recommendations"
        }
}

data class CreatorPlaybookSnapshot(
    val patterns: List<CreatorPlaybookPattern> = emptyList(),
) {
    val activePatterns: List<CreatorPlaybookPattern>
        get() = patterns.filter { it.state != CreatorPlaybookState.LEARNING }

    fun patternFor(kind: CreatorOpportunityKind): CreatorPlaybookPattern? =
        patterns.firstOrNull { it.opportunityKind == kind }
}

/**
 * First outcome-weighted Creator Playbook.
 *
 * It deliberately learns at the opportunity-family level rather than pretending a tiny sample can
 * prove a topic, hook or creative choice. One result never changes ranking. Positive history needs
 * at least two evaluated outcomes; negative history needs at least three before it can reduce a
 * score. Adjustments are intentionally small so current urgency and live platform evidence remain
 * the dominant decision inputs.
 */
object CreatorPlaybookEngine {
    fun build(outcomes: List<CreatorRecommendationOutcome>): CreatorPlaybookSnapshot {
        val evaluated = outcomes.filter {
            it.status == CreatorRecommendationOutcomeStatus.EVALUATED &&
                it.verdict != CreatorRecommendationVerdict.PENDING
        }

        val patterns = evaluated
            .groupBy { it.opportunityKind }
            .map { (kind, history) -> buildPattern(kind, history) }
            .sortedWith(
                compareByDescending<CreatorPlaybookPattern> { it.state == CreatorPlaybookState.PROVEN }
                    .thenByDescending { it.evaluatedCount }
                    .thenByDescending { it.positiveRatePercent }
            )

        return CreatorPlaybookSnapshot(patterns)
    }

    private fun buildPattern(
        kind: CreatorOpportunityKind,
        history: List<CreatorRecommendationOutcome>,
    ): CreatorPlaybookPattern {
        val positive = history.count {
            it.verdict == CreatorRecommendationVerdict.STRONG ||
                it.verdict == CreatorRecommendationVerdict.PROMISING
        }
        val strong = history.count { it.verdict == CreatorRecommendationVerdict.STRONG }
        val weak = history.count { it.verdict == CreatorRecommendationVerdict.WEAK }
        val count = history.size
        val positiveRate = if (count == 0) 0 else ((positive * 100.0) / count).toInt()

        val state = when {
            count >= 2 && positiveRate >= 70 -> CreatorPlaybookState.PROVEN
            count >= 3 && positiveRate <= 25 -> CreatorPlaybookState.CAUTION
            else -> CreatorPlaybookState.LEARNING
        }

        val rankingDelta = when (state) {
            CreatorPlaybookState.PROVEN -> {
                val sampleBoost = ((count - 2).coerceAtMost(3) * 3)
                val strongBoost = (strong.coerceAtMost(3) * 2)
                (14 + sampleBoost + strongBoost).coerceAtMost(28)
            }
            CreatorPlaybookState.CAUTION -> {
                val weakPenalty = weak.coerceAtMost(4) * 2
                (-10 - weakPenalty).coerceAtLeast(-18)
            }
            CreatorPlaybookState.LEARNING -> 0
        }

        return CreatorPlaybookPattern(
            opportunityKind = kind,
            evaluatedCount = count,
            positiveCount = positive,
            strongCount = strong,
            weakCount = weak,
            averageBaselineMultiple = history.map { it.baselineMultiple }.average().takeIf { !it.isNaN() } ?: 0.0,
            averageViewSharePercent = history.map { it.viewSharePercent }.average().takeIf { !it.isNaN() }?.toInt() ?: 0,
            state = state,
            rankingDelta = rankingDelta,
        )
    }
}
