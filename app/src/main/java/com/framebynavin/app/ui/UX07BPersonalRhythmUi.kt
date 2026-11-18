package com.framebynavin.app.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.ChevronRight
import androidx.compose.material.icons.outlined.EventRepeat
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.framebynavin.app.personal.PersonalAgendaPolicy
import com.framebynavin.app.personal.PersonalReminder
import com.framebynavin.app.personal.PersonalRepeat
import com.framebynavin.app.ui.theme.BacklotUX10Tokens
import com.framebynavin.app.ui.theme.CinemaLine
import com.framebynavin.app.ui.theme.CinemaSurface
import com.framebynavin.app.ui.theme.MutedGold
import com.framebynavin.app.ui.theme.MutedText
import com.framebynavin.app.ui.theme.ProjectorIvory
import java.time.Instant
import java.time.format.DateTimeFormatter
import java.util.Locale

/**
 * UX07B: A read-only projection of actual RECURRING Personal reminders.
 * This intentionally is not a second routine store, completion engine or alarm.
 * The caller retains all existing owner create/edit and navigation callbacks.
 */
internal object UX07BRhythmPolicy {
    fun upcoming(reminders: List<PersonalReminder>, nowMillis: Long): List<Pair<PersonalReminder, Long>> =
        reminders.asSequence()
            .filter { !it.paused && it.repeat != PersonalRepeat.ONCE }
            .mapNotNull { reminder ->
                runCatching { PersonalAgendaPolicy.dueAtMillis(reminder, nowMillis) }
                    .getOrNull()?.let { due -> reminder to due }
            }
            .sortedWith(
                compareBy<Pair<PersonalReminder, Long>> { it.second }
                    .thenBy { it.first.title.lowercase(Locale.ROOT) }
                    .thenBy { it.first.id }
            )
            .take(2)
            .toList()
}

/**
 * A quiet My Day shortcut, placed BELOW the existing authoritative Today
 * response card. No speculative streak, implicit Done or duplicated editor.
 */
@Composable
internal fun UX07BPersonalRhythm(
    reminders: List<PersonalReminder>,
    nowMillis: Long,
    enabled: Boolean,
    challengeCount: Int?,
    onNewRoutine: () -> Unit,
    onEditReminder: (PersonalReminder) -> Unit,
    onOpenChallenges: () -> Unit,
    onOpenGroups: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val rhythm = remember(reminders, nowMillis) {
        UX07BRhythmPolicy.upcoming(reminders, nowMillis)
    }
    val recurringCount = remember(reminders) {
        reminders.count { !it.paused && it.repeat != PersonalRepeat.ONCE }
    }

    Column(
        modifier = modifier.fillMaxWidth().testTag("ux07b-personal-rhythm"),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Row(
            Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                Text(
                    "YOUR RHYTHM",
                    color = MutedGold, fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                )
                Text(
                    if (recurringCount == 0) "Make room for the routines that matter."
                    else "$recurringCount active recurring reminder${if (recurringCount == 1) "" else "s"}",
                    color = MutedText, fontSize = 11.sp,
                )
            }
            TextButton(
                onClick = onNewRoutine,
                enabled = enabled,
                modifier = Modifier.heightIn(min = BacklotUX10Tokens.minimumTouchTarget).testTag("ux07b-new-routine"),
            ) {
                Icon(Icons.Outlined.Add, null, Modifier.size(16.dp))
                Spacer(Modifier.size(3.dp))
                Text("NEW", fontSize = 11.sp, fontWeight = FontWeight.Bold)
            }
        }

        if (rhythm.isEmpty()) {
            Text(
                "No upcoming recurring reminders. Create one whenever you're ready.",
                color = MutedText, fontSize = 12.sp,
                modifier = Modifier.testTag("ux07b-rhythm-empty"),
            )
        } else {
            rhythm.forEachIndexed { index, (reminder, due) ->
                Surface(
                    onClick = { onEditReminder(reminder) },
                    enabled = enabled,
                    modifier = Modifier.fillMaxWidth().heightIn(min = BacklotUX10Tokens.rowTarget)
                        .testTag("ux07b-rhythm-reminder-$index"),
                    shape = RoundedCornerShape(12.dp),
                    color = CinemaSurface,
                ) {
                    Row(
                        Modifier.padding(horizontal = 11.dp, vertical = 9.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(9.dp),
                    ) {
                        Icon(
                            Icons.Outlined.EventRepeat, contentDescription = null,
                            tint = MutedGold, modifier = Modifier.size(18.dp),
                        )
                        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                            Text(
                                reminder.title, color = ProjectorIvory,
                                fontSize = 13.sp, fontWeight = FontWeight.SemiBold,
                                maxLines = 1, overflow = TextOverflow.Ellipsis,
                            )
                            Text(
                                Instant.ofEpochMilli(due).atZone(reminder.zoneId)
                                    .format(DateTimeFormatter.ofPattern("EEE, d MMM · h:mm a", Locale.getDefault())),
                                color = MutedText, fontSize = 11.sp,
                                maxLines = 1, overflow = TextOverflow.Ellipsis,
                            )
                        }
                        Icon(
                            Icons.Outlined.ChevronRight,
                            contentDescription = "Edit recurring reminder",
                            tint = MutedText, modifier = Modifier.size(18.dp),
                        )
                    }
                }
            }
        }

        HorizontalDivider(color = CinemaLine.copy(alpha = 0.45f))

        Row(
            Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            TextButton(
                onClick = onOpenChallenges,
                enabled = enabled,
                modifier = Modifier.weight(1f).heightIn(min = BacklotUX10Tokens.minimumTouchTarget)
                    .testTag("ux07b-open-challenges"),
            ) {
                Text(
                    if (challengeCount == null) "21-DAY CHALLENGES"
                    else "CHALLENGES · $challengeCount SAVED",
                    color = ProjectorIvory,
                    fontSize = 10.sp, maxLines = 2,
                )
            }
            TextButton(
                onClick = onOpenGroups,
                enabled = enabled,
                modifier = Modifier.weight(1f).heightIn(min = BacklotUX10Tokens.minimumTouchTarget)
                    .testTag("ux07b-open-groups"),
            ) {
                Text("REMINDER GROUPS", color = ProjectorIvory, fontSize = 10.sp, maxLines = 2)
            }
        }
    }
}
