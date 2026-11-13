package com.framebynavin.app.data

import android.content.Context
import android.content.ContextWrapper
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import java.io.File
import java.util.UUID
import java.util.concurrent.atomic.AtomicBoolean
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertFalse
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Synthetic gate-only exercise. Fake ContextWrapper filesDir holds ONLY
 * test checkpoints, not app data; writer actions set in-memory booleans.
 * Does NOT invoke a real Creator/Personal backup/restore or any alarm.
 */
@RunWith(AndroidJUnit4::class)
class V357CreatorWriteScopeAndroidTest {
    private val app get() = InstrumentationRegistry.getInstrumentation().targetContext

    private fun fakeContext(root: File): Context = object : ContextWrapper(app) {
        override fun getApplicationContext(): Context = this
        override fun getFilesDir(): File = root
    }

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

    @Test fun onlyExactBeforeCreatorStageCanOpenScopedSyntheticWriter() = runBlocking {
        val testRoot = File(app.filesDir, "v357-gate-" + UUID.randomUUID())
        assertTrue(testRoot.mkdir())
        val context = fakeContext(testRoot)
        try {
            CreatorDataGate.installUnifiedRecoveryFence(context)
            val journal = V344UnifiedRestoreJournal(context)
            val start = journal.prepare(checkpoint(), ownerApproved = true)
            val before = journal.checkpoint(
                start, V344UnifiedRestoreStep.BEFORE_CREATOR,
                start.updatedAtMillis + 1L,
            )

            val ordinary = AtomicBoolean(false)
            assertThrows(IllegalStateException::class.java) {
                runBlocking {
                    CreatorDataGate.transaction { ordinary.set(true) }
                }
            }
            assertFalse(ordinary.get())

            val authorized = AtomicBoolean(false)
            CreatorDataGate.withJournalBoundCreatorWrite(
                context, before, ownerConfirmed = true,
            ) {
                // Existing nested Creator transactions remain usable only
                // inside the exact H4-scoped Job while its mutex is held.
                CreatorDataGate.transaction {
                    authorized.set(true)
                    CreatorDataGate.nonCancellable {
                        CreatorDataGate.transaction {
                            assertTrue(authorized.get())
                        }
                    }
                }
            }
            assertTrue(authorized.get())

            // The short-lived scoped marker cannot unfreeze unrelated work.
            assertThrows(IllegalStateException::class.java) {
                runBlocking {
                    CreatorDataGate.transaction { ordinary.set(true) }
                }
            }
            assertFalse(ordinary.get())
        } finally {
            CreatorDataGate.installUnifiedRecoveryFence(app)
            testRoot.deleteRecursively()
        }
    }

    @Test fun staleJournalOrMissingConsentDoesNotGrantRecoveryWriter() = runBlocking {
        val testRoot = File(app.filesDir, "v357-gate-" + UUID.randomUUID())
        assertTrue(testRoot.mkdir())
        val context = fakeContext(testRoot)
        try {
            CreatorDataGate.installUnifiedRecoveryFence(context)
            val journal = V344UnifiedRestoreJournal(context)
            val original = journal.prepare(checkpoint(), ownerApproved = true)
            val before = journal.checkpoint(
                original, V344UnifiedRestoreStep.BEFORE_CREATOR,
                original.updatedAtMillis + 1L,
            )
            val allowed = AtomicBoolean(false)
            assertThrows(IllegalArgumentException::class.java) {
                runBlocking {
                    CreatorDataGate.withJournalBoundCreatorWrite(
                        context, before, ownerConfirmed = false,
                    ) { allowed.set(true) }
                }
            }
            journal.checkpoint(
                before, V344UnifiedRestoreStep.CREATOR_WRITTEN,
                before.updatedAtMillis + 1L,
            )
            assertThrows(IllegalStateException::class.java) {
                runBlocking {
                    CreatorDataGate.withJournalBoundCreatorWrite(
                        context, before, ownerConfirmed = true,
                    ) { allowed.set(true) }
                }
            }
            assertFalse(allowed.get())
        } finally {
            CreatorDataGate.installUnifiedRecoveryFence(app)
            testRoot.deleteRecursively()
        }
    }
}
