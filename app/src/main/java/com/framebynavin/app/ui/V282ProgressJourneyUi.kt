package com.framebynavin.app.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.framebynavin.app.data.*
import com.framebynavin.app.ui.theme.*
import java.time.LocalDate
import java.time.YearMonth
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Progress is what actually happened. Content Calendar remains a separate
 * forward-looking schedule. This screen reads evidence and NEVER writes tasks.
 */
@Composable
internal fun V282ProgressJourneyScreen(
    tasks: List<CreatorTask>,
    ideas: List<CreatorIdea>,
    onClose: () -> Unit,
    onOpenProject: (String) -> Unit,
    onOpenIdeas: () -> Unit,
) {
    val context = androidx.compose.ui.platform.LocalContext.current
    val store = remember(context) { CreatorWorkflowTimelineStore(context.applicationContext) }
    var timeline by remember { mutableStateOf(emptyList<CreatorWorkflowTimelineEvent>()) }
    var reading by remember { mutableStateOf(true) }
    LaunchedEffect(tasks, ideas) {
        reading = true
        timeline = withContext(Dispatchers.IO) { store.loadAll() }
        reading = false
    }
    val now = System.currentTimeMillis()
    val zone = remember { ZoneId.systemDefault() }
    val events = remember(tasks, ideas, timeline) {
        ProgressJourneyEngine.build(tasks, ideas, timeline, now)
    }
    val today = remember { LocalDate.now(zone) }
    var selectedDateText by rememberSaveable { mutableStateOf(today.toString()) }
    var monthText by rememberSaveable { mutableStateOf(YearMonth.from(today).toString()) }
    var view by rememberSaveable { mutableStateOf("WEEK") }
    val selectedDate = remember(selectedDateText) { LocalDate.parse(selectedDateText) }
    val month = remember(monthText) { YearMonth.parse(monthText) }
    val activeDays = remember(events, zone) { ProgressJourneyEngine.activeDays(events, zone) }
    val selectedEvents = remember(events, selectedDate, zone) {
        ProgressJourneyEngine.onDay(events, selectedDate, zone)
    }
    val datesWithActivity = activeDays.count { YearMonth.from(it) == month }

    Surface(modifier = Modifier.fillMaxSize(), color = CinemaBlack) {
        Column(
            Modifier.fillMaxSize()
                .windowInsetsPadding(WindowInsets.safeDrawing)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp)
                .padding(bottom = 42.dp),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = onClose) {
                    Icon(Icons.Outlined.ArrowBack, contentDescription = "Back", tint = ProjectorIvory)
                }
                Column(Modifier.weight(1f)) {
                    Text("YOUR JOURNEY", color = RecRed, fontSize = 10.sp, fontWeight = FontWeight.Bold,
                        letterSpacing = 1.sp)
                    Text("Progress calendar", color = ProjectorIvory, fontSize = 23.sp,
                        fontWeight = FontWeight.Bold)
                }
            }
            Spacer(Modifier.height(8.dp))
            Text("The work you've actually done, day by day.", color = MutedText, fontSize = 12.sp)
            Spacer(Modifier.height(20.dp))

            Row(horizontalArrangement = Arrangement.spacedBy(6.dp), modifier = Modifier.fillMaxWidth()) {
                listOf("WEEK" to "Compact", "MONTH" to "Month", "JOURNEY" to "Journey").forEach { (key, label) ->
                    FilterChip(
                        selected = view == key,
                        onClick = { view = key },
                        label = { Text(label) },
                        modifier = Modifier.weight(1f),
                    )
                }
            }
            Spacer(Modifier.height(12.dp))
            if (view != "JOURNEY") {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(month.format(DateTimeFormatter.ofPattern("MMMM yyyy", Locale.getDefault())),
                        color = ProjectorIvory, fontSize = 17.sp, fontWeight = FontWeight.Bold,
                        modifier = Modifier.weight(1f))
                    Text("$datesWithActivity active days", color = MutedText, fontSize = 10.sp)
                    IconButton(onClick = {
                        val previous = month.minusMonths(1)
                        monthText = previous.toString()
                        selectedDateText = previous.atDay(1).toString()
                    }) {
                        Icon(Icons.Outlined.ChevronLeft, "Previous month", tint = ProjectorIvory)
                    }
                    IconButton(onClick = {
                        val next = month.plusMonths(1)
                        monthText = next.toString()
                        selectedDateText = next.atDay(1).toString()
                    }) {
                        Icon(Icons.Outlined.ChevronRight, "Next month", tint = ProjectorIvory)
                    }
                }
                if (view == "WEEK") {
                    val monday = selectedDate.minusDays((selectedDate.dayOfWeek.value - 1).toLong())
                    V282WeekHeader()
                    Row(Modifier.fillMaxWidth()) {
                        repeat(7) { offset ->
                            val day = monday.plusDays(offset.toLong())
                            V282DayCell(day, selectedDate, today, day in activeDays, day.month == month.month,
                                Modifier.weight(1f)) { selectedDateText = day.toString(); monthText = YearMonth.from(day).toString() }
                        }
                    }
                } else {
                    V282WeekHeader()
                    val first = month.atDay(1)
                    val offset = first.dayOfWeek.value - 1
                    repeat(6) { row ->
                        Row(Modifier.fillMaxWidth()) {
                            repeat(7) { column ->
                                val day = first.plusDays((row * 7 + column - offset).toLong())
                                V282DayCell(day, selectedDate, today, day in activeDays,
                                    YearMonth.from(day) == month, Modifier.weight(1f)) {
                                    selectedDateText = day.toString()
                                    if (YearMonth.from(day) != month) monthText = YearMonth.from(day).toString()
                                }
                            }
                        }
                    }
                }
                Spacer(Modifier.height(15.dp))
                HorizontalDivider(color = CinemaLine)
                Spacer(Modifier.height(15.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(selectedDate.format(DateTimeFormatter.ofPattern("EEE, d MMM", Locale.getDefault())),
                        color = ProjectorIvory, fontSize = 16.sp, fontWeight = FontWeight.Bold,
                        modifier = Modifier.weight(1f))
                    Text("${selectedEvents.size} activities", color = MutedGold, fontSize = 11.sp)
                }
                Spacer(Modifier.height(10.dp))
                if (reading) {
                    CircularProgressIndicator(color = RecRed, modifier = Modifier.size(22.dp))
                } else if (selectedEvents.isEmpty()) {
                    Text("No recorded activity on this day.", color = MutedText, fontSize = 12.sp,
                        modifier = Modifier.padding(vertical = 24.dp))
                } else {
                    selectedEvents.forEach { event -> V282ActivityRow(event, onOpenProject, onOpenIdeas) }
                }
            } else {
                Text("RECORDED MILESTONES", color = MutedGold, fontSize = 11.sp,
                    fontWeight = FontWeight.Bold, letterSpacing = 1.sp)
                Spacer(Modifier.height(12.dp))
                if (reading) {
                    CircularProgressIndicator(color = RecRed)
                } else if (events.isEmpty()) {
                    Text("Your journey starts with your next project or idea.",
                        color = MutedText, fontSize = 13.sp)
                } else {
                    events.take(100).groupBy { it.localDate(zone) }.forEach { (date, dayEvents) ->
                        Text(date.format(DateTimeFormatter.ofPattern("EEE, d MMM yyyy", Locale.getDefault())),
                            color = MutedGold, fontSize = 12.sp, fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(top = 12.dp, bottom = 6.dp))
                        dayEvents.forEach { event -> V282ActivityRow(event, onOpenProject, onOpenIdeas) }
                    }
                }
            }
            Spacer(Modifier.height(14.dp))
            Text("Creator history only. Personal Reminders stay private and are not counted until their completed-action history is available.",
                color = MutedText, fontSize = 10.sp, lineHeight = 14.sp)
        }
    }
}

@Composable
private fun V282WeekHeader() {
    Row(Modifier.fillMaxWidth().padding(vertical = 5.dp)) {
        listOf("M", "T", "W", "T", "F", "S", "S").forEach { label ->
            Box(Modifier.weight(1f), contentAlignment = Alignment.Center) {
                Text(label, color = MutedText, fontSize = 10.sp)
            }
        }
    }
}

@Composable
private fun V282DayCell(
    date: LocalDate,
    selected: LocalDate,
    today: LocalDate,
    hasActivity: Boolean,
    inMonth: Boolean,
    modifier: Modifier = Modifier,
    onClick: () -> Unit,
) {
    val active = date == selected
    Surface(
        modifier = modifier.padding(2.dp).heightIn(min = 49.dp),
        shape = RoundedCornerShape(12.dp),
        color = if (active) CinemaSurfaceRaised else Color.Transparent,
        border = if (active) BorderStroke(1.dp, RecRed) else null,
        onClick = onClick,
    ) {
        Column(Modifier.padding(vertical = 7.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Text(date.dayOfMonth.toString(), color = when {
                !inMonth -> MutedText.copy(alpha = .45f)
                active -> ProjectorIvory
                date == today -> RecRed
                else -> ProjectorIvory
            }, fontSize = 12.sp, fontWeight = if (active || date == today) FontWeight.Bold else FontWeight.Normal)
            Spacer(Modifier.height(5.dp))
            Box(Modifier.size(5.dp).background(
                if (hasActivity) SuccessGreen else Color.Transparent, CircleShape))
        }
    }
}

@Composable
private fun V282ActivityRow(
    event: ProgressJourneyEvent,
    onOpenProject: (String) -> Unit,
    onOpenIdeas: () -> Unit,
) {
    val idea = event.kind == ProgressJourneyKind.IDEA_CAPTURED
    val icon = when (event.kind) {
        ProgressJourneyKind.IDEA_CAPTURED -> Icons.Outlined.Lightbulb
        ProgressJourneyKind.PROJECT_CREATED -> Icons.Outlined.FolderOpen
        ProgressJourneyKind.STAGE_MOVED -> Icons.Outlined.SwapHoriz
        ProgressJourneyKind.PROJECT_COMPLETED -> Icons.Outlined.CheckCircle
        ProgressJourneyKind.PROJECT_PUBLISHED -> Icons.Outlined.Movie
    }
    Surface(
        onClick = { if (idea) onOpenIdeas() else onOpenProject(event.sourceId) },
        modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp),
        shape = RoundedCornerShape(15.dp),
        color = CinemaSurface,
        border = BorderStroke(1.dp, CinemaLine),
    ) {
        Row(Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
            Icon(icon, null, tint = if (idea) MutedGold else SuccessGreen, modifier = Modifier.size(19.dp))
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text(event.title, color = ProjectorIvory, fontSize = 13.sp, fontWeight = FontWeight.Bold,
                    maxLines = 2, overflow = TextOverflow.Ellipsis)
                Text(event.description, color = MutedText, fontSize = 11.sp)
            }
            Icon(Icons.Outlined.ChevronRight, "Open source", tint = MutedText, modifier = Modifier.size(18.dp))
        }
    }
}
