package com.framebynavin.app.personal

import org.junit.Assert.*
import org.junit.Test

/** Synthetic SHA-bound replay decisions; no DataStore/Room/JPEG writes. */
class B04ForwardReplayPlanPolicyTest {
    private val original = V300PersonalComponents("old-personal","old-challenges","old-groups","old-spaces")
    private val target = V300PersonalComponents("new-personal","new-challenges","new-groups","new-spaces")
    private val old = V304RestorePlanPolicy.fingerprint(original, emptyList())
    private val newer = V304RestorePlanPolicy.fingerprint(target, emptyList())
    private fun journal(step: StabilizationRestoreWriteStep) =
        StabilizationRestoreJournalRecord(
            transactionId = "e751c74e-8d47-4f69-bd46-c95c45d04c01",
            preparedId = "e751c74e-8d47-4f69-bd46-c95c45d04c01",
            escrowId = "613c863a-b019-4085-9df3-5f38b84454ea",
            originalDigest = old, targetDigest = newer,
            step = step, createdAtMillis = 1000, updatedAtMillis = 1001,
        )
    private fun states(step: StabilizationRestoreWriteStep) =
        B04RecoveryDomainAssessment(step, B04RecoveryDomain.entries.associateWith { d ->
            val before = when(d) {
                B04RecoveryDomain.PERSONAL -> StabilizationRestoreWriteStep.BEFORE_PERSONAL
                B04RecoveryDomain.CHALLENGES -> StabilizationRestoreWriteStep.BEFORE_CHALLENGES
                B04RecoveryDomain.GROUPS -> StabilizationRestoreWriteStep.BEFORE_GROUPS
                B04RecoveryDomain.SPACES -> StabilizationRestoreWriteStep.BEFORE_SPACES
            }
            if(step.ordinal > before.ordinal) B04RecoveryDomainState.TARGET
            else B04RecoveryDomainState.ORIGINAL
        })
    private fun photos(step: StabilizationRestoreWriteStep) =
        B04PrivatePhotoForensicReport(
            checkpoint = step, originalVerified = 0, originalMissingOrCorrupt = 0,
            incomingVerified = 0, incomingMissing = 0, incomingCorrupt = 0,
            interruptedPendingFiles = 0, untrackedEntries = 0, prematureIncomingFiles = 0,
            expectedOriginal = 0, expectedIncoming = 0,
        )
    private fun inspect(
        step: StabilizationRestoreWriteStep,
        j: StabilizationRestoreJournalRecord = journal(step),
        d: B04RecoveryDomainAssessment = states(step),
        f: B04PrivatePhotoForensicReport = photos(step),
        approval: Boolean = true,
    ) = B04ForwardReplayPlanPolicy.propose(
        j, original, emptyList(), target, emptyList(), d, f, approval,
    )
    @Test fun preparedRequiresAllFiveWriteStagesAndIndependentVerification() {
        val p=inspect(StabilizationRestoreWriteStep.PREPARED)
        assertEquals(B04ForwardReplayStep.entries, p.remaining)
        assertFalse(p.grantsMutationAuthority)
        assertFalse(p.mayRunWithoutOwnerConsent)
        assertFalse(p.mayClearJournal)
    }
    @Test fun afterPersonalRequiresNextFourPhases() {
        val p=inspect(StabilizationRestoreWriteStep.PERSONAL_WRITTEN)
        assertEquals(listOf(
            B04ForwardReplayStep.CHALLENGES, B04ForwardReplayStep.GROUPS,
            B04ForwardReplayStep.SPACES, B04ForwardReplayStep.PHOTOS,
            B04ForwardReplayStep.VERIFY,
        ),p.remaining)
    }
    @Test fun beforeChallengeMayNeedIdempotentChallengeReplay() {
        val p=inspect(StabilizationRestoreWriteStep.BEFORE_CHALLENGES)
        assertEquals(B04ForwardReplayStep.CHALLENGES,p.remaining.first())
    }
    @Test fun afterSpacesOnlyNeedsPhotosAndVerification() {
        val p=inspect(StabilizationRestoreWriteStep.SPACES_WRITTEN)
        assertEquals(listOf(B04ForwardReplayStep.PHOTOS,
            B04ForwardReplayStep.VERIFY),p.remaining)
    }
    @Test fun beforePhotosMustRevalidatePartialCompletedFiles() {
        val p=inspect(StabilizationRestoreWriteStep.BEFORE_PHOTOS)
        assertEquals(listOf(B04ForwardReplayStep.PHOTOS,
            B04ForwardReplayStep.VERIFY),p.remaining)
    }
    @Test fun photosWrittenNeedsIndependentPostWriteVerification() {
        val p=inspect(StabilizationRestoreWriteStep.PHOTOS_WRITTEN)
        assertEquals(listOf(B04ForwardReplayStep.VERIFY),p.remaining)
    }
    @Test fun verifyingNeverGrantsFinalization() {
        val p=inspect(StabilizationRestoreWriteStep.VERIFYING)
        assertEquals(listOf(B04ForwardReplayStep.VERIFY),p.remaining)
        assertFalse(p.mayClearJournal)
    }
    @Test fun missingOwnerRequestIsRejected() {
        assertThrows(IllegalArgumentException::class.java) {
            inspect(StabilizationRestoreWriteStep.PREPARED,approval=false)
        }
    }
    @Test fun mismatchedOriginalDigestIsRejected() {
        val j=journal(StabilizationRestoreWriteStep.PREPARED)
        assertThrows(IllegalArgumentException::class.java) {
            inspect(j.step,j=j.copy(originalDigest="a".repeat(64)))
        }
    }
    @Test fun changedTargetDigestIsRejected() {
        val j=journal(StabilizationRestoreWriteStep.PREPARED)
        assertThrows(IllegalArgumentException::class.java) {
            inspect(j.step,j=j.copy(targetDigest="b".repeat(64)))
        }
    }
    @Test fun mismatchedDomainCheckpointIsRejected() {
        val step=StabilizationRestoreWriteStep.BEFORE_GROUPS
        assertThrows(IllegalArgumentException::class.java) {
            inspect(step,d=states(StabilizationRestoreWriteStep.BEFORE_PERSONAL))
        }
    }
    @Test fun mismatchedPhotoCheckpointIsRejected() {
        val step=StabilizationRestoreWriteStep.BEFORE_GROUPS
        assertThrows(IllegalArgumentException::class.java) {
            inspect(step,f=photos(StabilizationRestoreWriteStep.BEFORE_PHOTOS))
        }
    }
    @Test fun noDomainCanBeUnaccountedFor() {
        val step=StabilizationRestoreWriteStep.PREPARED
        assertThrows(IllegalArgumentException::class.java) {
            inspect(step,d=states(step).copy(states=states(step).states -
                B04RecoveryDomain.GROUPS))
        }
    }
    @Test fun afterCheckpointStillOriginalIsRejected() {
        val step=StabilizationRestoreWriteStep.PERSONAL_WRITTEN
        val d=states(step).copy(states=states(step).states +
            (B04RecoveryDomain.PERSONAL to B04RecoveryDomainState.ORIGINAL))
        assertThrows(IllegalArgumentException::class.java) {inspect(step,d=d)}
    }
    @Test fun futureDomainAlreadyWrittenWithoutBeforeJournalIsRejected() {
        val step=StabilizationRestoreWriteStep.PREPARED
        val d=states(step).copy(states=states(step).states +
            (B04RecoveryDomain.SPACES to B04RecoveryDomainState.TARGET))
        assertThrows(IllegalArgumentException::class.java) {inspect(step,d=d)}
    }
    @Test fun pendingJpegCopyDisallowsReplay() {
        val step=StabilizationRestoreWriteStep.BEFORE_PHOTOS
        assertThrows(IllegalArgumentException::class.java) {
            inspect(step,f=photos(step).copy(interruptedPendingFiles=1))
        }
    }
    @Test fun untrackedPrivateFileDisallowsReplay() {
        val step=StabilizationRestoreWriteStep.BEFORE_PHOTOS
        assertThrows(IllegalArgumentException::class.java) {
            inspect(step,f=photos(step).copy(untrackedEntries=1))
        }
    }
    @Test fun missingPhotoBytesAfterCompletedCheckpointFailsClosed() {
        val step=StabilizationRestoreWriteStep.VERIFYING
        assertThrows(IllegalArgumentException::class.java) {
            inspect(step,f=photos(step).copy(expectedIncoming=1,incomingMissing=1))
        }
    }
    @Test fun prematurelyInstalledPhotoDisallowsReplay() {
        val step=StabilizationRestoreWriteStep.PERSONAL_WRITTEN
        assertThrows(IllegalArgumentException::class.java) {
            inspect(step,f=photos(step).copy(prematureIncomingFiles=1))
        }
    }
}
