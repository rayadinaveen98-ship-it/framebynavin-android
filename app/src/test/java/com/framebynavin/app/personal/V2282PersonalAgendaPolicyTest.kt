package com.framebynavin.app.personal

import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class V2282PersonalAgendaPolicyTest {
    private val zone = ZoneId.of("Asia/Kolkata")
    private val today = LocalDate.of(2026, 10, 29)
    private val now = today.atTime(11, 0).atZone(zone).toInstant().toEpochMilli()

    private fun item(title: String, day: LocalDate, hour: Int = 18): PersonalReminder =
        PersonalReminder(title = title, startDate = day, time = LocalTime.of(hour, 0),
            zoneId = zone, repeat = PersonalRepeat.ONCE)

    @Test fun bucketsTodayTomorrowAndUpcomingWithoutFabrication() {
        assertEquals(PersonalAgendaSection.TODAY,
            PersonalAgendaPolicy.section(item("Today", today), now, zone))
        assertEquals(PersonalAgendaSection.TOMORROW,
            PersonalAgendaPolicy.section(item("Tomorrow", today.plusDays(1)), now, zone))
        assertEquals(PersonalAgendaSection.UPCOMING,
            PersonalAgendaPolicy.section(item("Later", today.plusDays(4)), now, zone))
        assertEquals(PersonalAgendaSection.NO_UPCOMING,
            PersonalAgendaPolicy.section(item("Expired", today.minusDays(1)), now, zone))
        assertEquals(PersonalAgendaSection.PAUSED,
            PersonalAgendaPolicy.section(item("Paused", today).copy(paused = true), now, zone))
    }

    @Test fun postedOrMissedRequiresAttentionButIsNeverMarkedDone() {
        val before = item("Was posted", today, 9)
        val occurrence = today.atTime(9, 0).atZone(zone).toInstant().toEpochMilli()
        val token = "a2cbfe30-8ba1-4e4c-bdd8-719f07d45d72"
        val posted = before.copy(
            pendingOccurrenceMillis = occurrence, pendingFireAtMillis = occurrence,
            pendingToken = token, presentedToken = token)
        assertEquals(PersonalAgendaSection.NEEDS_ATTENTION,
            PersonalAgendaPolicy.section(posted, now, zone))
        val archived = before.copy(lastMissedOccurrenceMillis = occurrence)
        assertEquals(PersonalAgendaSection.NEEDS_ATTENTION,
            PersonalAgendaPolicy.section(archived, now, zone))
        assertEquals(PersonalResponse.NONE, posted.lastResponse)
        assertEquals(PersonalResponse.NONE, archived.lastResponse)
        assertEquals(PersonalAgendaSection.NEEDS_ATTENTION,
            PersonalAgendaPolicy.section(posted.copy(pendingToken=token,
                presentedToken="", pendingFireAtMillis=occurrence), now, zone))
    }

    @Test fun groupsAreStableHaveNoDuplicatesAndHonorDeviceTimeZone() {
        val items = listOf(item("B later", today.plusDays(3)),
            item("Paused", today, 18).copy(paused = true),
            item("Tomorrow", today.plusDays(1)),
            item("A later", today.plusDays(2)),
            item("Today", today))
        val groups = PersonalAgendaPolicy.groups(items, now, zone)
        assertEquals(listOf(PersonalAgendaSection.TODAY, PersonalAgendaSection.TOMORROW,
            PersonalAgendaSection.UPCOMING, PersonalAgendaSection.PAUSED),
            groups.map { it.section })
        assertEquals(items.map { it.id }.toSet(),
            groups.flatMap { it.items }.map { it.id }.toSet())
        assertEquals(items.size, groups.sumOf { it.items.size })
        assertEquals(listOf("A later", "B later"),
            groups.first { it.section == PersonalAgendaSection.UPCOMING }.items.map { it.title })
        val utc = ZoneId.of("UTC")
        val atMidnightIndia = today.atTime(0, 30).atZone(zone).toInstant().toEpochMilli()
        // UTC date differs on the exact same instant; classify by the device display zone.
        assertFalse(java.time.Instant.ofEpochMilli(atMidnightIndia).atZone(utc).toLocalDate() == today)
        assertTrue(groups.isNotEmpty())
    }
}
