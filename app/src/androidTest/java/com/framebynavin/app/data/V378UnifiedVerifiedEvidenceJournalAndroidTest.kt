package com.framebynavin.app.data

import android.content.Context
import android.content.ContextWrapper
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import java.io.File
import java.util.UUID
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith

/** Synthetic in-process app-private evidence; NEVER changes owner backups. */
@RunWith(AndroidJUnit4::class)
class V378UnifiedVerifiedEvidenceJournalAndroidTest {
    private val phone get() = InstrumentationRegistry.getInstrumentation().targetContext
    private fun isolated(root: File): Context = object : ContextWrapper(phone) {
        override fun getFilesDir(): File = root
        override fun getApplicationContext(): Context = this
    }

    private val checkpoint = V344UnifiedRestoreCheckpoint(
        transactionId = UUID.randomUUID().toString(),
        sourceId = UUID.randomUUID().toString(),
        sourceSha256 = "a".repeat(64),
        escrowId = UUID.randomUUID().toString(),
        escrowSha256 = "b".repeat(64),
        creatorOriginalSha256 = "c".repeat(64),
        personalOriginalSha256 = "d".repeat(64),
        step = V344UnifiedRestoreStep.BOTH_VERIFIED,
        createdAtMillis = 1_900_000_000_000L,
        updatedAtMillis = 1_900_000_000_100L,
    )
    private val choice = V347OwnerRecoveryDecision(
        decisionId = UUID.randomUUID().toString(),
        transactionId = checkpoint.transactionId,
        choice = V347OwnerRecoveryChoice.REQUEST_FORWARD_TARGET,
        sourceId = checkpoint.sourceId,
        sourceSha256 = checkpoint.sourceSha256,
        escrowId = checkpoint.escrowId,
        escrowSha256 = checkpoint.escrowSha256,
        checkpointStep = V344UnifiedRestoreStep.BEFORE_CREATOR,
        checkpointUpdatedAtMillis = checkpoint.createdAtMillis,
        checkpointSha256 = "1".repeat(64),
        targetCreatorSha256 = "2".repeat(64),
        targetPersonalSha256 = "3".repeat(64),
        originalCreatorSha256 = "4".repeat(64),
        originalPersonalSha256 = "5".repeat(64),
        requestedAtMillis = checkpoint.createdAtMillis,
    )
    private val proof = V377UnifiedFinalReadOnlyProof(
        transactionId = checkpoint.transactionId,
        checkpointSha256 = V378UnifiedVerifiedEvidenceJournal.sha256(
            V344UnifiedRestoreCheckpointCodec.encode(checkpoint),
        ),
        ownerDirectionSha256 = V378UnifiedVerifiedEvidenceJournal.sha256(
            V347OwnerRecoveryDecisionCodec.encode(choice),
        ),
        outcome = V377VerifiedOwnerOutcome.SELECTED_TARGET_RESTORED,
        expectedCreatorRecordSha256 = "6".repeat(64),
        liveCreatorRecordSha256 = "6".repeat(64),
        expectedPersonalSha256 = "7".repeat(64),
        livePersonalSha256 = "7".repeat(64),
        voiceIdeaCount = 2,
        bestFrameCount = 1,
        originalPhotoCount = 2,
        selectedPhotoCount = 3,
        preservedImportedJpegFiles = 1,
        archivedNativePersonalVerified = true,
    )

    @Test fun durableProofSurvivesReopenAndCannotReleaseOwnerEscrow() {
        val root = File(phone.filesDir, "synthetic-v378-" + UUID.randomUUID())
        assertTrue(root.mkdir())
        try {
            val journal = V378UnifiedVerifiedEvidenceJournal(isolated(root))
            assertFalse(journal.hasEvidence())
            assertEquals(proof, journal.prepareUnderBothOwnerReadLocks(
                true, checkpoint, choice, proof,
            ))
            assertEquals(proof, journal.inspect())
            assertFalse(proof.mayReleaseJournal)
            assertFalse(proof.mayRearmAlarms)
            assertFalse(proof.mayDeleteImportedMedia)
            assertFalse(proof.mayDeleteOriginalEscrow)
            assertTrue(runCatching {
                journal.prepareUnderBothOwnerReadLocks(
                    true, checkpoint, choice, proof,
                )
            }.isFailure)
        } finally { root.deleteRecursively() }
    }

    @Test fun forgedOrUnverifiedProofCannotCreateFinalReceipt() {
        val root = File(phone.filesDir, "synthetic-v378-" + UUID.randomUUID())
        assertTrue(root.mkdir())
        try {
            val journal = V378UnifiedVerifiedEvidenceJournal(isolated(root))
            assertTrue(runCatching {
                journal.prepareUnderBothOwnerReadLocks(
                    true, checkpoint, choice,
                    proof.copy(archivedNativePersonalVerified = false),
                )
            }.isFailure)
            assertTrue(runCatching {
                journal.prepareUnderBothOwnerReadLocks(
                    true, checkpoint, choice,
                    proof.copy(expectedCreatorRecordSha256 = "0".repeat(64)),
                )
            }.isFailure)
            assertTrue(runCatching {
                journal.prepareUnderBothOwnerReadLocks(
                    true, checkpoint, choice,
                    proof.copy(checkpointSha256 = "0".repeat(64)),
                )
            }.isFailure)
            assertFalse(journal.hasEvidence())
        } finally { root.deleteRecursively() }
    }

    @Test fun editedOrExtraEvidenceFileStaysBlocking() {
        val root = File(phone.filesDir, "synthetic-v378-" + UUID.randomUUID())
        assertTrue(root.mkdir())
        try {
            val journal = V378UnifiedVerifiedEvidenceJournal(isolated(root))
            journal.prepareUnderBothOwnerReadLocks(true, checkpoint, choice, proof)
            val dir = File(root, "backlot-unified-verified-outcome-v1")
            val extra = File(dir, "verified.json.bak")
            extra.writeText("torn synthetic result")
            assertTrue(runCatching { journal.inspect() }.isFailure)
            assertTrue(extra.exists())
            assertTrue(File(dir, "verified.json").exists())
        } finally { root.deleteRecursively() }
    }
}
