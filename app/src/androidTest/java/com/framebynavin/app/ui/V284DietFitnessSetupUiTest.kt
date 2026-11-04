package com.framebynavin.app.ui

import androidx.compose.ui.test.assertDoesNotExist
import androidx.compose.ui.test.assertExists
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.framebynavin.app.data.HardeningTestEnvironment
import com.framebynavin.app.personal.PersonalDietFitnessPlanV1
import com.framebynavin.app.ui.theme.FrameByNavinTheme
import java.time.LocalDate
import java.time.ZoneId
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/** Preview only, never confirms install or mutates actual Personal data. */
@RunWith(AndroidJUnit4::class)
class V284DietFitnessSetupUiTest {
    @get:Rule val rule = createComposeRule()

    @Test fun planCanBeReviewedAndCancelledWithoutCreatingAnything() {
        HardeningTestEnvironment.requireCiEmulator()
        rule.setContent {
            FrameByNavinTheme {
                V284DietFitnessSetupCard(reminders = emptyList(), enabled = true)
            }
        }
        rule.onNodeWithTag("v284-preview-plan").assertExists().performClick()
        rule.onNodeWithTag("v284-confirm-dialog").assertExists()
        rule.onNodeWithText("ADD REMINDERS").assertExists()
        rule.onNodeWithText("CANCEL").performClick()
        rule.onNodeWithTag("v284-confirm-dialog").assertDoesNotExist()
    }

    @Test fun previouslyInstalledPlanIsNotOfferedForDuplicateInstall() {
        HardeningTestEnvironment.requireCiEmulator()
        val zone = ZoneId.of("Asia/Kolkata")
        val existing = PersonalDietFitnessPlanV1.templates(LocalDate.of(2026, 11, 4), zone)
        rule.setContent {
            FrameByNavinTheme {
                V284DietFitnessSetupCard(reminders = existing, enabled = true)
            }
        }
        rule.onNodeWithTag("v284-plan-progress").assertExists()
        rule.onNodeWithTag("v284-preview-plan").assertDoesNotExist()
        rule.onNodeWithTag("v284-retry-delivery").assertExists()
    }
}
