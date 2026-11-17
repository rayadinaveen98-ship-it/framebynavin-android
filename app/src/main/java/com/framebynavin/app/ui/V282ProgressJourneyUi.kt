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
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.framebynavin.app.data.*
import com.framebynavin.app.personal.PersonalCompletionEvent
import com.framebynavin.app.personal.PersonalReminderStore
import com.framebynavin.app.ui.theme.*
import java.time.Instant
import java.time.LocalDate
import java.time.YearMonth
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.coroutines.flow.collectLatest

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
    val personalStore = remember(context) { PersonalReminderStore(context.applicationContext) }
    // A sensitive preference is not saveable: re-entry / process recreation
    // must require a fresh explicit decision before showing private history.
    var includePersonal by remember { mutableStateOf(false) }
    var personalHistory by remember { mutableStateOf(emptyList<PersonalCompletionEvent>()) }
    var personalHistoryReady by remember { mutableStateOf(false) }
    var personalHistoryUnavailable by remember { mutableStateOf(false) }
    LaunchedEffect(includePersonal) {
        personalHistory = emptyList()
        personalHistoryReady = false
        personalHistoryUnavailable = false
        if (includePersonal) {
            try {
                personalStore.completionHistoryFlow.collectLatest {
                    personalHistory = it
                    personalHistoryReady = true
                }
            } catch (failure: Exception) {
                if (failure is CancellationException) throw failure
                personalHistory = emptyList()
                personalHistoryReady = false
                personalHistoryUnavailable = true
            }
        }
    }
    var timeline by remember { mutableStateOf(emptyList<CreatorWorkflowTimelineEvent>()) }
    var reading by remember { mutableStateOf(true) }
    var timelineUnavailable by remember { mutableStateOf(false) }
    LaunchedEffect(tasks, ideas) {
        reading = true
        timelineUnavailable = false
        try {
            timeline = withContext(Dispatchers.IO) { store.loadAll() }
        } catch (failure: Exception) {
            if (failure is CancellationException) throw failure
            timeline = emptyList()
            timelineUnavailable = true
        } finally {
            reading = false
        }
    }
    val now = System.currentTimeMillis()
    val zone = remember { ZoneId.systemDefault() }
    val events = remember(tasks, ideas, timeline, includePersonal, personalHistory, now) {
        val creator = ProgressJourneyEngine.build(tasks, ideas, timeline, now)
        if (!includePersonal || !personalHistoryReady || personalHistoryUnavailable) creator else {
            (creator + personalHistory.filter { it.doneAtMillis in 1..now }.distinctBy { it.stableKey }.map { receipt ->
                ProgressJourneyEvent(
                    key = "personal-done:${receipt.stableKey}",
                    kind = ProgressJourneyKind.PERSONAL_COMPLETED,
                    title = "Personal reminder completed",
                    description = "Private · Done",
                    atMillis = receipt.doneAtMillis,
                    sourceId = receipt.reminderId,
                )
            }).sortedByDescending { it.atMillis }
        }
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
    val projectIds = remember(tasks) { tasks.mapTo(HashSet()) { it.id } }
    val ideaIds = remember(ideas) {
        ideas.filterNot { it.isTrashed }.mapTo(HashSet()) { it.id }
    }
    val monthEvents = remember(events, month, zone) {
        UX08BProgressPolicy.forMonth(events, month, zone)
    }

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
            Spacer(Modifier.height(12.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text("Include personal completions", color = ProjectorIvory, fontSize = 12.sp)
                    Text("Private · Off by default · No titles shown", color = MutedText, fontSize = 10.sp)
                }
                Switch(
                    checked = includePersonal,
                    onCheckedChange = {
                        includePersonal = it
                        if (!it) {
                            personalHistory = emptyList()
                            personalHistoryReady = false
                        }
                    },
                    modifier = Modifier.testTag("ux08b-personal-opt-in"),
                )
            }
            Spacer(Modifier.height(15.dp))

            if (reading) {
                CircularProgressIndicator(
                    color = RecRed,
                    modifier = Modifier.size(22.dp).testTag("ux08b-timeline-loading"),
                )
            } else {
                UX08BJourneySummaryStrip(
                    summary = UX08BProgressPolicy.summarize(events, zone),
                    partialCreatorHistory = timelineUnavailable,
                    personalStatus = when {
                        !includePersonal -> null
                        personalHistoryUnavailable ->
                            "Private Personal Done history is unavailable. No stored history was erased."
                        !personalHistoryReady ->
                            "Private Personal Done history is loading; only Creator events are shown so far."
                        else -> null
                    },
                    includePersonal = includePersonal,
                )
            }
            Spacer(Modifier.height(15.dp))

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
                Row(
                    Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        month.format(DateTimeFormatter.ofPattern("MMMM yyyy", Locale.getDefault())),
                        color = ProjectorIvory, fontSize = 17.sp,
                        fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f),
                    )
                    Text(
                        if (reading) "Loading records" else
                            "$datesWithActivity days · ${monthEvents.size} events",
                        color = MutedText, fontSize = 10.sp,
                    )
                }
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    TextButton(
                        onClick = {
                            selectedDateText = today.toString()
                            monthText = YearMonth.from(today).toString()
                        },
                        modifier = Modifier.heightIn(min = 48.dp)
                            .testTag("ux08b-jump-to-today"),
                    ) { Text("TODAY", color = MutedGold, fontSize = 11.sp) }
                    IconButton(
                        onClick = {
                            val (date, period) = UX08BProgressPolicy.shiftPeriod(
                                view, selectedDate, month, -1L,
                            )
                            selectedDateText = date.toString()
                            monthText = period.toString()
                        },
                        modifier = Modifier.testTag("ux08b-previous-period"),
                    ) {
                        Icon(
                            Icons.Outlined.ChevronLeft,
                            if (view == "WEEK") "Previous week" else "Previous month",
                            tint = ProjectorIvory,
                        )
                    }
                    IconButton(
                        onClick = {
                            val (date, period) = UX08BProgressPolicy.shiftPeriod(
                                view, selectedDate, month, 1L,
                            )
                            selectedDateText = date.toString()
                            monthText = period.toString()
                        },
                        modifier = Modifier.testTag("ux08b-next-period"),
                    ) {
                        Icon(
                            Icons.Outlined.ChevronRight,
                            if (view == "WEEK") "Next week" else "Next month",
                            tint = ProjectorIvory,
                        )
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
                    Text(
                        if (reading) "Loading" else "${selectedEvents.size} recorded events",
                        color = MutedGold, fontSize = 11.sp,
                    )
                }
                Spacer(Modifier.height(10.dp))
                if (reading) {
                    CircularProgressIndicator(color = RecRed, modifier = Modifier.size(22.dp))
                } else if (selectedEvents.isEmpty()) {
                    Text(
                        if (timelineUnavailable)
                            "No events in the available records for this day. Workflow timeline history could not be read."
                        else "No recorded events on this day.",
                        color = MutedText, fontSize = 12.sp,
                        modifier = Modifier.padding(vertical = 24.dp)
                            .testTag("ux08b-selected-date-empty"),
                    )
                } else {
                    selectedEvents.forEach { event ->
                        V282ActivityRow(
                            event = event,
                            sourceOpen = UX08BProgressPolicy.canOpen(
                                event, projectIds, ideaIds,
                            ),
                            zone = zone,
                            onOpenProject = onOpenProject,
                            onOpenIdeas = onOpenIdeas,
                        )
                    }
                }
            } else {
                Text("RECORDED MILESTONES", color = MutedGold, fontSize = 11.sp,
                    fontWeight = FontWeight.Bold, letterSpacing = 1.sp)
                Spacer(Modifier.height(12.dp))
                if (reading) {
                    CircularProgressIndicator(color = RecRed)
                } else if (events.isEmpty()) {
                    Text(
                        if (timelineUnavailable)
                            "Workflow timeline history is unavailable. Your saved records were not erased."
                        else "Your journey starts when a project or idea is recorded.",
                        color = MutedText, fontSize = 13.sp,
                        modifier = Modifier.testTag("ux08b-journey-empty"),
                    )
                } else {
                    if (events.size > 100) {
                        Text(
                            "Showing the 100 most recent of ${events.size} recorded events. Earlier history remains saved.",
                            color = MutedText, fontSize = 11.sp,
                            modifier = Modifier.testTag("ux08b-journey-window-info"),
                        )
                    }
                    events.take(100).groupBy { it.localDate(zone) }.forEach { (date, dayEvents) ->
                        Text(
                            date.format(DateTimeFormatter.ofPattern("EEE, d MMM yyyy", Locale.getDefault())),
                            color = MutedGold, fontSize = 12.sp, fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(top = 12.dp, bottom = 6.dp),
                        )
                        dayEvents.forEach { event ->
                            V282ActivityRow(
                                event = event,
                                sourceOpen = UX08BProgressPolicy.canOpen(
                                    event, projectIds, ideaIds,
                                ),
                                zone = zone,
                                onOpenProject = onOpenProject,
                                onOpenIdeas = onOpenIdeas,
                            )
                        }
                    }
                }
            }
            Spacer(Modifier.height(14.dp))
            Text("Personal Done history is private and only shown here after an explicit opt-in. Calendar views never award XP. Photo files and Personal Done history are not included in Space metadata exports.",
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
        modifier = modifier.padding(2.dp).heightIn(min = 49.dp)
            .testTag("ux08b-day-" + date)
            .semantics {
                contentDescription = date.toString() +
                    if (hasActivity) ", recorded activity" else ", no recorded activity"
            },
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
        ProgressJourneyKind.PERSONAL_COMPLETED -> Icons.Outlined.FavoriteBorder
    }
    val personal = event.kind == ProgressJourneyKind.PERSONAL_COMPLETED
    Surface(
        onClick = { if (idea) onOpenIdeas() else if (!personal) onOpenProject(event.sourceId) },
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
            if (!personal) Icon(Icons.Outlined.ChevronRight, "Open source", tint = MutedText, modifier = Modifier.size(18.dp))
        }
    }
}
