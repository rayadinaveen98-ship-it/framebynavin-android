package com.framebynavin.app.cloud

import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.framebynavin.app.data.HardeningTestEnvironment
import com.framebynavin.app.data.TaskStore
import com.framebynavin.app.data.IdeaVaultStore
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
        assertTrue(TrustBackupDomain.PERSONAL_PHOTO_BYTES in inventory.pendingDomains)
        assertTrue(TrustBackupDomain.HOME_SLIDESHOW_MEDIA in inventory.pendingDomains)
        assertFalse(inventory.crossStoreAtomicityVerified)
        assertFalse(inventory.canClaimCompleteBackup)
        assertFalse(inventory.canUploadAsUniversalBackup)

        assertEquals(beforeTasks, tasks.exportJson())
        assertEquals(beforeIdeas, ideas.exportJson())
        assertEquals(beforePersonal, personal.exportPersonalJson())
        assertEquals(beforeHabits, habits.exportJson())
    }
}
