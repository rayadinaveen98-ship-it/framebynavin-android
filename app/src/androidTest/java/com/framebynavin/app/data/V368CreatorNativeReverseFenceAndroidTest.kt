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

/** Tests isolated fake app-private H4/V347 journals only. Never restores or reads owner data. */
@RunWith(AndroidJUnit4::class)
class V368CreatorNativeReverseFenceAndroidTest {
    private val device get() = InstrumentationRegistry.getInstrumentation().targetContext
    private fun isolated(root: File): Context = object : ContextWrapper(device) {
        override fun getApplicationContext(): Context = this
        override fun getFilesDir(): File = root
    }

    private suspend fun beforeCreator(context: Context): V344UnifiedRestoreCheckpoint {
        val journal = V344UnifiedRestoreJournal(context)
        val base = V344UnifiedRestoreCheckpoint(
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
        val prepared = journal.prepare(base, ownerApproved = true)
        return journal.checkpoint(
            prepared, V344UnifiedRestoreStep.BEFORE_CREATOR,
            prepared.updatedAtMillis + 1L,
        )
    }

    private suspend fun choose(
        context: Context,
        checkpoint: V344UnifiedRestoreCheckpoint,
        choice: V347OwnerRecoveryChoice,
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
            targetPersonalSummary = "fake",
            originalPersonalSummary = "fake",
        )
        V347OwnerRecoveryDecisionJournal(context).recordChoice(
            proof = V346InterruptedRecoveryProof(
                journal = checkpoint,
                sealedArchives = pair,
                recoveryKind = V344RecoveryKind.CREATOR_MAY_BE_PARTIAL,
                creatorStandaloneRecoveryMarker = true,
                personalStandaloneRecoveryMarker = false,
            ),
            choice = choice,
            ownerConfirmedChoice = true,
        )
    }

    @Test fun nativeReverseRequiresValidDurableOwnerDirectionAndExactCheckpoint() = runBlocking {
        val root = File(device.filesDir, "v368-reverse-test-" + UUID.randomUUID())
        assertTrue(root.mkdir())
        try {
            val context = isolated(root)
            val checkpoint = beforeCreator(context)
            var entered = false
            assertTrue(runCatching {
                StabilizationPersonalMutationGate.withCombinedCreatorNativeReverseFence(
                    context, checkpoint, ownerConfirmed = true,
                ) { entered = true }
            }.isFailure)
            assertFalse(entered)

            choose(context, checkpoint, V347OwnerRecoveryChoice.REQUEST_REVERSE_ORIGINAL)
            val result = StabilizationPersonalMutationGate.withCombinedCreatorNativeReverseFence(
                context, checkpoint, ownerConfirmed = true,
            ) {
                entered = true
                "synthetic-lock-only"
            }
            assertTrue(entered)
            assertEquals("synthetic-lock-only", result)
            assertTrue(runCatching {
                StabilizationPersonalMutationGate.withCombinedCreatorNativeReverseFence(
                    context, checkpoint.copy(updatedAtMillis = checkpoint.updatedAtMillis + 1),
                    ownerConfirmed = true,
                ) { error("Should not enter a stale journal") }
            }.isFailure)
            assertEquals(checkpoint,
                V344UnifiedRestoreJournal(context).requireCheckpointReadOnly())
        } finally {
            root.deleteRecursively()
        }
    }

    @Test fun forwardChoiceAndMissingConsentNeverEnterCreatorReverse() = runBlocking {
        val root = File(device.filesDir, "v368-reverse-test-" + UUID.randomUUID())
        assertTrue(root.mkdir())
        try {
            val context = isolated(root)
            val checkpoint = beforeCreator(context)
            choose(context, checkpoint, V347OwnerRecoveryChoice.REQUEST_FORWARD_TARGET)
            var entered = false
            assertTrue(runCatching {
                StabilizationPersonalMutationGate.withCombinedCreatorNativeReverseFence(
                    context, checkpoint, ownerConfirmed = true,
                ) { entered = true }
            }.isFailure)
            assertTrue(runCatching {
                StabilizationPersonalMutationGate.withCombinedCreatorNativeReverseFence(
                    context, checkpoint, ownerConfirmed = false,
                ) { entered = true }
            }.isFailure)
            assertFalse(entered)
        } finally {
            root.deleteRecursively()
        }
    }
}
