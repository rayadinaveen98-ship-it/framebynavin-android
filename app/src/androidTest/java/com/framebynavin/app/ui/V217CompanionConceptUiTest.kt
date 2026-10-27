package com.framebynavin.app.ui

import android.graphics.Bitmap
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
import androidx.compose.ui.test.assertExists
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

    @Test fun existingCuteUsesAllFiveOriginalPoseFiles() {
        val expected = mapOf(
            CinePulseState.IDLE to R.drawable.guide_cute_welcome,
            CinePulseState.WALK to R.drawable.guide_cute_encourage,
            CinePulseState.POINT to R.drawable.guide_cute_encourage,
            CinePulseState.PRESENT to R.drawable.guide_cute_encourage,
            CinePulseState.LOOK to R.drawable.guide_cute_welcome,
            CinePulseState.NOD to R.drawable.guide_cute_listening,
            CinePulseState.WAVE to R.drawable.guide_cute_welcome,
            CinePulseState.THINK to R.drawable.guide_cute_thinking,
            CinePulseState.LISTEN to R.drawable.guide_cute_listening,
            CinePulseState.SUCCESS to R.drawable.guide_cute_celebrate,
            CinePulseState.CELEBRATE to R.drawable.guide_cute_celebrate,
            CinePulseState.REST to R.drawable.guide_cute_welcome,
        )
        assertEquals(CinePulseState.entries.toSet(), expected.keys)
        for ((state, drawable) in expected) {
            assertEquals("Cute pose for $state", drawable, v144GuideDrawable(V144GuideIdentity.CUTE, state))
        }
        assertEquals(5, expected.values.toSet().size)
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
        for (guide in set) {
            for (dp in listOf(48, 96, 160)) {
                rule.onNodeWithTag("v217-${guide.name}-${dp}").performScrollTo().assertExists()
            }
        }
        val instrument = InstrumentationRegistry.getInstrumentation()
        val profile = InstrumentationRegistry.getArguments().getString("backlotVisualProfile") ?: "unspecified"
        val photo = requireNotNull(instrument.uiAutomation.takeScreenshot()) { "Device capture unavailable" }
        val dir = File(instrument.targetContext.filesDir, "v217-concept-visual")
        assertTrue("Cannot make screenshot directory", dir.isDirectory || dir.mkdirs())
        val output = File(dir, "v217-${profile}-concept.png")
        try {
            output.outputStream().use { assertTrue(photo.compress(Bitmap.CompressFormat.PNG, 100, it)) }
        } finally {
            photo.recycle()
        }
        assertTrue("Empty screenshot", output.length() > 256)
    }
}
