package com.framebynavin.app.data

import java.util.Calendar
import java.util.Locale
import java.util.TimeZone
import kotlin.math.max

enum class CreatorMediaVerification {
    VERIFIED,
    DEVELOPING,
    RUMOR,
}

enum class CreatorMediaSignalKind {
    TRAILER,
    TEASER,
    ANNOUNCEMENT,
    OTT_RELEASE,
    RELEASE_DATE,
    CASTING,
    PRODUCTION,
    INTERVIEW,
    BOX_OFFICE,
    OTHER,
}

data class CreatorMediaSignal(
    val id: String,
    val sourceId: String,
    val title: String,
    val summary: String = "",
    val languages: Set<String> = emptySet(),
    val publishedAtMillis: Long,
    val eventAtMillis: Long? = null,
    val kind: CreatorMediaSignalKind = CreatorMediaSignalKind.OTHER,
    val verification: CreatorMediaVerification = CreatorMediaVerification.DEVELOPING,
    val storyKey: String = "",
    val platform: String = "",
    val authorityOverride: CreatorMediaSourceAuthority? = null,
)

data class CreatorRadarStory(
    val id: String,
    val title: String,
    val summary: String,
    val sourceId: String,
    val score: Int,
    val verification: CreatorMediaVerification,
    val kind: CreatorMediaSignalKind,
    val eventAtMillis: Long?,
    val platform: String,
    val corroborationCount: Int,
    val reasons: List<String>,
)

data class CreatorRadarSnapshot(
    val stories: List<CreatorRadarStory>,
)

enum class CreatorOttReleaseState {
    PAST,
    TODAY,
    THIS_WEEKEND,
    UPCOMING,
    LATER,
}

data class CreatorOttRelease(
    val id: String,
    val title: String,
    val platform: String,
    val releaseAtMillis: Long,
    val verification: CreatorMediaVerification,
    val sourceId: String,
    val state: CreatorOttReleaseState,
)

data class CreatorOttSnapshot(
    val weekend: List<CreatorOttRelease>,
    val today: List<CreatorOttRelease>,
    val upcoming: List<CreatorOttRelease>,
)

/**
 * V147 media-intelligence core used by Radar and OTT surfaces.
 *
 * Rules are intentionally deterministic and local-first: Backlot ranks only supplied signals,
 * rewards first-party/platform authority, heavily favors fresh verified reporting, and personalizes
 * by the creator's chosen languages/sources. Older news is dropped unless it points to a near-future
 * event. Duplicate reports collapse into one story with a corroboration bonus.
 */
object CreatorStoryIntelligenceEngine {
    private const val HOUR_MILLIS = 60L * 60L * 1000L
    private const val DAY_MILLIS = 24L * HOUR_MILLIS

    fun buildRadar(
        signals: List<CreatorMediaSignal>,
        profile: CreatorProfile,
        nowMillis: Long,
        maxStories: Int = 20,
    ): CreatorRadarSnapshot {
        val safeMax = maxStories.coerceIn(1, 50)
        val normalizedProfile = profile.normalized()
        val candidates = signals
            .asSequence()
            .filter { it.id.isNotBlank() && it.sourceId.isNotBlank() && it.title.isNotBlank() }
            .filter { it.publishedAtMillis <= nowMillis + 5L * 60L * 1000L }
            .filter { signal ->
                val age = max(0L, nowMillis - signal.publishedAtMillis)
                val hasNearFutureEvent = signal.eventAtMillis?.let { it >= nowMillis && it <= nowMillis + 31L * DAY_MILLIS } == true
                age <= 14L * DAY_MILLIS || hasNearFutureEvent
            }
            .toList()

        val grouped = candidates.groupBy(::storyDedupeKey)
        val ranked = grouped.values.mapNotNull { group ->
            val scored = group.map { signal -> signal to score(signal, normalizedProfile, nowMillis) }
            val representative = scored.maxWithOrNull(
                compareBy<Pair<CreatorMediaSignal, ScoreResult>> { it.second.score }
                    .thenBy { it.first.publishedAtMillis }
            ) ?: return@mapNotNull null
            val verification = group.maxByOrNull { verificationRank(it.verification) }?.verification
                ?: representative.first.verification
            val corroborationCount = group.map { it.sourceId.lowercase(Locale.US) }.distinct().size
            val corroborationBonus = ((corroborationCount - 1) * 4).coerceIn(0, 12)
            val reasons = buildList {
                addAll(representative.second.reasons)
                if (corroborationCount > 1) add("Corroborated by $corroborationCount sources")
            }
            CreatorRadarStory(
                id = representative.first.id,
                title = representative.first.title.trim(),
                summary = representative.first.summary.trim(),
                sourceId = representative.first.sourceId.trim(),
                score = (representative.second.score + corroborationBonus).coerceIn(0, 100),
                verification = verification,
                kind = representative.first.kind,
                eventAtMillis = representative.first.eventAtMillis,
                platform = representative.first.platform.trim(),
                corroborationCount = corroborationCount,
                reasons = reasons.distinct().take(5),
            )
        }

        return CreatorRadarSnapshot(
            stories = ranked
                .sortedWith(compareByDescending<CreatorRadarStory> { it.score }.thenByDescending { it.eventAtMillis ?: 0L }.thenBy { it.title.lowercase(Locale.US) })
                .take(safeMax),
        )
    }

    private data class ScoreResult(val score: Int, val reasons: List<String>)

    private fun score(
        signal: CreatorMediaSignal,
        profile: CreatorProfile,
        nowMillis: Long,
    ): ScoreResult {
        var score = 0
        val reasons = mutableListOf<String>()
        val source = CreatorMediaSourceRegistry.byId(signal.sourceId)
        val authority = signal.authorityOverride ?: source?.authority

        when (signal.verification) {
            CreatorMediaVerification.VERIFIED -> {
                score += 28
                reasons += "Verified"
            }
            CreatorMediaVerification.DEVELOPING -> score += 14
            CreatorMediaVerification.RUMOR -> score -= 12
        }

        when (authority) {
            CreatorMediaSourceAuthority.OFFICIAL -> {
                score += 24
                reasons += "Official source"
            }
            CreatorMediaSourceAuthority.PLATFORM -> {
                score += 22
                reasons += "Platform source"
            }
            CreatorMediaSourceAuthority.TRADE -> score += 16
            CreatorMediaSourceAuthority.REPUTABLE_NEWS -> score += 12
            null -> Unit
        }

        val ageHours = max(0L, nowMillis - signal.publishedAtMillis) / HOUR_MILLIS
        score += when {
            ageHours <= 12L -> 24
            ageHours <= 24L -> 20
            ageHours <= 48L -> 15
            ageHours <= 72L -> 10
            ageHours <= 7L * 24L -> 5
            else -> 0
        }
        if (ageHours <= 24L) reasons += "Fresh <24h"

        val preferredLanguages = profile.preferredMediaLanguages
            .map(CreatorMediaLanguageRegistry::canonicalLabel)
            .map(String::lowercase)
            .toSet()
        val signalLanguages = signal.languages
            .map(CreatorMediaLanguageRegistry::canonicalLabel)
            .filter(String::isNotBlank)
            .map(String::lowercase)
            .toSet()
        when {
            preferredLanguages.isEmpty() -> score += 4
            signalLanguages.isEmpty() -> score += 5
            signalLanguages.any(preferredLanguages::contains) -> {
                score += 14
                reasons += "Preferred language"
            }
            else -> score -= 6
        }

        if (profile.selectedMediaSourceIds.any { it.equals(signal.sourceId, ignoreCase = true) }) {
            score += 10
            reasons += "Selected source"
        }

        score += when (signal.kind) {
            CreatorMediaSignalKind.TRAILER, CreatorMediaSignalKind.TEASER -> 10
            CreatorMediaSignalKind.ANNOUNCEMENT, CreatorMediaSignalKind.OTT_RELEASE, CreatorMediaSignalKind.RELEASE_DATE -> 9
            CreatorMediaSignalKind.PRODUCTION -> 6
            CreatorMediaSignalKind.CASTING, CreatorMediaSignalKind.INTERVIEW, CreatorMediaSignalKind.BOX_OFFICE -> 4
            CreatorMediaSignalKind.OTHER -> 0
        }

        signal.eventAtMillis?.let { eventAt ->
            val until = eventAt - nowMillis
            if (until >= 0L) {
                when {
                    until <= DAY_MILLIS -> {
                        score += 12
                        reasons += "Event within 24h"
                    }
                    until <= 3L * DAY_MILLIS -> {
                        score += 8
                        reasons += "Event within 3 days"
                    }
                    until <= 7L * DAY_MILLIS -> score += 4
                }
            }
        }

        return ScoreResult(score.coerceIn(0, 100), reasons.distinct())
    }

    private fun verificationRank(value: CreatorMediaVerification): Int = when (value) {
        CreatorMediaVerification.VERIFIED -> 3
        CreatorMediaVerification.DEVELOPING -> 2
        CreatorMediaVerification.RUMOR -> 1
    }

    private fun storyDedupeKey(signal: CreatorMediaSignal): String {
        val explicit = signal.storyKey.trim().lowercase(Locale.US)
        if (explicit.isNotBlank()) return explicit
        return signal.title
            .lowercase(Locale.US)
            .replace(Regex("[^a-z0-9]+"), " ")
            .trim()
            .replace(Regex("\\s+"), " ")
    }
}

/** Keeps OTT dates honest: future dates can never be surfaced as already released. */
object CreatorOttFreshnessEngine {
    private const val DAY_MILLIS = 24L * 60L * 60L * 1000L

    fun build(
        signals: List<CreatorMediaSignal>,
        nowMillis: Long,
        timeZoneId: String = "Asia/Kolkata",
        upcomingDays: Int = 31,
    ): CreatorOttSnapshot {
        val zone = TimeZone.getTimeZone(timeZoneId)
        val todayStart = startOfDay(nowMillis, zone)
        val tomorrowStart = addDays(todayStart, 1, zone)
        val futureLimit = addDays(todayStart, upcomingDays.coerceIn(1, 60) + 1, zone)
        val weekendStart = currentOrUpcomingWeekendStart(todayStart, zone)
        val weekendEnd = addDays(weekendStart, 3, zone)

        val releases = signals
            .asSequence()
            .filter { it.kind == CreatorMediaSignalKind.OTT_RELEASE }
            .filter { it.title.isNotBlank() && it.eventAtMillis != null }
            .map { signal ->
                val releaseAt = signal.eventAtMillis!!
                CreatorOttRelease(
                    id = signal.id,
                    title = signal.title.trim(),
                    platform = signal.platform.trim(),
                    releaseAtMillis = releaseAt,
                    verification = signal.verification,
                    sourceId = signal.sourceId.trim(),
                    state = classify(releaseAt, nowMillis, zone, upcomingDays),
                )
            }
            .distinctBy { it.id }
            .sortedWith(compareBy<CreatorOttRelease> { it.releaseAtMillis }.thenBy { it.title.lowercase(Locale.US) })
            .toList()

        val today = releases.filter { it.releaseAtMillis in todayStart until tomorrowStart }
        val weekend = releases.filter { it.releaseAtMillis in weekendStart until weekendEnd }
        val weekendIds = weekend.map { it.id }.toSet()
        val todayIds = today.map { it.id }.toSet()
        val upcoming = releases.filter {
            it.releaseAtMillis in tomorrowStart until futureLimit &&
                it.id !in weekendIds &&
                it.id !in todayIds
        }

        return CreatorOttSnapshot(
            weekend = weekend,
            today = today,
            upcoming = upcoming,
        )
    }

    fun classify(
        releaseAtMillis: Long,
        nowMillis: Long,
        timeZoneId: String = "Asia/Kolkata",
        upcomingDays: Int = 31,
    ): CreatorOttReleaseState = classify(
        releaseAtMillis = releaseAtMillis,
        nowMillis = nowMillis,
        zone = TimeZone.getTimeZone(timeZoneId),
        upcomingDays = upcomingDays,
    )

    private fun classify(
        releaseAtMillis: Long,
        nowMillis: Long,
        zone: TimeZone,
        upcomingDays: Int,
    ): CreatorOttReleaseState {
        val todayStart = startOfDay(nowMillis, zone)
        val tomorrowStart = addDays(todayStart, 1, zone)
        if (releaseAtMillis < todayStart) return CreatorOttReleaseState.PAST
        if (releaseAtMillis < tomorrowStart) return CreatorOttReleaseState.TODAY

        val weekendStart = currentOrUpcomingWeekendStart(todayStart, zone)
        val weekendEnd = addDays(weekendStart, 3, zone)
        if (releaseAtMillis in weekendStart until weekendEnd) return CreatorOttReleaseState.THIS_WEEKEND

        val futureLimit = addDays(todayStart, upcomingDays.coerceIn(1, 60) + 1, zone)
        return if (releaseAtMillis < futureLimit) CreatorOttReleaseState.UPCOMING else CreatorOttReleaseState.LATER
    }

    private fun startOfDay(timeMillis: Long, zone: TimeZone): Long {
        val calendar = Calendar.getInstance(zone).apply {
            timeInMillis = timeMillis
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
        return calendar.timeInMillis
    }

    private fun addDays(startMillis: Long, days: Int, zone: TimeZone): Long =
        Calendar.getInstance(zone).apply {
            timeInMillis = startMillis
            add(Calendar.DAY_OF_YEAR, days)
        }.timeInMillis

    private fun currentOrUpcomingWeekendStart(todayStart: Long, zone: TimeZone): Long {
        val calendar = Calendar.getInstance(zone).apply { timeInMillis = todayStart }
        val offsetToFriday = when (calendar.get(Calendar.DAY_OF_WEEK)) {
            Calendar.FRIDAY -> 0
            Calendar.SATURDAY -> -1
            Calendar.SUNDAY -> -2
            Calendar.MONDAY -> 4
            Calendar.TUESDAY -> 3
            Calendar.WEDNESDAY -> 2
            Calendar.THURSDAY -> 1
            else -> 0
        }
        return addDays(todayStart, offsetToFriday, zone)
    }
}
