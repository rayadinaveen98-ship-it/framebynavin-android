package com.framebynavin.app.ui

import android.graphics.Bitmap
import android.os.SystemClock
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.getUnclippedBoundsInRoot
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.unit.dp
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.framebynavin.app.ui.theme.FrameByNavinTheme
import java.io.File
import org.junit.Assert.assertTrue
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/** Genuine production first-run and Settings companion UI, synthetic emulator only.
 *  No clearing app data, no logged-in accounts and no editing creator choices.
 */
@RunWith(AndroidJUnit4::class)
class V217LiveGuideScreensUiTest {
    @get:Rule val rule = createComposeRule()

    @Test fun actualFirstRunShowsOnlySparkAndItsRealSelection() {
        val selected = mutableListOf<V144GuideIdentity>()
        rule.setContent {
            FrameByNavinTheme {
                V217GuideChoiceContent(onSelect = { selected.add(it) })
            }
        }
        rule.waitForIdle()
        rule.onNodeWithTag("v217-choice-FUNNY").assertIsDisplayed()
        rule.onNodeWithTag("v217-choose-button-FUNNY").assertIsDisplayed()
        for (unused in listOf("FRAME", "NAVI", "CUTE")) {
            rule.onNodeWithTag("v217-choice-$unused").assertDoesNotExist()
        }
        capture("first-run-spark")
        rule.onNodeWithTag("v217-choose-button-FUNNY").performClick()
        assertEquals(listOf(V144GuideIdentity.FUNNY), selected)
    }

    private fun assertAligned(first: String, second: String) {
        val a = rule.onNodeWithTag("v217-choice-$first").getUnclippedBoundsInRoot()
        val b = rule.onNodeWithTag("v217-choice-$second").getUnclippedBoundsInRoot()
        assertTrue("Card tops misaligned: $first=$a, $second=$b",
            a.top <= b.top + 1.dp && b.top <= a.top + 1.dp)
        assertTrue("Card bottoms differ: $first=$a, $second=$b",
            a.bottom <= b.bottom + 1.dp && b.bottom <= a.bottom + 1.dp)
        val buttonA = rule.onNodeWithTag("v217-choose-button-$first").getUnclippedBoundsInRoot()
        val buttonB = rule.onNodeWithTag("v217-choose-button-$second").getUnclippedBoundsInRoot()
        assertTrue("Button baselines differ: $first=$buttonA, $second=$buttonB",
            buttonA.bottom <= buttonB.bottom + 1.dp && buttonB.bottom <= buttonA.bottom + 1.dp)
    }

    @Test fun realSettingsGuidePickerShowsStaged3DArtwork() {
        rule.setContent {
            FrameByNavinTheme {
                Column(
                    Modifier.fillMaxSize()
                        .background(Color(0xFF0F1219))
                        .statusBarsPadding()
                        .navigationBarsPadding()
                        .verticalScroll(rememberScrollState())
                        .padding(16.dp),
                ) {
                    // Actual settings section reused by the Backlot Settings UI.
                    V144GuidePicker()
                }
            }
        }
        rule.waitForIdle()
        rule.onNodeWithTag("v217-settings-FUNNY").assertIsDisplayed()
        for (unused in listOf("FRAME", "NAVI", "CUTE")) {
            rule.onNodeWithTag("v217-settings-$unused").assertDoesNotExist()
        }
        capture("settings-spark")
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
            realPixels = hasArtwork(screenshot, name)
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

    /** A single Spark card has far fewer bright pixels than the old four-card
     * chooser. Keep the old strict overview check for onboarding. For Settings
     * require *chromatic artwork in the center*, not just colorful card borders.
     * Thresholds were measured from real API35 screenshots (not mock images).
     */
    private fun hasArtwork(bitmap: Bitmap, scene: String): Boolean {
        val colors = HashSet<Int>()
        var bright = 0
        var centerChromatic = 0
        val dx = maxOf(4, bitmap.width / 64)
        val dy = maxOf(4, bitmap.height / 64)
        for (y in bitmap.height / 12 until bitmap.height * 11 / 12 step dy) {
            for (x in bitmap.width / 12 until bitmap.width * 11 / 12 step dx) {
                val rgb = bitmap.getPixel(x, y) and 0xFFFFFF
                val r = (rgb shr 16) and 255
                val g = (rgb shr 8) and 255
                val b = rgb and 255
                val high = maxOf(r, g, b)
                val low = minOf(r, g, b)
                colors.add(rgb)
                if (high > 110) bright++
                if (x in bitmap.width / 4 until bitmap.width * 3 / 4 &&
                    y < bitmap.height / 2 && high > 75 && high - low >= 20
                ) centerChromatic++
            }
        }
        return if (scene.startsWith("settings-")) {
            colors.size >= 32 && bright >= 6 && centerChromatic >= 7
        } else colors.size >= 32 && bright >= 28
    }
}
