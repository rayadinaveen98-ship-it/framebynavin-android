package com.framebynavin.app.personal

import com.framebynavin.app.personal.spaces.V289PersonalSpacePolicy
import com.framebynavin.app.personal.spaces.V289SpaceReminderLink
import com.framebynavin.app.personal.spaces.V289SpaceState
import com.framebynavin.app.personal.spaces.V289SpaceType
import com.framebynavin.app.personal.spaces.V289SpacesMetadataCodec
import java.time.LocalDate
import java.time.LocalTime
import org.junit.Assert.*
import org.junit.Test

class B04RecoveryDomainCheckpointPolicyTest {
    private val owner = PersonalReminder(
        id = "recover-owner", title = "Preserve owner",
        startDate = LocalDate.of(2026, 12, 11), time = LocalTime.of(8, 30),
        createdAtMillis = 100L, updatedAtMillis = 100L,
    )
    private val imported = owner.copy(
        id = "recover-imported", title = "Synthetic incoming", createdAtMillis = 200L,
        updatedAtMillis = 200L,
    )
    private val plan = V297Challenge(
        id = "recover-plan", title = "Imported challenge",
        startDate = LocalDate.of(2026, 12, 12), createdAtMillis = 200L,
    )
    private val group = V298ReminderGroup(
        id = "recover-group", name = "Imported group",
        reminderIds = listOf(imported.id), createdAtMillis = 200L, updatedAtMillis = 200L,
    )
    private val spaceId = "afcd17ed-5fd6-4a60-b8cb-31484ee653e7"
    private val state = V289SpaceState(
        spaces = listOf(V289PersonalSpacePolicy.newSpace(V289SpaceType.FITNESS, 200L, spaceId)),
        links = listOf(V289SpaceReminderLink(spaceId, imported.id, 300L)),
    )
    private val baseline = V300PersonalComponents(
        personal = PersonalCompletionBackupCodec.encode(listOf(owner), emptyList()),
        challenges = V297ChallengeCodec.encode(emptyList()),
        groups = V298ReminderGroupCodec.encode(emptyList()),
        spaces = V289SpacesMetadataCodec.encode(V289SpaceState()),
    )
    private val target = V300PersonalComponents(
        personal = PersonalCompletionBackupCodec.encode(listOf(owner, imported), emptyList()),
        challenges = V297ChallengeCodec.encode(listOf(plan)),
        groups = V298ReminderGroupCodec.encode(listOf(group)),
        spaces = V289SpacesMetadataCodec.encode(state),
    )
    private fun journal(step: StabilizationRestoreWriteStep) =
        StabilizationRestoreJournalRecord(
            transactionId = "e751c74e-8d47-4f69-bd46-c95c45d04c01",
            preparedId = "e751c74e-8d47-4f69-bd46-c95c45d04c01",
            escrowId = "613c863a-b019-4085-9df3-5f38b84454ea",
            originalDigest = "a".repeat(64), targetDigest = "b".repeat(64),
            step = step, createdAtMillis = 1000L, updatedAtMillis = 1001L,
        )
    private fun check(step: StabilizationRestoreWriteStep, c: V300PersonalComponents) =
        B04RecoveryDomainCheckpointPolicy.inspect(journal(step), baseline, target, c)
    private fun rejected(step: StabilizationRestoreWriteStep, c: V300PersonalComponents) {
        assertThrows(Exception::class.java) { check(step, c) }
    }

    @Test fun preparedNeedsUnmodifiedLocalData() {
        val state = check(StabilizationRestoreWriteStep.PREPARED, baseline)
        assertEquals(B04RecoveryDomainState.ORIGINAL, state.states[B04RecoveryDomain.PERSONAL])
        assertFalse(state.mayAutomaticallyResume)
        assertFalse(state.grantsRestoreAuthority)
    }
    @Test fun beforePersonalAllowsOriginal() {
        assertEquals(B04RecoveryDomainState.ORIGINAL,
            check(StabilizationRestoreWriteStep.BEFORE_PERSONAL, baseline)
                .states[B04RecoveryDomain.PERSONAL])
    }
    @Test fun beforePersonalAllowsAlreadyDurablePersonal() {
        val actual = baseline.copy(personal = target.personal)
        assertEquals(B04RecoveryDomainState.TARGET,
            check(StabilizationRestoreWriteStep.BEFORE_PERSONAL, actual)
                .states[B04RecoveryDomain.PERSONAL])
    }
    @Test fun completedPersonalMustContainTarget() {
        rejected(StabilizationRestoreWriteStep.PERSONAL_WRITTEN, baseline)
    }
    @Test fun personalCannotChangeBeforeItsCheckpoint() {
        rejected(StabilizationRestoreWriteStep.PREPARED,
            baseline.copy(personal = target.personal))
    }
    @Test fun challengeCannotAppearBeforeItsCheckpoint() {
        rejected(StabilizationRestoreWriteStep.PERSONAL_WRITTEN,
            baseline.copy(personal = target.personal, challenges = target.challenges))
    }
    @Test fun beforeChallengeCanHaveWrittenTarget() {
        val actual = baseline.copy(personal = target.personal, challenges = target.challenges)
        assertEquals(B04RecoveryDomainState.TARGET,
            check(StabilizationRestoreWriteStep.BEFORE_CHALLENGES, actual)
                .states[B04RecoveryDomain.CHALLENGES])
    }
    @Test fun completedChallengeRequiresTargetNotBaseline() {
        rejected(StabilizationRestoreWriteStep.CHALLENGES_WRITTEN,
            baseline.copy(personal = target.personal))
    }
    @Test fun beforeGroupCanHaveWrittenGroupButNoSpacesYet() {
        val actual = target.copy(spaces = baseline.spaces)
        assertEquals(B04RecoveryDomainState.TARGET,
            check(StabilizationRestoreWriteStep.BEFORE_GROUPS, actual)
                .states[B04RecoveryDomain.GROUPS])
    }
    @Test fun spaceCannotAppearBeforeItsCheckpoint() {
        rejected(StabilizationRestoreWriteStep.GROUPS_WRITTEN, target)
    }
    @Test fun beforeSpacesCanHaveFullRoomMetadata() {
        assertEquals(B04RecoveryDomainState.TARGET,
            check(StabilizationRestoreWriteStep.BEFORE_SPACES, target)
                .states[B04RecoveryDomain.SPACES])
    }
    @Test fun completedSpacesRequireTargetMetadata() {
        rejected(StabilizationRestoreWriteStep.SPACES_WRITTEN,
            target.copy(spaces = baseline.spaces))
    }
    @Test fun verifyingRequiresEveryPortableDomainTarget() {
        val report = check(StabilizationRestoreWriteStep.VERIFYING, target)
        assertEquals(4, report.states.size)
        assertTrue(report.needsIndependentPhotoAssessment)
        assertFalse(report.provesPrivatePhotosSafe)
    }
    @Test fun verifyingRejectsOriginalChallenge() {
        rejected(StabilizationRestoreWriteStep.VERIFYING,
            target.copy(challenges = baseline.challenges))
    }
    @Test fun incompleteOrThirdPartyMutationAlwaysFails() {
        rejected(StabilizationRestoreWriteStep.BEFORE_GROUPS,
            target.copy(groups = V298ReminderGroupCodec.encode(listOf(group.copy(name = "altered"))),
                spaces = baseline.spaces))
    }
    @Test fun corruptRecordNeverGetsAssumedEmpty() {
        rejected(StabilizationRestoreWriteStep.PREPARED,
            baseline.copy(groups = "{}"))
    }
    @Test fun futureStepsAlwaysNeedSeparatePhotoVerification() {
        for (step in listOf(StabilizationRestoreWriteStep.BEFORE_PHOTOS,
            StabilizationRestoreWriteStep.PHOTOS_WRITTEN,
            StabilizationRestoreWriteStep.VERIFYING)) {
            val assessment = check(step, target)
            assertTrue(assessment.needsIndependentPhotoAssessment)
            assertFalse(assessment.mayAutomaticallyResume)
        }
    }
}
