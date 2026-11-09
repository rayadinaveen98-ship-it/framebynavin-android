package com.framebynavin.app.cloud

import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.framebynavin.app.data.HardeningTestEnvironment
import com.framebynavin.app.data.TaskStore
import com.framebynavin.app.data.IdeaVaultStore
import com.framebynavin.app.data.WeeklyScheduleStore
import com.framebynavin.app.personal.PersonalReminderStore
import com.framebynavin.app.personal.Phase1HabitStore
import java.util.HashSet
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith

/**
 * T1B read-only inventory on disposable Android 15 emulator. Does not call
 * Backup, cloud, restore, delete, permissions or reschedule actions.
 */
@RunWith(AndroidJUnit4::class)
class TrustT1BInventoryDeviceTest {
    @Test fun actualOwnerStoreReadersProduceEvidenceWithoutWritingOrClaimingBackup() = runBlocking {
        HardeningTestEnvironment.requireCiEmulator()
        val app = InstrumentationRegistry.getInstrumentation()
            .targetContext.applicationContext
        val tasks = TaskStore(app)
        val ideas = IdeaVaultStore(app)
        val personal = PersonalReminderStore(app)
        val habits = Phase1HabitStore(app)
        val beforeTasks = tasks.exportJson()
        val beforeIdeas = ideas.exportJson()
        val beforePersonal = personal.exportPersonalJson()
        val beforeHabits = habits.exportJson()
        val weekly = WeeklyScheduleStore(app)
        val beforeWeekly = weekly.inspectSavedForInventory()
        val beforeCreatorSettings = app.getSharedPreferences(
            "creator_os_settings_v1", 0,
        ).all.toMap()
        val beforeSlides = app.getSharedPreferences(
            "best_frames_original_prefs", 0,
        ).all.toMap()

        val inventory = TrustLocalBackupInventoryReader(app).inspectReadOnly()
        assertEquals(inventory.inspected.map { it.domain }.toSet().size,
            inventory.inspected.size)
        val domains = inventory.inspected.mapTo(HashSet()) { it.domain }
        assertTrue(TrustBackupDomain.CREATOR_PROJECTS in domains)
        assertTrue(TrustBackupDomain.CREATOR_IDEAS in domains)
        assertTrue(TrustBackupDomain.CREATOR_FOCUS in domains)
        assertTrue(TrustBackupDomain.PERSONAL_REMINDERS in domains)
        assertTrue(TrustBackupDomain.PERSONAL_SINGLE_ACTIVITY_DONE in domains)
        assertTrue(TrustBackupDomain.PERSONAL_PHOTO_METADATA in domains)
        assertTrue(TrustBackupDomain.PERSONAL_PHOTO_BYTES in domains)
        assertTrue(TrustBackupDomain.CREATOR_HERO_ORIGINAL_BYTES in domains)
        assertTrue(TrustBackupDomain.HOME_SLIDESHOW_MEDIA in domains)
        assertTrue(TrustBackupDomain.CREATOR_WORKFLOW_TIMELINE in domains)
        assertTrue(TrustBackupDomain.CREATOR_PULSE_HISTORY in domains)
        assertTrue(TrustBackupDomain.CREATOR_POST_PUBLISH in domains)
        if (beforeWeekly == null) {
            assertTrue(TrustBackupDomain.CREATOR_WEEKLY_SLOTS in inventory.pendingDomains)
        } else {
            assertTrue(TrustBackupDomain.CREATOR_WEEKLY_SLOTS in domains)
        }
        assertTrue(TrustBackupDomain.CREATOR_MEDIA in inventory.pendingDomains)
        val hero = inventory.inspected.single {
            it.domain == TrustBackupDomain.CREATOR_HERO_ORIGINAL_BYTES
        }
        val slideshow = inventory.inspected.single {
            it.domain == TrustBackupDomain.HOME_SLIDESHOW_MEDIA
        }
        assertEquals(hero.sha256, slideshow.sha256)
        assertEquals(hero.bytes, slideshow.bytes)
        assertFalse(inventory.crossStoreAtomicityVerified)
        assertFalse(inventory.canClaimCompleteBackup)
        assertFalse(inventory.canUploadAsUniversalBackup)

        assertEquals(beforeTasks, tasks.exportJson())
        assertEquals(beforeIdeas, ideas.exportJson())
        assertEquals(beforePersonal, personal.exportPersonalJson())
        assertEquals(beforeHabits, habits.exportJson())
        assertEquals("Inventory may not seed or migrate the weekly plan",
            beforeWeekly, weekly.inspectSavedForInventory())
        assertEquals("Inventory must not initialize Creator settings",
            beforeCreatorSettings, app.getSharedPreferences(
                "creator_os_settings_v1", 0,
            ).all.toMap())
        assertEquals("Inventory must not change slideshow playback or image order",
            beforeSlides, app.getSharedPreferences(
                "best_frames_original_prefs", 0,
            ).all.toMap())
    }
}
