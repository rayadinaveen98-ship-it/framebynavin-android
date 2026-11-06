package com.framebynavin.app.personal

import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.framebynavin.app.data.HardeningTestEnvironment
import com.framebynavin.app.personal.spaces.V289SpaceState
import com.framebynavin.app.personal.spaces.V289SpacesMetadataCodec
import java.io.File
import java.time.LocalDate
import java.time.LocalTime
import java.util.UUID
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Deliberate crash window on a disposable, cleared Android 15 emulator.
 * This test only stages synthetic Personal data in the emulator's DataStore.
 * No production restore action, permission prompt, cloud, or owner phone.
 */
@RunWith(AndroidJUnit4::class)
class StabilizationRestoreCrossStoreInterruptDeviceTest {
    private class SyntheticProcessDeath : RuntimeException()

    @Test fun realPersonalWriteInterruptedBeforeChallengesRetainsOwnerAndJournal() = runBlocking {
        HardeningTestEnvironment.requireCiEmulator()
        val app = InstrumentationRegistry.getInstrumentation().targetContext.applicationContext
        val personal = PersonalReminderStore(app)
        val challenges = V297ChallengeStore(app)
        val groups = V298ReminderGroupStore(app)
        assertTrue("Requires cleared disposable emulator", personal.load().isEmpty())
        assertTrue(challenges.load().isEmpty())
        assertTrue(groups.load().isEmpty())

        val existing = PersonalReminder(
            id = "synthetic-original", title = "Retain existing original",
            startDate = LocalDate.of(2026, 12, 10), time = LocalTime.of(9, 0),
            createdAtMillis = 100L, updatedAtMillis = 100L,
        )
        val imported = PersonalReminder(
            id = "synthetic-imported", title = "Import additional record",
            startDate = LocalDate.of(2026, 12, 11), time = LocalTime.of(10, 0),
            createdAtMillis = 120L, updatedAtMillis = 120L,
        )
        personal.create(existing)
        val originalSnapshot = V301LocalRecoveryReader(app).read()
        assertTrue(originalSnapshot.localIntegrityVerified)
        val importedGroup = V298ReminderGroup(
            id = "group-imported", name = "Synthetic linked group",
            reminderIds = listOf(imported.id),
            createdAtMillis = 100L, updatedAtMillis = 100L,
        )
        val importedChallenge = V297Challenge(
            id = "challenge-imported", title = "Synthetic 21-day challenge",
            startDate = LocalDate.of(2026, 12, 1), createdAtMillis = 100L,
        )
        val incoming = V300PersonalComponents(
            personal = PersonalCompletionBackupCodec.encode(listOf(imported), emptyList()),
            challenges = V297ChallengeCodec.encode(listOf(importedChallenge)),
            groups = V298ReminderGroupCodec.encode(listOf(importedGroup)),
            spaces = V289SpacesMetadataCodec.encode(V289SpaceState()),
        )
        val target = StabilizationRestoreTargetPolicy.calculate(
            originalSnapshot, incoming, emptyList(),
        )
        assertFalse(target.authorizedToWriteLive)
        assertEquals(3, target.preview.adds)

        val root = File(app.cacheDir, "interrupted-cross-store-fixture-" + UUID.randomUUID())
        val barrier = V305CommitBarrierReceipt(
            preparedId = "e751c74e-8d47-4f69-bd46-c95c45d04c01",
            rollbackEscrowId = "613c863a-b019-4085-9df3-5f38b84454ea",
            localBaselineSha256 = target.startingDigest,
            incomingArchiveSha256 = "c".repeat(64),
            rollbackArchiveSha256 = "d".repeat(64),
        )
        try {
            val journal = StabilizationRestoreWriteAheadJournal(root)
            journal.prepare(barrier, target.targetDigest, true, 200L)
            try {
                journal.checkpoint(
                    StabilizationRestoreWriteStep.PREPARED,
                    StabilizationRestoreWriteStep.BEFORE_PERSONAL,
                    201L,
                )
                // One REAL Android DataStore transaction, never the user's data.
                personal.replaceFromApprovedImport(target.merged.personal)
                throw SyntheticProcessDeath() // before PERSONAL_WRITTEN checkpoint
            } catch (_: SyntheticProcessDeath) {
                // Simulated process death: discard in-memory journal instance.
            }
            val cold = StabilizationRestoreWriteAheadJournal(root).inspectAfterRestart()!!
            assertEquals(StabilizationRestoreWriteStep.BEFORE_PERSONAL, cold.step)
            assertTrue(cold.recoveryRequiresOwnerReview)
            assertFalse(cold.completedRestore)
            assertFalse(cold.authorizedToWriteLive)
            assertEquals(setOf(existing.id, imported.id),
                personal.load().map { it.id }.toSet())
            assertEquals(existing, personal.load().single { it.id == existing.id })
            assertTrue("Challenge must not be fabricated after interruption",
                challenges.load().isEmpty())
            assertTrue("Group must not be fabricated after interruption",
                groups.load().isEmpty())
            // A stale preview must not falsely say the live phone matches the
            // target after only Personal has committed.
            val partial = V301LocalRecoveryReader(app).read()
            val partialDigest = V304RestorePlanPolicy.fingerprint(
                partial.components, partial.photos,
            )
            assertNotEquals(target.startingDigest, partialDigest)
            assertNotEquals(target.targetDigest, partialDigest)
        } finally {
            root.deleteRecursively() // ONLY synthetic cache fixture.
        }
    }
}
