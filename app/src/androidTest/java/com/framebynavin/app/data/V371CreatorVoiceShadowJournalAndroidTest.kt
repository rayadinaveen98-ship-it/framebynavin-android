package com.framebynavin.app.data

import android.content.Context
import android.content.ContextWrapper
import com.framebynavin.app.personal.StabilizationPersonalMutationGate
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
 * Synthetic bytes in a unique isolated filesDir. No Creator records or owner
 * media are restored, overwritten, deleted or scheduled by this source test.
 */
@RunWith(AndroidJUnit4::class)
class V371CreatorVoiceShadowJournalAndroidTest {
    private val device get() = InstrumentationRegistry.getInstrumentation().targetContext

    private fun isolated(root: File): Context = object : ContextWrapper(device) {
        override fun getApplicationContext(): Context = this
        override fun getFilesDir(): File = root
    }

    private suspend fun checkpoint(context: Context): V344UnifiedRestoreCheckpoint {
        val journal = V344UnifiedRestoreJournal(context)
        val now = 1_900_000_000_000L
        val created = journal.prepare(
            V344UnifiedRestoreCheckpoint(
                transactionId = UUID.randomUUID().toString(),
                sourceId = UUID.randomUUID().toString(),
                sourceSha256 = "a".repeat(64),
                escrowId = UUID.randomUUID().toString(),
                escrowSha256 = "b".repeat(64),
                creatorOriginalSha256 = "c".repeat(64),
                personalOriginalSha256 = "d".repeat(64),
                step = V344UnifiedRestoreStep.PREPARED_NO_WRITES,
                createdAtMillis = now,
                updatedAtMillis = now,
            ),
            ownerApproved = true,
        )
        return journal.checkpoint(
            created, V344UnifiedRestoreStep.BEFORE_CREATOR,
            now + 1L,
        )
    }

    private suspend fun choose(
        context: Context,
        cp: V344UnifiedRestoreCheckpoint,
        direction: V347OwnerRecoveryChoice,
    ) {
        val proof = V346InterruptedRecoveryProof(
            journal = cp,
            sealedArchives = V345UnifiedAuthenticatedPairProof(
                sourceId = cp.sourceId, sourceSha256 = cp.sourceSha256,
                originalEscrowId = cp.escrowId,
                originalEscrowSha256 = cp.escrowSha256,
                targetCreatorSha256 = "1".repeat(64),
                targetPersonalSha256 = "2".repeat(64),
                originalCreatorSha256 = "3".repeat(64),
                originalPersonalSha256 = "4".repeat(64),
                targetCreatorProjects = 0, originalCreatorProjects = 0,
                targetPersonalSummary = "synthetic", originalPersonalSummary = "synthetic",
            ),
            recoveryKind = V344RecoveryKind.CREATOR_MAY_BE_PARTIAL,
            creatorStandaloneRecoveryMarker = false,
            personalStandaloneRecoveryMarker = false,
        )
        V347OwnerRecoveryDecisionJournal(context).recordChoice(
            proof, direction, ownerConfirmedChoice = true,
        )
    }

    @Test fun shadowReservationDurablyPreservesDamagedSourceAndDoesNotCopyAudio() =
        runBlocking {
            val root = File(device.filesDir, "v371-shadow-test-" + UUID.randomUUID())
            assertTrue(root.mkdir())
            try {
                val context = isolated(root)
                CreatorDataGate.installUnifiedRecoveryFence(context)
                val cp = checkpoint(context)
                val complete = File(root, "synthetic-target.m4a")
                complete.writeText("complete-original-archive-target-synthetic")
                val allocations = V365UnifiedCreatorVoiceInstallJournal(context)
                val base = CreatorDataGate.withJournalBoundCreatorWrite(
                    context, cp, ownerConfirmed = true,
                ) {
                    allocations.reserveForCurrentCreatorWriter(
                        mapOf("idea-1" to complete),
                    )
                }
                val voiceDir = File(root, "voice_ideas")
                assertTrue(voiceDir.mkdir())
                val damaged = File(voiceDir, base.entries.single().fileName)
                damaged.writeText("partial")
                choose(context, cp, V347OwnerRecoveryChoice.REQUEST_FORWARD_TARGET)

                val shadow = V371UnifiedCreatorVoiceShadowJournal(context)
                val plan = CreatorDataGate.withJournalBoundCreatorWrite(
                    context, cp, ownerConfirmed = true,
                ) {
                    shadow.reserveForCurrentCreatorWriter(
                        base, mapOf("idea-1" to complete),
                    )
                }
                assertTrue(shadow.hasBlockingEvidence())
                assertEquals(base.entries.single().fileName, plan.entries.single().damagedFile)
                assertTrue(damaged.readText() == "partial")
                val alternate = File(voiceDir, plan.entries.single().shadowFile)
                assertFalse(alternate.exists())
                assertEquals(1, shadow.auditShadowReadOnly(plan).missing)
                assertFalse(plan.mayOverwriteDamagedMedia)
                assertFalse(plan.mayImportCreatorRecords)

                alternate.writeBytes(complete.readBytes()) // Test emulates a future verified writer.
                assertTrue(shadow.auditShadowReadOnly(plan).allVerified)
                assertEquals("partial", damaged.readText())

                assertTrue(runCatching {
                    CreatorDataGate.withJournalBoundCreatorWrite(
                        context, cp, ownerConfirmed = true,
                    ) {
                        shadow.reserveForCurrentCreatorWriter(
                            base, mapOf("idea-1" to complete),
                        )
                    }
                }.isFailure)
                assertEquals(plan, shadow.inspectReadOnly())
                damaged.writeText("unexpected-owner-file-change")
                assertTrue(runCatching { shadow.auditShadowReadOnly(plan) }.isFailure)
                // Reset synthetic test data, never a real owner recording.
                damaged.writeText("partial")
                assertTrue(shadow.auditShadowReadOnly(plan).allVerified)
                // A left-over AtomicFile sibling is FORENSIC evidence, not a clean journal.
                File(root, "backlot-unified-creator-voice-shadow-v1/shadow.json.new")
                    .writeText("synthetic torn write")
                assertTrue(runCatching { shadow.inspectReadOnly() }.isFailure)
                assertEquals("partial", damaged.readText())
            } finally {
                CreatorDataGate.installUnifiedRecoveryFence(device)
                root.deleteRecursively()
            }
        }

    @Test fun reverseOwnerChoiceCannotReserveNewTargetShadowPaths() = runBlocking {
        val root = File(device.filesDir, "v371-shadow-test-" + UUID.randomUUID())
        assertTrue(root.mkdir())
        try {
            val context = isolated(root)
            CreatorDataGate.installUnifiedRecoveryFence(context)
            val cp = checkpoint(context)
            val complete = File(root, "synthetic.m4a")
            complete.writeText("synthetic original")
            val allocations = V365UnifiedCreatorVoiceInstallJournal(context)
            val base = CreatorDataGate.withJournalBoundCreatorWrite(
                context, cp, ownerConfirmed = true,
            ) { allocations.reserveForCurrentCreatorWriter(mapOf("idea-1" to complete)) }
            assertTrue(File(root, "voice_ideas").mkdir())
            File(root, "voice_ideas/" + base.entries.single().fileName).writeText("partial")
            choose(context, cp, V347OwnerRecoveryChoice.REQUEST_REVERSE_ORIGINAL)
            val shadow = V371UnifiedCreatorVoiceShadowJournal(context)
            assertTrue(runCatching {
                CreatorDataGate.withJournalBoundCreatorWrite(
                    context, cp, ownerConfirmed = true,
                ) { shadow.reserveForCurrentCreatorWriter(base, mapOf("idea-1" to complete)) }
            }.isFailure)
            assertFalse(shadow.hasBlockingEvidence())
        } finally {
            CreatorDataGate.installUnifiedRecoveryFence(device)
            root.deleteRecursively()
        }
    }

    @Test fun shadowWriterCopiesOnlyNewPathsAndVerifiedRetryIsIdempotent() = runBlocking {
        val root = File(device.filesDir, "v371-shadow-write-" + UUID.randomUUID())
        assertTrue(root.mkdir())
        try {
            val context = isolated(root)
            CreatorDataGate.installUnifiedRecoveryFence(context)
            val cp = checkpoint(context)
            val source = File(root, "synthetic-authenticated-target.m4a")
            source.writeText("synthetic-verified-audio-bytes")
            val original = V365UnifiedCreatorVoiceInstallJournal(context)
            val base = CreatorDataGate.withJournalBoundCreatorWrite(
                context, cp, ownerConfirmed = true,
            ) { original.reserveForCurrentCreatorWriter(mapOf("idea-a" to source)) }
            val directory = File(root, "voice_ideas")
            assertTrue(directory.mkdir())
            val partial = File(directory, base.entries.single().fileName)
            partial.writeText("interrupted")
            choose(context, cp, V347OwnerRecoveryChoice.REQUEST_FORWARD_TARGET)
            val shadow = V371UnifiedCreatorVoiceShadowJournal(context)
            val plan = CreatorDataGate.withJournalBoundCreatorWrite(
                context, cp, ownerConfirmed = true,
            ) { shadow.reserveForCurrentCreatorWriter(
                base, mapOf("idea-a" to source),
            ) }
            val replacement = File(directory, plan.entries.single().shadowFile)
            assertFalse(replacement.exists())
            val first = StabilizationPersonalMutationGate
                .withCombinedCreatorMissingVoiceFence(context, cp, ownerConfirmed = true) {
                    CreatorDataGate.withJournalBoundCreatorWrite(
                        context, cp, ownerConfirmed = true,
                    ) {
                        shadow.installMissingShadowsForCurrentOwnerWriter(
                            plan, mapOf("idea-a" to source),
                        )
                    }
                }
            assertTrue(first.allVerified)
            assertEquals(source.readText(), replacement.readText())
            assertEquals("interrupted", partial.readText())
            assertEquals(plan, shadow.inspectReadOnly())
            val second = StabilizationPersonalMutationGate
                .withCombinedCreatorMissingVoiceFence(context, cp, ownerConfirmed = true) {
                    CreatorDataGate.withJournalBoundCreatorWrite(
                        context, cp, ownerConfirmed = true,
                    ) {
                        shadow.installMissingShadowsForCurrentOwnerWriter(
                            plan, mapOf("idea-a" to source),
                        )
                    }
                }
            assertEquals(first, second)
            assertEquals(source.readText(), replacement.readText())

            replacement.writeText("partial-after-crash")
            assertTrue(runCatching {
                StabilizationPersonalMutationGate
                    .withCombinedCreatorMissingVoiceFence(context, cp, ownerConfirmed = true) {
                        CreatorDataGate.withJournalBoundCreatorWrite(
                            context, cp, ownerConfirmed = true,
                        ) {
                            shadow.installMissingShadowsForCurrentOwnerWriter(
                                plan, mapOf("idea-a" to source),
                            )
                        }
                    }
            }.isFailure)
            assertEquals("partial-after-crash", replacement.readText())
            assertEquals("interrupted", partial.readText())
            assertEquals(cp, V344UnifiedRestoreJournal(context).requireCheckpointReadOnly())
        } finally {
            CreatorDataGate.installUnifiedRecoveryFence(device)
            root.deleteRecursively()
        }
    }

    @Test fun shadowMediaWriterRejectsMissingPersonalFence() = runBlocking {
        val root = File(device.filesDir, "v371-shadow-locked-" + UUID.randomUUID())
        assertTrue(root.mkdir())
        try {
            val context = isolated(root)
            CreatorDataGate.installUnifiedRecoveryFence(context)
            val cp = checkpoint(context)
            val source = File(root, "synthetic-target.m4a")
            source.writeText("full-synthetic-owner-voice")
            val original = V365UnifiedCreatorVoiceInstallJournal(context)
            val base = CreatorDataGate.withJournalBoundCreatorWrite(
                context, cp, ownerConfirmed = true,
            ) { original.reserveForCurrentCreatorWriter(mapOf("idea-a" to source)) }
            assertTrue(File(root, "voice_ideas").mkdir())
            val damaged = File(root, "voice_ideas/" + base.entries.single().fileName)
            damaged.writeText("partial")
            choose(context, cp, V347OwnerRecoveryChoice.REQUEST_FORWARD_TARGET)
            val shadow = V371UnifiedCreatorVoiceShadowJournal(context)
            val plan = CreatorDataGate.withJournalBoundCreatorWrite(
                context, cp, ownerConfirmed = true,
            ) { shadow.reserveForCurrentCreatorWriter(
                base, mapOf("idea-a" to source),
            ) }
            val output = File(root, "voice_ideas/" + plan.entries.single().shadowFile)
            assertTrue(runCatching {
                CreatorDataGate.withJournalBoundCreatorWrite(
                    context, cp, ownerConfirmed = true,
                ) {
                    shadow.installMissingShadowsForCurrentOwnerWriter(
                        plan, mapOf("idea-a" to source),
                    )
                }
            }.isFailure)
            assertFalse(output.exists())
            assertEquals("partial", damaged.readText())
        } finally {
            CreatorDataGate.installUnifiedRecoveryFence(device)
            root.deleteRecursively()
        }
    }

}
