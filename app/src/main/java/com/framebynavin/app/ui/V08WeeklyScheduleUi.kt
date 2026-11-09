package com.framebynavin.app.ui

import android.app.TimePickerDialog
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
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
import androidx.compose.ui.platform.testTag
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
    autoPlanEnabled: Boolean = true,
) {
    val context = LocalContext.current
    val creatorProfile = remember(context) { CreatorOsSettingsStore(context).snapshot().creatorProfile }
    val primaryPlatform = remember(creatorProfile) { CreatorPlatformRegistry.primaryPlatform(creatorProfile) }
    var editorSlot by remember { mutableStateOf<WeeklyScheduleSlot?>(null) }
    var confirmReset by remember { mutableStateOf(false) }
    var confirmGenerate by remember { mutableStateOf(false) }
    var pendingDeleteSlot by remember { mutableStateOf<WeeklyScheduleSlot?>(null) }
    var pendingToggle by remember { mutableStateOf<Pair<WeeklyScheduleSlot, Boolean>?>(null) }
    var pendingSave by remember { mutableStateOf<Pair<WeeklyScheduleSlot, WeeklyScheduleSlot>?>(null) }
    val availabilityStore = remember(context) {
        CreatorWorkloadAvailabilityStore(context.applicationContext)
    }
    var availableMinutes by remember { mutableIntStateOf(availabilityStore.availableMinutes()) }
    var editAvailability by remember { mutableStateOf(false) }
    var availableInput by remember { mutableStateOf(availableMinutes.takeIf { it > 0 }?.toString().orEmpty()) }
    var workloadError by remember { mutableStateOf<String?>(null) }
    val workload = CreatorWorkloadPlanning.nextSevenDays(slots, availableMinutes)
    val activeCount = slots.count { it.enabled }
    val generated = tasks.count { it.scheduleSlotId.isNotBlank() && it.status != TaskStatus.SKIPPED }
    val next = remember(slots.toList()) { WeeklyScheduleEngine.nextOccurrence(slots) }
    val preview = CreatorWeeklyPlanPreviewEngine.build(slots, tasks)
    val resetPreview = CreatorWeeklyPresetPolicy.previewFor(creatorProfile)

    Surface(Modifier.fillMaxSize(), color = CinemaBlack) {
        LazyColumn(
            modifier = Modifier.fillMaxSize().windowInsetsPadding(WindowInsets.safeDrawing),
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
                        Text("WEEKLY PLAN", color = RecRed, fontSize = 9.sp, letterSpacing = 1.2.sp, fontWeight = FontWeight.Bold)
                        Text("Your Week", color = ProjectorIvory, fontSize = 23.sp, fontWeight = FontWeight.Black)
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
                        Text("PLAN MY WEEK", color = ProjectorIvory, fontSize = 17.sp, fontWeight = FontWeight.Black)
                        Spacer(Modifier.height(5.dp))
                        Text(
                            "Weekly slots describe future posts. Auto Plan must be on to create projects. Existing projects always stay yours.",
                            color = MutedText,
                            fontSize = 10.5.sp,
                            lineHeight = 15.sp,
                        )
                        Spacer(Modifier.height(13.dp))
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            V08Metric("ACTIVE SLOTS", activeCount.toString(), Modifier.weight(1f))
                            V08Metric("AUTO PROJECTS", generated.toString(), Modifier.weight(1f))
                        }
                        Text(if (autoPlanEnabled) "Auto Plan is on. Templates may create future projects." else "Auto Plan is off. Templates are saved, but no new projects are created.",
                            color = MutedGold, fontSize = 11.sp)
                        if (next != null) {
                            Spacer(Modifier.height(10.dp))
                            Surface(
                                Modifier.fillMaxWidth(),
                                RoundedCornerShape(14.dp),
                                MutedGold.copy(alpha = .06f),
                                border = androidx.compose.foundation.BorderStroke(1.dp, MutedGold.copy(alpha = .22f)),
                            ) {
                                Column(Modifier.padding(12.dp)) {
                                    Text("NEXT PLANNED POST", color = MutedText, fontSize = 8.5.sp, letterSpacing = 1.sp)
                                    Spacer(Modifier.height(3.dp))
                                    Text(next.slot.title, color = ProjectorIvory, fontSize = 12.5.sp, fontWeight = FontWeight.Bold)
                                    Text("${WeeklyScheduleEngine.formatOccurrence(next.publishAtMillis)} · ${next.slot.platform} ${next.slot.contentType}", color = MutedGold, fontSize = 9.5.sp)
                                }
                            }
                        }
                    }
                }

                Spacer(Modifier.height(12.dp))
                Surface(
                    modifier = Modifier.fillMaxWidth().testTag("phase5-workload"),
                    shape = RoundedCornerShape(18.dp),
                    color = CinemaSurface,
                    border = BorderStroke(1.dp, CinemaLine),
                ) {
                    Column(Modifier.padding(16.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Column(Modifier.weight(1f)) {
                                Text("OPTIONAL WORKLOAD", color = ProjectorIvory,
                                    fontSize = 14.sp, fontWeight = FontWeight.Bold)
                                Text("Next 7 days · estimates you entered for recurring slots",
                                    color = MutedText, fontSize = 11.sp)
                            }
                            TextButton(onClick = { editAvailability = !editAvailability }) {
                                Text(if (editAvailability) "CLOSE" else "EDIT TIME",
                                    color = RecRed, fontSize = 11.sp)
                            }
                        }
                        Spacer(Modifier.height(6.dp))
                        Text(workload.summary(), color = ProjectorIvory, fontSize = 13.sp)
                        if (!workload.availabilityIsSet) {
                            Text("Set your available minutes/week to check capacity.",
                                color = MutedText, fontSize = 11.sp)
                        }
                        if (workload.hasIncompleteEstimates) {
                            Text("This is a partial estimate; unestimated slots are excluded.",
                                color = MutedText, fontSize = 11.sp)
                        }
                        if (workload.isOverloaded) {
                            Text("More planned effort than the time you set. Review your slots; deadlines were not changed.",
                                color = RecRed, fontSize = 12.sp,
                                modifier = Modifier.testTag("phase5-overload-warning"))
                        }
                        if (editAvailability) {
                            Spacer(Modifier.height(10.dp))
                            OutlinedTextField(
                                value = availableInput,
                                onValueChange = { next ->
                                    if (next.length <= 5 && next.all(Char::isDigit)) {
                                        availableInput = next
                                        workloadError = null
                                    }
                                },
                                label = { Text("Minutes available per week") },
                                supportingText = { Text("30 to 10080 minutes · clear to leave unset") },
                                singleLine = true,
                                modifier = Modifier.fillMaxWidth(),
                            )
                            workloadError?.let {
                                Text(it, color = RecRed, fontSize = 11.sp)
                            }
                            Button(
                                onClick = {
                                    val next = if (availableInput.isBlank()) 0
                                        else availableInput.toIntOrNull() ?: -1
                                    if (next != 0 && next !in 30..10080) {
                                        workloadError = "Enter at least 30 minutes or leave it blank."
                                    } else {
                                        runCatching { availabilityStore.saveAvailableMinutes(next) }
                                            .onSuccess {
                                                availableMinutes = next
                                                editAvailability = false
                                                workloadError = null
                                            }
                                            .onFailure {
                                                workloadError = "Availability could not be saved. Try again."
                                            }
                                    }
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = RecRed),
                            ) { Text("SAVE AVAILABILITY") }
                            Text("Local preference only · not a verified cloud backup",
                                color = MutedText, fontSize = 10.sp)
                        }
                    }
                }
                Spacer(Modifier.height(12.dp))
                V197WeeklyPlanActions(
                    previewCount = preview.missing.size,
                    previewEnabled = autoPlanEnabled && preview.missing.isNotEmpty(),
                    onPreview = { confirmGenerate = true },
                    onAdd = {
                        editorSlot = WeeklyScheduleSlot(
                            id = "",
                            title = "New Creator Slot",
                            dayOfWeek = DayOfWeek.MONDAY,
                            hour = 19,
                            minute = 0,
                            platform = primaryPlatform,
                            contentType = CreatorPlatformRegistry.defaultFormat(primaryPlatform),
                            reminderMode = ReminderMode.SMART,
                            priority = TaskPriority.IMPORTANT,
                        )
                    },
                )
                Spacer(Modifier.height(8.dp))
                Text("${preview.alreadyCreated} upcoming slots already have projects. Existing projects won't be replaced.", color = MutedText, fontSize = 10.sp)
                Spacer(Modifier.height(18.dp))
                Text("YOUR WEEK", color = ProjectorIvory, fontSize = 19.sp, fontWeight = FontWeight.Black)
                Text("Edit a recurring template for future projects. Existing projects never change or disappear when you edit, disable or delete a slot.", color = MutedText, fontSize = 9.8.sp, lineHeight = 14.sp)
                Spacer(Modifier.height(12.dp))
            }

            DayOfWeek.values().forEach { day ->
                val daySlots = slots.filter { it.dayOfWeek == day }.sortedWith(compareBy<WeeklyScheduleSlot> { it.hour }.thenBy { it.minute })
                if (daySlots.isNotEmpty()) {
                    item(key = "day-${day.name}") {
                        Text(day.name, color = MutedGold, fontSize = 9.sp, letterSpacing = 1.2.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(top = 8.dp, bottom = 6.dp))
                    }
                    items(daySlots, key = { it.id }) { slot ->
                        V08SlotCard(
                            slot = slot,
                            onToggle = { pendingToggle = slot to it },
                            onEdit = { editorSlot = slot },
                        )
                        Spacer(Modifier.height(7.dp))
                    }
                }
            }

            item {
                Spacer(Modifier.height(12.dp))
                TextButton(onClick = { confirmReset = true }, modifier = Modifier.fillMaxWidth()) {
                    Text("RESTORE DEFAULT WEEK", color = MutedText, fontSize = 10.sp)
                }
            }
        }
    }

    editorSlot?.let { slot ->
        V08SlotEditor(
            slot = slot,
            onDismiss = { editorSlot = null },
            onSave = { updated -> pendingSave = slot to updated; editorSlot = null },
            onDelete = if (slot.id.isBlank()) null else ({ pendingDeleteSlot = slot; editorSlot = null }),
        )
    }

    pendingToggle?.let { (original, enabled) ->
        val unchanged = slots.firstOrNull { it.id == original.id } == original
        AlertDialog(
            onDismissRequest = { pendingToggle = null },
            containerColor = CinemaSurfaceRaised,
            title = { Text(if (enabled) "Enable recurring slot?" else "Disable recurring slot?",
                color = ProjectorIvory) },
            text = {
                Text(
                    if (!unchanged) "This weekly slot changed. Close and reopen it before editing."
                    else if (enabled) "Auto Plan can create future projects for this slot when enabled. Existing projects will not change."
                    else "This stops future slot generation. Existing projects, reminders and saved work will stay unchanged.",
                    color = MutedText,
                )
            },
            confirmButton = {
                Button(
                    enabled = unchanged,
                    onClick = { onToggle(original.id, enabled); pendingToggle = null },
                    colors = ButtonDefaults.buttonColors(containerColor = RecRed),
                ) { Text("CONFIRM") }
            },
            dismissButton = {
                TextButton(onClick = { pendingToggle = null }) { Text("CANCEL") }
            },
        )
    }

    pendingSave?.let { (before, after) ->
        val fresh = before.id.isBlank() || slots.firstOrNull { it.id == before.id } == before
        AlertDialog(
            onDismissRequest = { pendingSave = null },
            containerColor = CinemaSurfaceRaised,
            title = { Text(if (before.id.isBlank()) "Add weekly slot?" else "Save changes for future slots?",
                color = ProjectorIvory) },
            text = {
                Text(
                    if (!fresh) "This schedule changed while you edited. Reopen it before saving."
                    else "Future recurring previews will use '${after.title}' on ${after.dayOfWeek.name.lowercase()} at ${v08Time(after.hour, after.minute)}. Already-created projects and reminders will not be rewritten.",
                    color = MutedText,
                )
            },
            confirmButton = {
                Button(
                    enabled = fresh,
                    onClick = { onSave(after); pendingSave = null },
                    colors = ButtonDefaults.buttonColors(containerColor = RecRed),
                ) { Text("SAVE FUTURE TEMPLATE") }
            },
            dismissButton = {
                TextButton(onClick = { pendingSave = null }) { Text("CANCEL") }
            },
        )
    }

    pendingDeleteSlot?.let { slot ->
        AlertDialog(
            onDismissRequest = { pendingDeleteSlot = null },
            containerColor = CinemaSurfaceRaised,
            title = { Text("Remove this weekly template?", color = ProjectorIvory) },
            text = { Text("Remove '${slot.title}' from future weekly plans? Any project already created from it will stay unchanged.", color = MutedText) },
            confirmButton = {
                Button(onClick = { onDelete(slot.id); pendingDeleteSlot = null },
                    colors = ButtonDefaults.buttonColors(containerColor = RecRed)) { Text("REMOVE TEMPLATE") }
            },
            dismissButton = { TextButton(onClick = { pendingDeleteSlot = null }) { Text("CANCEL") } },
        )
    }

    if (confirmGenerate) {
        AlertDialog(
            onDismissRequest = { confirmGenerate = false },
            containerColor = CinemaSurfaceRaised,
            title = { Text("Create upcoming projects?", color = ProjectorIvory) },
            text = {
                Column {
                    Text("Auto Plan can add ${preview.missing.size} missing projects from the next 14 days. It won't edit or remove existing projects.", color = MutedText)
                    Spacer(Modifier.height(9.dp))
                    preview.missing.take(5).forEach { occurrence ->
                        Text("• ${occurrence.slot.title} · ${WeeklyScheduleEngine.formatOccurrence(occurrence.publishAtMillis)}",
                            color = ProjectorIvory, fontSize = 11.sp)
                    }
                    if (preview.missing.size > 5) Text("And ${preview.missing.size - 5} more…", color = MutedText)
                }
            },
            confirmButton = {
                Button(onClick = { confirmGenerate = false; onRefresh() },
                    colors = ButtonDefaults.buttonColors(containerColor = RecRed)) { Text("CREATE PROJECTS") }
            },
            dismissButton = { TextButton(onClick = { confirmGenerate = false }) { Text("CANCEL") } },
        )
    }

    if (confirmReset) {
        AlertDialog(
            onDismissRequest = { confirmReset = false },
            containerColor = CinemaSurfaceRaised,
            title = { Text("Reset weekly schedule?", color = ProjectorIvory, fontWeight = FontWeight.Bold) },
            text = { Text("Replace ${slots.size} saved weekly slots with ${resetPreview.size} starter slots for your creator profile? Your customized templates will be replaced, but existing projects and voice recordings stay saved.", color = MutedText) },
            confirmButton = {
                Button(onClick = { confirmReset = false; onReset() }, colors = ButtonDefaults.buttonColors(containerColor = RecRed)) { Text("RESET") }
            },
            dismissButton = { TextButton(onClick = { confirmReset = false }) { Text("CANCEL", color = MutedText) } },
        )
    }
}

@Composable
private fun V08Metric(label: String, value: String, modifier: Modifier) {
    Surface(modifier, RoundedCornerShape(14.dp), CinemaSurface, border = androidx.compose.foundation.BorderStroke(1.dp, CinemaLine)) {
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
        color = if (slot.enabled) CinemaSurfaceRaised else CinemaSurface,
        border = androidx.compose.foundation.BorderStroke(1.dp, CinemaLine.copy(alpha = if (slot.enabled) 1f else .65f)),
    ) {
        Column(Modifier.padding(13.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(Modifier.size(8.dp).background(if (slot.enabled) RecRed else MutedText.copy(alpha = .55f), CircleShape))
                Spacer(Modifier.width(9.dp))
                Column(Modifier.weight(1f)) {
                    Text(slot.title, color = if (slot.enabled) ProjectorIvory else MutedText, fontSize = 12.5.sp, fontWeight = FontWeight.Bold)
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
    Surface(shape = RoundedCornerShape(100.dp), color = CinemaSurface, border = androidx.compose.foundation.BorderStroke(1.dp, CinemaLine)) {
        Text(text, color = MutedText, fontSize = 7.5.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(horizontal = 6.dp, vertical = 4.dp))
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
    var estimateText by remember(slot.id) {
        mutableStateOf(slot.estimatedMinutes.takeIf { it > 0 }?.toString().orEmpty())
    }
    var showAdvanced by remember(slot.id) { mutableStateOf(false) }
    val creatorProfile = remember(context) { CreatorOsSettingsStore(context).snapshot().creatorProfile }
    val platformOptions = remember(creatorProfile, platform) { CreatorPlatformRegistry.orderedSelected(creatorProfile, platform) }
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
                        OutlinedTextField(title, { title = it }, label = { Text("Post name") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                        Spacer(Modifier.height(13.dp)); V08Label("DAY")
                        FlowRow(horizontalArrangement = Arrangement.spacedBy(5.dp), verticalArrangement = Arrangement.spacedBy(5.dp)) {
                            DayOfWeek.values().forEach { value -> FilterChip(day == value, { day = value }, { Text(value.name.take(3), fontSize = 10.sp) }) }
                        }
                        Spacer(Modifier.height(12.dp)); V08Label("PUBLISH TIME")
                        OutlinedButton(onClick = { chooseTime() }, modifier = Modifier.fillMaxWidth()) {
                            Icon(Icons.Outlined.Schedule, null, modifier = Modifier.size(16.dp)); Spacer(Modifier.width(7.dp)); Text(v08Time(hour, minute), color = ProjectorIvory, modifier = Modifier.weight(1f)); Text("CHANGE", color = RecRed, fontSize = 10.sp)
                        }
                        Spacer(Modifier.height(12.dp)); V08Label("PLATFORM")
                        FlowRow(horizontalArrangement = Arrangement.spacedBy(5.dp)) {
                            platformOptions.forEach { value -> FilterChip(platform == value, { platform = value }, { Text(value, fontSize = 10.sp) }) }
                        }
                        Spacer(Modifier.height(10.dp)); V08Label("FORMAT")
                        FlowRow(horizontalArrangement = Arrangement.spacedBy(5.dp), verticalArrangement = Arrangement.spacedBy(5.dp)) {
                            formats.forEach { value -> FilterChip(contentType == value, { contentType = value }, { Text(value, fontSize = 10.sp) }) }
                        }
                        Spacer(Modifier.height(12.dp)); V08Label("CADENCE")
                        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            FilterChip(cadence == ScheduleCadence.EVERY_WEEK, { cadence = ScheduleCadence.EVERY_WEEK }, { Text("Every week", fontSize = 10.sp) })
                            FilterChip(cadence == ScheduleCadence.WEEKS_1_3, { cadence = ScheduleCadence.WEEKS_1_3 }, { Text("Weeks 1 + 3", fontSize = 10.sp) })
                        }
                        Spacer(Modifier.height(11.dp))
                        val nextTimes = WeeklyScheduleEngine.upcomingOccurrences(
                            listOf(slot.copy(title = title.trim(), dayOfWeek = day,
                                hour = hour, minute = minute, cadence = cadence, platform = platform,
                                contentType = contentType)), daysAhead = 14
                        ).take(2)
                        Text("NEXT POSTS · PREVIEW ONLY", color = MutedGold, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                        if (nextTimes.isEmpty()) Text("No future date in this 14-day preview.", color = MutedText, fontSize = 10.sp)
                        nextTimes.forEach {
                            Text(WeeklyScheduleEngine.formatOccurrence(it.publishAtMillis),
                                color = ProjectorIvory, fontSize = 11.sp)
                        }
                        Text("Saving this template never rewrites an existing project.", color = MutedText, fontSize = 10.sp)
                        Spacer(Modifier.height(12.dp))
                        V08Label("OPTIONAL EFFORT ESTIMATE")
                        OutlinedTextField(
                            value = estimateText,
                            onValueChange = { candidate ->
                                if (candidate.length <= 3 && candidate.all(Char::isDigit))
                                    estimateText = candidate
                            },
                            singleLine = true,
                            label = { Text("Minutes needed for this recurring post") },
                            supportingText = { Text("Leave blank if unknown · maximum 720 minutes") },
                            modifier = Modifier.fillMaxWidth(),
                        )
                        Spacer(Modifier.height(11.dp))
                        TextButton(onClick = { showAdvanced = !showAdvanced }, modifier = Modifier.fillMaxWidth()) {
                            Text(if (showAdvanced) "HIDE EXTRA OPTIONS" else "MORE OPTIONS · REMINDERS & PRIORITY",
                                color = MutedGold, fontSize = 10.sp)
                        }
                        AnimatedVisibility(visible = showAdvanced) {
                            Column {
                        Spacer(Modifier.height(12.dp)); V08Label("AUTO STAGE REMINDER")
                        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            listOf(ReminderMode.NONE, ReminderMode.SIMPLE, ReminderMode.SMART).forEach { value ->
                                FilterChip(reminderMode == value, { reminderMode = value }, { Text(value.name.lowercase().replaceFirstChar { it.uppercase() }, fontSize = 10.sp) })
                            }
                        }
                        Text(
                            when (reminderMode) {
                                ReminderMode.SMART -> "Reminder timing follows your project's next step."
                                ReminderMode.SIMPLE -> "One notification targets the current step deadline."
                                else -> "Projects are generated without an automatic reminder."
                            },
                            color = MutedText,
                            fontSize = 9.sp,
                            lineHeight = 13.sp,
                        )
                        Spacer(Modifier.height(12.dp)); V08Label("PRIORITY")
                        Row(horizontalArrangement = Arrangement.spacedBy(5.dp)) {
                            TaskPriority.entries.forEach { value -> FilterChip(priority == value, { priority = value }, { Text(value.name.lowercase().replaceFirstChar { it.uppercase() }, fontSize = 10.sp) }) }
                        }
                        Spacer(Modifier.height(14.dp))
                        Surface(Modifier.fillMaxWidth(), RoundedCornerShape(14.dp), CinemaSurface, border = androidx.compose.foundation.BorderStroke(1.dp, CinemaLine)) {
                            Column(Modifier.padding(12.dp)) {
                                Text("PRODUCTION RUNWAY", color = MutedText, fontSize = 8.sp, letterSpacing = 1.sp)
                                Spacer(Modifier.height(4.dp))
                                Text(template.stages.joinToString("  →  ") { it.label }, color = ProjectorIvory, fontSize = 9.5.sp, lineHeight = 14.sp)
                            }
                        }
                            }
                        }
                        if (onDelete != null) {
                            Spacer(Modifier.height(7.dp)); TextButton(onClick = onDelete, modifier = Modifier.fillMaxWidth()) { Text("DELETE SLOT", color = Color(0xFFE87A73), fontSize = 10.sp) }
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    onSave(slot.copy(
                        title = title.trim(), dayOfWeek = day, hour = hour, minute = minute,
                        platform = platform, contentType = contentType, cadence = cadence,
                        reminderMode = reminderMode, priority = priority,
                        estimatedMinutes = estimateText.toIntOrNull() ?: 0,
                    ))
                },
                enabled = title.isNotBlank() &&
                    (estimateText.isBlank() || estimateText.toIntOrNull()?.let { it in 0..720 } == true),
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

private fun v08Formats(platform: String): List<String> = CreatorPlatformRegistry.formats(platform)

private fun v08Time(slot: WeeklyScheduleSlot): String = v08Time(slot.hour, slot.minute)

private fun v08Time(hour: Int, minute: Int): String =
    LocalTime.of(hour.coerceIn(0, 23), minute.coerceIn(0, 59)).format(DateTimeFormatter.ofPattern("h:mm a", Locale.getDefault()))
