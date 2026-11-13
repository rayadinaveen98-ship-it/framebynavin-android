package com.framebynavin.app.personal

import java.time.Instant
import java.time.LocalDate
import java.time.LocalTime
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Test

class Phase1HabitManagementPolicyTest {
    private val start = LocalDate.of(2026, 11, 13)
    private val receipt = Instant.parse("2026-11-13T05:15:00Z")

    private fun challenge() = Phase1HabitChallenge(
        id = "archive-test",
        activity = "Read for 20 minutes",
        startDate = start,
        zoneId = "Asia/Kolkata",
        alertTime = LocalTime.of(8, 30),
        alertKind = Phase1HabitAlertKind.ALARM,
        linkedSpaceId = "reading",
        linkedGroupId = "daily",
    ).checked()

    @Test fun changingTitleAndAlarmPreservesExactReceiptsAndLinks() {
        val before = challenge().confirmToday(receipt)
            .noteForDay(start, "Chapter one finished")
        val edited = Phase1HabitManagementPolicy.reviewedEdit(
            before, "Read for 30 minutes", start,
            LocalTime.of(9, 45), Phase1HabitAlertKind.VOICE,
            Phase1HabitCompletionMode.STRICT,
        )
        assertEquals("Read for 30 minutes", edited.activity)
        assertEquals(receipt, edited.confirmations[start])
        assertEquals(before.dayNotes, edited.dayNotes)
        assertEquals(before.linkedSpaceId, edited.linkedSpaceId)
        assertEquals(before.linkedGroupId, edited.linkedGroupId)
        assertEquals(LocalTime.of(9, 45), edited.alertTime)
    }

    @Test fun movingStartAfterProgressIsRefusedWithoutRebasingDone() {
        val before = challenge().confirmToday(receipt)
        assertThrows(IllegalArgumentException::class.java) {
            Phase1HabitManagementPolicy.reviewedEdit(
                before, before.activity, start.plusDays(1),
                before.alertTime, before.alertKind, before.completionMode,
            )
        }
        assertEquals(receipt, before.confirmations[start])
    }

    @Test fun movingStartWithExistingDayNoteIsRefused() {
        val before = challenge().noteForDay(start, "Remember to review")
        assertThrows(IllegalArgumentException::class.java) {
            Phase1HabitManagementPolicy.reviewedEdit(
                before, before.activity, start.plusDays(1),
                before.alertTime, before.alertKind, before.completionMode,
            )
        }
    }

    @Test fun movingStartOnUntouchedChallengeKeepsIdentityAndLinks() {
        val before = challenge()
        val next = Phase1HabitManagementPolicy.reviewedEdit(
            before, before.activity, start.plusDays(2),
            before.alertTime, before.alertKind, before.completionMode,
        )
        assertEquals(before.id, next.id)
        assertEquals(start.plusDays(2), next.startDate)
        assertEquals(before.linkedSpaceId, next.linkedSpaceId)
        assertTrue(next.confirmations.isEmpty())
    }

    @Test fun invalidStrictDowngradeCannotRewriteFlexiblePastDayReceipt() {
        val backfilled = challenge().copy(
            completionMode = Phase1HabitCompletionMode.FLEXIBLE,
        ).checked().confirmDay(start, receipt.plusSeconds(86_400L))
        assertThrows(IllegalArgumentException::class.java) {
            Phase1HabitManagementPolicy.reviewedEdit(
                backfilled, backfilled.activity, start, null, null,
                Phase1HabitCompletionMode.STRICT,
            )
        }
        assertEquals(1, backfilled.confirmations.size)
    }

    @Test fun archivingRetainsHistoryAndDisablesSchedulingOrCompletion() {
        val before = challenge().confirmToday(receipt)
            .noteForDay(start, "No history lost")
        val archived = before.setArchived(true)
        assertTrue(archived.archived)
        assertEquals(before.confirmations, archived.confirmations)
        assertEquals(before.dayNotes, archived.dayNotes)
        assertEquals(before.linkedSpaceId, archived.linkedSpaceId)
        assertEquals(null, archived.nextAlertAfter(receipt))
        assertFalse(archived.canSnooze(receipt.plusSeconds(60L)))
        assertThrows(IllegalArgumentException::class.java) {
            archived.confirmDay(start.plusDays(1), receipt.plusSeconds(86_400L))
        }
        assertEquals(before.confirmations, archived.setArchived(false).confirmations)
    }

    @Test fun archiveCannotBeEditedBeforeItIsRestored() {
        val archived = challenge().setArchived(true)
        assertThrows(IllegalArgumentException::class.java) {
            Phase1HabitManagementPolicy.reviewedEdit(
                archived, archived.activity, start,
                archived.alertTime, archived.alertKind, archived.completionMode,
            )
        }
    }
}
