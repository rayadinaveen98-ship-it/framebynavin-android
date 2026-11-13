package com.framebynavin.app.data

import android.content.Context
import android.content.ContextWrapper
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.framebynavin.app.personal.Phase1V2RecoveryRoots
import com.framebynavin.app.personal.Phase1V2RestoreCheckpoint
import com.framebynavin.app.personal.Phase1V2RestoreJournalCodec
import com.framebynavin.app.personal.Phase1V2RestoreStep
import com.framebynavin.app.personal.Phase1V2RestoreWriteAheadJournal
import com.framebynavin.app.personal.StabilizationPersonalMutationGate
import java.io.File
import java.nio.file.Files
import java.nio.file.StandardCopyOption
import java.util.UUID
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith

/**
 * All synthetic native v2 ZIP contents are inert, non-decryptable placeholders.
 * The inspector verifies JOURNAL/handoff consistency only; it never claims
 * proof of data or media integrity. No real Creator, Personal, Room or alarms.
 */
@RunWith(AndroidJUnit4::class)
class V360UnifiedPersonalV2HandoffAndroidTest {
    private val app get() = InstrumentationRegistry.getInstrumentation().targetContext
    private fun fake(root: File): Context = object : ContextWrapper(app) {
        override fun getApplicationContext(): Context = this
        override fun getFilesDir(): File = root
    }

    private fun combinedStart() = V344UnifiedRestoreCheckpoint(
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

    private fun native() = Phase1V2RestoreCheckpoint(
        transactionId = UUID.randomUUID().toString(),
        preparedId = UUID.randomUUID().toString(),
        escrowId = UUID.randomUUID().toString(),
        originalDigest = "e".repeat(64),
        targetDigest = "f".repeat(64),
        step = Phase1V2RestoreStep.PREPARED,
        createdAtMillis = 1_900_000_000_100L,
        updatedAtMillis = 1_900_000_000_100L,
    )

    private suspend fun combinedBeforePersonal(
        context: Context,
    ): V344UnifiedRestoreCheckpoint {
        val j = V344UnifiedRestoreJournal(context)
        val prepared = j.prepare(combinedStart(), ownerApproved = true)
        val creatorBefore = j.checkpoint(
            prepared, V344UnifiedRestoreStep.BEFORE_CREATOR,
            prepared.updatedAtMillis + 1L,
        )
        val creatorDone = j.checkpoint(
            creatorBefore, V344UnifiedRestoreStep.CREATOR_WRITTEN,
            creatorBefore.updatedAtMillis + 1L,
        )
        return j.checkpoint(
            creatorDone, V344UnifiedRestoreStep.BEFORE_PERSONAL,
            creatorDone.updatedAtMillis + 1L,
        )
    }

    private fun inertEncryptedEvidence(context: Context, id: String) {
        val parent = File(context.filesDir,
            "backlot-personal-v2-encrypted-transactions")
        val specific = File(parent, id)
        assertTrue(specific.mkdirs())
        File(specific, "incoming-encrypted.zip").writeText("synthetic-source-only")
        File(specific, "original-escrow-encrypted.zip").writeText("synthetic-escrow-only")
    }

    private suspend fun setup(): Prepared {
        val root = File(app.filesDir, "v360-handoff-" + UUID.randomUUID())
        assertTrue(root.mkdir())
        val context = fake(root)
        val h4 = combinedBeforePersonal(context)
        val native = native()
        V359UnifiedPersonalV2HandoffJournal(context).recordBeforeV2Prepare(h4, native)
        inertEncryptedEvidence(context, native.transactionId)
        val v2path = Phase1V2RecoveryRoots.alongsideLegacy(
            StabilizationPersonalMutationGate.journalRootFor(context))
        return Prepared(root, context, h4, native, v2path)
    }

    private data class Prepared(
        val root: File,
        val context: Context,
        val h4: V344UnifiedRestoreCheckpoint,
        val native: Phase1V2RestoreCheckpoint,
        val v2Root: File,
    )

    @Test fun handoffWithoutNativeJournalDoesNotInventCompletedImport() = runBlocking {
        val p = setup()
        try {
            val state = V360UnifiedPersonalV2RecoveryInspector(p.context).inspectReadOnly()
            assertEquals(
                V360UnifiedPersonalV2RecoveryInspector.Assessment
                    .HandoffBeforeNativePrepare(p.native.transactionId),
                state,
            )
            assertFalse(p.v2Root.exists())
        } finally { p.root.deleteRecursively() }
    }

    @Test fun activeNativeCheckpointIsNotMistakenForArchivedCompletion() = runBlocking {
        val p = setup()
        try {
            val nativeJournal = Phase1V2RestoreWriteAheadJournal(p.v2Root)
            val prepared = nativeJournal.prepare(p.native, ownerRequested = true)
            val interrupted = nativeJournal.checkpoint(
                Phase1V2RestoreStep.PREPARED,
                Phase1V2RestoreStep.BEFORE_PERSONAL,
                prepared.updatedAtMillis + 1L,
            )
            val status = V360UnifiedPersonalV2RecoveryInspector(p.context)
                .inspectReadOnly()
            assertEquals(
                V360UnifiedPersonalV2RecoveryInspector.Assessment.NativeV2Active(
                    interrupted.step, interrupted.transactionId),
                status,
            )
            assertTrue(p.v2Root.isDirectory)
        } finally { p.root.deleteRecursively() }
    }

    @Test fun fullyArchivedNativeV2JournalIsRecognizedButNotReplayed() = runBlocking {
        val p = setup()
        try {
            val j = Phase1V2RestoreWriteAheadJournal(p.v2Root)
            var state = j.prepare(p.native, ownerRequested = true)
            Phase1V2RestoreStep.entries.drop(1).forEach { next ->
                state = j.checkpoint(state.step, next, state.updatedAtMillis + 1L)
            }
            assertEquals(Phase1V2RestoreStep.VERIFYING, state.step)
            val completed = File(p.context.filesDir,
                "backlot-personal-v2-completed-journals")
            assertTrue(completed.mkdir())
            val final = File(completed, state.transactionId)
            Files.move(p.v2Root.toPath(), final.toPath(), StandardCopyOption.ATOMIC_MOVE)
            val status = V360UnifiedPersonalV2RecoveryInspector(p.context)
                .inspectReadOnly()
            assertEquals(
                V360UnifiedPersonalV2RecoveryInspector.Assessment
                    .NativeV2CompletedAndArchived(
                        state.transactionId, state.originalDigest, state.targetDigest,
                    ), status,
            )
            assertFalse(p.v2Root.exists())
            assertTrue(final.isDirectory)
            // It is metadata-only: no Personal records were restored by the test.
        } finally { p.root.deleteRecursively() }
    }

    @Test fun adjacentValidNativeCheckpointEvidenceIsClassifiedButNeverReplayed() =
        runBlocking {
            val p = setup()
            try {
                val journal = Phase1V2RestoreWriteAheadJournal(p.v2Root)
                val durable = journal.prepare(p.native, ownerRequested = true)
                val pending = Phase1V2RestoreJournalCodec.advance(
                    durable, Phase1V2RestoreStep.BEFORE_PERSONAL,
                    durable.updatedAtMillis + 1L,
                )
                File(p.v2Root, "active.pending").writeBytes(
                    Phase1V2RestoreJournalCodec.encode(pending),
                )
                val status = V360UnifiedPersonalV2RecoveryInspector(p.context)
                    .inspectReadOnly()
                assertEquals(
                    V360UnifiedPersonalV2RecoveryInspector.Assessment.NativeV2TornAdjacent(
                        durable.step, pending.step, durable.transactionId,
                    ),
                    status,
                )
                assertTrue(File(p.v2Root, "active.pending").isFile)
                assertTrue(File(p.v2Root, "active.journal").isFile)
                // V362 deliberately accepts only NativeV2Active and will
                // never open a writer from this torn state.
                assertFalse(status is
                    V360UnifiedPersonalV2RecoveryInspector.Assessment.NativeV2Active)
            } finally { p.root.deleteRecursively() }
        }

    @Test fun adjacentPendingCheckpointWithSwappedIdentityIsStillUntrusted() =
        runBlocking {
            val p = setup()
            try {
                val journal = Phase1V2RestoreWriteAheadJournal(p.v2Root)
                val durable = journal.prepare(p.native, ownerRequested = true)
                val forged = Phase1V2RestoreJournalCodec.advance(
                    durable, Phase1V2RestoreStep.BEFORE_PERSONAL,
                    durable.updatedAtMillis + 1L,
                ).copy(transactionId = UUID.randomUUID().toString())
                File(p.v2Root, "active.pending").writeBytes(
                    Phase1V2RestoreJournalCodec.encode(forged),
                )
                assertEquals(
                    V360UnifiedPersonalV2RecoveryInspector.Assessment.UnreadableOrContradictory,
                    V360UnifiedPersonalV2RecoveryInspector(p.context).inspectReadOnly(),
                )
                assertTrue(File(p.v2Root, "active.pending").isFile)
                assertTrue(File(p.v2Root, "active.journal").isFile)
            } finally { p.root.deleteRecursively() }
        }

    @Test fun tornNativeCheckpointNeverBecomesSafeArchivedHistory() = runBlocking {
        val p = setup()
        try {
            Phase1V2RestoreWriteAheadJournal(p.v2Root)
                .prepare(p.native, ownerRequested = true)
            File(p.v2Root, "active.pending").writeText("crash")
            assertEquals(
                V360UnifiedPersonalV2RecoveryInspector.Assessment.UnreadableOrContradictory,
                V360UnifiedPersonalV2RecoveryInspector(p.context).inspectReadOnly(),
            )
            assertTrue(File(p.v2Root, "active.pending").exists())
        } finally { p.root.deleteRecursively() }
    }

    @Test fun nativeJournalDigestMismatchFailsClosedWithoutDeletingEitherRecord() = runBlocking {
        val p = setup()
        try {
            Phase1V2RestoreWriteAheadJournal(p.v2Root).prepare(
                p.native.copy(targetDigest = "0".repeat(64)),
                ownerRequested = true,
            )
            assertEquals(
                V360UnifiedPersonalV2RecoveryInspector.Assessment.UnreadableOrContradictory,
                V360UnifiedPersonalV2RecoveryInspector(p.context).inspectReadOnly(),
            )
            assertTrue(p.v2Root.isDirectory)
            assertTrue(File(p.context.filesDir,
                "backlot-unified-personal-v2-handoff-v1/handoff.json").isFile)
        } finally { p.root.deleteRecursively() }
    }

    @Test fun mismatchedCombinedStageCannotConsumeEarlierPersonalHandoff() = runBlocking {
        val p = setup()
        try {
            V344UnifiedRestoreJournal(p.context).checkpoint(
                p.h4,
                V344UnifiedRestoreStep.PERSONAL_WRITTEN,
                p.h4.updatedAtMillis + 1L,
            )
            assertEquals(
                V360UnifiedPersonalV2RecoveryInspector.Assessment.WrongCombinedStage,
                V360UnifiedPersonalV2RecoveryInspector(p.context).inspectReadOnly(),
            )
        } finally { p.root.deleteRecursively() }
    }
}
