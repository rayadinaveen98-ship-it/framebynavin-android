package com.framebynavin.app.reminders

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.framebynavin.app.data.HardeningTestEnvironment
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Run only on a disposable CI emulator: do not interact with installed
 * owner apps, personal media, cloud state or reminder scheduling.
 */
@RunWith(AndroidJUnit4::class)
class BacklotV1RecoveryAppearanceUiTest {
    @get:Rule val rule = createComposeRule()

    @Test fun oldReminderSceneCollectionsAreAbsentAndAllNewMainsExist() {
        HardeningTestEnvironment.requireCiEmulator()
        rule.setContent {
            Column(Modifier.verticalScroll(rememberScrollState())) {
                RX02ReminderAppearanceSettings()
            }
        }
        rule.onNodeWithTag("rx02-open-older-looks").assertDoesNotExist()
        rule.onNodeWithTag("v391-scene-settings").assertDoesNotExist()
        rule.onNodeWithText("MAIN SCREEN & OLDER LOOKS").assertDoesNotExist()
        rule.onNodeWithText("CINEMATIC COLLECTION").assertDoesNotExist()
        rule.onNodeWithText("CLASSIC COLLECTION").assertDoesNotExist()

        rule.onNodeWithText("Velvet Sanctuary").assertExists()
        rule.onNodeWithTag("rx02-domain-creator").performClick()
        rule.onNodeWithText("Studio Nocturne").assertExists()
        rule.onNodeWithTag("rx02-domain-challenge_21").performClick()
        rule.onNodeWithText("Ember Command").assertExists()

        // The existing Spark/Mira opt-in survives the removal of old Settings.
        rule.onNodeWithTag("backlot-reminder-companion-toggle").assertExists()
    }

    @Test fun modernMainPreviewIsClearlyNonInteractive() {
        HardeningTestEnvironment.requireCiEmulator()
        rule.setContent {
            Column(Modifier.verticalScroll(rememberScrollState())) {
                RX02ReminderAppearanceSettings()
            }
        }
        rule.onNodeWithTag("backlot-preview-modern-main")
            .performScrollTo().performClick()
        rule.onNodeWithTag("backlot-main-background-preview").assertExists()
        rule.onNodeWithText("MAIN REMINDER PREVIEW").assertExists()
        rule.onNodeWithText("DONE").assertDoesNotExist()
        rule.onNodeWithText("SNOOZE").assertDoesNotExist()
        rule.onNodeWithTag("backlot-close-main-preview").performClick()
        rule.onNodeWithTag("backlot-main-background-preview").assertDoesNotExist()
    }
}
