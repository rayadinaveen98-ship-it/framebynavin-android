package com.framebynavin.app.ui

import android.graphics.Bitmap
import android.graphics.Color
import android.os.SystemClock
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.unit.dp
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.framebynavin.app.data.HardeningTestEnvironment
import com.framebynavin.app.ui.theme.FrameByNavinTheme
import java.io.File
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/** 27 staged vector assets only; no live companion setting is touched. */
@RunWith(AndroidJUnit4::class)
class V2213PoseConceptGalleryUiTest {
    @get:Rule val rule = createComposeRule()

    private fun verify(identity: V144GuideIdentity) {
        HardeningTestEnvironment.requireCiEmulator()
        rule.setContent {
            FrameByNavinTheme {
                Surface(Modifier.fillMaxSize()) {
                    V2213PoseReviewGallery(
                        identity,
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 20.dp),
                    )
                }
            }
        }
        val tag = identity.name.lowercase()
        rule.onNodeWithTag("v2213-gallery-$tag").assertIsDisplayed()
        for (pose in V2213Pose.entries) {
            rule.onNodeWithTag("v2213-pose-$tag-${pose.name.lowercase()}")
                .assertIsDisplayed()
            val id = V2213PoseCatalog.conceptDrawable(identity, pose)
            val drawable = InstrumentationRegistry.getInstrumentation()
                .targetContext.getDrawable(id)
            assertTrue("Vector failed to inflate: $identity/$pose", drawable != null)
        }
        rule.waitForIdle()
        InstrumentationRegistry.getInstrumentation().waitForIdleSync()
        SystemClock.sleep(700)
        val bmp = requireNotNull(InstrumentationRegistry.getInstrumentation()
            .uiAutomation.takeScreenshot())
        try {
            val w = bmp.width
            val h = bmp.height
            var colorfulPixels = 0
            for (y in (h / 8) until (h * 4 / 5) step 9) {
                for (x in (w / 10) until (w * 9 / 10) step 9) {
                    val color = bmp.getPixel(x, y)
                    val hi = maxOf(Color.red(color), Color.green(color), Color.blue(color))
                    val lo = minOf(Color.red(color), Color.green(color), Color.blue(color))
                    if (hi - lo >= 32 && hi > 90) colorfulPixels++
                }
            }
            assertTrue("Blank/monochrome pose gallery: $identity, colorful=$colorfulPixels",
                colorfulPixels >= 35)
            val profile = InstrumentationRegistry.getArguments()
                .getString("backlotVisualProfile")
            check(profile == "default" || profile == "compact")
            val dir = File(InstrumentationRegistry.getInstrumentation()
                .targetContext.filesDir, "v2213-gallery")
            assertTrue(dir.isDirectory || dir.mkdirs())
            val f = File(dir, "v2213-$profile-$tag-nine-poses.png")
            f.outputStream().use { assertTrue(bmp.compress(Bitmap.CompressFormat.PNG, 100, it)) }
            assertTrue("Pose sheet unreasonably small", f.length() > 15_000)
        } finally { bmp.recycle() }
    }

    @Test fun frameNineIndependentlyDrawnConceptsDecodeAndRender() {
        verify(V144GuideIdentity.FRAME)
    }

    @Test fun naviNineIndependentlyDrawnConceptsDecodeAndRender() {
        verify(V144GuideIdentity.NAVI)
    }

    @Test fun cuteNineIndependentlyDrawnConceptsDecodeAndRender() {
        verify(V144GuideIdentity.CUTE)
    }

    @Test fun originalSparkStillUsesItsOriginalNonConceptArtwork() {
        HardeningTestEnvironment.requireCiEmulator()
        assertEquals(3, V2213PoseCatalog.reviewIdentities.size)
        assertTrue(V2213PoseCatalog.reviewIdentities.none {
            it == V144GuideIdentity.FUNNY
        })
    }
}
