package com.framebynavin.app.data

enum class CreatorBrainDimension {
    CREATOR_MODE,
    ARCHETYPE,
    CONTENT_TYPE,
    PRODUCTION_STYLE,
    PLATFORM,
    DELIVERY_FORMAT,
    HOOK_STYLE,
    WORKFLOW_PACE,
}

enum class CreatorBrainPatternState {
    LEARNING,
    EMERGING,
    PROVEN,
    CAUTION,
}

data class CreatorBrainPattern(
    val dimension: CreatorBrainDimension,
    val key: String,
    val label: String,
    val evaluatedCount: Int,
    val positiveCount: Int,
    val strongCount: Int,
    val weakCount: Int,
    val averageBaselineMultiple: Double,
    val averageViewSharePercent: Int,
    val state: CreatorBrainPatternState,
    val rankingDelta: Int,
) {
    val positiveRatePercent: Int
        get() = if (evaluatedCount <= 0) 0 else ((positiveCount * 100.0) / evaluatedCount).toInt().coerceIn(0, 100)

    val id: String get() = "${dimension.name.lowercase()}:$key"
}

data class CreatorBrainSnapshot(
    val patterns: List<CreatorBrainPattern> = emptyList(),
) {
    val activePatterns: List<CreatorBrainPattern>
        get() = patterns.filter { it.state != CreatorBrainPatternState.LEARNING }

    val evaluatedPatternCount: Int get() = patterns.size
    val activePatternCount: Int get() = activePatterns.size
}

data class CreatorBrainMatch(
    val rankingDelta: Int = 0,
    val patterns: List<CreatorBrainPattern> = emptyList(),
)

/**
 * Alpha 1.4A Creator Brain foundation.
 *
 * This layer learns finer creator-specific patterns only from recommendation outcomes that have
 * reached real YouTube evaluation. It does not infer success from draft projects, clicks, Gemini,
 * or a single strong upload. Patterns need repeated evidence before they can influence ranking:
 * three evaluated outcomes can become EMERGING, four are required for PROVEN or CAUTION.
 *
 * Brain adjustments are deliberately smaller than current opportunity evidence and are capped when
 * multiple matching patterns overlap, so Content DNA history can inform a decision without taking
 * over live urgency, readiness, momentum or the Creator Playbook.
 */
object CreatorBrainEngine {
    fun build(
        outcomes: List<CreatorRecommendationOutcome>,
        tasks: List<CreatorTask>,
    ): CreatorBrainSnapshot {
        val tasksById = tasks.associateBy { it.id }
        val observations = mutableListOf<Observation>()

        outcomes.asSequence()
            .filter {
                it.status == CreatorRecommendationOutcomeStatus.EVALUATED &&
                    it.verdict != CreatorRecommendationVerdict.PENDING &&
                    it.taskId.isNotBlank()
            }
            .forEach { outcome ->
                val task = tasksById[outcome.taskId] ?: return@forEach
                outcomeSignals(task, outcome).forEach { signal ->
                    observations += Observation(signal, outcome)
                }
            }

        val patterns = observations
            .groupBy { it.signal.dimension to it.signal.key }
            .map { (_, group) -> buildPattern(group) }
            .sortedWith(
                compareByDescending<CreatorBrainPattern> { stateRank(it.state) }
                    .thenByDescending { it.evaluatedCount }
                    .thenByDescending { it.positiveRatePercent }
                    .thenBy { it.label }
            )

        return CreatorBrainSnapshot(patterns)
    }

    fun matchForTask(
        task: CreatorTask,
        snapshot: CreatorBrainSnapshot,
    ): CreatorBrainMatch {
        if (snapshot.activePatterns.isEmpty()) return CreatorBrainMatch()
        val candidateIds = currentSignals(task).map { it.dimension to it.key }.toSet()
        val matches = snapshot.activePatterns
            .filter { (it.dimension to it.key) in candidateIds }
            .sortedWith(
                compareByDescending<CreatorBrainPattern> { kotlin.math.abs(it.rankingDelta) }
                    .thenByDescending { it.evaluatedCount }
            )
            .take(2)

        if (matches.isEmpty()) return CreatorBrainMatch()
        val raw = matches.sumOf { it.rankingDelta }
        return CreatorBrainMatch(
            rankingDelta = raw.coerceIn(-14, 16),
            patterns = matches,
        )
    }

    internal fun classifyHookStyle(hook: String): String {
        val clean = hook.trim().lowercase()
        if (clean.isBlank()) return ""
        return when {
            clean.endsWith("?") || clean.startsWith("why ") || clean.startsWith("how ") || clean.startsWith("what ") -> "question"
            Regex("\\b(vs|versus|but|instead|however)\\b").containsMatchIn(clean) -> "contrast"
            Regex("\\b(secret|truth|never|always|best|worst|biggest|mistake)\\b").containsMatchIn(clean) -> "bold_claim"
            clean.startsWith("imagine ") || clean.startsWith("when ") || clean.startsWith("the day ") || clean.startsWith("once ") -> "narrative"
            clean.length <= 72 -> "direct"
            else -> "contextual"
        }
    }

    private fun buildPattern(group: List<Observation>): CreatorBrainPattern {
        val signal = group.first().signal
        val history = group.map { it.outcome }
        val positive = history.count {
            it.verdict == CreatorRecommendationVerdict.STRONG ||
                it.verdict == CreatorRecommendationVerdict.PROMISING
        }
        val strong = history.count { it.verdict == CreatorRecommendationVerdict.STRONG }
        val weak = history.count { it.verdict == CreatorRecommendationVerdict.WEAK }
        val count = history.size
        val positiveRate = if (count == 0) 0 else ((positive * 100.0) / count).toInt()

        val state = when {
            count >= 4 && positiveRate >= 75 -> CreatorBrainPatternState.PROVEN
            count >= 4 && positiveRate <= 25 -> CreatorBrainPatternState.CAUTION
            count >= 3 && positiveRate >= 67 -> CreatorBrainPatternState.EMERGING
            else -> CreatorBrainPatternState.LEARNING
        }
        val delta = when (state) {
            CreatorBrainPatternState.PROVEN -> (8 + (count - 4).coerceAtMost(3) + strong.coerceAtMost(2)).coerceAtMost(13)
            CreatorBrainPatternState.EMERGING -> 5
            CreatorBrainPatternState.CAUTION -> (-6 - weak.coerceAtMost(3)).coerceAtLeast(-10)
            CreatorBrainPatternState.LEARNING -> 0
        }

        return CreatorBrainPattern(
            dimension = signal.dimension,
            key = signal.key,
            label = signal.label,
            evaluatedCount = count,
            positiveCount = positive,
            strongCount = strong,
            weakCount = weak,
            averageBaselineMultiple = history.map { it.baselineMultiple }.average().takeIf { !it.isNaN() } ?: 0.0,
            averageViewSharePercent = history.map { it.viewSharePercent }.average().takeIf { !it.isNaN() }?.toInt() ?: 0,
            state = state,
            rankingDelta = delta,
        )
    }

    private fun outcomeSignals(
        task: CreatorTask,
        outcome: CreatorRecommendationOutcome,
    ): List<BrainSignal> = buildList {
        addAll(currentSignals(task))
        val elapsed = outcome.publishedAtMillis - outcome.actedAtMillis
        if (elapsed > 0L) {
            val pace = when {
                elapsed <= 3L * DAY_MILLIS -> "fast"
                elapsed <= 10L * DAY_MILLIS -> "steady"
                else -> "long_cycle"
            }
            add(
                BrainSignal(
                    dimension = CreatorBrainDimension.WORKFLOW_PACE,
                    key = pace,
                    label = "Workflow pace · ${pretty(pace)}",
                )
            )
        }
    }.distinctBy { it.dimension to it.key }

    private fun currentSignals(task: CreatorTask): List<BrainSignal> {
        val dna = CreatorContentDnaEngine.effective(task)
        val signals = buildList {
            dna.creatorModeId.takeIf { it.isNotBlank() }?.let {
                add(BrainSignal(CreatorBrainDimension.CREATOR_MODE, normalize(it), "Creator mode · ${pretty(it)}"))
            }
            dna.archetypeId.takeIf { it.isNotBlank() }?.let {
                add(BrainSignal(CreatorBrainDimension.ARCHETYPE, normalize(it), "Archetype · ${pretty(it)}"))
            }
            task.contentType.takeIf { it.isNotBlank() }?.let {
                add(BrainSignal(CreatorBrainDimension.CONTENT_TYPE, normalize(it), "Content type · ${pretty(it)}"))
            }
            dna.productionStyles.forEach { style ->
                if (style.isNotBlank()) {
                    add(BrainSignal(CreatorBrainDimension.PRODUCTION_STYLE, normalize(style), "Style · ${pretty(style)}"))
                }
            }
            dna.platform.takeIf { it.isNotBlank() }?.let {
                add(BrainSignal(CreatorBrainDimension.PLATFORM, normalize(it), "Platform · ${pretty(it)}"))
            }
            dna.deliveryFormat.takeIf { it.isNotBlank() }?.let {
                add(BrainSignal(CreatorBrainDimension.DELIVERY_FORMAT, normalize(it), "Format · ${pretty(it)}"))
            }
            classifyHookStyle(task.workspace.hook).takeIf { it.isNotBlank() }?.let {
                add(BrainSignal(CreatorBrainDimension.HOOK_STYLE, it, "Hook · ${pretty(it)}"))
            }
        }
        return signals.distinctBy { it.dimension to it.key }
    }

    private fun normalize(value: String): String = value.trim().lowercase().replace(Regex("\\s+"), "_")

    private fun pretty(value: String): String = value
        .trim()
        .replace('_', ' ')
        .replace('-', ' ')
        .replaceFirstChar { if (it.isLowerCase()) it.titlecase() else it.toString() }

    private fun stateRank(state: CreatorBrainPatternState): Int = when (state) {
        CreatorBrainPatternState.PROVEN -> 4
        CreatorBrainPatternState.EMERGING -> 3
        CreatorBrainPatternState.CAUTION -> 2
        CreatorBrainPatternState.LEARNING -> 1
    }

    private data class BrainSignal(
        val dimension: CreatorBrainDimension,
        val key: String,
        val label: String,
    )

    private data class Observation(
        val signal: BrainSignal,
        val outcome: CreatorRecommendationOutcome,
    )

    private const val DAY_MILLIS = 24L * 60L * 60L * 1000L
}
