package com.framebynavin.app.ui

import android.Manifest
import android.app.AlarmManager
import android.app.DatePickerDialog
import android.app.TimePickerDialog
import android.content.Intent
import android.content.Context
import android.os.Build
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.framebynavin.app.personal.*
import com.framebynavin.app.ui.theme.*
import java.time.DayOfWeek
import java.time.Instant
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Calendar
import java.util.Locale
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.flow.collect

/**
 * V223.2 real Personal UI. No fake CreatorTask; no project or YouTube writes.
 * This screen uses existing app-owned full screen (no conflicting nav insets).
 */
@Composable
internal fun V2232PersonalReminderHub(
    onClose: () -> Unit,
    onCreator: () -> Unit,
) {
    val context = LocalContext.current
    val store = remember(context) { PersonalReminderStore(context) }
    val commands = remember(context) { PersonalReminderCommands(context) }
    val coordinator = remember(context) { PersonalDeliveryCoordinator(context) }
    val scope = rememberCoroutineScope()
    var reminders by remember { mutableStateOf(emptyList<PersonalReminder>()) }
    var loadError by remember { mutableStateOf<String?>(null) }
    var loaded by remember { mutableStateOf(false) }
    var busy by remember { mutableStateOf(false) }
    var editing by remember { mutableStateOf<PersonalReminder?>(null) }
    var editorOpen by remember { mutableStateOf(false) }
    var deleteTarget by remember { mutableStateOf<PersonalReminder?>(null) }
    var infoMessage by remember { mutableStateOf<String?>(null) }
    // Recompute local calendar labels while this screen is visible; the next
    // occurrence journal and Android alarms remain authoritative.
    var agendaNowMillis by remember { mutableLongStateOf(System.currentTimeMillis()) }
    LaunchedEffect(Unit) {
        while (true) {
            delay(60_000L)
            agendaNowMillis = System.currentTimeMillis()
        }
    }

    LaunchedEffect(store) {
        try {
            store.remindersFlow.collect { records ->
                reminders = records.sortedWith(
                    compareBy<PersonalReminder> { it.paused }
                        .thenBy { it.pendingFireAtMillis.takeIf { time -> time > 0L }
                            ?: PersonalRecurrence.nextAfter(it, System.currentTimeMillis())
                            ?: Long.MAX_VALUE }
                )
                loaded = true
                loadError = null
            }
        } catch (error: Exception) {
            loadError = "Personal data couldn't be opened. Nothing was overwritten."
            loaded = true
        }
    }

    fun perform(block: suspend () -> Unit) {
        if (busy) return
        busy = true
        scope.launch {
            try {
                block()
                infoMessage = null
            } catch (error: Exception) {
                infoMessage = error.message ?: "Could not save. Your existing reminders were kept."
            } finally {
                busy = false
            }
        }
    }

    var permissionRevision by remember { mutableIntStateOf(0) }
    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                permissionRevision++
                agendaNowMillis = System.currentTimeMillis()
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }
    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { permissionRevision++ }
    val notificationsReady = remember(permissionRevision, context) {
        PersonalReminderNotifications.canPost(context)
    }
    val preciseReady = remember(permissionRevision, context) {
        val am = context.getSystemService(AlarmManager::class.java)
        Build.VERSION.SDK_INT < Build.VERSION_CODES.S || am.canScheduleExactAlarms()
    }

    CreatorAdaptiveFullScreen(onDismissRequest = onClose) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(start = 11.dp, end = 15.dp, top = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            IconButton(onClick = onClose) {
                Icon(Icons.Outlined.ArrowBack, contentDescription = "Close reminders", tint = ProjectorIvory)
            }
            Column(Modifier.weight(1f)) {
                Text("PERSONAL REMINDERS", color = MutedGold, fontSize = 10.sp, fontWeight = FontWeight.Black)
                Text("Your everyday life", color = ProjectorIvory, fontSize = 19.sp, fontWeight = FontWeight.Bold)
            }
            IconButton(
                enabled = !busy && loadError == null,
                onClick = { editing = null; editorOpen = true },
                modifier = Modifier.testTag("v2232-personal-add"),
            ) {
                Icon(Icons.Outlined.AddCircleOutline, "Create personal reminder", tint = ProjectorIvory)
            }
        }

        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 18.dp, vertical = 7.dp)
                .testTag("v2232-personal-space-switcher"),
            horizontalArrangement = Arrangement.spacedBy(9.dp),
        ) {
            Surface(onClick = onCreator,
                modifier = Modifier.weight(1f).height(43.dp).testTag("v2232-open-creator"),
                color = CinemaSurface,
                shape = RoundedCornerShape(13.dp),
                border = BorderStroke(1.dp, CinemaLine)) {
                Box(contentAlignment = Alignment.Center) {
                    Text("CREATOR", color = ProjectorIvory, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                }
            }
            Surface(
                modifier = Modifier.weight(1f).height(43.dp),
                color = MutedGold.copy(alpha = .14f),
                shape = RoundedCornerShape(13.dp),
                border = BorderStroke(1.dp, MutedGold.copy(alpha = .55f))) {
                Box(contentAlignment = Alignment.Center) {
                    Text("PERSONAL", color = ProjectorIvory, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                }
            }
        }
        Column(
            modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState())
                .padding(horizontal = 18.dp, vertical = 10.dp)
                .padding(bottom = 56.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text("Private reminders, separate from your creative projects.",
                color = MutedText, fontSize = 12.sp)

            if (!notificationsReady) {
                Surface(
                    color = CinemaSurface,
                    shape = RoundedCornerShape(14.dp),
                    border = BorderStroke(1.dp, RecRed.copy(alpha = .42f)),
                ) {
                    Column(Modifier.padding(13.dp)) {
                        Text("Notifications need permission", color = ProjectorIvory, fontWeight = FontWeight.Bold)
                        Text("Personal alerts cannot be shown until notifications are enabled.",
                            color = MutedText, fontSize = 11.sp)
                        TextButton(onClick = {
                            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                                permissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                            } else {
                                context.startActivity(Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS)
                                    .putExtra(Settings.EXTRA_APP_PACKAGE, context.packageName))
                            }
                        }) {
                            Text("ENABLE NOTIFICATIONS", color = MutedGold)
                        }
                    }
                }
            } else if (!preciseReady) {
                Text("Exact timing is unavailable; Android may deliver personal alerts later.",
                    color = MutedGold, fontSize = 11.sp)
            }
            // The standalone Personal media/ringing services are intentionally
            // not enabled: a selected Voice or Alarm is clearly labeled fallback.
            Text("Voice and Alarm attempt short audio playback when Android permits; notifications and response buttons stay available.",
                color = MutedText, fontSize = 10.sp,
                modifier = Modifier.testTag("v2232-personal-fallback-notice"))

            if (loadError != null) {
                Text(loadError.orEmpty(), color = RecRed,
                    modifier = Modifier.testTag("v2232-personal-load-error"))
            } else if (!loaded) {
                CircularProgressIndicator(color = MutedGold)
            } else if (reminders.isEmpty()) {
                Surface(
                    color = CinemaSurface, shape = RoundedCornerShape(20.dp),
                    border = BorderStroke(1.dp, CinemaLine),
                    modifier = Modifier.fillMaxWidth().testTag("v2232-personal-empty"),
                ) {
                    Column(Modifier.padding(23.dp), horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        Icon(Icons.Outlined.NotificationsNone, null, tint = MutedGold,
                            modifier = Modifier.size(30.dp))
                        Text("Nothing to remember yet", color = ProjectorIvory,
                            fontWeight = FontWeight.Bold)
                        Text("Keep life simple: what, when and how you want to be reminded.",
                            color = MutedText, fontSize = 12.sp)
                        Button(onClick = { editing = null; editorOpen = true },
                            modifier = Modifier.testTag("v2232-personal-empty-add"),
                            colors = ButtonDefaults.buttonColors(containerColor = RecRed)) {
                            Text("ADD PERSONAL REMINDER")
                        }
                    }
                }
            } else {
                Text("${reminders.count { !it.paused }} ACTIVE · ${reminders.count { it.paused }} PAUSED",
                    color = MutedGold, fontWeight = FontWeight.Bold, fontSize = 10.sp,
                    modifier = Modifier.testTag("v2232-personal-count"))
                val agendaGroups = remember(reminders, agendaNowMillis) {
                    PersonalAgendaPolicy.groups(reminders, agendaNowMillis)
                }
                agendaGroups.forEach { group ->
                    Row(
                        modifier = Modifier.fillMaxWidth()
                            .testTag("v2282-personal-section-${group.section.name}"),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween,
                    ) {
                        Text(group.section.label, color = if (group.section == PersonalAgendaSection.NEEDS_ATTENTION)
                            RecRed else MutedGold, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                        Text("${group.items.size}", color = MutedText, fontSize = 11.sp)
                    }
                    group.items.forEach { item ->
                        V2232PersonalItem(
                            reminder = item,
                            nowMillis = agendaNowMillis,
                            busy = busy,
                            onEdit = { editing = item; editorOpen = true },
                            onPause = { perform { commands.setPaused(item.id, !item.paused) } },
                            onDelete = { deleteTarget = item },
                            onRespond = { action ->
                                perform {
                                    val result = coordinator.apply(
                                        id = item.id, originalOccurrence = item.pendingOccurrenceMillis,
                                        token = item.pendingToken, action = action,
                                    )
                                    check(result == PersonalApplyResult.ACCEPTED) {
                                        "This notification is no longer active. Reopen the reminder."
                                    }
                                }
                            },
                        )
                    }
                }
            }
            if (infoMessage != null) {
                Text(infoMessage.orEmpty(), color = RecRed,
                    modifier = Modifier.testTag("v2232-personal-feedback"))
            }
        }
    }

    if (editorOpen) {
        V2232PersonalEditor(
            existing = editing,
            saving = busy,
            onDismiss = { if (!busy) editorOpen = false },
            onSave = { draft ->
                perform {
                    if (editing == null) commands.create(draft)
                    else commands.revise(editing!!.id, draft)
                    editorOpen = false
                    editing = null
                }
            },
        )
    }
    deleteTarget?.let { item ->
        AlertDialog(
            onDismissRequest = { if (!busy) deleteTarget = null },
            title = { Text("Delete personal reminder?") },
            text = { Text("This removes only this personal reminder. Creator projects stay unchanged.") },
            confirmButton = {
                TextButton(enabled = !busy, onClick = {
                    perform {
                        commands.delete(item.id)
                        deleteTarget = null
                    }
                }) { Text("DELETE", color = RecRed) }
            },
            dismissButton = {
                TextButton(onClick = { deleteTarget = null }) { Text("CANCEL") }
            },
        )
    }
}

@Composable
private fun V2232PersonalItem(
    reminder: PersonalReminder,
    nowMillis: Long,
    busy: Boolean,
    onEdit: () -> Unit,
    onPause: () -> Unit,
    onDelete: () -> Unit,
    onRespond: (String) -> Unit,
) {
    val deliveryHealth = PersonalDeliveryHealthPolicy.status(reminder, nowMillis)
    val next = PersonalAgendaPolicy.dueAtMillis(reminder, nowMillis)
    val whenLabel = next?.let {
        Instant.ofEpochMilli(it).atZone(reminder.zoneId)
            .format(DateTimeFormatter.ofPattern("EEE, d MMM · h:mm a", Locale.getDefault()))
    } ?: "No upcoming occurrence"
    Surface(
        color = CinemaSurface,
        shape = RoundedCornerShape(17.dp),
        border = BorderStroke(1.dp, CinemaLine),
        modifier = Modifier.fillMaxWidth().testTag("v2232-personal-item"),
    ) {
        Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Outlined.NotificationsActive, null,
                    tint = if (reminder.paused) MutedText else MutedGold,
                    modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(10.dp))
                Column(Modifier.weight(1f)) {
                    Text(reminder.title, color = ProjectorIvory, fontSize = 15.sp,
                        fontWeight = FontWeight.Bold, maxLines = 2,
                        overflow = TextOverflow.Ellipsis)
                    Text(if (reminder.paused) "PAUSED" else whenLabel,
                        color = MutedText, fontSize = 11.sp)
                }
            }
            Text("${reminder.repeat.name.replace('_', ' ')} · ${reminder.alert.name}" +
                    "${if (reminder.alert == PersonalAlert.NOTIFICATION) "" else " (audio when permitted)"}",
                color = MutedText, fontSize = 10.sp)
            Text(
                deliveryHealth.label,
                color = if (deliveryHealth.needsAttention) RecRed else MutedGold,
                fontSize = 11.sp,
                modifier = Modifier.testTag("v2282-personal-delivery-health"),
            )
            if (reminder.lastMissedOccurrenceMillis > reminder.lastAcknowledgedOccurrenceMillis) {
                Text(
                    "MISSED · An earlier reminder received no response",
                    color = RecRed, fontSize = 10.sp,
                    modifier = Modifier.testTag("v2234-personal-missed"),
                )
            }
            Row(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(5.dp)) {
                TextButton(enabled = !busy, onClick = onEdit) { Text("EDIT", color = ProjectorIvory) }
                TextButton(enabled = !busy, onClick = onPause) {
                    Text(if (reminder.paused) "RESUME" else "PAUSE", color = MutedGold)
                }
                TextButton(enabled = !busy, onClick = onDelete) { Text("DELETE", color = RecRed) }
            }
            if (reminder.presentedToken.isNotBlank() &&
                reminder.presentedToken == reminder.pendingToken && !reminder.paused) {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedButton(enabled = !busy,
                        onClick = { onRespond(PersonalDeliveryContract.ACTION_DONE) }) {
                        Text("DONE")
                    }
                    OutlinedButton(enabled = !busy,
                        onClick = { onRespond(PersonalDeliveryContract.ACTION_SNOOZE) }) {
                        Text("SNOOZE 10m")
                    }
                }
            }
        }
    }
}

@Composable
internal fun V2232PersonalEditor(
    existing: PersonalReminder?,
    saving: Boolean,
    onDismiss: () -> Unit,
    onSave: (PersonalReminderDraft) -> Unit,
) {
    val context = LocalContext.current
    val zone = ZoneId.systemDefault()
    val selectedStart = remember(existing?.id) {
        existing?.startDate?.atTime(existing.time)?.atZone(existing.zoneId)?.toInstant()?.toEpochMilli()
            ?: (System.currentTimeMillis() + 60 * 60_000L)
    }
    var selectedTime by rememberSaveable(existing?.id) { mutableLongStateOf(selectedStart) }
    var title by rememberSaveable(existing?.id) { mutableStateOf(existing?.title.orEmpty()) }
    var notes by rememberSaveable(existing?.id) { mutableStateOf(existing?.notes.orEmpty()) }
    var repeat by rememberSaveable(existing?.id) {
        mutableStateOf(existing?.repeat?.name ?: PersonalRepeat.ONCE.name)
    }
    var alert by rememberSaveable(existing?.id) {
        mutableStateOf(existing?.alert?.name ?: PersonalAlert.NOTIFICATION.name)
    }
    var daySelection by rememberSaveable(existing?.id) {
        mutableStateOf(existing?.weeklyDays?.joinToString(",") { it.name }
            ?: Instant.ofEpochMilli(selectedStart).atZone(zone).dayOfWeek.name)
    }
    var everyNDays by rememberSaveable(existing?.id) { mutableStateOf((existing?.everyNDays ?: 3).toString()) }
    var error by remember { mutableStateOf<String?>(null) }
    val repeatOptions = PersonalRepeat.entries
    val alertOptions = PersonalAlert.entries
    val date = Instant.ofEpochMilli(selectedTime).atZone(zone)
    val weekDays = remember(daySelection) {
        daySelection.split(",").mapNotNull { runCatching { DayOfWeek.valueOf(it) }.getOrNull() }.toSet()
    }

    // V227.3: avoid a second Android Dialog window. On API 35, a separately
    // inset Dialog can shift its Cancel button outside the display despite
    // the Compose root measuring it inside its own bounds. The existing
    // Activity-owned overlay handles safe drawing and IME exactly once.
    CreatorAdaptiveFullScreen(onDismissRequest = onDismiss) {
        Surface(
            modifier = Modifier.fillMaxSize()
                .testTag("v2232-personal-editor"),
            color = CinemaSurfaceRaised,
            shape = RoundedCornerShape(20.dp),
        ) {
            Column(Modifier.fillMaxSize().padding(18.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        Text(if (existing == null) "NEW PERSONAL REMINDER" else "EDIT PERSONAL REMINDER",
                            color = MutedGold, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                        Text("What, when and how", color = ProjectorIvory,
                            fontSize = 19.sp, fontWeight = FontWeight.Bold)
                    }
                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Outlined.Close, contentDescription = "Close editor", tint = ProjectorIvory)
                    }
                }
                Column(Modifier.weight(1f).verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(11.dp)) {
                    OutlinedTextField(
                        value = title, onValueChange = { title = it.take(180); error = null },
                        label = { Text("What to remember?") }, singleLine = true,
                        modifier = Modifier.fillMaxWidth().testTag("v2232-personal-title"),
                    )
                    OutlinedTextField(
                        value = notes, onValueChange = { notes = it.take(2000) },
                        label = { Text("Optional notes") }, minLines = 2, maxLines = 3,
                        modifier = Modifier.fillMaxWidth(),
                    )
                    OutlinedButton(onClick = {
                        v2232PickDateTime(context, selectedTime) { selectedTime = it; error = null }
                    }, modifier = Modifier.fillMaxWidth().testTag("v2232-personal-date")) {
                        Icon(Icons.Outlined.Event, null)
                        Spacer(Modifier.width(7.dp))
                        Text(date.format(DateTimeFormatter.ofPattern("EEE, d MMM yyyy · h:mm a")))
                    }
                    Text("REPEAT", color = MutedText, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                    Row(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(7.dp)) {
                        repeatOptions.forEach { choice ->
                            FilterChip(selected = repeat == choice.name,
                                onClick = { repeat = choice.name },
                                label = { Text(choice.name.replace('_', ' '), fontSize = 10.sp) })
                        }
                    }
                    if (repeat == PersonalRepeat.WEEKLY.name) {
                        Row(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                            horizontalArrangement = Arrangement.spacedBy(5.dp)) {
                            DayOfWeek.values().forEach { day ->
                                FilterChip(selected = day in weekDays,
                                    onClick = {
                                        val next = if (day in weekDays) weekDays - day else weekDays + day
                                        daySelection = next.sortedBy { it.value }.joinToString(",") { it.name }
                                    },
                                    label = { Text(day.name.take(3), fontSize = 10.sp) })
                            }
                        }
                    }
                    if (repeat == PersonalRepeat.EVERY_N_DAYS.name) {
                        OutlinedTextField(
                            value = everyNDays,
                            onValueChange = { everyNDays = it.filter(Char::isDigit).take(3) },
                            label = { Text("Every how many days? (1–365)") },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth(),
                        )
                    }
                    Text("REMIND ME WITH", color = MutedText, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                    Row(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(7.dp)) {
                        alertOptions.forEach { choice ->
                            FilterChip(selected = alert == choice.name,
                                onClick = { alert = choice.name },
                                label = { Text(choice.name, fontSize = 10.sp) })
                        }
                    }
                    if (alert != PersonalAlert.NOTIFICATION.name) {
                        Text("Voice and Alarm play short audio when Android allows; the notification remains your fallback.",
                            color = MutedGold, fontSize = 11.sp)
                    }
                    error?.let {
                        Text(it, color = RecRed, modifier = Modifier.testTag("v2232-editor-error"))
                    }
                }
                // V227.1: keyboard-safe, adaptive actions; retains all validation.
                V2271PersonalEditorFooter(
                    saving = saving,
                    onCancel = onDismiss,
                        onSave = {
                            try {
                                val chosen = Instant.ofEpochMilli(selectedTime).atZone(zone)
                                val draft = PersonalReminderDraft(
                                    title = title.trim(),
                                    notes = notes.trim(),
                                    startDate = chosen.toLocalDate(),
                                    time = chosen.toLocalTime().withSecond(0).withNano(0),
                                    zoneId = zone,
                                    repeat = PersonalRepeat.valueOf(repeat),
                                    weeklyDays = if (repeat == PersonalRepeat.WEEKLY.name)
                                        weekDays else emptySet(),
                                    everyNDays = everyNDays.toIntOrNull() ?: 0,
                                    alert = PersonalAlert.valueOf(alert),
                                ).checked()
                                onSave(draft)
                            } catch (invalid: Exception) {
                                error = invalid.message ?: "Check the reminder details."
                            }
                },
                )
            }
        }
    }
}

private fun v2232PickDateTime(context: Context, currentMillis: Long, onPicked: (Long) -> Unit) {
    val initial = Calendar.getInstance().apply { timeInMillis = currentMillis }
    DatePickerDialog(context, { _, year, month, day ->
        TimePickerDialog(context, { _, hour, minute ->
            val chosen = Calendar.getInstance().apply {
                set(year, month, day, hour, minute, 0)
                set(Calendar.MILLISECOND, 0)
            }.timeInMillis
            onPicked(chosen)
        }, initial.get(Calendar.HOUR_OF_DAY), initial.get(Calendar.MINUTE), false).show()
    }, initial.get(Calendar.YEAR), initial.get(Calendar.MONTH), initial.get(Calendar.DAY_OF_MONTH)).show()
}
