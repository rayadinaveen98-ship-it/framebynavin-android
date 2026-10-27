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
import androidx.compose.material.icons.outlined.CheckCircleOutline
import androidx.compose.material.icons.outlined.History
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.framebynavin.app.data.CreatorTask
import com.framebynavin.app.reminders.ReminderAcknowledgementStore
import com.framebynavin.app.ui.theme.CinemaLine
import com.framebynavin.app.ui.theme.CinemaSurface
import com.framebynavin.app.ui.theme.MutedGold
import com.framebynavin.app.ui.theme.MutedText
import com.framebynavin.app.ui.theme.ProjectorIvory

/**
 * Read-only evidence from existing stores. No scheduling, acknowledgment,
 * writes or upgrades. Journal keeps up to 12 occurrences per known task and
 * may be cleared across restores; this is not a lifetime activity ledger.
 */
@Composable
internal fun V2193ReminderHistorySection(
    tasks: List<CreatorTask>,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val journal = remember(context) {
        ReminderAcknowledgementStore(context.applicationContext)
    }
    val records = tasks.associate { it.id to journal.recent(it.id) }
    val responses = V2193ReminderHistoryPolicy.confirmedResponses(tasks, records)
    val completed = V2193ReminderHistoryPolicy.completedProjects(tasks)

    Column(
        modifier = modifier.fillMaxWidth().testTag("v2193-history-section"),
        verticalArrangement = Arrangement.spacedBy(13.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Outlined.History, null, tint = MutedGold)
            Spacer(Modifier.width(8.dp))
            Text("RECORDED HISTORY", color = ProjectorIvory,
                fontWeight = FontWeight.Black, fontSize = 15.sp)
        }
        Text(
            "Backlot shows only confirmed responses and saved project completions. " +
                "A delivered notification is not proof you saw it.",
            color = MutedText, fontSize = 11.sp, lineHeight = 16.sp,
        )

        Text("CONFIRMED REMINDER RESPONSES", color = MutedGold,
            fontWeight = FontWeight.Bold, fontSize = 11.sp)
        Surface(
            modifier = Modifier.fillMaxWidth().testTag("v2193-confirmed-responses"),
            color = CinemaSurface, shape = RoundedCornerShape(16.dp),
            border = BorderStroke(1.dp, CinemaLine),
        ) {
            Column(Modifier.padding(13.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                if (responses.isEmpty()) {
                    Text("No confirmed reminder responses recorded for these projects.",
                        color = MutedText, fontSize = 12.sp)
                } else responses.forEachIndexed { index, entry ->
                    if (index > 0) HorizontalDivider(color = CinemaLine)
                    Text(entry.projectTitle, color = ProjectorIvory,
                        fontWeight = FontWeight.Bold, fontSize = 12.sp,
                        maxLines = 2, overflow = TextOverflow.Ellipsis)
                    Text("${entry.action} · ${pFormatDateTime(entry.atMillis)}",
                        color = MutedText, fontSize = 11.sp)
                }
            }
        }

        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Outlined.CheckCircleOutline, null, tint = MutedGold)
            Spacer(Modifier.width(7.dp))
            Text("COMPLETED PROJECTS", color = MutedGold,
                fontWeight = FontWeight.Bold, fontSize = 11.sp)
        }
        Surface(
            modifier = Modifier.fillMaxWidth().testTag("v2193-completed-projects"),
            color = CinemaSurface, shape = RoundedCornerShape(16.dp),
            border = BorderStroke(1.dp, CinemaLine),
        ) {
            Column(Modifier.padding(13.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                if (completed.isEmpty()) {
                    Text("No completed projects found in the saved project list.",
                        color = MutedText, fontSize = 12.sp)
                } else completed.forEachIndexed { index, entry ->
                    if (index > 0) HorizontalDivider(color = CinemaLine)
                    Text(entry.title, color = ProjectorIvory,
                        fontWeight = FontWeight.Bold, fontSize = 12.sp,
                        maxLines = 2, overflow = TextOverflow.Ellipsis)
                    Text(
                        if (entry.timeVerified) "Completed · ${pFormatDateTime(entry.completedAtMillis)}"
                        else "Completed · date not recorded in older project data",
                        color = MutedText, fontSize = 11.sp,
                    )
                }
            }
        }

        Text(
            "Reminder responses are kept locally for recent occurrences (up to 12 per project) " +
                "and may be reset on restore. Dismissed or snoozed does not mean a project is complete. " +
                "This view does not reconstruct missing history.",
            color = MutedText, fontSize = 10.sp, lineHeight = 15.sp,
        )
    }
}
