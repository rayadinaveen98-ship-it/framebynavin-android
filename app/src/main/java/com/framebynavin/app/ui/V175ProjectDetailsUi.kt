package com.framebynavin.app.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.KeyboardArrowDown
import androidx.compose.material.icons.outlined.KeyboardArrowUp
import androidx.compose.material.icons.outlined.Notes
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.framebynavin.app.data.ProjectAttentionPlan
import com.framebynavin.app.data.ReminderDeliveryPreference
import com.framebynavin.app.ui.theme.*

/** Presentation-only guidance. This does not change reminder or permission policy. */
internal object V175ProjectDetailsCopy {
    fun reminderOutcome(plan: ProjectAttentionPlan, delivery: ReminderDeliveryPreference): String =
        when (plan) {
            ProjectAttentionPlan.OFF -> "No alerts will be scheduled for this project."
            ProjectAttentionPlan.LIGHT -> "A gentle check-in ahead of your deadline."
            ProjectAttentionPlan.GUIDED -> "Check-ins can follow your project stages."
            ProjectAttentionPlan.URGENT -> "More persistent reminders as your deadline gets closer."
            ProjectAttentionPlan.CUSTOM -> "Your reminder will use the time you choose."
        } + when {
            plan == ProjectAttentionPlan.OFF -> ""
            delivery == ReminderDeliveryPreference.AUTO -> " Backlot chooses the alert type."
            delivery == ReminderDeliveryPreference.NOTIFICATION -> " Notifications only."
            delivery == ReminderDeliveryPreference.VOICE -> " Using a spoken alert."
            delivery == ReminderDeliveryPreference.ALARM -> " Using an alarm."
            else -> " Alerts can escalate if you don't respond."
        }

    fun permissionsWarning(needsAdvancedPermissions: Boolean, ready: Boolean): String? =
        if (needsAdvancedPermissions && !ready)
            "Voice, alarms or full-screen alerts may need permissions. Review Settings before relying on them."
        else null
}

@Composable
internal fun V175ProjectNotesField(
    notes: String,
    onNotesChange: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    var expanded by rememberSaveable { mutableStateOf(notes.isNotBlank()) }
    Surface(
        modifier = modifier.fillMaxWidth().testTag("project-notes-section"),
        shape = RoundedCornerShape(16.dp),
        color = CinemaSurface,
        border = BorderStroke(1.dp, CinemaLine),
    ) {
        Column {
            Surface(
                onClick = { expanded = !expanded },
                modifier = Modifier.fillMaxWidth().heightIn(min = V173ProjectUiMetrics.touchTarget),
                color = CinemaSurface,
            ) {
                Row(
                    Modifier.fillMaxWidth().padding(horizontal = 13.dp, vertical = 11.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Icon(Icons.Outlined.Notes, null, tint = MutedGold, modifier = Modifier.size(19.dp))
                    Spacer(Modifier.width(9.dp))
                    Column(Modifier.weight(1f)) {
                        Text("Project notes · Optional", color = ProjectorIvory, fontSize = V173ProjectUiMetrics.bodyText, fontWeight = FontWeight.Bold)
                        Text(
                            if (notes.isBlank()) "Add a creative angle, idea or reference."
                            else notes,
                            color = MutedText,
                            fontSize = V173ProjectUiMetrics.supportingText,
                            maxLines = 2,
                        )
                    }
                    Icon(
                        if (expanded) Icons.Outlined.KeyboardArrowUp else Icons.Outlined.KeyboardArrowDown,
                        if (expanded) "Hide project notes" else "Edit project notes",
                        tint = MutedGold,
                    )
                }
            }
            AnimatedVisibility(visible = expanded) {
                OutlinedTextField(
                    value = notes,
                    onValueChange = onNotesChange,
                    modifier = Modifier.fillMaxWidth().padding(start = 13.dp, end = 13.dp, bottom = 13.dp).heightIn(min = 84.dp).testTag("project-notes-input"),
                    placeholder = { Text("What should you remember about this project?") },
                    minLines = 2,
                    shape = RoundedCornerShape(13.dp),
                )
            }
        }
    }
}

@Composable
internal fun V175ReminderMeaning(
    plan: ProjectAttentionPlan,
    delivery: ReminderDeliveryPreference,
    warning: String?,
) {
    Column(Modifier.fillMaxWidth().testTag("project-reminder-explanation")) {
        Text(
            V175ProjectDetailsCopy.reminderOutcome(plan, delivery),
            color = MutedText,
            fontSize = V173ProjectUiMetrics.bodyText,
        )
        if (warning != null) {
            Spacer(Modifier.height(8.dp))
            Text(warning, color = MutedGold, fontSize = V173ProjectUiMetrics.supportingText)
        }
    }
}
