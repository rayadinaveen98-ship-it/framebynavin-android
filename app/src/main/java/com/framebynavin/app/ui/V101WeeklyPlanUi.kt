package com.framebynavin.app.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.ArrowBack
import androidx.compose.material.icons.outlined.CalendarMonth
import androidx.compose.material.icons.outlined.EditCalendar
import androidx.compose.material.icons.outlined.Refresh
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.framebynavin.app.data.*
import com.framebynavin.app.ui.theme.*
import java.time.DayOfWeek
import java.time.LocalTime
import java.time.format.DateTimeFormatter
import java.util.Locale

@Composable
internal fun V101WeeklyPlanScreen(
    autoPlanEnabled: Boolean,
    slots: List<WeeklyScheduleSlot>,
    tasks: List<CreatorTask>,
    onAutoPlanChange: (Boolean) -> Unit,
    onClose: () -> Unit,
    onToggle: (String, Boolean) -> Unit,
    onSave: (WeeklyScheduleSlot) -> Unit,
    onDelete: (String) -> Unit,
    onRefresh: () -> Unit,
    onReset: () -> Unit,
) {
    var showTemplateEditor by remember { mutableStateOf(false) }

    if (showTemplateEditor) {
        V08WeeklyScheduleScreen(
            slots = slots,
            tasks = tasks,
            onClose = { showTemplateEditor = false },
            onToggle = onToggle,
            onSave = onSave,
            onDelete = onDelete,
            onRefresh = onRefresh,
            onReset = onReset,
        )
        return
    }

    val next = remember(autoPlanEnabled, slots.toList()) {
        if (autoPlanEnabled) WeeklyScheduleEngine.nextOccurrence(slots) else null
    }
    val weeklyProjects = tasks.count { it.origin == CreatorTaskOrigin.WEEKLY && it.status != TaskStatus.SKIPPED }

    Surface(Modifier.fillMaxSize(), color = CinemaBlack) {
        Column(
            Modifier.fillMaxSize().verticalScroll(rememberScrollState()).statusBarsPadding().padding(horizontal = 20.dp).padding(bottom = 40.dp)
        ) {
            Row(Modifier.fillMaxWidth().padding(top = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = onClose) { Icon(Icons.Outlined.ArrowBack, "Back", tint = ProjectorIvory) }
                Spacer(Modifier.width(4.dp))
                Column(Modifier.weight(1f)) {
                    Text("WEEKLY PLAN", color = RecRed, fontSize = 8.8.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.1.sp)
                    Text("Plan my week", color = ProjectorIvory, fontSize = 24.sp, fontWeight = FontWeight.Black)
                }
                Icon(Icons.Outlined.CalendarMonth, null, tint = MutedGold, modifier = Modifier.size(26.dp))
            }

            Spacer(Modifier.height(18.dp))
            Surface(
                Modifier.fillMaxWidth(),
                RoundedCornerShape(21.dp),
                CinemaSurfaceRaised,
                border = BorderStroke(1.dp, CinemaLine),
            ) {
                Column(Modifier.padding(17.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Column(Modifier.weight(1f)) {
                            Text("WEEKLY AUTO PLAN", color = ProjectorIvory, fontSize = 16.sp, fontWeight = FontWeight.Black)
                            Text(
                                if (autoPlanEnabled) "Your recurring content will be added to Home automatically."
                                else "Off by default. Your weekly template stays saved without adding anything to Home.",
                                color = MutedText,
                                fontSize = 10.sp,
                                lineHeight = 15.sp,
                            )
                        }
                        Switch(
                            checked = autoPlanEnabled,
                            onCheckedChange = onAutoPlanChange,
                            colors = SwitchDefaults.colors(checkedTrackColor = RecRed),
                        )
                    }

                    if (autoPlanEnabled && next != null) {
                        Spacer(Modifier.height(14.dp))
                        HorizontalDivider(color = CinemaLine)
                        Spacer(Modifier.height(12.dp))
                        Text("NEXT", color = MutedText, fontSize = 8.sp, fontWeight = FontWeight.Bold)
                        Text(next.slot.title, color = ProjectorIvory, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                        Text(WeeklyScheduleEngine.formatOccurrence(next.publishAtMillis), color = MutedGold, fontSize = 9.5.sp)
                    }
                }
            }

            Spacer(Modifier.height(16.dp))
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedButton(
                    onClick = { showTemplateEditor = true },
                    modifier = Modifier.weight(1f),
                    border = BorderStroke(1.dp, CinemaLine),
                    shape = RoundedCornerShape(14.dp),
                ) {
                    Icon(Icons.Outlined.EditCalendar, null, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(6.dp))
                    Text("EDIT SCHEDULE", color = ProjectorIvory, fontSize = 9.sp)
                }
                Button(
                    onClick = onRefresh,
                    enabled = autoPlanEnabled,
                    modifier = Modifier.weight(1f),
                    colors = ButtonDefaults.buttonColors(containerColor = RecRed),
                    shape = RoundedCornerShape(14.dp),
                ) {
                    Icon(Icons.Outlined.Refresh, null, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(6.dp))
                    Text("UPDATE WEEK", fontSize = 9.sp, fontWeight = FontWeight.Bold)
                }
            }

            Spacer(Modifier.height(22.dp))
            Text("YOUR SCHEDULE", color = ProjectorIvory, fontSize = 18.sp, fontWeight = FontWeight.Black)
            Text("${slots.count { it.enabled }} recurring items · $weeklyProjects weekly projects kept", color = MutedText, fontSize = 9.5.sp)
            Spacer(Modifier.height(10.dp))

            DayOfWeek.values().forEach { day ->
                val daySlots = slots.filter { it.dayOfWeek == day && it.enabled }.sortedWith(compareBy<WeeklyScheduleSlot> { it.hour }.thenBy { it.minute })
                if (daySlots.isNotEmpty()) {
                    Text(day.name.lowercase().replaceFirstChar { it.uppercase() }, color = MutedGold, fontSize = 9.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(top = 10.dp, bottom = 5.dp))
                    daySlots.forEach { slot ->
                        Surface(
                            Modifier.fillMaxWidth().padding(bottom = 6.dp),
                            RoundedCornerShape(15.dp),
                            CinemaSurface,
                            border = BorderStroke(1.dp, CinemaLine),
                        ) {
                            Row(Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                                Column(Modifier.weight(1f)) {
                                    Text(slot.title, color = ProjectorIvory, fontSize = 11.5.sp, fontWeight = FontWeight.Bold)
                                    Text("${slot.platform} · ${slot.contentType}", color = MutedText, fontSize = 8.8.sp)
                                }
                                Text(v101Time(slot.hour, slot.minute), color = MutedGold, fontSize = 9.5.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }
        }
    }
}

private fun v101Time(hour: Int, minute: Int): String =
    LocalTime.of(hour, minute).format(DateTimeFormatter.ofPattern("h:mm a", Locale.getDefault()))
