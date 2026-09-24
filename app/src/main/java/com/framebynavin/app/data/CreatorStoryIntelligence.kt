package com.framebynavin.app.data

import java.time.LocalDate

enum class CreatorStoryPriority {
    COVER_NOW,
    PREPARE,
    VERIFY_FIRST,
}

data class CreatorStoryRecommendation(
    val signalId: String,
    val title: String,
    val summary: String,
    val priority: CreatorStoryPriority,
    val verification: CreatorMediaVerification,
    val releaseBucket: CreatorMediaReleaseBucket,
    val score: Int,
    val confidence: Int,
    val whyNow: String,
    val actionLabel: String,
    val evidence: List<CreatorMediaEvidence>,
)

data class CreatorStoryIntelligenceSnapshot(
    val coverNow: List<CreatorStoryRecommendation> = emptyList(),
    val prepare: List<CreatorStoryRecommendation> = emptyList(),
    val verifyFirst: List<CreatorStoryRecommendation> = emptyList(),
) {
    val all: List<CreatorStoryRecommendation>
        get() = coverNow + prepare + verifyFirst
}

object CreatorStoryIntelligenceEngine {
    fun build(
        signals: List<CreatorMediaSignal>,
        today: LocalDate,
        nowMillis: Long,
    ): CreatorStoryIntelligenceSnapshot {
        val ranked = signals
            .asSequence()
            .filter { it.id.isNotBlank() && it.title.isNotBlank() }
            .filter { it.evidence.isNotEmpty() }
            .distinctBy { it.id }
            .map { recommendation(it, today, nowMillis) }
            .sortedWith(
                compareByDescending<CreatorStoryRecommendation> { it.score }
                    .thenByDescending { it.confidence }
            )
            .toList()

        return CreatorStoryIntelligenceSnapshot(
            coverNow = ranked.filter { it.priority == CreatorStoryPriority.COVER_NOW }.take(5),
            prepare = ranked.filter { it.priority == CreatorStoryPriority.PREPARE }.take(8),
            verifyFirst = ranked.filter { it.priority == CreatorStoryPriority.VERIFY_FIRST }.take(5),
        )
    }

    private fun recommendation(
        signal: CreatorMediaSignal,
        today: LocalDate,
        nowMillis: Long,
    ): CreatorStoryRecommendation {
        val releaseBucket = CreatorMediaSignalClassifier.releaseBucket(signal.releaseDate, today)
        val freshness = CreatorMediaSignalClassifier.freshnessScore(signal, nowMillis)
        val releaseUrgency = when (releaseBucket) {
            CreatorMediaReleaseBucket.TODAY -> 100
            CreatorMediaReleaseBucket.THIS_WEEKEND -> 90
            CreatorMediaReleaseBucket.UPCOMING -> 72
            CreatorMediaReleaseBucket.RELEASED -> 38
            CreatorMediaReleaseBucket.LATER -> 30
            CreatorMediaReleaseBucket.UNSCHEDULED -> freshness
        }
        val verificationReadiness = when (signal.verification) {
            CreatorMediaVerification.VERIFIED -> 100
            CreatorMediaVerification.DEVELOPING -> 66
            CreatorMediaVerification.RUMOR -> 28
        }
        val score = (
            freshness.coerceIn(0, 100) * 30 +
                releaseUrgency.coerceIn(0, 100) * 20 +
                signal.relevanceScore.coerceIn(0, 100) * 20 +
                signal.audienceImpactScore.coerceIn(0, 100) * 15 +
                signal.evidenceStrength.coerceIn(0, 100) * 15
            ) / 100
        val confidence = (
            signal.evidenceStrength * 60 +
                verificationReadiness * 40
            ) / 100

        val priority = when {
            signal.verification == CreatorMediaVerification.RUMOR -> CreatorStoryPriority.VERIFY_FIRST
            signal.verification == CreatorMediaVerification.VERIFIED &&
                freshness >= 70 &&
                (releaseBucket == CreatorMediaReleaseBucket.TODAY ||
                    releaseBucket == CreatorMediaReleaseBucket.THIS_WEEKEND ||
                    score >= 78) -> CreatorStoryPriority.COVER_NOW
            else -> CreatorStoryPriority.PREPARE
        }

        return CreatorStoryRecommendation(
            signalId = signal.id,
            title = signal.title,
            summary = signal.summary,
            priority = priority,
            verification = signal.verification,
            releaseBucket = releaseBucket,
            score = score,
            confidence = confidence.coerceIn(0, 100),
            whyNow = whyNow(signal, releaseBucket, freshness),
            actionLabel = when (priority) {
                CreatorStoryPriority.COVER_NOW -> "COVER NOW"
                CreatorStoryPriority.PREPARE -> "PREPARE COVERAGE"
                CreatorStoryPriority.VERIFY_FIRST -> "VERIFY FIRST"
            },
            evidence = signal.evidence,
        )
    }

    private fun whyNow(
        signal: CreatorMediaSignal,
        bucket: CreatorMediaReleaseBucket,
        freshness: Int,
    ): String {
        val timing = when (bucket) {
            CreatorMediaReleaseBucket.TODAY -> "releases today"
            CreatorMediaReleaseBucket.THIS_WEEKEND -> "lands this weekend"
            CreatorMediaReleaseBucket.UPCOMING -> "is due within 30 days"
            CreatorMediaReleaseBucket.RELEASED -> "has already released"
            CreatorMediaReleaseBucket.LATER -> "is scheduled beyond the 30-day window"
            CreatorMediaReleaseBucket.UNSCHEDULED -> "has no confirmed release date"
        }
        val verification = when (signal.verification) {
            CreatorMediaVerification.VERIFIED -> "verified"
            CreatorMediaVerification.DEVELOPING -> "still developing"
            CreatorMediaVerification.RUMOR -> "unverified"
        }
        return "${signal.kind.name.replace('_', ' ').lowercase().replaceFirstChar { it.uppercase() }} $timing; the signal is $verification with ${signal.evidence.size} evidence source${if (signal.evidence.size == 1) "" else "s"} and freshness $freshness/100."
    }
}
