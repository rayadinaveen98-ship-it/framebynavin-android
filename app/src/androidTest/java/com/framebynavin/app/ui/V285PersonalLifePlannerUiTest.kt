package com.framebynavin.app.ui

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

/** Never confirms the installer; UI tests cannot alter Personal data. */
@RunWith(AndroidJUnit4::class)
class V285PersonalLifePlannerUiTest {
    @get:Rule val rule = createComposeRule()

    @Test fun dateSwitchAndCancelDoNotInstallRoutines() {
        HardeningTestEnvironment.requireCiEmulator()
        rule.setContent {
            FrameByNavinTheme {
                V285PersonalLifePlannerCard(reminders = emptyList(), enabled = true)
            }
        }
        rule.onNodeWithTag("v285-personal-life-planner").assertExists()
        rule.onNodeWithTag("v285-day-2026-11-07").performClick()
        rule.onNodeWithText("NOV 7 · FIRST UPLOAD").assertExists()
        rule.onNodeWithTag("v285-install-routines").performClick()
        rule.onNodeWithTag("v285-install-dialog").assertExists()
        rule.onNodeWithText("CANCEL").performClick()
        rule.onNodeWithTag("v285-install-dialog").assertDoesNotExist()
    }

    @Test fun bedtimeCollisionIsNotSilentlyFixed() {
        HardeningTestEnvironment.requireCiEmulator()
        val existing = PersonalDietFitnessPlanV1.templates(
            LocalDate.of(2026, 11, 5), ZoneId.of("Asia/Kolkata")
        ).filter { it.id == PersonalDietFitnessPlanV1.ID_PREFIX + "sleep" }
        rule.setContent {
            FrameByNavinTheme {
                V285PersonalLifePlannerCard(reminders = existing, enabled = true)
            }
        }
        rule.onNodeWithTag("v285-reading-conflict").assertExists()
        rule.onNodeWithTag("v285-install-count").assertExists()
    }
}
