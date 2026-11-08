package com.framebynavin.app.personal

import java.time.Instant
import java.time.LocalDate
import java.time.LocalTime
import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Pure JUnit tests; no AlarmManager, receiver, saved owner records, migration,
 * alerts, cloud calls, or notification side effects.
 */
class Phase1HabitAlarmPlanTest {
    private val first = LocalDate.of(2026, 11, 7)
    private val beforeFirst = Instant.parse("2026-11-07T00:00:00Z")
    private val afterFirst = Instant.parse("2026-11-07T04:00:00Z")
    private val beforeSecond = Instant.parse("2026-11-08T00:00:00Z")
    private fun habit() = Phase1HabitChallenge(
        id = "policy-test",
        activity = "Read 20 minutes",
        startDate = first,
        zoneId = "Asia/Kolkata",
        alertTime = LocalTime.of(7, 30),
        alertKind = Phase1HabitAlertKind.ALARM,
    ).checked()

    private fun firstLease(
        stage: Phase1HabitLeaseStage = Phase1HabitLeaseStage.ARMED,
    ) = Phase1HabitAlarmLease(
        scheduleKey = habit().scheduleKey,
        day = first,
        fireAt = Instant.parse("2026-11-07T02:00:00Z"),
        kind = Phase1HabitAlertKind.ALARM,
        token = "fresh-token-123",
        stage = stage,
    )

    @Test fun newChallengeProducesExactlyOneFutureTarget() {
        val plan = Phase1HabitAlarmPlanner.plan(habit(), null, beforeFirst)
        assertTrue(plan is Phase1HabitAlarmPlan.Install)
        val install = plan as Phase1HabitAlarmPlan.Install
        assertEquals(first, install.target.day)
        assertEquals("phase1-habit-policy-test", install.target.scheduleKey)
        assertEquals(Instant.parse("2026-11-07T02:00:00Z"), install.target.fireAt)
        assertEquals(null, install.replacing)
    }

    @Test fun sameFutureLeaseIsKeptOnRepeatedReconciliation() {
        val lease = firstLease()
        assertEquals(Phase1HabitAlarmPlan.Keep(lease),
            Phase1HabitAlarmPlanner.plan(habit(), lease, beforeFirst))
        assertEquals(Phase1HabitAlarmPlan.Keep(lease),
            Phase1HabitAlarmPlanner.plan(habit(), lease, beforeFirst))
    }

    @Test fun confirmedTodayReplacesUpcomingLeaseWithoutClaimingAutoDone() {
        val confirmed = habit().confirmToday(afterFirst)
        val plan = Phase1HabitAlarmPlanner.plan(confirmed, firstLease(), beforeFirst)
        assertTrue(plan is Phase1HabitAlarmPlan.Install)
        val install = plan as Phase1HabitAlarmPlan.Install
        assertEquals(first.plusDays(1), install.target.day)
        assertEquals(beforeSecond.plusSeconds(2 * 3600), install.target.fireAt)
        assertEquals(firstLease(), install.replacing)
        assertEquals(1, confirmed.confirmations.size)
    }

    @Test fun pauseAndNoAlertCancelFutureLeaseButDoNotMarkProgress() {
        val lease = firstLease()
        assertEquals(Phase1HabitAlarmPlan.Cancel(lease),
            Phase1HabitAlarmPlanner.plan(habit().setPaused(true), lease, beforeFirst))
        val noAlert = habit().copy(alertTime = null, alertKind = null).checked()
        assertEquals(Phase1HabitAlarmPlan.Cancel(lease),
            Phase1HabitAlarmPlanner.plan(noAlert, lease, beforeFirst))
        assertTrue(noAlert.confirmations.isEmpty())
        assertEquals(Phase1HabitAlarmPlan.Idle,
            Phase1HabitAlarmPlanner.plan(noAlert, null, beforeFirst))
    }

    @Test fun noAlertIsScheduledAfterDayTwentyOne() {
        val afterEnd = Instant.parse("2026-11-28T04:00:00Z")
        assertEquals(Phase1HabitAlarmPlan.Idle,
            Phase1HabitAlarmPlanner.plan(habit(), null, afterEnd))
    }

    @Test fun deliveredOccurrenceCannotBeReplacedOrCancelledUntilRecovery() {
        val presented = firstLease(Phase1HabitLeaseStage.PRESENTED)
        val changed = habit().setPaused(true)
        assertEquals(Phase1HabitAlarmPlan.HoldUnresolved(presented),
            Phase1HabitAlarmPlanner.plan(changed, presented, afterFirst))
        assertTrue(changed.confirmations.isEmpty())
    }

    @Test fun overdueUnacknowledgedOccurrenceIsHeldNotSilentlySkipped() {
        val lease = firstLease()
        val plan = Phase1HabitAlarmPlanner.plan(habit(), lease, afterFirst)
        assertEquals(Phase1HabitAlarmPlan.HoldUnresolved(lease), plan)
        assertTrue(habit().confirmations.isEmpty())
    }

    @Test fun identityCollisionOrCorruptDateIsRefused() {
        val wrongOwner = firstLease().copy(scheduleKey = "phase1-habit-someone-else")
        assertThrows(IllegalArgumentException::class.java) {
            Phase1HabitAlarmPlanner.plan(habit(), wrongOwner, beforeFirst)
        }
        val wrongDay = firstLease().copy(day = first.plusDays(2))
        assertThrows(IllegalArgumentException::class.java) {
            Phase1HabitAlarmPlanner.plan(habit(), wrongDay, beforeFirst)
        }
        val weakToken = firstLease().copy(token = "x")
        assertThrows(IllegalArgumentException::class.java) {
            Phase1HabitAlarmPlanner.plan(habit(), weakToken, beforeFirst)
        }
    }

    @Test fun recurrenceChangeCannotSilentlyReuseOldPendingLease() {
        val newTime = habit().copy(alertTime = LocalTime.of(8, 45)).checked()
        val plan = Phase1HabitAlarmPlanner.plan(newTime, firstLease(), beforeFirst)
        assertTrue(plan is Phase1HabitAlarmPlan.Install)
        val target = (plan as Phase1HabitAlarmPlan.Install).target
        assertEquals(Instant.parse("2026-11-07T03:15:00Z"), target.fireAt)
    }

    @Test fun aCancelledLeaseNeedsSeparateExplicitJournalClearBeforeNextInstall() {
        val presented = firstLease(Phase1HabitLeaseStage.PRESENTED)
        val resumed = habit().setPaused(true).setPaused(false)
        assertEquals(Phase1HabitAlarmPlan.HoldUnresolved(presented),
            Phase1HabitAlarmPlanner.plan(resumed, presented, beforeFirst))
    }
}
