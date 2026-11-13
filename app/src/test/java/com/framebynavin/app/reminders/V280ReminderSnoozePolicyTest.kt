package com.framebynavin.app.reminders

import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Test

class V280ReminderSnoozePolicyTest {
    @Test fun projectOverridesGlobalAndLegacyKeepsGlobal() {
        assertEquals(5, V280ReminderSnoozePolicy.effectiveCreatorMinutes(5, 10))
        assertEquals(30, V280ReminderSnoozePolicy.effectiveCreatorMinutes(30, 15))
        assertEquals(15, V280ReminderSnoozePolicy.effectiveCreatorMinutes(0, 15))
        assertEquals(10, V280ReminderSnoozePolicy.effectiveCreatorMinutes(999, 10))
        assertEquals(10, V280ReminderSnoozePolicy.effectiveCreatorMinutes(0, 6))
        assertEquals(10, V280ReminderSnoozePolicy.effectiveCreatorMinutes(7, 90))
    }

    @Test fun userSnoozeStartsAtResponseTime() {
        val clickAt = 1_900_000_000_000L
        assertEquals(clickAt + 5L * 60_000L,
            V280ReminderSnoozePolicy.nextAtMillis(clickAt, 5))
        assertEquals(clickAt + 30L * 60_000L,
            V280ReminderSnoozePolicy.nextAtMillis(clickAt, 30))
    }

    @Test fun invalidMinutesNeverSchedule() {
        for (invalid in listOf(0, 6, 7, 29, 31, 120)) {
            assertThrows(IllegalArgumentException::class.java) {
                V280ReminderSnoozePolicy.nextAtMillis(100L, invalid)
            }
        }
    }
}
