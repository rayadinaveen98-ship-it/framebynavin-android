package com.framebynavin.app.data

import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import java.util.UUID

@RunWith(AndroidJUnit4::class)
class ProjectPulseRc3PersistenceInstrumentedTest {

    @Test
    fun customStageManagedFlagAndCheckpointSurviveTaskStoreReload() = runBlocking {
        HardeningTestEnvironment.requireCiEmulator()
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val store = TaskStore(context)
        val id = "rc3-custom-${UUID.randomUUID()}"
        val now = System.currentTimeMillis()
        val checkpoint = now + 90 * 60_000L
        val task = CreatorTask(
            id = id,
            title = "RC3 Custom persistence",
            platform = "YouTube",
            contentType = "Long-form",
            dueLabel = "Later",
            dueAtMillis = now + 6 * 60 * 60_000L,
            status = TaskStatus.WORKING,
            workflowStageIndex = 2,
            progress = 25,
            attentionPlan = ProjectAttentionPlan.CUSTOM,
            pulseManagedReminder = true,
            reminderEnabled = true,
            reminderAtMillis = checkpoint,
            reminderMode = ReminderMode.VOICE,
            deliveryPreference = ReminderDeliveryPreference.VOICE,
            voiceEnabled = true,
            checkpointStageId = "script",
            checkpointAtMillis = checkpoint,
        )

        try {
            store.mutate { current -> current.filterNot { it.id == id } + task }
            val restored = store.load().first { it.id == id }

            assertEquals(ProjectAttentionPlan.CUSTOM, restored.attentionPlan)
            assertTrue(restored.pulseManagedReminder)
            assertTrue(restored.reminderEnabled)
            assertEquals(ReminderMode.VOICE, restored.reminderMode)
            assertEquals(ReminderDeliveryPreference.VOICE, restored.deliveryPreference)
            assertEquals(checkpoint, restored.reminderAtMillis)
            assertEquals("script", restored.checkpointStageId)
            assertEquals(checkpoint, restored.checkpointAtMillis)
        } finally {
            store.mutate { current -> current.filterNot { it.id == id } }
        }
    }

    @Test
    fun legacyRc2CustomFalseFlagStaysFalseButIsStillRecognizedAsStageCheckIn() = runBlocking {
        HardeningTestEnvironment.requireCiEmulator()
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val store = TaskStore(context)
        val id = "rc3-legacy-custom-${UUID.randomUUID()}"
        val now = System.currentTimeMillis()
        val task = CreatorTask(
            id = id,
            title = "RC2 legacy Custom",
            platform = "Instagram",
            contentType = "Reel",
            dueLabel = "Later",
            dueAtMillis = now + 3 * 60 * 60_000L,
            attentionPlan = ProjectAttentionPlan.CUSTOM,
            pulseManagedReminder = false,
            reminderEnabled = true,
            reminderAtMillis = now + 60 * 60_000L,
            reminderMode = ReminderMode.SIMPLE,
        )

        try {
            store.mutate { current -> current.filterNot { it.id == id } + task }
            val restored = store.load().first { it.id == id }

            assertFalse(restored.pulseManagedReminder)
            assertTrue(restored.reminderEnabled)
            assertTrue(ProjectPulseEngine.isStageCheckIn(restored))
        } finally {
            store.mutate { current -> current.filterNot { it.id == id } }
        }
    }
}
