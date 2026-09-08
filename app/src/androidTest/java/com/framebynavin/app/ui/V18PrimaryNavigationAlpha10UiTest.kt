package com.framebynavin.app.ui

import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class V18PrimaryNavigationAlpha10UiTest {

    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun primaryNavigationDispatchesEveryDestination() {
        var selected = PTab.TODAY
        composeRule.setContent {
            PBottomNav(selected = selected, onSelect = { selected = it })
        }

        listOf(
            "Ideas" to PTab.IDEAS,
            "Create" to PTab.CREATE,
            "Insights" to PTab.INSIGHTS,
            "Today" to PTab.TODAY,
        ).forEach { (label, expected) ->
            composeRule.onNodeWithText(label).performClick()
            composeRule.runOnIdle { assertEquals(expected, selected) }
        }
    }

    @Test
    fun ideasRenderAsFirstClassScreenWithoutBackAffordance() {
        composeRule.setContent {
            V09IdeaVaultScreen(
                ideas = emptyList(),
                onClose = null,
                onSave = { null },
                onDelete = {},
                onArchive = {},
                onConvert = { _, _, _, _ -> null },
            )
        }
        composeRule.onNodeWithText("Capture now. Produce later.").assertIsDisplayed()
        composeRule.onAllNodesWithContentDescription("Back").assertCountEquals(0)
    }

    @Test
    fun calendarRendersAsFirstClassScreenWithoutBackAffordance() {
        composeRule.setContent {
            V15ContentCalendarScreen(tasks = emptyList(), weeklySlots = emptyList(), onClose = null)
        }
        composeRule.onNodeWithText("The next 14 days").assertIsDisplayed()
        composeRule.onAllNodesWithContentDescription("Back").assertCountEquals(0)
    }

    @Test
    fun createScreenUsesCreatorNeutralProductLanguage() {
        composeRule.setContent {
            V131StudioScreen(
                tasks = emptyList(),
                onAdd = {},
                onAdvance = {},
                onBack = {},
                onFocus = {},
                onArchive = {},
                onUnarchive = {},
                onDelete = {},
            )
        }
        composeRule.onNodeWithText("CREATE").assertIsDisplayed()
        composeRule.onNodeWithText("Turn ideas into finished work.").assertIsDisplayed()
        composeRule.onNodeWithText("CREATE PROJECT").assertIsDisplayed()
    }
}
