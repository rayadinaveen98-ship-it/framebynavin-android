package com.framebynavin.app.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Alarm
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.ExpandLess
import androidx.compose.material.icons.outlined.ExpandMore
import androidx.compose.material.icons.outlined.NotificationsActive
import androidx.compose.material.icons.outlined.NotificationsNone
import androidx.compose.material.icons.outlined.NotificationsOff
import androidx.compose.material.icons.outlined.Tune
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.framebynavin.app.data.ProjectAttentionPlan
import com.framebynavin.app.data.ReminderDeliveryPreference
import com.framebynavin.app.ui.theme.*

/**
 * Copy-only presentation mapping. Does not change the persisted five plans, their
 * delivery policy, or the preference used by existing project/reminder records.
 */
internal object V156ReminderSetupCopy {
    fun title(plan: ProjectAttentionPlan): String = when (plan) {
        ProjectAttentionPlan.OFF -> "No reminders"
        ProjectAttentionPlan.LIGHT -> "Occasional reminders"
        ProjectAttentionPlan.GUIDED -> "Keep me on track"
        ProjectAttentionPlan.URGENT -> "Important deadline"
        ProjectAttentionPlan.CUSTOM -> "Choose my own time"
    }

    fun description(plan: ProjectAttentionPlan): String = when (plan) {
        ProjectAttentionPlan.OFF -> "Save the project without alerts."
        ProjectAttentionPlan.LIGHT -> "A gentle check-in before the deadline."
        ProjectAttentionPlan.GUIDED -> "Check in on each step. May use voice or alarm when work is at risk."
        ProjectAttentionPlan.URGENT -> "Stronger reminders, including voice and alarms, as the deadline approaches."
        ProjectAttentionPlan.CUSTOM -> "Pick a reminder time and decide how Backlot alerts you."
    }

    fun shortTitle(plan: ProjectAttentionPlan): String = when (plan) {
        ProjectAttentionPlan.OFF -> "Off"
        ProjectAttentionPlan.LIGHT -> "Light"
        ProjectAttentionPlan.GUIDED -> "On track"
        ProjectAttentionPlan.URGENT -> "Urgent"
        ProjectAttentionPlan.CUSTOM -> "My time"
    }

    fun alertLabel(preference: ReminderDeliveryPreference): String = when (preference) {
        ReminderDeliveryPreference.AUTO -> "Automatic alerts"
        ReminderDeliveryPreference.NOTIFICATION -> "Notification only"
        ReminderDeliveryPreference.VOICE -> "Voice alert"
        ReminderDeliveryPreference.ALARM -> "Ringing alarm"
        ReminderDeliveryPreference.SMART -> "Smart escalation"
    }

    fun summary(plan: ProjectAttentionPlan, preference: ReminderDeliveryPreference): String =
        if (plan == ProjectAttentionPlan.OFF) title(plan)
        else "${title(plan)} · ${alertLabel(preference)}"

    fun alertExplanation(preference: ReminderDeliveryPreference): String = when (preference) {
        ReminderDeliveryPreference.AUTO -> "Backlot chooses a suitable alert as your project needs change."
        ReminderDeliveryPreference.NOTIFICATION -> "Always use a notification, not an automatic voice or alarm."
        ReminderDeliveryPreference.VOICE -> "Speak the reminder using your chosen voice."
        ReminderDeliveryPreference.ALARM -> "Ring an alarm at the reminder time."
        ReminderDeliveryPreference.SMART -> "Start gently and escalate if you don't respond."
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
internal fun V156ReminderPlanPicker(
    selected: ProjectAttentionPlan,
    onSelected: (ProjectAttentionPlan) -> Unit,
    compact: Boolean = false,
) {
    if (compact) {
        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(7.dp),
        ) {
            ProjectAttentionPlan.entries.forEach { plan ->
                FilterChip(
                    modifier = Modifier.testTag("reminder-plan-compact-${plan.name}"),
                    selected = selected == plan,
                    onClick = { onSelected(plan) },
                    label = { Text(V156ReminderSetupCopy.shortTitle(plan), fontSize = 12.sp) },
                    leadingIcon = if (selected == plan) {
                        { Icon(Icons.Outlined.CheckCircle, null, Modifier.size(17.dp)) }
                    } else null,
                )
            }
        }
        Spacer(Modifier.height(5.dp))
        Text(
            V156ReminderSetupCopy.description(selected),
            color = MutedText,
            fontSize = 12.sp,
            lineHeight = 17.sp,
        )
    } else {
        ProjectAttentionPlan.entries.forEach { plan ->
            val chosen = selected == plan
            Surface(
                onClick = { onSelected(plan) },
                modifier = Modifier.fillMaxWidth().testTag("reminder-plan-full-${plan.name}"),
                shape = RoundedCornerShape(14.dp),
                color = if (chosen) RecRed.copy(alpha = .10f) else CinemaSurface,
                border = BorderStroke(
                    1.dp,
                    if (chosen) RecRed.copy(alpha = .75f) else CinemaLine,
                ),
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 13.dp, vertical = 11.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Icon(
                        when (plan) {
                            ProjectAttentionPlan.OFF -> Icons.Outlined.NotificationsOff
                            ProjectAttentionPlan.LIGHT -> Icons.Outlined.NotificationsNone
                            ProjectAttentionPlan.GUIDED -> Icons.Outlined.NotificationsActive
                            ProjectAttentionPlan.URGENT -> Icons.Outlined.Alarm
                            ProjectAttentionPlan.CUSTOM -> Icons.Outlined.Tune
                        },
                        contentDescription = null,
                        tint = if (chosen) RecRed else MutedGold,
                        modifier = Modifier.size(22.dp),
                    )
                    Spacer(Modifier.width(11.dp))
                    Column(Modifier.weight(1f)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                V156ReminderSetupCopy.title(plan),
                                color = ProjectorIvory,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold,
                            )
                            if (plan == ProjectAttentionPlan.GUIDED) {
                                Spacer(Modifier.width(7.dp))
                                Text("SUGGESTED", color = MutedGold, fontSize = 9.sp)
                            }
                        }
                        Spacer(Modifier.height(3.dp))
                        Text(
                            V156ReminderSetupCopy.description(plan),
                            color = MutedText,
                            fontSize = 11.sp,
                            lineHeight = 16.sp,
                        )
                    }
                    if (chosen) {
                        Spacer(Modifier.width(7.dp))
                        Icon(Icons.Outlined.CheckCircle, "Selected", tint = RecRed, modifier = Modifier.size(20.dp))
                    }
                }
            }
            Spacer(Modifier.height(7.dp))
        }
    }
}

@Composable
internal fun V156AdvancedReminderSettings(
    expanded: Boolean,
    onToggle: () -> Unit,
    preference: ReminderDeliveryPreference,
    content: @Composable () -> Unit,
) {
    Surface(
        onClick = onToggle,
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(14.dp),
        color = CinemaSurface,
        border = BorderStroke(1.dp, CinemaLine),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().heightIn(min = 56.dp).padding(horizontal = 13.dp, vertical = 9.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(Icons.Outlined.Tune, null, tint = MutedGold, modifier = Modifier.size(20.dp))
            Spacer(Modifier.width(10.dp))
            Column(Modifier.weight(1f)) {
                Text("Advanced reminder settings", color = ProjectorIvory, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                Text(V156ReminderSetupCopy.alertLabel(preference), color = MutedText, fontSize = 11.sp)
            }
            Icon(
                if (expanded) Icons.Outlined.ExpandLess else Icons.Outlined.ExpandMore,
                if (expanded) "Collapse advanced reminder settings" else "Expand advanced reminder settings",
                tint = MutedGold,
            )
        }
    }
    AnimatedVisibility(visible = expanded) {
        Column(Modifier.fillMaxWidth().padding(top = 12.dp, bottom = 5.dp)) {
            content()
        }
    }
}
