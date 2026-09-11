package com.framebynavin.app.data

import kotlin.math.abs

enum class CreatorCreativeFeatureKind {
    HOOK_STRUCTURE,
    TITLE_STRUCTURE,
    SCRIPT_SIZE,
    ARCHETYPE,
    PRODUCTION_STYLE,
    DELIVERY_FORMAT,
}

enum class CreatorCreativeEvidenceStrength { EARLY, REPEATED }
enum class CreatorCreativeOutcome { CTR, MIDPOINT_RETENTION, SUBSCRIBER_CONVERSION }

data class CreatorCreativeFeature(
    val kind: CreatorCreativeFeatureKind,
    val key: String,
    val label: String,
)

data class CreatorCreativeCohort(
    val feature: CreatorCreativeFeature,
    val projectCount: Int,
    val ctrSamples: Int,
    val averageCtrPercent: Double?,
    val retentionSamples: Int,
    val averageMidpointRetention: Double?,
    val subscriberSamples: Int,
    val averageSubscribersPerThousandViews: Double?,
)

data class CreatorCreativePattern(
    val outcome: CreatorCreativeOutcome,
    val featureKind: CreatorCreativeFeatureKind,
    val leaderLabel: String,
    val comparisonLabel: String,
    val leaderValue: Double,
    val comparisonValue: Double,
    val relativeLiftPercent: Int,
    val leaderSamples: Int,
    val comparisonSamples: Int,
    val strength: CreatorCreativeEvidenceStrength,
)

data class CreatorCreativeIntelligenceSnapshot(
    val connectedProjects: Int,
    val cohorts: List<CreatorCreativeCohort>,
    val patterns: List<CreatorCreativePattern>,
) {
    val hasRepeatedEvidence: Boolean get() = patterns.isNotEmpty()
}

/**
 * Deterministic, creator-specific creative correlations.
 *
 * This engine intentionally analyses structural evidence only. It does not claim to understand
 * thumbnail imagery, emotional tone or semantic hook intent without the later optional AI layer.
 * Missing CTR/retention values are excluded from that metric rather than converted to zero.
 */
object CreatorCreativeIntelligenceEngine {
    const val MIN_PATTERN_SAMPLES = 2
    private const val REPEATED_SAMPLE_THRESHOLD = 4

    fun snapshot(postmortems: List<CreatorVideoPostmortem>): CreatorCreativeIntelligenceSnapshot {
        val unique = postmortems.distinctBy { it.videoId }
        val rows = unique.flatMap { postmortem ->
            features(postmortem).distinctBy { it.kind to it.key }.map { feature -> feature to postmortem }
        }
        val cohorts = rows.groupBy { it.first.kind to it.first.key }.map { (_, values) ->
            val feature = values.first().first
            val records = values.map { it.second }
            val ctr = records.mapNotNull { it.performance.ctrPercent }
            val retention = records.mapNotNull { it.performance.retentionAt50Percent }
            val subscribers = records.mapNotNull(::subscribersPerThousand)
            CreatorCreativeCohort(
                feature = feature,
                projectCount = records.size,
                ctrSamples = ctr.size,
                averageCtrPercent = ctr.averageOrNull(),
                retentionSamples = retention.size,
                averageMidpointRetention = retention.averageOrNull(),
                subscriberSamples = subscribers.size,
                averageSubscribersPerThousandViews = subscribers.averageOrNull(),
            )
        }.sortedWith(compareBy<CreatorCreativeCohort> { it.feature.kind.ordinal }.thenByDescending { it.projectCount })

        val patterns = buildList {
            CreatorCreativeFeatureKind.entries.forEach { kind ->
                bestPattern(cohorts.filter { it.feature.kind == kind }, CreatorCreativeOutcome.CTR)?.let(::add)
                bestPattern(cohorts.filter { it.feature.kind == kind }, CreatorCreativeOutcome.MIDPOINT_RETENTION)?.let(::add)
                bestPattern(cohorts.filter { it.feature.kind == kind }, CreatorCreativeOutcome.SUBSCRIBER_CONVERSION)?.let(::add)
            }
        }.sortedWith(
            compareByDescending<CreatorCreativePattern> { it.strength == CreatorCreativeEvidenceStrength.REPEATED }
                .thenByDescending { abs(it.relativeLiftPercent) }
                .thenBy { it.featureKind.ordinal },
        )

        return CreatorCreativeIntelligenceSnapshot(
            connectedProjects = unique.size,
            cohorts = cohorts,
            patterns = patterns,
        )
    }

    internal fun features(value: CreatorVideoPostmortem): List<CreatorCreativeFeature> = buildList {
        add(hookFeature(value.creation.hook))
        add(titleFeature(value.packaging.titleSnapshot))
        add(scriptFeature(value.creation.scriptPresent, value.creation.scriptCharacterCount))
        value.creation.archetypeId.takeIf { it.isNotBlank() }?.let {
            add(CreatorCreativeFeature(CreatorCreativeFeatureKind.ARCHETYPE, it.lowercase(), pretty(it)))
        }
        value.creation.productionStyles.filter { it.isNotBlank() }.forEach {
            add(CreatorCreativeFeature(CreatorCreativeFeatureKind.PRODUCTION_STYLE, it.lowercase(), pretty(it)))
        }
        value.creation.deliveryFormat.takeIf { it.isNotBlank() }?.let {
            add(CreatorCreativeFeature(CreatorCreativeFeatureKind.DELIVERY_FORMAT, it.lowercase(), it))
        }
    }

    internal fun classifyHook(text: String): String {
        val clean = text.trim()
        if (clean.isBlank()) return "missing"
        if ('?' in clean) return "question"
        if (clean.startsWith('"') || clean.startsWith('“') || clean.startsWith('‘') || clean.startsWith('\'')) return "quote"
        if (clean.length >= 140) return "long_context"
        if (clean.length <= 80) return "short_statement"
        return "medium_statement"
    }

    internal fun classifyTitle(text: String): String {
        val clean = text.trim()
        if (clean.isBlank()) return "missing"
        if ('?' in clean) return "question"
        if (clean.length <= 55) return "short_statement"
        return "long_statement"
    }

    private fun hookFeature(text: String): CreatorCreativeFeature {
        val key = classifyHook(text)
        val label = when (key) {
            "question" -> "Question hook"
            "quote" -> "Quote-led hook"
            "long_context" -> "Long-context hook"
            "short_statement" -> "Short statement hook"
            "medium_statement" -> "Medium statement hook"
            else -> "Hook not captured"
        }
        return CreatorCreativeFeature(CreatorCreativeFeatureKind.HOOK_STRUCTURE, key, label)
    }

    private fun titleFeature(text: String): CreatorCreativeFeature {
        val key = classifyTitle(text)
        val label = when (key) {
            "question" -> "Question title"
            "short_statement" -> "Short statement title"
            "long_statement" -> "Long statement title"
            else -> "Title not captured"
        }
        return CreatorCreativeFeature(CreatorCreativeFeatureKind.TITLE_STRUCTURE, key, label)
    }

    private fun scriptFeature(present: Boolean, characters: Int): CreatorCreativeFeature {
        val key = when {
            !present -> "missing"
            characters <= 1_200 -> "short"
            characters <= 4_000 -> "medium"
            else -> "long"
        }
        val label = when (key) {
            "short" -> "Short captured script"
            "medium" -> "Medium captured script"
            "long" -> "Long captured script"
            else -> "Script not captured"
        }
        return CreatorCreativeFeature(CreatorCreativeFeatureKind.SCRIPT_SIZE, key, label)
    }

    private fun subscribersPerThousand(value: CreatorVideoPostmortem): Double? {
        val views = value.performance.periodViews
        if (views <= 0L) return null
        return value.performance.netSubscribers.toDouble() * 1_000.0 / views.toDouble()
    }

    private fun bestPattern(cohorts: List<CreatorCreativeCohort>, outcome: CreatorCreativeOutcome): CreatorCreativePattern? {
        val eligible = cohorts.mapNotNull { cohort ->
            val metric = metric(cohort, outcome) ?: return@mapNotNull null
            val samples = samples(cohort, outcome)
            if (samples < MIN_PATTERN_SAMPLES || cohort.feature.key == "missing") return@mapNotNull null
            Triple(cohort, metric, samples)
        }.sortedByDescending { it.second }
        if (eligible.size < 2) return null
        val leader = eligible[0]
        val comparison = eligible[1]
        if (leader.second <= comparison.second) return null
        val relativeLift = if (abs(comparison.second) < 0.0001) 0
        else (((leader.second - comparison.second) / abs(comparison.second)) * 100.0).toInt()
        val minSamples = minOf(leader.third, comparison.third)
        return CreatorCreativePattern(
            outcome = outcome,
            featureKind = leader.first.feature.kind,
            leaderLabel = leader.first.feature.label,
            comparisonLabel = comparison.first.feature.label,
            leaderValue = leader.second,
            comparisonValue = comparison.second,
            relativeLiftPercent = relativeLift,
            leaderSamples = leader.third,
            comparisonSamples = comparison.third,
            strength = if (minSamples >= REPEATED_SAMPLE_THRESHOLD) CreatorCreativeEvidenceStrength.REPEATED
                else CreatorCreativeEvidenceStrength.EARLY,
        )
    }

    private fun metric(cohort: CreatorCreativeCohort, outcome: CreatorCreativeOutcome): Double? = when (outcome) {
        CreatorCreativeOutcome.CTR -> cohort.averageCtrPercent
        CreatorCreativeOutcome.MIDPOINT_RETENTION -> cohort.averageMidpointRetention
        CreatorCreativeOutcome.SUBSCRIBER_CONVERSION -> cohort.averageSubscribersPerThousandViews
    }

    private fun samples(cohort: CreatorCreativeCohort, outcome: CreatorCreativeOutcome): Int = when (outcome) {
        CreatorCreativeOutcome.CTR -> cohort.ctrSamples
        CreatorCreativeOutcome.MIDPOINT_RETENTION -> cohort.retentionSamples
        CreatorCreativeOutcome.SUBSCRIBER_CONVERSION -> cohort.subscriberSamples
    }

    private fun Iterable<Double>.averageOrNull(): Double? {
        val list = toList()
        return if (list.isEmpty()) null else list.average()
    }

    private fun pretty(raw: String): String = raw
        .replace('-', ' ')
        .replace('_', ' ')
        .trim()
        .split(Regex("\\s+"))
        .joinToString(" ") { it.lowercase().replaceFirstChar(Char::uppercase) }
}
