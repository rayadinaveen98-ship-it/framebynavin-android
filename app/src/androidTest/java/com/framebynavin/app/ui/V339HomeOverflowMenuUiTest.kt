package com.framebynavin.app.ui

import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.unit.dp
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.framebynavin.app.ui.theme.FrameByNavinTheme
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Synthetic Compose acceptance: no real reminders, companion data,
 * photo picker, images, accounts or OS notification delivery are changed.
 */
@RunWith(AndroidJUnit4::class)
class V339HomeOverflowMenuUiTest {
    @get:Rule val rule = createComposeRule()

    @Test fun homeHasOne48DpTriggerAndNoDuplicateGalleryButton() {
        var notificationOpens = 0
        var sparkOpens = 0
        var galleryOpens = 0
        rule.setContent {
            FrameByNavinTheme {
                V18HeroGreetingBar(
                    greeting = "Good morning, Creator",
                    hasFrames = true,
                    importing = false,
                    onPhotoClick = { throw AssertionError("Old Gallery should be hidden") },
                    galleryInOverflow = true,
                    companionAvatar = {
                        V339HomeOverflowMenu(
                            reminderActionNeeded = true,
                            sparkEnabled = true,
                            sparkSuggestionAvailable = false,
                            onOpenNotifications = { notificationOpens++ },
                            onOpenSpark = { sparkOpens++ },
                            onOpenGallery = { galleryOpens++ },
                        )
                    },
                )
            }
        }
        val trigger = rule.onNodeWithTag("v339-home-overflow-trigger")
        trigger.assertIsDisplayed()
        val bounds = trigger.getUnclippedBoundsInRoot()
        assertTrue("Three-dot trigger is narrower than 48dp: $bounds", bounds.right - bounds.left >= 48.dp)
        assertTrue("Three-dot trigger is shorter than 48dp: $bounds", bounds.bottom - bounds.top >= 48.dp)
        rule.onNodeWithTag("v2221-home-gallery").assertDoesNotExist()
        rule.onNodeWithTag("v339-home-action-indicator", useUnmergedTree = true).assertExists()

        trigger.performClick()
        rule.onNodeWithTag("v339-home-menu-NOTIFICATION_CENTER").assertIsDisplayed()
        rule.onNodeWithTag("v339-home-menu-SPARK").assertIsDisplayed()
        rule.onNodeWithTag("v339-home-menu-GALLERY").assertIsDisplayed()
        rule.onNodeWithTag("v339-home-menu-GALLERY").performClick()
        rule.onNodeWithTag("v339-home-overflow-list").assertDoesNotExist()
        rule.runOnIdle {
            assertEquals(1, galleryOpens)
            assertEquals(0, notificationOpens)
            assertEquals(0, sparkOpens)
        }

        trigger.performClick()
        rule.onNodeWithTag("v339-home-menu-NOTIFICATION_CENTER").performClick()
        trigger.performClick()
        rule.onNodeWithTag("v339-home-menu-SPARK").performClick()
        rule.runOnIdle {
            assertEquals(1, galleryOpens)
            assertEquals(1, notificationOpens)
            assertEquals(1, sparkOpens)
        }
    }

    @Test fun disabledSparkDoesNotBlockInboxOrGallery() {
        var notifications = 0
        var gallery = 0
        rule.setContent {
            FrameByNavinTheme {
                V339HomeOverflowMenu(
                    reminderActionNeeded = false,
                    sparkEnabled = false,
                    sparkSuggestionAvailable = true,
                    onOpenNotifications = { notifications++ },
                    onOpenSpark = { throw AssertionError("Spark disabled by owner") },
                    onOpenGallery = { gallery++ },
                )
            }
        }
        rule.onNodeWithTag("v339-home-overflow-trigger").assertIsDisplayed().performClick()
        rule.onNodeWithTag("v339-home-menu-SPARK").assertIsNotEnabled()
        rule.onNodeWithTag("v339-home-action-indicator", useUnmergedTree = true)
            .assertDoesNotExist()
        rule.onNodeWithTag("v339-home-menu-NOTIFICATION_CENTER").performClick()
        rule.onNodeWithTag("v339-home-overflow-trigger").performClick()
        rule.onNodeWithTag("v339-home-menu-GALLERY").performClick()
        rule.runOnIdle {
            assertEquals(1, notifications)
            assertEquals(1, gallery)
        }
    }

    @Test fun closedMenuRetainsGoldSparkCueAndReadableMeaning() {
        rule.setContent {
            FrameByNavinTheme {
                V339HomeOverflowMenu(
                    reminderActionNeeded = false,
                    sparkEnabled = true,
                    sparkSuggestionAvailable = true,
                    onOpenNotifications = {},
                    onOpenSpark = {},
                    onOpenGallery = {},
                )
            }
        }
        rule.onNodeWithTag("v339-home-spark-indicator", useUnmergedTree = true)
            .assertExists()
        rule.onNodeWithTag("v339-home-action-indicator", useUnmergedTree = true)
            .assertDoesNotExist()
        rule.onNodeWithTag("v339-home-overflow-trigger")
            .assert(
                SemanticsMatcher.expectValue(
                    androidx.compose.ui.semantics.SemanticsProperties.StateDescription,
                    "Collapsed; a Spark suggestion is available",
                )
            )
    }

    @Test fun collapsedMenuHidesTheThreeActionRows() {
        rule.setContent {
            FrameByNavinTheme {
                V339HomeOverflowMenu(
                    reminderActionNeeded = false,
                    sparkEnabled = true,
                    sparkSuggestionAvailable = false,
                    onOpenNotifications = {},
                    onOpenSpark = {},
                    onOpenGallery = {},
                )
            }
        }
        rule.onNodeWithTag("v339-home-overflow-list").assertDoesNotExist()
        rule.onNodeWithTag("v339-home-overflow-trigger").performClick()
        rule.onNodeWithTag("v339-home-overflow-list").assertExists()
        rule.onNodeWithTag("v339-home-overflow-trigger").performClick()
        rule.onNodeWithTag("v339-home-overflow-list").assertDoesNotExist()
    }
}
