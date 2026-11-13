package com.framebynavin.app.data

import android.content.Context
import android.content.ContextWrapper
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import java.io.File
import java.nio.file.Files
import java.util.UUID
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith

/** Synthetic private folders only. No real reminder, photo or owner ZIP. */
@RunWith(AndroidJUnit4::class)
class V379UnifiedReleaseJournalAndroidTest {
    private val phone get() = InstrumentationRegistry.getInstrumentation().targetContext
    private fun fake(dir: File): Context = object : ContextWrapper(phone) {
        override fun getFilesDir(): File = dir
        override fun getApplicationContext(): Context = this
    }
    private fun record() = V379UnifiedReleaseRecord(
        transactionId = UUID.randomUUID().toString(),
        checkpointSha256 = "a".repeat(64),
        verificationSha256 = "b".repeat(64),
        outcome = V377VerifiedOwnerOutcome.ORIGINAL_RESTORED,
        step = V379ReleaseStep.PREPARED,
    )

    @Test fun durableOwnerReleaseStagesAreAdjacentAndNeverDeleteAnyMedia() {
        val root = File(phone.filesDir, "synthetic-v379-" + UUID.randomUUID())
        assertTrue(root.mkdir())
        try {
            val stages = V379UnifiedReleaseJournal(fake(root))
            val first = record()
            assertEquals(first, stages.prepare(first, ownerApproved = true))
            assertEquals(first, V379UnifiedReleaseJournal(fake(root)).inspect())
            val sub = stages.advance(first, V379ReleaseStep.JOURNALS_RETAINED)
            assertEquals(sub, stages.inspect())
            assertTrue(runCatching {
                stages.advance(first, V379ReleaseStep.READY_TO_ARCHIVE_H4)
            }.isFailure)
            assertTrue(runCatching {
                stages.prepare(record(), ownerApproved = true)
            }.isFailure)
            assertFalse(sub.mayDeleteOwnerMedia)
            assertFalse(sub.mayDeleteOriginalArchive)
            assertFalse(sub.mayUnfreezeBeforeH4Archived)
        } finally { root.deleteRecursively() }
    }

    @Test fun atomicHistoryMoveRetainsBytesAndIsRestartIdempotent() {
        val root = File(phone.filesDir, "synthetic-v379-" + UUID.randomUUID())
        assertTrue(root.mkdir())
        try {
            val app = fake(root)
            val transactionId = UUID.randomUUID().toString()
            val old = File(root, "backlot-test-evidence-v1")
            assertTrue(old.mkdir())
            val marker = File(old, "owner-proof.json")
            marker.writeText("synthetic owner original retained")
            val moved = requireNotNull(V379AtomicOwnerHistory.retain(
                app, transactionId,
                "backlot-test-evidence-v1", required = true,
            ))
            assertFalse(old.exists())
            assertEquals("synthetic owner original retained",
                File(moved, "owner-proof.json").readText())
            assertEquals(moved, V379AtomicOwnerHistory.retain(
                app, transactionId, "backlot-test-evidence-v1",
                required = true,
            ))
            assertTrue(runCatching {
                old.mkdir()
                V379AtomicOwnerHistory.retain(
                    app, transactionId, "backlot-test-evidence-v1",
                    required = true,
                )
            }.isFailure)
        } finally { root.deleteRecursively() }
    }

    @Test fun unknownSiblingOrTruncatedReleaseJournalStaysBlocking() {
        val root = File(phone.filesDir, "synthetic-v379-" + UUID.randomUUID())
        assertTrue(root.mkdir())
        try {
            val stages = V379UnifiedReleaseJournal(fake(root))
            stages.prepare(record(), ownerApproved = true)
            val dir = File(root, "backlot-unified-final-release-v1")
            val torn = File(dir, "release.json.new")
            torn.writeText("crash")
            assertTrue(stages.hasEvidence())
            assertTrue(runCatching { stages.inspect() }.isFailure)
            assertTrue(File(dir, "release.json").isFile)
        } finally { root.deleteRecursively() }
    }
}
