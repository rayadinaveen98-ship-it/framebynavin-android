package com.framebynavin.app.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.ArrowBack
import androidx.compose.material.icons.outlined.CalendarMonth
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.WarningAmber
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.framebynavin.app.data.*
import com.framebynavin.app.ui.theme.*
import java.text.SimpleDateFormat
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Date
import java.util.Locale

@Composable
internal fun V15DailyBriefScreen(
    tasks: List<CreatorTask>,
    weeklySlots: List<WeeklyScheduleSlot>,
    onClose: () -> Unit,
) {
    val brief = CreatorDailyBriefEngine.build(tasks, weeklySlots)
    Surface(Modifier.fillMaxSize(), color = CinemaBlack) {
        Column(
            Modifier.fillMaxSize().verticalScroll(rememberScrollState()).statusBarsPadding()
                .padding(horizontal = 20.dp).padding(bottom = 42.dp)
        ) {
            V15BackHeader("DAILY BRIEF", "What matters today", onClose)
            Spacer(Modifier.height(18.dp))

            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                V15Metric("ACTIVE", brief.activeCount.toString(), ProjectorIvory, Modifier.weight(1f))
                V15Metric("DUE TODAY", brief.dueTodayCount.toString(), MutedGold, Modifier.weight(1f))
                V15Metric("OVERDUE", brief.overdueCount.toString(), if (brief.overdueCount > 0) RecRed else SuccessGreen, Modifier.weight(1f))
            }

            Spacer(Modifier.height(18.dp))
            Text("FOCUS", color = RecRed, fontSize = 8.5.sp, fontWeight = FontWeight.Black, letterSpacing = 1.1.sp)
            Spacer(Modifier.height(7.dp))
            Surface(Modifier.fillMaxWidth(), RoundedCornerShape(20.dp), CinemaSurface, border = BorderStroke(1.dp, CinemaLine)) {
                Column(Modifier.padding(16.dp)) {
                    Text(
                        brief.focusTask?.title ?: "Your creator queue is clear",
                        color = ProjectorIvory,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Black,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                    )
                    Spacer(Modifier.height(5.dp))
                    Text(brief.focusAction, color = MutedGold, fontSize = 11.sp, lineHeight = 16.sp)
                    Spacer(Modifier.height(5.dp))
                    Text(brief.focusReason, color = MutedText, fontSize = 9.5.sp, lineHeight = 14.sp)
                }
            }

            if (brief.nudges.isNotEmpty()) {
                Spacer(Modifier.height(20.dp))
                Text("NEEDS ATTENTION", color = ProjectorIvory, fontSize = 14.sp, fontWeight = FontWeight.Black)
                Text("Context from deadlines, progress and workflow stage.", color = MutedText, fontSize = 8.8.sp)
                Spacer(Modifier.height(9.dp))
                brief.nudges.forEach { nudge ->
                    val accent = when (nudge.level) {
                        CreatorContextNudgeLevel.NOW -> RecRed
                        CreatorContextNudgeLevel.SOON -> MutedGold
                        CreatorContextNudgeLevel.READY -> SuccessGreen
                    }
                    Surface(
                        Modifier.fillMaxWidth().padding(bottom = 7.dp),
                        RoundedCornerShape(16.dp),
                        CinemaSurface,
                        border = BorderStroke(1.dp, accent.copy(alpha = .35f)),
                    ) {
                        Row(Modifier.padding(13.dp), verticalAlignment = Alignment.Top) {
                            Icon(
                                if (nudge.level == CreatorContextNudgeLevel.READY) Icons.Outlined.CheckCircle else Icons.Outlined.WarningAmber,
                                null,
                                tint = accent,
                                modifier = Modifier.size(18.dp),
                            )
                            Spacer(Modifier.width(9.dp))
                            Column(Modifier.weight(1f)) {
                                Text(nudge.title, color = ProjectorIvory, fontSize = 11.5.sp, fontWeight = FontWeight.Bold)
                                Text(nudge.message, color = MutedText, fontSize = 8.8.sp, lineHeight = 13.sp)
                                Spacer(Modifier.height(3.dp))
                                Text(nudge.action, color = accent, fontSize = 8.8.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }

            Spacer(Modifier.height(20.dp))
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Text("NEXT 7 DAYS", color = ProjectorIvory, fontSize = 14.sp, fontWeight = FontWeight.Black)
                Spacer(Modifier.weight(1f))
                Text("${brief.reminderCount} reminders in 24h", color = MutedText, fontSize = 8.5.sp)
            }
            Spacer(Modifier.height(8.dp))
            if (brief.calendar.isEmpty()) {
                Text("Nothing scheduled yet.", color = MutedText, fontSize = 9.5.sp)
            } else {
                brief.calendar.forEach { item -> V15CalendarRow(item) }
            }
        }
    }
}

@Composable
internal fun V15ContentCalendarScreen(
    tasks: List<CreatorTask>,
    weeklySlots: List<WeeklyScheduleSlot>,
    onClose: () -> Unit,
) {
    val items = CreatorContentCalendarEngine.upcoming(tasks, weeklySlots, daysAhead = 14)
    val grouped = CreatorContentCalendarEngine.groupedByDate(items)
    val zone = ZoneId.systemDefault()
    val today = LocalDate.now(zone)

    Surface(Modifier.fillMaxSize(), color = CinemaBlack) {
        Column(
            Modifier.fillMaxSize().verticalScroll(rememberScrollState()).statusBarsPadding()
                .padding(horizontal = 20.dp).padding(bottom = 42.dp)
        ) {
            V15BackHeader("CONTENT CALENDAR", "The next 14 days", onClose)
            Spacer(Modifier.height(8.dp))
            Text(
                "Projects and enabled weekly slots in one timeline. Weekly items already created as projects are shown only once.",
                color = MutedText,
                fontSize = 9.2.sp,
                lineHeight = 14.sp,
            )
            Spacer(Modifier.height(18.dp))

            if (grouped.isEmpty()) {
                Surface(Modifier.fillMaxWidth(), RoundedCornerShape(20.dp), CinemaSurface, border = BorderStroke(1.dp, CinemaLine)) {
                    Column(Modifier.padding(24.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(Icons.Outlined.CalendarMonth, null, tint = MutedGold, modifier = Modifier.size(30.dp))
                        Spacer(Modifier.height(9.dp))
                        Text("Calendar is clear", color = ProjectorIvory, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                        Text("Add project deadlines or enable Weekly Plan slots.", color = MutedText, fontSize = 9.sp)
                    }
                }
            } else {
                grouped.forEach { (date, dayItems) ->
                    val label = when (date) {
                        today -> "TODAY"
                        today.plusDays(1) -> "TOMORROW"
                        else -> date.format(DateTimeFormatter.ofPattern("EEE · d MMM", Locale.getDefault())).uppercase(Locale.getDefault())
                    }
                    Text(label, color = if (date == today) RecRed else MutedGold, fontSize = 8.8.sp, fontWeight = FontWeight.Black, letterSpacing = 1.05.sp)
                    Spacer(Modifier.height(7.dp))
                    dayItems.forEach { item -> V15CalendarRow(item) }
                    Spacer(Modifier.height(10.dp))
                }
            }
        }
    }
}

@Composable
private fun V15CalendarRow(item: CreatorCalendarItem) {
    val accent = when {
        item.completed -> SuccessGreen
        item.source == CreatorCalendarSource.WEEKLY_PLAN -> MutedGold
        else -> RecRed
    }
    Surface(
        Modifier.fillMaxWidth().padding(bottom = 7.dp),
        RoundedCornerShape(16.dp),
        CinemaSurface,
        border = BorderStroke(1.dp, CinemaLine),
    ) {
        Row(Modifier.padding(13.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(Modifier.size(8.dp).background(accent, CircleShape))
            Spacer(Modifier.width(10.dp))
            Column(Modifier.weight(1f)) {
                Text(item.title, color = ProjectorIvory, fontSize = 11.5.sp, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Text("${item.platform} · ${item.contentType} · ${item.stageLabel}", color = MutedText, fontSize = 8.5.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
            Spacer(Modifier.width(8.dp))
            Column(horizontalAlignment = Alignment.End) {
                Text(SimpleDateFormat("h:mm a", Locale.getDefault()).format(Date(item.atMillis)), color = ProjectorIvory, fontSize = 9.sp, fontWeight = FontWeight.Bold)
                Text(if (item.source == CreatorCalendarSource.PROJECT) "PROJECT" else "WEEKLY", color = accent, fontSize = 7.5.sp, fontWeight = FontWeight.Black)
            }
        }
    }
}

@Composable
private fun V15Metric(label: String, value: String, accent: Color, modifier: Modifier) {
    Surface(modifier, RoundedCornerShape(16.dp), CinemaSurface, border = BorderStroke(1.dp, CinemaLine)) {
        Column(Modifier.padding(12.dp)) {
            Text(value, color = accent, fontSize = 22.sp, fontWeight = FontWeight.Black)
            Text(label, color = MutedText, fontSize = 7.4.sp, lineHeight = 9.sp)
        }
    }
}

@Composable
private fun V15BackHeader(kicker: String, title: String, onBack: () -> Unit) {
    Row(Modifier.fillMaxWidth().padding(top = 6.dp), verticalAlignment = Alignment.CenterVertically) {
        IconButton(onClick = onBack) { Icon(Icons.Outlined.ArrowBack, "Back", tint = ProjectorIvory) }
        Spacer(Modifier.width(4.dp))
        Column {
            Text(kicker, color = RecRed, fontSize = 8.5.sp, fontWeight = FontWeight.Black, letterSpacing = 1.2.sp)
            Text(title, color = ProjectorIvory, fontSize = 23.sp, fontWeight = FontWeight.Black)
        }
    }
}
