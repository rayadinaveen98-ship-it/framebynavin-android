package com.framebynavin.app.ui

import com.framebynavin.app.data.ProgressJourneyEvent
import com.framebynavin.app.data.ProgressJourneyKind
import java.time.LocalDate
import java.time.YearMonth
import java.time.ZoneId
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * UX08B source test coverage; founder requested all JVM/instrumentation
 * execution after the full UX01–UX10 source staging and P0 reconciliation.
 */
class UX08BProgressPolicyTest {
    private val zone = ZoneId.of("UTC")
    private fun event(
        key: String, kind: ProgressJourneyKind,
        day: Int, sourceId: String = "source",
    ): ProgressJourneyEvent = ProgressJourneyEvent(
        key = key,
        kind = kind,
        title = "Recorded work",
        description = "Saved action",
        atMillis = LocalDate.of(2026, 11, day)
            .atTime(9, 0).atZone(zone).toInstant().toEpochMilli(),
        sourceId = sourceId,
    )

    @Test
    fun `genuine event summary distinguishes recorded days and private Done`() {
        val events = listOf(
            event("p1", ProgressJourneyKind.PROJECT_CREATED, 16),
            event("i1", ProgressJourneyKind.IDEA_CAPTURED, 16),
            event("pr1", ProgressJourneyKind.PERSONAL_COMPLETED, 17),
        )
        assertEquals(
            UX08BJourneySummary(
                recordedEvents = 3, recordedDays = 2,
                creatorEvents = 2, personalDoneReceipts = 1,
            ),
            UX08BProgressPolicy.summarize(events, zone),
        )
        assertEquals(
            UX08BJourneySummary(0, 0, 0, 0),
            UX08BProgressPolicy.summarize(emptyList(), zone),
        )
    }

    @Test
    fun `month filter uses actual event date and does not mutate records`() {
        val november = event("nov", ProgressJourneyKind.STAGE_MOVED, 17)
        val december = november.copy(
            key = "dec",
            atMillis = LocalDate.of(2026, 12, 1)
                .atTime(9, 0).atZone(zone).toInstant().toEpochMilli(),
        )
        val items = listOf(december, november)
        assertEquals(
            listOf(november),
            UX08BProgressPolicy.forMonth(items, YearMonth.of(2026, 11), zone),
        )
        assertEquals(2, items.size)
    }

    @Test
    fun `week arrows move seven days while month arrows navigate month starts`() {
        val day = LocalDate.of(2026, 11, 30)
        val month = YearMonth.from(day)
        assertEquals(
            LocalDate.of(2026, 12, 7) to YearMonth.of(2026, 12),
            UX08BProgressPolicy.shiftPeriod("WEEK", day, month, 1L),
        )
        assertEquals(
            LocalDate.of(2026, 10, 1) to YearMonth.of(2026, 10),
            UX08BProgressPolicy.shiftPeriod("MONTH", day, month, -1L),
        )
    }

    @Test
    fun `deleted source and private Done have no Creator destination`() {
        val idea = event("idea", ProgressJourneyKind.IDEA_CAPTURED, 17, "idea-1")
        val project = event("project", ProgressJourneyKind.PROJECT_PUBLISHED, 17, "proj-1")
        val privateDone = event("private", ProgressJourneyKind.PERSONAL_COMPLETED, 17, "personal-1")
        assertTrue(
            UX08BProgressPolicy.canOpen(idea, emptySet(), setOf("idea-1")),
        )
        assertTrue(
            UX08BProgressPolicy.canOpen(project, setOf("proj-1"), emptySet()),
        )
        assertFalse(
            UX08BProgressPolicy.canOpen(idea, emptySet(), emptySet()),
        )
        assertFalse(
            UX08BProgressPolicy.canOpen(project, emptySet(), emptySet()),
        )
        assertFalse(
            UX08BProgressPolicy.canOpen(privateDone, setOf("personal-1"), setOf("personal-1")),
        )
    }

    @Test
    fun `event labels preserve source meaning and do not equate view with XP`() {
        assertEquals(
            "PROJECT STAGE",
            UX08BProgressPolicy.sourceLabel(ProgressJourneyKind.STAGE_MOVED),
        )
        assertEquals(
            "PRIVATE PERSONAL DONE",
            UX08BProgressPolicy.sourceLabel(ProgressJourneyKind.PERSONAL_COMPLETED),
        )
    }
}
