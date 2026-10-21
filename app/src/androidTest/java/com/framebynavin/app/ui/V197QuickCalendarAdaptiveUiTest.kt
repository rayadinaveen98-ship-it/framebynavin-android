package com.framebynavin.app.ui

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import com.framebynavin.app.data.CreatorRewardsV22Engine
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

/** V197.5 preserves shortcut handlers and calendar navigation across compact layouts. */
class V197QuickCalendarAdaptiveUiTest {
    @get:Rule val rule = createComposeRule()

    @Test fun largeTextQuickActionsStacksPrimaryCardsWithoutChangingDestinations() {
        val projects = mutableIntStateOf(0)
        val captured = mutableIntStateOf(0)
        val calendar = mutableIntStateOf(0)
        rule.setContent {
            val density = LocalDensity.current
            CompositionLocalProvider(LocalDensity provides Density(density.density, 1.7f)) {
                MaterialTheme {
                    V161QuickActionsHub(
                        tasks = emptyList(), ideas = emptyList(),
                        weeklyAutoPlanEnabled = false,
                        creatorProgress = CreatorRewardsV22Engine.snapshot(emptyList()),
                        onNewProject = { projects.intValue++ },
                        onQuickCapture = { captured.intValue++ },
                        onDailyBrief = {}, onCalendar = { calendar.intValue++ },
                        onRelease = {}, onIdeas = {}, onWeek = {}, onReminders = {},
                        onAutomation = {}, onCreatorProgress = {}, onSettings = {},
                        onPublishLate = {}, onReschedule = {}, onSkip = {},
                    )
                }
            }
        }
        val projectCard = rule.onNodeWithTag("quick-action-new-project").getUnclippedBoundsInRoot()
        val captureCard = rule.onNodeWithTag("quick-action-quick-capture").getUnclippedBoundsInRoot()
        val root = rule.onRoot().getUnclippedBoundsInRoot()
        assertTrue("Primary cards should stack at large text size", projectCard.bottom <= captureCard.top)
        assertTrue("Primary cards clipped horizontally", projectCard.right <= root.right && captureCard.right <= root.right)
        rule.onNodeWithTag("quick-action-new-project").performClick()
        rule.onNodeWithTag("quick-action-quick-capture").performClick()
        rule.onNodeWithTag("quick-action-content-calendar").performScrollTo().performClick()
        rule.runOnIdle {
            assertEquals(1, projects.intValue)
            assertEquals(1, captured.intValue)
            assertEquals(1, calendar.intValue)
        }
    }

    @Test fun accessibleWeeklyPlanActionsStackButPreservePreviewAndAdd() {
        val preview = mutableIntStateOf(0)
        val add = mutableIntStateOf(0)
        rule.setContent {
            val density = LocalDensity.current
            CompositionLocalProvider(LocalDensity provides Density(density.density, 1.7f)) {
                MaterialTheme {
                    V197WeeklyPlanActions(
                        previewCount = 2, previewEnabled = true,
                        onPreview = { preview.intValue++ },
                        onAdd = { add.intValue++ },
                    )
                }
            }
        }
        val p = rule.onNodeWithTag("v164-create-preview").getUnclippedBoundsInRoot()
        val a = rule.onNodeWithTag("v197-add-weekly-post").getUnclippedBoundsInRoot()
        assertTrue("Weekly Plan buttons should stack", p.bottom <= a.top)
        rule.onNodeWithTag("v164-create-preview").performClick()
        rule.onNodeWithTag("v197-add-weekly-post").performClick()
        rule.runOnIdle {
            assertEquals(1, preview.intValue)
            assertEquals(1, add.intValue)
        }
    }

    @Test fun calendarOpenedAsFullScreenOverlayCanCloseAndShowLastContent() {
        val closes = mutableIntStateOf(0)
        rule.setContent {
            MaterialTheme {
                V15ContentCalendarScreen(
                    tasks = emptyList(),
                    weeklySlots = emptyList(),
                    onClose = { closes.intValue++ },
                    bottomNavigationClearance = 36.dp,
                )
            }
        }
        rule.onNodeWithText("All dates").assertIsDisplayed()
        rule.onNodeWithText("Calendar is clear").assertIsDisplayed()
        rule.onNodeWithContentDescription("Back").performClick()
        rule.runOnIdle { assertEquals(1, closes.intValue) }
    }
}
