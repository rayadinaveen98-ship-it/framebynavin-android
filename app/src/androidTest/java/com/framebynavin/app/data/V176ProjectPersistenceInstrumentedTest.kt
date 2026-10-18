package com.framebynavin.app.data

import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import java.util.UUID

/**
 * Real Android TaskStore round trip, not a mocked serialization test.
 * Uses a dedicated UUID and deletes only that record. No user data reset or migration.
 */
@RunWith(AndroidJUnit4::class)
class V176ProjectPersistenceInstrumentedTest {
    @Test
    fun projectNotesVoiceAndOverdueReminderStateSurviveEditingAndReload() = runBlocking {
        HardeningTestEnvironment.requireCiEmulator()
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val store = TaskStore(context)
        val id = "v176-project-${UUID.randomUUID()}"
        val due = System.currentTimeMillis() - 60_000L
        val nextReminder = System.currentTimeMillis() + 1_800_000L
        val original = CreatorTask(
            id = id,
            title = "Creator Studio Film",
            platform = "YouTube",
            contentType = "Long-form",
            dueLabel = "Overdue",
            dueAtMillis = due,
            status = TaskStatus.WORKING,
            progress = 44,
            attentionPlan = ProjectAttentionPlan.CUSTOM,
            reminderEnabled = true,
            reminderMode = ReminderMode.VOICE,
            deliveryPreference = ReminderDeliveryPreference.VOICE,
            voiceEnabled = true,
            voicePersona = VoicePersona.ROBOT,
            reminderAtMillis = nextReminder,
            snoozeCount = 2,
            workingUntilMillis = nextReminder,
            acknowledgedCheckpointStageId = "script",
            acknowledgedCheckpointDueAtMillis = nextReminder,
            notes = "Initial framing notes",
        )
        try {
            store.mutate { current -> current.filterNot { it.id == id } + original }
            val firstLoad = store.load().firstOrNull { it.id == id }
            assertNotNull(firstLoad)
            assertEquals(original.title, firstLoad!!.title)
            assertEquals(original.dueAtMillis, firstLoad.dueAtMillis)
            assertEquals(original.notes, firstLoad.notes)
            assertEquals(original.voicePersona, firstLoad.voicePersona)
            assertEquals(original.snoozeCount, firstLoad.snoozeCount)

            // Simulate an unrelated title and notes edit; guard against accidental deadline,
            // snooze, voice or progress resets in the persisted record.
            store.updateTask(id) { current ->
                current.copy(title = "Creator Studio Film — Updated", notes = "Final color reference")
            }
            val edited = store.load().firstOrNull { it.id == id }
            assertNotNull(edited)
            assertEquals("Creator Studio Film — Updated", edited!!.title)
            assertEquals("Final color reference", edited.notes)
            assertEquals(due, edited.dueAtMillis)
            assertEquals(44, edited.progress)
            assertEquals(2, edited.snoozeCount)
            assertEquals(nextReminder, edited.workingUntilMillis)
            assertEquals(nextReminder, edited.reminderAtMillis)
            assertEquals("script", edited.acknowledgedCheckpointStageId)
            assertEquals(VoicePersona.ROBOT, edited.voicePersona)
            assertEquals(ReminderDeliveryPreference.VOICE, edited.deliveryPreference)
            assertTrue(edited.reminderEnabled)
        } finally {
            // Do not wipe unrelated app data, even when a test assertion fails.
            CreatorDataGate.nonCancellable {
                store.mutate { current -> current.filterNot { it.id == id } }
            }
        }
    }
}
