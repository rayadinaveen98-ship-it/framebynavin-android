package com.framebynavin.app.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.framebynavin.app.personal.PersonalAlert
import com.framebynavin.app.personal.PersonalDeliveryContract
import com.framebynavin.app.personal.PersonalPurpose
import com.framebynavin.app.personal.PersonalReminder
import com.framebynavin.app.personal.V288PersonalTodayPolicy
import com.framebynavin.app.personal.V288TodaySnapshot
import com.framebynavin.app.ui.theme.CinemaLine
import com.framebynavin.app.ui.theme.CinemaSurface
import com.framebynavin.app.ui.theme.MutedGold
import com.framebynavin.app.ui.theme.MutedText
import com.framebynavin.app.ui.theme.ProjectorIvory
import com.framebynavin.app.ui.theme.RecRed
import java.time.Instant
import java.time.format.DateTimeFormatter
import java.util.Locale

/** Deliberately light: the first screenful focuses on actual next actions. */
@Composable
internal fun V288PersonalTodayCard(
    snapshot: V288TodaySnapshot,
    nowMillis: Long,
    busy: Boolean,
    onRespond: (PersonalReminder, String) -> Unit,
) {
    val next = snapshot.next
    val needsAttention = V288PersonalTodayPolicy.isNeedsAttention(next, nowMillis)
    val dateFormatter = DateTimeFormatter.ofPattern("EEEE, d MMMM", Locale.getDefault())
    Column(
        modifier = Modifier.fillMaxWidth().testTag("v288-today-home"),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text(snapshot.date.format(dateFormatter), color = MutedGold,
                    fontSize = 11.sp, fontWeight = FontWeight.Bold)
                Text("Make today count.", color = ProjectorIvory,
                    fontSize = 21.sp, fontWeight = FontWeight.Bold)
            }
            // These are separate factual counts, not an invented progress ratio.
            Column(horizontalAlignment = Alignment.End) {
                Text("${snapshot.confirmedDoneToday} done",
                    color = ProjectorIvory, fontSize = 13.sp, fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.testTag("v288-today-confirmed-done"))
                Text("${snapshot.activeReminderCount} active routines",
                    color = MutedText, fontSize = 10.sp)
            }
        }
        Surface(
            modifier = Modifier.fillMaxWidth().testTag("v288-next-action"),
            color = CinemaSurface,
            shape = RoundedCornerShape(18.dp),
            border = BorderStroke(1.dp, if (needsAttention) MutedGold else CinemaLine),
        ) {
            Column(Modifier.padding(15.dp), verticalArrangement = Arrangement.spacedBy(9.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Outlined.Bolt, null, tint = MutedGold,
                        modifier = Modifier.size(17.dp))
                    Spacer(Modifier.size(7.dp))
                    Text(if (needsAttention) "NEEDS ATTENTION" else "UP NEXT",
                        color = MutedGold, fontWeight = FontWeight.Black, fontSize = 10.sp)
                }
                if (next == null) {
                    Text("You're clear for now.", color = ProjectorIvory,
                        fontSize = 16.sp, fontWeight = FontWeight.SemiBold)
                    Text("No upcoming activity today. Your other routines are in the list below.",
                        color = MutedText, fontSize = 11.sp)
                } else {
                    Row(verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(11.dp)) {
                        V288PersonalReminderIcon(next, large = true)
                        Column(Modifier.weight(1f)) {
                            Text(next.title, color = ProjectorIvory, fontSize = 16.sp,
                                fontWeight = FontWeight.Bold, maxLines = 2,
                                overflow = TextOverflow.Ellipsis,
                                modifier = Modifier.testTag("v288-next-title"))
                            val label = snapshot.nextAtMillis?.let {
                                Instant.ofEpochMilli(it).atZone(next.zoneId)
                                    .format(DateTimeFormatter.ofPattern("EEE, h:mm a", Locale.getDefault()))
                            } ?: "Time to check this reminder"
                            Text(label, color = MutedText, fontSize = 12.sp)
                        }
                    }
                    if (V288PersonalTodayPolicy.canRespond(next)) {
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            OutlinedButton(
                                enabled = !busy,
                                onClick = { onRespond(next, PersonalDeliveryContract.ACTION_DONE) },
                                modifier = Modifier.testTag("v288-next-done"),
                            ) {
                                Icon(Icons.Outlined.CheckCircle, null, tint = MutedGold,
                                    modifier = Modifier.size(16.dp))
                                Spacer(Modifier.size(5.dp))
                                Text("DONE", color = ProjectorIvory, fontSize = 11.sp)
                            }
                            OutlinedButton(
                                enabled = !busy,
                                onClick = { onRespond(next, PersonalDeliveryContract.ACTION_SNOOZE) },
                                modifier = Modifier.testTag("v288-next-snooze"),
                            ) {
                                Icon(Icons.Outlined.Snooze, null, tint = MutedGold,
                                    modifier = Modifier.size(16.dp))
                                Spacer(Modifier.size(5.dp))
                                Text("SNOOZE ${next.snoozeMinutes}m",
                                    color = ProjectorIvory, fontSize = 11.sp)
                            }
                        }
                    } else if (needsAttention) {
                        Text("Awaiting Android delivery or an eligible response. " +
                            "Open its row below for details.",
                            color = MutedText, fontSize = 11.sp)
                    }
                }
            }
        }
        if (snapshot.attentionCount > 1) {
            Text("${snapshot.attentionCount} reminders need attention",
                color = MutedGold, fontSize = 11.sp)
        }
    }
}

@Composable
internal fun V288PersonalReminderIcon(reminder: PersonalReminder, large: Boolean = false) {
    val name = reminder.title.lowercase(Locale.getDefault())
    val id = reminder.id.lowercase(Locale.ROOT)
    val size = if (large) 26.dp else 20.dp
    val icon = when {
        id.contains("skin") || name.contains("skin") || name.contains("sunscreen") ||
            name.contains("serum") || name.contains("face wash") ->
            Icons.Outlined.WbSunny
        id.contains("novel") || name.contains("read") || name.contains("novel") ||
            name.contains("book") -> Icons.Outlined.MenuBook
        id.contains("game") || name.contains("gaming") -> Icons.Outlined.SportsEsports
        id.contains("edit") || id.contains("write") || id.contains("polish") ||
            name.contains("edit") || name.contains("script") || name.contains("video") ->
            Icons.Outlined.Movie
        id.contains("nap") || reminder.purpose == PersonalPurpose.REST ||
            reminder.purpose == PersonalPurpose.SLEEP || name.contains("sleep") ->
            Icons.Outlined.Bedtime
        reminder.purpose == PersonalPurpose.MOVEMENT ||
            name.contains("workout") || name.contains("walk") ->
            Icons.Outlined.FitnessCenter
        reminder.purpose == PersonalPurpose.HYDRATION || name.contains("water") ->
            Icons.Outlined.WaterDrop
        reminder.alert == PersonalAlert.ALARM -> Icons.Outlined.Alarm
        else -> Icons.Outlined.NotificationsNone
    }
    Icon(icon, contentDescription = null, tint = MutedGold, modifier = Modifier.size(size))
}

/** Light section titles for the moved Settings panels, not extra boxed cards. */
@Composable
internal fun V288PersonalSettingsHeading(title: String, detail: String) {
    Column(Modifier.fillMaxWidth().padding(top = 8.dp, bottom = 2.dp),
        verticalArrangement = Arrangement.spacedBy(3.dp)) {
        Text(title.uppercase(Locale.getDefault()), color = MutedGold,
            fontWeight = FontWeight.Bold, fontSize = 11.sp)
        Text(detail, color = MutedText, fontSize = 11.sp)
    }
}
