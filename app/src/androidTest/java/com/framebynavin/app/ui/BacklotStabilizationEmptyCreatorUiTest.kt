package com.framebynavin.app.ui

import android.graphics.Bitmap
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.framebynavin.app.data.HardeningTestEnvironment
import com.framebynavin.app.ui.theme.FrameByNavinTheme
import java.io.File
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/** First-run empty Creator is compact, but verified History remains accessible. */
@RunWith(AndroidJUnit4::class)
class BacklotStabilizationEmptyCreatorUiTest {
    @get:Rule val rule = createComposeRule()

    @Test fun zeroRemindersHideRedundantDashboardAndKeepHistoryAndActivity() {
        HardeningTestEnvironment.requireCiEmulator()
        rule.setContent {
            FrameByNavinTheme {
                V157ReminderCenterUi(
                    tasks = emptyList(),
                    onDismiss = {},
                    onNew = {},
                    onEdit = {},
                    onDeleteReminders = {},
                    onOpenReminderSettings = {},
                    onSnooze = null,
                    onPause = null,
                    onReschedule = null,
                    snoozeMinutes = 10,
                )
            }
        }
        rule.onNodeWithText("Nothing needs attention").assertExists()
        rule.onNodeWithTag("v219-search-input").assertDoesNotExist()
        rule.onNodeWithTag("v219-hub-filters").assertDoesNotExist()
        rule.onNodeWithText("NEEDS").assertDoesNotExist()
        rule.onNodeWithText("TODAY").assertDoesNotExist()
        rule.onNodeWithText("DELAYED").assertDoesNotExist()
        rule.onNodeWithTag("v307-reminder-permission-warning").assertDoesNotExist()
        rule.onNodeWithTag("v2193-history-tab").assertExists()
        rule.onNodeWithTag("v306-open-activity").assertExists()

        val app = InstrumentationRegistry.getInstrumentation().targetContext.applicationContext
        val dir = File(requireNotNull(app.getExternalFilesDir(null)), "stabilization-visual-audit")
        check(dir.isDirectory || dir.mkdirs())
        rule.waitForIdle()
        val shot = rule.onRoot().captureToImage().asAndroidBitmap()
        try {
            val file = File(dir, "01-first-run-minimal-creator.png")
            file.outputStream().use { out ->
                assertTrue(shot.compress(Bitmap.CompressFormat.PNG, 100, out))
            }
            check(file.length() > 10_000L)
        } finally { shot.recycle() }

        rule.onNodeWithTag("v2193-history-tab").performClick()
        rule.onNodeWithTag("v219-hub-filters").assertExists()
        rule.onNodeWithTag("v219-filter-all").performClick()
        rule.onNodeWithTag("v219-search-input").assertDoesNotExist()
        rule.onNodeWithTag("v306-open-activity").performClick()
        rule.onNodeWithTag("v306-activity-center").assertExists()
    }
}
