package com.framebynavin.app.reminders

import android.graphics.Bitmap
import android.graphics.Color
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.test.captureToImage
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.framebynavin.app.data.HardeningTestEnvironment
import com.framebynavin.app.personal.PersonalAlert
import com.framebynavin.app.personal.PersonalReminder
import com.framebynavin.app.personal.V2233PersonalAlertScreen
import com.framebynavin.app.ui.theme.FrameByNavinTheme
import java.io.File
import java.time.LocalDate
import java.time.LocalTime
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/** Four REAL Compose screens captured on the emulator; no image overlays or
 * artificial reminder/action writes. Acceptance reference is the approved
 * Director's Cut screenshot, but this test also checks the preserved controls.
 */
@RunWith(AndroidJUnit4::class)
class V336DirectorReminderVisualUiTest {
    @get:Rule val rule = createComposeRule()

    @Test fun creatorVoiceVisualAndActionAvailability() =
        creator(ReminderCheckInKind.VOICE, "creator_voice")

    @Test fun creatorAlarmVisualAndActionAvailability() =
        creator(ReminderCheckInKind.ALARM, "creator_alarm")

    @Test fun personalVoiceVisualAndActionAvailability() =
        personal(PersonalAlert.VOICE, "personal_voice")

    @Test fun personalAlarmVisualAndActionAvailability() =
        personal(PersonalAlert.ALARM, "personal_alarm")

    private fun creator(kind: ReminderCheckInKind, name: String) {
        HardeningTestEnvironment.requireCiEmulator()
        rule.setContent {
            FrameByNavinTheme {
                ReminderCheckInScreen(
                    kind = kind, title = "Finish the film sequence",
                    dueLabel = "Today at 10:30 AM", stageLabel = "Editing",
                    suggestedAction = "Review the final cut", notes = "Synthetic UI test",
                    snoozeMinutes = 10, stageCheckIn = false,
                    onStageDone = {}, onWorking = {}, onSnooze = {},
                    onPause = {}, onReschedule = {}, onDismiss = {},
                    onReplay = if (kind == ReminderCheckInKind.VOICE) ({}) else null,
                )
            }
        }
        val intro = rule.onAllNodesWithContentDescription("Show reminder controls")
        if (intro.fetchSemanticsNodes().isNotEmpty()) intro[0].performClick()
        rule.waitForIdle()
        rule.onNodeWithTag("v200-reminder-title").assertExists()
        rule.onNodeWithTag("v200-primary-reminder-action").assertExists()
        rule.onNodeWithTag("v336-director-signal-art").assertExists()
        rule.onNodeWithTag("v336-creator-reminder-context").assertExists()
        capture(name)
    }

    private fun personal(alert: PersonalAlert, name: String) {
        HardeningTestEnvironment.requireCiEmulator()
        val fixture = PersonalReminder(
            id = "v336:visual:${alert.name.lowercase()}",
            title = "Time for your evening routine",
            startDate = LocalDate.now(),
            time = LocalTime.of(18, 30),
            alert = alert,
        ).copy(
            pendingOccurrenceMillis = System.currentTimeMillis() + 600_000L,
        )
        rule.setContent {
            FrameByNavinTheme {
                V2233PersonalAlertScreen(
                    reminder = fixture, invalid = false, busy = false, error = null,
                    onDone = {}, onSnooze = {}, onDismiss = {}, onClose = {},
                )
            }
        }
        rule.waitForIdle()
        rule.onNodeWithTag("v2233-personal-alert-title").assertExists()
        rule.onNodeWithTag("v2233-personal-done").assertExists()
        rule.onNodeWithTag("v2233-personal-snooze").assertExists()
        rule.onNodeWithTag("v2233-personal-dismiss").assertExists()
        rule.onNodeWithTag("v336-personal-reminder-status").assertExists()
        capture(name)
    }

    private fun capture(name: String) {
        val bitmap = rule.onRoot().captureToImage().asAndroidBitmap()
        val dir = requireNotNull(
            InstrumentationRegistry.getInstrumentation().targetContext.getExternalFilesDir(null)
        )
        val file = File(dir, "v336_${name}.png")
        file.outputStream().use { out ->
            assertTrue(bitmap.compress(Bitmap.CompressFormat.PNG, 100, out))
        }
        // Do not accept a black/empty scene as a successful visual test.
        var warmPixels = 0
        for (y in 0 until bitmap.height step 13) {
            for (x in 0 until bitmap.width step 13) {
                val c = bitmap.getPixel(x, y)
                if (Color.red(c) > 120 && Color.red(c) > Color.green(c) * 1.30 &&
                    Color.red(c) > Color.blue(c) * 1.08) warmPixels++
            }
        }
        assertTrue("Cinematic crimson art not visible in screenshot: $name", warmPixels > 30)
    }
}
