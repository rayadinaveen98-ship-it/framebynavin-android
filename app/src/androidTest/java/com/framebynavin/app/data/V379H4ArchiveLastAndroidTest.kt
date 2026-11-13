package com.framebynavin.app.data

import android.content.Context
import android.content.ContextWrapper
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import java.io.File
import java.util.UUID
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Fake private data only. This checks the irreversible H4 move stays LAST.
 * An actual release must first independently authenticate both full ZIPs,
 * each media byte, the owner choice and signed-by-digest completion receipt.
 */
@RunWith(AndroidJUnit4::class)
class V379H4ArchiveLastAndroidTest {
    private val phone get() = InstrumentationRegistry.getInstrumentation().targetContext
    private fun fake(root: File): Context = object : ContextWrapper(phone) {
        override fun getApplicationContext(): Context = this
        override fun getFilesDir(): File = root
    }

    @Test fun h4CannotUnfreezeBeforeAllRequiredOriginalEvidenceIsRetained() = runBlocking {
        val root = File(phone.filesDir, "synthetic-h4-last-" + UUID.randomUUID())
        assertTrue(root.mkdir())
        try {
            val ctx = fake(root)
            val h4 = V344UnifiedRestoreJournal(ctx)
            val first = V344UnifiedRestoreCheckpoint(
                transactionId = UUID.randomUUID().toString(),
                sourceId = UUID.randomUUID().toString(),
                sourceSha256 = "a".repeat(64),
                escrowId = UUID.randomUUID().toString(),
                escrowSha256 = "b".repeat(64),
                creatorOriginalSha256 = "c".repeat(64),
                personalOriginalSha256 = "d".repeat(64),
                step = V344UnifiedRestoreStep.PREPARED_NO_WRITES,
                createdAtMillis = 1_900_000_000_000L,
                updatedAtMillis = 1_900_000_000_000L,
            )
            var current = h4.prepare(first, ownerApproved = true)
            V344UnifiedRestoreStep.entries.drop(1).forEach { next ->
                current = h4.checkpoint(current, next, current.updatedAtMillis + 1L)
            }
            val decision = V347OwnerRecoveryDecision(
                decisionId = UUID.randomUUID().toString(),
                transactionId = current.transactionId,
                choice = V347OwnerRecoveryChoice.REQUEST_FORWARD_TARGET,
                sourceId = current.sourceId,
                sourceSha256 = current.sourceSha256,
                escrowId = current.escrowId,
                escrowSha256 = current.escrowSha256,
                checkpointStep = V344UnifiedRestoreStep.BEFORE_CREATOR,
                checkpointUpdatedAtMillis = first.updatedAtMillis + 1L,
                checkpointSha256 = "1".repeat(64),
                targetCreatorSha256 = "2".repeat(64),
                targetPersonalSha256 = "3".repeat(64),
                originalCreatorSha256 = "4".repeat(64),
                originalPersonalSha256 = "5".repeat(64),
                requestedAtMillis = first.updatedAtMillis + 1L,
            )
            val proof = V377UnifiedFinalReadOnlyProof(
                transactionId = current.transactionId,
                checkpointSha256 = V379UnifiedReleaseJournal.sha256(
                    V344UnifiedRestoreCheckpointCodec.encode(current),
                ),
                ownerDirectionSha256 = V379UnifiedReleaseJournal.sha256(
                    V347OwnerRecoveryDecisionCodec.encode(decision),
                ),
                outcome = V377VerifiedOwnerOutcome.SELECTED_TARGET_RESTORED,
                expectedCreatorRecordSha256 = "f".repeat(64),
                liveCreatorRecordSha256 = "f".repeat(64),
                expectedPersonalSha256 = "e".repeat(64),
                livePersonalSha256 = "e".repeat(64),
                voiceIdeaCount = 0,
                bestFrameCount = 0,
                originalPhotoCount = 0,
                selectedPhotoCount = 0,
                preservedImportedJpegFiles = 0,
                archivedNativePersonalVerified = true,
            )
            V378UnifiedVerifiedEvidenceJournal(ctx).prepareUnderBothOwnerReadLocks(
                ownerConfirmed = true, checkpoint = current,
                decision = decision, proof = proof,
            )
            val stages = V379UnifiedReleaseJournal(ctx)
            var state = stages.prepare(
                V379UnifiedReleaseRecord(
                    transactionId = current.transactionId,
                    checkpointSha256 = proof.checkpointSha256,
                    verificationSha256 =
                        V379UnifiedReleaseJournal.proofDigest(proof),
                    outcome = proof.outcome,
                    step = V379ReleaseStep.PREPARED,
                ),
                ownerApproved = true,
            )
            repeat(3) {
                state = stages.advance(
                    state, V379ReleaseStep.entries[state.step.ordinal + 1],
                )
            }
            assertEquals(V379ReleaseStep.READY_TO_ARCHIVE_H4, state.step)
            val history = File(
                File(root, "backlot-unified-retained-recovery-history-v1"),
                current.transactionId,
            )
            // No original owner ZIP evidence has been retained yet.
            assertTrue(runCatching {
                h4.archiveFullyVerifiedCheckpoint(
                    current, proof, state,
                    File(history, V344UnifiedRestoreJournal.ROOT_NAME),
                    ownerConfirmed = true,
                )
            }.isFailure)
            assertTrue(V344UnifiedRestoreJournal.hasPendingFor(ctx))
            for (name in listOf(
                "backlot-unified-owner-recovery-decision-v1",
                "backlot-unified-owner-recovery-escrow-v1",
                "backlot-unified-owner-restore-preparation-v1",
            )) {
                val active = File(root, name)
                assertTrue(active.mkdir())
                File(active, "synthetic-checksum").writeText("inert test evidence")
                assertTrue(V379AtomicOwnerHistory.retain(
                    ctx, current.transactionId, name,
                    required = true,
                )!!.isDirectory)
            }
            assertTrue(h4.archiveFullyVerifiedCheckpoint(
                current, proof, state,
                File(history, V344UnifiedRestoreJournal.ROOT_NAME),
                ownerConfirmed = true,
            ))
            assertFalse(V344UnifiedRestoreJournal.hasPendingFor(ctx))
            assertEquals(
                current,
                V344UnifiedRestoreCheckpointCodec.decode(
                    File(history,
                        V344UnifiedRestoreJournal.ROOT_NAME + "/active.journal")
                        .readBytes(),
                ),
            )
            assertTrue(File(history,
                "backlot-unified-owner-recovery-escrow-v1").isDirectory)
        } finally { root.deleteRecursively() }
    }
}
