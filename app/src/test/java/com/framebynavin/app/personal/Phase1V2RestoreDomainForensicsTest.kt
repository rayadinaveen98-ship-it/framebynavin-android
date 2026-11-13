package com.framebynavin.app.personal

import com.framebynavin.app.personal.spaces.V289PersonalSpace
import com.framebynavin.app.personal.spaces.V289SpaceReminderLink
import com.framebynavin.app.personal.spaces.V289SpaceState
import com.framebynavin.app.personal.spaces.V289SpaceType
import com.framebynavin.app.personal.spaces.V289SpacesMetadataCodec
import java.time.Instant
import java.time.LocalDate
import java.time.LocalTime
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Test

class Phase1V2RestoreDomainForensicsTest {
    private val owner = PersonalReminder(
        id = "v2-owner", title = "Keep owner",
        startDate = LocalDate.of(2026, 11, 7), time = LocalTime.of(8, 30),
        createdAtMillis = 100L, updatedAtMillis = 100L,
    )
    private val imported = owner.copy(id = "v2-imported", title = "New",
        createdAtMillis = 200L, updatedAtMillis = 200L)
    private val original = V300PersonalComponents(
        personal = PersonalCompletionBackupCodec.encode(listOf(owner), emptyList()),
        challenges = V297ChallengeCodec.encode(emptyList()),
        groups = V298ReminderGroupCodec.encode(emptyList()),
        spaces = V289SpacesMetadataCodec.encode(V289SpaceState()),
        singleActivityChallenges = Phase1HabitCodec.encode(emptyList()),
    )
    private val habit = Phase1HabitChallenge(
        id = "v2-single-habit", activity = "Read",
        startDate = LocalDate.of(2026, 11, 7), zoneId = "Asia/Kolkata",
    )
    private val target = V300PersonalComponents(
        personal = PersonalCompletionBackupCodec.encode(
            listOf(owner, imported), emptyList()),
        challenges = V297ChallengeCodec.encode(listOf(V297Challenge(
            id = "v2-legacy", title = "Existing format",
            startDate = LocalDate.of(2026, 11, 7), createdAtMillis = 200L,
        ))),
        groups = V298ReminderGroupCodec.encode(listOf(V298ReminderGroup(
            id = "v2-group", name = "Daily",
            reminderIds = listOf(imported.id), createdAtMillis = 200L,
            updatedAtMillis = 200L,
        ))),
        spaces = V289SpacesMetadataCodec.encode(V289SpaceState(
            spaces = listOf(V289PersonalSpace(
                id = "afcd17ed-5fd6-4a60-b8cb-31484ee653e7",
                title = "Personal", type = V289SpaceType.CUSTOM,
                createdAtMillis = 200L,
            )),
            links = listOf(V289SpaceReminderLink(
                "afcd17ed-5fd6-4a60-b8cb-31484ee653e7", imported.id, 210L)),
        )),
        singleActivityChallenges = Phase1HabitCodec.encode(listOf(
            habit.confirmToday(Instant.parse("2026-11-07T04:00:00Z")))),
    )
    private fun checkpoint(step: Phase1V2RestoreStep) = Phase1V2RestoreCheckpoint(
        transactionId = "e751c74e-8d47-4f69-bd46-c95c45d04c01",
        preparedId = "e751c74e-8d47-4f69-bd46-c95c45d04c01",
        escrowId = "613c863a-b019-4a10-9824-54f655f174aa",
        originalDigest = Phase1V2RestoreFingerprint.calculate(original, emptyList()),
        targetDigest = Phase1V2RestoreFingerprint.calculate(target, emptyList()),
        step = step, createdAtMillis = 100L, updatedAtMillis = 101L,
    )
    private fun assess(step: Phase1V2RestoreStep, current: V300PersonalComponents) =
        Phase1V2RestoreDomainForensics.inspect(
            checkpoint(step), original, target, current,
            emptyList(), emptyList(), emptyList(),
        )

    @Test fun preparedRequiresEveryStoreToMatchOriginal() {
        val r = assess(Phase1V2RestoreStep.PREPARED, original)
        assertEquals(5, r.domains.size)
        assertTrue(r.domains.values.all { it == Phase1V2RecoveryDomainState.ORIGINAL })
        assertFalse(r.authorizesLiveWrite)
        assertFalse(r.requiresIndependentPhotoProof)
    }
    @Test fun verifyingRequiresEveryStoreToMatchTarget() {
        val r = assess(Phase1V2RestoreStep.VERIFYING, target)
        assertTrue(r.domains.values.all { it == Phase1V2RecoveryDomainState.TARGET })
        assertTrue(r.requiresIndependentPhotoProof)
        assertFalse(r.authorizesJournalRelease)
        assertFalse(r.mayAutoRestore)
    }
    @Test fun halfRestoredStoresMustFollowExactJournalOrder() {
        val partial = original.copy(
            personal = target.personal, challenges = target.challenges,
            singleActivityChallenges = target.singleActivityChallenges,
        )
        val r = assess(Phase1V2RestoreStep.BEFORE_GROUPS, partial)
        assertEquals(Phase1V2RecoveryDomainState.TARGET,
            r.domains[Phase1V2RecoveryDomain.SINGLE_ACTIVITY])
        assertEquals(Phase1V2RecoveryDomainState.ORIGINAL,
            r.domains[Phase1V2RecoveryDomain.GROUPS])
        assertEquals(Phase1V2RecoveryDomainState.ORIGINAL,
            r.domains[Phase1V2RecoveryDomain.SPACES])
    }
    @Test fun activeBeforeMarkerAllowsOnlyTwoCompleteRevisions() {
        val a = original.copy(personal = target.personal,
            challenges = target.challenges)
        val b = a.copy(singleActivityChallenges = target.singleActivityChallenges)
        val mark = Phase1V2RestoreStep.BEFORE_SINGLE_ACTIVITY
        assertEquals(Phase1V2RecoveryDomainState.ORIGINAL,
            assess(mark, a).domains[Phase1V2RecoveryDomain.SINGLE_ACTIVITY])
        assertEquals(Phase1V2RecoveryDomainState.TARGET,
            assess(mark, b).domains[Phase1V2RecoveryDomain.SINGLE_ACTIVITY])
    }
    @Test fun earlyChallengeWriteMustBeRejected() {
        val tampered = original.copy(personal = target.personal,
            challenges = target.challenges,
            singleActivityChallenges = target.singleActivityChallenges)
        assertThrows(IllegalArgumentException::class.java) {
            assess(Phase1V2RestoreStep.LEGACY_CHALLENGES_WRITTEN, tampered)
        }
    }
    @Test fun missingConfirmedChallengeAfterMarkerMustBeRejected() {
        val incomplete = original.copy(personal = target.personal,
            challenges = target.challenges)
        assertThrows(IllegalArgumentException::class.java) {
            assess(Phase1V2RestoreStep.SINGLE_ACTIVITY_WRITTEN, incomplete)
        }
    }
    @Test fun forgedDoneReceiptIsAnUnknownThirdHistory() {
        val forged = target.copy(singleActivityChallenges =
            Phase1HabitCodec.encode(listOf(
                habit.confirmToday(Instant.parse("2026-11-07T07:00:00Z")))))
        assertThrows(IllegalArgumentException::class.java) {
            assess(Phase1V2RestoreStep.VERIFYING, forged)
        }
    }
    @Test fun outOfOrderGroupWriteIsRejected() {
        assertThrows(IllegalArgumentException::class.java) {
            assess(Phase1V2RestoreStep.PREPARED,
                original.copy(groups = target.groups))
        }
    }
    @Test fun tamperedJournalFingerprintBlocksInspection() {
        assertThrows(IllegalArgumentException::class.java) {
            Phase1V2RestoreDomainForensics.inspect(
                checkpoint(Phase1V2RestoreStep.PREPARED)
                    .copy(targetDigest = "0".repeat(64)),
                original, target, original, emptyList(), emptyList(), emptyList(),
            )
        }
    }
    @Test fun missingNewStoreIsNeverTreatedAsEmpty() {
        assertThrows(IllegalArgumentException::class.java) {
            assess(Phase1V2RestoreStep.PREPARED,
                original.copy(singleActivityChallenges = null))
        }
    }
    @Test fun photoInstallationNeedsIndependentByteVerification() {
        val r = assess(Phase1V2RestoreStep.BEFORE_PHOTOS, target)
        assertTrue(r.requiresIndependentPhotoProof)
        assertFalse(r.authorizesLiveWrite)
    }

    @Test fun inverseMarkerMayObserveWholeOriginalOrTargetDomainDuringReverse() {
        val partlyReverted = original.copy(
            personal = target.personal,
            challenges = original.challenges,
            singleActivityChallenges = original.singleActivityChallenges,
            groups = target.groups,
            spaces = target.spaces,
        )
        val inspection = Phase1V2RestoreDomainForensics.inspectWithReverseJournal(
            checkpoint(Phase1V2RestoreStep.VERIFYING),
            original, target, partlyReverted,
            emptyList(), emptyList(), emptyList(),
        )
        assertEquals(Phase1V2RecoveryDomainState.TARGET,
            inspection.domains[Phase1V2RecoveryDomain.PERSONAL])
        assertEquals(Phase1V2RecoveryDomainState.ORIGINAL,
            inspection.domains[Phase1V2RecoveryDomain.SINGLE_ACTIVITY])
        assertEquals(Phase1V2RecoveryDomainState.TARGET,
            inspection.domains[Phase1V2RecoveryDomain.GROUPS])
        assertTrue(inspection.requiresIndependentPhotoProof)
        assertFalse(inspection.authorizesLiveWrite)
    }

    @Test fun inverseMarkerStillRejectsForgedDoneHistoryAndUnknownDomainStates() {
        val forged = target.copy(singleActivityChallenges =
            Phase1HabitCodec.encode(listOf(
                habit.confirmToday(Instant.parse("2026-11-07T11:00:00Z")),
            )))
        assertThrows(IllegalArgumentException::class.java) {
            Phase1V2RestoreDomainForensics.inspectWithReverseJournal(
                checkpoint(Phase1V2RestoreStep.VERIFYING),
                original, target, forged, emptyList(),
                emptyList(), emptyList(),
            )
        }
        assertThrows(IllegalArgumentException::class.java) {
            Phase1V2RestoreDomainForensics.inspectWithReverseJournal(
                checkpoint(Phase1V2RestoreStep.VERIFYING),
                original, target.copy(groups = "[]"),
                original, emptyList(), emptyList(), emptyList(),
            )
        }
    }

}
