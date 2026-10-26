package com.framebynavin.app.ui

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Color
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.framebynavin.app.ui.theme.CinemaBlack
import com.framebynavin.app.ui.theme.FrameByNavinTheme
import java.io.File
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * V212: real Android-emulator PIXELS from existing Home hero + bottom-nav
 * components, NOT an end-to-end screenshot of the whole Activity.
 *
 * All test media/creator names are synthetic. We never upload owner files.
 * Profiles are passed explicitly from the CI harness: default or compact.
 */
@RunWith(AndroidJUnit4::class)
class V212HomeNavVisualEvidenceUiTest {
    @get:Rule val rule = createComposeRule()
    private val context get() = InstrumentationRegistry.getInstrumentation().targetContext

    private fun capture(name: String) {
        val profile = InstrumentationRegistry.getArguments().getString("backlotVisualProfile")
        check(profile in setOf("default", "compact")) { "Explicit visual profile required" }
        val dir = File(context.filesDir, "home-nav-visual")
        check(dir.isDirectory || dir.mkdirs()) { "Cannot create synthetic visual evidence folder" }
        rule.waitForIdle()
        val bitmap = rule.onRoot().captureToImage().asAndroidBitmap()
        val output = File(dir, "backlot-$name-$profile.png")
        output.outputStream().use {
            check(bitmap.compress(Bitmap.CompressFormat.PNG, 100, it)) { "PNG failed" }
        }
        check(output.isFile && output.length() > 64L) { "Empty screenshot: $name" }
    }

    @Test fun emptyHomeHeroShowsGreetingWithoutReservedPhotoBanner() {
        rule.setContent {
            FrameByNavinTheme {
                Box(Modifier.fillMaxSize().background(CinemaBlack)) {
                    V18CinematicHomeHero("Creator")
                }
            }
        }
        rule.onNodeWithText("BACKLOT").assertIsDisplayed()
        rule.onNodeWithContentDescription("Add slideshow images").assertIsDisplayed()
        capture("home-empty")
    }

    @Test fun populatedHomeHeroShowsPhotoAndRetainsAccessibleManagement() {
        val prefs = context.getSharedPreferences("best_frames_original_prefs", Context.MODE_PRIVATE)
        val oldOrder = prefs.getString("frame_order", null)
        val folder = File(context.filesDir, "best_frames_original").apply { mkdirs() }
        val source = File(folder, "visual_qa_synthetic_${System.nanoTime()}.png")
        val pixels = Bitmap.createBitmap(400, 220, Bitmap.Config.ARGB_8888)
        try {
            pixels.eraseColor(Color.rgb(53, 89, 121))
            source.outputStream().use { check(pixels.compress(Bitmap.CompressFormat.PNG, 100, it)) }
            check(prefs.edit().putString("frame_order", source.name).commit())
            rule.setContent {
                FrameByNavinTheme {
                    Box(Modifier.fillMaxSize().background(CinemaBlack)) {
                        V18CinematicHomeHero("Creator", autoPlay = false)
                    }
                }
            }
            rule.onNodeWithContentDescription("Manage slideshow images").assertIsDisplayed()
            capture("home-photo")
        } finally {
            if (oldOrder == null) prefs.edit().remove("frame_order").commit()
            else prefs.edit().putString("frame_order", oldOrder).commit()
            source.delete()
            pixels.recycle()
        }
    }

    @Test fun bottomNavigationIconsAndLabelsFitWithLargeText() {
        rule.setContent {
            val density = LocalDensity.current
            CompositionLocalProvider(LocalDensity provides Density(density.density, 1.8f)) {
                MaterialTheme {
                    Box(Modifier.fillMaxSize().background(CinemaBlack)) {
                        PBottomNav(
                            selected = PTab.INSIGHTS,
                            onSelect = {},
                            onCapture = {},
                            modifier = Modifier.align(Alignment.BottomCenter)
                                .navigationBarsPadding()
                                .padding(horizontal = 16.dp, vertical = 12.dp)
                                .testTag("visual-bottom-nav"),
                        )
                    }
                }
            }
        }
        val root = rule.onRoot().getUnclippedBoundsInRoot()
        val bar = rule.onNodeWithTag("visual-bottom-nav").getUnclippedBoundsInRoot()
        assertTrue("Nav left clipped: $bar vs $root", bar.left >= root.left)
        assertTrue("Nav right clipped: $bar vs $root", bar.right <= root.right)
        assertTrue("Nav bottom clipped: $bar vs $root", bar.bottom <= root.bottom)
        for (label in listOf("Home", "Ideas", "Projects", "Insights")) {
            val node = rule.onNodeWithText(label, useUnmergedTree = true)
            node.assertIsDisplayed()
            val bounds = node.getUnclippedBoundsInRoot()
            assertTrue("$label clipped at nav edge: $bounds vs $bar",
                bounds.left >= bar.left && bounds.right <= bar.right)
            val lines = mutableListOf<TextLayoutResult>()
            val hasLayout = runCatching {
                node.fetchSemanticsNode().config[SemanticsActions.GetTextLayoutResult]
                    .action?.invoke(lines) == true
            }.getOrDefault(false)
            assertTrue("$label missing text-layout accessibility action",
                hasLayout && lines.isNotEmpty())
            assertTrue("$label wrapped across ${lines.single().lineCount} rows",
                lines.single().lineCount == 1)
        }
        rule.onNodeWithContentDescription("Create project").assertExists()
        capture("bottom-nav-largefont")
    }
}
