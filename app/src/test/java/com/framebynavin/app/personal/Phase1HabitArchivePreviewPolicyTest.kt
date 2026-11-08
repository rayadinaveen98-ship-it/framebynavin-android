package com.framebynavin.app.personal

import java.time.Instant
import java.time.LocalDate
import java.time.LocalTime
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Test

class Phase1HabitArchivePreviewPolicyTest {
    private fun plan(id: String = "morning") = Phase1HabitChallenge(
        id = id, activity = "Read every day",
        startDate = LocalDate.of(2026, 11, 7),
        zoneId = "Asia/Kolkata",
        alertTime = LocalTime.of(7, 30),
        alertKind = Phase1HabitAlertKind.QUIET,
    ).checked()

    private val dayOne = Instant.parse("2026-11-07T04:00:00Z")
    private val laterOnDayOne = Instant.parse("2026-11-07T07:00:00Z")

    @Test fun identicalPlanAndExactReceiptRequireNoConflict() {
        val owner = plan().confirmToday(dayOne)
        val report = Phase1HabitArchivePreviewPolicy.compare(
            listOf(owner), Phase1HabitCodec.encode(listOf(owner)),
        )
        assertTrue(report.archiveHasChallengeCoverage)
        assertEquals(1, report.identical)
        assertEquals(0, report.conflicts)
        assertEquals(0, report.conflictingReceiptDates)
        assertEquals(1, report.importedDoneReceipts)
        assertFalse(report.needsOwnerReview)
        assertFalse(report.authorizesRestore)
    }

    @Test fun newerArchiveWithDifferentIdIsOnlyAProposedAddition() {
        val report = Phase1HabitArchivePreviewPolicy.compare(
            listOf(plan()), Phase1HabitCodec.encode(
                listOf(plan("new-habit").confirmToday(dayOne))),
        )
        assertEquals(1, report.additions)
        assertEquals(1, report.localOnly)
        assertTrue(report.needsOwnerReview)
        assertFalse(report.authorizesRestore)
    }

    @Test fun conflictingDoneInstantIsNeverMergedWithoutOwnerChoice() {
        val original = plan().confirmToday(dayOne)
        val modified = plan().confirmToday(laterOnDayOne)
        val report = Phase1HabitArchivePreviewPolicy.compare(
            listOf(original), Phase1HabitCodec.encode(listOf(modified)),
        )
        assertEquals(1, report.conflicts)
        assertEquals(1, report.conflictingReceiptDates)
        assertTrue(report.warning.contains("Owner conflict review"))
        assertFalse(report.authorizesRestore)
        assertEquals(dayOne, original.confirmations.values.single())
    }

    @Test fun pauseOrAlertDifferenceAlsoRequiresConflictReview() {
        val report = Phase1HabitArchivePreviewPolicy.compare(
            listOf(plan()), Phase1HabitCodec.encode(listOf(plan().setPaused(true))),
        )
        assertEquals(1, report.conflicts)
        assertEquals(0, report.conflictingReceiptDates)
        assertTrue(report.needsOwnerReview)
    }

    @Test fun v1ZipWithoutChallengeDomainNeverClaimsThatProgressWasBackedUp() {
        val report = Phase1HabitArchivePreviewPolicy.compare(
            listOf(plan().confirmToday(dayOne)), null,
        )
        assertFalse(report.archiveHasChallengeCoverage)
        assertEquals(1, report.localOnly)
        assertEquals(0, report.importedDoneReceipts)
        assertTrue(report.needsOwnerReview)
        assertFalse(report.authorizesRestore)
    }

    @Test fun emptyV2ArchiveIsDistinguishedFromLegacyUncoveredArchive() {
        val report = Phase1HabitArchivePreviewPolicy.compare(
            emptyList(), Phase1HabitCodec.encode(emptyList()),
        )
        assertTrue(report.archiveHasChallengeCoverage)
        assertFalse(report.needsOwnerReview)
        assertFalse(report.authorizesRestore)
    }

    @Test fun duplicateOrTamperedRecordsFailBeforeComparison() {
        assertThrows(IllegalArgumentException::class.java) {
            Phase1HabitArchivePreviewPolicy.compare(
                listOf(plan(), plan()), Phase1HabitCodec.encode(emptyList()),
            )
        }
        val correct = Phase1HabitCodec.encode(listOf(plan()))
        assertThrows(IllegalArgumentException::class.java) {
            Phase1HabitArchivePreviewPolicy.compare(
                emptyList(), correct.replace("Read every day", "Falsified"),
            )
        }
    }
}
