package com.framebynavin.app.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.History
import androidx.compose.material.icons.outlined.NotificationsActive
import androidx.compose.material.icons.outlined.WarningAmber
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.framebynavin.app.data.CreatorTask
import com.framebynavin.app.reminders.ReminderAckRecord
import com.framebynavin.app.reminders.ReminderAckState
import com.framebynavin.app.reminders.ReminderAcknowledgementStore
import com.framebynavin.app.reminders.ReminderActionSafety
import com.framebynavin.app.reminders.ReminderConstants
import com.framebynavin.app.reminders.ReminderOccurrenceStore
import com.framebynavin.app.reminders.V179FollowUpClaimStore
import com.framebynavin.app.ui.theme.*

/**
 * V181: current acknowledgement inbox + read-only recent occurrence evidence.
 *
 * Uses the same token/action authority as notification buttons. All writes belong to
 * ReminderActionReceiver, not this composable; a stale card can never mutate a project.
 * This section does not assume notification delivery means seen/heard.
 */
@Composable
internal fun V181ReminderResponseSection(
    tasks: List<CreatorTask>,
    nowMillis: Long,
    snoozeMinutes: Int,
    onOpenProject: (String) -> Unit,
    onAcknowledge: ((CreatorTask, String, String) -> Unit)?,
) {
    val context = LocalContext.current
    val journal = remember(context) { ReminderAcknowledgementStore(context.applicationContext) }
    val occurrences = remember(context) { ReminderOccurrenceStore(context.applicationContext) }
    val claims = remember(context) { V179FollowUpClaimStore(context.applicationContext) }
    var historyExpanded by remember { mutableStateOf(false) }

    // The latest task snapshot determines authority. A historical entry may be displayed,
    // but NEVER used as a currently actionable token.
    val pending = tasks.mapNotNull { task ->
        val record = journal.awaitingResponse(task)
        if (!V181ReminderInboxPolicy.actionable(task, record, record != null &&
                occurrences.matches(task, record.occurrenceId))) null
        else task to requireNotNull(record)
    }.sortedBy { it.second.issuedAtMillis }
    val failed = tasks.mapNotNull { task ->
        val record = journal.recent(task.id).lastOrNull()
        if (V181ReminderInboxPolicy.failedDelivery(task, record, record != null &&
                occurrences.matches(task, record.occurrenceId))) task to requireNotNull(record)
        else null
    }
    val recent = tasks.flatMap { task ->
        journal.recent(task.id).takeLast(4).map { task to it }
    }.sortedByDescending { it.second.changedAtMillis }.take(6)

    Column(Modifier.fillMaxWidth().testTag("v181-reminder-response-section")) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Outlined.NotificationsActive, null, tint = RecRed)
            Spacer(Modifier.width(8.dp))
            Text("NEEDS YOUR RESPONSE", color = ProjectorIvory, fontSize = 14.sp,
                fontWeight = FontWeight.Black, modifier = Modifier.weight(1f))
            Text("${pending.size}", color = RecRed, fontSize = 16.sp, fontWeight = FontWeight.Bold)
        }
        Spacer(Modifier.height(4.dp))
        Text(
            "Unanswered means no action was recorded. It does not mean you saw or heard an alert.",
            color = MutedText, fontSize = 11.sp, lineHeight = 16.sp,
        )
        Spacer(Modifier.height(10.dp))
        if (pending.isEmpty()) {
            Surface(
                modifier = Modifier.fillMaxWidth().testTag("v181-no-pending-reminders"),
                shape = RoundedCornerShape(16.dp),
                color = CinemaSurface, border = BorderStroke(1.dp, CinemaLine),
            ) {
                Row(Modifier.padding(15.dp), verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Outlined.CheckCircle, null, tint = MutedGold)
                    Spacer(Modifier.width(10.dp))
                    Text("No reminders are awaiting your response.",
                        color = ProjectorIvory, fontSize = 12.sp)
                }
            }
        } else {
            pending.forEach { (task, record) ->
                V181PendingReminderCard(
                    task = task,
                    record = record,
                    followUpAttempts = claims.attemptedCount(task.id, record.occurrenceId),
                    snoozeMinutes = snoozeMinutes,
                    onOpenProject = { onOpenProject(task.id) },
                    onAcknowledge = { action ->
                        onAcknowledge?.invoke(task, record.occurrenceId, action)
                    },
                    controlsEnabled = onAcknowledge != null,
                )
            }
        }

        if (failed.isNotEmpty()) {
            Spacer(Modifier.height(16.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Outlined.WarningAmber, null, tint = MutedGold)
                Spacer(Modifier.width(8.dp))
                Text("DELIVERY NEEDS ATTENTION", color = ProjectorIvory, fontSize = 12.sp,
                    fontWeight = FontWeight.Bold)
            }
            failed.forEach { (task, record) ->
                Surface(
                    modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
                    color = CinemaSurface, shape = RoundedCornerShape(14.dp),
                    border = BorderStroke(1.dp, CinemaLine),
                ) {
                    Column(Modifier.padding(13.dp)) {
                        Text(task.title, color = ProjectorIvory, fontSize = 13.sp,
                            fontWeight = FontWeight.Bold)
                        Text(V181ReminderInboxPolicy.status(record.state),
                            color = MutedGold, fontSize = 11.sp)
                        TextButton(onClick = { onOpenProject(task.id) }) {
                            Text("OPEN PROJECT", color = MutedGold)
                        }
                    }
                }
            }
        }
        Spacer(Modifier.height(14.dp))
        TextButton(
            modifier = Modifier.fillMaxWidth().testTag("v181-reminder-history-toggle"),
            onClick = { historyExpanded = !historyExpanded },
        ) {
            Icon(Icons.Outlined.History, null, tint = MutedGold)
            Spacer(Modifier.width(7.dp))
            Text(if (historyExpanded) "HIDE RECENT REMINDER ACTIVITY" else "VIEW RECENT REMINDER ACTIVITY",
                color = MutedGold, fontSize = 11.sp)
        }
        if (historyExpanded) {
            Surface(
                modifier = Modifier.fillMaxWidth().testTag("v181-reminder-history"),
                shape = RoundedCornerShape(15.dp),
                color = CinemaSurface, border = BorderStroke(1.dp, CinemaLine),
            ) {
                Column(Modifier.padding(13.dp)) {
                    if (recent.isEmpty()) {
                        Text("No reminder activity recorded yet.", color = MutedText,
                            fontSize = 12.sp)
                    } else {
                        recent.forEachIndexed { index, (task, record) ->
                            if (index != 0) {
                                Spacer(Modifier.height(10.dp))
                                HorizontalDivider(color = CinemaLine)
                                Spacer(Modifier.height(10.dp))
                            }
                            Text(task.title, color = ProjectorIvory, fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                maxLines = 1, overflow = TextOverflow.Ellipsis)
                            Text(
                                "${V181ReminderInboxPolicy.recentLabel(record)} · " +
                                    pFormatDateTime(record.changedAtMillis),
                                color = MutedText, fontSize = 11.sp,
                            )
                        }
                    }
                    Spacer(Modifier.height(7.dp))
                    Text(
                        "Activity records what Backlot attempted and your confirmed actions, " +
                            "not whether you read an Android notification.",
                        color = MutedText, fontSize = 10.sp, lineHeight = 15.sp,
                    )
                }
            }
        }
    }
}

@Composable
internal fun V181PendingReminderCard(
    task: CreatorTask,
    record: ReminderAckRecord,
    followUpAttempts: Int,
    snoozeMinutes: Int,
    onOpenProject: () -> Unit,
    onAcknowledge: (String) -> Unit,
    controlsEnabled: Boolean = true,
) {
    var requestedAction by remember(task.id, record.occurrenceId) { mutableStateOf(false) }
    val canRespond = controlsEnabled && !requestedAction &&
        record.state != ReminderAckState.RESPONDED &&
        record.state != ReminderAckState.DELIVERY_FAILED &&
        ReminderActionSafety.isActionable(task)

    Surface(
        modifier = Modifier.fillMaxWidth().padding(bottom = 9.dp)
            .testTag("v181-pending-${task.id}"),
        color = CinemaSurface,
        border = BorderStroke(1.dp, RecRed.copy(alpha = .38f)),
        shape = RoundedCornerShape(16.dp),
    ) {
        Column(Modifier.padding(14.dp)) {
            Text(task.title, color = ProjectorIvory, fontSize = 14.sp,
                fontWeight = FontWeight.Bold, maxLines = 2, overflow = TextOverflow.Ellipsis)
            Spacer(Modifier.height(4.dp))
            Text(V181ReminderInboxPolicy.status(record.state), color = MutedGold,
                fontSize = 11.sp, lineHeight = 16.sp)
            Text(
                if (followUpAttempts > 0)
                    "Quiet follow-up attempts: ${followUpAttempts.coerceIn(0, 2)} of 2"
                else "No quiet follow-up attempted yet",
                color = MutedText, fontSize = 11.sp, lineHeight = 16.sp,
                modifier = Modifier.padding(top = 4.dp),
            )
            if (requestedAction) {
                Text("Response requested · checking your project state",
                    color = MutedGold, fontSize = 11.sp,
                    modifier = Modifier.padding(top = 7.dp))
            }
            Spacer(Modifier.height(11.dp))
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(
                    onClick = {
                        requestedAction = true
                        onAcknowledge(ReminderConstants.ACTION_STARTED)
                    },
                    enabled = canRespond,
                    modifier = Modifier.weight(1f).heightIn(min = 45.dp)
                        .testTag("v181-start-${task.id}"),
                    colors = ButtonDefaults.buttonColors(containerColor = RecRed),
                    shape = RoundedCornerShape(12.dp),
                ) { Text("I'M WORKING", fontSize = 11.sp, maxLines = 1) }
                OutlinedButton(
                    onClick = {
                        requestedAction = true
                        onAcknowledge(ReminderConstants.ACTION_SNOOZE)
                    },
                    enabled = canRespond,
                    modifier = Modifier.weight(1f).heightIn(min = 45.dp)
                        .testTag("v181-snooze-${task.id}"),
                    border = BorderStroke(1.dp, CinemaLine),
                    shape = RoundedCornerShape(12.dp),
                ) { Text("SNOOZE ${snoozeMinutes}m", fontSize = 11.sp, maxLines = 1) }
            }
            TextButton(onClick = onOpenProject) {
                Text("OPEN PROJECT", fontSize = 11.sp, color = MutedGold)
            }
        }
    }
}
