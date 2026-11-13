package com.framebynavin.app.data

import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import java.io.File
import java.util.UUID
import kotlinx.coroutines.runBlocking
import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Synthetic cross-domain journal tests. Never invokes a real restore,
 * user backup, Creator/Personal writer, private photo or alarm scheduler.
 */
@RunWith(AndroidJUnit4::class)
class V344UnifiedRestoreJournalAndroidTest {
    private val app get() = InstrumentationRegistry.getInstrumentation().targetContext
    private fun privateTestRoot() = File(app.filesDir, "v344-test-" + UUID.randomUUID())
    private val hashA = "a".repeat(64)
    private val hashB = "b".repeat(64)

    private fun checkpoint(step: V344UnifiedRestoreStep =
        V344UnifiedRestoreStep.PREPARED_NO_WRITES
    ) = V344UnifiedRestoreCheckpoint(
        transactionId = UUID.randomUUID().toString(),
        sourceId = UUID.randomUUID().toString(),
        sourceSha256 = hashA,
        escrowId = UUID.randomUUID().toString(),
        escrowSha256 = hashB,
        creatorOriginalSha256 = hashA,
        personalOriginalSha256 = hashB,
        step = step,
        createdAtMillis = 1_800_000_000_000L,
        updatedAtMillis = 1_800_000_000_000L,
    )

    @Test fun codecBindsBothOwnerDomainsAndRejectsTamperedMetadata() {
        val saved = checkpoint()
        val bytes = V344UnifiedRestoreCheckpointCodec.encode(saved)
        assertEquals(saved, V344UnifiedRestoreCheckpointCodec.decode(bytes))
        val changed = JSONObject(String(bytes)).put("personalOriginalSha256", hashA)
        assertThrows(Exception::class.java) {
            V344UnifiedRestoreCheckpointCodec.decode(changed.toString().toByteArray())
        }
        val unexpected = JSONObject(String(bytes)).put("executeRestore", true)
        assertThrows(Exception::class.java) {
            V344UnifiedRestoreCheckpointCodec.decode(unexpected.toString().toByteArray())
        }
        assertThrows(Exception::class.java) {
            V344UnifiedRestoreCheckpointCodec.decode(ByteArray(5000))
        }
    }

    @Test fun adjacentStagesAreRequiredAndNeverAuthorizeAnAutomaticRecovery() {
        var state = checkpoint()
        assertThrows(IllegalArgumentException::class.java) {
            V344UnifiedRestorePolicy.next(
                state, V344UnifiedRestoreStep.CREATOR_WRITTEN,
                state.updatedAtMillis + 1L,
            )
        }
        for (next in V344UnifiedRestoreStep.entries.drop(1)) {
            state = V344UnifiedRestorePolicy.next(
                state, next, state.updatedAtMillis + 1L,
            )
            assertFalse(V344UnifiedRestorePolicy.mayAutoRunRestoreOrRollback(next))
            assertFalse(V344UnifiedRestorePolicy.mayDiscardOriginalEscrow(next))
        }
        assertEquals(V344UnifiedRestoreStep.BOTH_VERIFIED, state.step)
        assertThrows(IllegalArgumentException::class.java) {
            V344UnifiedRestorePolicy.next(
                state, V344UnifiedRestoreStep.PREPARED_NO_WRITES,
                state.updatedAtMillis + 1L,
            )
        }
    }

    @Test fun durableCheckpointsClassifyPartialCreatorAndPersonalWrites() = runBlocking {
        val root = privateTestRoot()
        try {
            val journal = V344UnifiedRestoreJournal(app, root)
            assertEquals(V344UnifiedRestoreJournal.Inspection.NoJournal,
                journal.inspectAfterRestart())
            val initial = checkpoint()
            assertThrows(IllegalArgumentException::class.java) {
                runBlocking { journal.prepare(initial, ownerApproved = false) }
            }
            assertFalse(root.exists())
            var state = journal.prepare(initial, ownerApproved = true)
            assertEquals(initial, journal.requireCheckpointReadOnly())
            assertTrue(journal.hasBlockingEvidence())
            assertEquals(V344RecoveryKind.NO_LIVE_WRITES,
                (journal.inspectAfterRestart() as
                    V344UnifiedRestoreJournal.Inspection.OwnerRecoveryRequired).kind)

            state = journal.checkpoint(
                state, V344UnifiedRestoreStep.BEFORE_CREATOR,
                state.updatedAtMillis + 1,
            )
            assertEquals(V344RecoveryKind.CREATOR_MAY_BE_PARTIAL,
                (journal.inspectAfterRestart() as
                    V344UnifiedRestoreJournal.Inspection.OwnerRecoveryRequired).kind)

            state = journal.checkpoint(
                state, V344UnifiedRestoreStep.CREATOR_WRITTEN,
                state.updatedAtMillis + 1,
            )
            assertEquals(V344RecoveryKind.CREATOR_FINISHED_PERSONAL_NOT_STARTED,
                (journal.inspectAfterRestart() as
                    V344UnifiedRestoreJournal.Inspection.OwnerRecoveryRequired).kind)
            state = journal.checkpoint(
                state, V344UnifiedRestoreStep.BEFORE_PERSONAL,
                state.updatedAtMillis + 1,
            )
            assertEquals(V344RecoveryKind.PERSONAL_MAY_BE_PARTIAL,
                (journal.inspectAfterRestart() as
                    V344UnifiedRestoreJournal.Inspection.OwnerRecoveryRequired).kind)
            for (next in V344UnifiedRestoreStep.entries.drop(4)) {
                state = journal.checkpoint(state, next, state.updatedAtMillis + 1)
            }
            assertEquals(V344RecoveryKind.VERIFIED_NEEDS_OWNER_RELEASE,
                (journal.inspectAfterRestart() as
                    V344UnifiedRestoreJournal.Inspection.OwnerRecoveryRequired).kind)
            assertTrue(root.exists()) // Even completed evidence is never auto-removed.
        } finally { root.deleteRecursively() }
    }

    @Test fun staleCheckpointCannotOverwriteAnewerJournal() = runBlocking {
        val root = privateTestRoot()
        try {
            val journal = V344UnifiedRestoreJournal(app, root)
            val original = journal.prepare(checkpoint(), ownerApproved = true)
            journal.checkpoint(original, V344UnifiedRestoreStep.BEFORE_CREATOR,
                original.updatedAtMillis + 1L)
            assertThrows(IllegalStateException::class.java) {
                runBlocking {
                    journal.checkpoint(original, V344UnifiedRestoreStep.BEFORE_CREATOR,
                        original.updatedAtMillis + 2L)
                }
            }
            assertEquals(V344UnifiedRestoreStep.BEFORE_CREATOR,
                journal.requireCheckpointReadOnly().step)
        } finally { root.deleteRecursively() }
    }

    @Test fun tornOrMalformedJournalRemainsQuarantinedOnReadOnlyInspection() = runBlocking {
        val root = privateTestRoot()
        try {
            assertTrue(root.mkdir())
            val backup = File(root, "active.journal.bak")
            backup.writeBytes(V344UnifiedRestoreCheckpointCodec.encode(checkpoint()))
            val journal = V344UnifiedRestoreJournal(app, root)
            assertEquals(V344UnifiedRestoreJournal.Inspection.UnreadableRecoveryEvidence,
                journal.inspectAfterRestart())
            assertTrue(backup.isFile)
            assertThrows(Exception::class.java) {
                runBlocking { journal.requireCheckpointReadOnly() }
            }
            assertTrue(backup.isFile)
        } finally { root.deleteRecursively() }
    }

    @Test fun invalidOwnerStageDoesNotCreateARecoveryJournal() = runBlocking {
        val root = privateTestRoot()
        try {
            val journal = V344UnifiedRestoreJournal(app, root)
            assertThrows(IllegalArgumentException::class.java) {
                runBlocking { journal.prepare(
                    checkpoint(V344UnifiedRestoreStep.BEFORE_CREATOR),
                    ownerApproved = true,
                ) }
            }
            assertFalse(root.exists())
        } finally { root.deleteRecursively() }
    }
}
