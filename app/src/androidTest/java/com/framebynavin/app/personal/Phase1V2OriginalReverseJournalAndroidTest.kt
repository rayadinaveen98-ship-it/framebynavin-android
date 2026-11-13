package com.framebynavin.app.personal

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

/**
 * Synthetic app-private isolated reverse journals ONLY.
 * Never reads, deletes or alters a real owner reminder, Room, image or H4.
 */
@RunWith(AndroidJUnit4::class)
class Phase1V2OriginalReverseJournalAndroidTest {
    private val phone get() = InstrumentationRegistry.getInstrumentation().targetContext
    private fun isolated(root: File): Context = object : ContextWrapper(phone) {
        override fun getFilesDir(): File = root
        override fun getApplicationContext(): Context = this
    }
    private fun record(): Phase1V2ReverseRecord = Phase1V2ReverseRecord(
        combinedId = UUID.randomUUID().toString(),
        combinedDigest = "a".repeat(64),
        ownerDecisionDigest = "b".repeat(64),
        nativeV2Id = UUID.randomUUID().toString(),
        nativeStep = Phase1V2RestoreStep.BEFORE_PHOTOS,
        originalDigest = "c".repeat(64),
        targetDigest = "d".repeat(64),
        step = Phase1V2ReverseStep.PREPARED,
    )

    @Test fun durableInverseMarkerSurvivesReopenAndAdvancesAdjacentStagesOnly() {
        val root = File(phone.filesDir, "synthetic-v2-inverse-" + UUID.randomUUID())
        assertTrue(root.mkdir())
        try {
            val context = isolated(root)
            val journal = Phase1V2OriginalReverseJournal(context)
            val initial = record()
            assertFalse(journal.hasEvidence())
            assertEquals(initial, journal.prepare(true, initial))
            assertEquals(initial, Phase1V2OriginalReverseJournal(context).inspect())
            assertTrue(journal.hasEvidence())
            assertTrue(runCatching {
                journal.prepare(true, initial)
            }.isFailure)
            assertTrue(runCatching {
                journal.advance(initial, Phase1V2ReverseStep.SPACES_REVERTED)
            }.isFailure)
            val before = journal.advance(
                initial, Phase1V2ReverseStep.BEFORE_PHOTO_ROWS,
            )
            val done = journal.advance(
                before, Phase1V2ReverseStep.PHOTO_ROWS_REVERTED,
            )
            assertEquals(done, Phase1V2OriginalReverseJournal(context).inspect())
            assertTrue(runCatching {
                journal.advance(before, Phase1V2ReverseStep.BEFORE_SPACES)
            }.isFailure)
            assertFalse(done.mayDeleteOriginalMedia)
            assertFalse(done.mayReleaseEscrow)
            assertFalse(done.mayRearmAlarms)
        } finally {
            root.deleteRecursively()
        }
    }

    @Test fun tornOrUnexpectedJournalRemainsBlockingEvidence() {
        val root = File(phone.filesDir, "synthetic-v2-inverse-" + UUID.randomUUID())
        assertTrue(root.mkdir())
        try {
            val context = isolated(root)
            val journal = Phase1V2OriginalReverseJournal(context)
            journal.prepare(true, record())
            val folder = File(root, "backlot-unified-personal-v2-reverse-actions-v1")
            File(folder, "reverse.json.new").writeText("torn synthetic checkpoint")
            assertTrue(journal.hasEvidence())
            assertTrue(runCatching { journal.inspect() }.isFailure)
            assertTrue(runCatching {
                journal.prepare(true, record())
            }.isFailure)
            assertTrue(File(folder, "reverse.json").isFile)
        } finally {
            root.deleteRecursively()
        }
    }
}
