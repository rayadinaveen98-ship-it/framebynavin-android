package com.framebynavin.app.ui

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.framebynavin.app.data.CreatorIdea
import com.framebynavin.app.data.IdeaStatus
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class V18CreatorJourneyAlpha12UiTest {
    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun ideaConversionUsesCreateLanguageAndCompletesCallback() {
        var converted = false
        composeRule.setContent {
            V09IdeaVaultScreen(
                ideas = listOf(
                    CreatorIdea(
                        id = "idea-1",
                        title = "A creator idea",
                        status = IdeaStatus.READY_TO_PRODUCE,
                        platformHint = "YouTube",
                        formatHint = "Long-form",
                    ),
                ),
                onClose = null,
                onSave = { null },
                onDelete = {},
                onArchive = {},
                onConvert = { _, _, _, _ ->
                    converted = true
                    "project-1"
                },
            )
        }

        composeRule.onNodeWithText("TURN INTO PROJECT").performClick()
        composeRule.onNodeWithText("CREATE PROJECT").assertIsDisplayed()
        composeRule.onNodeWithText("The project will open in Create with the right workflow and reminder timing for its format.").assertIsDisplayed()
        composeRule.onNodeWithText("CREATE PROJECT").performClick()
        composeRule.runOnIdle { assertTrue(converted) }
    }
}
