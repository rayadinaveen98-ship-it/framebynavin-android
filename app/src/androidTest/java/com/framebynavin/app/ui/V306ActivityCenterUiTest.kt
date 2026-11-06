package com.framebynavin.app.ui

import android.graphics.Bitmap
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.framebynavin.app.data.HardeningTestEnvironment
import com.framebynavin.app.personal.PersonalReminderStore
import com.framebynavin.app.ui.theme.FrameByNavinTheme
import java.io.File
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/** Synthetic emulator only: never invokes notification actions or modifies user reminders. */
@RunWith(AndroidJUnit4::class)
class V306ActivityCenterUiTest {
    @get:Rule val rule = createComposeRule()
    private val app get() =
        InstrumentationRegistry.getInstrumentation().targetContext.applicationContext

    private fun capture(name: String) {
        HardeningTestEnvironment.requireCiEmulator()
        rule.waitForIdle()
        val folder = File(requireNotNull(app.getExternalFilesDir(null)), "v306-visual-audit")
        check(folder.isDirectory || folder.mkdirs())
        val image = rule.onRoot().captureToImage().asAndroidBitmap()
        try {
            check(image.width >= 200 && image.height >= 200)
            File(folder, name).outputStream().use { out ->
                assertTrue(image.compress(Bitmap.CompressFormat.PNG, 100, out))
            }
            check(File(folder, name).length() > 10_000L)
        } finally { image.recycle() }
    }

    @Test fun filtersRenderAndLeaveAllPersonalRecordsUntouched() {
        HardeningTestEnvironment.requireCiEmulator()
        val store = PersonalReminderStore(app)
        val remindersBefore = runBlocking { store.load() }
        val historyBefore = runBlocking { store.loadCompletionHistory() }
        var closed = false
        rule.setContent {
            FrameByNavinTheme {
                V306ActivityCenterScreen(
                    tasks = emptyList(),
                    nowMillis = System.currentTimeMillis(),
                    onClose = { closed = true },
                    onOpenCreator = { error("No Creator rows") },
                    onOpenPersonal = { error("No Personal rows") },
                )
            }
        }
        rule.waitUntil(15_000) {
            rule.onAllNodesWithTag("v306-activity-center").fetchSemanticsNodes().isNotEmpty()
        }
        rule.onNodeWithText("Your real reminder activity").assertExists()
        rule.onNodeWithTag("v306-filter-needs_action").assertExists()
        rule.onNodeWithTag("v306-filter-all").assertExists()
        rule.onNodeWithTag("v306-filter-history").assertExists()
        capture("01-activity-needs-action.png")
        rule.onNodeWithTag("v306-filter-history").performClick()
        capture("02-activity-history.png")
        rule.onNodeWithTag("v306-filter-all").performClick()
        rule.onNodeWithTag("v306-activity-refresh").performClick()
        assertEquals(remindersBefore, runBlocking { store.load() })
        assertEquals(historyBefore, runBlocking { store.loadCompletionHistory() })
        rule.runOnIdle { assertEquals(false, closed) }
    }

    @Test fun personalToolbarOffersTheUnifiedCenterWithoutCreatingReminder() {
        HardeningTestEnvironment.requireCiEmulator()
        val store = PersonalReminderStore(app)
        val before = runBlocking { store.load() }
        var activityOpens = 0
        rule.setContent {
            FrameByNavinTheme {
                V2232PersonalReminderHub(
                    onClose = {},
                    onCreator = {},
                    onOpenActivity = { activityOpens++ },
                )
            }
        }
        rule.waitUntil(15_000) {
            rule.onAllNodesWithTag("v306-personal-open-activity")
                .fetchSemanticsNodes().isNotEmpty()
        }
        rule.onNodeWithTag("v306-personal-open-activity").performClick()
        rule.runOnIdle { assertEquals(1, activityOpens) }
        assertEquals(before, runBlocking { store.load() })
    }
}
