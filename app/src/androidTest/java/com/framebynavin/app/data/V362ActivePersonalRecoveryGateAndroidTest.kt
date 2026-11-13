package com.framebynavin.app.data

import android.content.Context
import android.content.ContextWrapper
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.framebynavin.app.personal.Phase1V2RecoveryRoots
import com.framebynavin.app.personal.Phase1V2RestoreCheckpoint
import com.framebynavin.app.personal.Phase1V2RestoreStep
import com.framebynavin.app.personal.Phase1V2RestoreWriteAheadJournal
import com.framebynavin.app.personal.StabilizationPersonalMutationGate
import java.io.File
import java.util.UUID
import java.util.concurrent.atomic.AtomicBoolean
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Synthetic checkpoint/mutex test only. Does NOT decrypt or import any
 * Personal/Creator data, alter reminders, or write Space JPEG files.
 */
@RunWith(AndroidJUnit4::class)
class V362ActivePersonalRecoveryGateAndroidTest {
    private val app get() =
        InstrumentationRegistry.getInstrumentation().targetContext

    private fun fake(root: File): Context = object : ContextWrapper(app) {
        override fun getApplicationContext(): Context = this
        override fun getFilesDir(): File = root
    }

    private suspend fun prepare(context: Context):
        Pair<V344UnifiedRestoreCheckpoint, Phase1V2RestoreCheckpoint> {
        val h4 = V344UnifiedRestoreJournal(context)
        val stamp = 1_900_000_000_000L
        var combined = h4.prepare(V344UnifiedRestoreCheckpoint(
            transactionId = UUID.randomUUID().toString(),
            sourceId = UUID.randomUUID().toString(),
            sourceSha256 = "a".repeat(64),
            escrowId = UUID.randomUUID().toString(),
            escrowSha256 = "b".repeat(64),
            creatorOriginalSha256 = "c".repeat(64),
            personalOriginalSha256 = "d".repeat(64),
            step = V344UnifiedRestoreStep.PREPARED_NO_WRITES,
            createdAtMillis = stamp, updatedAtMillis = stamp,
        ), ownerApproved = true)
        for (next in listOf(
            V344UnifiedRestoreStep.BEFORE_CREATOR,
            V344UnifiedRestoreStep.CREATOR_WRITTEN,
            V344UnifiedRestoreStep.BEFORE_PERSONAL,
        )) {
            combined = h4.checkpoint(combined, next, combined.updatedAtMillis + 1L)
        }
        val initial = Phase1V2RestoreCheckpoint(
            transactionId = UUID.randomUUID().toString(),
            preparedId = UUID.randomUUID().toString(),
            escrowId = UUID.randomUUID().toString(),
            originalDigest = "d".repeat(64),
            targetDigest = "e".repeat(64),
            step = Phase1V2RestoreStep.PREPARED,
            createdAtMillis = stamp + 5L, updatedAtMillis = stamp + 5L,
        )
        V359UnifiedPersonalV2HandoffJournal(context).recordBeforeV2Prepare(
            combined, initial,
        )
        val v2 = Phase1V2RestoreWriteAheadJournal(
            Phase1V2RecoveryRoots.alongsideLegacy(
                StabilizationPersonalMutationGate.journalRootFor(context),
            ),
        )
        var state = v2.prepare(initial, ownerRequested = true)
        state = v2.checkpoint(
            state.step, Phase1V2RestoreStep.BEFORE_PERSONAL,
            state.updatedAtMillis + 1L,
        )
        return combined to state
    }

    @Test fun combinedGateUsesExistingNativeV2JournalAndBlocksUnscopedEdits() =
        runBlocking {
            val root = File(app.filesDir, "v362-synthetic-" + UUID.randomUUID())
            assertTrue(root.mkdir())
            val context = fake(root)
            try {
                val (combined, native) = prepare(context)
                val attemptedMutation = AtomicBoolean(false)
                val sawSnapshot = AtomicBoolean(false)
                val result = StabilizationPersonalMutationGate
                    .withCombinedActiveV2Resume(
                        context, combined, ownerConfirmedResume = true,
                    ) { checkpoint ->
                        assertEquals(native, checkpoint)
                        StabilizationPersonalMutationGate.withStableSnapshot(context) {
                            sawSnapshot.set(true)
                        }
                        var denied = false
                        try {
                            StabilizationPersonalMutationGate.withOwnerMutation(context) {
                                attemptedMutation.set(true)
                            }
                        } catch (_: IllegalStateException) {
                            denied = true
                        }
                        assertTrue("No native write permit; normal edits must be refused", denied)
                        assertFalse(attemptedMutation.get())
                        // Synthetic journal advancement only: no owner data
                        // has actually been restored in this test.
                        V344UnifiedRestoreJournal(context).checkpoint(
                            combined, V344UnifiedRestoreStep.PERSONAL_WRITTEN,
                            combined.updatedAtMillis + 1L,
                        )
                        "synthetic-finish"
                    }
                assertEquals("synthetic-finish", result)
                assertTrue(sawSnapshot.get())
                assertFalse(attemptedMutation.get())
            } finally {
                root.deleteRecursively()
            }
        }

    @Test fun missingConsentAndWrongH4StageNeverOpenActiveRecovery() = runBlocking {
        val root = File(app.filesDir, "v362-synthetic-" + UUID.randomUUID())
        assertTrue(root.mkdir())
        val context = fake(root)
        try {
            val (combined, _) = prepare(context)
            val invoked = AtomicBoolean(false)
            var refused = false
            try {
                StabilizationPersonalMutationGate.withCombinedActiveV2Resume(
                    context, combined, ownerConfirmedResume = false,
                ) { invoked.set(true) }
            } catch (_: IllegalArgumentException) {
                refused = true
            }
            assertTrue(refused)
            assertFalse(invoked.get())
            refused = false
            try {
                StabilizationPersonalMutationGate.withCombinedActiveV2Resume(
                    context, combined.copy(
                        step = V344UnifiedRestoreStep.PERSONAL_WRITTEN,
                    ), ownerConfirmedResume = true,
                ) { invoked.set(true) }
            } catch (_: IllegalArgumentException) {
                refused = true
            }
            assertTrue(refused)
            assertFalse(invoked.get())
        } finally { root.deleteRecursively() }
    }
}
