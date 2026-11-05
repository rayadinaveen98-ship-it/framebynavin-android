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
     * A successful Compose node assertion is not a proof the image has painted:
     * CI's original Settings capture was all background and Spaces was partial.
     * Reject nearly-empty screenshots, retry after rendering, then capture the
     * actual Compose root if Android window compositing is not yet available.
     */
    private fun hasVisiblePaint(bitmap: Bitmap): Boolean {
        val left = (bitmap.width / 16).coerceAtLeast(1)
        val right = bitmap.width - left
        val startY = (bitmap.height / 12).coerceAtLeast(1)
        val endY = (bitmap.height * 4 / 5).coerceAtMost(bitmap.height - 1)
        var sampled = 0
        var visible = 0
        for (y in startY until endY step 16) {
            for (x in left until right step 16) {
                val pixel = bitmap.getPixel(x, y)
                val red = (pixel ushr 16) and 255
                val green = (pixel ushr 8) and 255
                val blue = pixel and 255
                if (maxOf(red, green, blue) > 115) visible++
                sampled++
            }
        }
        return sampled > 0 && visible.toDouble() / sampled >= 0.018
    }

    private fun screenshot(name: String) {
        HardeningTestEnvironment.requireCiEmulator()
        rule.waitForIdle()
        val folder = File(requireNotNull(app.getExternalFilesDir(null)), "v294-visual-audit")
        check(folder.isDirectory || folder.mkdirs()) { "Couldn't prepare synthetic screenshot folder" }
        val target = File(folder, name)

        var screenshot: Bitmap? = null
        // The Android window may lag the Compose semantics tree. Never publish
        // an all-black/unfinished screenshot as passing visual evidence.
        for (attempt in 1..12) {
            rule.waitForIdle()
            val fresh = InstrumentationRegistry.getInstrumentation().uiAutomation.takeScreenshot()
            if (fresh != null) {
                if (hasVisiblePaint(fresh)) {
                    screenshot = fresh
                    break
                }
                fresh.recycle()
            }
            android.os.SystemClock.sleep(350)
        }
        // Compose root capture is a deterministic fallback without system bars.
        if (screenshot == null) {
            rule.waitForIdle()
            screenshot = rule.onRoot().captureToImage().asAndroidBitmap()
        }
        val bitmap = screenshot
        try {
            check(hasVisiblePaint(bitmap)) {
                "Visual QA rejected incomplete or all-background capture: $name"
            }
            target.outputStream().use { stream ->
                assertTrue(bitmap.compress(Bitmap.CompressFormat.PNG, 100, stream))
            }
            check(target.isFile && target.length() > 10_000L) {
                "Synthetic screenshot unexpectedly small: $name"
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
        rule.onNodeWithText("Backlot, your way").assertExists()
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
        screenshot("02-personal-my-day-empty.png")
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
            val repo = V289SpacesRepository(db.spaces())
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
}
