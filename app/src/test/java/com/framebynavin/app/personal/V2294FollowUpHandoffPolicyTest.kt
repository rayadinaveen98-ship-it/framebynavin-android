package com.framebynavin.app.personal

import java.time.LocalDate
import java.time.LocalTime
import org.junit.Assert.assertEquals
import org.junit.Test

class V2294FollowUpHandoffPolicyTest {
    private val oldToken = "a1b2c3d4-0000-4000-9000-0123456789ab"
    private val newToken = "b1b2c3d4-0000-4000-9000-0123456789ab"
    private val sample = PersonalReminder(
        title = "Medication",
        startDate = LocalDate.of(2026, 10, 29),
        time = LocalTime.of(10, 0),
        pendingOccurrenceMillis = 1_800_000_000_000L,
        pendingFireAtMillis = 1_800_000_000_000L,
        pendingToken = oldToken,
        presentedToken = oldToken,
        presentedAtMillis = 1_800_000_000_000L,
    )

    @Test fun noSavedReminderOrUnpostedLeaseCancelsStaleNotification() {
        assertEquals(PersonalFollowUpHandoffRepair.CANCEL_STALE_CARD,
            PersonalFollowUpHandoffPolicy.repair(null))
        assertEquals(PersonalFollowUpHandoffRepair.CANCEL_STALE_CARD,
            PersonalFollowUpHandoffPolicy.repair(sample.copy(
                pendingToken = "", presentedToken = "",
                pendingOccurrenceMillis = 0L, pendingFireAtMillis = 0L,
            )))
        assertEquals(PersonalFollowUpHandoffRepair.CANCEL_STALE_CARD,
            PersonalFollowUpHandoffPolicy.repair(sample.copy(
                pendingToken = newToken, presentedToken = "",
            )))
    }

    @Test fun pausedRecordNeverGetsRestored() {
        assertEquals(PersonalFollowUpHandoffRepair.CANCEL_STALE_CARD,
            PersonalFollowUpHandoffPolicy.repair(sample.copy(paused = true)))
    }

    @Test fun olderOrNewerAlreadyPostedReminderGetsActionableCardRestored() {
        assertEquals(PersonalFollowUpHandoffRepair.RESTORE_LATEST_POSTED_CARD,
            PersonalFollowUpHandoffPolicy.repair(sample))
        assertEquals(PersonalFollowUpHandoffRepair.RESTORE_LATEST_POSTED_CARD,
            PersonalFollowUpHandoffPolicy.repair(sample.copy(
                pendingToken = newToken, presentedToken = newToken,
            )))
    }
}
