package com.framebynavin.app.ui

import android.app.TimePickerDialog
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.ArrowBack
import androidx.compose.material.icons.outlined.CalendarMonth
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material.icons.outlined.Refresh
import androidx.compose.material.icons.outlined.Schedule
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.framebynavin.app.data.*
import com.framebynavin.app.ui.theme.*
import java.time.DayOfWeek
import java.time.LocalTime
import java.time.format.DateTimeFormatter
import java.util.Locale

@OptIn(ExperimentalLayoutApi::class)
@Composable
internal fun V08WeeklyScheduleScreen(
    slots: List<WeeklyScheduleSlot>,
    tasks: List<CreatorTask>,
    onClose: () -> Unit,
    onToggle: (String, Boolean) -> Unit,
    onSave: (WeeklyScheduleSlot) -> Unit,
    onDelete: (String) -> Unit,
    onRefresh: () -> Unit,
    onReset: () -> Unit,
) {
    var editorSlot by remember { mutableStateOf<WeeklyScheduleSlot?>(null) }
    var confirmReset by remember { mutableStateOf(false) }
    val activeCount = slots.count { it.enabled }
    val generated = tasks.count { it.scheduleSlotId.isNotBlank() && it.status != TaskStatus.SKIPPED }
    val next = remember(slots.toList()) { WeeklyScheduleEngine.nextOccurrence(slots) }

    Surface(Modifier.fillMaxSize(), color = CinemaBlack) {
        LazyColumn(
            modifier = Modifier.fillMaxSize().statusBarsPadding(),
            contentPadding = PaddingValues(start = 20.dp, end = 20.dp, bottom = 42.dp),
        ) {
            item {
                Row(
                    Modifier.fillMaxWidth().padding(top = 8.dp, bottom = 18.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    IconButton(onClick = onClose) { Icon(Icons.Outlined.ArrowBack, "Back", tint = ProjectorIvory) }
                    Spacer(Modifier.width(5.dp))
                    Column(Modifier.weight(1f)) {
                        Text("WEEKLY ENGINE", color = RecRed, fontSize = 9.sp, letterSpacing = 1.2.sp, fontWeight = FontWeight.Bold)
                        Text("FrameByNavin Week", color = ProjectorIvory, fontSize = 23.sp, fontWeight = FontWeight.Black)
                    }
                    Icon(Icons.Outlined.CalendarMonth, null, tint = MutedGold, modifier = Modifier.size(26.dp))
                }

                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(22.dp),
                    color = CinemaSurfaceRaised,
                    border = androidx.compose.foundation.BorderStroke(1.dp, CinemaLine),
                ) {
                    Column(Modifier.padding(17.dp)) {
                        Text("AUTO-PLAN THE CREATOR WEEK", color = ProjectorIvory, fontSize = 17.sp, fontWeight = FontWeight.Black)
                        Spacer(Modifier.height(5.dp))
                        Text(
                            "Enabled slots generate real Studio projects for the next 8 days. Each project is placed into the correct production stage and its reminder can follow stage checkpoints automatically.",
                            color = MutedText,
                            fontSize = 10.5.sp,
                            lineHeight = 15.sp,
                        )
                        Spacer(Modifier.height(13.dp))
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            V08Metric("ACTIVE SLOTS", activeCount.toString(), Modifier.weight(1f))
                            V08Metric("AUTO PROJECTS", generated.toString(), Modifier.weight(1f))
                        }
                        if (next != null) {
                            Spacer(Modifier.height(10.dp))
                            Surface(
                                Modifier.fillMaxWidth(),
                                RoundedCornerShape(14.dp),
                                Color(0xFF15120F),
                                border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF3A2B22)),
                            ) {
                                Column(Modifier.padding(12.dp)) {
                                    Text("NEXT SLOT", color = MutedText, fontSize = 8.5.sp, letterSpacing = 1.sp)
                                    Spacer(Modifier.height(3.dp))
                                    Text(next.slot.title, color = ProjectorIvory, fontSize = 12.5.sp, fontWeight = FontWeight.Bold)
                                    Text("${WeeklyScheduleEngine.formatOccurrence(next.publishAtMillis)} · ${next.slot.platform} ${next.slot.contentType}", color = MutedGold, fontSize = 9.5.sp)
                                }
                            }
                        }
                    }
                }

                Spacer(Modifier.height(12.dp))
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Button(
                        onClick = onRefresh,
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.buttonColors(containerColor = RecRed),
                        shape = RoundedCornerShape(14.dp),
                    ) {
                        Icon(Icons.Outlined.Refresh, null, modifier = Modifier.size(17.dp))
                        Spacer(Modifier.width(6.dp))
                        Text("REFRESH 8 DAYS", fontSize = 9.5.sp, fontWeight = FontWeight.Bold)
                    }
                    OutlinedButton(
                        onClick = {
                            editorSlot = WeeklyScheduleSlot(
                                id = "",
                                title = "New Creator Slot",
                                dayOfWeek = DayOfWeek.MONDAY,
                                hour = 19,
                                minute = 0,
                                platform = "Instagram",
                                contentType = "Reel",
                                reminderMode = ReminderMode.SMART,
                                priority = TaskPriority.IMPORTANT,
                            )
                        },
                        modifier = Modifier.weight(1f),
                        border = androidx.compose.foundation.BorderStroke(1.dp, CinemaLine),
                        shape = RoundedCornerShape(14.dp),
                    ) {
                        Icon(Icons.Outlined.Add, null, modifier = Modifier.size(17.dp))
                        Spacer(Modifier.width(6.dp))
                        Text("CUSTOM SLOT", color = ProjectorIvory, fontSize = 9.5.sp, fontWeight = FontWeight.Bold)
                    }
                }
                Spacer(Modifier.height(18.dp))
                Text("YOUR WEEK", color = ProjectorIvory, fontSize = 19.sp, fontWeight = FontWeight.Black)
                Text("Tap a slot to edit it. Turning one off stops future generation; projects already created stay in your queue.", color = MutedText, fontSize = 9.8.sp, lineHeight = 14.sp)
                Spacer(Modifier.height(12.dp))
            }

            DayOfWeek.entries.forEach { day ->
                val daySlots = slots.filter { it.dayOfWeek == day }.sortedWith(compareBy<WeeklyScheduleSlot> { it.hour }.thenBy { it.minute })
                if (daySlots.isNotEmpty()) {
                    item(key = "day-${day.name}") {
                        Text(day.name, color = MutedGold, fontSize = 9.sp, letterSpacing = 1.2.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(top = 8.dp, bottom = 6.dp))
                    }
                    items(daySlots, key = { it.id }) { slot ->
                        V08SlotCard(
                            slot = slot,
                            onToggle = { onToggle(slot.id, it) },
                            onEdit = { editorSlot = slot },
                        )
                        Spacer(Modifier.height(7.dp))
                    }
                }
            }

            item {
                Spacer(Modifier.height(12.dp))
                TextButton(onClick = { confirmReset = true }, modifier = Modifier.fillMaxWidth()) {
                    Text("RESET TO FRAMEBYNAVIN DEFAULT WEEK", color = MutedText, fontSize = 9.5.sp)
                }
            }
        }
    }

    editorSlot?.let { slot ->
        V08SlotEditor(
            slot = slot,
            onDismiss = { editorSlot = null },
            onSave = { onSave(it); editorSlot = null },
            onDelete = if (slot.id.isBlank()) null else ({ onDelete(slot.id); editorSlot = null }),
        )
    }

    if (confirmReset) {
        AlertDialog(
            onDismissRequest = { confirmReset = false },
            containerColor = CinemaSurfaceRaised,
            title = { Text("Reset weekly schedule?", color = ProjectorIvory, fontWeight = FontWeight.Bold) },
            text = { Text("This restores the locked FrameByNavin weekly slots. Existing generated projects are not deleted.", color = MutedText) },
            confirmButton = {
                Button(onClick = { confirmReset = false; onReset() }, colors = ButtonDefaults.buttonColors(containerColor = RecRed)) { Text("RESET") }
            },
            dismissButton = { TextButton(onClick = { confirmReset = false }) { Text("CANCEL", color = MutedText) } },
        )
    }
}

@Composable
private fun V08Metric(label: String, value: String, modifier: Modifier) {
    Surface(modifier, RoundedCornerShape(14.dp), Color(0xFF111111), border = androidx.compose.foundation.BorderStroke(1.dp, CinemaLine)) {
        Column(Modifier.padding(11.dp)) {
            Text(label, color = MutedText, fontSize = 7.8.sp)
            Text(value, color = ProjectorIvory, fontSize = 23.sp, fontWeight = FontWeight.Black)
        }
    }
}

@Composable
private fun V08SlotCard(slot: WeeklyScheduleSlot, onToggle: (Boolean) -> Unit, onEdit: () -> Unit) {
    Surface(
        modifier = Modifier.fillMaxWidth().clickable(onClick = onEdit),
        shape = RoundedCornerShape(17.dp),
        color = if (slot.enabled) CinemaSurfaceRaised else Color(0xFF0E0E0E),
        border = androidx.compose.foundation.BorderStroke(1.dp, if (slot.enabled) CinemaLine else Color(0xFF181818)),
    ) {
        Column(Modifier.padding(13.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(Modifier.size(8.dp).background(if (slot.enabled) RecRed else Color(0xFF494641), CircleShape))
                Spacer(Modifier.width(9.dp))
                Column(Modifier.weight(1f)) {
                    Text(slot.title, color = if (slot.enabled) ProjectorIvory else Color(0xFF77726C), fontSize = 12.5.sp, fontWeight = FontWeight.Bold)
                    Text("${slot.platform} · ${slot.contentType}", color = MutedText, fontSize = 9.2.sp)
                }
                Switch(checked = slot.enabled, onCheckedChange = onToggle, colors = SwitchDefaults.colors(checkedTrackColor = RecRed))
            }
            Spacer(Modifier.height(8.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Outlined.Schedule, null, tint = MutedGold, modifier = Modifier.size(14.dp))
                Spacer(Modifier.width(5.dp))
                Text(v08Time(slot), color = MutedGold, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                Spacer(Modifier.width(8.dp))
                V08TinyChip(if (slot.cadence == ScheduleCadence.WEEKS_1_3) "WEEKS 1 + 3" else "WEEKLY")
                Spacer(Modifier.width(5.dp))
                V08TinyChip(slot.reminderMode.name)
                Spacer(Modifier.weight(1f))
                Icon(Icons.Outlined.Edit, null, tint = MutedText, modifier = Modifier.size(15.dp))
            }
        }
    }
}

@Composable
private fun V08TinyChip(text: String) {
    Surface(shape = RoundedCornerShape(100.dp), color = Color(0xFF111111), border = androidx.compose.foundation.BorderStroke(1.dp, CinemaLine)) {
        Text(text, color = Color(0xFF97918A), fontSize = 7.5.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(horizontal = 6.dp, vertical = 4.dp))
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun V08SlotEditor(
    slot: WeeklyScheduleSlot,
    onDismiss: () -> Unit,
    onSave: (WeeklyScheduleSlot) -> Unit,
    onDelete: (() -> Unit)?,
) {
    val context = LocalContext.current
    var title by remember(slot.id) { mutableStateOf(slot.title) }
    var day by remember(slot.id) { mutableStateOf(slot.dayOfWeek) }
    var hour by remember(slot.id) { mutableIntStateOf(slot.hour) }
    var minute by remember(slot.id) { mutableIntStateOf(slot.minute) }
    var platform by remember(slot.id) { mutableStateOf(slot.platform) }
    var contentType by remember(slot.id) { mutableStateOf(slot.contentType) }
    var cadence by remember(slot.id) { mutableStateOf(slot.cadence) }
    var reminderMode by remember(slot.id) { mutableStateOf(slot.reminderMode) }
    var priority by remember(slot.id) { mutableStateOf(slot.priority) }
    val formats = v08Formats(platform)
    LaunchedEffect(platform) { if (contentType !in formats) contentType = formats.first() }
    val template = CreatorWorkflowEngine.templateFor(platform, contentType)

    fun chooseTime() {
        TimePickerDialog(context, { _, h, m -> hour = h; minute = m }, hour, minute, false).show()
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = CinemaSurfaceRaised,
        title = { Text(if (slot.id.isBlank()) "New Weekly Slot" else "Edit Weekly Slot", color = ProjectorIvory, fontWeight = FontWeight.Black) },
        text = {
            Column(Modifier.fillMaxWidth().heightIn(max = 650.dp)) {
                androidx.compose.foundation.rememberScrollState().let { scroll ->
                    Column(Modifier.fillMaxWidth().verticalScroll(scroll)) {
                        OutlinedTextField(title, { title = it }, label = { Text("Slot title") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                        Spacer(Modifier.height(13.dp)); V08Label("DAY")
                        FlowRow(horizontalArrangement = Arrangement.spacedBy(5.dp), verticalArrangement = Arrangement.spacedBy(5.dp)) {
                            DayOfWeek.entries.forEach { value -> FilterChip(day == value, { day = value }, { Text(value.name.take(3), fontSize = 8.5.sp) }) }
                        }
                        Spacer(Modifier.height(12.dp)); V08Label("PUBLISH TIME")
                        OutlinedButton(onClick = { chooseTime() }, modifier = Modifier.fillMaxWidth()) {
                            Icon(Icons.Outlined.Schedule, null, modifier = Modifier.size(16.dp)); Spacer(Modifier.width(7.dp)); Text(v08Time(hour, minute), color = ProjectorIvory, modifier = Modifier.weight(1f)); Text("CHANGE", color = RecRed, fontSize = 8.sp)
                        }
                        Spacer(Modifier.height(12.dp)); V08Label("PLATFORM")
                        FlowRow(horizontalArrangement = Arrangement.spacedBy(5.dp)) {
                            listOf("Instagram", "YouTube", "X").forEach { value -> FilterChip(platform == value, { platform = value }, { Text(value, fontSize = 9.sp) }) }
                        }
                        Spacer(Modifier.height(10.dp)); V08Label("FORMAT")
                        FlowRow(horizontalArrangement = Arrangement.spacedBy(5.dp), verticalArrangement = Arrangement.spacedBy(5.dp)) {
                            formats.forEach { value -> FilterChip(contentType == value, { contentType = value }, { Text(value, fontSize = 8.5.sp) }) }
                        }
                        Spacer(Modifier.height(12.dp)); V08Label("CADENCE")
                        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            FilterChip(cadence == ScheduleCadence.EVERY_WEEK, { cadence = ScheduleCadence.EVERY_WEEK }, { Text("Every week", fontSize = 9.sp) })
                            FilterChip(cadence == ScheduleCadence.WEEKS_1_3, { cadence = ScheduleCadence.WEEKS_1_3 }, { Text("Weeks 1 + 3", fontSize = 9.sp) })
                        }
                        Spacer(Modifier.height(12.dp)); V08Label("AUTO STAGE REMINDER")
                        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            listOf(ReminderMode.NONE, ReminderMode.SIMPLE, ReminderMode.SMART).forEach { value ->
                                FilterChip(reminderMode == value, { reminderMode = value }, { Text(value.name.lowercase().replaceFirstChar { it.uppercase() }, fontSize = 8.7.sp) })
                            }
                        }
                        Text(
                            when (reminderMode) {
                                ReminderMode.SMART -> "Smart escalation targets the deadline of the current production stage and moves forward when you complete that stage."
                                ReminderMode.SIMPLE -> "One notification targets the current stage deadline."
                                else -> "Projects are generated without an automatic reminder."
                            },
                            color = MutedText,
                            fontSize = 9.sp,
                            lineHeight = 13.sp,
                        )
                        Spacer(Modifier.height(12.dp)); V08Label("PRIORITY")
                        Row(horizontalArrangement = Arrangement.spacedBy(5.dp)) {
                            TaskPriority.entries.forEach { value -> FilterChip(priority == value, { priority = value }, { Text(value.name.lowercase().replaceFirstChar { it.uppercase() }, fontSize = 8.5.sp) }) }
                        }
                        Spacer(Modifier.height(14.dp))
                        Surface(Modifier.fillMaxWidth(), RoundedCornerShape(14.dp), Color(0xFF10100F), border = androidx.compose.foundation.BorderStroke(1.dp, CinemaLine)) {
                            Column(Modifier.padding(12.dp)) {
                                Text("PRODUCTION RUNWAY", color = MutedText, fontSize = 8.sp, letterSpacing = 1.sp)
                                Spacer(Modifier.height(4.dp))
                                Text(template.stages.joinToString("  →  ") { it.label }, color = ProjectorIvory, fontSize = 9.5.sp, lineHeight = 14.sp)
                            }
                        }
                        if (onDelete != null) {
                            Spacer(Modifier.height(7.dp)); TextButton(onClick = onDelete, modifier = Modifier.fillMaxWidth()) { Text("DELETE SLOT", color = Color(0xFFE87A73), fontSize = 9.sp) }
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    onSave(slot.copy(title = title.trim(), dayOfWeek = day, hour = hour, minute = minute, platform = platform, contentType = contentType, cadence = cadence, reminderMode = reminderMode, priority = priority))
                },
                enabled = title.isNotBlank(),
                colors = ButtonDefaults.buttonColors(containerColor = RecRed),
            ) { Text("SAVE") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("CANCEL", color = MutedText) } },
    )
}

@Composable
private fun V08Label(text: String) {
    Text(text, color = MutedText, fontSize = 8.sp, letterSpacing = 1.sp, fontWeight = FontWeight.SemiBold)
    Spacer(Modifier.height(5.dp))
}

private fun v08Formats(platform: String): List<String> = when (platform) {
    "YouTube" -> listOf("Long-form", "Short", "Cinematic Moment")
    "X" -> listOf("Post", "Video", "Update")
    else -> listOf("Reel", "Post", "Story")
}

private fun v08Time(slot: WeeklyScheduleSlot): String = v08Time(slot.hour, slot.minute)

private fun v08Time(hour: Int, minute: Int): String =
    LocalTime.of(hour.coerceIn(0, 23), minute.coerceIn(0, 59)).format(DateTimeFormatter.ofPattern("h:mm a", Locale.getDefault()))
