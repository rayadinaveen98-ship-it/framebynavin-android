package com.framebynavin.app.ui

import android.graphics.Bitmap
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.room.Room
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.framebynavin.app.data.CreatorOsSettings
import com.framebynavin.app.data.HardeningTestEnvironment
import com.framebynavin.app.personal.spaces.V289PersonalSpacePolicy
import com.framebynavin.app.personal.spaces.V289SpaceEntry
import com.framebynavin.app.personal.spaces.V289SpaceEntryKind
import com.framebynavin.app.personal.spaces.V289SpaceEntrySource
import com.framebynavin.app.personal.spaces.V289SpacesDatabase
import com.framebynavin.app.personal.spaces.V289SpacesRepository
import com.framebynavin.app.personal.spaces.V289SpaceType
import com.framebynavin.app.ui.theme.FrameByNavinTheme
import java.io.File
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * V294 visual evidence gate: ONLY synthetic/disposable emulator screenshots.
 * No real owner data, Google account, photos, notifications, alarms or cloud use.
 * Does not edit production UI or back up/restores app data.
 */
@RunWith(AndroidJUnit4::class)
class V294VisualAuditDeviceTest {
    @get:Rule val rule = createComposeRule()
    private val app get() =
        InstrumentationRegistry.getInstrumentation().targetContext.applicationContext

    /**
     * Capture the actual app Compose tree, not the whole emulator display.
     * A system-level screenshot previously recorded a "Quickstep isn't responding"
     * ANR dialog in all four PNGs, falsely passing the brightness gate.
     */
    private fun screenshot(name: String) {
        HardeningTestEnvironment.requireCiEmulator()
        rule.waitForIdle()
        val folder = File(requireNotNull(app.getExternalFilesDir(null)), "v294-visual-audit")
        check(folder.isDirectory || folder.mkdirs()) { "Couldn't prepare synthetic screenshot folder" }
        val target = File(folder, name)
        val bitmap = rule.onRoot().captureToImage().asAndroidBitmap()
        try {
            check(bitmap.width >= 200 && bitmap.height >= 200) {
                "Compose root unexpectedly small: $name"
            }
            target.outputStream().use { stream ->
                assertTrue(bitmap.compress(Bitmap.CompressFormat.PNG, 100, stream))
            }
            check(target.isFile && target.length() > 10_000L) {
                "Compose screenshot unexpectedly small: $name"
            }
        } finally { bitmap.recycle() }
    }

    @Test fun settingsHomeEmptyOwnerProfile() {
        HardeningTestEnvironment.requireCiEmulator()
        rule.setContent {
            FrameByNavinTheme {
                V144SettingsHub(
                    settings = CreatorOsSettings(), tasks = emptyList(),
                    weeklyAutoPlanEnabled = false, notificationsReady = false,
                    preciseTimingReady = false, fullScreenReady = false,
                    batteryReady = false, onClose = {}, onProfile = {}, onVoice = {},
                    onAlarmTimeout = {}, onSnooze = {}, onWeeklyAutoPlan = {},
                    onContextNudges = {}, onNotifications = {}, onPreciseTiming = {},
                    onFullScreen = {}, onBattery = {}, onReplayTour = {},
                    onCloudSync = {}, onYouTube = {},
                )
            }
        }
        rule.onNodeWithText("Settings").assertExists()
        screenshot("01-settings-home.png")
    }

    @Test fun personalMyDayEmpty() {
        HardeningTestEnvironment.requireCiEmulator()
        rule.setContent {
            FrameByNavinTheme {
                V2232PersonalReminderHub(onClose = {}, onCreator = {})
            }
        }
        rule.waitUntil(15_000) {
            rule.onAllNodesWithTag("v2232-personal-space-switcher")
                .fetchSemanticsNodes().isNotEmpty()
        }
        rule.waitUntil(15_000) {
            rule.onAllNodesWithTag("v289-open-spaces").fetchSemanticsNodes().isNotEmpty()
        }
        rule.onNodeWithText("My Spaces").assertExists()
        screenshot("02-personal-my-day-empty.png")
        // Verify the previously hidden destination is now reachable by label.
        rule.onNodeWithTag("v289-open-spaces").performClick()
        rule.waitUntil(15_000) {
            rule.onAllNodesWithTag("v289-spaces-root").fetchSemanticsNodes().isNotEmpty()
        }
    }

    @Test fun mySpacesEmpty() {
        HardeningTestEnvironment.requireCiEmulator()
        val db = Room.inMemoryDatabaseBuilder(app, V289SpacesDatabase::class.java).build()
        val mounted = mutableStateOf(true)
        try {
            rule.setContent {
                if (mounted.value) FrameByNavinTheme {
                    V289PersonalSpacesScreen(onClose = {}, databaseForTesting = db)
                }
            }
            rule.waitUntil(15_000) {
                rule.onAllNodesWithTag("v289-spaces-empty").fetchSemanticsNodes().isNotEmpty()
            }
            rule.onNodeWithText("No spaces yet").assertExists()
            screenshot("03-my-spaces-empty.png")
        } finally {
            rule.runOnIdle { mounted.value = false }
            rule.waitForIdle()
            db.close()
        }
    }

    @Test fun mySpacesWithSyntheticProgress() {
        HardeningTestEnvironment.requireCiEmulator()
        val db = Room.inMemoryDatabaseBuilder(app, V289SpacesDatabase::class.java).build()
        val id = "10000000-0000-4000-8000-000000000094"
        val entryId = "20000000-0000-4000-8000-000000000094"
        val time = System.currentTimeMillis()
        val space = V289PersonalSpacePolicy.newSpace(V289SpaceType.FITNESS, time, id)
        runBlocking {
            val repo = V289SpacesRepository(app, db.spaces())
            repo.createSpace(space)
            repo.logManualEntry(
                V289SpaceEntry(
                    id = entryId, spaceId = id, kind = V289SpaceEntryKind.METRIC,
                    source = V289SpaceEntrySource.MANUAL,
                    occurredAtMillis = time, createdAtMillis = time,
                    note = "Synthetic sample walk", metricName = "Workout time",
                    metricValue = 40.0, metricUnit = "minutes",
                )
            )
        }
        val mounted = mutableStateOf(true)
        try {
            rule.setContent {
                if (mounted.value) FrameByNavinTheme {
                    V289PersonalSpacesScreen(onClose = {}, databaseForTesting = db)
                }
            }
            rule.waitUntil(15_000) {
                rule.onAllNodesWithTag("v289-space-" + id).fetchSemanticsNodes().isNotEmpty()
            }
            rule.onNodeWithTag("v289-space-" + id).performClick()
            rule.waitUntil(15_000) {
                rule.onAllNodesWithTag("v292-recorded-metric")
                    .fetchSemanticsNodes().isNotEmpty()
            }
            screenshot("04-my-spaces-fitness-synthetic.png")
        } finally {
            rule.runOnIdle { mounted.value = false }
            rule.waitForIdle()
            db.close()
        }
    }
    @Test fun creatorTodayEmpty() {
        HardeningTestEnvironment.requireCiEmulator()
        rule.setContent {
            FrameByNavinTheme {
                PTodayScreen(
                    creatorProfile = com.framebynavin.app.data.CreatorProfile(),
                    tasks = emptyList(),
                    projectsLoaded = true,
                    onAdd = {}, onStart = {}, onAdvance = {},
                    onViewAllReminders = {}, onFocus = {},
                )
            }
        }
        rule.onNodeWithText("No projects yet").assertExists()
        screenshot("05-creator-today-empty.png")
    }

    @Test fun ideaVaultEmpty() {
        HardeningTestEnvironment.requireCiEmulator()
        rule.setContent {
            FrameByNavinTheme {
                V09IdeaVaultScreen(
                    ideas = emptyList(), onClose = null,
                    onSave = { null }, onDelete = {}, onArchive = {},
                    onConvert = { _, _, _, _ -> null },
                    onCreateIdeaRequested = {},
                )
            }
        }
        rule.onNodeWithText("No ideas yet").assertExists()
        screenshot("06-ideas-empty.png")
    }

    @Test fun contentCalendarEmpty() {
        HardeningTestEnvironment.requireCiEmulator()
        rule.setContent {
            FrameByNavinTheme {
                V15ContentCalendarScreen(
                    tasks = emptyList(), weeklySlots = emptyList(),
                    onClose = null,
                )
            }
        }
        rule.onNodeWithText("Next 14 days").assertExists()
        screenshot("07-calendar-empty.png")
    }

    @Test fun insightsDisconnected() {
        HardeningTestEnvironment.requireCiEmulator()
        rule.setContent {
            FrameByNavinTheme {
                V11InsightsScreen(
                    creatorProfile = com.framebynavin.app.data.CreatorProfile(),
                    tasks = emptyList(), ideas = emptyList(), onAdd = {},
                )
            }
        }
        rule.onNodeWithText("Insights").assertExists()
        screenshot("08-insights-disconnected.png")
    }

    @Test fun dailyBriefEmpty() {
        HardeningTestEnvironment.requireCiEmulator()
        rule.setContent {
            FrameByNavinTheme {
                V15DailyBriefScreen(
                    tasks = emptyList(), weeklySlots = emptyList(), onClose = {},
                )
            }
        }
        rule.onNodeWithText("What matters today").assertExists()
        screenshot("09-daily-brief-empty.png")
    }


    @Test fun personalSettingsAreUniversal() {
        HardeningTestEnvironment.requireCiEmulator()
        rule.setContent {
            FrameByNavinTheme {
                V2232PersonalReminderHub(onClose = {}, onCreator = {})
            }
        }
        rule.onNodeWithTag("v288-personal-toggle-settings").performClick()
        rule.waitUntil(15_000) {
            rule.onAllNodesWithTag("v295-personal-routines")
                .fetchSemanticsNodes().isNotEmpty()
        }
        rule.onNodeWithText("My Routines").assertExists()
        rule.onNodeWithText("Daily Rhythm").assertExists()
        rule.onNodeWithTag("v295-routines-new").assertExists()
        rule.onNodeWithText("YOUR LIFE · 21-DAY RESET").assertDoesNotExist()
        rule.onNodeWithText("Device reliability").assertDoesNotExist()
        screenshot("10-personal-settings-universal.png")
        rule.onNodeWithTag("v295-routines-new").performClick()
        rule.onNodeWithTag("v2232-personal-editor").assertExists()
        rule.onNodeWithContentDescription("Close editor").performClick()
    }
    @Test fun ownerChallengeStartsWithBlankTemplate() {
        HardeningTestEnvironment.requireCiEmulator()
        rule.setContent {
            FrameByNavinTheme { V297ChallengeScreen(onClose = {}) }
        }
        rule.waitUntil(15_000) {
            rule.onAllNodesWithTag("v297-create-open")
                .fetchSemanticsNodes().isNotEmpty()
        }
        rule.onNodeWithText("21-day challenges").assertExists()
        screenshot("11-challenges-owner-blank.png")
    }

    @Test fun ownerReminderGroupsStartUnpopulated() {
        HardeningTestEnvironment.requireCiEmulator()
        rule.setContent {
            FrameByNavinTheme { V298ReminderGroupsScreen(onClose = {}) }
        }
        rule.waitUntil(15_000) {
            rule.onAllNodesWithTag("v298-create-open")
                .fetchSemanticsNodes().isNotEmpty()
        }
        rule.onNodeWithText("Reminder Groups").assertExists()
        rule.onNodeWithTag("v298-create-open").assertExists()
        screenshot("12-reminder-groups-blank.png")
    }

    @Test fun localBackupReadinessIsOwnerInitiatedNotAnAutomaticCloudClaim() {
        HardeningTestEnvironment.requireCiEmulator()
        rule.setContent {
            FrameByNavinTheme { V299RecoveryReadinessPanel() }
        }
        rule.onNodeWithTag("v299-preflight-counts").assertDoesNotExist()
        rule.onNodeWithTag("v299-run-preflight").performClick()
        rule.waitUntil(20_000) {
            rule.onAllNodesWithTag("v299-preflight-counts")
                .fetchSemanticsNodes().isNotEmpty() ||
            rule.onAllNodesWithTag("v299-preflight-unavailable")
                .fetchSemanticsNodes().isNotEmpty()
        }
        screenshot("13-local-backup-preflight.png")
    }

    @Test fun encryptedBackupPanelDisclosesNoRestoreAndRequiresPassphrase() {
        HardeningTestEnvironment.requireCiEmulator()
        rule.setContent {
            FrameByNavinTheme { V302EncryptedPersonalArchivePanel() }
        }
        rule.onNodeWithTag("v302-export-encrypted").assertIsNotEnabled()
        rule.onNodeWithTag("v302-preview-archive").assertIsNotEnabled()
        rule.onNodeWithText("No restore action is available here.", substring = true)
            .assertExists()
        screenshot("14-encrypted-owner-archive-gate.png")
    }

}
