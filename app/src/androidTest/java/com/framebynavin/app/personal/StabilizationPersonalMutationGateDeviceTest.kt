package com.framebynavin.app.personal

import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.framebynavin.app.data.HardeningTestEnvironment
import com.framebynavin.app.personal.spaces.V290PrivatePhotoFiles
import com.framebynavin.app.personal.spaces.V290SpacePhotoEntity
import java.io.File
import java.time.LocalDate
import java.time.LocalTime
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith

/** Real Android DataStore, on a cleared disposable emulator ONLY. */
@RunWith(AndroidJUnit4::class)
class StabilizationPersonalMutationGateDeviceTest {
    @Test fun directPrivateJpegHelpersCannotCreateOrDeleteDuringRecovery() {
        HardeningTestEnvironment.requireCiEmulator()
        val app = InstrumentationRegistry.getInstrumentation().targetContext.applicationContext
        val root = StabilizationPersonalMutationGate.journalRootFor(app)
        assertFalse("Requires cleared disposable emulator", root.exists())
        val id = "2e01c0b5-ec32-4f4c-8ea9-5ca7b4100cd1"
        val space = "81ed09c3-5713-4bca-a40c-fbb909a55f06"
        val prepared = V290PrivatePhotoFiles.PreparedPhoto(
            id = id, fileName = "$id.jpg",
            bytes = byteArrayOf(0xFF.toByte(), 0xD8.toByte()),
            sha256Hex = "a".repeat(64), widthPx = 10, heightPx = 10,
        )
        val existing = V290SpacePhotoEntity(
            id = id, spaceId = space, takenAtMillis = 100L,
            addedAtMillis = 120L, fileName = "$id.jpg",
            sha256Hex = "a".repeat(64), widthPx = 10, heightPx = 10, note = "",
        )
        val target = File(File(File(app.filesDir, "backlot-personal-space-photos"), space), "$id.jpg")
        try {
            assertTrue(root.mkdir())
            assertThrows(IllegalStateException::class.java) {
                V290PrivatePhotoFiles.storePrepared(app, space, prepared)
            }
            assertThrows(IllegalStateException::class.java) {
                V290PrivatePhotoFiles.deleteFile(app, existing)
            }
            assertFalse("No photo bytes were written", target.exists())
            assertTrue(root.exists())
        } finally {
            root.deleteRecursively() // Only synthetic transaction marker.
        }
    }

    @Test fun followUpSettingsCannotChangeDuringPendingRecovery() = runBlocking {
        HardeningTestEnvironment.requireCiEmulator()
        val app = InstrumentationRegistry.getInstrumentation().targetContext.applicationContext
        val root = StabilizationPersonalMutationGate.journalRootFor(app)
        check(!root.exists()) { "Only a cleared CI emulator can run this test" }
        val settings = PersonalFollowUpSettingsStore(app)
        settings.setEnabled(false)
        settings.setDelay(60)
        val before = settings.current()
        try {
            check(root.mkdir())
            assertThrows(IllegalStateException::class.java) {
                runBlocking { settings.setEnabled(true) }
            }
            assertThrows(IllegalStateException::class.java) {
                runBlocking { settings.setDelay(120) }
            }
            assertEquals(before, settings.current())
        } finally {
            root.deleteRecursively() // Synthetic emulator-only recovery marker
        }
    }

    @Test fun pendingJournalBlocksAllThreeDataStoresAndSnapshotWithoutDataLoss() = runBlocking {
        HardeningTestEnvironment.requireCiEmulator()
        val app = InstrumentationRegistry.getInstrumentation().targetContext.applicationContext
        val root = StabilizationPersonalMutationGate.journalRootFor(app)
        check(!root.exists()) { "Disposable test must not contain prior recovery state" }
        val personal = PersonalReminderStore(app)
        val challenges = V297ChallengeStore(app)
        val groups = V298ReminderGroupStore(app)
        assertTrue(personal.load().isEmpty())
        assertTrue(challenges.load().isEmpty())
        assertTrue(groups.load().isEmpty())

        val record = PersonalReminder(
            id = "synthetic-original", title = "Keep original reminder",
            startDate = LocalDate.of(2026, 12, 11),
            time = LocalTime.of(9, 30),
            createdAtMillis = 100L, updatedAtMillis = 100L,
        )
        val plan = V297Challenge(
            id = "synthetic-plan", title = "Owner-defined synthetic plan",
            startDate = LocalDate.of(2026, 12, 1), createdAtMillis = 100L,
        )
        val group = V298ReminderGroup(
            id = "synthetic-group", name = "Synthetic group",
            reminderIds = listOf(record.id),
            createdAtMillis = 100L, updatedAtMillis = 100L,
        )
        personal.create(record)
        challenges.upsert(plan)
        groups.create(group)
        assertEquals(listOf(record), personal.load())
        assertEquals(listOf(plan), challenges.load())
        assertEquals(listOf(group), groups.load())

        try {
            // Simulates the persistent directory of a PREPARED or interrupted
            // recovery transaction. Its presence alone must fail closed.
            check(root.mkdir())
            assertThrows(IllegalStateException::class.java) {
                runBlocking {
                    personal.create(record.copy(id = "duplicate-unapproved"))
                }
            }
            assertThrows(IllegalStateException::class.java) {
                runBlocking {
                    challenges.upsert(plan.copy(id = "another-plan"))
                }
            }
            assertThrows(IllegalStateException::class.java) {
                runBlocking {
                    groups.create(group.copy(id = "another-group"))
                }
            }
            assertThrows(IllegalStateException::class.java) {
                runBlocking { V301LocalRecoveryReader(app).read() }
            }
            assertEquals(listOf(record), personal.load())
            assertEquals(listOf(plan), challenges.load())
            assertEquals(listOf(group), groups.load())
            assertTrue(root.exists())
        } finally {
            // Emulated synthetic transaction only; never owner data.
            root.deleteRecursively()
        }

        // Existing functionality is preserved when there is no transaction.
        challenges.upsert(plan.copy(title = "Updated after recovery test"))
        assertEquals("Updated after recovery test", challenges.load().single().title)
    }
}
