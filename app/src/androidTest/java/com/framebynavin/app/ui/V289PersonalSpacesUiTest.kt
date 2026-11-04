package com.framebynavin.app.ui

import androidx.compose.ui.test.*
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.room.Room
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.framebynavin.app.data.HardeningTestEnvironment
import com.framebynavin.app.personal.PersonalReminderStore
import com.framebynavin.app.personal.spaces.V289SpacesDatabase
import com.framebynavin.app.ui.theme.FrameByNavinTheme
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/** Disposable Room and CI emulator only. Nothing here writes real owner reminders. */
@RunWith(AndroidJUnit4::class)
class V289PersonalSpacesUiTest {
    @get:Rule val rule = createComposeRule()
    private val app get() =
        InstrumentationRegistry.getInstrumentation().targetContext.applicationContext

    @Test fun openingSpacesDoesNotSeedAnyDataOrAlterReminders() {
        HardeningTestEnvironment.requireCiEmulator()
        val before = runBlocking { PersonalReminderStore(app).load() }
        val db = Room.inMemoryDatabaseBuilder(app, V289SpacesDatabase::class.java).build()
        val mounted = mutableStateOf(true)
        try {
            rule.setContent {
                if (mounted.value) {
                    FrameByNavinTheme {
                        V289PersonalSpacesScreen(onClose = {}, databaseForTesting = db)
                    }
                }
            }
            rule.onNodeWithTag("v289-spaces-root").assertExists()
            rule.waitUntil(12_000) {
                rule.onAllNodesWithTag("v289-spaces-empty").fetchSemanticsNodes().isNotEmpty()
            }
            assertEquals(0, runBlocking { db.spaces().loadSpaces().size })
            assertEquals(before, runBlocking { PersonalReminderStore(app).load() })
        } finally {
            // Dispose Room Flow collectors before closing the in-memory database.
            rule.runOnIdle { mounted.value = false }
            rule.waitForIdle()
            db.close()
        }
    }

    @Test fun createSpaceThenManualLogPersistsInIsolatedDatabase() {
        HardeningTestEnvironment.requireCiEmulator()
        val before = runBlocking { PersonalReminderStore(app).load() }
        val db = Room.inMemoryDatabaseBuilder(app, V289SpacesDatabase::class.java).build()
        val mounted = mutableStateOf(true)
        try {
            rule.setContent {
                if (mounted.value) {
                    FrameByNavinTheme {
                        V289PersonalSpacesScreen(onClose = {}, databaseForTesting = db)
                    }
                }
            }
            rule.waitUntil(12_000) {
                rule.onAllNodesWithTag("v289-spaces-empty").fetchSemanticsNodes().isNotEmpty()
            }
            rule.onNodeWithTag("v289-create-open").assertIsEnabled().performClick()
            rule.onNodeWithTag("v289-create-save").performClick()
            rule.waitUntil(12_000) {
                rule.onAllNodesWithTag("v289-log-open").fetchSemanticsNodes().isNotEmpty()
            }
            rule.onNodeWithTag("v289-log-open").performClick()
            rule.onNodeWithTag("v289-checkin-note").performTextInput(
                "Washed face and applied sunscreen"
            )
            rule.onNodeWithTag("v289-checkin-save").performClick()
            rule.waitUntil(12_000) {
                rule.onAllNodesWithTag("v289-entry").fetchSemanticsNodes().size == 1
            }
            val spaces = runBlocking { db.spaces().loadSpaces() }
            assertEquals(1, spaces.size)
            val logs = runBlocking { db.spaces().entriesForSpace(spaces.single().id) }
            assertEquals(1, logs.size)
            assertEquals("MANUAL", logs.single().source)
            assertEquals("Washed face and applied sunscreen", logs.single().note)
            assertTrue(runBlocking { db.spaces().linksForSpace(spaces.single().id).isEmpty() })
            assertEquals(before, runBlocking { PersonalReminderStore(app).load() })
        } finally {
            // Dispose Room Flow collectors before closing the in-memory database.
            rule.runOnIdle { mounted.value = false }
            rule.waitForIdle()
            db.close()
        }
    }
    @Test fun editingSpaceDetailsDoesNotCreateOrMutateReminders() {
        HardeningTestEnvironment.requireCiEmulator()
        val before = runBlocking { PersonalReminderStore(app).load() }
        val db = Room.inMemoryDatabaseBuilder(app, V289SpacesDatabase::class.java).build()
        val mounted = mutableStateOf(true)
        try {
            rule.setContent {
                if (mounted.value) {
                    FrameByNavinTheme {
                        V289PersonalSpacesScreen(onClose = {}, databaseForTesting = db)
                    }
                }
            }
            rule.waitUntil(12_000) {
                rule.onAllNodesWithTag("v289-spaces-empty").fetchSemanticsNodes().isNotEmpty()
            }
            rule.onNodeWithTag("v289-create-open").performClick()
            rule.onNodeWithTag("v289-create-save").performClick()
            rule.waitUntil(12_000) {
                rule.onAllNodesWithTag("v289-edit-open").fetchSemanticsNodes().isNotEmpty()
            }
            rule.onNodeWithTag("v289-edit-open").performClick()
            rule.onNodeWithTag("v289-edit-title").performTextReplacement("My Skin Journal")
            rule.onNodeWithTag("v289-edit-goal").performTextInput(
                "A simple routine I can maintain"
            )
            rule.onNodeWithTag("v289-edit-save").performClick()
            rule.waitUntil(12_000) {
                runBlocking {
                    db.spaces().loadSpaces().singleOrNull()?.title == "My Skin Journal"
                }
            }
            val saved = runBlocking { db.spaces().loadSpaces().single() }
            assertEquals("A simple routine I can maintain", saved.goalText)
            assertTrue(runBlocking { db.spaces().linksForSpace(saved.id).isEmpty() })
            assertEquals(before, runBlocking { PersonalReminderStore(app).load() })
        } finally {
            rule.runOnIdle { mounted.value = false }
            rule.waitForIdle()
            db.close()
        }
    }

    @Test fun manualBackupDialogWarnsAboutPlaintextAndRequiresExplicitPicker() {
        HardeningTestEnvironment.requireCiEmulator()
        val personalBefore = runBlocking { PersonalReminderStore(app).load() }
        val db = Room.inMemoryDatabaseBuilder(app, V289SpacesDatabase::class.java).build()
        val mounted = mutableStateOf(true)
        try {
            rule.setContent {
                if (mounted.value) {
                    FrameByNavinTheme {
                        V289PersonalSpacesScreen(onClose = {}, databaseForTesting = db)
                    }
                }
            }
            rule.waitUntil(12_000) {
                rule.onAllNodesWithTag("v289-spaces-empty").fetchSemanticsNodes().isNotEmpty()
            }
            rule.onNodeWithTag("v289-backup-open").performClick()
            rule.onNodeWithTag("v289-backup-plaintext-warning").assertExists()
            rule.onNodeWithTag("v289-backup-export").assertExists().assertIsEnabled()
            rule.onNodeWithTag("v289-backup-import").assertExists().assertIsEnabled()
            rule.onNodeWithTag("v289-backup-import-confirm").assertDoesNotExist()
            // Merely opening the backup dialog cannot create a file or import data.
            assertTrue(runBlocking { db.spaces().loadSpaces().isEmpty() })
            assertEquals(personalBefore, runBlocking { PersonalReminderStore(app).load() })
        } finally {
            rule.runOnIdle { mounted.value = false }
            rule.waitForIdle()
            db.close()
        }
    }

    @Test fun newSpacePhotoJournalIsOptInAndWarnsAboutNoCloudBackup() {
        HardeningTestEnvironment.requireCiEmulator()
        val originalReminders = runBlocking { PersonalReminderStore(app).load() }
        val db = Room.inMemoryDatabaseBuilder(app, V289SpacesDatabase::class.java).build()
        val mounted = mutableStateOf(true)
        try {
            rule.setContent {
                if (mounted.value) {
                    FrameByNavinTheme {
                        V289PersonalSpacesScreen(onClose = {}, databaseForTesting = db)
                    }
                }
            }
            rule.waitUntil(12_000) {
                rule.onAllNodesWithTag("v289-spaces-empty").fetchSemanticsNodes().isNotEmpty()
            }
            rule.onNodeWithTag("v289-create-open").performClick()
            rule.onNodeWithTag("v289-create-save").performClick()
            rule.waitUntil(12_000) {
                rule.onAllNodesWithTag("v290-photo-journal").fetchSemanticsNodes().isNotEmpty()
            }
            rule.onNodeWithTag("v290-photo-empty").assertExists()
            rule.onNodeWithTag("v290-pick-photo").assertExists().assertIsEnabled()
            rule.onNodeWithTag("v290-photo-private-notice").assertExists()
            val space = runBlocking { db.spaces().loadSpaces().single() }
            assertTrue(runBlocking { db.spaces().photosForSpace(space.id).isEmpty() })
            assertEquals(originalReminders, runBlocking { PersonalReminderStore(app).load() })
        } finally {
            rule.runOnIdle { mounted.value = false }
            rule.waitForIdle()
            db.close()
        }
    }

}
