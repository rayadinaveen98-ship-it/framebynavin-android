package com.framebynavin.app.data

import java.time.DayOfWeek
import java.time.LocalDate
import java.time.temporal.ChronoUnit

enum class CreatorMediaSignalKind {
    OTT_RELEASE,
    THEATRICAL_RELEASE,
    TRAILER,
    TEASER,
    ANNOUNCEMENT,
    INDUSTRY_NEWS,
    PRODUCTION_UPDATE,
    PLATFORM_CHANGE,
}

enum class CreatorMediaVerification {
    VERIFIED,
    DEVELOPING,
    RUMOR,
}

enum class CreatorMediaEvidenceTier {
    OFFICIAL,
    TRADE,
    REPUTABLE_NEWS,
    SECONDARY,
}

enum class CreatorMediaReleaseBucket {
    TODAY,
    THIS_WEEKEND,
    UPCOMING,
    RELEASED,
    LATER,
    UNSCHEDULED,
}

data class CreatorMediaEvidence(
    val id: String,
    val label: String,
    val url: String = "",
    val tier: CreatorMediaEvidenceTier,
)

data class CreatorMediaSignal(
    val id: String,
    val title: String,
    val summary: String,
    val kind: CreatorMediaSignalKind,
    val publishedAtMillis: Long,
    val verification: CreatorMediaVerification,
    val evidence: List<CreatorMediaEvidence>,
    val releaseDate: LocalDate? = null,
    val platform: String = "",
    val languages: Set<String> = emptySet(),
    val relevanceScore: Int = 50,
    val audienceImpactScore: Int = 50,
) {
    val evidenceStrength: Int
        get() {
            if (evidence.isEmpty()) return 0
            val tierScore = evidence.maxOf {
                when (it.tier) {
                    CreatorMediaEvidenceTier.OFFICIAL -> 92
                    CreatorMediaEvidenceTier.TRADE -> 82
                    CreatorMediaEvidenceTier.REPUTABLE_NEWS -> 72
                    CreatorMediaEvidenceTier.SECONDARY -> 52
                }
            }
            val corroboration = ((evidence.size - 1).coerceAtLeast(0) * 5).coerceAtMost(15)
            val verificationAdjustment = when (verification) {
                CreatorMediaVerification.VERIFIED -> 0
                CreatorMediaVerification.DEVELOPING -> -12
                CreatorMediaVerification.RUMOR -> -28
            }
            return (tierScore + corroboration + verificationAdjustment).coerceIn(0, 100)
        }
}

object CreatorMediaSignalClassifier {
    private const val UPCOMING_WINDOW_DAYS = 30L

    fun releaseBucket(
        releaseDate: LocalDate?,
        today: LocalDate,
    ): CreatorMediaReleaseBucket {
        if (releaseDate == null) return CreatorMediaReleaseBucket.UNSCHEDULED
        if (releaseDate == today) return CreatorMediaReleaseBucket.TODAY
        if (releaseDate.isBefore(today)) return CreatorMediaReleaseBucket.RELEASED
        if (isInCurrentOrNextWeekend(releaseDate, today)) return CreatorMediaReleaseBucket.THIS_WEEKEND

        val daysAhead = ChronoUnit.DAYS.between(today, releaseDate)
        return if (daysAhead in 1..UPCOMING_WINDOW_DAYS) {
            CreatorMediaReleaseBucket.UPCOMING
        } else {
            CreatorMediaReleaseBucket.LATER
        }
    }

    fun ageHours(signal: CreatorMediaSignal, nowMillis: Long): Long {
        if (signal.publishedAtMillis <= 0L || nowMillis <= signal.publishedAtMillis) return 0L
        return (nowMillis - signal.publishedAtMillis) / 3_600_000L
    }

    fun freshnessScore(signal: CreatorMediaSignal, nowMillis: Long): Int = when (ageHours(signal, nowMillis)) {
        in 0..5 -> 100
        in 6..12 -> 92
        in 13..24 -> 84
        in 25..48 -> 70
        in 49..72 -> 58
        in 73..168 -> 42
        else -> 24
    }

    private fun isInCurrentOrNextWeekend(
        releaseDate: LocalDate,
        today: LocalDate,
    ): Boolean {
        val friday = when (today.dayOfWeek) {
            DayOfWeek.FRIDAY -> today
            DayOfWeek.SATURDAY -> today.minusDays(1)
            DayOfWeek.SUNDAY -> today.minusDays(2)
            else -> {
                val daysUntilFriday = (DayOfWeek.FRIDAY.value - today.dayOfWeek.value + 7) % 7
                today.plusDays(daysUntilFriday.toLong())
            }
        }
        val sunday = friday.plusDays(2)
        return !releaseDate.isBefore(friday) && !releaseDate.isAfter(sunday)
    }
}
