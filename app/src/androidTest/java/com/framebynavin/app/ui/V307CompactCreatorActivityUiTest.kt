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

/**
 * Empty Creator reminder center should be visually calm. Synthetic emulator
 * only; no persisted reminder or acknowledgement gets created by this test.
 */
@RunWith(AndroidJUnit4::class)
class V307CompactCreatorActivityUiTest {
    @get:Rule val rule = createComposeRule()
    private val app get() =
        InstrumentationRegistry.getInstrumentation().targetContext.applicationContext

    @Test fun noEmptyResponseOrHistoryCardsAndActivityRemainsReachable() {
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
        rule.onNodeWithTag("v307-creator-compact-inbox").assertDoesNotExist()
        rule.onNodeWithText("NEEDS YOUR RESPONSE").assertDoesNotExist()
        rule.onNodeWithText("VIEW RECENT REMINDER ACTIVITY").assertDoesNotExist()
        rule.onNodeWithTag("v306-open-activity").assertExists()

        val dir = File(requireNotNull(app.getExternalFilesDir(null)), "v307-visual-audit")
        check(dir.isDirectory || dir.mkdirs())
        rule.waitForIdle()
        val bitmap = rule.onRoot().captureToImage().asAndroidBitmap()
        try {
            check(bitmap.width > 200 && bitmap.height > 200)
            File(dir, "01-creator-clean-empty.png").outputStream().use { out ->
                assertTrue(bitmap.compress(Bitmap.CompressFormat.PNG, 100, out))
            }
            check(File(dir, "01-creator-clean-empty.png").length() > 10_000L)
        } finally { bitmap.recycle() }

        rule.onNodeWithTag("v306-open-activity").performClick()
        rule.onNodeWithTag("v306-activity-center").assertExists()
        rule.onNodeWithTag("v306-filter-needs_action").assertExists()
    }
}
