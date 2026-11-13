package com.framebynavin.app.data

import android.content.Context
import android.content.ContextWrapper
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.framebynavin.app.personal.StabilizationPersonalMutationGate
import java.io.File
import java.util.UUID
import java.util.concurrent.atomic.AtomicBoolean
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Synthetic H3 Personal permit tests. Fake filesDir contains an H4 checkpoint
 * only. No current Personal records, Space photos, Creator backup, Room tables,
 * reminder deliveries or real owner archive bytes are read or modified.
 */
@RunWith(AndroidJUnit4::class)
class V358UnifiedPersonalWriteScopeAndroidTest {
    private val app get() = InstrumentationRegistry.getInstrumentation().targetContext

    private fun fakeContext(root: File): Context = object : ContextWrapper(app) {
        override fun getApplicationContext(): Context = this
        override fun getFilesDir(): File = root
    }

    private fun fresh() = V344UnifiedRestoreCheckpoint(
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

    private suspend fun preparedCreatorEnd(
        journal: V344UnifiedRestoreJournal,
    ): V344UnifiedRestoreCheckpoint {
        val initial = journal.prepare(fresh(), ownerApproved = true)
        val before = journal.checkpoint(
            initial, V344UnifiedRestoreStep.BEFORE_CREATOR,
            initial.updatedAtMillis + 1L,
        )
        return journal.checkpoint(
            before, V344UnifiedRestoreStep.CREATOR_WRITTEN,
            before.updatedAtMillis + 1L,
        )
    }

    @Test fun beforePersonalCheckpointIsRequiredBeforeAnyNestedPersonalWrite() = runBlocking {
        val privateRoot = File(app.filesDir, "v358-personal-scope-" + UUID.randomUUID())
        assertTrue(privateRoot.mkdir())
        val testApp = fakeContext(privateRoot)
        try {
            val journal = V344UnifiedRestoreJournal(testApp)
            val creatorDone = preparedCreatorEnd(journal)
            val ordinaryRan = AtomicBoolean(false)
            assertThrows(IllegalStateException::class.java) {
                runBlocking {
                    StabilizationPersonalMutationGate.withOwnerMutation(testApp) {
                        ordinaryRan.set(true)
                    }
                }
            }
            assertFalse(ordinaryRan.get())

            val nestedWriteRan = AtomicBoolean(false)
            val snapshotRan = AtomicBoolean(false)
            val finished = StabilizationPersonalMutationGate.withCombinedPersonalForwardFence(
                testApp, creatorDone, ownerConfirmed = true,
            ) {
                StabilizationPersonalMutationGate.withStableSnapshot(testApp) {
                    snapshotRan.set(true)
                }
                var deniedNestedWrite = false
                try {
                    StabilizationPersonalMutationGate.withOwnerMutation(testApp) {
                        StabilizationPersonalMutationGate.withOwnerMutation(testApp) {
                            nestedWriteRan.set(true)
                        }
                    }
                } catch (_: IllegalStateException) {
                    deniedNestedWrite = true
                }
                assertTrue(deniedNestedWrite)
                assertFalse(nestedWriteRan.get())
                val before = journal.checkpoint(
                    creatorDone, V344UnifiedRestoreStep.BEFORE_PERSONAL,
                    creatorDone.updatedAtMillis + 1L,
                )
                // Only IN-MEMORY synthetic callback; no Personal v2 journal
                // or actual data writes are installed by this test.
                StabilizationPersonalMutationGate.withOwnerMutation(testApp) {
                    snapshotRan.set(true)
                }
                journal.checkpoint(
                    before, V344UnifiedRestoreStep.PERSONAL_WRITTEN,
                    before.updatedAtMillis + 1L,
                )
                "checkpoint-only"
            }
            assertEquals("checkpoint-only", finished)
            assertTrue(snapshotRan.get())
            assertFalse(nestedWriteRan.get())
            assertEquals(V344UnifiedRestoreStep.PERSONAL_WRITTEN,
                journal.requireCheckpointReadOnly().step)
        } finally {
            privateRoot.deleteRecursively()
        }
    }

    @Test fun wrongCheckpointAndMissingOwnerConsentRefusePersonalPermit() = runBlocking {
        val privateRoot = File(app.filesDir, "v358-personal-scope-" + UUID.randomUUID())
        assertTrue(privateRoot.mkdir())
        val testApp = fakeContext(privateRoot)
        try {
            val journal = V344UnifiedRestoreJournal(testApp)
            val creatorDone = preparedCreatorEnd(journal)
            val entered = AtomicBoolean(false)
            assertThrows(IllegalArgumentException::class.java) {
                runBlocking {
                    StabilizationPersonalMutationGate.withCombinedPersonalForwardFence(
                        testApp, creatorDone, ownerConfirmed = false,
                    ) { entered.set(true) }
                }
            }
            val advanced = journal.checkpoint(
                creatorDone, V344UnifiedRestoreStep.BEFORE_PERSONAL,
                creatorDone.updatedAtMillis + 1L,
            )
            assertThrows(IllegalStateException::class.java) {
                runBlocking {
                    StabilizationPersonalMutationGate.withCombinedPersonalForwardFence(
                        testApp, creatorDone, ownerConfirmed = true,
                    ) { entered.set(true) }
                }
            }
            assertFalse(entered.get())
            assertEquals(advanced, journal.requireCheckpointReadOnly())
        } finally {
            privateRoot.deleteRecursively()
        }
    }
}
