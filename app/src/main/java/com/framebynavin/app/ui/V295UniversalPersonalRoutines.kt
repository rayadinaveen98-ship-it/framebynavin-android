package com.framebynavin.app.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.ChevronRight
import androidx.compose.material.icons.outlined.EditCalendar
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.framebynavin.app.personal.PersonalReminder
import com.framebynavin.app.personal.PersonalRecurrence
import com.framebynavin.app.personal.PersonalSelfCarePresets
import com.framebynavin.app.ui.theme.*
import java.time.Instant
import java.time.format.DateTimeFormatter

/**
 * V295 foundation: No user-specific plans, auto-installed samples or fake
 * 21-day challenge completion. The source is saved Personal reminders only.
 */
@Composable
internal fun V295UniversalPersonalRoutines(
    reminders: List<PersonalReminder>,
    enabled: Boolean,
    onCreateReminder: () -> Unit,
    onEditReminder: (PersonalReminder) -> Unit,
    onOpenSpaces: () -> Unit,
    onOpenChallenges: () -> Unit,
    onOpenGroups: () -> Unit,
) {
    val active = reminders.filter { !it.paused }
    val now = System.currentTimeMillis()
    val rhythm = active.mapNotNull { reminder ->
        val next = runCatching { PersonalRecurrence.nextAfter(reminder, now) }.getOrNull()
        next?.let { reminder to it }
    }.sortedBy { it.second }.take(4)

    Surface(
        modifier = Modifier.fillMaxWidth().testTag("v295-personal-routines"),
        color = CinemaSurface,
        shape = RoundedCornerShape(18.dp),
        border = BorderStroke(1.dp, CinemaLine),
    ) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(11.dp)) {
            Text("My Routines", color = ProjectorIvory,
                fontSize = 18.sp, fontWeight = FontWeight.Bold)
            Text(
                if (reminders.isEmpty()) "Build your own routine. No plan or schedule is preloaded."
                else "${reminders.size} personal reminders · ${active.size} active",
                color = MutedText, fontSize = 13.sp,
            )
            Button(
                onClick = onCreateReminder, enabled = enabled,
                modifier = Modifier.fillMaxWidth().testTag("v295-routines-new"),
            ) {
                Icon(Icons.Outlined.Add, null, modifier = Modifier.size(17.dp))
                Spacer(Modifier.width(7.dp))
                Text("NEW ROUTINE REMINDER")
            }
            Text("Daily Rhythm", color = ProjectorIvory,
                fontSize = 15.sp, fontWeight = FontWeight.SemiBold)
            if (rhythm.isEmpty()) {
                Text("Your scheduled reminders will appear here.",
                    color = MutedText, fontSize = 12.sp)
            } else {
                rhythm.forEach { (reminder, due) ->
                    Surface(
                        onClick = { onEditReminder(reminder) },
                        modifier = Modifier.fillMaxWidth().testTag("v295-routine-existing"),
                        color = CinemaSurfaceRaised,
                        shape = RoundedCornerShape(12.dp),
                    ) {
                        Row(Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(9.dp)) {
                            Icon(Icons.Outlined.EditCalendar, null,
                                tint = MutedText, modifier = Modifier.size(19.dp))
                            Column(Modifier.weight(1f)) {
                                Text(reminder.title, color = ProjectorIvory,
                                    fontSize = 13.sp, maxLines = 1,
                                    overflow = TextOverflow.Ellipsis)
                                Text(Instant.ofEpochMilli(due).atZone(reminder.zoneId)
                                    .format(DateTimeFormatter.ofPattern("EEE, d MMM · h:mm a")) +
                                    " · " + PersonalSelfCarePresets.scheduleLabel(
                                        reminder.repeat, reminder.everyNHours,
                                        reminder.everyNDays, reminder.weeklyDays,
                                    ),
                                    color = MutedText, fontSize = 11.sp,
                                    maxLines = 2, overflow = TextOverflow.Ellipsis)
                            }
                            Icon(Icons.Outlined.ChevronRight, "Edit reminder",
                                tint = MutedText, modifier = Modifier.size(18.dp))
                        }
                    }
                }
            }
            OutlinedButton(
                enabled = enabled,
                onClick = onOpenChallenges,
                modifier = Modifier.fillMaxWidth().testTag("v328-routines-open-challenges"),
            ) { Text("MY 21-DAY CHALLENGES") }
            OutlinedButton(
                enabled = enabled,
                onClick = onOpenGroups,
                modifier = Modifier.fillMaxWidth().testTag("v298-routines-open-groups"),
            ) { Text("MY REMINDER GROUPS") }
            TextButton(onClick = onOpenSpaces,
                modifier = Modifier.fillMaxWidth().testTag("v295-routines-open-spaces")) {
                Text("MY SPACES · NOTES & PHOTOS", color = RecRed)
            }
        }
    }
}
