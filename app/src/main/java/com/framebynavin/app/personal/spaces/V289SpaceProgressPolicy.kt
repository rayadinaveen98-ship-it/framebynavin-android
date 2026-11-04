package com.framebynavin.app.personal.spaces

import java.time.Instant
import java.time.ZoneId

/** Read-only display projection: never infers a check-in from notification delivery. */
internal data class V289SpaceProgressSummary(
    val totalRecordedEntries: Int,
    val manualCheckIns: Int,
    val confirmedDoneReceipts: Int,
    val daysWithActivityLastSeven: Int,
)

internal object V289SpaceProgressPolicy {
    fun summarize(
        entries: List<V289SpaceEntryEntity>,
        nowMillis: Long,
        zone: ZoneId = ZoneId.systemDefault(),
    ): V289SpaceProgressSummary {
        val currentDay = Instant.ofEpochMilli(nowMillis).atZone(zone).toLocalDate()
        // An incorrectly dated future entry must not inflate today's progress.
        val logged = entries.filter { it.occurredAtMillis in 1..nowMillis }
        val localDays = logged.map {
            Instant.ofEpochMilli(it.occurredAtMillis).atZone(zone).toLocalDate()
        }.filter { day ->
            !day.isBefore(currentDay.minusDays(6)) && !day.isAfter(currentDay)
        }.toSet()
        return V289SpaceProgressSummary(
            totalRecordedEntries = logged.size,
            manualCheckIns = logged.count {
                it.source == V289SpaceEntrySource.MANUAL.name &&
                    it.kind == V289SpaceEntryKind.CHECK_IN.name
            },
            confirmedDoneReceipts = logged.count {
                it.source == V289SpaceEntrySource.CONFIRMED_REMINDER_DONE.name &&
                    it.kind == V289SpaceEntryKind.CHECK_IN.name
            },
            daysWithActivityLastSeven = localDays.size,
        )
    }
}
