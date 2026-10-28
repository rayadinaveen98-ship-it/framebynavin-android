package com.framebynavin.app.widget

import com.framebynavin.app.personal.PersonalAlert
import com.framebynavin.app.personal.PersonalReminder
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class V224WidgetPolicyTest {
    private val now = 1_800_000_000_000L

    private fun record(id: String, at: Long) = PersonalReminder(
        id = id,
        title = "PRIVATE: never show on launcher",
        notes = "Secret personal notes must not leave Backlot",
        startDate = LocalDate.of(2027, 1, 1),
        time = LocalTime.of(8, 0),
        zoneId = ZoneId.of("Asia/Kolkata"),
        alert = PersonalAlert.VOICE,
        pendingOccurrenceMillis = at,
        pendingFireAtMillis = at,
        pendingToken = "b6a4e3af-7a2b-4b8d-b039-76b91e3406a0",
    )

    @Test fun launcherPersonalSummaryContainsOnlyCountsAndTimes() {
        val early = record("personal:one", now + 10_000)
        val late = record("personal:two", now + 60_000)
        val paused = record("personal:paused", now + 20_000).copy(paused = true)
        val summary = V224WidgetPolicy.personalSummary(listOf(late, paused, early), now)
        assertEquals(2, summary.upcomingCount)
        assertEquals(now + 10_000, summary.nextAtMillis)
        val text = V224WidgetPolicy.personalSafeLabel(summary)
        assertTrue(text.contains("2 upcoming"))
        assertFalse(text.contains("PRIVATE"))
        assertFalse(text.contains("Secret"))
    }

    @Test fun displayedButUnansweredOneTimeIsNotClaimedAsUpcoming() {
        val posted = record("personal:posted", now - 5_000)
            .copy(presentedToken = "b6a4e3af-7a2b-4b8d-b039-76b91e3406a0")
        val summary = V224WidgetPolicy.personalSummary(listOf(posted), now)
        assertEquals(0, summary.upcomingCount)
        assertNull(summary.nextAtMillis)
    }

    @Test fun unreadablePersonalStoreMustNotBeShownAsZero() {
        assertTrue(V224WidgetPolicy.personalSafeLabel(null).contains("Open to check"))
        assertFalse(V224WidgetPolicy.personalSafeLabel(null).contains("Nothing upcoming"))
    }
}
