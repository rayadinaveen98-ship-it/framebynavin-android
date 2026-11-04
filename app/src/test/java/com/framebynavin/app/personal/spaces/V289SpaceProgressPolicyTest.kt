package com.framebynavin.app.personal.spaces

import java.time.LocalDate
import java.time.ZoneId
import org.junit.Assert.assertEquals
import org.junit.Test

/** Display-only progress: never counts notifications, future dates or inferred failures. */
class V289SpaceProgressPolicyTest {
    private val zone = ZoneId.of("Asia/Kolkata")
    private val day = LocalDate.of(2026, 11, 4)
    private val now = day.atTime(19, 0).atZone(zone).toInstant().toEpochMilli()

    private fun record(
        id: String,
        date: LocalDate,
        source: String = V289SpaceEntrySource.MANUAL.name,
        kind: String = V289SpaceEntryKind.CHECK_IN.name,
    ) = V289SpaceEntryEntity(
        id = id,
        spaceId = "30000000-0000-4000-8000-000000000001",
        kind = kind,
        source = source,
        occurredAtMillis = date.atTime(18, 0).atZone(zone).toInstant().toEpochMilli(),
        createdAtMillis = now,
        note = "",
        metricName = null,
        metricValue = null,
        metricUnit = null,
        reminderId = null,
        originalOccurrenceMillis = null,
    )

    @Test fun emptyProgressIsZeroWithoutInventedDone() {
        assertEquals(
            V289SpaceProgressSummary(0, 0, 0, 0),
            V289SpaceProgressPolicy.summarize(emptyList(), now, zone)
        )
    }

    @Test fun multipleCheckInsSameDayCountAsOneActiveDay() {
        val records = listOf(
            record("a", day),
            record("b", day),
            record("c", day.minusDays(1)),
        )
        val summary = V289SpaceProgressPolicy.summarize(records, now, zone)
        assertEquals(3, summary.totalRecordedEntries)
        assertEquals(3, summary.manualCheckIns)
        assertEquals(0, summary.confirmedDoneReceipts)
        assertEquals(2, summary.daysWithActivityLastSeven)
    }

    @Test fun oldAndFutureEventsDoNotInflateCurrentProgress() {
        val records = listOf(
            record("old", day.minusDays(7)),
            record("future", day.plusDays(1)),
            record("today", day),
            record("note", day.minusDays(1), kind = V289SpaceEntryKind.NOTE.name),
        )
        val summary = V289SpaceProgressPolicy.summarize(records, now, zone)
        assertEquals(3, summary.totalRecordedEntries)
        assertEquals(2, summary.daysWithActivityLastSeven)
        assertEquals(2, summary.manualCheckIns)
    }

    @Test fun confirmedDoneAndManualAreDifferentCounts() {
        val records = listOf(
            record("manual", day),
            record("confirmed", day,
                source = V289SpaceEntrySource.CONFIRMED_REMINDER_DONE.name),
        )
        val summary = V289SpaceProgressPolicy.summarize(records, now, zone)
        assertEquals(1, summary.manualCheckIns)
        assertEquals(1, summary.confirmedDoneReceipts)
        assertEquals(1, summary.daysWithActivityLastSeven)
    }
}
