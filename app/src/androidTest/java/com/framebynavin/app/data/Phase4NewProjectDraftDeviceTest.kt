package com.framebynavin.app.data

import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.framebynavin.app.reminders.ReminderMusicMode
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Disposable emulator-only; no real projects or reminders are created,
 * no alarm is scheduled and no unrelated preferences are deleted.
 */
@RunWith(AndroidJUnit4::class)
class Phase4NewProjectDraftDeviceTest {
    @Test fun localFormSurvivesNewStoreAndCanOnlyBeClearedByMatchingReceipt() {
        HardeningTestEnvironment.requireCiEmulator()
        val app = InstrumentationRegistry.getInstrumentation()
            .targetContext.applicationContext
        val generation = Long.MAX_VALUE - 920L
        val olderGeneration = generation - 1L
        val store = CreatorNewProjectDraftStore(app)
        val original = CreatorNewProjectFormDraft(
            title = "తెలుగు documentary idea",
            creatorModeId = "filmmaker", archetypeId = "video",
            productionStyles = listOf("cinematic"),
            platform = "YouTube", contentType = "Long video",
            dueAt = 1_800_000_000_000L,
            attentionPlan = ProjectAttentionPlan.GUIDED,
            deliveryPreference = ReminderDeliveryPreference.AUTO,
            customReminderAt = 1_799_999_000_000L,
            priority = TaskPriority.IMPORTANT,
            notes = "Research and interviews",
            soundUri = "", voiceMusicMode = ReminderMusicMode.entries.first(),
            voiceMusicUri = "", voice = VoicePersona.entries.first(),
            repeatCount = 3, repeatGap = 10, alarmTimeout = 120,
            snoozeMinutes = 5, autoRetryEnabled = false,
            autoRetryMinutes = 5, autoRetryMaxAttempts = 2,
        )
        try {
            store.save(generation, original)
            val receipt = requireNotNull(store.contentFingerprint(generation))
            val again = CreatorNewProjectDraftStore(app)
            assertEquals(original, again.load(generation))
            assertEquals(receipt, again.contentFingerprint(generation))
            assertNull(again.load(olderGeneration))
            assertFalse(again.clearIfUnchanged(generation, "0".repeat(64)))
            assertEquals(original, again.load(generation))
            again.save(generation, original.copy(notes = "Research, interviews, visual plan"))
            assertFalse(again.clearIfUnchanged(generation, receipt))
            assertEquals("Research, interviews, visual plan",
                again.load(generation)?.notes)
            assertTrue(again.hasUnreviewedDifferentGeneration(olderGeneration))
            val finalReceipt = requireNotNull(again.contentFingerprint(generation))
            assertTrue(again.clearIfUnchanged(generation, finalReceipt))
            assertNull(again.load(generation))
        } finally {
            store.clear(generation)
        }
    }
}
