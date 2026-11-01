package com.framebynavin.app.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.ChevronRight
import androidx.compose.material.icons.outlined.CalendarMonth
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.platform.LocalContext
import com.framebynavin.app.data.CreatorIdea
import com.framebynavin.app.data.CreatorTask
import com.framebynavin.app.data.CreatorWorkflowTimelineEvent
import com.framebynavin.app.data.CreatorWorkflowTimelineStore
import com.framebynavin.app.data.ProgressJourneyEngine
import com.framebynavin.app.ui.theme.*
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Small Home preview. Deliberately placed below Up Next and Reminders.
 * Never shows Personal data or inactive historical legacy assumptions.
 */
@Composable
internal fun V282ProgressHomeStrip(
    tasks: List<CreatorTask>,
    ideas: List<CreatorIdea>,
    onOpen: () -> Unit,
) {
    val context = LocalContext.current
    val store = remember(context) { CreatorWorkflowTimelineStore(context.applicationContext) }
    var timeline by remember { mutableStateOf(emptyList<CreatorWorkflowTimelineEvent>()) }
    LaunchedEffect(tasks, ideas) {
        timeline = withContext(Dispatchers.IO) { store.loadAll() }
    }
    val zone = remember { ZoneId.systemDefault() }
    val events = remember(tasks, ideas, timeline) { ProgressJourneyEngine.build(tasks, ideas, timeline) }
    if (events.isEmpty()) return
    val activeDays = remember(events, zone) { ProgressJourneyEngine.activeDays(events, zone) }
    val today = LocalDate.now(zone)
    val start = today.minusDays(6)

    Surface(
        modifier = Modifier.fillMaxWidth().clickable(onClick = onOpen),
        shape = RoundedCornerShape(18.dp),
        color = CinemaSurface,
        border = BorderStroke(1.dp, CinemaLine),
    ) {
        Column(Modifier.padding(horizontal = 14.dp, vertical = 13.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Outlined.CalendarMonth, null, tint = MutedGold, modifier = Modifier.size(19.dp))
                Spacer(Modifier.width(9.dp))
                Text("PROGRESS JOURNEY", color = ProjectorIvory, fontSize = 11.sp,
                    fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
                Icon(Icons.Outlined.ChevronRight, "Open Progress Journey", tint = MutedText,
                    modifier = Modifier.size(18.dp))
            }
            Spacer(Modifier.height(9.dp))
            Row(Modifier.fillMaxWidth()) {
                repeat(7) { index ->
                    val day = start.plusDays(index.toLong())
                    Column(Modifier.weight(1f), horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(day.format(DateTimeFormatter.ofPattern("EEEEE")), color = MutedText, fontSize = 10.sp)
                        Spacer(Modifier.height(5.dp))
                        Text(day.dayOfMonth.toString(), color = if (day == today) ProjectorIvory else MutedText,
                            fontSize = 11.sp, fontWeight = if (day == today) FontWeight.Bold else FontWeight.Normal)
                        Spacer(Modifier.height(6.dp))
                        Box(Modifier.size(6.dp).background(
                            if (day in activeDays) SuccessGreen else Color.Transparent, CircleShape))
                    }
                }
            }
        }
    }
}
