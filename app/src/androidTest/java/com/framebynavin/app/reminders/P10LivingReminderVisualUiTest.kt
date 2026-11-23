package com.framebynavin.app.reminders

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Text
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithContentDescription
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performSemanticsAction
import com.framebynavin.app.data.HardeningTestEnvironment
import com.framebynavin.app.personal.PersonalAlert
import com.framebynavin.app.personal.PersonalReminder
import com.framebynavin.app.personal.V2233PersonalAlertScreen
import com.framebynavin.app.ui.theme.FrameByNavinTheme
import java.time.LocalDate
import java.time.LocalTime
import org.junit.Rule
import org.junit.Test

/**
 * P10 replacement presentation source tests; DO NOT execute until the
 * founder opens the consolidated compile/QA gate.
 * Synthetic screens never invoke live alarm, receipt or audio services.
 */
class P10LivingReminderVisualUiTest {
    @get:Rule val rule = createComposeRule()

    @Test fun creatorVoiceShowsNewEmblemAndOriginalResponseControls() =
        creator(ReminderCheckInKind.VOICE)

    @Test fun creatorAlarmShowsNewEmblemAndOriginalResponseControls() =
        creator(ReminderCheckInKind.ALARM)

    @Test fun personalVoiceShowsNewEmblemAndOriginalResponseControls() =
        personal(PersonalAlert.VOICE)

    @Test fun personalAlarmShowsNewEmblemAndOriginalResponseControls() =
        personal(PersonalAlert.ALARM)

    @Test fun explicitAccessibleOpenRevealsWithoutLegacyOneTapParticleTarget() {
        HardeningTestEnvironment.requireCiEmulator()
        rule.setContent {
            FrameByNavinTheme {
                var open by remember { mutableStateOf(false) }
                Box(Modifier.fillMaxSize()) {
                    if (!open) {
                        P10ReminderEntranceStage(
                            scope = P1ReminderScope.CREATOR,
                            kind = ReminderCheckInKind.VOICE,
                            title = "Finish the next edit",
                            occurrenceKey = "synthetic:p10:accessible-open",
                            locked = false,
                            media = null,
                            onOpen = { open = true },
                        )
                    } else Text("OPENED")
                }
            }
        }
        // TalkBack activation invokes the semantic OnClick directly. A normal
        // physical single tap is intentionally NOT the double-tap reveal gesture.
        rule.onNodeWithText("OPENED").assertDoesNotExist()
        rule.onNodeWithContentDescription("Open creator reminder")
            .performSemanticsAction(SemanticsActions.OnClick)
        rule.waitForIdle()
        rule.onNodeWithText("OPENED").assertExists()
        rule.onNodeWithTag("p10-entrance-creator").assertDoesNotExist()
    }

    private fun creator(kind: ReminderCheckInKind) {
        HardeningTestEnvironment.requireCiEmulator()
        rule.setContent {
            FrameByNavinTheme {
                ReminderCheckInScreen(
                    kind = kind,
                    title = "Finish the film sequence",
                    dueLabel = "Today at 10:30 AM",
                    stageLabel = "Editing",
                    suggestedAction = "Review the final cut",
                    notes = "Synthetic P10 UI",
                    snoozeMinutes = 10,
                    stageCheckIn = false,
                    onStageDone = {},
                    onWorking = {},
                    onSnooze = {},
                    onPause = {},
                    onReschedule = {},
                    onDismiss = {},
                    onReplay = if (kind == ReminderCheckInKind.VOICE) ({}) else null,
                )
            }
        }
        // Animator-off CI may reveal immediately. Otherwise use the accessible
        // explicit action, which does not require a physical double tap.
        val intro = rule.onAllNodesWithContentDescription("Open creator reminder")
        if (intro.fetchSemanticsNodes().isNotEmpty()) intro[0].performClick()
        rule.onNodeWithTag("v200-reminder-title").assertExists()
        rule.onNodeWithTag("v200-primary-reminder-action").assertExists()
        rule.onNodeWithTag("p10-creator-context-stage").assertExists()
        rule.onNodeWithTag(
            if (kind == ReminderCheckInKind.VOICE) "p10-identity-voice" else "p10-identity-alarm",
        ).assertExists()
        rule.onNodeWithText("SNOOZE 10 MIN").assertExists()
    }

    private fun personal(alert: PersonalAlert) {
        HardeningTestEnvironment.requireCiEmulator()
        val fixture = PersonalReminder(
            id = "p10:visual:${alert.name.lowercase()}",
            title = "Your evening routine",
            startDate = LocalDate.now(),
            time = LocalTime.of(18, 30),
            alert = alert,
        ).copy(pendingOccurrenceMillis = System.currentTimeMillis() + 600_000L)
        rule.setContent {
            FrameByNavinTheme {
                V2233PersonalAlertScreen(
                    reminder = fixture,
                    invalid = false,
                    busy = false,
                    error = null,
                    onDone = {},
                    onSnooze = {},
                    onDismiss = {},
                    onClose = {},
                )
            }
        }
        rule.onNodeWithTag("v2233-personal-alert-title").assertExists()
        rule.onNodeWithTag("v2233-personal-done").assertExists()
        rule.onNodeWithTag("v2233-personal-snooze").assertExists()
        rule.onNodeWithTag("v2233-personal-dismiss").assertExists()
        rule.onNodeWithTag("p10-personal-info-stage").assertExists()
        rule.onNodeWithTag(
            if (alert == PersonalAlert.VOICE) "p10-identity-voice" else "p10-identity-alarm",
        ).assertExists()
    }
}
