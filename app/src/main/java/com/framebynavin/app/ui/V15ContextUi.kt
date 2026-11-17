package com.framebynavin.app.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
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
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
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
    onOpenProject: (String) -> Unit = {},
    onOpenWeeklyPlan: () -> Unit = {},
    onOpenCalendar: () -> Unit = {},
    projectsLoaded: Boolean = true,
) {
    val nowMillis = System.currentTimeMillis()
    // Existing canonical priority/recommendation is never replaced.
    val brief = if (projectsLoaded)
        CreatorDailyBriefEngine.build(tasks, weeklySlots, nowMillis) else null
    val sections = if (projectsLoaded)
        UX08DDailyBriefPolicy.build(tasks, weeklySlots, nowMillis) else null

    Surface(Modifier.fillMaxSize(), color = CinemaBlack) {
        Column(
            Modifier.fillMaxSize().windowInsetsPadding(WindowInsets.safeDrawing)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp).padding(bottom = 36.dp),
        ) {
            V15BackHeader("DAILY BRIEF", "What matters today", onClose)
            Spacer(Modifier.height(6.dp))
            Text(
                "A practical look at your saved Creator projects and Weekly Plan. " +
                    "Open the original source to make changes.",
                color = MutedText, fontSize = 11.sp, lineHeight = 16.sp,
                modifier = Modifier.testTag("ux08d-source-explanation"),
            )
            Spacer(Modifier.height(17.dp))
            if (brief == null || sections == null) {
                CircularProgressIndicator(
                    color = RecRed, modifier = Modifier.size(24.dp)
                        .testTag("ux08d-projects-loading"),
                )
                Spacer(Modifier.height(8.dp))
                Text(
                    "Loading Creator projects. An unread project list is not an empty workday.",
                    color = MutedText, fontSize = 12.sp,
                )
            } else {
                Row(
                    Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    V15Metric(
                        "ACTIVE", brief.activeCount.toString(),
                        ProjectorIvory, Modifier.weight(1f),
                    )
                    V15Metric(
                        "DUE DATE TODAY", brief.dueTodayCount.toString(),
                        MutedGold, Modifier.weight(1f),
                    )
                    V15Metric(
                        "PAST DEADLINE", brief.overdueCount.toString(),
                        if (brief.overdueCount > 0) RecRed else SuccessGreen,
                        Modifier.weight(1f),
                    )
                }
                Spacer(Modifier.height(7.dp))
                Text(
                    "Today's deadline count includes items that became overdue earlier today. " +
                        "A deadline is a plan, not proof of publication.",
                    color = MutedText, fontSize = 10.sp, lineHeight = 14.sp,
                )

                Spacer(Modifier.height(22.dp))
                Text(
                    "START HERE", color = RecRed, fontSize = 10.sp,
                    fontWeight = FontWeight.Bold, letterSpacing = 1.1.sp,
                )
                Spacer(Modifier.height(8.dp))
                Surface(
                    onClick = { brief.focusTask?.id?.let(onOpenProject) },
                    enabled = brief.focusTask != null,
                    modifier = Modifier.fillMaxWidth().heightIn(min = 78.dp)
                        .testTag("ux08d-focus-original-project"),
                    shape = RoundedCornerShape(17.dp),
                    color = CinemaSurface,
                    border = BorderStroke(1.dp, CinemaLine.copy(alpha = 0.46f)),
                ) {
                    Column(
                        Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp),
                    ) {
                        Text(
                            brief.focusTask?.title?.ifBlank { "Untitled project" }
                                ?: "Your active Creator project queue is clear",
                            color = ProjectorIvory, fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            maxLines = 2, overflow = TextOverflow.Ellipsis,
                        )
                        Text(
                            brief.focusAction, color = MutedGold,
                            fontSize = 12.sp, lineHeight = 16.sp,
                        )
                        Text(
                            brief.focusReason, color = MutedText,
                            fontSize = 11.sp, lineHeight = 15.sp,
                        )
                        if (brief.focusTask != null) {
                            Text(
                                "OPEN ORIGINAL PROJECT",
                                color = RecRed, fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                            )
                        }
                    }
                }

                if (sections.overdueTotal > 0 || sections.otherAttentionTotal > 0) {
                    Spacer(Modifier.height(21.dp))
                    UX08DBriefSectionHeading(
                        "NEEDS REVIEW",
                        "Past deadlines and other genuine project signals. " +
                            "Your focus project is shown above, not duplicated below.",
                        sections.overdueTotal + sections.otherAttentionTotal,
                        color = if (sections.overdueTotal > 0) RecRed else ProjectorIvory,
                        modifier = Modifier.testTag("ux08d-needs-review"),
                    )
                    Spacer(Modifier.height(9.dp))
                    sections.overdue.forEach { task ->
                        UX08DBriefProjectRow(
                            task = task,
                            prefix = "Past deadline",
                            onOpenProject = onOpenProject,
                        )
                        Spacer(Modifier.height(7.dp))
                    }
                    sections.otherAttention.forEach { nudge ->
                        val accent = when (nudge.level) {
                            CreatorContextNudgeLevel.NOW -> RecRed
                            CreatorContextNudgeLevel.SOON -> MutedGold
                            CreatorContextNudgeLevel.READY -> SuccessGreen
                        }
                        Surface(
                            onClick = { onOpenProject(nudge.taskId) },
                            modifier = Modifier.fillMaxWidth()
                                .heightIn(min = 54.dp)
                                .padding(bottom = 7.dp)
                                .testTag("ux08d-attention-" + nudge.taskId),
                            color = CinemaSurface,
                            shape = RoundedCornerShape(12.dp),
                            border = BorderStroke(
                                1.dp, CinemaLine.copy(alpha = 0.42f),
                            ),
                        ) {
                            Row(
                                Modifier.padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(10.dp),
                            ) {
                                Icon(
                                    if (nudge.level == CreatorContextNudgeLevel.READY)
                                        Icons.Outlined.CheckCircle
                                    else Icons.Outlined.WarningAmber,
                                    contentDescription = null,
                                    tint = accent,
                                    modifier = Modifier.size(18.dp),
                                )
                                Column(Modifier.weight(1f)) {
                                    Text(
                                        nudge.title, color = ProjectorIvory,
                                        fontSize = 12.sp, fontWeight = FontWeight.SemiBold,
                                        maxLines = 2, overflow = TextOverflow.Ellipsis,
                                    )
                                    Text(
                                        nudge.message, color = MutedText,
                                        fontSize = 11.sp, lineHeight = 15.sp,
                                    )
                                    Text(
                                        "OPEN PROJECT · " + nudge.action,
                                        color = accent, fontSize = 10.sp,
                                        maxLines = 2, overflow = TextOverflow.Ellipsis,
                                    )
                                }
                            }
                        }
                    }
                    val reviewVisible = sections.overdue.size +
                        sections.otherAttention.size +
                        (if (sections.focusIsOverdue) 1 else 0)
                    if (sections.overdueTotal + sections.otherAttentionTotal >
                        reviewVisible
                    ) {
                        Text(
                            "Showing the closest priorities. Open your Projects " +
                                "list for remaining saved work.",
                            color = MutedText, fontSize = 11.sp,
                            modifier = Modifier.testTag("ux08d-review-truncated"),
                        )
                    }
                }

                if (sections.dueLaterTodayTotal > 0) {
                    Spacer(Modifier.height(21.dp))
                    UX08DBriefSectionHeading(
                        "DUE LATER TODAY",
                        "Upcoming deadlines for active projects · local date",
                        sections.dueLaterTodayTotal,
                        modifier = Modifier.testTag("ux08d-due-later-today"),
                    )
                    Spacer(Modifier.height(9.dp))
                    sections.dueLaterToday.forEach { task ->
                        UX08DBriefProjectRow(
                            task = task,
                            prefix = "Planned deadline",
                            onOpenProject = onOpenProject,
                        )
                        Spacer(Modifier.height(7.dp))
                    }
                    val dueLaterVisible = sections.dueLaterToday.size +
                        (if (sections.focusIsDueLaterToday) 1 else 0)
                    if (sections.dueLaterTodayTotal > dueLaterVisible) {
                        Text(
                            "Additional due-today projects remain in the original " +
                                "Projects workspace.",
                            color = MutedText, fontSize = 11.sp,
                        )
                    }
                }

                Spacer(Modifier.height(21.dp))
                Row(
                    Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Column(Modifier.weight(1f)) {
                        Text(
                            "COMING UP",
                            color = ProjectorIvory, fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                        )
                        Text(
                            "Tomorrow onward · within the next 7-day window",
                            color = MutedText, fontSize = 11.sp,
                        )
                    }
                    TextButton(
                        onClick = onOpenCalendar,
                        modifier = Modifier.heightIn(min = 48.dp)
                            .testTag("ux08d-open-calendar"),
                    ) { Text("CALENDAR", color = MutedGold, fontSize = 11.sp) }
                }
                Spacer(Modifier.height(5.dp))
                Text(
                    "${brief.reminderCount} configured Creator project reminder times " +
                        "within 24 hours · not delivery confirmations",
                    color = MutedText, fontSize = 11.sp,
                    modifier = Modifier.testTag("ux08d-reminders-source"),
                )
                Spacer(Modifier.height(8.dp))
                if (sections.upcoming.isEmpty()) {
                    Text(
                        "No upcoming saved project deadlines or weekly-plan " +
                            "occurrences in this window.",
                        color = MutedText, fontSize = 12.sp,
                        modifier = Modifier.padding(vertical = 12.dp)
                            .testTag("ux08d-upcoming-empty"),
                    )
                } else {
                    sections.upcoming.forEach { item ->
                        // The original row routes saved projects to Projects
                        // and weekly templates to Weekly Plan; it never
                        // materializes a new task just by being viewed.
                        V15CalendarRow(item, onOpenProject, onOpenWeeklyPlan)
                    }
                    if (sections.upcomingTotal > sections.upcoming.size) {
                        Text(
                            "Showing ${sections.upcoming.size} of " +
                                "${sections.upcomingTotal} upcoming saved dates. " +
                                "View Calendar for the complete window.",
                            color = MutedText, fontSize = 11.sp,
                        )
                    }
                }
                Spacer(Modifier.height(10.dp))
                TextButton(
                    onClick = onOpenWeeklyPlan,
                    modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp)
                        .testTag("ux08d-open-weekly-plan"),
                ) {
                    Text("REVIEW WEEKLY PLAN", color = MutedGold, fontSize = 12.sp)
                }
                if (sections.isEmpty) {
                    Text(
                        "Nothing active or scheduled in these sources yet. " +
                            "Create a project from Quick Actions or plan your first week.",
                        color = MutedText, fontSize = 12.sp, lineHeight = 17.sp,
                        modifier = Modifier.testTag("ux08d-empty-state"),
                    )
                }
            }
        }
    }
}

@Composable
internal fun V15ContentCalendarScreen(
    tasks: List<CreatorTask>,
    weeklySlots: List<WeeklyScheduleSlot>,
    onClose: (() -> Unit)?,
    onOpenProject: (String) -> Unit = {},
    onOpenWeeklyPlan: () -> Unit = {},
    bottomNavigationClearance: androidx.compose.ui.unit.Dp = 124.dp,
    projectsLoaded: Boolean = true,
) {
    val zone = ZoneId.systemDefault()
    val today = LocalDate.now(zone)
    val nowMillis = System.currentTimeMillis()
    var viewName by rememberSaveable { mutableStateOf(UX08ECalendarView.AGENDA.name) }
    val view = runCatching { UX08ECalendarView.valueOf(viewName) }
        .getOrDefault(UX08ECalendarView.AGENDA)
    var selectedDateText by rememberSaveable { mutableStateOf(today.toString()) }
    val selectedDate = UX08ECalendarPolicy.constrained(
        runCatching { LocalDate.parse(selectedDateText) }.getOrDefault(today),
        today,
    )
    var selectedDayOnly by rememberSaveable { mutableStateOf(false) }
    // The date being inspected must NOT shift the Agenda's 14-day window.
    // Period arrows and Today are the only controls that move that window.
    var selectedDayText by rememberSaveable { mutableStateOf<String?>(null) }
    val focusedDay = selectedDayText?.let { date ->
        runCatching { LocalDate.parse(date) }.getOrNull()
    }?.takeIf { UX08ECalendarPolicy.isSelectable(it, today) } ?: selectedDate

    // The engine is the *existing* project/Weekly Plan projection. A 90-day
    // read-only horizon provides actual future month navigation, not 90 writes.
    val upcoming = if (projectsLoaded) CreatorContentCalendarEngine.upcoming(
        tasks, weeklySlots, nowMillis,
        daysAhead = UX08ECalendarPolicy.PLANNING_DAYS,
    ) else emptyList()
    val visiblePeriod = UX08ECalendarPolicy.visibleItems(
        upcoming, view, selectedDate, today, zone,
    )
    val displayed = if (selectedDayOnly)
        UX08ECalendarPolicy.visibleItems(
            upcoming, view, selectedDate, today, zone,
            selectedDayOnly = true,
            focusedDay = focusedDay,
        )
    else visiblePeriod
    val windowCounts = UX08ECalendarPolicy.count(visiblePeriod)
    // The view has a bounded composition cost even with a large library.
    // This does not trim or rewrite the original calendar data.
    val rendered = displayed.take(UX08ECalendarPolicy.MAX_VISIBLE_ROWS)
    val grouped = rendered.groupBy {
        Instant.ofEpochMilli(it.atMillis).atZone(zone).toLocalDate()
    }.toSortedMap()
    val overdue = if (projectsLoaded)
        CreatorCalendarNavigationPolicy.recentOverdue(tasks, nowMillis)
    else emptyList()

    Surface(Modifier.fillMaxSize(), color = CinemaBlack) {
        Column(
            Modifier.fillMaxSize().statusBarsPadding()
                .then(if (onClose == null) Modifier else Modifier.navigationBarsPadding())
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp)
                .padding(bottom = bottomNavigationClearance)
        ) {
            V15BackHeader("CALENDAR", "Content Planner", onClose)
            Spacer(Modifier.height(7.dp))
            Text(
                "See real upcoming project deadlines and recurring Weekly Plan previews. " +
                    "Plans are not published content, and opening a date changes nothing.",
                color = MutedText, fontSize = 11.sp, lineHeight = 16.sp,
                modifier = Modifier.testTag("ux08e-calendar-source-explanation"),
            )
            Spacer(Modifier.height(15.dp))
            if (!projectsLoaded) {
                CircularProgressIndicator(
                    color = RecRed,
                    modifier = Modifier.size(24.dp).testTag("ux08e-calendar-loading"),
                )
                Spacer(Modifier.height(8.dp))
                Text(
                    "Loading Creator projects before displaying your planning calendar.",
                    color = MutedText, fontSize = 12.sp,
                )
            } else {
                UX08ECalendarNavigation(
                    view = view,
                    selected = selectedDate,
                    highlightedDay = if (selectedDayOnly) focusedDay else null,
                    today = today,
                    items = upcoming,
                    zone = zone,
                    onViewChange = { chosen ->
                        viewName = chosen.name
                        selectedDayOnly = false
                        selectedDayText = null
                    },
                    onDateSelect = { date ->
                        selectedDayText = date.toString()
                        selectedDayOnly = true
                    },
                    onMoveTo = { date ->
                        selectedDateText = date.toString()
                        selectedDayText = null
                        selectedDayOnly = false
                    },
                )
                Spacer(Modifier.height(11.dp))
                Row(
                    Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                    ) {
                        Box(Modifier.size(6.dp).background(RecRed, CircleShape))
                        Text("Saved projects", color = MutedText, fontSize = 10.sp)
                    }
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                    ) {
                        Box(Modifier.size(6.dp).background(MutedGold, CircleShape))
                        Text("Plan previews", color = MutedText, fontSize = 10.sp)
                    }
                }
                Spacer(Modifier.height(8.dp))
                Surface(
                    modifier = Modifier.fillMaxWidth()
                        .testTag("ux08e-calendar-count-summary"),
                    color = CinemaSurface,
                    shape = RoundedCornerShape(13.dp),
                ) {
                    Column(
                        Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
                        verticalArrangement = Arrangement.spacedBy(3.dp),
                    ) {
                        Text(
                            "${windowCounts.savedProjects} saved projects · " +
                                "${windowCounts.weeklyPreviews} weekly previews",
                            color = ProjectorIvory, fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold,
                        )
                        Text(
                            "${windowCounts.completedProjects} saved projects " +
                                "marked completed in this viewing period",
                            color = MutedText, fontSize = 10.sp,
                        )
                    }
                }

                if (!selectedDayOnly && overdue.isNotEmpty()) {
                    Spacer(Modifier.height(17.dp))
                    Row(
                        Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Column(Modifier.weight(1f)) {
                            Text(
                                "PAST-DUE PROJECTS",
                                color = RecRed, fontSize = 13.sp,
                                fontWeight = FontWeight.Bold,
                            )
                            Text(
                                "Separate from future planning · saved active projects from the last 14 days",
                                color = MutedText, fontSize = 10.sp,
                            )
                        }
                        Text(
                            overdue.size.toString(),
                            color = MutedGold, fontSize = 11.sp,
                        )
                    }
                    Spacer(Modifier.height(8.dp))
                    overdue.take(6).forEach { task ->
                        V163OverdueProjectRow(task, onOpenProject)
                    }
                    if (overdue.size > 6) {
                        Text(
                            "Showing 6 of ${overdue.size} past-due projects. " +
                                "Open your Projects workspace for the rest.",
                            color = MutedText, fontSize = 11.sp,
                            modifier = Modifier.testTag("ux08e-overdue-truncated"),
                        )
                    }
                }

                Spacer(Modifier.height(18.dp))
                Row(
                    Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Column(Modifier.weight(1f)) {
                        Text(
                            if (selectedDayOnly) "SELECTED DATE" else "PLANNED ITEMS",
                            color = ProjectorIvory, fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                        )
                        Text(
                            if (selectedDayOnly)
                                focusedDay.format(DateTimeFormatter.ofPattern(
                                    "EEE, d MMM yyyy", Locale.getDefault()
                                )) + " · ${displayed.size} recorded plan item(s)"
                            else "${displayed.size} upcoming item(s) in this view",
                            color = MutedText, fontSize = 11.sp,
                        )
                    }
                    if (selectedDayOnly) {
                        TextButton(
                            onClick = {
                                selectedDayOnly = false
                                selectedDayText = null
                            },
                            modifier = Modifier.heightIn(min = 48.dp)
                                .testTag("ux08e-show-full-period"),
                        ) {
                            Text("SHOW PERIOD", color = MutedGold, fontSize = 10.sp)
                        }
                    }
                }
                Spacer(Modifier.height(9.dp))
                if (grouped.isEmpty()) {
                    Surface(
                        modifier = Modifier.fillMaxWidth()
                            .testTag("ux08e-calendar-empty"),
                        color = CinemaSurface,
                        shape = RoundedCornerShape(15.dp),
                    ) {
                        Column(
                            Modifier.padding(19.dp),
                            verticalArrangement = Arrangement.spacedBy(7.dp),
                        ) {
                            Icon(
                                Icons.Outlined.CalendarMonth, null,
                                tint = MutedGold, modifier = Modifier.size(28.dp),
                            )
                            Text(
                                if (selectedDayOnly)
                                    "Nothing planned for this selected date"
                                else "No upcoming items in this viewing period",
                                color = ProjectorIvory, fontSize = 14.sp,
                                fontWeight = FontWeight.Bold,
                            )
                            Text(
                                "This view only projects upcoming saved deadlines " +
                                    "and enabled Weekly Plan occurrences within 90 days. " +
                                    "It does not prove no past publishing activity.",
                                color = MutedText, fontSize = 11.sp,
                                lineHeight = 16.sp,
                            )
                        }
                    }
                } else {
                    grouped.forEach { (date, dayItems) ->
                        Text(
                            when (date) {
                                today -> "TODAY"
                                today.plusDays(1) -> "TOMORROW"
                                else -> date.format(DateTimeFormatter.ofPattern(
                                    "EEE · d MMM", Locale.getDefault()
                                )).uppercase(Locale.getDefault())
                            },
                            color = if (date == today) RecRed else MutedGold,
                            fontSize = 10.sp, fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(top = 8.dp, bottom = 7.dp),
                        )
                        dayItems.forEach { item ->
                            // Existing original row is the only destination owner:
                            // Project -> its saved ID, Preview -> Weekly Plan.
                            V15CalendarRow(item, onOpenProject, onOpenWeeklyPlan)
                        }
                    }
                }
                if (displayed.size > rendered.size) {
                    Spacer(Modifier.height(8.dp))
                    Text(
                        "Showing the first ${rendered.size} of ${displayed.size} " +
                            "upcoming items in this period. Select a date or a " +
                            "shorter view to inspect the remainder; nothing was deleted.",
                        color = MutedText, fontSize = 11.sp,
                        modifier = Modifier.testTag("ux08e-list-truncated"),
                    )
                }
                Spacer(Modifier.height(16.dp))
                TextButton(
                    onClick = onOpenWeeklyPlan,
                    modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp)
                        .testTag("ux08e-review-weekly-plan"),
                ) {
                    Text("REVIEW WEEKLY PLAN", color = MutedGold, fontSize = 12.sp)
                }
                Text(
                    "Upcoming-only planning window · 90 calendar dates from today. " +
                        "Completed project labels reflect saved app state, not verified " +
                        "publication. Earlier historical work belongs in Progress Journey.",
                    color = MutedText, fontSize = 10.sp, lineHeight = 14.sp,
                    modifier = Modifier.testTag("ux08e-window-limitation"),
                )
            }
        }
    }
}

@Composable
private fun V163OverdueProjectRow(task: CreatorTask, onOpenProject: (String) -> Unit) {
    Surface(Modifier.fillMaxWidth().padding(bottom = 7.dp).clickable { onOpenProject(task.id) },
        RoundedCornerShape(16.dp), CinemaSurface,
        border = BorderStroke(1.dp, RecRed.copy(alpha = .4f))) {
        Column(Modifier.padding(14.dp)) {
            Text(task.title, color = ProjectorIvory, fontSize = 14.sp, fontWeight = FontWeight.Bold)
            Text("Overdue project · tap to open", color = RecRed, fontSize = 11.sp)
        }
    }
}

@Composable
private fun V15CalendarRow(
    item: CreatorCalendarItem,
    onOpenProject: (String) -> Unit = {},
    onOpenWeeklyPlan: () -> Unit = {},
) {
    val accent = when {
        item.completed -> SuccessGreen
        item.source == CreatorCalendarSource.WEEKLY_PLAN -> MutedGold
        else -> RecRed
    }
    Surface(
        Modifier.fillMaxWidth().padding(bottom = 7.dp).clickable {
            if (CreatorCalendarNavigationPolicy.isProjectLink(item)) onOpenProject(item.taskId)
            else onOpenWeeklyPlan()
        },
        RoundedCornerShape(16.dp), CinemaSurface, border = BorderStroke(1.dp, CinemaLine),
    ) {
        Row(Modifier.padding(13.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(Modifier.size(8.dp).background(accent, CircleShape))
            Spacer(Modifier.width(10.dp))
            Column(Modifier.weight(1f)) {
                Text(item.title, color = ProjectorIvory, fontSize = 11.5.sp, fontWeight = FontWeight.Bold, maxLines = 2, overflow = TextOverflow.Ellipsis)
                Text("${item.platform} · ${item.contentType}", color = MutedText, fontSize = 10.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Text(CreatorCalendarNavigationPolicy.kindLabel(item) + " · tap to open", color = accent, fontSize = 9.5.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
            Spacer(Modifier.width(8.dp))
            Column(horizontalAlignment = Alignment.End) {
                Text(SimpleDateFormat("h:mm a", Locale.getDefault()).format(Date(item.atMillis)), color = ProjectorIvory, fontSize = 9.sp, fontWeight = FontWeight.Bold)
                Text(if (item.source == CreatorCalendarSource.PROJECT) "PROJECT" else "PLAN", color = accent, fontSize = 9.sp, fontWeight = FontWeight.Bold)
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
private fun V15BackHeader(kicker: String, title: String, onBack: (() -> Unit)?) {
    Row(Modifier.fillMaxWidth().padding(top = 6.dp), verticalAlignment = Alignment.CenterVertically) {
        if (onBack != null) {
            IconButton(onClick = onBack) { Icon(Icons.Outlined.ArrowBack, "Back", tint = ProjectorIvory) }
            Spacer(Modifier.width(4.dp))
        }
        Column {
            Text(kicker, color = RecRed, fontSize = 8.5.sp, fontWeight = FontWeight.Black, letterSpacing = 1.2.sp)
            Text(title, color = ProjectorIvory, fontSize = 23.sp, fontWeight = FontWeight.Black)
        }
    }
}
