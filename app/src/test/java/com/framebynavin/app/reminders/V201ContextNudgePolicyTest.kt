package com.framebynavin.app.reminders

import java.time.LocalDateTime
import java.time.ZoneId
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class V201ContextNudgePolicyTest {
    private val zone = ZoneId.of("Asia/Kolkata")

    private fun at(day: Int, hour: Int, minute: Int = 0): Long =
        LocalDateTime.of(2026, 10, day, hour, minute).atZone(zone).toInstant().toEpochMilli()

    private fun eligible(
        now: Long,
        last: Long = 0L,
        oldKey: String = "",
        newKey: String = "project:next",
        quiet: Boolean = false,
        quietStart: Int = 22 * 60,
        quietEnd: Int = 8 * 60,
    ) = V201ContextNudgePolicy.mayShow(
        nowMillis = now,
        lastShownMillis = last,
        lastKey = oldKey,
        candidateKey = newKey,
        quietHoursEnabled = quiet,
        quietStartMinute = quietStart,
        quietEndMinute = quietEnd,
        zone = zone,
    )

    @Test fun respectsDaytimeEvenWhenQuietHoursDisabled() {
        assertFalse(eligible(at(21, 8, 59)))
        assertTrue(eligible(at(21, 9)))
        assertTrue(eligible(at(21, 19, 59)))
        assertFalse(eligible(at(21, 20)))
        assertFalse(eligible(at(21, 23)))
    }

    @Test fun honorsUsersCustomQuietWindow() {
        assertFalse(eligible(at(21, 11), quiet = true, quietStart = 10 * 60, quietEnd = 12 * 60))
        assertTrue(eligible(at(21, 12), quiet = true, quietStart = 10 * 60, quietEnd = 12 * 60))
    }

    @Test fun globallyLimitsDifferentMessagesToOnePer24Hours() {
        val previous = at(20, 12)
        assertFalse(eligible(at(21, 11), previous, oldKey = "a", newKey = "b"))
        assertTrue(eligible(at(21, 12), previous, oldKey = "a", newKey = "b"))
    }

    @Test fun repeatedMessageNeeds72Hours() {
        val previous = at(18, 12)
        assertFalse(eligible(at(20, 12), previous, oldKey = "a", newKey = "a"))
        assertTrue(eligible(at(21, 12), previous, oldKey = "a", newKey = "a"))
    }

    @Test fun emptyKeyAndClockRollbackFailClosed() {
        assertFalse(eligible(at(21, 12), newKey = " "))
        assertFalse(eligible(at(21, 12), at(22, 12), oldKey = "a", newKey = "b"))
    }
}
