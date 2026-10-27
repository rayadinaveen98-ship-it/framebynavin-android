package com.framebynavin.app.youtube

import org.junit.Assert.*
import org.junit.Test

class YouTubeReportEvidenceV187Test {
    @Test fun missingReportIsNotReportedZero() {
        val e = YouTubeReportEvidence(false, false, emptySet(), emptySet(), emptyList())
        assertFalse(e.hasMetric("views"))
        assertFalse(e.canCompare("views"))
        assertNull(e.lastReturnedDay)
    }

    @Test fun reportedZeroIsStillAvailable() {
        val e = YouTubeReportEvidence(
            true, true, setOf("views", "estimatedMinutesWatched"),
            setOf("views"), listOf("2026-10-12"),
        )
        assertTrue(e.hasMetric("views"))
        assertTrue(e.canCompare("views"))
        assertFalse(e.canCompare("estimatedMinutesWatched"))
    }

    @Test fun requestedPeriodDoesNotInventReturnedDays() {
        val e = YouTubeReportEvidence(
            true, false, setOf("views"), emptySet(),
            listOf("2026-10-01", "2026-10-07"),
        )
        assertEquals(2, e.returnedDailyDates.size)
        assertEquals("2026-10-01", e.firstReturnedDay)
        assertEquals("2026-10-07", e.lastReturnedDay)
        assertFalse(e.canCompare("views"))
    }
}
