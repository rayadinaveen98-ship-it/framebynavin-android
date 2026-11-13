package com.framebynavin.app.data

import android.content.Context
import android.content.ContextWrapper
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.framebynavin.app.personal.StabilizationPersonalMutationGate
import java.io.File
import java.util.UUID
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Only synthetic app-private H4/owner-choice journals and process locks.
 * This test deliberately never imports Creator data or touches user audio.
 */
@RunWith(AndroidJUnit4::class)
class V370CreatorMissingVoiceFenceAndroidTest {
    private val app get() = InstrumentationRegistry.getInstrumentation().targetContext

    private fun isolated(root: File): Context = object : ContextWrapper(app) {
        override fun getApplicationContext(): Context = this
        override fun getFilesDir(): File = root
    }

    private suspend fun beforeCreator(context: Context): V344UnifiedRestoreCheckpoint {
        val journal = V344UnifiedRestoreJournal(context)
        val initial = V344UnifiedRestoreCheckpoint(
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
        val prepared = journal.prepare(initial, ownerApproved = true)
        return journal.checkpoint(prepared,
            V344UnifiedRestoreStep.BEFORE_CREATOR,
            prepared.updatedAtMillis + 1L)
    }

    private suspend fun choose(
        context: Context,
        checkpoint: V344UnifiedRestoreCheckpoint,
        direction: V347OwnerRecoveryChoice,
    ) {
        val pair = V345UnifiedAuthenticatedPairProof(
            sourceId = checkpoint.sourceId,
            sourceSha256 = checkpoint.sourceSha256,
            originalEscrowId = checkpoint.escrowId,
            originalEscrowSha256 = checkpoint.escrowSha256,
            targetCreatorSha256 = "1".repeat(64),
            targetPersonalSha256 = "2".repeat(64),
            originalCreatorSha256 = "3".repeat(64),
            originalPersonalSha256 = "4".repeat(64),
            targetCreatorProjects = 0,
            originalCreatorProjects = 0,
            targetPersonalSummary = "synthetic-only",
            originalPersonalSummary = "synthetic-only",
        )
        V347OwnerRecoveryDecisionJournal(context).recordChoice(
            proof = V346InterruptedRecoveryProof(
                journal = checkpoint,
                sealedArchives = pair,
                recoveryKind = V344RecoveryKind.CREATOR_MAY_BE_PARTIAL,
                creatorStandaloneRecoveryMarker = false,
                personalStandaloneRecoveryMarker = false,
            ),
            choice = direction,
            ownerConfirmedChoice = true,
        )
    }

    @Test fun permitsReadOnlyPersonalButNotPersonalMutations() = runBlocking {
        val root = File(app.filesDir, "v370-media-fence-" + UUID.randomUUID())
        assertTrue(root.mkdir())
        try {
            val context = isolated(root)
            CreatorDataGate.installUnifiedRecoveryFence(context)
            val checkpoint = beforeCreator(context)
            choose(context, checkpoint, V347OwnerRecoveryChoice.REQUEST_FORWARD_TARGET)
            var personalRead = false
            var creatorEntered = false
            var personalWrite = false

            StabilizationPersonalMutationGate.withCombinedCreatorMissingVoiceFence(
                context, checkpoint, ownerConfirmed = true,
            ) {
                CreatorDataGate.withJournalBoundCreatorWrite(
                    context, checkpoint, ownerConfirmed = true,
                ) {
                    StabilizationPersonalMutationGate.withStableSnapshot(context) {
                        personalRead = true
                    }
                    creatorEntered = true
                    assertTrue(runCatching {
                        StabilizationPersonalMutationGate.withOwnerMutation(context) {
                            personalWrite = true
                        }
                    }.isFailure)
                }
            }
            assertTrue(personalRead)
            assertTrue(creatorEntered)
            assertFalse(personalWrite)
            assertEquals(checkpoint, V344UnifiedRestoreJournal(context)
                .requireCheckpointReadOnly())
        } finally {
            CreatorDataGate.installUnifiedRecoveryFence(app)
            root.deleteRecursively()
        }
    }

    @Test fun wrongRecoveryDirectionOrMissingConsentNeverEnterFence() = runBlocking {
        val root = File(app.filesDir, "v370-media-fence-" + UUID.randomUUID())
        assertTrue(root.mkdir())
        try {
            val context = isolated(root)
            val checkpoint = beforeCreator(context)
            choose(context, checkpoint, V347OwnerRecoveryChoice.REQUEST_REVERSE_ORIGINAL)
            var entered = false
            assertTrue(runCatching {
                StabilizationPersonalMutationGate.withCombinedCreatorMissingVoiceFence(
                    context, checkpoint, ownerConfirmed = true,
                ) { entered = true }
            }.isFailure)
            assertTrue(runCatching {
                StabilizationPersonalMutationGate.withCombinedCreatorMissingVoiceFence(
                    context, checkpoint, ownerConfirmed = false,
                ) { entered = true }
            }.isFailure)
            assertFalse(entered)
        } finally {
            root.deleteRecursively()
        }
    }

    @Test fun verifiedCreatorCheckpointRetainsTheOriginalOwnerForwardDirection() = runBlocking {
        val root = File(app.filesDir, "v370-creator-finish-" + UUID.randomUUID())
        assertTrue(root.mkdir())
        try {
            val context = isolated(root)
            CreatorDataGate.installUnifiedRecoveryFence(context)
            val before = beforeCreator(context)
            choose(context, before, V347OwnerRecoveryChoice.REQUEST_FORWARD_TARGET)
            val decision = (V347OwnerRecoveryDecisionJournal(context).readOnlyStatus() as
                V347OwnerRecoveryDecisionJournal.ReadState.Recorded).choice
            var creatorEntered = false
            val next = StabilizationPersonalMutationGate
                .withCombinedCreatorRecoveryFinishFence(
                    context, before, ownerConfirmed = true,
                ) {
                    CreatorDataGate.withJournalBoundCreatorWrite(
                        context, before, ownerConfirmed = true,
                    ) {
                        StabilizationPersonalMutationGate.withStableSnapshot(context) {
                            creatorEntered = true
                        }
                        // No user data is changed in this synthetic test.
                    }
                    V344UnifiedRestoreJournal(context).checkpoint(
                        before, V344UnifiedRestoreStep.CREATOR_WRITTEN,
                        before.updatedAtMillis + 1L,
                        otherRecoveryPending = {
                            !V347OwnerRecoveryDecisionJournal(context)
                                .matchesUnchangedForwardChoice(before, decision)
                        },
                    )
                }
            assertTrue(creatorEntered)
            assertEquals(V344UnifiedRestoreStep.CREATOR_WRITTEN, next.step)
            assertEquals(
                V347OwnerRecoveryDecisionJournal.ReadState
                    .CreatorWrittenRetained(decision, next),
                V347OwnerRecoveryDecisionJournal(context).readOnlyStatus(),
            )
            assertEquals(next, V344UnifiedRestoreJournal(context)
                .requireCheckpointReadOnly())
            assertTrue(V347OwnerRecoveryDecisionJournal.hasPendingFor(context))
            assertTrue(runCatching {
                CreatorDataGate.readyTransaction(context) {
                    error("Ordinary Creator editing must remain frozen")
                }
            }.isFailure)
        } finally {
            CreatorDataGate.installUnifiedRecoveryFence(app)
            root.deleteRecursively()
        }
    }

    @Test fun recoveryFinishNeverAcceptsReverseChoiceOrUnadvancedJournal() = runBlocking {
        val root = File(app.filesDir, "v370-creator-finish-" + UUID.randomUUID())
        assertTrue(root.mkdir())
        try {
            val context = isolated(root)
            val before = beforeCreator(context)
            choose(context, before, V347OwnerRecoveryChoice.REQUEST_REVERSE_ORIGINAL)
            var executed = false
            assertTrue(runCatching {
                StabilizationPersonalMutationGate.withCombinedCreatorRecoveryFinishFence(
                    context, before, ownerConfirmed = true,
                ) { executed = true }
            }.isFailure)
            assertFalse(executed)
            assertEquals(before, V344UnifiedRestoreJournal(context)
                .requireCheckpointReadOnly())
        } finally {
            root.deleteRecursively()
        }
    }

}
