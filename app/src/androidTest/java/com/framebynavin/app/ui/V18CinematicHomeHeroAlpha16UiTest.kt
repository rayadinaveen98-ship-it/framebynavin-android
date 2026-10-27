package com.framebynavin.app.ui

import android.content.Context
import android.graphics.Bitmap
import androidx.test.platform.app.InstrumentationRegistry
import java.io.File
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class V18CinematicHomeHeroAlpha16UiTest {
    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun emptyHeroKeepsGreetingAndOffersImageAddAction() {
        composeRule.setContent { V18CinematicHomeHero("King") }

        composeRule.onNodeWithText("BACKLOT").assertIsDisplayed()
        composeRule.onNode(hasText("King", substring = true)).assertIsDisplayed()
        composeRule.onNodeWithContentDescription("Add slideshow images").assertIsDisplayed()
    }

    @Test
    fun photoHeaderOffersAccessibleManageActionWithExistingImages() {
        var managementTaps = 0
        composeRule.setContent {
            V18HeroGreetingBar(
                greeting = "Good Morning, King",
                hasFrames = true,
                importing = false,
                onPhotoClick = { managementTaps += 1 },
            )
        }

        composeRule.onAllNodesWithText("Good Morning, King").assertCountEquals(1)
        composeRule.onNodeWithContentDescription("Manage slideshow images").assertIsDisplayed()
        composeRule.onNodeWithContentDescription("Manage slideshow images").performClick()
        composeRule.runOnIdle { check(managementTaps == 1) }
    }

    @Test
    fun emptyHeaderOffersImageAddActionBesideGreeting() {
        var addTaps = 0
        composeRule.setContent {
            V18HeroGreetingBar(
                greeting = "Good Night, King",
                hasFrames = false,
                importing = false,
                onPhotoClick = { addTaps += 1 },
            )
        }

        composeRule.onNodeWithText("BACKLOT").assertIsDisplayed()
        composeRule.onNodeWithContentDescription("Add slideshow images").assertIsDisplayed()
        composeRule.onNodeWithContentDescription("Manage slideshow images").assertDoesNotExist()
        composeRule.onNodeWithContentDescription("Add slideshow images").performClick()
        composeRule.runOnIdle { check(addTaps == 1) }
    }

    @Test
    fun populatedSlideshowShowsSingleGreetingAndOpensExistingManagement() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val prefs = context.getSharedPreferences("best_frames_original_prefs", Context.MODE_PRIVATE)
        val previousOrder = prefs.getString("frame_order", null)
        val dir = File(context.filesDir, "best_frames_original").apply { mkdirs() }
        val photo = File(dir, "home_header_ui_${System.nanoTime()}.png")
        val bitmap = Bitmap.createBitmap(32, 32, Bitmap.Config.ARGB_8888)
        try {
            photo.outputStream().use { bitmap.compress(Bitmap.CompressFormat.PNG, 100, it) }
            check(prefs.edit().putString("frame_order", photo.name).commit())

            composeRule.setContent { V18CinematicHomeHero("King") }

            composeRule.onAllNodesWithText("King", substring = true).assertCountEquals(1)
            composeRule.onNodeWithContentDescription("Manage slideshow images").assertIsDisplayed()
            composeRule.onNodeWithContentDescription("Manage slideshow images").performClick()
            composeRule.onNodeWithContentDescription("Change slideshow images").assertIsDisplayed()
        } finally {
            if (previousOrder == null) prefs.edit().remove("frame_order").commit()
            else prefs.edit().putString("frame_order", previousOrder).commit()
            photo.delete()
            bitmap.recycle()
        }
    }

    @Test
    fun slideshowEditorReordersAndRemovesWithoutDeletingOriginalFiles() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val prefs = context.getSharedPreferences("best_frames_original_prefs", Context.MODE_PRIVATE)
        val previousOrder = prefs.getString("frame_order", null)
        val directory = File(context.filesDir, "best_frames_original").apply { mkdirs() }
        val first = File(directory, "home_editor_first_${System.nanoTime()}.png")
        val second = File(directory, "home_editor_second_${System.nanoTime()}.png")
        val bitmap = Bitmap.createBitmap(36, 36, Bitmap.Config.ARGB_8888)
        try {
            first.outputStream().use { check(bitmap.compress(Bitmap.CompressFormat.PNG, 100, it)) }
            second.outputStream().use { check(bitmap.compress(Bitmap.CompressFormat.PNG, 100, it)) }
            check(prefs.edit().putString("frame_order", "${first.name}\n${second.name}").commit())

            composeRule.setContent { V18CinematicHomeHero("King", autoPlay = false) }
            composeRule.onNodeWithContentDescription("Manage slideshow images").performClick()
            composeRule.onNodeWithContentDescription("Edit slideshow order").performClick()
            composeRule.onNodeWithContentDescription("Move photo 1 down").performClick()
            val expectedMove = "${second.name}\n${first.name}"
            val moveResult = runCatching {
                composeRule.waitUntil(25_000) {
                    prefs.getString("frame_order", "") == expectedMove
                }
            }
            check(moveResult.isSuccess) {
                "Home reorder did not persist: expected=$expectedMove, " +
                    "actual=${prefs.getString("frame_order", "")}, " +
                    "originalsPresent=${first.isFile && second.isFile}"
            }
            check(first.isFile && second.isFile)
            // The preference change commits on IO before the Compose coroutine
            // has necessarily cleared editingPhotos and re-enabled actions.
            composeRule.waitUntil(10_000) {
                runCatching {
                    composeRule.onNodeWithContentDescription("Remove photo 1").assertIsEnabled()
                    true
                }.getOrDefault(false)
            }
            composeRule.onNodeWithContentDescription("Remove photo 1").performClick()
            composeRule.onNodeWithText("REMOVE PHOTO").performClick()
            composeRule.waitUntil(15_000) { prefs.getString("frame_order", "") == first.name }
            check(first.isFile && second.isFile)
            composeRule.waitUntil(10_000) {
                runCatching {
                    composeRule.onNodeWithContentDescription("Remove photo 1").assertIsEnabled()
                    true
                }.getOrDefault(false)
            }
            composeRule.onNodeWithContentDescription("Remove photo 1").performClick()
            composeRule.onNodeWithText("REMOVE PHOTO").performClick()
            composeRule.waitUntil(15_000) { prefs.getString("frame_order", "")?.isEmpty() == true }
            composeRule.onNodeWithText("DONE").performClick()
            composeRule.onNodeWithContentDescription("Add slideshow images").assertIsDisplayed()
            check(first.isFile && second.isFile)
        } finally {
            if (previousOrder == null) prefs.edit().remove("frame_order").commit()
            else prefs.edit().putString("frame_order", previousOrder).commit()
            first.delete()
            second.delete()
            bitmap.recycle()
        }
    }

    @Test
    fun missingOriginalCannotBeSilentlyDroppedWhenRemovingPhoto() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val prefs = context.getSharedPreferences("best_frames_original_prefs", Context.MODE_PRIVATE)
        val previousOrder = prefs.getString("frame_order", null)
        val directory = File(context.filesDir, "best_frames_original").apply { mkdirs() }
        val photo = File(directory, "home_editor_present_${System.nanoTime()}.png")
        val missing = "home_editor_missing_${System.nanoTime()}.png"
        val bitmap = Bitmap.createBitmap(36, 36, Bitmap.Config.ARGB_8888)
        val expected = "${photo.name}\n$missing"
        try {
            photo.outputStream().use { check(bitmap.compress(Bitmap.CompressFormat.PNG, 100, it)) }
            check(prefs.edit().putString("frame_order", expected).commit())
            composeRule.setContent { V18CinematicHomeHero("King") }
            composeRule.onNodeWithContentDescription("Manage slideshow images").performClick()
            composeRule.onNodeWithContentDescription("Edit slideshow order").performClick()
            composeRule.onNodeWithContentDescription("Remove photo 1").performClick()
            composeRule.onNodeWithText("REMOVE PHOTO").performClick()
            composeRule.waitUntil(15_000) {
                composeRule.onAllNodesWithText("Slideshow unchanged").fetchSemanticsNodes().isNotEmpty()
            }
            check(prefs.getString("frame_order", "") == expected)
            check(photo.isFile)
        } finally {
            if (previousOrder == null) prefs.edit().remove("frame_order").commit()
            else prefs.edit().putString("frame_order", previousOrder).commit()
            photo.delete()
            bitmap.recycle()
        }
    }

    @Test
    fun settingsLaunchOpensPopulatedEditorOnceAndDoesNotReopenAfterDone() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val prefs = context.getSharedPreferences("best_frames_original_prefs", Context.MODE_PRIVATE)
        val previousOrder = prefs.getString("frame_order", null)
        val dir = File(context.filesDir, "best_frames_original").apply { mkdirs() }
        val photo = File(dir, "home_settings_open_${System.nanoTime()}.png")
        val bitmap = Bitmap.createBitmap(32, 32, Bitmap.Config.ARGB_8888)
        var handled = 0
        try {
            photo.outputStream().use { check(bitmap.compress(Bitmap.CompressFormat.PNG, 100, it)) }
            check(prefs.edit().putString("frame_order", photo.name).commit())
            composeRule.setContent {
                V18CinematicHomeHero(
                    creatorName = "King",
                    openEditorNonce = 1L,
                    onEditorRequestHandled = { handled += 1 },
                )
            }
            composeRule.onNodeWithText("Slideshow photos").assertIsDisplayed()
            composeRule.runOnIdle { check(handled == 1) }
            composeRule.onNodeWithText("DONE").performClick()
            composeRule.onNodeWithText("Slideshow photos").assertDoesNotExist()
            composeRule.runOnIdle { check(handled == 1) }
        } finally {
            if (previousOrder == null) prefs.edit().remove("frame_order").commit()
            else prefs.edit().putString("frame_order", previousOrder).commit()
            photo.delete()
            bitmap.recycle()
        }
    }

    @Test
    fun slideshowPausePersistsWithoutRemovingPhotos() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val prefs = context.getSharedPreferences("best_frames_original_prefs", Context.MODE_PRIVATE)
        val previousOrder = prefs.getString("frame_order", null)
        val previousPaused = if (prefs.contains("home_slideshow_paused"))
            prefs.getBoolean("home_slideshow_paused", false) else null
        val directory = File(context.filesDir, "best_frames_original").apply { mkdirs() }
        val first = File(directory, "pause_first_${System.nanoTime()}.png")
        val second = File(directory, "pause_second_${System.nanoTime()}.png")
        val bitmap = Bitmap.createBitmap(36, 36, Bitmap.Config.ARGB_8888)
        try {
            first.outputStream().use { check(bitmap.compress(Bitmap.CompressFormat.PNG, 100, it)) }
            second.outputStream().use { check(bitmap.compress(Bitmap.CompressFormat.PNG, 100, it)) }
            check(prefs.edit().putString("frame_order", "${first.name}\n${second.name}")
                .putBoolean("home_slideshow_paused", false).commit())
            composeRule.setContent { V18CinematicHomeHero("King", autoPlay = false) }
            composeRule.onNodeWithContentDescription("Pause slideshow").assertIsDisplayed().performClick()
            composeRule.onNodeWithContentDescription("Play slideshow").assertIsDisplayed()
            check(prefs.getBoolean("home_slideshow_paused", false))
            composeRule.onNodeWithContentDescription("Play slideshow").performClick()
            composeRule.onNodeWithContentDescription("Pause slideshow").assertIsDisplayed()
            check(!prefs.getBoolean("home_slideshow_paused", true))
            check(first.isFile && second.isFile)
        } finally {
            val editor = prefs.edit()
            if (previousOrder == null) editor.remove("frame_order") else editor.putString("frame_order", previousOrder)
            if (previousPaused == null) editor.remove("home_slideshow_paused")
            else editor.putBoolean("home_slideshow_paused", previousPaused)
            check(editor.commit())
            first.delete()
            second.delete()
            bitmap.recycle()
        }
    }

    @Test
    fun playbackPolicyRespectsPauseAccessibilityAndEditing() {
        fun policy(paused: Boolean, animations: Boolean, editing: Boolean, count: Int) =
            V18HeroPlaybackPolicy.shouldAnimate(
                autoPlay = true,
                userPaused = paused,
                animationsEnabled = animations,
                isEditing = editing,
                photoCount = count,
            )
        check(policy(false, true, false, 2))
        check(!policy(true, true, false, 2))
        check(!policy(false, false, false, 2))
        check(!policy(false, true, true, 2))
        check(!policy(false, true, false, 1))
        check(!V18HeroPlaybackPolicy.shouldAnimate(false, false, true, false, 2))
    }

    @Test
    fun photoEditorShowsAddControlWhenThereIsCapacity() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val prefs = context.getSharedPreferences("best_frames_original_prefs", Context.MODE_PRIVATE)
        val previousOrder = prefs.getString("frame_order", null)
        val directory = File(context.filesDir, "best_frames_original").apply { mkdirs() }
        val photo = File(directory, "append_control_${System.nanoTime()}.png")
        val bitmap = Bitmap.createBitmap(32, 32, Bitmap.Config.ARGB_8888)
        try {
            photo.outputStream().use { check(bitmap.compress(Bitmap.CompressFormat.PNG, 100, it)) }
            check(prefs.edit().putString("frame_order", photo.name).commit())
            composeRule.setContent { V18CinematicHomeHero("King", autoPlay = false) }
            composeRule.onNodeWithContentDescription("Manage slideshow images").performClick()
            composeRule.onNodeWithContentDescription("Edit slideshow order").performClick()
            composeRule.onNodeWithText("ADD PHOTOS").assertIsDisplayed()
            composeRule.onNodeWithText("DONE").performClick()
            composeRule.onNodeWithText("Slideshow photos").assertDoesNotExist()
        } finally {
            if (previousOrder == null) prefs.edit().remove("frame_order").commit()
            else prefs.edit().putString("frame_order", previousOrder).commit()
            photo.delete()
            bitmap.recycle()
        }
    }

    @Test
    fun expandedHeroManagementUsesIconActions() {
        composeRule.setContent {
            V18HeroManageActions(
                expanded = true,
                onToggle = {},
                onChange = {},
                onRemove = {},
            )
        }

        composeRule.onNodeWithContentDescription("Change slideshow images").assertIsDisplayed()
        composeRule.onNodeWithContentDescription("Remove slideshow images").assertIsDisplayed()
        composeRule.onNodeWithContentDescription("Slideshow options").assertIsDisplayed()
    }
}
