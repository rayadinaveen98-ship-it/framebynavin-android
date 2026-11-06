package com.framebynavin.app.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.ChevronRight
import androidx.compose.material.icons.outlined.NotificationsActive
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.framebynavin.app.data.CreatorTask
import com.framebynavin.app.reminders.ReminderAckRecord
import com.framebynavin.app.reminders.ReminderAcknowledgementStore
import com.framebynavin.app.reminders.ReminderOccurrenceStore
import com.framebynavin.app.reminders.V179FollowUpClaimStore
import com.framebynavin.app.ui.theme.*

/**
 * V307: a compact Creator inbox, keeping the original response button component
 * reachable by explicit expansion. The separate Activity Center owns history.
 */
internal data class V307CreatorActivitySnapshot(
    val pending: List<Pair<CreatorTask, ReminderAckRecord>>,
    val failed: List<Pair<CreatorTask, ReminderAckRecord>>,
) {
    val needsAttention: Int get() = pending.size + failed.size
}
internal object V307CreatorActivityPolicy {
    fun from(evidence: List<V306CreatorEvidence>): V307CreatorActivitySnapshot {
        val pending = mutableListOf<Pair<CreatorTask, ReminderAckRecord>>()
        val failed = mutableListOf<Pair<CreatorTask, ReminderAckRecord>>()
        evidence.distinctBy { it.task.id }.forEach { entry ->
            val recent = entry.history.lastOrNull() ?: return@forEach
            if (V181ReminderInboxPolicy.actionable(
                    entry.task, recent, entry.latestTokenCurrent
                )) pending.add(entry.task to recent)
            else if (V181ReminderInboxPolicy.failedDelivery(
                    entry.task, recent, entry.latestTokenCurrent
                )) failed.add(entry.task to recent)
        }
        return V307CreatorActivitySnapshot(
            pending.sortedBy { it.second.issuedAtMillis },
            failed.sortedByDescending { it.second.changedAtMillis },
        )
    }
}

@Composable
internal fun V307CompactCreatorActivity(
    tasks: List<CreatorTask>,
    nowMillis: Long,
    snoozeMinutes: Int,
    onOpenActivityCenter: () -> Unit,
    onOpenProject: (String) -> Unit,
    onAcknowledge: ((CreatorTask, String, String) -> Unit)?,
) {
    val context = LocalContext.current.applicationContext
    val journal = remember(context) { ReminderAcknowledgementStore(context) }
    val tokens = remember(context) { ReminderOccurrenceStore(context) }
    val claims = remember(context) { V179FollowUpClaimStore(context) }
    val evidence = remember(tasks, nowMillis) {
        runCatching {
            tasks.map { task ->
                val recent = journal.recent(task.id)
                V306CreatorEvidence(
                    task = task,
                    history = recent,
                    latestTokenCurrent = recent.lastOrNull()?.let {
                        tokens.matches(task, it.occurrenceId)
                    } ?: false,
                )
            }
        }.getOrNull()
    }
    val state = V307CreatorActivityPolicy.from(evidence.orEmpty())
    var expanded by remember { mutableStateOf(false) }
    if (evidence == null) {
        Text(
            "Reminder activity couldn't be loaded. Your records haven't changed.",
            color = RecRed, fontSize = 12.sp,
            modifier = Modifier.testTag("v307-creator-inbox-error"),
        )
        return
    }
    // With zero confirmed current response/failure records, show nothing on
    // the main Creator screen. History remains in V306, always accessible via toolbar.
    if (state.needsAttention == 0) return

    Surface(
        modifier = Modifier.fillMaxWidth().testTag("v307-creator-compact-inbox"),
        shape = RoundedCornerShape(14.dp),
        border = BorderStroke(1.dp, if (state.failed.isNotEmpty()) RecRed else CinemaLine),
        color = CinemaSurface,
    ) {
        Column(Modifier.padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(7.dp)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(9.dp),
            ) {
                Icon(Icons.Outlined.NotificationsActive, null, tint = MutedGold)
                Column(Modifier.weight(1f)) {
                    Text(
                        if (state.pending.isNotEmpty())
                            "${state.pending.size} reminder${if (state.pending.size == 1) "" else "s"} awaiting response"
                        else "${state.failed.size} delivery problem${if (state.failed.size == 1) "" else "s"}",
                        color = ProjectorIvory, fontSize = 12.sp, fontWeight = FontWeight.Bold,
                    )
                    if (state.failed.isNotEmpty() && state.pending.isNotEmpty()) {
                        Text("${state.failed.size} also need delivery attention",
                            color = MutedText, fontSize = 11.sp)
                    }
                }
                IconButton(
                    onClick = onOpenActivityCenter,
                    modifier = Modifier.testTag("v307-open-activity"),
                ) {
                    Icon(Icons.Outlined.ChevronRight, "Open Activity Center",
                        tint = MutedGold)
                }
            }
            if (state.pending.isNotEmpty()) {
                TextButton(
                    onClick = { expanded = !expanded },
                    modifier = Modifier.fillMaxWidth()
                        .testTag("v307-toggle-actions"),
                ) {
                    Text(if (expanded) "HIDE RESPONSE CONTROLS" else "RESPOND NOW",
                        color = MutedGold, fontSize = 11.sp)
                }
            }
            if (expanded) {
                state.pending.forEach { (task, record) ->
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
            if (state.failed.isNotEmpty()) {
                TextButton(
                    onClick = onOpenActivityCenter,
                    modifier = Modifier.fillMaxWidth().testTag("v307-review-delivery"),
                ) {
                    Text("REVIEW DELIVERY PROBLEMS", color = MutedGold, fontSize = 11.sp)
                }
            }
        }
    }
}
