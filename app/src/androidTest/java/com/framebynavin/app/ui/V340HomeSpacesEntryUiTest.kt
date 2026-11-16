package com.framebynavin.app.ui

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.framebynavin.app.personal.spaces.V289SpaceEntity
import com.framebynavin.app.ui.theme.FrameByNavinTheme
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Isolated, synthetic UI acceptance. No Room file, Personal reminder, journal
 * or private photo is opened or written by these tests.
 */
@RunWith(AndroidJUnit4::class)
class V340HomeSpacesEntryUiTest {
    @get:Rule val rule = createComposeRule()

    private fun space(id: String, title: String, order: Int, archived: Boolean = false) =
        V289SpaceEntity(
            id = id, title = title, type = "READING",
            iconKey = "book-open", goalText = "", createdAtMillis = 1_000L,
            archivedAtMillis = if (archived) 2_000L else null, displayOrder = order,
        )

    @Test fun activeChipsOpenExactlySelectedSpaceAndHeaderOpensOverview() {
        val visited = mutableListOf<String?>()
        rule.setContent {
            FrameByNavinTheme {
                V340HomeSpacesCard(
                    spaces = listOf(
                        space("second", "Skin Care", 2),
                        space("archived", "Old Notes", 0, archived = true),
                        space("first", "Reading", 1),
                    ),
                    loaded = true,
                    unavailable = false,
                    onOpenSpaces = { visited += it },
                )
            }
        }

        rule.onNodeWithTag("v340-home-spaces-entry").assertIsDisplayed()
        rule.onNodeWithText("MY SPACES").assertIsDisplayed()
        rule.onNodeWithText("2 active · 1 archived").assertIsDisplayed()
        rule.onNodeWithTag("v340-home-space-archived").assertDoesNotExist()

        rule.onNodeWithTag("v340-home-space-first").assertIsDisplayed().performClick()
        rule.onNodeWithTag("v340-home-space-second").assertIsDisplayed().performClick()
        rule.onNodeWithTag("v340-home-spaces-open").assertIsDisplayed().performClick()
        rule.runOnIdle {
            assertEquals(listOf("first", "second", null), visited)
        }
    }

    @Test fun noSpaceYetShowsRealEmptyStateAndCanOpenTheExistingScreen() {
        val visits = mutableListOf<String?>()
        rule.setContent {
            FrameByNavinTheme {
                V340HomeSpacesCard(
                    spaces = emptyList(),
                    loaded = true,
                    unavailable = false,
                    onOpenSpaces = { visits += it },
                )
            }
        }
        rule.onNodeWithTag("v340-home-spaces-empty").assertIsDisplayed()
        rule.onNodeWithTag("v340-home-spaces-open").performClick()
        rule.runOnIdle { assertEquals(listOf<String?>(null), visits) }
    }

    @Test fun unreadableSpacesAreNotMisrepresentedAsZeroSavedSpaces() {
        rule.setContent {
            FrameByNavinTheme {
                V340HomeSpacesCard(
                    spaces = emptyList(),
                    loaded = true, unavailable = true,
                    onOpenSpaces = {},
                )
            }
        }
        rule.onNodeWithTag("v340-home-spaces-unavailable").assertIsDisplayed()
        rule.onNodeWithTag("v340-home-spaces-empty").assertDoesNotExist()
    }

    @Test fun loadingDoesNotFlashAnUntrueEmptyDatabaseState() {
        rule.setContent {
            FrameByNavinTheme {
                V340HomeSpacesCard(
                    spaces = emptyList(),
                    loaded = false, unavailable = false,
                    onOpenSpaces = {},
                )
            }
        }
        rule.onNodeWithText("Opening saved Spaces…").assertIsDisplayed()
        rule.onNodeWithTag("v340-home-spaces-empty").assertDoesNotExist()
    }
}
