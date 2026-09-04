package com.framebynavin.app.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.ChevronRight
import androidx.compose.material.icons.outlined.NotificationsNone
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.framebynavin.app.data.CreatorTask
import com.framebynavin.app.data.ReminderMode
import com.framebynavin.app.data.TaskStatus
import com.framebynavin.app.reminders.SmartSessionStore
import com.framebynavin.app.ui.theme.*
import androidx.compose.ui.platform.LocalContext
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/** Compact awareness surface for Today. It never competes with the primary Up Next project. */
@Composable
internal fun PTodayReminders(
    tasks: List<CreatorTask>,
    onViewAll: () -> Unit,
) {
    val context = LocalContext.current
    val sessions = SmartSessionStore(context.applicationContext)
    val now = System.currentTimeMillis()
    val active = tasks
        .filter {
            (it.status == TaskStatus.PLANNED || it.status == TaskStatus.WORKING) &&
                it.reminderEnabled && it.reminderMode != ReminderMode.NONE
        }
        .sortedBy { it.reminderAtMillis.takeIf { time -> time > 0L } ?: Long.MAX_VALUE }

    val remindingNow = active.filter { sessions.current(it.id) != null }
    val future = active.filter { it.reminderAtMillis >= now && it !in remindingNow }.take(3)
    val visible = (remindingNow.take(1) + future).distinctBy { it.id }.take(3)

    Column(Modifier.fillMaxWidth()) {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Text("REMINDERS", color = ProjectorIvory, fontSize = 14.sp, fontWeight = FontWeight.Black)
            Spacer(Modifier.weight(1f))
            TextButton(onClick = onViewAll, contentPadding = PaddingValues(horizontal = 4.dp, vertical = 0.dp)) {
                Text("VIEW ALL", color = RecRed, fontSize = 8.5.sp, fontWeight = FontWeight.Black)
                Spacer(Modifier.width(2.dp))
                Icon(Icons.Outlined.ChevronRight, null, tint = RecRed, modifier = Modifier.size(15.dp))
            }
        }
        Text(
            if (visible.isEmpty()) "Nothing else is waiting for your attention." else "Your next alerts, without turning Today into a calendar.",
            color = MutedText,
            fontSize = 8.9.sp,
        )
        Spacer(Modifier.height(8.dp))

        Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(18.dp),
            color = CinemaSurface,
            border = BorderStroke(1.dp, CinemaLine),
        ) {
            if (visible.isEmpty()) {
                Row(Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
                    Box(Modifier.size(34.dp), contentAlignment = Alignment.Center) {
                        Icon(Icons.Outlined.NotificationsNone, null, tint = MutedGold, modifier = Modifier.size(19.dp))
                    }
                    Spacer(Modifier.width(8.dp))
                    Text("No upcoming reminders", color = MutedText, fontSize = 10.sp)
                }
            } else {
                Column(Modifier.padding(horizontal = 13.dp, vertical = 6.dp)) {
                    visible.forEachIndexed { index, task ->
                        val isNow = sessions.current(task.id) != null
                        Row(
                            Modifier.fillMaxWidth().padding(vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Box(
                                Modifier.size(34.dp),
                                contentAlignment = Alignment.Center,
                            ) {
                                Surface(
                                    modifier = Modifier.fillMaxSize(),
                                    shape = CircleShape,
                                    color = if (isNow) RecRed.copy(alpha = .12f) else MutedGold.copy(alpha = .09f),
                                ) { }
                                Icon(pModeIcon(task.reminderMode), null, tint = if (isNow) RecRed else MutedGold, modifier = Modifier.size(17.dp))
                            }
                            Spacer(Modifier.width(9.dp))
                            Column(Modifier.weight(1f)) {
                                Text(task.title, color = ProjectorIvory, fontSize = 10.8.sp, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                Text(
                                    if (isNow) "REMINDING NOW · ${pModeLabel(task.reminderMode)}" else "${pHomeReminderTime(task.reminderAtMillis)} · ${pModeLabel(task.reminderMode)}",
                                    color = if (isNow) RecRed else MutedText,
                                    fontSize = 8.2.sp,
                                    fontWeight = if (isNow) FontWeight.Bold else FontWeight.Medium,
                                )
                            }
                            if (task.reminderMode == ReminderMode.SMART) {
                                Text(task.priority.name.lowercase().replaceFirstChar { it.uppercase() }, color = MutedGold, fontSize = 7.8.sp)
                            }
                        }
                        if (index != visible.lastIndex) HorizontalDivider(color = CinemaLine.copy(alpha = .65f))
                    }
                }
            }
        }
    }
}

private fun pHomeReminderTime(millis: Long): String {
    if (millis <= 0L) return "No time"
    return SimpleDateFormat("EEE · h:mm a", Locale.getDefault()).format(Date(millis))
}
