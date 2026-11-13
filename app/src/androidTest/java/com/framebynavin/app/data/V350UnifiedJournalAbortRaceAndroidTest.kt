package com.framebynavin.app.data

import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import java.io.File
import java.util.UUID
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith

/**
 * H3 synthetic checkpoint-only tests. This test NEVER calls a live Creator,
 * Personal, Reminder, Room, photo, media or alarm write. Every file lives in
 * isolated v350-test-... folders immediately beneath the app-private root.
 */
@RunWith(AndroidJUnit4::class)
class V350UnifiedJournalAbortRaceAndroidTest {
    private val app get() = InstrumentationRegistry.getInstrumentation().targetContext

    private fun newCheckpoint() = V344UnifiedRestoreCheckpoint(
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

    private fun roots() : Triple<File, File, File> {
        val id = UUID.randomUUID()
        val journal = File(app.filesDir, "v350-test-journal-$id")
        val history = File(app.filesDir, "v350-test-history-$id")
        assertTrue(history.mkdir())
        return Triple(journal, history, File(history, "placeholder"))
    }

    @Test fun ownerApprovedArchiveRetainsExactPreparedCheckpointWithoutCopyingLiveData() = runBlocking {
        val (root, history) = roots()
        try {
            val api = V344UnifiedRestoreJournal(app, root)
            val original = api.prepare(newCheckpoint(), ownerApproved = true)
            val destination = File(history, "${original.transactionId}-aborted")
            assertTrue(api.archiveUnwrittenCheckpoint(
                original, destination, ownerConfirmed = true,
                otherRecoveryPending = { false },
            ))
            assertFalse(root.exists())
            assertTrue(destination.isDirectory)
            val archived = File(destination, "active.journal")
            assertTrue(archived.isFile)
            assertEquals(original,
                V344UnifiedRestoreCheckpointCodec.decode(archived.readBytes()))
            assertEquals(V344UnifiedRestoreJournal.Inspection.NoJournal,
                api.inspectAfterRestart())
        } finally { root.deleteRecursively(); history.deleteRecursively() }
    }

    @Test fun anyDurableBeforeCreatorMarkerOutranksStaleAbort() = runBlocking {
        val (root, history) = roots()
        try {
            val api = V344UnifiedRestoreJournal(app, root)
            val start = api.prepare(newCheckpoint(), ownerApproved = true)
            val next = api.checkpoint(
                start, V344UnifiedRestoreStep.BEFORE_CREATOR,
                start.updatedAtMillis + 1L,
            )
            val destination = File(history, "${start.transactionId}-aborted")
            assertThrows(Exception::class.java) {
                runBlocking {
                    api.archiveUnwrittenCheckpoint(
                        start, destination, ownerConfirmed = true,
                        otherRecoveryPending = { false },
                    )
                }
            }
            assertEquals(next, api.requireCheckpointReadOnly())
            assertFalse(destination.exists())
            assertTrue(root.isDirectory)
        } finally { root.deleteRecursively(); history.deleteRecursively() }
    }

    @Test fun cancellationAndCheckpointShareOneMutexEvenWhenRequestIsRacing() = runBlocking {
        val (root, history) = roots()
        val ownerEnteredCheck = CountDownLatch(1)
        val releaseOwnerCheck = CountDownLatch(1)
        try {
            val api = V344UnifiedRestoreJournal(app, root)
            val before = api.prepare(newCheckpoint(), ownerApproved = true)
            val destination = File(history, "${before.transactionId}-aborted")
            val archiveJob = async(Dispatchers.IO) {
                api.archiveUnwrittenCheckpoint(
                    expected = before,
                    destination = destination,
                    ownerConfirmed = true,
                    otherRecoveryPending = {
                        ownerEnteredCheck.countDown()
                        check(releaseOwnerCheck.await(8, TimeUnit.SECONDS))
                        false
                    },
                )
            }
            assertTrue("Archive must enter its shared checkpoint mutex",
                ownerEnteredCheck.await(8, TimeUnit.SECONDS))
            val writerAttempt = async(Dispatchers.IO) {
                runCatching {
                    api.checkpoint(before, V344UnifiedRestoreStep.BEFORE_CREATOR,
                        before.updatedAtMillis + 1L)
                }.isFailure
            }
            releaseOwnerCheck.countDown()
            withTimeout(12_000L) {
                assertTrue(archiveJob.await())
                assertTrue("Checkpoint must refuse to advance after archived PREPARED",
                    writerAttempt.await())
            }
            assertFalse(root.exists())
            assertEquals(before,
                V344UnifiedRestoreCheckpointCodec.decode(
                    File(destination, "active.journal").readBytes()
                ))
        } finally {
            releaseOwnerCheck.countDown()
            root.deleteRecursively(); history.deleteRecursively()
        }
    }

    @Test fun pendingOwnerChoiceOrRecoveryBlocksAtomicArchive() = runBlocking {
        val (root, history) = roots()
        try {
            val api = V344UnifiedRestoreJournal(app, root)
            val before = api.prepare(newCheckpoint(), ownerApproved = true)
            assertThrows(IllegalStateException::class.java) {
                runBlocking {
                    api.archiveUnwrittenCheckpoint(
                        before, File(history, "${before.transactionId}-aborted"),
                        ownerConfirmed = true, otherRecoveryPending = { true },
                    )
                }
            }
            assertEquals(before, api.requireCheckpointReadOnly())
            assertTrue(root.isDirectory)
        } finally { root.deleteRecursively(); history.deleteRecursively() }
    }

    @Test fun noOwnerConsentCannotMoveAnyRecoveryCheckpoint() = runBlocking {
        val (root, history) = roots()
        try {
            val api = V344UnifiedRestoreJournal(app, root)
            val before = api.prepare(newCheckpoint(), ownerApproved = true)
            assertThrows(IllegalArgumentException::class.java) {
                runBlocking {
                    api.archiveUnwrittenCheckpoint(
                        before, File(history, "${before.transactionId}-aborted"),
                        ownerConfirmed = false, otherRecoveryPending = { false },
                    )
                }
            }
            assertEquals(before, api.requireCheckpointReadOnly())
        } finally { root.deleteRecursively(); history.deleteRecursively() }
    }
}
