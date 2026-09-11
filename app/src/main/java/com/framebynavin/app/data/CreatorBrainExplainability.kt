package com.framebynavin.app.data

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject

enum class CreatorBrainLearningControl {
    ACTIVE,
    PAUSED,
    RETIRED,
}

data class CreatorBrainTeachingEvidence(
    val outcomeId: String,
    val taskId: String,
    val title: String,
    val videoId: String,
    val verdict: CreatorRecommendationVerdict,
    val baselineMultiple: Double,
    val viewSharePercent: Int,
    val evaluatedAtMillis: Long,
)

data class CreatorBrainPatternExplanation(
    val patternId: String,
    val label: String,
    val state: CreatorBrainPatternState,
    val sampleSize: Int,
    val positiveCount: Int,
    val positiveRatePercent: Int,
    val freshness: CreatorBrainFreshness,
    val trajectory: CreatorBrainTrajectory,
    val rawRankingDelta: Int,
    val effectiveRankingDelta: Int,
    val control: CreatorBrainLearningControl,
    val why: String,
    val freshnessReason: String,
    val trajectoryReason: String,
    val evidence: List<CreatorBrainTeachingEvidence>,
)

data class CreatorBrainExplainabilitySnapshot(
    val explanations: List<CreatorBrainPatternExplanation> = emptyList(),
    val activeCount: Int = 0,
    val pausedCount: Int = 0,
    val retiredCount: Int = 0,
)

/**
 * Alpha 1.4E user controls for Creator Brain learning.
 *
 * PAUSED and RETIRED never delete outcomes or rewrite historical evidence. They only stop that
 * pattern from affecting current ranking and guidance. Restoring a pattern returns the original
 * time-aware Brain memory, including freshness and trajectory safeguards.
 */
object CreatorBrainLearningControlEngine {
    fun apply(
        snapshot: CreatorBrainSnapshot,
        controls: Map<String, CreatorBrainLearningControl>,
    ): CreatorBrainSnapshot {
        if (controls.isEmpty()) return snapshot
        return snapshot.copy(
            patterns = snapshot.patterns.filter { pattern ->
                controlFor(pattern.id, controls) == CreatorBrainLearningControl.ACTIVE
            },
        )
    }

    fun controlFor(
        patternId: String,
        controls: Map<String, CreatorBrainLearningControl>,
    ): CreatorBrainLearningControl = controls[patternId] ?: CreatorBrainLearningControl.ACTIVE
}

class CreatorBrainLearningControlStore(context: Context) {
    private val prefs = context.applicationContext.getSharedPreferences(PREFS, Context.MODE_PRIVATE)

    fun load(): Map<String, CreatorBrainLearningControl> {
        val raw = prefs.getString(KEY, null) ?: return emptyMap()
        return runCatching {
            val array = JSONArray(raw)
            buildMap {
                for (index in 0 until array.length()) {
                    val item = array.optJSONObject(index) ?: continue
                    val patternId = item.optString("patternId").trim()
                    val control = runCatching {
                        enumValueOf<CreatorBrainLearningControl>(item.optString("control"))
                    }.getOrNull() ?: continue
                    if (patternId.isNotBlank() && control != CreatorBrainLearningControl.ACTIVE) {
                        put(patternId, control)
                    }
                }
            }
        }.getOrDefault(emptyMap())
    }

    fun set(patternId: String, control: CreatorBrainLearningControl) {
        if (patternId.isBlank()) return
        val current = load().toMutableMap()
        if (control == CreatorBrainLearningControl.ACTIVE) {
            current.remove(patternId)
        } else {
            current[patternId] = control
        }
        save(current)
    }

    private fun save(controls: Map<String, CreatorBrainLearningControl>) {
        val array = JSONArray()
        controls.toSortedMap().forEach { (patternId, control) ->
            array.put(
                JSONObject()
                    .put("patternId", patternId)
                    .put("control", control.name),
            )
        }
        prefs.edit().putString(KEY, array.toString()).apply()
    }

    private companion object {
        const val PREFS = "creator_brain_learning_controls_v1"
        const val KEY = "controls"
    }
}

/**
 * Builds an auditable explanation for every qualified Brain conclusion.
 *
 * Evidence is traced back to the evaluated recommendation outcomes that actually generated the
 * pattern. The engine intentionally reconstructs the pattern signal for each outcome rather than
 * guessing from titles. This keeps sample size, source video/project, verdict, freshness and
 * trajectory visible and deterministic.
 */
object CreatorBrainExplainabilityEngine {
    fun build(
        rawBrain: CreatorBrainSnapshot,
        outcomes: List<CreatorRecommendationOutcome>,
        tasks: List<CreatorTask>,
        controls: Map<String, CreatorBrainLearningControl> = emptyMap(),
        nowMillis: Long = System.currentTimeMillis(),
    ): CreatorBrainExplainabilitySnapshot {
        if (rawBrain.patterns.isEmpty()) return CreatorBrainExplainabilitySnapshot()
        val tasksById = tasks.associateBy { it.id }

        val explanations = rawBrain.patterns
            .asSequence()
            .filter { pattern ->
                pattern.state != CreatorBrainPatternState.LEARNING ||
                    CreatorBrainLearningControlEngine.controlFor(pattern.id, controls) != CreatorBrainLearningControl.ACTIVE
            }
            .map { pattern ->
                val control = CreatorBrainLearningControlEngine.controlFor(pattern.id, controls)
                val evidence = outcomes.asSequence()
                    .filter {
                        it.status == CreatorRecommendationOutcomeStatus.EVALUATED &&
                            it.verdict != CreatorRecommendationVerdict.PENDING &&
                            it.taskId.isNotBlank()
                    }
                    .mapNotNull { outcome ->
                        val task = tasksById[outcome.taskId] ?: return@mapNotNull null
                        if (!teachesPattern(pattern.id, outcome, task, nowMillis)) return@mapNotNull null
                        CreatorBrainTeachingEvidence(
                            outcomeId = outcome.id,
                            taskId = task.id,
                            title = task.title,
                            videoId = outcome.publishedVideoId.ifBlank {
                                CreatorRecommendationOutcomeEngine.extractYouTubeVideoId(task.publishedUrl)
                                    .ifBlank { outcome.sourceVideoId }
                            },
                            verdict = outcome.verdict,
                            baselineMultiple = outcome.baselineMultiple,
                            viewSharePercent = outcome.viewSharePercent,
                            evaluatedAtMillis = outcome.evaluatedAtMillis,
                        )
                    }
                    .sortedByDescending { it.evaluatedAtMillis }
                    .take(MAX_EVIDENCE_PER_PATTERN)
                    .toList()

                CreatorBrainPatternExplanation(
                    patternId = pattern.id,
                    label = pattern.label,
                    state = pattern.state,
                    sampleSize = pattern.evaluatedCount,
                    positiveCount = pattern.positiveCount,
                    positiveRatePercent = pattern.positiveRatePercent,
                    freshness = pattern.freshness,
                    trajectory = pattern.trajectory,
                    rawRankingDelta = pattern.rankingDelta,
                    effectiveRankingDelta = if (control == CreatorBrainLearningControl.ACTIVE) pattern.rankingDelta else 0,
                    control = control,
                    why = conclusionReason(pattern, control),
                    freshnessReason = freshnessReason(pattern),
                    trajectoryReason = trajectoryReason(pattern),
                    evidence = evidence,
                )
            }
            .sortedWith(
                compareByDescending<CreatorBrainPatternExplanation> { it.control != CreatorBrainLearningControl.ACTIVE }
                    .thenByDescending { stateRank(it.state) }
                    .thenByDescending { kotlin.math.abs(it.rawRankingDelta) }
                    .thenByDescending { it.sampleSize }
                    .thenBy { it.label }
            )
            .toList()

        return CreatorBrainExplainabilitySnapshot(
            explanations = explanations,
            activeCount = explanations.count { it.control == CreatorBrainLearningControl.ACTIVE },
            pausedCount = explanations.count { it.control == CreatorBrainLearningControl.PAUSED },
            retiredCount = explanations.count { it.control == CreatorBrainLearningControl.RETIRED },
        )
    }

    private fun teachesPattern(
        patternId: String,
        outcome: CreatorRecommendationOutcome,
        task: CreatorTask,
        nowMillis: Long,
    ): Boolean = CreatorBrainEngine
        .build(listOf(outcome), listOf(task), nowMillis)
        .patterns
        .any { it.id == patternId }

    private fun conclusionReason(
        pattern: CreatorBrainPattern,
        control: CreatorBrainLearningControl,
    ): String {
        val threshold = when (pattern.state) {
            CreatorBrainPatternState.PROVEN ->
                "Proven requires at least 4 evaluated outcomes and at least 75% positive."
            CreatorBrainPatternState.CAUTION ->
                "Caution requires at least 4 evaluated outcomes and at most 25% positive."
            CreatorBrainPatternState.EMERGING ->
                "Emerging requires at least 3 evaluated outcomes and at least 67% positive."
            CreatorBrainPatternState.LEARNING ->
                "Learning memory has not crossed a rule threshold yet."
        }
        val controlNote = when (control) {
            CreatorBrainLearningControl.ACTIVE -> ""
            CreatorBrainLearningControl.PAUSED -> " You paused this pattern, so its effective ranking influence is 0."
            CreatorBrainLearningControl.RETIRED -> " You retired this conclusion, so it stays in history but its effective ranking influence is 0."
        }
        return "${pattern.positiveCount}/${pattern.evaluatedCount} outcomes were positive (${pattern.positiveRatePercent}%). $threshold$controlNote"
    }

    private fun freshnessReason(pattern: CreatorBrainPattern): String = when (pattern.freshness) {
        CreatorBrainFreshness.FRESH ->
            "Fresh evidence: the latest evaluated result is within 45 days, so normal time-aware influence is allowed."
        CreatorBrainFreshness.AGING ->
            "Aging evidence: the latest result is 46–120 days old, so ranking influence is automatically reduced."
        CreatorBrainFreshness.STALE ->
            "Stale evidence: the latest result is older than 120 days, so this pattern cannot change ranking until fresh evidence returns."
    }

    private fun trajectoryReason(pattern: CreatorBrainPattern): String = when (pattern.trajectory) {
        CreatorBrainTrajectory.INSUFFICIENT ->
            "Trajectory needs at least 6 evaluated outcomes before recent performance is compared with the previous run."
        CreatorBrainTrajectory.STABLE ->
            "Trajectory is stable: the latest three outcomes are broadly consistent with the previous three."
        CreatorBrainTrajectory.IMPROVING ->
            "Trajectory is improving: the latest three evaluated outcomes are stronger than the previous three."
        CreatorBrainTrajectory.WEAKENING ->
            "Trajectory is weakening: the latest three evaluated outcomes are weaker than the previous three."
    }

    private fun stateRank(state: CreatorBrainPatternState): Int = when (state) {
        CreatorBrainPatternState.PROVEN -> 4
        CreatorBrainPatternState.CAUTION -> 3
        CreatorBrainPatternState.EMERGING -> 2
        CreatorBrainPatternState.LEARNING -> 1
    }

    private const val MAX_EVIDENCE_PER_PATTERN = 6
}
