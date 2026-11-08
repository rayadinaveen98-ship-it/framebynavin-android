package com.framebynavin.app.personal

import com.framebynavin.app.personal.spaces.V289SpaceState
import com.framebynavin.app.personal.spaces.V289SpacesMetadataCodec
import java.time.Instant
import java.time.LocalDate
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Test

class Phase1V2RestoreCheckpointPolicyTest {
    private val originalReceipt = Instant.parse("2026-11-07T04:00:00Z")
    private val alteredReceipt = Instant.parse("2026-11-07T07:00:00Z")
    private val activity = Phase1HabitChallenge(
        id = "v2-restore-fixture", activity = "Write daily",
        startDate = LocalDate.of(2026, 11, 7), zoneId = "Asia/Kolkata",
    ).checked()

    private val empty = V300PersonalComponents(
        personal = PersonalCompletionBackupCodec.encode(emptyList(), emptyList()),
        challenges = V297ChallengeCodec.encode(emptyList()),
        groups = V298ReminderGroupCodec.encode(emptyList()),
        spaces = V289SpacesMetadataCodec.encode(V289SpaceState()),
        singleActivityChallenges = Phase1HabitCodec.encode(emptyList()),
    )
    private val original = empty.copy(
        singleActivityChallenges = Phase1HabitCodec.encode(
            listOf(activity.confirmToday(originalReceipt))),
    )
    private val target = empty.copy(
        singleActivityChallenges = Phase1HabitCodec.encode(
            listOf(activity.confirmToday(alteredReceipt))),
    )

    private fun checkpoint(step: Phase1V2RestoreStep): Phase1V2RestoreCheckpoint =
        Phase1V2RestoreCheckpoint(
            transactionId = "e751c74e-8d47-4f69-bd46-c95c45d04c01",
            preparedId = "e751c74e-8d47-4f69-bd46-c95c45d04c01",
            escrowId = "613c863a-b019-4a10-9824-54f655f174aa",
            originalDigest = Phase1V2RestoreFingerprint.calculate(original, emptyList()),
            targetDigest = Phase1V2RestoreFingerprint.calculate(target, emptyList()),
            step = step, createdAtMillis = 10_000L, updatedAtMillis = 10_010L,
        )

    private fun inspect(
        step: Phase1V2RestoreStep,
        observed: V300PersonalComponents,
    ): Phase1V2HabitsRecoveryAssessment = Phase1V2HabitsRecoveryPolicy.inspect(
        checkpoint(step), original, target, observed, emptyList(), emptyList(),
    )

    @Test fun exactConfirmedInstantChangesWholeV2SnapshotFingerprint() {
        val a = Phase1V2RestoreFingerprint.calculate(original, emptyList())
        val b = Phase1V2RestoreFingerprint.calculate(target, emptyList())
        assertNotEquals(a, b)
        assertTrue(a.matches(Regex("[a-f0-9]{64}")))
        assertEquals(a, Phase1V2RestoreFingerprint.calculate(original, emptyList()))
    }

    @Test fun legacyV1ComponentsCannotBeAuthenticatedAsCompleteV2Snapshot() {
        assertThrows(IllegalArgumentException::class.java) {
            Phase1V2RestoreFingerprint.calculate(empty.copy(singleActivityChallenges = null),
                emptyList())
        }
    }

    @Test fun exactV2CheckpointRoundTripIsNeverAWritePermit() {
        val staged = checkpoint(Phase1V2RestoreStep.BEFORE_SINGLE_ACTIVITY)
        val decoded = Phase1V2RestoreJournalCodec.decode(
            Phase1V2RestoreJournalCodec.encode(staged))
        assertEquals(staged, decoded)
        assertFalse(decoded.canRestoreLiveData)
        assertFalse(decoded.canFinalizeOrReleaseJournal)
        assertFalse(decoded.mayRecoverAutomatically)
    }

    @Test fun v2AndLegacyV1JournalFormatsCannotBeConfused() {
        val two = Phase1V2RestoreJournalCodec.encode(
            checkpoint(Phase1V2RestoreStep.BEFORE_PERSONAL))
        assertThrows(IllegalArgumentException::class.java) {
            StabilizationRestoreJournalCodec.decode(two)
        }
        val one = StabilizationRestoreJournalCodec.encode(
            StabilizationRestoreJournalRecord(
                transactionId = "e751c74e-8d47-4f69-bd46-c95c45d04c01",
                preparedId = "e751c74e-8d47-4f69-bd46-c95c45d04c01",
                escrowId = "613c863a-b019-4a10-9824-54f655f174aa",
                originalDigest = checkpoint(Phase1V2RestoreStep.PREPARED).originalDigest,
                targetDigest = checkpoint(Phase1V2RestoreStep.PREPARED).targetDigest,
                step = StabilizationRestoreWriteStep.PREPARED,
                createdAtMillis = 10_000L, updatedAtMillis = 10_010L,
            ))
        assertThrows(IllegalArgumentException::class.java) {
            Phase1V2RestoreJournalCodec.decode(one)
        }
        assertEquals(StabilizationRestoreWriteStep.PREPARED,
            StabilizationRestoreJournalCodec.decode(one).step)
    }

    @Test fun corruptedOrTruncatedCheckpointIsRejected() {
        val raw = Phase1V2RestoreJournalCodec.encode(
            checkpoint(Phase1V2RestoreStep.BEFORE_SINGLE_ACTIVITY))
        assertThrows(IllegalArgumentException::class.java) {
            Phase1V2RestoreJournalCodec.decode(
                raw.toString(Charsets.UTF_8)
                    .replace("BEFORE_SINGLE_ACTIVITY", "SINGLE_ACTIVITY_WRITTEN")
                    .toByteArray(Charsets.UTF_8))
        }
        assertThrows(IllegalArgumentException::class.java) {
            Phase1V2RestoreJournalCodec.decode(raw.copyOf(raw.size - 8))
        }
    }

    @Test fun journalProgressCannotSkipRepeatOrRewindAStore() {
        val staged = checkpoint(Phase1V2RestoreStep.BEFORE_LEGACY_CHALLENGES)
        val advanced = Phase1V2RestoreJournalCodec.advance(
            staged, Phase1V2RestoreStep.LEGACY_CHALLENGES_WRITTEN, 10_015L)
        assertEquals(Phase1V2RestoreStep.LEGACY_CHALLENGES_WRITTEN, advanced.step)
        assertThrows(IllegalArgumentException::class.java) {
            Phase1V2RestoreJournalCodec.advance(
                staged, Phase1V2RestoreStep.BEFORE_SINGLE_ACTIVITY, 10_015L)
        }
        assertThrows(IllegalArgumentException::class.java) {
            Phase1V2RestoreJournalCodec.advance(
                advanced, Phase1V2RestoreStep.LEGACY_CHALLENGES_WRITTEN, 10_015L)
        }
        assertThrows(IllegalArgumentException::class.java) {
            Phase1V2RestoreJournalCodec.advance(
                advanced, Phase1V2RestoreStep.BEFORE_SINGLE_ACTIVITY, 10_000L)
        }
    }

    @Test fun beforeNewDataStoreCheckpointOnlyOriginalIsValid() {
        val receipt = inspect(Phase1V2RestoreStep.LEGACY_CHALLENGES_WRITTEN, original)
        assertEquals(Phase1V2HabitsRecoveryState.ORIGINAL, receipt.state)
        assertFalse(receipt.mayWrite)
        assertThrows(IllegalArgumentException::class.java) {
            inspect(Phase1V2RestoreStep.LEGACY_CHALLENGES_WRITTEN, target)
        }
    }

    @Test fun atBeforeNewDataStoreCheckpointEitherWholeRevisionMayBePresent() {
        assertEquals(Phase1V2HabitsRecoveryState.ORIGINAL,
            inspect(Phase1V2RestoreStep.BEFORE_SINGLE_ACTIVITY, original).state)
        assertEquals(Phase1V2HabitsRecoveryState.TARGET,
            inspect(Phase1V2RestoreStep.BEFORE_SINGLE_ACTIVITY, target).state)
    }

    @Test fun afterDurableNewDataStoreCheckpointOriginalReceiptIsRejected() {
        assertEquals(Phase1V2HabitsRecoveryState.TARGET,
            inspect(Phase1V2RestoreStep.SINGLE_ACTIVITY_WRITTEN, target).state)
        assertThrows(IllegalArgumentException::class.java) {
            inspect(Phase1V2RestoreStep.SINGLE_ACTIVITY_WRITTEN, original)
        }
    }

    @Test fun forgedOrThirdValueCannotPassCrashForensics() {
        assertThrows(IllegalArgumentException::class.java) {
            inspect(Phase1V2RestoreStep.BEFORE_SINGLE_ACTIVITY, empty)
        }
        assertThrows(IllegalArgumentException::class.java) {
            Phase1V2HabitsRecoveryPolicy.inspect(
                checkpoint(Phase1V2RestoreStep.BEFORE_SINGLE_ACTIVITY)
                    .copy(targetDigest = "0".repeat(64)),
                original, target, original, emptyList(), emptyList())
        }
    }
}
