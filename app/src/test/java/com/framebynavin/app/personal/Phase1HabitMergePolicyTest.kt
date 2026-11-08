package com.framebynavin.app.personal

import java.time.Instant
import java.time.LocalDate
import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Test

class Phase1HabitMergePolicyTest {
    private fun habit(id: String): Phase1HabitChallenge = Phase1HabitChallenge(
        id = id, activity = "Read 20 minutes", startDate = LocalDate.of(2026, 11, 7),
        zoneId = "Asia/Kolkata"
    ).checked()

    @Test fun identicalIdentityDoesNotOverwriteExistingOwnerProgress() {
        val original = habit("original").confirmToday(Instant.parse("2026-11-07T04:00:00Z"))
        val conflicting = habit("original").copy(activity = "Other imported activity")
        val imported = habit("incoming")
        val error = assertThrows(IllegalArgumentException::class.java) {
            Phase1HabitMergePolicy.addMissing(
                listOf(original), listOf(conflicting, imported),
            )
        }
        assertTrue(error.message.orEmpty().contains("Owner review required"))
        assertEquals(1, original.confirmations.size)
        assertEquals("Read 20 minutes", original.activity)
    }

    @Test fun identicalCollisionRetainsExistingReceiptAndAddsOnlyNewIds() {
        val original = habit("original").confirmToday(Instant.parse("2026-11-07T04:00:00Z"))
        val added = habit("new")
        assertEquals(
            listOf(original, added),
            Phase1HabitMergePolicy.addMissing(listOf(original), listOf(original, added)),
        )
    }

    @Test fun sameIdDifferentDoneInstantIsRejectedNotIgnored() {
        val original = habit("original").confirmToday(Instant.parse("2026-11-07T04:00:00Z"))
        val later = habit("original").confirmToday(Instant.parse("2026-11-07T07:00:00Z"))
        assertThrows(IllegalArgumentException::class.java) {
            Phase1HabitMergePolicy.addMissing(listOf(original), listOf(later))
        }
        assertEquals(Instant.parse("2026-11-07T04:00:00Z"),
            original.confirmations.values.single())
    }

    @Test fun duplicateImportedIdentitiesRejectBeforeAnyMutation() {
        val same = habit("imported")
        assertThrows(IllegalArgumentException::class.java) {
            Phase1HabitMergePolicy.addMissing(emptyList(), listOf(same, same))
        }
    }

    @Test fun noIncomingPlansCannotEraseExisting() {
        val existing = listOf(habit("a"), habit("b"))
        assertEquals(existing, Phase1HabitMergePolicy.addMissing(existing, emptyList()))
        assertTrue(Phase1HabitMergePolicy.addMissing(emptyList(), emptyList()).isEmpty())
    }
}
