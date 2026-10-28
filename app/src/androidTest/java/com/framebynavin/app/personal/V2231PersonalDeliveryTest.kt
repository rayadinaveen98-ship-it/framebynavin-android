package com.framebynavin.app.personal

import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.framebynavin.app.data.HardeningTestEnvironment
import com.framebynavin.app.data.TaskStore
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Actual Android AlarmManager identities, encrypted-by-app-sandbox Personal
 * DataStore journal and duplicate/stale user-response boundaries.
 * Tests use synthetic future dates. No creator data is mutated.
 */
@RunWith(AndroidJUnit4::class)
class V2231PersonalDeliveryTest {
    private val app get() = InstrumentationRegistry.getInstrumentation().targetContext.applicationContext
    private val zone = ZoneId.of("Asia/Kolkata")

    private fun future(tag: String, repeat: PersonalRepeat = PersonalRepeat.ONCE) = PersonalReminder(
        id = "v2231:$tag",
        title = "Private daily-life reminder",
        startDate = LocalDate.now(zone).plusDays(4),
        time = LocalTime.of(19, 45),
        zoneId = zone,
        repeat = repeat,
        alert = PersonalAlert.NOTIFICATION,
        createdAtMillis = 1234L,
        updatedAtMillis = 1234L,
    )

    @Test fun staleActionCannotClaimNewSnoozeOrChangeCreatorProjects() = runBlocking {
        HardeningTestEnvironment.requireCiEmulator()
        val store = PersonalReminderStore(app)
        val coordinator = PersonalDeliveryCoordinator(app)
        store.mutate { emptyList() }
        val creatorBefore = TaskStore(app).load()
        try {
            store.create(future("snooze"))
            coordinator.reconcile()
            val first = store.load().single()
            assertTrue(first.pendingOccurrenceMillis > System.currentTimeMillis())
            assertTrue(first.pendingToken.isNotBlank())
            assertEquals(first.pendingOccurrenceMillis, first.pendingFireAtMillis)
            assertEquals(PersonalApplyResult.NOT_PRESENTED, coordinator.apply(
                first.id, first.pendingOccurrenceMillis, first.pendingToken,
                PersonalDeliveryContract.ACTION_DONE,
            ))
            store.update(first.id) { it.copy(presentedToken = first.pendingToken) }
            val accepted = coordinator.apply(
                first.id, first.pendingOccurrenceMillis, first.pendingToken,
                PersonalDeliveryContract.ACTION_SNOOZE,
            )
            assertEquals(PersonalApplyResult.ACCEPTED, accepted)
            val afterSnooze = store.load().single()
            assertNotEquals(first.pendingToken, afterSnooze.pendingToken)
            assertEquals(first.pendingOccurrenceMillis, afterSnooze.pendingOccurrenceMillis)
            assertEquals(PersonalResponse.SNOOZED, afterSnooze.lastResponse)
            assertEquals(0L, afterSnooze.lastAcknowledgedOccurrenceMillis)
            assertEquals(PersonalApplyResult.STALE, coordinator.apply(
                first.id, first.pendingOccurrenceMillis, first.pendingToken,
                PersonalDeliveryContract.ACTION_DONE,
            ))
            store.update(first.id) { it.copy(presentedToken = it.pendingToken) }
            val live = store.load().single()
            assertEquals(PersonalApplyResult.ACCEPTED, coordinator.apply(
                live.id, live.pendingOccurrenceMillis, live.pendingToken,
                PersonalDeliveryContract.ACTION_DONE,
            ))
            val done = store.load().single()
            assertEquals(PersonalResponse.DONE, done.lastResponse)
            assertEquals(first.pendingOccurrenceMillis, done.lastAcknowledgedOccurrenceMillis)
            assertTrue(done.pendingToken.isEmpty())
            assertEquals(PersonalApplyResult.STALE, coordinator.apply(
                live.id, live.pendingOccurrenceMillis, live.pendingToken,
                PersonalDeliveryContract.ACTION_DONE,
            ))
            assertEquals(creatorBefore, TaskStore(app).load())
        } finally {
            coordinator.cancelPending("v2231:snooze")
            store.mutate { emptyList() }
            PersonalReminderNotifications.cancel(app, "v2231:snooze")
        }
    }

    @Test fun explicitDismissIsNotDoneAndRecurringSeriesSchedulesNextDay() = runBlocking {
        HardeningTestEnvironment.requireCiEmulator()
        val store = PersonalReminderStore(app)
        val coordinator = PersonalDeliveryCoordinator(app)
        store.mutate { emptyList() }
        try {
            store.create(future("repeat", repeat = PersonalRepeat.DAILY))
            coordinator.reconcile()
            val scheduled = store.load().single()
            store.update(scheduled.id) { it.copy(presentedToken = scheduled.pendingToken) }
            assertEquals(PersonalApplyResult.ACCEPTED, coordinator.apply(
                scheduled.id, scheduled.pendingOccurrenceMillis, scheduled.pendingToken,
                PersonalDeliveryContract.ACTION_DISMISS,
            ))
            val current = store.load().single()
            assertEquals(PersonalResponse.DISMISSED, current.lastResponse)
            assertFalse(current.lastResponse == PersonalResponse.DONE)
            assertEquals(scheduled.pendingOccurrenceMillis, current.lastAcknowledgedOccurrenceMillis)
            assertEquals(
                scheduled.pendingOccurrenceMillis + 24L * 60 * 60 * 1000,
                current.pendingOccurrenceMillis,
            )
            assertTrue(current.pendingToken.isNotBlank())
        } finally {
            coordinator.cancelPending("v2231:repeat")
            store.mutate { emptyList() }
        }
    }

    @Test fun rescheduleNeedsValidFutureTimeAndTokenKeepsOriginalOccurrence() = runBlocking {
        HardeningTestEnvironment.requireCiEmulator()
        val store = PersonalReminderStore(app)
        val coordinator = PersonalDeliveryCoordinator(app)
        store.mutate { emptyList() }
        try {
            store.create(future("reschedule"))
            coordinator.reconcile()
            val original = store.load().single()
            store.update(original.id) { it.copy(presentedToken = original.pendingToken) }
            val now = System.currentTimeMillis()
            assertEquals(PersonalApplyResult.INVALID_TIME, coordinator.apply(
                original.id, original.pendingOccurrenceMillis, original.pendingToken,
                PersonalDeliveryContract.ACTION_RESCHEDULE, now, now - 100,
            ))
            assertEquals(original.pendingToken, store.load().single().pendingToken)
            assertEquals(PersonalApplyResult.ACCEPTED, coordinator.apply(
                original.id, original.pendingOccurrenceMillis, original.pendingToken,
                PersonalDeliveryContract.ACTION_RESCHEDULE, now, now + 2L * 60 * 60 * 1000,
            ))
            val moved = store.load().single()
            assertEquals(PersonalResponse.RESCHEDULED, moved.lastResponse)
            assertEquals(original.pendingOccurrenceMillis, moved.pendingOccurrenceMillis)
            assertEquals(now + 2L * 60 * 60 * 1000, moved.pendingFireAtMillis)
            assertNotEquals(original.pendingToken, moved.pendingToken)
        } finally {
            coordinator.cancelPending("v2231:reschedule")
            store.mutate { emptyList() }
        }
    }
}
