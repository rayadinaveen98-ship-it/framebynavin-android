package com.framebynavin.app.data

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject

enum class CreatorRecommendationOutcomeStatus {
    ACTED,
    PROJECT_CREATED,
    PUBLISHED,
    EVALUATED,
}

enum class CreatorRecommendationVerdict {
    PENDING,
    STRONG,
    PROMISING,
    MIXED,
    WEAK,
}

data class CreatorRecommendationOutcome(
    val id: String,
    val opportunityId: String,
    val opportunityKind: CreatorOpportunityKind,
    val targetKind: CreatorOpportunityTargetKind,
    val targetId: String,
    val title: String,
    val actedAtMillis: Long,
    val taskId: String = "",
    val ideaId: String = "",
    val sourceVideoId: String = "",
    val status: CreatorRecommendationOutcomeStatus = CreatorRecommendationOutcomeStatus.ACTED,
    val publishedVideoId: String = "",
    val publishedAtMillis: Long = 0L,
    val evaluatedAtMillis: Long = 0L,
    val baselineMultiple: Double = 0.0,
    val viewSharePercent: Int = 0,
    val periodViews: Long = 0L,
    val verdict: CreatorRecommendationVerdict = CreatorRecommendationVerdict.PENDING,
)

data class CreatorRecommendationLearningSummary(
    val acted: Int = 0,
    val projectsCreated: Int = 0,
    val published: Int = 0,
    val evaluated: Int = 0,
    val positive: Int = 0,
) {
    val positiveRatePercent: Int
        get() = if (evaluated <= 0) 0 else ((positive * 100.0) / evaluated).toInt().coerceIn(0, 100)
}

object CreatorRecommendationOutcomeEngine {
    fun reconcile(
        outcomes: List<CreatorRecommendationOutcome>,
        tasks: List<CreatorTask>,
        performanceSignals: List<CreatorPlatformOpportunitySignal>,
        nowMillis: Long = System.currentTimeMillis(),
    ): List<CreatorRecommendationOutcome> = outcomes.map { outcome ->
        val linkedTask = resolveTask(outcome, tasks)
        if (linkedTask == null) return@map outcome

        val projectLinked = outcome.copy(
            taskId = linkedTask.id,
            status = if (outcome.status == CreatorRecommendationOutcomeStatus.ACTED) {
                CreatorRecommendationOutcomeStatus.PROJECT_CREATED
            } else {
                outcome.status
            },
        )

        if (linkedTask.publishedAtMillis <= 0L) return@map projectLinked

        val videoId = extractYouTubeVideoId(linkedTask.publishedUrl)
        val published = projectLinked.copy(
            status = CreatorRecommendationOutcomeStatus.PUBLISHED,
            publishedVideoId = videoId,
            publishedAtMillis = linkedTask.publishedAtMillis,
        )
        if (videoId.isBlank()) return@map published

        val signal = performanceSignals.firstOrNull { it.videoId == videoId } ?: return@map published
        if (signal.periodViews <= 0L) return@map published

        published.copy(
            status = CreatorRecommendationOutcomeStatus.EVALUATED,
            evaluatedAtMillis = nowMillis,
            baselineMultiple = signal.baselineMultiple,
            viewSharePercent = signal.viewSharePercent,
            periodViews = signal.periodViews,
            verdict = verdict(signal),
        )
    }

    fun summary(outcomes: List<CreatorRecommendationOutcome>): CreatorRecommendationLearningSummary {
        val evaluated = outcomes.count { it.status == CreatorRecommendationOutcomeStatus.EVALUATED }
        return CreatorRecommendationLearningSummary(
            acted = outcomes.size,
            projectsCreated = outcomes.count {
                it.status == CreatorRecommendationOutcomeStatus.PROJECT_CREATED ||
                    it.status == CreatorRecommendationOutcomeStatus.PUBLISHED ||
                    it.status == CreatorRecommendationOutcomeStatus.EVALUATED
            },
            published = outcomes.count {
                it.status == CreatorRecommendationOutcomeStatus.PUBLISHED ||
                    it.status == CreatorRecommendationOutcomeStatus.EVALUATED
            },
            evaluated = evaluated,
            positive = outcomes.count {
                it.status == CreatorRecommendationOutcomeStatus.EVALUATED &&
                    (it.verdict == CreatorRecommendationVerdict.STRONG || it.verdict == CreatorRecommendationVerdict.PROMISING)
            },
        )
    }

    private fun resolveTask(
        outcome: CreatorRecommendationOutcome,
        tasks: List<CreatorTask>,
    ): CreatorTask? {
        if (outcome.taskId.isNotBlank()) {
            tasks.firstOrNull { it.id == outcome.taskId }?.let { return it }
        }
        if (outcome.ideaId.isNotBlank()) {
            tasks.firstOrNull {
                it.origin == CreatorTaskOrigin.IDEA_VAULT && it.sourceRefId == outcome.ideaId
            }?.let { return it }
        }
        if (outcome.targetKind == CreatorOpportunityTargetKind.PROJECT && outcome.targetId.isNotBlank()) {
            return tasks.firstOrNull { it.id == outcome.targetId }
        }
        return null
    }

    private fun verdict(signal: CreatorPlatformOpportunitySignal): CreatorRecommendationVerdict = when {
        signal.baselineMultiple >= 1.35 || signal.viewSharePercent >= 35 -> CreatorRecommendationVerdict.STRONG
        signal.baselineMultiple >= 1.05 || signal.viewSharePercent >= 20 -> CreatorRecommendationVerdict.PROMISING
        signal.baselineMultiple >= 0.85 -> CreatorRecommendationVerdict.MIXED
        else -> CreatorRecommendationVerdict.WEAK
    }

    internal fun extractYouTubeVideoId(url: String): String {
        val clean = url.trim()
        if (clean.isBlank()) return ""
        val patterns = listOf(
            Regex("(?:youtube\\.com/watch\\?[^#]*v=)([A-Za-z0-9_-]{6,})", RegexOption.IGNORE_CASE),
            Regex("(?:youtu\\.be/)([A-Za-z0-9_-]{6,})", RegexOption.IGNORE_CASE),
            Regex("(?:youtube\\.com/shorts/)([A-Za-z0-9_-]{6,})", RegexOption.IGNORE_CASE),
            Regex("(?:youtube\\.com/live/)([A-Za-z0-9_-]{6,})", RegexOption.IGNORE_CASE),
        )
        return patterns.firstNotNullOfOrNull { it.find(clean)?.groupValues?.getOrNull(1) }.orEmpty()
    }
}

class CreatorRecommendationOutcomeStore(context: Context) {
    private val prefs = context.applicationContext.getSharedPreferences(PREFS, Context.MODE_PRIVATE)

    fun load(): List<CreatorRecommendationOutcome> {
        val raw = prefs.getString(KEY, null) ?: return emptyList()
        return runCatching {
            val array = JSONArray(raw)
            buildList {
                for (index in 0 until array.length()) {
                    val json = array.optJSONObject(index) ?: continue
                    decode(json)?.let(::add)
                }
            }
        }.getOrDefault(emptyList())
    }

    fun recordAction(
        opportunity: CreatorOpportunity,
        nowMillis: Long = System.currentTimeMillis(),
    ): CreatorRecommendationOutcome {
        val current = load().toMutableList()
        val duplicate = current.lastOrNull {
            it.opportunityId == opportunity.id &&
                nowMillis - it.actedAtMillis in 0..DUPLICATE_WINDOW_MILLIS
        }
        if (duplicate != null) return duplicate

        val outcome = CreatorRecommendationOutcome(
            id = "${opportunity.id}-$nowMillis",
            opportunityId = opportunity.id,
            opportunityKind = opportunity.kind,
            targetKind = opportunity.targetKind,
            targetId = opportunity.targetId,
            title = opportunity.title,
            actedAtMillis = nowMillis,
            taskId = opportunity.taskId.orEmpty(),
            ideaId = opportunity.ideaId.orEmpty(),
            sourceVideoId = opportunity.videoId.orEmpty(),
        )
        current += outcome
        save(current.takeLast(MAX_RECORDS))
        return outcome
    }

    fun reconcile(
        tasks: List<CreatorTask>,
        performanceSignals: List<CreatorPlatformOpportunitySignal>,
        nowMillis: Long = System.currentTimeMillis(),
    ): List<CreatorRecommendationOutcome> {
        val before = load()
        val after = CreatorRecommendationOutcomeEngine.reconcile(before, tasks, performanceSignals, nowMillis)
        if (after != before) save(after)
        return after
    }

    private fun save(outcomes: List<CreatorRecommendationOutcome>) {
        val array = JSONArray()
        outcomes.forEach { array.put(encode(it)) }
        prefs.edit().putString(KEY, array.toString()).apply()
    }

    private fun encode(outcome: CreatorRecommendationOutcome): JSONObject = JSONObject()
        .put("id", outcome.id)
        .put("opportunityId", outcome.opportunityId)
        .put("opportunityKind", outcome.opportunityKind.name)
        .put("targetKind", outcome.targetKind.name)
        .put("targetId", outcome.targetId)
        .put("title", outcome.title)
        .put("actedAtMillis", outcome.actedAtMillis)
        .put("taskId", outcome.taskId)
        .put("ideaId", outcome.ideaId)
        .put("sourceVideoId", outcome.sourceVideoId)
        .put("status", outcome.status.name)
        .put("publishedVideoId", outcome.publishedVideoId)
        .put("publishedAtMillis", outcome.publishedAtMillis)
        .put("evaluatedAtMillis", outcome.evaluatedAtMillis)
        .put("baselineMultiple", outcome.baselineMultiple)
        .put("viewSharePercent", outcome.viewSharePercent)
        .put("periodViews", outcome.periodViews)
        .put("verdict", outcome.verdict.name)

    private fun decode(json: JSONObject): CreatorRecommendationOutcome? = runCatching {
        CreatorRecommendationOutcome(
            id = json.getString("id"),
            opportunityId = json.getString("opportunityId"),
            opportunityKind = enumValueOf(json.getString("opportunityKind")),
            targetKind = enumValueOf(json.getString("targetKind")),
            targetId = json.optString("targetId"),
            title = json.optString("title"),
            actedAtMillis = json.optLong("actedAtMillis"),
            taskId = json.optString("taskId"),
            ideaId = json.optString("ideaId"),
            sourceVideoId = json.optString("sourceVideoId"),
            status = enumValueOf(json.optString("status", CreatorRecommendationOutcomeStatus.ACTED.name)),
            publishedVideoId = json.optString("publishedVideoId"),
            publishedAtMillis = json.optLong("publishedAtMillis"),
            evaluatedAtMillis = json.optLong("evaluatedAtMillis"),
            baselineMultiple = json.optDouble("baselineMultiple", 0.0),
            viewSharePercent = json.optInt("viewSharePercent", 0),
            periodViews = json.optLong("periodViews", 0L),
            verdict = enumValueOf(json.optString("verdict", CreatorRecommendationVerdict.PENDING.name)),
        )
    }.getOrNull()

    private companion object {
        const val PREFS = "creator_recommendation_outcomes_v1"
        const val KEY = "outcomes"
        const val MAX_RECORDS = 120
        const val DUPLICATE_WINDOW_MILLIS = 12L * 60L * 60L * 1000L
    }
}
