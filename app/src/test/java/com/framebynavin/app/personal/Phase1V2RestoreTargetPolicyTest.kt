package com.framebynavin.app.personal

import com.framebynavin.app.personal.spaces.V289SpaceState
import com.framebynavin.app.personal.spaces.V289SpacesMetadataCodec
import java.time.Instant
import java.time.LocalDate
import java.time.LocalTime
import org.junit.Assert.*
import org.junit.Test

class Phase1V2RestoreTargetPolicyTest {
    private val confirmed = Phase1HabitChallenge(
        id = "keep-21", activity = "Read daily",
        startDate = LocalDate.of(2026, 11, 7), zoneId = "Asia/Kolkata",
    ).confirmToday(Instant.parse("2026-11-07T04:00:00Z"))
    private val newHabit = Phase1HabitChallenge(
        id = "new-21", activity = "Stretch daily",
        startDate = LocalDate.of(2026, 11, 7), zoneId = "Asia/Kolkata",
    )
    private fun bundle(
        habits: List<Phase1HabitChallenge> = emptyList(),
        reminders: List<PersonalReminder> = emptyList(),
    ) = V300PersonalComponents(
        personal = PersonalCompletionBackupCodec.encode(reminders, emptyList()),
        challenges = V297ChallengeCodec.encode(emptyList()),
        groups = V298ReminderGroupCodec.encode(emptyList()),
        spaces = V289SpacesMetadataCodec.encode(V289SpaceState()),
        singleActivityChallenges = Phase1HabitCodec.encode(habits),
    )
    private fun device(b: V300PersonalComponents, valid: Boolean = true) =
        V301LocalRecoverySnapshot(b, emptyList(), valid)

    @Test fun incomingDifferentIdIsAdditiveAndRetainsExactOwnerDoneReceipt() {
        val original = bundle(listOf(confirmed))
        val plan = Phase1V2RestoreTargetPolicy.propose(
            device(original), bundle(listOf(newHabit)), emptyList())
        assertEquals(listOf(confirmed, newHabit), Phase1HabitCodec.decode(
            requireNotNull(plan.combined.singleActivityChallenges)))
        assertEquals(1, plan.preview.newChallenges.additions)
        assertEquals(1, plan.preview.newChallenges.localOnly)
        assertFalse(plan.mayRestoreLive)
        assertFalse(plan.mayReleaseJournal)
        assertTrue(plan.requiresOwnerApproval)
        assertNotEquals(plan.baselineDigest, plan.targetDigest)
    }

    @Test fun sameIdAndSameDoneReceiptAreIdempotentNotDuplicated() {
        val original = bundle(listOf(confirmed))
        val planned = Phase1V2RestoreTargetPolicy.propose(
            device(original), original, emptyList())
        assertEquals(listOf(confirmed), Phase1HabitCodec.decode(
            requireNotNull(planned.combined.singleActivityChallenges)))
        assertEquals(1, planned.preview.newChallenges.identical)
        assertEquals(0, planned.preview.newChallenges.additions)
        assertFalse(planned.mayRestoreLive)
    }

    @Test fun differingExactConfirmedAtInstantFailsBeforeTargetConstruction() {
        val modified = confirmed.copy(confirmations = mapOf(
            LocalDate.of(2026, 11, 7) to Instant.parse("2026-11-07T07:00:00Z")))
        assertThrows(IllegalArgumentException::class.java) {
            Phase1V2RestoreTargetPolicy.propose(
                device(bundle(listOf(confirmed))), bundle(listOf(modified)), emptyList())
        }
        assertEquals(Instant.parse("2026-11-07T04:00:00Z"),
            confirmed.confirmations.values.single())
    }

    @Test fun importedAlarmLeaseCannotBeTransferredToAnotherPhone() {
        val remote = PersonalReminder(
            id = "remote", title = "Wake up",
            startDate = LocalDate.of(2026, 11, 9),
            time = LocalTime.of(8, 30),
            createdAtMillis = 100L, updatedAtMillis = 100L,
            pendingOccurrenceMillis = 3000L, pendingFireAtMillis = 3500L,
            pendingToken = "bca17856-6da3-4cdf-84d0-e7a94a301ac3",
        )
        val plan = Phase1V2RestoreTargetPolicy.propose(
            device(bundle(listOf(confirmed))),
            bundle(listOf(newHabit), listOf(remote)),
            emptyList(),
        )
        val transferred = PersonalCompletionBackupCodec.decode(
            plan.combined.personal).reminders.single()
        assertTrue(transferred.pendingToken.isEmpty())
        assertEquals(0L, transferred.pendingOccurrenceMillis)
        assertEquals(listOf(confirmed, newHabit), Phase1HabitCodec.decode(
            requireNotNull(plan.combined.singleActivityChallenges)))
    }

    @Test fun oldReminderConflictCannotBeHiddenByNewChallengeAddition() {
        val owner = PersonalReminder(
            id = "same", title = "Original",
            startDate = LocalDate.of(2026, 11, 9), time = LocalTime.of(8, 30),
            createdAtMillis = 100L, updatedAtMillis = 100L,
        )
        val old = bundle(listOf(confirmed), listOf(owner))
        val incoming = bundle(listOf(newHabit),
            listOf(owner.copy(title = "Changed remotely")))
        assertThrows(IllegalArgumentException::class.java) {
            Phase1V2RestoreTargetPolicy.propose(device(old), incoming, emptyList())
        }
        assertEquals("Original",
            PersonalCompletionBackupCodec.decode(old.personal).reminders.single().title)
    }

    @Test fun incompleteLocalOrIncomingArchiveCoverageBlocksPlanning() {
        val original = bundle(listOf(confirmed))
        assertThrows(IllegalArgumentException::class.java) {
            Phase1V2RestoreTargetPolicy.propose(
                device(original, false), bundle(listOf(newHabit)), emptyList())
        }
        assertThrows(IllegalArgumentException::class.java) {
            Phase1V2RestoreTargetPolicy.propose(
                device(original),
                bundle(listOf(newHabit)).copy(singleActivityChallenges = null), emptyList())
        }
    }

    @Test fun legacyV1PlannerStillRejectsAnyV2ArchiveEvenEmptyHistory() {
        val v2 = bundle()
        assertThrows(IllegalStateException::class.java) {
            StabilizationRestoreTargetPolicy.calculate(device(v2), v2, emptyList())
        }
    }
}
