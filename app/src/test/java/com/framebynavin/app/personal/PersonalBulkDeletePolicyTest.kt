package com.framebynavin.app.personal

import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId
import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Pure policy tests. No Android store, alarm, real deletion, owner record or
 * fake receipt is touched by any test.
 */
class PersonalBulkDeletePolicyTest {
    private val zone = ZoneId.of("Asia/Kolkata")
    private fun reminder(id: String, title: String) = PersonalReminder(
        id = id,
        title = title,
        startDate = LocalDate.of(2026, 11, 13),
        time = LocalTime.of(8, 30),
        zoneId = zone,
        createdAtMillis = 1000L,
        updatedAtMillis = 1000L,
    ).checked()

    @Test fun reviewedBatchRemovesOnlySelectedPersonalRecords() {
        val a = reminder("a", "Drink water")
        val b = reminder("b", "Workout")
        val c = reminder("c", "Read")
        val output = PersonalBulkDeletePolicy.deleteReviewed(
            listOf(a, b, c),
            listOf(PersonalBulkDeletePolicy.ticket(a), PersonalBulkDeletePolicy.ticket(c)),
        )
        assertEquals(listOf(b), output)
        assertEquals("Workout", output.single().title)
    }

    @Test fun staleTicketRejectsWholeBatchInsteadOfPartiallyDeleting() {
        val a = reminder("a", "Drink water")
        val b = reminder("b", "Workout")
        val updatedB = b.copy(title = "Evening workout", updatedAtMillis = 1001L).checked()
        assertThrows(IllegalArgumentException::class.java) {
            PersonalBulkDeletePolicy.deleteReviewed(
                listOf(a, updatedB),
                listOf(PersonalBulkDeletePolicy.ticket(a), PersonalBulkDeletePolicy.ticket(b)),
            )
        }
        assertEquals("Drink water", a.title)
        assertEquals("Evening workout", updatedB.title)
    }

    @Test fun duplicateAndUnknownIdsAreRejectedBeforeChangingAnything() {
        val a = reminder("a", "Drink water")
        val ticket = PersonalBulkDeletePolicy.ticket(a)
        assertThrows(IllegalArgumentException::class.java) {
            PersonalBulkDeletePolicy.deleteReviewed(listOf(a), listOf(ticket, ticket))
        }
        assertThrows(IllegalArgumentException::class.java) {
            PersonalBulkDeletePolicy.deleteReviewed(listOf(a), listOf(
                ticket.copy(id = "missing")))
        }
        assertThrows(IllegalArgumentException::class.java) {
            PersonalBulkDeletePolicy.deleteReviewed(listOf(a), emptyList())
        }
    }

    @Test fun schedulingTokenRotationDoesNotInvalidateReviewOfSameReminder() {
        val a = reminder("a", "Drink water")
        val rotated = a.copy(
            pendingToken = "aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa",
            pendingOccurrenceMillis = 1_000_000L,
            pendingFireAtMillis = 1_000_000L,
        ).checked()
        val output = PersonalBulkDeletePolicy.deleteReviewed(
            listOf(rotated), listOf(PersonalBulkDeletePolicy.ticket(a)))
        assertTrue(output.isEmpty())
    }

    @Test fun visibleSelectAllIgnoresChallengeRowsEvenWhenIdsCollide() {
        val a = reminder("same-id", "Drink water")
        val ch = Phase1HabitChallenge(
            id = "same-id", activity = "Read books",
            startDate = LocalDate.of(2026, 11, 13),
            zoneId = zone.id, alertTime = LocalTime.of(9, 0),
            alertKind = Phase1HabitAlertKind.ALARM,
        ).checked()
        val groups = listOf(V337AgendaGroup(
            PersonalAgendaSection.TODAY,
            listOf(
                V337AgendaEntry.Personal(a, PersonalAgendaSection.TODAY, 123L),
                V337AgendaEntry.Challenge(ch, PersonalAgendaSection.TODAY, 123L),
            )
        ))
        val selected = PersonalBulkDeletePolicy.visiblePersonalTickets(groups)
        assertEquals(listOf("same-id"), selected.map { it.id })
        assertEquals(a.title, selected.single().title)
    }

    @Test fun visibleSelectAllWithOnlyChallengeRowsReturnsEmptySelection() {
        val challenge = Phase1HabitChallenge(
            id = "challenge-only", activity = "Meditate",
            startDate = LocalDate.of(2026, 11, 13), zoneId = zone.id,
            alertTime = LocalTime.of(8, 0),
            alertKind = Phase1HabitAlertKind.VOICE,
        )
        val items = listOf(V337AgendaGroup(
            PersonalAgendaSection.TODAY,
            listOf(V337AgendaEntry.Challenge(
                challenge, PersonalAgendaSection.TODAY, 123L)),
        ))
        assertTrue(PersonalBulkDeletePolicy.visiblePersonalTickets(items).isEmpty())
    }
}
