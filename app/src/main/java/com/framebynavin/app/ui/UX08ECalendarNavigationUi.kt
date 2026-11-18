package com.framebynavin.app.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.ChevronLeft
import androidx.compose.material.icons.outlined.ChevronRight
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.framebynavin.app.data.CreatorCalendarItem
import com.framebynavin.app.data.CreatorCalendarSource
import com.framebynavin.app.ui.theme.*
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.YearMonth
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

/** A navigation-only surface. All click actions are local filters, not writes. */
@Composable
internal fun UX08ECalendarNavigation(
    view: UX08ECalendarView,
    selected: LocalDate,
    highlightedDay: LocalDate?,
    today: LocalDate,
    items: List<CreatorCalendarItem>,
    zone: ZoneId,
    onViewChange: (UX08ECalendarView) -> Unit,
    onDateSelect: (LocalDate) -> Unit,
    onMoveTo: (LocalDate) -> Unit,
    modifier: Modifier = Modifier,
) {
    val window = UX08ECalendarPolicy.window(view, selected, today)
    val previous = UX08ECalendarPolicy.shift(view, selected, today, -1L)
    val next = UX08ECalendarPolicy.shift(view, selected, today, 1L)
    Column(
        modifier = modifier.fillMaxWidth().testTag("ux08e-calendar-navigation"),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Row(
            Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            UX08ECalendarView.entries.forEach { candidate ->
                FilterChip(
                    selected = view == candidate,
                    onClick = { onViewChange(candidate) },
                    label = { Text(candidate.name, fontSize = 10.sp) },
                    modifier = Modifier.weight(1f)
                        .heightIn(min = 48.dp)
                        .testTag("ux08e-mode-" + candidate.name.lowercase()),
                )
            }
        }
        Row(
            Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(Modifier.weight(1f)) {
                Text(
                    when (view) {
                        UX08ECalendarView.MONTH ->
                            YearMonth.from(selected).format(
                                DateTimeFormatter.ofPattern("MMMM yyyy", Locale.getDefault())
                            )
                        UX08ECalendarView.WEEK ->
                            window.start.format(DateTimeFormatter.ofPattern(
                                "d MMM", Locale.getDefault()
                            )) + " – " + window.endInclusive.format(
                                DateTimeFormatter.ofPattern("d MMM yyyy", Locale.getDefault())
                            )
                        UX08ECalendarView.AGENDA ->
                            "NEXT 14 DAYS"
                    },
                    color = ProjectorIvory,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.SemiBold,
                )
                if (view == UX08ECalendarView.AGENDA) {
                    Text(
                        window.start.format(DateTimeFormatter.ofPattern(
                            "d MMM yyyy", Locale.getDefault()
                        )) + " onward",
                        color = MutedText, fontSize = 11.sp,
                    )
                }
            }
            TextButton(
                onClick = { onMoveTo(today) },
                modifier = Modifier.heightIn(min = 48.dp)
                    .testTag("ux08e-calendar-today"),
            ) { Text("TODAY", color = MutedGold, fontSize = 10.sp) }
            IconButton(
                onClick = { previous?.let(onMoveTo) },
                enabled = previous != null,
                modifier = Modifier.testTag("ux08e-calendar-previous"),
            ) {
                Icon(Icons.Outlined.ChevronLeft, "Previous " + view.name.lowercase(),
                    tint = if (previous != null) ProjectorIvory else MutedText)
            }
            IconButton(
                onClick = { next?.let(onMoveTo) },
                enabled = next != null,
                modifier = Modifier.testTag("ux08e-calendar-next"),
            ) {
                Icon(Icons.Outlined.ChevronRight, "Next " + view.name.lowercase(),
                    tint = if (next != null) ProjectorIvory else MutedText)
            }
        }

        when (view) {
            UX08ECalendarView.WEEK -> {
                Row(
                    Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(3.dp),
                ) {
                    (0L..6L).forEach { offset ->
                        val date = window.start.plusDays(offset)
                        UX08EDateCell(
                            date = date, selected = highlightedDay ?: selected,
                            today = today, zone = zone, items = items,
                            inViewedMonth = true, onDateSelect = onDateSelect,
                            modifier = Modifier.weight(1f),
                        )
                    }
                }
            }
            UX08ECalendarView.MONTH -> {
                Row(Modifier.fillMaxWidth()) {
                    listOf("M", "T", "W", "T", "F", "S", "S").forEach { initial ->
                        Text(
                            initial, color = MutedText, fontSize = 10.sp,
                            modifier = Modifier.weight(1f),
                            textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                        )
                    }
                }
                UX08ECalendarPolicy.monthGrid(YearMonth.from(selected))
                    .chunked(7).forEach { week ->
                        Row(
                            Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(3.dp),
                        ) {
                            week.forEach { date ->
                                UX08EDateCell(
                                    date = date,
                                    selected = highlightedDay ?: selected,
                                    today = today, zone = zone, items = items,
                                    inViewedMonth = YearMonth.from(date) ==
                                        YearMonth.from(selected),
                                    onDateSelect = onDateSelect,
                                    modifier = Modifier.weight(1f),
                                )
                            }
                        }
                    }
            }
            UX08ECalendarView.AGENDA -> {
                Row(
                    Modifier.fillMaxWidth().horizontalScroll(
                        androidx.compose.foundation.rememberScrollState()
                    ),
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    (0L until UX08ECalendarPolicy.AGENDA_DAYS).forEach { offset ->
                        val date = window.start.plusDays(offset)
                        if (UX08ECalendarPolicy.isSelectable(date, today)) {
                            UX08EAgendaDateChip(
                                date = date,
                                selected = highlightedDay ?: selected,
                                zone = zone,
                                items = items,
                                onDateSelect = onDateSelect,
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun UX08EDateCell(
    date: LocalDate,
    selected: LocalDate,
    today: LocalDate,
    zone: ZoneId,
    items: List<CreatorCalendarItem>,
    inViewedMonth: Boolean,
    onDateSelect: (LocalDate) -> Unit,
    modifier: Modifier = Modifier,
) {
    val enabled = inViewedMonth && UX08ECalendarPolicy.isSelectable(date, today)
    val onDate = items.filter {
        java.time.Instant.ofEpochMilli(it.atMillis).atZone(zone).toLocalDate() == date
    }
    val hasProject = onDate.any { it.source == CreatorCalendarSource.PROJECT }
    val hasPlan = onDate.any { it.source == CreatorCalendarSource.WEEKLY_PLAN }
    Surface(
        onClick = { onDateSelect(date) },
        enabled = enabled,
        modifier = modifier.heightIn(min = BacklotUX10Tokens.rowTarget)
            .testTag("ux08e-date-" + date)
            .semantics {
                contentDescription = date.toString() + ", " +
                    onDate.size + " upcoming planned items" +
                    (if (date == selected) ", selected" else "")
            },
        color = if (date == selected) CinemaSurfaceRaised else Color.Transparent,
        shape = RoundedCornerShape(10.dp),
        border = if (date == selected) BorderStroke(
            1.dp, MutedGold.copy(alpha = 0.52f)
        ) else null,
    ) {
        Column(
            Modifier.padding(vertical = 8.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(5.dp),
        ) {
            Text(
                date.dayOfMonth.toString(),
                color = when {
                    !enabled -> MutedText.copy(alpha = 0.4f)
                    date == today -> RecRed
                    else -> ProjectorIvory
                },
                fontSize = 12.sp,
                fontWeight = if (date == selected) FontWeight.Bold else FontWeight.Normal,
            )
            Row(
                horizontalArrangement = Arrangement.spacedBy(3.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                if (enabled && hasProject) {
                    Box(Modifier.size(5.dp).background(RecRed, CircleShape))
                }
                if (enabled && hasPlan) {
                    Box(Modifier.size(5.dp).background(MutedGold, CircleShape))
                }
                if (!hasProject && !hasPlan) {
                    Spacer(Modifier.size(5.dp))
                }
            }
        }
    }
}

@Composable
private fun UX08EAgendaDateChip(
    date: LocalDate,
    selected: LocalDate,
    zone: ZoneId,
    items: List<CreatorCalendarItem>,
    onDateSelect: (LocalDate) -> Unit,
) {
    val count = UX08ECalendarPolicy.countOnDate(items, date, zone)
    Surface(
        onClick = { onDateSelect(date) },
        modifier = Modifier.heightIn(min = BacklotUX10Tokens.minimumTouchTarget)
            .testTag("ux08e-agenda-date-" + date),
        shape = RoundedCornerShape(11.dp),
        color = if (date == selected) CinemaSurfaceRaised else CinemaSurface,
        border = if (date == selected)
            BacklotUX10Tokens.activeBorder(MutedGold) else null,
    ) {
        Column(
            Modifier.padding(horizontal = 11.dp, vertical = 6.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(
                date.format(DateTimeFormatter.ofPattern("EEE d", Locale.getDefault())),
                color = ProjectorIvory, fontSize = 11.sp,
            )
            Text(
                "${count} planned",
                color = MutedText, fontSize = 10.sp,
            )
        }
    }
}
