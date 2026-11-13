package com.framebynavin.app.data

import java.util.UUID
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/** Pure policy tests: no Android owner files or media operations. */
class V371VoiceShadowPolicyTest {
    private val original = V365VoiceCopyPlan(
        transactionId = UUID.randomUUID().toString(),
        sourceSha256 = "a".repeat(64),
        escrowSha256 = "b".repeat(64),
        entries = listOf(
            V365VoiceCopyPlanEntry(
                ideaId = "idea-a",
                fileName = "voice_idea_" + UUID.randomUUID() + ".m4a",
                sizeBytes = 100L,
                sha256 = "c".repeat(64),
            ),
            V365VoiceCopyPlanEntry(
                ideaId = "idea-b",
                fileName = "voice_idea_" + UUID.randomUUID() + ".m4a",
                sizeBytes = 200L,
                sha256 = "d".repeat(64),
            ),
        ),
    )
    private val damaged = V371VoiceShadowEntry(
        ideaId = "idea-a",
        damagedFile = original.entries[0].fileName,
        damagedBytes = 17L,
        damagedSha256 = "e".repeat(64),
        shadowFile = "voice_idea_" + UUID.randomUUID() + ".m4a",
        expectedBytes = original.entries[0].sizeBytes,
        expectedSha256 = original.entries[0].sha256,
    )
    private val plan = V371VoiceShadowPlan(
        transactionId = original.transactionId,
        sourceSha256 = original.sourceSha256,
        escrowSha256 = original.escrowSha256,
        allocationSha256 = V371VoiceShadowPolicy.allocationDigest(original),
        entries = listOf(damaged),
    )
    private fun accepted(item: V371VoiceShadowPlan): Boolean =
        runCatching { V371VoiceShadowPolicy.validate(item, original) }.isSuccess

    @Test fun immutableDistinctShadowPlanMayDescribePreviouslyDamagedBytes() {
        assertTrue(accepted(plan))
        assertFalse(plan.mayOverwriteDamagedMedia)
        assertFalse(plan.mayImportCreatorRecords)
        assertFalse(plan.mayReleaseEscrow)
    }

    @Test fun incorrectOwnerOrAllocationFingerprintRejected() {
        assertFalse(accepted(plan.copy(transactionId = UUID.randomUUID().toString())))
        assertFalse(accepted(plan.copy(sourceSha256 = "f".repeat(64))))
        assertFalse(accepted(plan.copy(allocationSha256 = "f".repeat(64))))
        assertFalse(accepted(plan.copy(entries = emptyList())))
    }

    @Test fun newCopyCannotOverwriteAnyExistingPlannedRecordingOrAnotherShadow() {
        assertFalse(accepted(plan.copy(entries = listOf(
            damaged.copy(shadowFile = damaged.damagedFile),
        ))))
        assertFalse(accepted(plan.copy(entries = listOf(
            damaged.copy(shadowFile = original.entries[1].fileName),
        ))))
        assertFalse(accepted(plan.copy(entries = listOf(damaged, damaged))))
        assertFalse(accepted(plan.copy(entries = listOf(
            damaged.copy(damagedBytes = damaged.expectedBytes,
                damagedSha256 = damaged.expectedSha256),
        ))))
    }

    @Test fun changingSourceByteEvidenceOrTargetReferenceFailsClosed() {
        assertFalse(accepted(plan.copy(entries = listOf(
            damaged.copy(ideaId = "unrelated-idea"),
        ))))
        assertFalse(accepted(plan.copy(entries = listOf(
            damaged.copy(expectedBytes = 101L),
        ))))
        assertFalse(accepted(plan.copy(entries = listOf(
            damaged.copy(expectedSha256 = "f".repeat(64)),
        ))))
        assertFalse(accepted(plan.copy(entries = listOf(
            damaged.copy(damagedSha256 = "corrupt"),
        ))))
    }

    @Test fun missingAndIncompleteShadowInventoryCannotClaimCompletedRecovery() {
        val missing = V371VoiceShadowAudit(1, 0, 1, 0, 0)
        assertFalse(missing.allVerified)
        assertFalse(missing.mayPromoteIntoCreator)
        val torn = V371VoiceShadowAudit(1, 0, 0, 1, 0)
        assertFalse(torn.allVerified)
        val intact = V371VoiceShadowAudit(1, 1, 0, 0, 0)
        assertTrue(intact.allVerified)
        assertFalse(intact.mayPromoteIntoCreator)
    }
}
