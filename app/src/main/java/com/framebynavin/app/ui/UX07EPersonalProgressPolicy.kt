package com.framebynavin.app.ui

import com.framebynavin.app.personal.PersonalCompletionEvent
import com.framebynavin.app.personal.Phase1HabitChallenge
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId

/**
 * UX07E READ-ONLY snapshots. Never writes completion receipts, challenge days,
 * journal notes, or creates a shared source of truth.
 *
 * Null means the canonical source is unavailable/unverified; an empty list
 * means it was read successfully and contains no records.
 */
internal data class UX07EPersonalProgressSnapshot(
    val personalDoneLastSeven: Int?,
    val personalActiveDaysLastSeven: Int?,
    val challengeDaysConfirmedLastSeven: Int?,
    val challengeReflectionDaysLastSeven: Int?,
)

internal data class UX07EChallengeReflection(
    val day: LocalDate,
    val note: String,
    val confirmedAt: Instant?,
)

internal object UX07EPersonalProgressPolicy {
    fun lastSevenDays(
        personalDone: List<PersonalCompletionEvent>?,
        challenges: List<Phase1HabitChallenge>?,
        now: Instant,
        displayZone: ZoneId,
    ): UX07EPersonalProgressSnapshot {
        val dateToday = now.atZone(displayZone).toLocalDate()
        val first = dateToday.minusDays(6)
        val validReceipts = personalDone?.asSequence()
            ?.filter { it.occurrenceMillis > 0 && it.doneAtMillis > 0 &&
                !Instant.ofEpochMilli(it.doneAtMillis).isAfter(now) }
            ?.distinctBy { it.stableKey }
            ?.filter { event ->
                val date = Instant.ofEpochMilli(event.doneAtMillis)
                    .atZone(displayZone).toLocalDate()
                !date.isBefore(first) && !date.isAfter(dateToday)
            }
            ?.toList()

        // A challenge confirmation belongs to the *selected challenge day*.
        // Flexible backfill can be recorded later; never relabel the target
        // day as the timestamp the owner actually confirmed it.
        val confirmedDays = challenges?.sumOf { challenge ->
            val challengeToday = now.atZone(ZoneId.of(challenge.zoneId)).toLocalDate()
            val firstChallengeDay = challengeToday.minusDays(6)
            challenge.confirmations.keys.count { day ->
                !day.isBefore(firstChallengeDay) && !day.isAfter(challengeToday)
            }
        }
        val reflectionDays = challenges?.sumOf { challenge ->
            val challengeToday = now.atZone(ZoneId.of(challenge.zoneId)).toLocalDate()
            val firstChallengeDay = challengeToday.minusDays(6)
            challenge.dayNotes.count { (day, note) ->
                note.isNotBlank() && !day.isBefore(firstChallengeDay) &&
                    !day.isAfter(challengeToday)
            }
        }
        return UX07EPersonalProgressSnapshot(
            personalDoneLastSeven = validReceipts?.size,
            personalActiveDaysLastSeven = validReceipts?.map {
                Instant.ofEpochMilli(it.doneAtMillis).atZone(displayZone).toLocalDate()
            }?.toSet()?.size,
            challengeDaysConfirmedLastSeven = confirmedDays,
            challengeReflectionDaysLastSeven = reflectionDays,
        )
    }

    /** Read-only note previews. A saved note does NOT prove its day was done. */
    fun recentReflections(challenge: Phase1HabitChallenge, limit: Int = 3): List<UX07EChallengeReflection> {
        require(limit in 0..21)
        return challenge.dayNotes.asSequence()
            .filter { (day, note) ->
                note.isNotBlank() && !day.isBefore(challenge.startDate) &&
                    !day.isAfter(challenge.endDate)
            }
            .sortedByDescending { (day, _) -> day }
            .take(limit)
            .map { (day, note) ->
                UX07EChallengeReflection(
                    day = day,
                    note = note,
                    confirmedAt = challenge.confirmations[day],
                )
            }
            .toList()
    }
}
