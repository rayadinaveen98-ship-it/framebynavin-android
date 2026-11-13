package com.framebynavin.app.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.AutoAwesome
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.material3.Icon
import com.framebynavin.app.personal.PersonalAgendaSection
import com.framebynavin.app.personal.V337AgendaEntry
import com.framebynavin.app.ui.theme.CinemaLine
import com.framebynavin.app.ui.theme.CinemaSurface
import com.framebynavin.app.ui.theme.MutedText
import com.framebynavin.app.ui.theme.ProjectorIvory
import com.framebynavin.app.ui.theme.RecRed
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

/**
 * A navigation-only projection of the real challenge schedule. Never calls
 * PersonalReminderCommands, adds a PersonalReminder or sends an alarm.
 */
@Composable
internal fun V337ChallengeReminderRow(
    entry: V337AgendaEntry.Challenge,
    onOpenChallenge: (String) -> Unit,
) {
    val challenge = entry.record
    val whenText = entry.dueAtMillis?.let {
        Instant.ofEpochMilli(it).atZone(ZoneId.of(challenge.zoneId))
            .format(DateTimeFormatter.ofPattern("EEE, d MMM · h:mm a", Locale.getDefault()))
    }
    val status = when {
        challenge.archived -> "Archived · alerts stopped · history retained"
        challenge.paused -> "Paused · resume in 21-Day Challenge"
        entry.section == PersonalAgendaSection.NO_UPCOMING ->
            "No more scheduled alerts in this 21-day window"
        entry.section == PersonalAgendaSection.NEEDS_ATTENTION ->
            "Activity not marked Done · scheduled $whenText"
        else -> "Scheduled $whenText"
    }
    Surface(
        color = CinemaSurface,
        shape = RoundedCornerShape(16.dp),
        border = BorderStroke(1.dp,
            if (entry.section == PersonalAgendaSection.NEEDS_ATTENTION)
                RecRed.copy(alpha = .55f) else CinemaLine),
        modifier = Modifier.fillMaxWidth().testTag("v337-challenge-reminder"),
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Outlined.AutoAwesome, contentDescription = null, tint = RecRed)
                Spacer(Modifier.width(12.dp))
                Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                    Text(challenge.activity, color = ProjectorIvory, fontSize = 14.sp,
                        fontWeight = FontWeight.SemiBold, maxLines = 2,
                        overflow = TextOverflow.Ellipsis)
                    Text("21-DAY CHALLENGE · " +
                        (challenge.alertKind?.name ?: "OFF"),
                        color = RecRed, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                    Text(status, color = MutedText, fontSize = 11.sp)
                }
                TextButton(
                    onClick = { onOpenChallenge(challenge.id) },
                    modifier = Modifier.testTag("v337-open-challenge"),
                ) { Text("OPEN", color = ProjectorIvory, fontSize = 11.sp) }
            }
            if (entry.section == PersonalAgendaSection.NEEDS_ATTENTION) {
                Text("Not marked Done is not proof that a notification failed. " +
                    "Open the challenge to check progress or Alert Health.",
                    color = MutedText, fontSize = 10.sp)
            }
            Text("Manage Edit, Delete, Archive, Done and Snooze inside the originating " +
                "challenge. This row does not schedule a second alarm.",
                color = MutedText, fontSize = 10.sp)
        }
    }
}
