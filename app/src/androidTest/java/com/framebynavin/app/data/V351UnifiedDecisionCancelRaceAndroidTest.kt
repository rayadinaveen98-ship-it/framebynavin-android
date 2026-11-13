package com.framebynavin.app.data

import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import java.io.File
import java.util.UUID
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith

/**
 * All journals and approvals are synthetic and isolated under app-private
 * test siblings. Never accesses a real owner backup or live data store.
 */
@RunWith(AndroidJUnit4::class)
class V351UnifiedDecisionCancelRaceAndroidTest {
    private val app get() = InstrumentationRegistry.getInstrumentation().targetContext

    private fun checkpoint() = V344UnifiedRestoreCheckpoint(
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

    private fun proof(record: V344UnifiedRestoreCheckpoint) =
        V346InterruptedRecoveryProof(
            journal = record,
            sealedArchives = V345UnifiedAuthenticatedPairProof(
                sourceId = record.sourceId,
                sourceSha256 = record.sourceSha256,
                originalEscrowId = record.escrowId,
                originalEscrowSha256 = record.escrowSha256,
                targetCreatorSha256 = "e".repeat(64),
                targetPersonalSha256 = "f".repeat(64),
                originalCreatorSha256 = "a".repeat(64),
                originalPersonalSha256 = "b".repeat(64),
                targetCreatorProjects = 1,
                originalCreatorProjects = 2,
                targetPersonalSummary = "synthetic",
                originalPersonalSummary = "synthetic",
            ),
            recoveryKind = V344UnifiedRestorePolicy.classify(record.step),
            creatorStandaloneRecoveryMarker = false,
            personalStandaloneRecoveryMarker = false,
        )

    @Test fun decisionRecordedFirstPreventsLaterNoWriteJournalArchive() = runBlocking {
        val id = UUID.randomUUID()
        val root = File(app.filesDir, "v351-journal-$id")
        val decisionRoot = File(app.filesDir, "v351-choice-$id")
        val history = File(app.filesDir, "v351-history-$id")
        try {
            assertTrue(history.mkdir())
            val journal = V344UnifiedRestoreJournal(app, root)
            val start = journal.prepare(checkpoint(), ownerApproved = true)
            val choices = V347OwnerRecoveryDecisionJournal(
                app, decisionRoot, journal,
            )
            choices.recordChoice(
                proof(start),
                V347OwnerRecoveryChoice.REQUEST_REVERSE_ORIGINAL,
                ownerConfirmedChoice = true,
            )
            assertTrue(decisionRoot.exists())
            assertThrows(IllegalStateException::class.java) {
                runBlocking {
                    journal.archiveUnwrittenCheckpoint(
                        start,
                        File(history, "${start.transactionId}-aborted"),
                        ownerConfirmed = true,
                        otherRecoveryPending = { decisionRoot.exists() },
                    )
                }
            }
            assertEquals(start, journal.requireCheckpointReadOnly())
            assertTrue(root.exists())
        } finally {
            root.deleteRecursively()
            decisionRoot.deleteRecursively()
            history.deleteRecursively()
        }
    }

    @Test fun cancelledFirstRejectsSubsequentRecoveryDirectionAndPreservesHistory() = runBlocking {
        val id = UUID.randomUUID()
        val root = File(app.filesDir, "v351-journal-$id")
        val decisionRoot = File(app.filesDir, "v351-choice-$id")
        val history = File(app.filesDir, "v351-history-$id")
        try {
            assertTrue(history.mkdir())
            val journal = V344UnifiedRestoreJournal(app, root)
            val start = journal.prepare(checkpoint(), ownerApproved = true)
            val archived = File(history, "${start.transactionId}-aborted")
            assertTrue(journal.archiveUnwrittenCheckpoint(
                start, archived, ownerConfirmed = true,
                otherRecoveryPending = { false },
            ))
            val choices = V347OwnerRecoveryDecisionJournal(
                app, decisionRoot, journal,
            )
            assertThrows(Exception::class.java) {
                runBlocking {
                    choices.recordChoice(
                        proof(start),
                        V347OwnerRecoveryChoice.REQUEST_FORWARD_TARGET,
                        ownerConfirmedChoice = true,
                    )
                }
            }
            assertFalse(decisionRoot.exists())
            assertTrue(archived.isDirectory)
            assertEquals(start, V344UnifiedRestoreCheckpointCodec.decode(
                File(archived, "active.journal").readBytes(),
            ))
        } finally {
            root.deleteRecursively()
            decisionRoot.deleteRecursively()
            history.deleteRecursively()
        }
    }

    @Test fun checkpointAdvancedFirstRefusesStaleOwnerDirection() = runBlocking {
        val id = UUID.randomUUID()
        val root = File(app.filesDir, "v351-journal-$id")
        val decisionRoot = File(app.filesDir, "v351-choice-$id")
        try {
            val journal = V344UnifiedRestoreJournal(app, root)
            val first = journal.prepare(checkpoint(), ownerApproved = true)
            val later = journal.checkpoint(
                first, V344UnifiedRestoreStep.BEFORE_CREATOR,
                first.updatedAtMillis + 1L,
            )
            assertThrows(Exception::class.java) {
                runBlocking {
                    V347OwnerRecoveryDecisionJournal(app, decisionRoot, journal)
                        .recordChoice(
                            proof(first),
                            V347OwnerRecoveryChoice.REQUEST_FORWARD_TARGET,
                            ownerConfirmedChoice = true,
                        )
                }
            }
            assertFalse(decisionRoot.exists())
            assertEquals(later, journal.requireCheckpointReadOnly())
        } finally {
            root.deleteRecursively(); decisionRoot.deleteRecursively()
        }
    }
}
