package com.framebynavin.app.ui

import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.framebynavin.app.data.HardeningTestEnvironment
import com.framebynavin.app.personal.PersonalDietFitnessPlanV1
import com.framebynavin.app.ui.theme.FrameByNavinTheme
import java.time.LocalDate
import java.time.ZoneId
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/** Only reads permissions and paints UI; never mutates a real Personal record. */
@RunWith(AndroidJUnit4::class)
class V286PersonalReadinessUiTest {
    @get:Rule val rule = createComposeRule()

    @Test fun emptyPersonalAgendaStillShowsNonDestructiveDiagnostics() {
        HardeningTestEnvironment.requireCiEmulator()
        rule.setContent {
            FrameByNavinTheme {
                V286PersonalReadinessCard(
                    reminders = emptyList(), permissionRevision = 0,
                    enabled = true, onRecheck = {},
                )
            }
        }
        rule.onNodeWithTag("v286-personal-readiness").assertExists()
        rule.onNodeWithTag("v286-readiness-summary").assertExists()
        rule.onNodeWithTag("v286-recheck-scheduling").assertDoesNotExist()
    }

    @Test fun existingAlarmCanRequestManualRecheckWithoutAutoPrompting() {
        HardeningTestEnvironment.requireCiEmulator()
        val reminder = PersonalDietFitnessPlanV1.templates(
            LocalDate.of(2026, 11, 5), ZoneId.of("Asia/Kolkata")
        ).single { it.id == PersonalDietFitnessPlanV1.ID_PREFIX + "wake" }
        rule.setContent {
            FrameByNavinTheme {
                V286PersonalReadinessCard(
                    reminders = listOf(reminder), permissionRevision = 0,
                    enabled = true, onRecheck = {},
                )
            }
        }
        rule.onNodeWithTag("v286-recheck-scheduling").assertExists()
        rule.onNodeWithTag("v286-open-notification-settings").assertExists()
    }
}
