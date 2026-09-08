package com.framebynavin.app.ui

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.hasText
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

        composeRule.onNodeWithText("FRAME BY NAVIN").assertIsDisplayed()
        composeRule.onNode(hasText("King", substring = true)).assertIsDisplayed()
        composeRule.onNodeWithContentDescription("Add slideshow images").assertIsDisplayed()
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
