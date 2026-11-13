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
 * Only isolated fake-app filesDir, private synthetic bytes, and H4 journal
 * markers are used. This does not restore Creator projects, read a real Voice
 * Idea, modify original Best Frames or schedule an owner reminder.
 */
@RunWith(AndroidJUnit4::class)
class V365CreatorVoiceAllocationJournalAndroidTest {
    private val app get() = InstrumentationRegistry.getInstrumentation().targetContext
    private fun isolated(root: File): Context = object : ContextWrapper(app) {
        override fun getApplicationContext(): Context = this
        override fun getFilesDir(): File = root
    }

    private suspend fun beforeCreator(context: Context): V344UnifiedRestoreCheckpoint {
        val timestamp = 1_900_000_000_000L
        val journal = V344UnifiedRestoreJournal(context)
        val prepared = journal.prepare(
            V344UnifiedRestoreCheckpoint(
                transactionId = UUID.randomUUID().toString(),
                sourceId = UUID.randomUUID().toString(),
                sourceSha256 = "a".repeat(64),
                escrowId = UUID.randomUUID().toString(),
                escrowSha256 = "b".repeat(64),
                creatorOriginalSha256 = "c".repeat(64),
                personalOriginalSha256 = "d".repeat(64),
                step = V344UnifiedRestoreStep.PREPARED_NO_WRITES,
                createdAtMillis = timestamp, updatedAtMillis = timestamp,
            ),
            ownerApproved = true,
        )
        return journal.checkpoint(
            prepared, V344UnifiedRestoreStep.BEFORE_CREATOR,
            prepared.updatedAtMillis + 1L,
        )
    }

    @Test fun planIsDurableBeforeCopiesAndCrashInventoryCannotDeleteAnyMedia() =
        runBlocking {
            val root = File(app.filesDir, "v365-voice-test-" + UUID.randomUUID())
            assertTrue(root.mkdir())
            val context = isolated(root)
            try {
                CreatorDataGate.installUnifiedRecoveryFence(context)
                val checkpoint = beforeCreator(context)
                val staged = File(root, "synthetic-audio.m4a")
                staged.writeText("synthetic-voice-idea-audio-only")
                val reader = V365UnifiedCreatorVoiceInstallJournal(context)
                val plan = CreatorDataGate.withJournalBoundCreatorWrite(
                    context, checkpoint, ownerConfirmed = true,
                ) {
                    reader.reserveForCurrentCreatorWriter(
                        mapOf("synthetic-idea-1" to staged),
                    )
                }
                assertEquals(1, plan.entries.size)
                assertFalse(plan.mayDeleteAnyMedia)
                assertFalse(plan.mayReplayCreator)
                assertTrue(reader.hasBlockingEvidence())
                assertEquals(plan, reader.inspectReadOnly())

                val missing = reader.auditReadOnly(plan)
                assertEquals(1, missing.missingCopies)
                assertFalse(missing.mayCleanOrReplay)

                val destination = File(File(root, "voice_ideas"), plan.entries.single().fileName)
                assertTrue(destination.parentFile.mkdirs())
                destination.writeBytes(staged.readBytes())
                val matching = reader.auditReadOnly(plan)
                assertEquals(1, matching.matchingCopies)
                assertEquals(0, matching.alteredOrPartialCopies)
                assertFalse(matching.mayCleanOrReplay)

                // H4 stays BEFORE_CREATOR with no native Creator journal.
                // Even a perfectly copied file is not a Creator restore.
                val status = V364UnifiedCreatorPendingRecoveryProof(context)
                    .inspectReadOnly() as
                    V364UnifiedCreatorPendingRecoveryProof.Assessment
                        .VoiceAllocatedBeforeNativeCreatorJournal
                assertEquals(1, status.voice.matchingCopies)
                assertFalse(status.mayReplayOrDelete)

                destination.writeText("changed-after-crash")
                val altered = reader.auditReadOnly(plan)
                assertEquals(1, altered.alteredOrPartialCopies)
                assertTrue(destination.isFile)
                assertFalse(altered.mayCleanOrReplay)
            } finally {
                CreatorDataGate.installUnifiedRecoveryFence(app)
                root.deleteRecursively()
            }
        }

    @Test fun secondReservationCannotOverwriteFirstOwnersVoiceAllocation() =
        runBlocking {
            val root = File(app.filesDir, "v365-voice-test-" + UUID.randomUUID())
            assertTrue(root.mkdir())
            val context = isolated(root)
            try {
                CreatorDataGate.installUnifiedRecoveryFence(context)
                val checkpoint = beforeCreator(context)
                val staged = File(root, "synthetic.m4a")
                staged.writeText("synthetic-audio")
                val reader = V365UnifiedCreatorVoiceInstallJournal(context)
                CreatorDataGate.withJournalBoundCreatorWrite(
                    context, checkpoint, ownerConfirmed = true,
                ) {
                    reader.reserveForCurrentCreatorWriter(
                        mapOf("synthetic-idea-1" to staged),
                    )
                }
                val original = reader.inspectReadOnly()
                var denied = false
                try {
                    CreatorDataGate.withJournalBoundCreatorWrite(
                        context, checkpoint, ownerConfirmed = true,
                    ) {
                        reader.reserveForCurrentCreatorWriter(
                            mapOf("synthetic-idea-1" to staged),
                        )
                    }
                } catch (_: IllegalStateException) { denied = true }
                assertTrue(denied)
                assertEquals(original, reader.inspectReadOnly())
            } finally {
                CreatorDataGate.installUnifiedRecoveryFence(app)
                root.deleteRecursively()
            }
        }
}
