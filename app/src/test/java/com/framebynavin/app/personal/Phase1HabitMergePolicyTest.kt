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
        val merged = Phase1HabitMergePolicy.addMissing(
            listOf(original), listOf(conflicting, imported),
        )
        assertEquals(listOf(original, imported), merged)
        assertEquals(1, merged[0].confirmations.size)
        assertEquals("Read 20 minutes", merged[0].activity)
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
