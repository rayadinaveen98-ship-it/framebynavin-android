package com.framebynavin.app.personal

import java.time.LocalDate
import java.time.ZoneId
import java.util.UUID
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/** Scoping the screen must never change scheduler data or hide pending actions. */
class V285PersonalAgendaFilterTest {
    private val zone = ZoneId.of("Asia/Kolkata")
    private val start = LocalDate.of(2026, 11, 5)
    private val now = start.atTime(6, 0).atZone(zone).toInstant().toEpochMilli()

    private fun sample(): List<PersonalReminder> =
        PersonalDietFitnessPlanV1.templates(start, zone) +
            V285PersonalLifePlan.templates(start, zone) +
            V285PersonalLifePlan.focusTemplates(now - 1L, zone)

    @Test fun defaultTodayWindowIsReadableAndSevenDaysExtendsIt() {
        val all = sample()
        val today = V285PersonalAgendaFilter.groups(all, now, V285AgendaScope.TODAY, zone)
            .flatMap { it.items }
        val week = V285PersonalAgendaFilter.groups(all, now, V285AgendaScope.SEVEN_DAYS, zone)
            .flatMap { it.items }
        val every = V285PersonalAgendaFilter.groups(all, now, V285AgendaScope.ALL, zone)
            .flatMap { it.items }
        assertTrue(today.isNotEmpty())
        assertTrue(week.size >= today.size)
        assertTrue(every.size >= week.size)
        assertTrue(today.map { it.id }.toSet().all { id -> id in week.map { it.id }.toSet() })
        assertEquals(all.size, every.size)
    }

    @Test fun attentionAlwaysSurvivesBothFilteredViews() {
        val source = PersonalDietFitnessPlanV1.templates(start, zone).first()
        val token = UUID.randomUUID().toString()
        val presented = source.copy(
            pendingOccurrenceMillis = now - 120_000L,
            pendingFireAtMillis = now - 120_000L,
            pendingToken = token,
            presentedToken = token,
            presentedAtMillis = now - 60_000L,
        ).checked()
        for (scope in listOf(V285AgendaScope.TODAY, V285AgendaScope.SEVEN_DAYS)) {
            val visible = V285PersonalAgendaFilter.groups(
                listOf(presented), now, scope, zone
            ).flatMap { it.items }
            assertEquals(listOf(presented), visible)
        }
    }

    @Test fun pausedOnlyInAllWhileRecordsNeverDeleted() {
        val paused = PersonalDietFitnessPlanV1.templates(start, zone).first()
            .copy(paused = true)
        assertTrue(V285PersonalAgendaFilter.groups(listOf(paused), now,
            V285AgendaScope.TODAY, zone).isEmpty())
        assertTrue(V285PersonalAgendaFilter.groups(listOf(paused), now,
            V285AgendaScope.SEVEN_DAYS, zone).isEmpty())
        assertEquals(1, V285PersonalAgendaFilter.visibleCount(listOf(paused), now,
            V285AgendaScope.ALL, zone))
    }
}
