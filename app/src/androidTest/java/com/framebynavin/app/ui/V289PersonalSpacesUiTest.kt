package com.framebynavin.app.ui

import androidx.compose.ui.test.*
import android.graphics.Bitmap
import java.io.ByteArrayOutputStream
import java.util.UUID
import com.framebynavin.app.personal.spaces.V289PersonalSpacePolicy
import com.framebynavin.app.personal.spaces.V289SpacesRepository
import com.framebynavin.app.personal.spaces.V289SpaceType
import com.framebynavin.app.personal.spaces.V290PrivatePhotoFiles
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.room.Room
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.framebynavin.app.data.HardeningTestEnvironment
import com.framebynavin.app.personal.PersonalReminderStore
import com.framebynavin.app.personal.PersonalReminder
import java.time.LocalDate
import java.time.LocalTime
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
            rule.onNodeWithTag("v289-create-save").assertIsEnabled().performClick()
            // Room writes and the photo section are asynchronous. On shared
            // Android 15 CI runners the first composition can exceed 12s.
            rule.waitUntil(30_000) {
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

    @Test fun interruptedSaveOffersOwnerRecoveryWithoutWritingPersonalReminders() {
        HardeningTestEnvironment.requireCiEmulator()
        val originalReminders = runBlocking { PersonalReminderStore(app).load() }
        val db = Room.inMemoryDatabaseBuilder(app, V289SpacesDatabase::class.java).build()
        val space = V289PersonalSpacePolicy.newSpace(
            V289SpaceType.SKIN_CARE, System.currentTimeMillis(), UUID.randomUUID().toString()
        )
        runBlocking { V289SpacesRepository(app, db.spaces()).createSpace(space) }

        val original = Bitmap.createBitmap(48, 48, Bitmap.Config.ARGB_8888)
        val output = ByteArrayOutputStream()
        try {
            assertTrue(original.compress(Bitmap.CompressFormat.PNG, 100, output))
        } finally {
            original.recycle()
        }
        val prepared = V290PrivatePhotoFiles.sanitizeImage(
            output.toByteArray(), UUID.randomUUID().toString()
        )
        val orphan = V290PrivatePhotoFiles.storePrepared(app, space.id, prepared)
        assertTrue(orphan.setLastModified(System.currentTimeMillis() - 120_000L))
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
                rule.onAllNodesWithTag("v289-space-" + space.id).fetchSemanticsNodes()
                    .isNotEmpty()
            }
            rule.onNodeWithTag("v289-space-" + space.id).performClick()
            rule.waitUntil(12_000) {
                rule.onAllNodesWithTag("v290-recover-photo-open").fetchSemanticsNodes()
                    .isNotEmpty()
            }
            assertTrue(runBlocking { db.spaces().photosForSpace(space.id).isEmpty() })
            rule.onNodeWithTag("v290-recover-photo-open").performClick()
            rule.onNodeWithTag("v290-interrupted-action-confirm").performClick()
            rule.waitUntil(12_000) {
                runBlocking { db.spaces().photosForSpace(space.id).size == 1 }
            }
            val restored = runBlocking { db.spaces().photosForSpace(space.id).single() }
            assertEquals(prepared.id, restored.id)
            assertEquals(originalReminders, runBlocking { PersonalReminderStore(app).load() })
        } finally {
            rule.runOnIdle { mounted.value = false }
            rule.waitForIdle()
            orphan.delete()
            db.close()
        }
    }

    @Test fun deleteActionIsBehindMoreMenuAndCancelPreservesOwnerData() {
        HardeningTestEnvironment.requireCiEmulator()
        val remindersBefore = runBlocking { PersonalReminderStore(app).load() }
        val db = Room.inMemoryDatabaseBuilder(app, V289SpacesDatabase::class.java).build()
        val mounted = mutableStateOf(true)
        try {
            rule.setContent {
                if (mounted.value) FrameByNavinTheme {
                    V289PersonalSpacesScreen(onClose = {}, databaseForTesting = db)
                }
            }
            rule.waitUntil(12_000) {
                rule.onAllNodesWithTag("v289-spaces-empty").fetchSemanticsNodes().isNotEmpty()
            }
            rule.onNodeWithTag("v289-create-open").performClick()
            rule.onNodeWithTag("v289-create-save").performClick()
            rule.waitUntil(12_000) {
                rule.onAllNodesWithTag("v294-space-actions").fetchSemanticsNodes().isNotEmpty()
            }
            val before = runBlocking { db.spaces().loadSpaces() }
            assertEquals(1, before.size)
            rule.onNodeWithTag("v289-delete-open").assertDoesNotExist()
            rule.onNodeWithTag("v294-space-actions").performClick()
            rule.onNodeWithTag("v289-delete-open").assertExists().performClick()
            rule.onNodeWithText("Delete this Space?").assertExists()
            rule.onNodeWithTag("v289-delete-confirm").assertExists()
            rule.onNodeWithText("CANCEL").performClick()
            rule.onNodeWithTag("v289-delete-confirm").assertDoesNotExist()
            assertEquals(before, runBlocking { db.spaces().loadSpaces() })
            assertEquals(remindersBefore, runBlocking { PersonalReminderStore(app).load() })
        } finally {
            rule.runOnIdle { mounted.value = false }
            rule.waitForIdle()
            db.close()
        }
    }


    @Test fun standaloneNoteIsDurableAndDoesNotClaimReminderDone() {
        HardeningTestEnvironment.requireCiEmulator()
        val before = runBlocking { PersonalReminderStore(app).load() }
        val db = Room.inMemoryDatabaseBuilder(app, V289SpacesDatabase::class.java).build()
        val mounted = mutableStateOf(true)
        try {
            rule.setContent {
                if (mounted.value) FrameByNavinTheme {
                    V289PersonalSpacesScreen(onClose = {}, databaseForTesting = db)
                }
            }
            rule.waitUntil(12_000) {
                rule.onAllNodesWithTag("v289-spaces-empty").fetchSemanticsNodes().isNotEmpty()
            }
            rule.onNodeWithTag("v289-create-open").performClick()
            rule.onNodeWithTag("v289-create-save").performClick()
            rule.waitUntil(12_000) {
                rule.onAllNodesWithTag("v295-note-open").fetchSemanticsNodes().isNotEmpty()
            }
            rule.onNodeWithTag("v295-note-open").performScrollTo().performClick()
            rule.onNodeWithTag("v295-note-input").performTextInput("Private standalone observation")
            rule.onNodeWithTag("v295-note-save").performClick()
            rule.waitUntil(12_000) {
                runBlocking { db.spaces().allEntries().size == 1 }
            }
            val entry = runBlocking { db.spaces().allEntries().single() }
            assertEquals("NOTE", entry.kind)
            assertEquals("MANUAL", entry.source)
            assertEquals("Private standalone observation", entry.note)
            assertEquals(null, entry.reminderId)
            assertEquals(null, entry.originalOccurrenceMillis)
            rule.onNodeWithTag("v295-saved-note").assertExists()
            rule.onNodeWithText("0 logs").assertExists()
            rule.onNodeWithText("0 active days").assertExists()
            assertEquals(before, runBlocking { PersonalReminderStore(app).load() })
        } finally {
            rule.runOnIdle { mounted.value = false }
            rule.waitForIdle()
            db.close()
        }
    }

    @Test fun linkingAndUnlinkingReminderNeverMutatesItsSchedule() {
        HardeningTestEnvironment.requireCiEmulator()
        val store = PersonalReminderStore(app)
        val before = runBlocking { store.load() }
        val reminder = PersonalReminder(
            id = "v295-fixture-" + UUID.randomUUID(),
            title = "V295 skincare fixture",
            startDate = LocalDate.now().plusDays(1),
            time = LocalTime.of(8, 0),
        ).checked()
        val db = Room.inMemoryDatabaseBuilder(app, V289SpacesDatabase::class.java).build()
        val mounted = mutableStateOf(true)
        val openedId = mutableStateOf("")
        try {
            runBlocking { store.mutate { it + reminder } }
            rule.setContent {
                if (mounted.value) FrameByNavinTheme {
                    V289PersonalSpacesScreen(
                        onClose = {}, databaseForTesting = db,
                        onEditReminder = { openedId.value = it },
                    )
                }
            }
            rule.waitUntil(12_000) {
                rule.onAllNodesWithTag("v289-spaces-empty").fetchSemanticsNodes().isNotEmpty()
            }
            rule.onNodeWithTag("v289-create-open").performClick()
            rule.onNodeWithTag("v289-create-save").performClick()
            rule.waitUntil(12_000) {
                rule.onAllNodesWithTag("v295-link-open").fetchSemanticsNodes().isNotEmpty()
            }
            rule.onNodeWithTag("v295-link-open").performScrollTo().performClick()
            rule.waitUntil(12_000) {
                rule.onAllNodesWithText("V295 skincare fixture").fetchSemanticsNodes().isNotEmpty()
            }
            rule.onNodeWithText("V295 skincare fixture").performClick()
            rule.waitUntil(12_000) {
                runBlocking {
                    val space = db.spaces().loadSpaces().single()
                    db.spaces().linksForSpace(space.id).size == 1
                }
            }
            assertEquals(before + reminder, runBlocking { store.load() })
            rule.onNodeWithTag("v295-edit-linked-reminder").performScrollTo().performClick()
            assertEquals(reminder.id, openedId.value)
            rule.onNodeWithTag("v295-unlink-reminder").performScrollTo().performClick()
            rule.waitUntil(12_000) {
                runBlocking {
                    val space = db.spaces().loadSpaces().single()
                    db.spaces().linksForSpace(space.id).isEmpty()
                }
            }
            assertEquals(before + reminder, runBlocking { store.load() })
        } finally {
            rule.runOnIdle { mounted.value = false }
            rule.waitForIdle()
            runBlocking { store.mutate { it.filterNot { item -> item.id == reminder.id } } }
            db.close()
        }
    }
}
