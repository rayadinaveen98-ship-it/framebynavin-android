package com.framebynavin.app.personal

import java.time.Instant
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Pure presentation rules: these checks never create real alarms, write a store,
 * alter completion receipts or synthesize a PersonalReminder for a challenge.
 */
class V337UnifiedPersonalAgendaTest {
    private val zone = ZoneId.of("Asia/Kolkata")
    private val date = LocalDate.of(2026, 11, 13)
    private val early = date.atTime(6, 0).atZone(zone).toInstant()
    private val due = date.atTime(8, 30).atZone(zone).toInstant()

    private fun challenge(
        id: String = "shared-id",
        start: LocalDate = date,
        kind: Phase1HabitAlertKind? = Phase1HabitAlertKind.ALARM,
    ) = Phase1HabitChallenge(
        id = id,
        activity = "Read for 20 minutes",
        startDate = start,
        zoneId = zone.id,
        alertTime = if (kind == null) null else LocalTime.of(8, 30),
        alertKind = kind,
    ).checked()

    private fun personal(id: String = "shared-id") = PersonalReminder(
        id = id, title = "Drink water",
        startDate = date, time = LocalTime.of(8, 30),
        zoneId = zone, repeat = PersonalRepeat.DAILY,
    ).checked()

    private fun view(
        personal: List<PersonalReminder> = emptyList(),
        challenges: List<Phase1HabitChallenge> = listOf(challenge()),
        now: Instant = early,
        scope: V285AgendaScope = V285AgendaScope.ALL,
    ) = V337UnifiedPersonalAgenda.groups(personal, challenges,
        now.toEpochMilli(), scope, zone).flatMap { it.entries }

    @Test fun personalAndChallengeWithSameIdRemainDistinct() {
        val rows = view(personal = listOf(personal()))
        assertEquals(2, rows.size)
        assertEquals(setOf("personal:shared-id", "challenge:shared-id"),
            rows.map { it.stableKey }.toSet())
        assertEquals(1, rows.count { it is V337AgendaEntry.Personal })
        assertEquals(1, rows.count { it is V337AgendaEntry.Challenge })
    }

    @Test fun challengeIsVisibleInTodayAndSevenDays() {
        for (scope in listOf(V285AgendaScope.TODAY,
                V285AgendaScope.SEVEN_DAYS, V285AgendaScope.ALL)) {
            val rows = view(scope = scope)
            assertEquals(1, rows.size)
            val item = rows.single() as V337AgendaEntry.Challenge
            assertEquals(PersonalAgendaSection.TODAY, item.section)
            assertEquals(due.toEpochMilli(), item.dueAtMillis)
        }
    }

    @Test fun alreadyDueUnconfirmedChallengeStaysVisibleWithoutInferringMissedAlert() {
        val item = view(now = due.plusSeconds(3600L),
            scope = V285AgendaScope.TODAY).single() as V337AgendaEntry.Challenge
        assertEquals(PersonalAgendaSection.NEEDS_ATTENTION, item.section)
        assertEquals(due.toEpochMilli(), item.dueAtMillis)
        assertTrue(item.record.confirmations.isEmpty())
    }

    @Test fun completedTodayShowsActualTomorrowScheduleNotDuplicateToday() {
        val finishedToday = challenge().confirmToday(due.plusSeconds(60L))
        val item = view(challenges = listOf(finishedToday),
            now = due.plusSeconds(120L), scope = V285AgendaScope.SEVEN_DAYS)
            .single() as V337AgendaEntry.Challenge
        assertEquals(PersonalAgendaSection.TOMORROW, item.section)
        assertEquals(date.plusDays(1).atTime(8, 30).atZone(zone)
            .toInstant().toEpochMilli(), item.dueAtMillis)
        assertEquals(1, item.record.confirmations.size)
    }

    @Test fun snoozedChallengeUsesActualUpcomingSnooze() {
        val snoozed = challenge().snoozeToday(due.plusSeconds(30L), 10)
        val item = view(challenges = listOf(snoozed),
            now = due.plusSeconds(40L), scope = V285AgendaScope.TODAY)
            .single() as V337AgendaEntry.Challenge
        assertEquals(snoozed.snoozedUntil?.toEpochMilli(), item.dueAtMillis)
        assertEquals(PersonalAgendaSection.TODAY, item.section)
    }

    @Test fun pausedAndArchivedAppearOnlyUnderAllNotDailyWindows() {
        for (item in listOf(challenge().setPaused(true),
                challenge().setArchived(true))) {
            assertTrue(view(challenges = listOf(item),
                scope = V285AgendaScope.TODAY).isEmpty())
            assertTrue(view(challenges = listOf(item),
                scope = V285AgendaScope.SEVEN_DAYS).isEmpty())
            val result = view(challenges = listOf(item)).single()
            assertEquals(PersonalAgendaSection.PAUSED, result.section)
            assertEquals(null, result.dueAtMillis)
        }
    }

    @Test fun alertOffIsNotAnAlertAndCannotBecomeAnExtraPersonalReminder() {
        assertTrue(view(challenges = listOf(challenge(kind = null))).isEmpty())
        assertEquals(1, view(
            personal = listOf(personal()), challenges = listOf(challenge(kind = null))
        ).size)
    }

    @Test fun notYetStartedChallengeRespectsWindowAndAllView() {
        val upcoming = challenge(start = date.plusDays(10))
        assertTrue(view(challenges = listOf(upcoming),
            scope = V285AgendaScope.TODAY).isEmpty())
        assertTrue(view(challenges = listOf(upcoming),
            scope = V285AgendaScope.SEVEN_DAYS).isEmpty())
        assertEquals(PersonalAgendaSection.UPCOMING,
            view(challenges = listOf(upcoming)).single().section)
    }

    @Test fun endedChallengeHasNoUpcomingOccurrenceInAll() {
        val old = challenge(start = date.minusDays(22))
        val result = view(challenges = listOf(old), scope = V285AgendaScope.ALL)
        assertEquals(1, result.size)
        assertEquals(PersonalAgendaSection.NO_UPCOMING, result.single().section)
        assertEquals(null, result.single().dueAtMillis)
    }

    @Test fun bothSourcesMaintainOriginalInstancesAndNoDataWrites() {
        val savedPersonal = personal()
        val savedChallenge = challenge()
        val personalBefore = savedPersonal.copy()
        val challengeBefore = savedChallenge.copy()
        view(personal = listOf(savedPersonal), challenges = listOf(savedChallenge))
        assertEquals(personalBefore, savedPersonal)
        assertEquals(challengeBefore, savedChallenge)
        assertFalse(savedChallenge.confirmations.isNotEmpty())
    }
}
