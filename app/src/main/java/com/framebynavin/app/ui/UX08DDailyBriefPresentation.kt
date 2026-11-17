package com.framebynavin.app.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.ChevronRight
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.framebynavin.app.data.CreatorCalendarItem
import com.framebynavin.app.data.CreatorContentCalendarEngine
import com.framebynavin.app.data.CreatorContextNudge
import com.framebynavin.app.data.CreatorContextNudgeEngine
import com.framebynavin.app.data.CreatorPriorityEngine
import com.framebynavin.app.data.CreatorTask
import com.framebynavin.app.data.WeeklyScheduleSlot
import com.framebynavin.app.ui.theme.*
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

/**
 * UX08D: READ-ONLY grouping on top of canonical Creator priority, nudge and
 * weekly-calendar engines. Does NOT change deadlines, schedule alarms, generate
 * tasks from weekly slots or write a new daily snapshot.
 */
internal data class UX08DBriefSections(
    val overdue: List<CreatorTask>,
    val dueLaterToday: List<CreatorTask>,
    val otherAttention: List<CreatorContextNudge>,
    val upcoming: List<CreatorCalendarItem>,
    val overdueTotal: Int,
    val dueLaterTodayTotal: Int,
    val otherAttentionTotal: Int,
    val upcomingTotal: Int,
    val activeTotal: Int,
) {
    val isEmpty: Boolean get() =
        activeTotal == 0 && otherAttentionTotal == 0 && upcomingTotal == 0
}

internal object UX08DDailyBriefPolicy {
    const val MAX_OVERDUE = 3
    const val MAX_DUE_TODAY = 4
    const val MAX_NUDGES = 3
    const val MAX_UPCOMING = 6

    fun build(
        tasks: List<CreatorTask>,
        weeklySlots: List<WeeklyScheduleSlot>,
        nowMillis: Long,
        zone: ZoneId = ZoneId.systemDefault(),
    ): UX08DBriefSections {
        val today = Instant.ofEpochMilli(nowMillis).atZone(zone).toLocalDate()
        val ranked = CreatorPriorityEngine.rankActive(tasks, nowMillis)
        val focusId = ranked.firstOrNull()?.id

        val allOverdue = ranked.filter { it.dueAtMillis > 0 && it.dueAtMillis < nowMillis }
            .sortedBy { it.dueAtMillis }
        val allDueLater = ranked.filter { task ->
            task.dueAtMillis >= nowMillis && task.dueAtMillis > 0 &&
                Instant.ofEpochMilli(task.dueAtMillis).atZone(zone).toLocalDate() == today
        }.sortedBy { it.dueAtMillis }

        // Focus is already the prominent primary action. Do not list it a second
        // time as a separate task card in another section.
        val overdue = allOverdue.filterNot { it.id == focusId }.take(MAX_OVERDUE)
        val dueLaterToday = allDueLater.filterNot { it.id == focusId }.take(MAX_DUE_TODAY)
        val occupiedIds = (allOverdue + allDueLater).mapTo(HashSet()) { it.id }
        if (focusId != null) occupiedIds += focusId

        val allAdditionalNudges = CreatorContextNudgeEngine.nudges(tasks, nowMillis)
            .filterNot { it.taskId in occupiedIds }

        // Use the ORIGINAL de-duplicated projection. A weekly occurrence is not
        // an actual Creator project and must route back to Weekly Plan.
        val startTomorrow = today.plusDays(1)
        val upcoming = CreatorContentCalendarEngine
            .upcoming(tasks, weeklySlots, nowMillis, daysAhead = 7)
            .filter {
                !Instant.ofEpochMilli(it.atMillis).atZone(zone).toLocalDate()
                    .isBefore(startTomorrow)
            }

        return UX08DBriefSections(
            overdue = overdue,
            dueLaterToday = dueLaterToday,
            otherAttention = allAdditionalNudges.take(MAX_NUDGES),
            upcoming = upcoming.take(MAX_UPCOMING),
            overdueTotal = allOverdue.size,
            dueLaterTodayTotal = allDueLater.size,
            otherAttentionTotal = allAdditionalNudges.size,
            upcomingTotal = upcoming.size,
            activeTotal = ranked.size,
        )
    }
}

@Composable
internal fun UX08DBriefSectionHeading(
    title: String,
    subtitle: String,
    count: Int,
    color: Color = ProjectorIvory,
    modifier: Modifier = Modifier,
) {
    Row(modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
            Text(title, color = color, fontSize = 14.sp, fontWeight = FontWeight.Bold)
            Text(subtitle, color = MutedText, fontSize = 11.sp, lineHeight = 15.sp)
        }
        Text(count.toString(), color = MutedGold, fontSize = 12.sp)
    }
}

@Composable
internal fun UX08DBriefProjectRow(
    task: CreatorTask,
    prefix: String,
    nowMillis: Long,
    onOpenProject: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    val zone = ZoneId.systemDefault()
    val dueLabel = if (task.dueAtMillis > 0L) {
        Instant.ofEpochMilli(task.dueAtMillis).atZone(zone).format(
            DateTimeFormatter.ofPattern("EEE, d MMM · h:mm a", Locale.getDefault()),
        )
    } else "No deadline"
    Surface(
        onClick = { onOpenProject(task.id) },
        modifier = modifier.fillMaxWidth().heightIn(min = 54.dp)
            .testTag("ux08d-project-" + task.id),
        shape = RoundedCornerShape(12.dp),
        color = CinemaSurface,
        border = BorderStroke(1.dp, CinemaLine.copy(alpha = 0.4f)),
    ) {
        Row(
            Modifier.padding(horizontal = 13.dp, vertical = 11.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(
                    task.title.ifBlank { "Untitled project" },
                    color = ProjectorIvory, fontSize = 13.sp,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 2, overflow = TextOverflow.Ellipsis,
                )
                Text(prefix + " · " + dueLabel, color = MutedText, fontSize = 11.sp)
            }
            Icon(
                Icons.Outlined.ChevronRight,
                contentDescription = "Open original project",
                tint = MutedGold,
                modifier = Modifier.size(18.dp),
            )
        }
    }
}
