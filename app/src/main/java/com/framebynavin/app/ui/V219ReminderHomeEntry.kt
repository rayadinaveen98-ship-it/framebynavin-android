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
import androidx.compose.material.icons.outlined.NotificationsActive
import androidx.compose.material.icons.outlined.ChevronRight
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.framebynavin.app.data.CreatorTask
import com.framebynavin.app.data.ReminderCenterGroup
import com.framebynavin.app.data.ReminderCenterPolicy
import com.framebynavin.app.ui.theme.CinemaLine
import com.framebynavin.app.ui.theme.CinemaSurface
import com.framebynavin.app.ui.theme.MutedGold
import com.framebynavin.app.ui.theme.MutedText
import com.framebynavin.app.ui.theme.ProjectorIvory
import com.framebynavin.app.ui.theme.RecRed

/**
 * Only a doorway into the EXISTING Reminders Center. The same canonical
 * ReminderCenterPolicy that powers that Center computes these counts.
 * Never schedules, snoozes or writes a creator record.
 */
internal data class V219ReminderHomeSnapshot(
    val attention: Int,
    val today: Int,
    val postponed: Int,
    val upcoming: Int,
) {
    val total: Int get() = attention + today + postponed + upcoming
    companion object {
        fun from(tasks: List<CreatorTask>, now: Long): V219ReminderHomeSnapshot {
            val grouped = ReminderCenterPolicy.grouped(tasks, now)
            return V219ReminderHomeSnapshot(
                attention = grouped[ReminderCenterGroup.NEEDS_ATTENTION].orEmpty().size,
                today = grouped[ReminderCenterGroup.TODAY].orEmpty().size,
                postponed = grouped[ReminderCenterGroup.SNOOZED_PAUSED].orEmpty().size,
                upcoming = grouped[ReminderCenterGroup.UPCOMING].orEmpty().size,
            )
        }
    }
}

@Composable
internal fun V219ReminderHomeEntry(
    tasks: List<CreatorTask>,
    onOpenHub: () -> Unit,
    modifier: Modifier = Modifier,
    now: Long = System.currentTimeMillis(),
) {
    val data = V219ReminderHomeSnapshot.from(tasks, now)
    Surface(
        onClick = onOpenHub,
        modifier = modifier.fillMaxWidth().testTag("v219-reminder-home-entry"),
        color = CinemaSurface,
        shape = RoundedCornerShape(18.dp),
        border = BorderStroke(1.dp, if (data.attention > 0) RecRed.copy(alpha = .55f) else CinemaLine),
    ) {
        Column(Modifier.padding(horizontal = 14.dp, vertical = 13.dp),
            verticalArrangement = Arrangement.spacedBy(9.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Outlined.NotificationsActive, contentDescription = null,
                    tint = if (data.attention > 0) RecRed else MutedGold)
                Spacer(Modifier.width(9.dp))
                Column(Modifier.weight(1f)) {
                    Text("REMINDER HUB", color = ProjectorIvory,
                        fontWeight = FontWeight.Black, fontSize = 12.sp)
                    Text(
                        if (data.total == 0) "No active reminders · open to create one"
                        else if (data.attention > 0) "${data.attention} need attention · open to respond"
                        else "${data.total} active · open to manage",
                        color = MutedText, fontSize = 10.sp,
                    )
                }
                Icon(Icons.Outlined.ChevronRight, contentDescription = "Open Reminders Hub",
                    tint = MutedGold)
            }
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                V219ReminderCount("NEEDS", data.attention,
                    if (data.attention > 0) RecRed else MutedText, Modifier.weight(1f))
                V219ReminderCount("TODAY", data.today, MutedGold, Modifier.weight(1f))
                V219ReminderCount("DELAYED", data.postponed, ProjectorIvory, Modifier.weight(1f))
            }
        }
    }
}

@Composable
private fun V219ReminderCount(label: String, count: Int,
    color: androidx.compose.ui.graphics.Color, modifier: Modifier) {
    Column(modifier) {
        Text(count.toString(), color = color, fontSize = 19.sp, fontWeight = FontWeight.Black)
        Text(label, color = MutedText, fontSize = 9.sp, fontWeight = FontWeight.Bold,
            maxLines = 1)
    }
}
