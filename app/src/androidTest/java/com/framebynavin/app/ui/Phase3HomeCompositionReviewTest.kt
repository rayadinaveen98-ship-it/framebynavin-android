package com.framebynavin.app.ui

import android.graphics.Bitmap
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.layout.findRootCoordinates
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInRoot
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.framebynavin.app.data.CreatorProfile
import com.framebynavin.app.data.CreatorTask
import com.framebynavin.app.data.HardeningTestEnvironment
import com.framebynavin.app.data.TaskStatus
import com.framebynavin.app.ui.theme.CinemaBlack
import com.framebynavin.app.ui.theme.FrameByNavinTheme
import java.io.File
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/** Real Home + real floating navigation, using read-only synthetic project fixtures. */
@RunWith(AndroidJUnit4::class)
class Phase3HomeCompositionReviewTest {
    @get:Rule val rule = createComposeRule()
    private val profile = CreatorProfile(
        displayName = "Demo Creator",
        primaryCreatorMode = "Filmmaker",
        platforms = setOf("YouTube"),
        primaryGoal = "Make an original short film",
        weeklyPublishingTarget = 2,
    )

    private fun project() = CreatorTask(
        id = "phase3e-mixed-script",
        title = "ఆలోచనలు · Why This Scene Works — Long English and Telugu cinema analysis",
        platform = "YouTube",
        contentType = "Long-form",
        dueLabel = "Today", // stale text must not be treated as a real deadline
        dueAtMillis = 0L,
        status = TaskStatus.WORKING,
    )

    private fun render(projects: List<CreatorTask>, fontScale: Float) {
        rule.setContent {
            val ambientDensity = LocalDensity.current
            CompositionLocalProvider(
                LocalDensity provides Density(ambientDensity.density, fontScale),
            ) {
                FrameByNavinTheme {
                    val density = LocalDensity.current.density
                    var clearance by remember {
                        mutableFloatStateOf(V197BottomNavigationClearance.MINIMUM_DP)
                    }
                    Box(Modifier.fillMaxSize().background(CinemaBlack)) {
                        PTodayScreen(
                            creatorProfile = profile,
                            tasks = projects,
                            onAdd = {}, onStart = {}, onAdvance = {},
                            onViewAllReminders = {}, onFocus = {},
                            bottomNavigationClearance = clearance.dp,
                        )
                        PBottomNav(
                            selected = PTab.TODAY,
                            onSelect = {}, onCapture = {},
                            modifier = Modifier.align(Alignment.BottomCenter)
                                .navigationBarsPadding()
                                .padding(horizontal = 16.dp, vertical = 12.dp)
                                .testTag("phase3e-floating-nav")
                                .onGloballyPositioned { coordinates ->
                                    val updated = V197BottomNavigationClearance.calculate(
                                        rootHeightPx = coordinates.findRootCoordinates().size.height,
                                        navigationTopPx = coordinates.positionInRoot().y,
                                        density = density,
                                    )
                                    if (updated != clearance) clearance = updated
                                },
                        )
                    }
                }
            }
        }
        rule.waitForIdle()
    }

    private fun assertScrollableHubAboveFloatingNavigation() {
        val hub = rule.onNodeWithTag("v219-reminder-home-entry")
        hub.performScrollTo()
        rule.waitForIdle()
        val navTop = rule.onNodeWithTag("phase3e-floating-nav").getUnclippedBoundsInRoot().top
        repeat(5) {
            if (hub.getUnclippedBoundsInRoot().bottom + 4.dp < navTop) return@repeat
            rule.onAllNodes(hasScrollAction()).onFirst().performTouchInput { swipeUp() }
            rule.waitForIdle()
        }
        hub.assertIsDisplayed()
        val actual = hub.getUnclippedBoundsInRoot()
        assertTrue("Reminder Hub overlaps navigation at this font scale: ${actual.bottom} >= $navTop",
            actual.bottom + 4.dp < navTop)
    }

    @Test fun emptyHomeAtLargeFontShowsHonestCreateAndReminderEntry() {
        HardeningTestEnvironment.requireCiEmulator()
        render(emptyList(), fontScale = 1.5f)
        rule.onNodeWithText("CREATE PROJECT").assertExists()
        rule.onNodeWithTag("phase3-home-continue").assertDoesNotExist()
        rule.onNodeWithTag("v215-project-entry").assertDoesNotExist()
        assertScrollableHubAboveFloatingNavigation()
        capture("empty-150")
    }

    @Test fun bilingualWorkingProjectAtDoubleFontRetainsPrimaryActionsAndHub() {
        HardeningTestEnvironment.requireCiEmulator()
        render(listOf(project()), fontScale = 2f)
        rule.onNodeWithTag("v215-project-entry").performScrollTo().assertExists()
        rule.onNodeWithTag("phase3-home-continue").performScrollTo().assertExists()
        rule.onNodeWithTag("phase3-home-focus").performScrollTo().assertExists()
        rule.onNodeWithText("START THIS MOVE", useUnmergedTree = true).assertDoesNotExist()
        rule.onNodeWithText("NO DEADLINE", useUnmergedTree = true).assertExists()
        assertScrollableHubAboveFloatingNavigation()
        capture("bilingual-200")
    }

    private fun capture(name: String) {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        rule.waitForIdle()
        val dir = File(context.filesDir, "phase3e-composition").apply { mkdirs() }
        val file = File(dir, "$name.png")
        file.outputStream().use { stream ->
            assertTrue(rule.onRoot().captureToImage().asAndroidBitmap()
                .compress(Bitmap.CompressFormat.PNG, 100, stream))
        }
        assertTrue(file.length() > 10_000L)
    }
}
