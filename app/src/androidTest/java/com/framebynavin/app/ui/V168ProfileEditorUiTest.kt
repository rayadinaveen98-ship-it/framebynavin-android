package com.framebynavin.app.ui

import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.framebynavin.app.data.CreatorProfile
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class V168ProfileEditorUiTest {
    @get:Rule val rule = createComposeRule()
    private val profile = CreatorProfile(
        displayName = "Local filmmaker", category = "Film & Entertainment",
        primaryCreatorMode = "Film & Entertainment",
        platforms = setOf("YouTube"), productionStyles = setOf("Voiceover"),
        primaryGoal = "Improve content quality", weeklyPublishingTarget = 2,
    )

    @Test fun cancelMakesNoSaveRequestAndDoesNotOpenOnboarding() {
        var saved = 0
        var cancelled = 0
        rule.setContent {
            V168CreatorProfileEditor(
                profile = profile,
                onCancel = { cancelled++ },
                onSave = { saved++; true },
            )
        }
        rule.onNodeWithText("CANCEL").performClick()
        rule.runOnIdle {
            assertEquals(1, cancelled)
            assertEquals(0, saved)
            assertEquals("Film & Entertainment", profile.resolvedPrimaryCreatorMode)
        }
    }

    @Test fun editorCanSaveProfileWithoutTouchingOriginal() {
        var saved: CreatorProfile? = null
        rule.setContent {
            V168CreatorProfileEditor(profile, onCancel = {}, onSave = { saved = it; true })
        }
        rule.onNodeWithText("Instagram").performClick()
        rule.onNodeWithText("SAVE CHANGES").performClick()
        rule.runOnIdle {
            assertTrue(saved?.platforms?.contains("Instagram") == true)
            assertEquals(setOf("YouTube"), profile.platforms)
            assertEquals("Local filmmaker", saved?.displayName)
        }
    }

    @Test fun profileEntryOpensNewEditorAndNeverTriggersLegacyOnboarding() {
        var onboardingOpens = 0
        var saved = 0
        rule.setContent {
            V23ProfileAccountScreen(
                creatorProfile = profile,
                onClose = {},
                onEditCreatorSetup = { onboardingOpens++ },
                onOpenYouTube = {},
                onSaveCreatorProfile = { saved++; true },
            )
        }
        rule.onNodeWithText("EDIT CREATOR DETAILS").performClick()
        rule.onNodeWithText("Edit creator details").assertExists()
        rule.runOnIdle { assertEquals(0, onboardingOpens) }
        rule.onNodeWithText("SAVE CHANGES").performClick()
        rule.runOnIdle {
            assertEquals(0, onboardingOpens)
            assertEquals(1, saved)
        }
    }
}
