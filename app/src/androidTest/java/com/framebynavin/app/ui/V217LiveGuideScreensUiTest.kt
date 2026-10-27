package com.framebynavin.app.ui

import android.graphics.Bitmap
import android.os.SystemClock
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.unit.dp
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.framebynavin.app.ui.theme.FrameByNavinTheme
import java.io.File
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/** Genuine production first-run and Settings companion UI, synthetic emulator only.
 *  No clearing app data, no logged-in accounts and no editing creator choices.
 */
@RunWith(AndroidJUnit4::class)
class V217LiveGuideScreensUiTest {
    @get:Rule val rule = createComposeRule()

    @Test fun actualFirstRunCompanionChoicesShowStaged3DArtwork() {
        rule.setContent {
            FrameByNavinTheme {
                // V217GuideChoiceContent is used by the REAL V144GuideChoiceGate,
                // not a hand-built imitation for screenshot purposes.
                V217GuideChoiceContent(onSelect = {})
            }
        }
        rule.waitForIdle()
        for (guide in V144GuideIdentity.entries) {
            rule.onNodeWithTag("v217-choice-${guide.name}").assertExists()
        }
        capture("first-run-top")
        rule.onNodeWithTag("v217-choice-CUTE").performScrollTo()
        rule.waitForIdle()
        capture("first-run-bottom")
    }

    @Test fun realSettingsGuidePickerShowsStaged3DArtwork() {
        rule.setContent {
            FrameByNavinTheme {
                Column(
                    Modifier.fillMaxSize()
                        .background(Color(0xFF0F1219))
                        .verticalScroll(rememberScrollState())
                        .padding(16.dp),
                ) {
                    // Actual settings section reused by the Backlot Settings UI.
                    V144GuidePicker()
                }
            }
        }
        rule.waitForIdle()
        for (guide in V144GuideIdentity.entries) {
            rule.onNodeWithTag("v217-settings-${guide.name}").assertExists()
        }
        capture("settings-top")
        rule.onNodeWithTag("v217-settings-CUTE").performScrollTo()
        rule.waitForIdle()
        capture("settings-bottom")
    }

    private fun capture(name: String) {
        val instrument = InstrumentationRegistry.getInstrumentation()
        val profile = InstrumentationRegistry.getArguments().getString("backlotVisualProfile")
        check(profile == "default" || profile == "compact") { "Explicit Android viewport required" }
        var image: Bitmap? = null
        var realPixels = false
        for (attempt in 0..2) {
            rule.waitForIdle()
            instrument.waitForIdleSync()
            SystemClock.sleep(500L + attempt * 250L)
            val screenshot = requireNotNull(instrument.uiAutomation.takeScreenshot())
            realPixels = hasArtwork(screenshot)
            if (realPixels || attempt == 2) {
                image = screenshot
                break
            }
            screenshot.recycle()
        }
        val bitmap = requireNotNull(image)
        val folder = File(instrument.targetContext.filesDir, "v217-live-screens")
        assertTrue(folder.isDirectory || folder.mkdirs())
        val output = File(folder, "v217-${profile}-${name}.png")
        try {
            output.outputStream().use { assertTrue(bitmap.compress(Bitmap.CompressFormat.PNG, 100, it)) }
        } finally {
            bitmap.recycle()
        }
        assertTrue(output.length() > 256)
        assertTrue("Blank Android capture instead of real live UI: $output", realPixels)
    }

    private fun hasArtwork(bitmap: Bitmap): Boolean {
        val colors = HashSet<Int>()
        var bright = 0
        val dx = maxOf(4, bitmap.width / 64)
        val dy = maxOf(4, bitmap.height / 64)
        for (y in bitmap.height / 12 until bitmap.height * 11 / 12 step dy) {
            for (x in bitmap.width / 12 until bitmap.width * 11 / 12 step dx) {
                val rgb = bitmap.getPixel(x, y) and 0xFFFFFF
                colors.add(rgb)
                if (maxOf((rgb shr 16) and 255, (rgb shr 8) and 255, rgb and 255) > 110) bright++
            }
        }
        return colors.size >= 32 && bright >= 28
    }
}
