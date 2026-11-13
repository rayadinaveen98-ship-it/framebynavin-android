package com.framebynavin.app.data

import android.content.Context
import android.content.ContextWrapper
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import java.io.File
import java.util.UUID
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith

/** Synthetic owner-independent app-private witness data only. */
@RunWith(AndroidJUnit4::class)
class V376CreatorOriginalInverseJournalAndroidTest {
    private val phone get() = InstrumentationRegistry.getInstrumentation().targetContext
    private fun isolated(dir: File): Context = object : ContextWrapper(phone) {
        override fun getApplicationContext(): Context = this
        override fun getFilesDir(): File = dir
    }
    private fun record() = V376CreatorInverseWitness(
        transactionId = UUID.randomUUID().toString(),
        checkpointSha256 = "a".repeat(64),
        reverseDecisionSha256 = "b".repeat(64),
        originalEscrowSha256 = "c".repeat(64),
        priorCreatorFingerprint = "d".repeat(64),
        originalCreatorFingerprint = "e".repeat(64),
    )

    @Test fun coldRestartRetainsExactWitnessWithoutAnyWritePermit() {
        val dir = File(phone.filesDir, "synthetic-creator-inverse-" + UUID.randomUUID())
        assertTrue(dir.mkdir())
        try {
            val context = isolated(dir)
            val journal = V376CreatorOriginalInverseJournal(context)
            val witness = record()
            assertFalse(journal.hasEvidence())
            assertEquals(witness, journal.prepare(witness))
            assertEquals(witness,
                V376CreatorOriginalInverseJournal(context).inspect())
            assertTrue(journal.hasEvidence())
            assertTrue(V376CreatorInversePolicy.matchesPendingBeforeState(
                witness, witness.priorCreatorFingerprint,
            ))
            assertFalse(V376CreatorInversePolicy.matchesPendingBeforeState(
                witness, witness.originalCreatorFingerprint,
            ))
            assertFalse(witness.grantsNativeRestoreAuthority)
            assertFalse(witness.mayDeleteNativeJournal)
            assertFalse(witness.mayReleaseCombinedRecovery)
            assertTrue(runCatching { journal.prepare(witness) }.isFailure)
        } finally { dir.deleteRecursively() }
    }

    @Test fun crashSiblingOrEditedWitnessFailsClosedWithoutDeletingFiles() {
        val dir = File(phone.filesDir, "synthetic-creator-inverse-" + UUID.randomUUID())
        assertTrue(dir.mkdir())
        try {
            val context = isolated(dir)
            val journal = V376CreatorOriginalInverseJournal(context)
            journal.prepare(record())
            val root = File(dir, "backlot-unified-creator-original-inverse-v1")
            val active = File(root, "original-witness.json")
            val sibling = File(root, "original-witness.json.bak")
            sibling.writeText("synthetic torn record")
            assertTrue(runCatching { journal.inspect() }.isFailure)
            assertTrue(active.isFile && sibling.isFile)
            assertTrue(journal.hasEvidence())
            assertTrue(runCatching { journal.prepare(record()) }.isFailure)
        } finally { dir.deleteRecursively() }
    }

    @Test fun unrelatedCreatorNativeJournalCannotBeReplayed() {
        val dir = File(phone.filesDir, "synthetic-creator-inverse-" + UUID.randomUUID())
        assertTrue(dir.mkdir())
        try {
            val context = isolated(dir)
            val journal = V376CreatorOriginalInverseJournal(context)
            val saved = journal.prepare(record())
            val nativeRoot = File(dir, "creator_restore_recovery_v181")
            assertTrue(nativeRoot.mkdir())
            File(nativeRoot, "pending.json").writeText(
                """{"file":"pre-restore-foreign.fbnbackup","sha256":"0"}""",
            )
            assertTrue(runCatching {
                journal.requireBoundNativePending(
                    saved, V343UnifiedBaselineReader(context),
                )
            }.isFailure)
            assertTrue(File(nativeRoot, "pending.json").isFile)
        } finally { dir.deleteRecursively() }
    }
}
