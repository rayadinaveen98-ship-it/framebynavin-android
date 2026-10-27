package com.framebynavin.app.reminders

import android.graphics.Bitmap
import android.os.SystemClock
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Text
import androidx.compose.ui.graphics.Color
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithContentDescription
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onFirst
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.platform.app.InstrumentationRegistry
import com.framebynavin.app.data.HardeningTestEnvironment
import com.framebynavin.app.ui.theme.FrameByNavinTheme
import java.io.File
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

/**
 * Screenshots are from the ACTUAL unified Canvas renderer, not AI concept art.
 * No live alarm, services, scheduled tasks, recording or backup are triggered.
 */
class V218UnifiedWaveUiTest {
    @get:Rule val rule = createComposeRule()

    @Test fun alarmClockFormsFromSameColoredWaveAndTapStillReveals() =
        captureAndCheck(ReminderCheckInKind.ALARM, false)

    @Test fun voiceMicFormsFromSameColoredWaveAndTapStillReveals() =
        captureAndCheck(ReminderCheckInKind.VOICE, false)

    @Test fun batterySaverAlarmRetainsWaveAndManualReveal() =
        captureAndCheck(ReminderCheckInKind.ALARM, true)

    @Test fun batterySaverVoiceRetainsWaveAndManualReveal() =
        captureAndCheck(ReminderCheckInKind.VOICE, true)

    @Test fun actualAlarmActionScreenRemainsReadableAboveWave() =
        actualControls(ReminderCheckInKind.ALARM)

    @Test fun actualVoiceActionScreenRetainsReplayAndSnooze() =
        actualControls(ReminderCheckInKind.VOICE)

    private fun actualControls(kind: ReminderCheckInKind) {
        HardeningTestEnvironment.requireCiEmulator()
        rule.setContent {
            FrameByNavinTheme {
                ReminderCheckInScreen(
                    kind = kind,
                    title = "Prepare the next film scene",
                    dueLabel = "Today",
                    stageLabel = "Pre-production",
                    suggestedAction = "Start the storyboard",
                    notes = "", snoozeMinutes = 10, stageCheckIn = false,
                    onStageDone = {}, onWorking = {}, onSnooze = {},
                    onPause = {}, onReschedule = {}, onDismiss = {},
                    onReplay = if (kind == ReminderCheckInKind.VOICE) ({}) else null,
                )
            }
        }
        val tap = rule.onAllNodesWithContentDescription("Show reminder controls")
        if (tap.fetchSemanticsNodes().isNotEmpty()) tap.onFirst().performClick()
        rule.onNodeWithTag("v200-primary-reminder-action").assertExists()
        rule.onNodeWithTag("v1952-ambient-particles").assertExists()
        capture("${kind.name.lowercase()}-actual-controls")
    }

    private fun captureAndCheck(kind: ReminderCheckInKind, lowPower: Boolean) {
        HardeningTestEnvironment.requireCiEmulator()
        rule.setContent {
            FrameByNavinTheme {
                var revealed by remember { mutableStateOf(false) }
                Box(Modifier.fillMaxSize()) {
                    V195ReminderParticleScene(
                        kind = kind, revealed = revealed, lowPower = lowPower,
                        onReveal = { revealed = true },
                        // Deterministic fully formed wave (seconds=2.5),
                        // even with Android globally disabling animators.
                        motionEnabled = false,
                    )
                    if (revealed) Text("ACTIONS READY", color = Color.White)
                }
            }
        }
        rule.onNodeWithTag("v1952-ambient-particles").assertExists()
        rule.onNodeWithText("ACTIONS READY").assertDoesNotExist()
        val kindName = kind.name.lowercase()
        val power = if (lowPower) "saver" else "normal"
        capture("$kindName-$power-intro")
        rule.onNodeWithContentDescription("Show reminder controls").performClick()
        rule.onNodeWithText("ACTIONS READY").assertExists()
        rule.onNodeWithTag("v1952-ambient-particles").assertExists()
        capture("$kindName-$power-revealed")
    }

    private fun capture(stage: String) {
        val instrumentation = InstrumentationRegistry.getInstrumentation()
        val profile = InstrumentationRegistry.getArguments()
            .getString("backlotVisualProfile") ?: "unspecified"
        val directory = File(instrumentation.targetContext.filesDir, "v218-wave-evidence")
        assertTrue(directory.isDirectory || directory.mkdirs())
        var bitmap: Bitmap? = null
        var valid = false
        for (attempt in 0..2) {
            rule.waitForIdle()
            instrumentation.waitForIdleSync()
            SystemClock.sleep(300L + attempt * 200L)
            val next = requireNotNull(instrumentation.uiAutomation.takeScreenshot())
            valid = hasRealWavePixels(next)
            if (valid || attempt == 2) { bitmap = next; break }
            next.recycle()
        }
        val image = requireNotNull(bitmap)
        val output = File(directory, "$profile-$stage.png")
        try {
            output.outputStream().use {
                assertTrue(image.compress(Bitmap.CompressFormat.PNG, 100, it))
            }
        } finally {
            image.recycle()
        }
        assertTrue(output.length() > 2_000)
        assertTrue("Blank/stalled wave screenshot: $output", valid)
    }

    private fun hasRealWavePixels(image: Bitmap): Boolean {
        var bright = 0
        val seen = HashSet<Int>()
        val dx = maxOf(4, image.width / 60)
        val dy = maxOf(4, image.height / 60)
        for (y in image.height / 10 until image.height * 9 / 10 step dy) {
            for (x in image.width / 10 until image.width * 9 / 10 step dx) {
                val rgb = image.getPixel(x, y) and 0xFFFFFF
                seen.add(rgb)
                val r = (rgb shr 16) and 255
                val g = (rgb shr 8) and 255
                val b = rgb and 255
                if (maxOf(r, g, b) > 52) bright++
            }
        }
        return seen.size > 75 && bright > 20
    }
}
