package com.framebynavin.app.personal

import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.LocalDate
import java.time.LocalTime
import java.util.UUID

class V279PersonalPresentationReceiptTest {
    private val token = UUID.randomUUID().toString()
    private val sample = PersonalReminder(
        id="v279:presentation-synthetic", title="Synthetic only",
        startDate=LocalDate.of(2026,11,1), time=LocalTime.of(10,0),
        alert=PersonalAlert.ALARM,
        pendingOccurrenceMillis=1000L, pendingFireAtMillis=1000L,
        pendingToken=token,
    )
    @Test fun justPostedAlarmWaitsForExactReceiptInsteadOfShowingStale() {
        assertEquals(PersonalAlertPresentationState.WAITING,
            PersonalAlertPresentationPolicy.state(sample, sample.id, token))
        assertEquals(PersonalAlertPresentationState.READY,
            PersonalAlertPresentationPolicy.state(sample.copy(presentedToken=token), sample.id, token))
    }

    @Test fun staleRotatedOrPausedOccurrenceCannotBecomeActionable() {
        assertEquals(PersonalAlertPresentationState.INVALID,
            PersonalAlertPresentationPolicy.state(null, sample.id, token))
        assertEquals(PersonalAlertPresentationState.INVALID,
            PersonalAlertPresentationPolicy.state(sample, sample.id, UUID.randomUUID().toString()))
        assertEquals(PersonalAlertPresentationState.INVALID,
            PersonalAlertPresentationPolicy.state(sample.copy(paused=true), sample.id, token))
    }

    @Test fun waitForReceiptIsStrictlyBounded() {
        assertEquals(10,PersonalAlertPresentationPolicy.MAX_READ_ATTEMPTS)
        assertEquals(150L,PersonalAlertPresentationPolicy.RETRY_DELAY_MILLIS)
    }
}
