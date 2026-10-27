package com.framebynavin.app.ui

import android.graphics.Bitmap
import android.os.SystemClock
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
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
import com.framebynavin.app.R
import com.framebynavin.app.ui.theme.FrameByNavinTheme
import java.io.File
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/** V217 alpha-art previews and existing Cute pose map on synthetic fixtures only. */
@RunWith(AndroidJUnit4::class)
class V217CompanionConceptUiTest {
    @get:Rule val rule = createComposeRule()

    /** The old non-welcome Cute WebPs include corrupted/deformed frames.
     *  Do not silently enable them without replacement asset approval. */
    @Test fun existingCuteRemainsOnValidatedUndistortedMaster() {
        for (state in CinePulseState.entries) {
            assertEquals("Cute must stay visually safe at $state",
                R.drawable.guide_cute_welcome,
                v144GuideDrawable(V144GuideIdentity.CUTE, state))
        }
    }

    @Test fun sparkOriginalPoseMappingAndIdentityRemainUntouched() {
        val spark = V144GuideIdentity.FUNNY
        assertEquals("Spark", spark.displayName)
        assertEquals(R.drawable.guide_funny_welcome, v217ConceptDrawable(spark))
        val expected = mapOf(
            CinePulseState.IDLE to R.drawable.guide_funny_welcome,
            CinePulseState.WALK to R.drawable.guide_funny_point,
            CinePulseState.POINT to R.drawable.guide_funny_point,
            CinePulseState.PRESENT to R.drawable.guide_funny_present,
            CinePulseState.LOOK to R.drawable.guide_funny_welcome,
            CinePulseState.NOD to R.drawable.guide_funny_point,
            CinePulseState.WAVE to R.drawable.guide_funny_welcome,
            CinePulseState.THINK to R.drawable.guide_funny_thinking,
            CinePulseState.LISTEN to R.drawable.guide_funny_welcome,
            CinePulseState.SUCCESS to R.drawable.guide_funny_celebrate,
            CinePulseState.CELEBRATE to R.drawable.guide_funny_celebrate,
            CinePulseState.REST to R.drawable.guide_funny_welcome,
        )
        for ((state, drawable) in expected) {
            assertEquals("Unmodified Spark pose for $state", drawable, v144GuideDrawable(spark, state))
        }
        assertEquals(listOf("FRAME", "NAVI", "FUNNY", "CUTE"),
            V144GuideIdentity.entries.map { it.name })
    }

    @Test fun transparentLookdevCutoutsDecodeInActualComposeView() {
        val set = listOf(V144GuideIdentity.FRAME, V144GuideIdentity.NAVI, V144GuideIdentity.CUTE)
        assertEquals(3, set.map(::v217ConceptDrawable).toSet().size)
        for (guide in set) {
            assertNotEquals(R.drawable.guide_funny_welcome, v217ConceptDrawable(guide))
        }
        rule.setContent {
            FrameByNavinTheme {
                Column(
                    Modifier.fillMaxSize()
                        .background(Color(0xFF131821))
                        .verticalScroll(rememberScrollState())
                        .padding(10.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    for (guide in set) {
                        Text(guide.displayName, color = Color.White)
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            V217CompanionConceptPreview(
                                guide, Modifier.size(48.dp).testTag("v217-${guide.name}-48"),
                            )
                            V217CompanionConceptPreview(
                                guide, Modifier.size(96.dp).testTag("v217-${guide.name}-96"),
                            )
                            V217CompanionConceptPreview(
                                guide, Modifier.size(160.dp).testTag("v217-${guide.name}-160"),
                            )
                        }
                    }
                }
            }
        }
        rule.waitForIdle()
        captureVisual("top")
        for (guide in set) {
            for (dp in listOf(48, 96, 160)) {
                rule.onNodeWithTag("v217-${guide.name}-${dp}").performScrollTo().assertExists()
            }
        }
        captureVisual("bottom")
    }

    /**
     * Real Spark must appear beside real existing Frame/Navi/Cute and the three
     * staged concept cutouts. Synthetic, screen-test only: no preference mutation.
     */
    @Test fun compareOriginalGuidesToStagedCutoutsAtAppSizes() {
        val guides = listOf(
            V144GuideIdentity.FUNNY,
            V144GuideIdentity.FRAME,
            V144GuideIdentity.NAVI,
            V144GuideIdentity.CUTE,
        )
        rule.setContent {
            FrameByNavinTheme {
                Column(
                    Modifier.fillMaxSize()
                        .background(Color(0xFF141923))
                        .verticalScroll(rememberScrollState())
                        .padding(12.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    for (guide in guides) {
                        Text(guide.displayName + " · Existing / V217 study", color = Color.White)
                        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                            V144GuideCharacter(
                                guide = guide,
                                state = CinePulseState.IDLE,
                                modifier = Modifier.size(96.dp).testTag("v217-current-${guide.name}"),
                            )
                            V217CompanionConceptPreview(
                                identity = guide,
                                modifier = Modifier.size(96.dp).testTag("v217-study-${guide.name}"),
                            )
                            V217CompanionConceptPreview(
                                identity = guide,
                                modifier = Modifier.size(48.dp).testTag("v217-study-small-${guide.name}"),
                            )
                        }
                    }
                }
            }
        }
        rule.waitForIdle()
        rule.onNodeWithTag("v217-current-FUNNY").assertExists()
        rule.onNodeWithTag("v217-study-FUNNY").assertExists()
        captureVisual("compare-top")
        for (guide in guides) {
            rule.onNodeWithTag("v217-study-small-${guide.name}").performScrollTo().assertExists()
            rule.onNodeWithTag("v217-current-${guide.name}").performScrollTo().assertExists()
        }
        captureVisual("compare-bottom")
    }

    private fun captureVisual(position: String) {
        rule.waitForIdle()
        val instrument = InstrumentationRegistry.getInstrumentation()
        val profile = InstrumentationRegistry.getArguments().getString("backlotVisualProfile") ?: "unspecified"
        // Compose test rule can report idle before the Android SurfaceFlinger layer
        // has presented its first frame. Real V217 top screenshots were black even
        // though UI assertions passed. Capture and CHECK rendered pixels, retrying
        // a bounded number of times, rather than accepting only a valid PNG header.
        var rendered = false
        var photo: Bitmap? = null
        for (attempt in 0..2) {
            instrument.waitForIdleSync()
            SystemClock.sleep(350L + attempt * 200L)
            val candidate = requireNotNull(instrument.uiAutomation.takeScreenshot()) {
                "Device screenshot unavailable at attempt $attempt"
            }
            rendered = containsActualCharacterPixels(candidate)
            if (rendered || attempt == 2) {
                photo = candidate
                break
            }
            candidate.recycle()
        }

        val bitmap = requireNotNull(photo)
        val dir = File(instrument.targetContext.filesDir, "v217-concept-visual")
        assertTrue("Cannot make screenshot directory", dir.isDirectory || dir.mkdirs())
        val output = File(dir, "v217-${profile}-${position}.png")
        try {
            output.outputStream().use { assertTrue(bitmap.compress(Bitmap.CompressFormat.PNG, 100, it)) }
        } finally {
            bitmap.recycle()
        }
        assertTrue("Empty screenshot", output.length() > 256)
        assertTrue("Screenshot is a blank Android window despite a green Compose test: $output", rendered)
    }

    /**
     * Pixel acceptance is intentionally independent of the screenshot PNG header
     * and status/nav bar: the actual character content is bright and colorful,
     * unlike the dark stage. This rejects the black top captures from the first
     * otherwise-green V217 emulator run.
     */
    private fun containsActualCharacterPixels(bitmap: Bitmap): Boolean {
        val colors = HashSet<Int>()
        var colorful = 0
        val dx = maxOf(4, bitmap.width / 64)
        val dy = maxOf(4, bitmap.height / 64)
        val x0 = bitmap.width / 12
        val x1 = bitmap.width * 11 / 12
        val y0 = bitmap.height / 12
        val y1 = bitmap.height * 11 / 12
        for (y in y0 until y1 step dy) {
            for (x in x0 until x1 step dx) {
                val rgb = bitmap.getPixel(x, y) and 0x00FFFFFF
                colors.add(rgb)
                val r = (rgb shr 16) and 0xFF
                val g = (rgb shr 8) and 0xFF
                val b = rgb and 0xFF
                if (maxOf(r, g, b) > 110) colorful++
            }
        }
        return colors.size >= 32 && colorful >= 28
    }
}
