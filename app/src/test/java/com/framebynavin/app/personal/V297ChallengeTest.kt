package com.framebynavin.app.personal

import java.time.LocalDate
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Test

class V297ChallengeTest {
    private fun plan() = V297Challenge(
        id = "test-21-days",
        title = "My own challenge",
        startDate = LocalDate.of(2026, 12, 1),
        createdAtMillis = 1_000L,
    ).checked()

    @Test fun startDateAndAllActivitiesBelongToTheOwner() {
        val created = plan()
        assertEquals(21, created.days.size)
        assertTrue(created.days.all { it.activity.isBlank() && it.completedAtMillis == null })
        assertEquals(LocalDate.of(2026, 12, 1), created.activityDate(0))
        assertEquals(LocalDate.of(2026, 12, 21), created.activityDate(20))
        assertThrows(IllegalArgumentException::class.java) { created.activityDate(21) }
    }

    @Test fun onlyExplicitMarkDoneChangesProgress() {
        val initial = plan()
        assertThrows(IllegalArgumentException::class.java) { initial.markDone(0, 2_000L) }
        val updated = initial.editDay(0, "Read something meaningful")
        assertEquals(null, updated.days[0].completedAtMillis)
        val completed = updated.markDone(0, 2_000L)
        assertEquals(2_000L, completed.days[0].completedAtMillis)
        assertEquals(null, completed.days[1].completedAtMillis)
        val undone = completed.undoDone(0)
        assertEquals(null, undone.days[0].completedAtMillis)
        assertEquals("Read something meaningful", undone.days[0].activity)
    }

    @Test fun editingCompletedActivityResetsConfirmation() {
        val complete = plan().editDay(8, "Exercise").markDone(8, 3_000L)
        assertEquals(3_000L, complete.editDay(8, "Exercise").days[8].completedAtMillis)
        val revised = complete.editDay(8, "Walk outdoors")
        assertEquals(null, revised.days[8].completedAtMillis)
        assertEquals("Walk outdoors", revised.days[8].activity)
        assertEquals(null, complete.days[7].completedAtMillis)
    }

    @Test fun archiveRetainsDataButPreventsWritesUntilRestored() {
        val saved = plan().editDay(0, "My chosen activity").markDone(0, 2_000L)
        val archived = saved.archive(3_000L)
        assertEquals(3_000L, archived.archivedAtMillis)
        assertEquals(2_000L, archived.days[0].completedAtMillis)
        assertThrows(IllegalArgumentException::class.java) { archived.editDay(1, "Another day") }
        assertThrows(IllegalArgumentException::class.java) { archived.undoDone(0) }
        assertEquals(saved, archived.restore())
    }

    @Test fun checksumVerifiedBackupRoundTripsMultipleChallenges() {
        val first = plan().editDay(0, "Read").markDone(0, 2_000L)
        val second = plan().copy(id = "second", title = "Another goal").archive(4_000L)
        val raw = V297ChallengeCodec.encode(listOf(first, second))
        assertEquals(listOf(first, second), V297ChallengeCodec.decode(raw))
        assertThrows(IllegalArgumentException::class.java) {
            V297ChallengeCodec.encode(listOf(first, first))
        }
        val changed = raw.replace("Read", "Fake")
        assertThrows(IllegalArgumentException::class.java) {
            V297ChallengeCodec.decode(changed)
        }
    }

    @Test fun invalidProgressOrStructureIsRejected() {
        assertThrows(IllegalArgumentException::class.java) {
            plan().copy(days = List(20) { V297ChallengeDay() }).checked()
        }
        assertThrows(IllegalArgumentException::class.java) {
            plan().copy(days = List(21) { V297ChallengeDay("", 1_000L) }).checked()
        }
        assertThrows(IllegalArgumentException::class.java) {
            plan().copy(title = " ").checked()
        }
        assertFalse(plan().archivedAtMillis != null)
    }
    @Test fun importOnlyAddsNewIdsAndKeepsExistingEdits() {
        val original = plan().editDay(0, "My original activity").markDone(0, 3_000L)
        val conflictingImport = original.copy(title = "Overwritten in backup")
        val newPlan = plan().copy(id = "new-plan", title = "New imported plan")
        val merged = V297ChallengeMergePolicy.addMissing(
            listOf(original), listOf(conflictingImport, newPlan),
        )
        assertEquals(2, merged.size)
        assertEquals(original, merged[0])
        assertEquals(newPlan, merged[1])
        assertEquals(3_000L, merged[0].days[0].completedAtMillis)
        assertThrows(IllegalArgumentException::class.java) {
            V297ChallengeMergePolicy.addMissing(listOf(original), listOf(newPlan, newPlan))
        }
    }

}
