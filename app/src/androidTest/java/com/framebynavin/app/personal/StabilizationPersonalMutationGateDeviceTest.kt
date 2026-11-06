package com.framebynavin.app.personal

import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.framebynavin.app.data.HardeningTestEnvironment
import java.time.LocalDate
import java.time.LocalTime
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith

/** Real Android DataStore, on a cleared disposable emulator ONLY. */
@RunWith(AndroidJUnit4::class)
class StabilizationPersonalMutationGateDeviceTest {
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
