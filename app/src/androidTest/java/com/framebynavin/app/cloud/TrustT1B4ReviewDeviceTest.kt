package com.framebynavin.app.cloud

import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.framebynavin.app.data.HardeningTestEnvironment
import com.framebynavin.app.data.IdeaVaultStore
import com.framebynavin.app.data.TaskStore
import com.framebynavin.app.data.WeeklyScheduleStore
import com.framebynavin.app.personal.PersonalReminderStore
import com.framebynavin.app.personal.Phase1HabitStore
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith

/**
 * On disposable Android15 only. This test must not mutate live owner stores:
 * no app backup, cloud session, account login, restore, alerts or file writes.
 */
@RunWith(AndroidJUnit4::class)
class TrustT1B4ReviewDeviceTest {
    @Test fun twoPassOwnerInventoryProducesOnlyReviewEvidenceWithoutSaving() = runBlocking {
        HardeningTestEnvironment.requireCiEmulator()
        val app = InstrumentationRegistry.getInstrumentation()
            .targetContext.applicationContext
        val tasks = TaskStore(app)
        val ideas = IdeaVaultStore(app)
        val personal = PersonalReminderStore(app)
        val habits = Phase1HabitStore(app)
        val weekly = WeeklyScheduleStore(app)

        val beforeTasks = tasks.exportJson()
        val beforeIdeas = ideas.exportJson()
        val beforePersonal = personal.exportPersonalJson()
        val beforeHabits = habits.exportJson()
        val beforeWeekly = weekly.inspectSavedForInventory()
        val beforeRoutines = app.getSharedPreferences(
            "creator_automation_preferences_v17", 0,
        ).all.toMap()

        val receipt = TrustInventoryReviewReader(app).inspectTwiceReadOnly()
        assertTrue(receipt.stableAcrossTwoReads)
        assertFalse(receipt.crossStoreAtomicityVerified)
        assertFalse(receipt.canClaimCompleteBackup)
        assertFalse(receipt.canStartCloudUpload)
        assertEquals(64, receipt.sha256.length)
        assertTrue(TrustBackupDomain.NOTIFICATION_PREFERENCES in receipt.pendingDomains)
        assertTrue(TrustBackupDomain.DEVICE_AI_SHARING_CONSENT in
            receipt.excludedDeviceOnlyDomains)
        assertEquals(beforeTasks, tasks.exportJson())
        assertEquals(beforeIdeas, ideas.exportJson())
        assertEquals(beforePersonal, personal.exportPersonalJson())
        assertEquals(beforeHabits, habits.exportJson())
        assertEquals(beforeWeekly, weekly.inspectSavedForInventory())
        assertEquals(beforeRoutines, app.getSharedPreferences(
            "creator_automation_preferences_v17", 0,
        ).all.toMap())
    }
}
