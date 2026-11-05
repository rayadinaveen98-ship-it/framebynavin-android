package com.framebynavin.app.ui

import android.app.Application
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.ViewModelStore
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.framebynavin.app.data.CreatorOsSettingsStore
import com.framebynavin.app.data.CreatorProjectTemplates
import com.framebynavin.app.data.CreatorViewModel
import com.framebynavin.app.data.HardeningTestEnvironment
import com.framebynavin.app.data.IdeaVaultStore
import com.framebynavin.app.data.TaskStore
import com.framebynavin.app.data.WeeklyScheduleStore
import com.framebynavin.app.personal.PersonalReminderStore
import com.framebynavin.app.personal.spaces.V289SpacesDatabase
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith

/**
 * First-install data contract, running strictly on an isolated emulator.
 * The shell runner clears app data BEFORE this test and never touches
 * a physical phone or a connected user's real data.
 *
 * Templates, default preferences, sample labels and UI placeholders are not
 * equivalent to persisted creator records: they must be opt-in.
 */
@RunWith(AndroidJUnit4::class)
class V294FirstRunEmptyStateDeviceTest {
    private val app: Application get() =
        InstrumentationRegistry.getInstrumentation().targetContext.applicationContext as Application

    private fun checkAllStoresEmpty() = runBlocking {
        assertTrue("Fresh install must not create projects", TaskStore(app).load().isEmpty())
        assertTrue("Fresh install must not create ideas", IdeaVaultStore(app).load().isEmpty())
        assertTrue("Fresh install must not create weekly slots", WeeklyScheduleStore(app).loadOrSeed().isEmpty())
        val personal = PersonalReminderStore(app)
        assertTrue("Fresh install must not create personal reminders", personal.load().isEmpty())
        assertTrue("Fresh install must not invent completion receipts", personal.loadCompletionHistory().isEmpty())
        val db = V289SpacesDatabase.open(app)
        try {
            assertTrue("Spaces must be owner-created", db.spaces().loadSpaces().isEmpty())
            assertTrue("No synthetic progress activity may be inserted", db.spaces().allEntries().isEmpty())
        } finally {
            db.close()
        }
    }

    @Test fun newInstallAndCreatorStartupDoNotCreateExampleWork() {
        HardeningTestEnvironment.requireCiEmulator()
        val settings = CreatorOsSettingsStore(app).snapshot()
        assertFalse("No account identity on fresh install", settings.accountOnboardingComplete)
        assertFalse("No creator onboarding silently completed", settings.onboardingComplete)
        assertFalse("Weekly automation must be opt-in", settings.weeklyAutoPlanEnabled)
        assertEquals("", settings.creatorProfile.displayName)

        checkAllStoresEmpty()

        // Template definitions are opt-in suggestions, not automatically saved work.
        assertTrue(CreatorProjectTemplates.presets.isNotEmpty())
        assertTrue(CreatorProjectTemplates.contentTemplates.isNotEmpty())
        checkAllStoresEmpty()

        // Exercise real asynchronous Creator startup to catch accidental seeds.
        val vmStore = ViewModelStore()
        try {
            val model = ViewModelProvider(
                vmStore, ViewModelProvider.AndroidViewModelFactory.getInstance(app),
            )[CreatorViewModel::class.java]
            var loaded = model.tasksLoaded
            for (attempt in 1..120) {
                if (loaded) break
                Thread.sleep(125)
                loaded = model.tasksLoaded
            }
            assertTrue("Creator startup didn't reach a loaded state", loaded)
            assertTrue("Creator startup introduced sample projects", model.tasks.isEmpty())
            assertTrue("Creator startup introduced sample ideas", model.ideas.isEmpty())
            assertTrue("Creator startup introduced a default publishing plan", model.weeklySlots.isEmpty())
            assertTrue("Creator startup introduced demo progress", model.postPublishCheckpoints.isEmpty())
            checkAllStoresEmpty()
        } finally {
            vmStore.clear()
        }
    }
}
