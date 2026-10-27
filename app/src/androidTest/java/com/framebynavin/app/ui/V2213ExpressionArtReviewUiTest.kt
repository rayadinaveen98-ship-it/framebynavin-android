package com.framebynavin.app.ui

import android.graphics.Bitmap
import android.graphics.Canvas
import android.os.SystemClock
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.framebynavin.app.data.HardeningTestEnvironment
import com.framebynavin.app.ui.theme.FrameByNavinTheme
import java.io.File
import java.security.MessageDigest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/** Review-only actual Android rendering of 27 independently drawn vectors. */
@RunWith(AndroidJUnit4::class)
class V2213ExpressionArtReviewUiTest {
    @get:Rule val rule = createComposeRule()

    private fun capture(guide: V144GuideIdentity, scene: String) {
        rule.waitForIdle()
        InstrumentationRegistry.getInstrumentation().waitForIdleSync()
        SystemClock.sleep(350)
        val i = InstrumentationRegistry.getInstrumentation()
        val profile = InstrumentationRegistry.getArguments().getString("backlotVisualProfile")
        require(profile == "default" || profile == "compact")
        val folder = File(i.targetContext.filesDir, "v2213-art-review")
        assertTrue(folder.isDirectory || folder.mkdirs())
        val file = File(folder, "v2213-${profile}-${guide.name.lowercase()}-${scene}.png")
        // uiAutomation sometimes captures the launch/black surface before
        // Android has presented the first real Compose frame, even after
        // semantics report the gallery as displayed (observed in CI PR).
        // Retry only that transient capture; a persistently blank window
        // MUST still fail this visual gate, never be recorded as success.
        for (attempt in 1..5) {
            val bitmap = requireNotNull(i.uiAutomation.takeScreenshot())
            try {
                file.outputStream().use {
                    assertTrue(bitmap.compress(Bitmap.CompressFormat.PNG,100,it))
                }
            } finally { bitmap.recycle() }
            if (file.length() > 24_000) break
            if (attempt < 5) {
                SystemClock.sleep(450)
                i.waitForIdleSync()
            }
        }
        assertTrue("Blank V221.3 artwork after real-frame retries: $file", file.length() > 24_000)
    }

    private fun review(guide: V144GuideIdentity) {
        HardeningTestEnvironment.requireCiEmulator()
        rule.setContent {
            FrameByNavinTheme { V2213ExpressionArtReview(guide) }
        }
        rule.onNodeWithTag("v2213-review-${guide.name}").assertIsDisplayed()
        rule.onNodeWithTag("v2213-original").assertIsDisplayed()
        rule.onNodeWithTag("v2213-pose-art-${guide.name}-IDLE").assertIsDisplayed()
        rule.onNodeWithTag("v2213-pose-art-${guide.name}-WELCOME").assertIsDisplayed()
        capture(guide,"first-fold")
        rule.onNodeWithTag("v2213-pose-art-${guide.name}-REST").performScrollTo()
        rule.onNodeWithTag("v2213-pose-art-${guide.name}-REST").assertIsDisplayed()
        rule.onNodeWithTag("v2213-pose-art-${guide.name}-CELEBRATE").assertExists()
        capture(guide,"last-fold")
    }

    @Test fun frameHasNineReviewStatesAndOriginalReference() = review(V144GuideIdentity.FRAME)
    @Test fun naviHasNineReviewStatesAndOriginalReference() = review(V144GuideIdentity.NAVI)
    @Test fun cuteHasNineReviewStatesAndOriginalReference() = review(V144GuideIdentity.CUTE)

    @Test fun allNinePerGuideRasterizeDifferentlyAndWithVisiblePixels() {
        HardeningTestEnvironment.requireCiEmulator()
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val allDigests = mutableSetOf<String>()
        for (guide in V2213PoseCatalog.studiedGuides) {
            val current = mutableSetOf<String>()
            for (pose in V2213Pose.entries) {
                val id = requireNotNull(V2213PoseCatalog.drawable(guide,pose))
                val drawable = requireNotNull(context.getDrawable(id)).mutate()
                val bitmap = Bitmap.createBitmap(256,256,Bitmap.Config.ARGB_8888)
                try {
                    drawable.setBounds(0,0,256,256)
                    drawable.draw(Canvas(bitmap))
                    val pixels = IntArray(256*256)
                    bitmap.getPixels(pixels,0,256,0,0,256,256)
                    assertTrue("Empty pose for $guide/$pose", pixels.count { it ushr 24 > 80 } > 2500)
                    val digest = MessageDigest.getInstance("SHA-256")
                    val bb = java.nio.ByteBuffer.allocate(pixels.size * 4)
                    pixels.forEach { bb.putInt(it) }
                    val hash = digest.digest(bb.array()).joinToString("") { "%02x".format(it) }
                    assertTrue("Duplicate pose art for $guide/$pose", current.add(hash))
                    assertTrue("Accidental cross-guide duplicate for $guide/$pose", allDigests.add(hash))
                } finally { bitmap.recycle() }
            }
            assertEquals("Nine truly distinct rendered poses required for $guide",9,current.size)
        }
        assertEquals(27,allDigests.size)
        // Real artist approval is still a separate gate; these are vector studies.
    }
}
