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
 * Synthetic Creator native journal evidence under an isolated filesDir.
 * No real Creator task, Best Frame, Voice Idea, Personal record or alarm is
 * restored, deleted, migrated or scheduled by these read-only tests.
 */
@RunWith(AndroidJUnit4::class)
class V364UnifiedCreatorNativeProofAndroidTest {
    private val app get() =
        InstrumentationRegistry.getInstrumentation().targetContext

    private fun fake(root: File): Context = object : ContextWrapper(app) {
        override fun getApplicationContext(): Context = this
        override fun getFilesDir(): File = root
    }

    private suspend fun prepare(context: Context):
        V344UnifiedRestoreCheckpoint {
        val stamp = 1_900_000_000_000L
        val j = V344UnifiedRestoreJournal(context)
        val initial = j.prepare(V344UnifiedRestoreCheckpoint(
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
        return j.checkpoint(
            initial, V344UnifiedRestoreStep.BEFORE_CREATOR,
            stamp + 1L,
        )
    }

    @Test fun noNativeMarkerNeverProvesVoiceMediaWasUnchanged() = runBlocking {
        val root = File(app.filesDir, "v364-creator-proof-" + UUID.randomUUID())
        assertTrue(root.mkdir())
        try {
            val context = fake(root)
            prepare(context)
            assertEquals(
                V364UnifiedCreatorPendingRecoveryProof.Assessment
                    .NoNativeCreatorJournalMediaMayExist,
                V364UnifiedCreatorPendingRecoveryProof(context).inspectReadOnly(),
            )
        } finally { root.deleteRecursively() }
    }

    @Test fun tornNativeMarkerRefusesToAssumeCreatorCanBeRolledBack() = runBlocking {
        val root = File(app.filesDir, "v364-creator-proof-" + UUID.randomUUID())
        assertTrue(root.mkdir())
        try {
            val context = fake(root)
            prepare(context)
            val native = File(root, "creator_restore_recovery_v181")
            assertTrue(native.mkdir())
            File(native, "pending.json.bak").writeText("torn")
            assertEquals(
                V364UnifiedCreatorPendingRecoveryProof.Assessment
                    .UnreadableOrUnboundNativeEvidence,
                V364UnifiedCreatorPendingRecoveryProof(context).inspectReadOnly(),
            )
            assertTrue(File(native, "pending.json.bak").isFile)
        } finally { root.deleteRecursively() }
    }

    @Test fun corruptedNativeSnapshotLeavesOwnerCopiesIntact() = runBlocking {
        val root = File(app.filesDir, "v364-creator-proof-" + UUID.randomUUID())
        assertTrue(root.mkdir())
        try {
            val context = fake(root)
            prepare(context)
            val native = File(root, "creator_restore_recovery_v181")
            assertTrue(native.mkdir())
            val stored = File(native, "pre-restore-test.fbnbackup")
            stored.writeText("modified-native-owner-data")
            val marker = File(native, "pending.json")
            marker.writeText(
                """{"file":"pre-restore-test.fbnbackup","sha256":"${"a".repeat(64)}","originalFrames":"[]","originalFramesSha256":"${"b".repeat(64)}"}"""
            )
            assertEquals(
                V364UnifiedCreatorPendingRecoveryProof.Assessment
                    .UnreadableOrUnboundNativeEvidence,
                V364UnifiedCreatorPendingRecoveryProof(context).inspectReadOnly(),
            )
            assertTrue(marker.isFile)
            assertEquals("modified-native-owner-data", stored.readText())
        } finally { root.deleteRecursively() }
    }

    @Test fun wrongCombinedStageDoesNotInspectStandaloneCreatorRecovery() = runBlocking {
        val root = File(app.filesDir, "v364-creator-proof-" + UUID.randomUUID())
        assertTrue(root.mkdir())
        try {
            val context = fake(root)
            val initial = prepare(context)
            V344UnifiedRestoreJournal(context).checkpoint(
                initial, V344UnifiedRestoreStep.CREATOR_WRITTEN,
                initial.updatedAtMillis + 1L,
            )
            assertEquals(
                V364UnifiedCreatorPendingRecoveryProof.Assessment.WrongCombinedStage,
                V364UnifiedCreatorPendingRecoveryProof(context).inspectReadOnly(),
            )
        } finally { root.deleteRecursively() }
    }

    @Test fun evenBoundNativeOriginalCannotGrantCreatorDataOrMediaWrite() {
        val receipt = V364UnifiedCreatorPendingRecoveryProof.Assessment.BoundOriginal(
            combinedTransactionId = UUID.randomUUID().toString(),
            nativeOriginalFileName = "pre-restore-synthetic.fbnbackup",
            nativeOriginalSha256 = "a".repeat(64),
            originalCreatorFingerprint = "b".repeat(64),
            verifiedOriginalBestFrameCount = 2,
        )
        assertFalse(receipt.preexistingVoiceOrphansCanBeRuledOut)
        assertFalse(receipt.mayReplayCreator)
        assertFalse(receipt.mayRollbackCreator)
        assertFalse(receipt.mayDeleteOwnedMedia)
        assertFalse(receipt.mayReleaseRecovery)
    }
}
