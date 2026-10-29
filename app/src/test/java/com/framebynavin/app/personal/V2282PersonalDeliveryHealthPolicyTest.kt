package com.framebynavin.app.personal

import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class V2282PersonalDeliveryHealthPolicyTest {
    private val zone = ZoneId.of("Asia/Kolkata")
    private val today = LocalDate.of(2026, 10, 29)
    private val now = today.atTime(12, 0).atZone(zone).toInstant().toEpochMilli()
    private val token = "ed378a35-92ac-4946-b1f4-7b9361f26b1a"

    private fun reminder() = PersonalReminder(
        id = "personal-delivery-health-fixture",
        title = "Drink water", startDate = today,
        time = LocalTime.of(11, 0), zoneId = zone, repeat = PersonalRepeat.DAILY,
    )

    private fun lease(fireAt: Long) = reminder().copy(
        pendingOccurrenceMillis = now - 3_600_000L,
        pendingFireAtMillis = fireAt,
        pendingToken = token,
    )

    @Test fun postedNotificationStillRequiresUserAction() {
        val posted = lease(now - 8 * 60_000L).copy(presentedToken = token)
        val state = PersonalDeliveryHealthPolicy.status(posted, now)
        assertEquals(PersonalDeliveryHealthState.AWAITING_RESPONSE, state.state)
        assertTrue(state.needsAttention)
        assertEquals(PersonalResponse.NONE, posted.lastResponse)
        assertEquals(0L, posted.lastAcknowledgedOccurrenceMillis)
    }

    @Test fun overdueIsUnconfirmedNotFalselyDelivered() {
        val due = PersonalDeliveryHealthPolicy.status(lease(now - 2 * 60_000L), now)
        val late = PersonalDeliveryHealthPolicy.status(lease(now - 8 * 60_000L), now)
        assertEquals(PersonalDeliveryHealthState.DUE_UNCONFIRMED, due.state)
        assertEquals(PersonalDeliveryHealthState.LATE_UNCONFIRMED, late.state)
        assertTrue(late.label.contains("not confirmed"))
        assertTrue(late.needsAttention)
    }

    @Test fun missedAndUserDeferredAreDistinct() {
        val missed = lease(now + 20_000L).copy(
            lastMissedOccurrenceMillis = now - 2 * 3_600_000L)
        assertEquals(PersonalDeliveryHealthState.EARLIER_MISSED,
            PersonalDeliveryHealthPolicy.status(missed, now).state)
        val snoozed = lease(now + 10 * 60_000L).copy(
            snoozedUntilMillis = now + 10 * 60_000L,
            lastResponse = PersonalResponse.SNOOZED,
        )
        assertEquals(PersonalDeliveryHealthState.USER_DEFERRED,
            PersonalDeliveryHealthPolicy.status(snoozed, now).state)
        assertFalse(PersonalDeliveryHealthPolicy.status(snoozed, now).needsAttention)
    }

    @Test fun pausedUpcomingAndNoFutureDoNotClaimDelivery() {
        assertEquals(PersonalDeliveryHealthState.PAUSED,
            PersonalDeliveryHealthPolicy.status(
                lease(now - 10 * 60_000L).copy(paused = true), now).state)
        assertEquals(PersonalDeliveryHealthState.UPCOMING,
            PersonalDeliveryHealthPolicy.status(lease(now + 15 * 60_000L), now).state)
        assertEquals(PersonalDeliveryHealthState.NO_UPCOMING,
            PersonalDeliveryHealthPolicy.status(reminder().copy(
                startDate = today.minusDays(1), repeat = PersonalRepeat.ONCE), now).state)
    }

    @Test fun resultDoesNotMutateReminder() {
        val original = lease(now - 10 * 60_000L)
        assertEquals(PersonalDeliveryHealthPolicy.status(original, now),
            PersonalDeliveryHealthPolicy.status(original, now))
        assertEquals("", original.presentedToken)
        assertEquals(PersonalResponse.NONE, original.lastResponse)
        assertEquals(0L, original.lastAcknowledgedOccurrenceMillis)
    }
}
