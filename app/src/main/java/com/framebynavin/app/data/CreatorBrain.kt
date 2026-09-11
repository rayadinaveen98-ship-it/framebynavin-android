package com.framebynavin.app.data

enum class CreatorBrainDimension {
    CREATOR_MODE,
    ARCHETYPE,
    CONTENT_TYPE,
    PRODUCTION_STYLE,
    PLATFORM,
    DELIVERY_FORMAT,
    HOOK_STYLE,
    ANGLE_STYLE,
    TOPIC,
    SERIES,
    COMBINATION,
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
    val combinationPatternCount: Int get() = patterns.count { it.dimension == CreatorBrainDimension.COMBINATION }
}

data class CreatorBrainMatch(
    val rankingDelta: Int = 0,
    val patterns: List<CreatorBrainPattern> = emptyList(),
)

/**
 * Alpha 1.4B Creator Brain combinations.
 *
 * The Brain still learns only from recommendation outcomes that reached real YouTube evaluation,
 * but it can now remember repeatable combinations instead of treating every trait in isolation.
 * Structured Content DNA, hook family, angle family and explicitly identifiable topic/series memory
 * can form combinations such as archetype + hook, format + hook or angle + hook.
 *
 * Topic and series are intentionally conservative: they are read only from explicit `topic:` /
 * `series:` metadata, hashtags, or clear Episode/Part/Chapter naming. Arbitrary video titles are not
 * silently converted into topic rules. Three evaluated outcomes can become EMERGING; four are still
 * required for PROVEN or CAUTION. Matching remains capped so Brain history cannot overpower current
 * urgency, readiness, live platform momentum, the Creator Playbook or optional Gemini evidence.
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
                    .thenByDescending { it.dimension == CreatorBrainDimension.COMBINATION }
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
                compareByDescending<CreatorBrainPattern> { it.dimension == CreatorBrainDimension.COMBINATION }
                    .thenByDescending { kotlin.math.abs(it.rankingDelta) }
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

    internal fun classifyAngleStyle(angle: String): String {
        val clean = angle.trim().lowercase()
        if (clean.isBlank()) return ""
        return when {
            Regex("\\b(vs|versus|compare|comparison|better than|worse than|contrast)\\b").containsMatchIn(clean) -> "comparison"
            Regex("\\b(cinematography|camera|shot|frame|lighting|editing|screenplay|sound|bgm|performance|blocking|composition|craft)\\b").containsMatchIn(clean) -> "craft"
            Regex("\\b(emotion|emotional|love|fear|grief|pain|sad|sadness|relationship|mother|father|loss)\\b").containsMatchIn(clean) -> "emotional"
            Regex("\\b(explain|explained|meaning|ending|timeline|understand|breakdown|how|why)\\b").containsMatchIn(clean) -> "explanatory"
            Regex("\\b(review|my take|opinion|overrated|underrated|deserved|worth watching)\\b").containsMatchIn(clean) -> "opinion"
            else -> "focused"
        }
    }

    internal fun extractTopicKey(task: CreatorTask): String =
        explicitNamedValue("topic", listOf(task.notes, task.workspace.angle))

    internal fun extractSeriesKey(task: CreatorTask): String {
        explicitNamedValue("series", listOf(task.notes, task.workspace.angle)).takeIf { it.isNotBlank() }?.let { return it }

        Regex("#([A-Za-z][A-Za-z0-9_]{2,40})")
            .find(task.title)
            ?.groupValues
            ?.getOrNull(1)
            ?.let(::normalize)
            ?.takeIf { it.isNotBlank() }
            ?.let { return it }

        val episode = Regex("(?i)\\b(?:episode|ep\\.?|part|chapter)\\s*#?\\d+\\b").find(task.title) ?: return ""
        val stem = task.title
            .removeRange(episode.range)
            .replace(Regex("[|:–—-]+"), " ")
            .replace(Regex("\\s+"), " ")
            .trim()
        return stem.takeIf { it.length in 3..60 }?.let(::normalize).orEmpty()
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
        val archetype = dna.archetypeId.takeIf { it.isNotBlank() }?.let(::normalize).orEmpty()
        val contentType = task.contentType.takeIf { it.isNotBlank() }?.let(::normalize).orEmpty()
        val platform = dna.platform.takeIf { it.isNotBlank() }?.let(::normalize).orEmpty()
        val format = dna.deliveryFormat.takeIf { it.isNotBlank() }?.let(::normalize).orEmpty()
        val hookStyle = classifyHookStyle(task.workspace.hook)
        val angleStyle = classifyAngleStyle(task.workspace.angle)
        val topic = extractTopicKey(task)
        val series = extractSeriesKey(task)
        val styles = dna.productionStyles
            .map(::normalize)
            .filter { it.isNotBlank() }
            .distinct()
            .take(2)

        val signals = buildList {
            dna.creatorModeId.takeIf { it.isNotBlank() }?.let {
                add(BrainSignal(CreatorBrainDimension.CREATOR_MODE, normalize(it), "Creator mode · ${pretty(it)}"))
            }
            if (archetype.isNotBlank()) {
                add(BrainSignal(CreatorBrainDimension.ARCHETYPE, archetype, "Archetype · ${pretty(archetype)}"))
            }
            if (contentType.isNotBlank()) {
                add(BrainSignal(CreatorBrainDimension.CONTENT_TYPE, contentType, "Content type · ${pretty(contentType)}"))
            }
            styles.forEach { style ->
                add(BrainSignal(CreatorBrainDimension.PRODUCTION_STYLE, style, "Style · ${pretty(style)}"))
            }
            if (platform.isNotBlank()) {
                add(BrainSignal(CreatorBrainDimension.PLATFORM, platform, "Platform · ${pretty(platform)}"))
            }
            if (format.isNotBlank()) {
                add(BrainSignal(CreatorBrainDimension.DELIVERY_FORMAT, format, "Format · ${pretty(format)}"))
            }
            if (hookStyle.isNotBlank()) {
                add(BrainSignal(CreatorBrainDimension.HOOK_STYLE, hookStyle, "Hook · ${pretty(hookStyle)}"))
            }
            if (angleStyle.isNotBlank()) {
                add(BrainSignal(CreatorBrainDimension.ANGLE_STYLE, angleStyle, "Angle · ${pretty(angleStyle)}"))
            }
            if (topic.isNotBlank()) {
                add(BrainSignal(CreatorBrainDimension.TOPIC, topic, "Topic · ${pretty(topic)}"))
            }
            if (series.isNotBlank()) {
                add(BrainSignal(CreatorBrainDimension.SERIES, series, "Series · ${pretty(series)}"))
            }

            addCombination("archetype_hook", archetype, hookStyle, "${pretty(archetype)} + ${pretty(hookStyle)} hook")
            addCombination("format_hook", format, hookStyle, "${pretty(format)} + ${pretty(hookStyle)} hook")
            addCombination("angle_hook", angleStyle, hookStyle, "${pretty(angleStyle)} angle + ${pretty(hookStyle)} hook")
            addCombination("archetype_format", archetype, format, "${pretty(archetype)} + ${pretty(format)}")
            styles.forEach { style ->
                addCombination("archetype_style", archetype, style, "${pretty(archetype)} + ${pretty(style)} style")
            }
            addCombination("topic_hook", topic, hookStyle, "${pretty(topic)} topic + ${pretty(hookStyle)} hook")
            addCombination("series_hook", series, hookStyle, "${pretty(series)} series + ${pretty(hookStyle)} hook")
        }
        return signals.distinctBy { it.dimension to it.key }
    }

    private fun MutableList<BrainSignal>.addCombination(
        family: String,
        left: String,
        right: String,
        label: String,
    ) {
        if (left.isBlank() || right.isBlank() || left == right) return
        add(
            BrainSignal(
                dimension = CreatorBrainDimension.COMBINATION,
                key = "$family:$left+$right",
                label = "Combination · $label",
            )
        )
    }

    private fun explicitNamedValue(name: String, sources: List<String>): String {
        val regex = Regex("(?i)(?:^|[|;\\n])\\s*$name\\s*[:=]\\s*([^|;\\n]{2,60})")
        return sources.firstNotNullOfOrNull { source ->
            regex.find(source)?.groupValues?.getOrNull(1)?.trim()?.takeIf { it.isNotBlank() }
        }?.let(::normalize).orEmpty()
    }

    private fun normalize(value: String): String = value
        .trim()
        .lowercase()
        .replace(Regex("[^a-z0-9]+"), "_")
        .trim('_')
        .take(64)

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
