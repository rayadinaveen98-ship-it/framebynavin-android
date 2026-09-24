package com.framebynavin.app.data

import java.time.DayOfWeek
import java.time.LocalDate
import java.time.temporal.ChronoUnit

data class CreatorOttReleaseEntry(
    val signalId: String,
    val title: String,
    val summary: String,
    val platform: String,
    val languages: Set<String>,
    val releaseDate: LocalDate,
    val verification: CreatorMediaVerification,
    val evidenceStrength: Int,
    val evidence: List<CreatorMediaEvidence>,
)

data class CreatorOttReleaseBoard(
    val weekend: List<CreatorOttReleaseEntry> = emptyList(),
    val today: List<CreatorOttReleaseEntry> = emptyList(),
    val upcoming: List<CreatorOttReleaseEntry> = emptyList(),
    val recentlyReleased: List<CreatorOttReleaseEntry> = emptyList(),
    val laterCount: Int = 0,
    val unscheduledCount: Int = 0,
)

/**
 * Builds the OTT surface from evidence-backed media signals.
 *
 * The board intentionally keeps the four user-facing time lanes deterministic:
 *  - Weekend: Friday-Sunday in the current/next weekend window, including a release that is today.
 *  - Today: exact local calendar date.
 *  - Upcoming: the next 30 days after removing Today and Weekend duplicates.
 *  - Recently released: the previous 14 days only.
 *
 * A future date can therefore never enter the released lane. Unknown-language signals are retained
 * because hiding them would turn incomplete metadata into a false negative.
 */
object CreatorOttReleaseBoardEngine {
    private const val UPCOMING_WINDOW_DAYS = 30L
    private const val RECENT_RELEASE_WINDOW_DAYS = 14L

    fun build(
        signals: List<CreatorMediaSignal>,
        today: LocalDate,
        preferredLanguages: Set<String> = emptySet(),
    ): CreatorOttReleaseBoard {
        val preferred = preferredLanguages.normalizedLanguages()
        val ottSignals = signals
            .asSequence()
            .filter { it.kind == CreatorMediaSignalKind.OTT_RELEASE }
            .filter { it.id.isNotBlank() && it.title.isNotBlank() }
            .filter { it.evidence.isNotEmpty() }
            .map(CreatorMediaVerificationPolicy::normalize)
            .filter { signal ->
                val signalLanguages = signal.languages.normalizedLanguages()
                preferred.isEmpty() || signalLanguages.isEmpty() || signalLanguages.any(preferred::contains)
            }
            .toList()

        val unscheduledCount = ottSignals.count { it.releaseDate == null }
        val scheduled = ottSignals
            .filter { it.releaseDate != null }
            .groupBy(::dedupeKey)
            .mapNotNull { (_, candidates) -> candidates.maxWithOrNull(signalQualityComparator) }

        val entries = scheduled.associateWith(::toEntry)
        val weekendRange = weekendRange(today)

        val todayEntries = scheduled
            .filter { it.releaseDate == today }
            .mapNotNull(entries::get)
            .sortedWith(entryComparator)

        val weekendEntries = scheduled
            .filter { signal -> signal.releaseDate?.let { it in weekendRange } == true }
            .mapNotNull(entries::get)
            .sortedWith(entryComparator)

        val upcomingEntries = scheduled
            .filter { signal ->
                val date = signal.releaseDate ?: return@filter false
                val daysAhead = ChronoUnit.DAYS.between(today, date)
                daysAhead in 1..UPCOMING_WINDOW_DAYS && date !in weekendRange
            }
            .mapNotNull(entries::get)
            .sortedWith(entryComparator)

        val recentlyReleasedEntries = scheduled
            .filter { signal ->
                val date = signal.releaseDate ?: return@filter false
                val daysAgo = ChronoUnit.DAYS.between(date, today)
                daysAgo in 1..RECENT_RELEASE_WINDOW_DAYS
            }
            .mapNotNull(entries::get)
            .sortedWith(compareByDescending<CreatorOttReleaseEntry> { it.releaseDate }
                .thenByDescending { it.evidenceStrength }
                .thenBy { it.title.lowercase() })

        val laterCount = scheduled.count { signal ->
            val date = signal.releaseDate ?: return@count false
            ChronoUnit.DAYS.between(today, date) > UPCOMING_WINDOW_DAYS
        }

        return CreatorOttReleaseBoard(
            weekend = weekendEntries,
            today = todayEntries,
            upcoming = upcomingEntries,
            recentlyReleased = recentlyReleasedEntries,
            laterCount = laterCount,
            unscheduledCount = unscheduledCount,
        )
    }

    private fun toEntry(signal: CreatorMediaSignal): CreatorOttReleaseEntry? {
        val date = signal.releaseDate ?: return null
        return CreatorOttReleaseEntry(
            signalId = signal.id,
            title = signal.title.trim(),
            summary = signal.summary.trim(),
            platform = signal.platform.trim(),
            languages = signal.languages.map(String::trim).filter(String::isNotBlank).toSet(),
            releaseDate = date,
            verification = signal.verification,
            evidenceStrength = signal.evidenceStrength,
            evidence = signal.evidence,
        )
    }

    private fun dedupeKey(signal: CreatorMediaSignal): String = listOf(
        signal.title.trim().lowercase(),
        signal.platform.trim().lowercase(),
        signal.releaseDate?.toString().orEmpty(),
    ).joinToString("|")

    private fun weekendRange(today: LocalDate): ClosedRange<LocalDate> {
        val friday = when (today.dayOfWeek) {
            DayOfWeek.FRIDAY -> today
            DayOfWeek.SATURDAY -> today.minusDays(1)
            DayOfWeek.SUNDAY -> today.minusDays(2)
            else -> {
                val daysUntilFriday = (DayOfWeek.FRIDAY.value - today.dayOfWeek.value + 7) % 7
                today.plusDays(daysUntilFriday.toLong())
            }
        }
        return friday..friday.plusDays(2)
    }

    private fun Set<String>.normalizedLanguages(): Set<String> =
        map(String::trim).filter(String::isNotBlank).map(String::lowercase).toSet()

    private val signalQualityComparator = compareBy<CreatorMediaSignal> { it.evidenceStrength }
        .thenBy {
            when (it.verification) {
                CreatorMediaVerification.VERIFIED -> 3
                CreatorMediaVerification.DEVELOPING -> 2
                CreatorMediaVerification.RUMOR -> 1
            }
        }
        .thenBy { it.publishedAtMillis }

    private val entryComparator = compareBy<CreatorOttReleaseEntry> { it.releaseDate }
        .thenByDescending { it.evidenceStrength }
        .thenBy { it.title.lowercase() }
}
