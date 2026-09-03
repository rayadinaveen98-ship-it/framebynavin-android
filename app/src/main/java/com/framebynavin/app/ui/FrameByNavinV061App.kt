package com.framebynavin.app.ui

import android.Manifest
import android.app.DatePickerDialog
import android.app.NotificationManager
import android.app.TimePickerDialog
import android.content.Intent
import android.content.pm.PackageManager
import android.media.RingtoneManager
import android.net.Uri
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.provider.Settings
import android.speech.tts.TextToSpeech
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.Alarm
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material.icons.outlined.MusicNote
import androidx.compose.material.icons.outlined.Notifications
import androidx.compose.material.icons.outlined.RecordVoiceOver
import androidx.compose.material.icons.outlined.Schedule
import androidx.compose.material.icons.outlined.Tune
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.viewmodel.compose.viewModel
import com.framebynavin.app.data.CreatorTask
import com.framebynavin.app.data.CreatorViewModel
import com.framebynavin.app.data.ReminderMode
import com.framebynavin.app.data.TaskPriority
import com.framebynavin.app.data.TaskStatus
import com.framebynavin.app.data.VoicePersona
import com.framebynavin.app.reminders.ReminderScheduler
import com.framebynavin.app.reminders.VoicePersonaEngine
import com.framebynavin.app.ui.theme.*
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

@Composable
fun FrameByNavinV061App(vm: CreatorViewModel = viewModel()) {
    val context = LocalContext.current
    val activity = context as? ComponentActivity
    val scheduler = remember { ReminderScheduler(context.applicationContext) }
    val notificationManager = remember { context.getSystemService(NotificationManager::class.java) }

    var showCenter by rememberSaveable { mutableStateOf(false) }
    var showComposer by rememberSaveable { mutableStateOf(false) }
    var editTaskId by rememberSaveable { mutableStateOf<String?>(null) }
    var notificationReady by remember { mutableStateOf(hasNotifications(context)) }
    var exactReady by remember { mutableStateOf(scheduler.canScheduleExact()) }
    var fullScreenReady by remember {
        mutableStateOf(Build.VERSION.SDK_INT < 34 || notificationManager.canUseFullScreenIntent())
    }

    val notificationPermissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { notificationReady = it }

    DisposableEffect(activity) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                notificationReady = hasNotifications(context)
                exactReady = scheduler.canScheduleExact()
                fullScreenReady = Build.VERSION.SDK_INT < 34 || notificationManager.canUseFullScreenIntent()
                vm.reconcileReminders()
            }
        }
        activity?.lifecycle?.addObserver(observer)
        onDispose { activity?.lifecycle?.removeObserver(observer) }
    }

    Box(Modifier.fillMaxSize()) {
        FrameByNavinV06App(vm)

        // One top-level interception point for Quick Add. The old V0 layers remain underneath
        // for compatibility, but the user always enters the unified composer from v0.6.1.
        Box(
            Modifier
                .align(Alignment.TopEnd)
                .statusBarsPadding()
                .padding(end = 6.dp)
                .size(76.dp)
                .clickable {
                    editTaskId = null
                    showComposer = true
                }
        )

        // Same rule for the existing red reminder button: it now opens the unified center.
        Box(
            Modifier
                .align(Alignment.BottomEnd)
                .navigationBarsPadding()
                .padding(end = 8.dp, bottom = 80.dp)
                .size(82.dp)
                .clickable { showCenter = true }
        )
    }

    if (showCenter) {
        UnifiedReminderCenter(
            tasks = vm.tasks,
            onDismiss = { showCenter = false },
            onNew = {
                showCenter = false
                editTaskId = null
                showComposer = true
            },
            onEdit = { id ->
                showCenter = false
                editTaskId = id
                showComposer = true
            }
        )
    }

    if (showComposer) {
        val task = editTaskId?.let { id -> vm.tasks.firstOrNull { it.id == id } }
        UnifiedReminderComposer(
            task = task,
            notificationReady = notificationReady,
            exactReady = exactReady,
            fullScreenReady = fullScreenReady,
            onDismiss = { showComposer = false },
            onRequestNotifications = {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                } else notificationReady = true
            },
            onRequestExact = {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                    context.startActivity(
                        Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM).apply {
                            data = Uri.parse("package:${context.packageName}")
                        }
                    )
                } else exactReady = true
            },
            onRequestFullScreen = {
                if (Build.VERSION.SDK_INT >= 34) {
                    context.startActivity(
                        Intent(Settings.ACTION_MANAGE_APP_USE_FULL_SCREEN_INTENT).apply {
                            data = Uri.parse("package:${context.packageName}")
                        }
                    )
                } else fullScreenReady = true
            },
            onSave = { draft ->
                vm.saveTaskConfiguration(
                    id = task?.id,
                    title = draft.title,
                    platform = draft.platform,
                    contentType = draft.contentType,
                    dueLabel = dueLabel(draft.dueAtMillis),
                    dueAtMillis = draft.dueAtMillis,
                    reminderMode = draft.mode,
                    reminderAtMillis = draft.reminderAtMillis,
                    priority = draft.priority,
                    notes = draft.notes,
                    alarmSoundUri = draft.alarmSoundUri,
                    voicePersona = draft.voicePersona,
                    voiceRepeatCount = draft.voiceRepeatCount,
                    voiceRepeatIntervalSeconds = draft.voiceRepeatIntervalSeconds,
                    alarmTimeoutSeconds = draft.alarmTimeoutSeconds,
                )
                showComposer = false
            },
            onRemoveReminder = {
                task?.let { vm.cancelReminder(it.id) }
                showComposer = false
            },
        )
    }
}

private data class ReminderDraft(
    val title: String,
    val platform: String,
    val contentType: String,
    val dueAtMillis: Long,
    val mode: ReminderMode,
    val reminderAtMillis: Long,
    val priority: TaskPriority,
    val notes: String,
    val alarmSoundUri: String,
    val voicePersona: VoicePersona,
    val voiceRepeatCount: Int,
    val voiceRepeatIntervalSeconds: Int,
    val alarmTimeoutSeconds: Int,
)

@Composable
private fun UnifiedReminderCenter(
    tasks: List<CreatorTask>,
    onDismiss: () -> Unit,
    onNew: () -> Unit,
    onEdit: (String) -> Unit,
) {
    val active = tasks.filter { it.status != TaskStatus.DONE && it.status != TaskStatus.SKIPPED }
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = CinemaSurfaceRaised,
        titleContentColor = ProjectorIvory,
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Outlined.Tune, null, tint = RecRed)
                Spacer(Modifier.width(8.dp))
                Column {
                    Text("Reminder Center", fontWeight = FontWeight.Black)
                    Text("Create and edit from one place", color = MutedText, fontSize = 9.5.sp)
                }
            }
        },
        text = {
            Column(Modifier.fillMaxWidth().heightIn(max = 590.dp).verticalScroll(rememberScrollState())) {
                Button(
                    onClick = onNew,
                    modifier = Modifier.fillMaxWidth().height(50.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = RecRed),
                    shape = RoundedCornerShape(14.dp),
                ) {
                    Icon(Icons.Outlined.Add, null)
                    Spacer(Modifier.width(7.dp))
                    Text("NEW TASK / REMINDER", fontWeight = FontWeight.Black)
                }
                Spacer(Modifier.height(18.dp))
                Text("ACTIVE CREATOR QUEUE", color = MutedText, fontSize = 9.sp, letterSpacing = 1.sp)
                Spacer(Modifier.height(8.dp))

                if (active.isEmpty()) {
                    Text("Nothing active yet. Create your next content item here.", color = MutedText, fontSize = 11.sp)
                }

                active.forEach { task ->
                    Surface(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 5.dp)
                            .clickable { onEdit(task.id) },
                        shape = RoundedCornerShape(16.dp),
                        color = CinemaSurface,
                        border = androidx.compose.foundation.BorderStroke(1.dp, CinemaLine),
                    ) {
                        Row(Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
                            Column(Modifier.weight(1f)) {
                                Text(task.title, color = ProjectorIvory, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                                Text("${task.platform} · ${task.contentType}", color = MutedText, fontSize = 9.5.sp)
                                Spacer(Modifier.height(3.dp))
                                Text(
                                    if (task.reminderEnabled && task.reminderMode != ReminderMode.NONE)
                                        "${modeLabel(task.reminderMode)} · ${formatDateTime(task.reminderAtMillis)}"
                                    else "No reminder",
                                    color = if (task.reminderEnabled) MutedGold else Color(0xFF77726C),
                                    fontSize = 9.5.sp,
                                )
                            }
                            Icon(Icons.Outlined.Edit, null, tint = MutedText, modifier = Modifier.size(18.dp))
                        }
                    }
                }
            }
        },
        confirmButton = { TextButton(onClick = onDismiss) { Text("CLOSE", color = MutedText) } },
    )
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun UnifiedReminderComposer(
    task: CreatorTask?,
    notificationReady: Boolean,
    exactReady: Boolean,
    fullScreenReady: Boolean,
    onDismiss: () -> Unit,
    onRequestNotifications: () -> Unit,
    onRequestExact: () -> Unit,
    onRequestFullScreen: () -> Unit,
    onSave: (ReminderDraft) -> Unit,
    onRemoveReminder: () -> Unit,
) {
    val context = LocalContext.current
    val now = System.currentTimeMillis()
    val fallbackDue = now + 60 * 60_000L

    var title by rememberSaveable(task?.id) { mutableStateOf(task?.title.orEmpty()) }
    var platform by rememberSaveable(task?.id) { mutableStateOf(task?.platform ?: "Instagram") }
    var contentType by rememberSaveable(task?.id) { mutableStateOf(task?.contentType ?: "Reel") }
    var dueAt by rememberSaveable(task?.id) {
        mutableLongStateOf(task?.dueAtMillis?.takeIf { it > now } ?: task?.reminderAtMillis?.takeIf { it > now } ?: fallbackDue)
    }
    var mode by rememberSaveable(task?.id) { mutableStateOf(task?.reminderMode ?: ReminderMode.SMART) }
    var reminderAt by rememberSaveable(task?.id) {
        mutableLongStateOf(task?.reminderAtMillis?.takeIf { it > now } ?: dueAt)
    }
    var priority by rememberSaveable(task?.id) { mutableStateOf(task?.priority ?: TaskPriority.IMPORTANT) }
    var notes by rememberSaveable(task?.id) { mutableStateOf(task?.notes.orEmpty()) }
    var soundUri by rememberSaveable(task?.id) {
        mutableStateOf(task?.alarmSoundUri?.takeIf { it.isNotBlank() } ?: RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM).toString())
    }
    var voicePersona by rememberSaveable(task?.id) { mutableStateOf(task?.voicePersona ?: VoicePersona.WARM) }
    var repeatCount by rememberSaveable(task?.id) { mutableIntStateOf(task?.voiceRepeatCount ?: 3) }
    var repeatInterval by rememberSaveable(task?.id) { mutableIntStateOf(task?.voiceRepeatIntervalSeconds ?: 20) }
    var alarmTimeout by rememberSaveable(task?.id) { mutableIntStateOf(task?.alarmTimeoutSeconds ?: 120) }

    val formats = formatsFor(platform)
    fun setPlatform(value: String) {
        platform = value
        contentType = formatsFor(value).first()
    }

    fun pickDateTime(current: Long, onPicked: (Long) -> Unit) {
        val initial = Calendar.getInstance().apply { timeInMillis = current }
        DatePickerDialog(
            context,
            { _, year, month, day ->
                TimePickerDialog(
                    context,
                    { _, hour, minute ->
                        onPicked(Calendar.getInstance().apply {
                            set(year, month, day, hour, minute, 0)
                            set(Calendar.MILLISECOND, 0)
                        }.timeInMillis)
                    },
                    initial.get(Calendar.HOUR_OF_DAY), initial.get(Calendar.MINUTE), false
                ).show()
            },
            initial.get(Calendar.YEAR), initial.get(Calendar.MONTH), initial.get(Calendar.DAY_OF_MONTH)
        ).show()
    }

    val ringtoneLauncher = rememberLauncherForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        @Suppress("DEPRECATION")
        val picked = result.data?.getParcelableExtra<Uri>(RingtoneManager.EXTRA_RINGTONE_PICKED_URI)
        if (picked != null) soundUri = picked.toString()
    }
    fun chooseTone() {
        ringtoneLauncher.launch(
            Intent(RingtoneManager.ACTION_RINGTONE_PICKER).apply {
                putExtra(RingtoneManager.EXTRA_RINGTONE_TYPE, RingtoneManager.TYPE_ALARM)
                putExtra(RingtoneManager.EXTRA_RINGTONE_SHOW_DEFAULT, true)
                putExtra(RingtoneManager.EXTRA_RINGTONE_SHOW_SILENT, false)
                putExtra(RingtoneManager.EXTRA_RINGTONE_EXISTING_URI, Uri.parse(soundUri))
                putExtra(RingtoneManager.EXTRA_RINGTONE_TITLE, "Choose FrameByNavin alarm sound")
            }
        )
    }

    val toneTitle = remember(soundUri) {
        runCatching { RingtoneManager.getRingtone(context, Uri.parse(soundUri))?.getTitle(context) }.getOrNull() ?: "Default alarm"
    }
    val needsReminder = mode != ReminderMode.NONE
    val needsExact = mode == ReminderMode.VOICE || mode == ReminderMode.ALARM || mode == ReminderMode.SMART
    val needsFullScreen = mode == ReminderMode.VOICE || mode == ReminderMode.ALARM || mode == ReminderMode.SMART
    val permissionReady = !needsReminder || (notificationReady && (!needsExact || exactReady) && (!needsFullScreen || fullScreenReady))
    val timeReady = !needsReminder || reminderAt > System.currentTimeMillis()

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = CinemaSurfaceRaised,
        titleContentColor = ProjectorIvory,
        textContentColor = MutedText,
        title = {
            Column {
                Text(if (task == null) "Create" else "Edit", color = RecRed, fontSize = 10.sp, letterSpacing = 1.2.sp, fontWeight = FontWeight.Bold)
                Text("Creator Reminder", fontWeight = FontWeight.Black)
            }
        },
        text = {
            Column(Modifier.fillMaxWidth().heightIn(max = 660.dp).verticalScroll(rememberScrollState())) {
                OutlinedTextField(title, { title = it }, label = { Text("Task / content title") }, singleLine = true, modifier = Modifier.fillMaxWidth())

                Spacer(Modifier.height(14.dp))
                TinyLabel("PLATFORM")
                FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    listOf("Instagram", "YouTube", "X").forEach { value ->
                        FilterChip(platform == value, { setPlatform(value) }, { Text(value) })
                    }
                }

                Spacer(Modifier.height(12.dp))
                TinyLabel("FORMAT")
                FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    formats.forEach { value -> FilterChip(contentType == value, { contentType = value }, { Text(value) }) }
                }

                Spacer(Modifier.height(16.dp))
                TinyLabel("PUBLISH / DUE TIME")
                DateTimeButton(formatDateTime(dueAt), "CHANGE", { pickDateTime(dueAt) { picked ->
                    dueAt = picked
                    if (task == null || reminderAt <= now) reminderAt = picked
                } })

                Spacer(Modifier.height(18.dp))
                TinyLabel("REMINDER MODE")
                FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    ReminderMode.entries.forEach { value ->
                        FilterChip(
                            selected = mode == value,
                            onClick = { mode = value },
                            label = { Text(modeLabel(value), fontSize = 10.sp) },
                        )
                    }
                }
                Spacer(Modifier.height(7.dp))
                Text(modeDescription(mode), color = MutedText, fontSize = 10.sp, lineHeight = 15.sp)

                if (needsReminder) {
                    Spacer(Modifier.height(16.dp))
                    PermissionCard(
                        notificationReady = notificationReady,
                        exactReady = exactReady,
                        fullScreenReady = fullScreenReady,
                        needsExact = needsExact,
                        needsFullScreen = needsFullScreen,
                        onRequestNotifications = onRequestNotifications,
                        onRequestExact = onRequestExact,
                        onRequestFullScreen = onRequestFullScreen,
                    )

                    Spacer(Modifier.height(16.dp))
                    TinyLabel(if (mode == ReminderMode.SMART) "SMART TARGET / DEADLINE" else "REMIND AT")
                    DateTimeButton(formatDateTime(reminderAt), "CHANGE", { pickDateTime(reminderAt) { reminderAt = it } })
                    Spacer(Modifier.height(7.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(5.dp)) {
                        listOf(1L to "1 MIN", 3L to "3 MIN", 10L to "10 MIN").forEach { (mins, label) ->
                            AssistChip(onClick = { reminderAt = System.currentTimeMillis() + mins * 60_000L }, label = { Text(label, fontSize = 8.5.sp) })
                        }
                    }

                    Spacer(Modifier.height(14.dp))
                    TinyLabel("PRIORITY")
                    Row(horizontalArrangement = Arrangement.spacedBy(5.dp)) {
                        TaskPriority.entries.forEach { value ->
                            FilterChip(priority == value, { priority = value }, { Text(value.name.lowercase().replaceFirstChar { it.uppercase() }, fontSize = 9.sp) })
                        }
                    }
                }

                if (mode == ReminderMode.VOICE || mode == ReminderMode.SMART) {
                    Spacer(Modifier.height(16.dp))
                    TinyLabel("VOICE")
                    VoicePersona.entries.forEach { persona ->
                        Surface(
                            modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp).clickable { voicePersona = persona },
                            shape = RoundedCornerShape(14.dp),
                            color = if (voicePersona == persona) Color(0xFF1B1711) else CinemaSurface,
                            border = androidx.compose.foundation.BorderStroke(1.dp, if (voicePersona == persona) MutedGold.copy(alpha = 0.65f) else CinemaLine),
                        ) {
                            Row(Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                                RadioButton(voicePersona == persona, { voicePersona = persona }, colors = RadioButtonDefaults.colors(selectedColor = MutedGold))
                                Column(Modifier.weight(1f)) {
                                    Text(VoicePersonaEngine.label(persona), color = ProjectorIvory, fontSize = 11.5.sp, fontWeight = FontWeight.Bold)
                                    if (persona == VoicePersona.WARM) Text("Recommended · calm and sweet", color = MutedGold, fontSize = 8.5.sp)
                                }
                                TextButton(onClick = { previewVoice(context, persona) }) { Text("PREVIEW", color = RecRed, fontSize = 8.5.sp) }
                            }
                        }
                    }

                    Spacer(Modifier.height(10.dp))
                    TinyLabel("VOICE REPEATS")
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        listOf(1, 2, 3).forEach { count -> FilterChip(repeatCount == count, { repeatCount = count }, { Text("$count×") }) }
                    }
                    Spacer(Modifier.height(6.dp))
                    TinyLabel("REPEAT GAP")
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        listOf(20, 30, 60).forEach { seconds -> FilterChip(repeatInterval == seconds, { repeatInterval = seconds }, { Text("${seconds}s", fontSize = 9.sp) }) }
                    }
                }

                if (mode == ReminderMode.ALARM || mode == ReminderMode.SMART) {
                    Spacer(Modifier.height(16.dp))
                    TinyLabel("ALARM SOUND")
                    OutlinedButton(onClick = { chooseTone() }, modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(13.dp)) {
                        Icon(Icons.Outlined.MusicNote, null, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(7.dp))
                        Text(toneTitle, color = ProjectorIvory, modifier = Modifier.weight(1f), maxLines = 1)
                        Text("CHANGE", color = RecRed, fontSize = 8.5.sp)
                    }
                    Spacer(Modifier.height(10.dp))
                    TinyLabel("AUTO-STOP ALARM")
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        listOf(30 to "30 sec", 60 to "1 min", 120 to "2 min", 300 to "5 min").forEach { (seconds, label) ->
                            FilterChip(alarmTimeout == seconds, { alarmTimeout = seconds }, { Text(label, fontSize = 9.sp) })
                        }
                    }
                }

                if (mode == ReminderMode.SMART) {
                    Spacer(Modifier.height(14.dp))
                    Surface(Modifier.fillMaxWidth(), RoundedCornerShape(15.dp), Color(0xFF15120F), border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF3A2B22))) {
                        Column(Modifier.padding(13.dp)) {
                            Text("SMART OWNS THE ESCALATION", color = ProjectorIvory, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                            Spacer(Modifier.height(5.dp))
                            Text("Normal: gentle reminder. Important: notification → voice → alarm. Critical: notification → voice → alarm → critical alarm. Your acknowledgement can pause or cancel the remaining stages.", color = MutedText, fontSize = 9.5.sp, lineHeight = 14.sp)
                        }
                    }
                }

                Spacer(Modifier.height(14.dp))
                OutlinedTextField(notes, { notes = it }, label = { Text("Notes / voice detail (optional)") }, minLines = 2, maxLines = 4, modifier = Modifier.fillMaxWidth())

                if (task != null && task.reminderEnabled) {
                    Spacer(Modifier.height(5.dp))
                    TextButton(onClick = onRemoveReminder) { Text("REMOVE REMINDER ONLY", color = MutedText, fontSize = 9.5.sp) }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    onSave(
                        ReminderDraft(
                            title = title,
                            platform = platform,
                            contentType = contentType,
                            dueAtMillis = dueAt,
                            mode = mode,
                            reminderAtMillis = if (mode == ReminderMode.NONE) 0L else reminderAt,
                            priority = priority,
                            notes = notes,
                            alarmSoundUri = soundUri,
                            voicePersona = voicePersona,
                            voiceRepeatCount = repeatCount,
                            voiceRepeatIntervalSeconds = repeatInterval,
                            alarmTimeoutSeconds = alarmTimeout,
                        )
                    )
                },
                enabled = title.isNotBlank() && dueAt > System.currentTimeMillis() && timeReady && permissionReady,
                colors = ButtonDefaults.buttonColors(containerColor = RecRed),
            ) {
                Text(if (task == null) "CREATE" else "SAVE")
            }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("CANCEL", color = MutedText) } },
    )
}

@Composable
private fun PermissionCard(
    notificationReady: Boolean,
    exactReady: Boolean,
    fullScreenReady: Boolean,
    needsExact: Boolean,
    needsFullScreen: Boolean,
    onRequestNotifications: () -> Unit,
    onRequestExact: () -> Unit,
    onRequestFullScreen: () -> Unit,
) {
    val allReady = notificationReady && (!needsExact || exactReady) && (!needsFullScreen || fullScreenReady)
    Surface(
        Modifier.fillMaxWidth(),
        RoundedCornerShape(15.dp),
        if (allReady) Color(0xFF101511) else Color(0xFF15120F),
        border = androidx.compose.foundation.BorderStroke(1.dp, if (allReady) SuccessGreen.copy(alpha = 0.25f) else Color(0xFF3A2B22)),
    ) {
        Column(Modifier.padding(12.dp)) {
            Text(if (allReady) "ANDROID READY" else "ANDROID SETUP", color = if (allReady) SuccessGreen else ProjectorIvory, fontSize = 9.5.sp, fontWeight = FontWeight.Bold)
            if (!notificationReady) TextButton(onClick = onRequestNotifications) { Text("ENABLE NOTIFICATIONS", color = RecRed) }
            if (needsExact && !exactReady) TextButton(onClick = onRequestExact) { Text("ALLOW EXACT ALARMS", color = RecRed) }
            if (needsFullScreen && !fullScreenReady) TextButton(onClick = onRequestFullScreen) { Text("ALLOW FULL-SCREEN ALERTS", color = RecRed) }
        }
    }
}

@Composable
private fun TinyLabel(text: String) {
    Text(text, color = MutedText, fontSize = 8.5.sp, letterSpacing = 1.sp, fontWeight = FontWeight.SemiBold)
    Spacer(Modifier.height(5.dp))
}

@Composable
private fun DateTimeButton(text: String, action: String, onClick: () -> Unit) {
    OutlinedButton(onClick = onClick, modifier = Modifier.fillMaxWidth().height(54.dp), shape = RoundedCornerShape(13.dp), border = androidx.compose.foundation.BorderStroke(1.dp, CinemaLine)) {
        Icon(Icons.Outlined.Schedule, null, modifier = Modifier.size(16.dp))
        Spacer(Modifier.width(8.dp))
        Text(text, color = ProjectorIvory, modifier = Modifier.weight(1f), fontSize = 11.sp, fontWeight = FontWeight.Bold)
        Text(action, color = RecRed, fontSize = 8.5.sp)
    }
}

private fun previewVoice(context: android.content.Context, persona: VoicePersona) {
    var engine: TextToSpeech? = null
    engine = TextToSpeech(context.applicationContext) { status ->
        if (status == TextToSpeech.SUCCESS) {
            engine?.language = Locale.getDefault()
            engine?.let { VoicePersonaEngine.apply(it, persona) }
            engine?.speak(
                "FrameByNavin. Your YouTube Short is due soon.",
                TextToSpeech.QUEUE_FLUSH,
                null,
                "preview-${persona.name}"
            )
            Handler(Looper.getMainLooper()).postDelayed({ engine?.shutdown() }, 7_000L)
        } else engine?.shutdown()
    }
}

private fun hasNotifications(context: android.content.Context): Boolean =
    Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU ||
        context.checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED

private fun formatsFor(platform: String): List<String> = when (platform) {
    "YouTube" -> listOf("Long-form", "Short", "Cinematic Moment")
    "X" -> listOf("Post", "Video", "Update")
    else -> listOf("Reel", "Post", "Story")
}

private fun modeLabel(mode: ReminderMode): String = when (mode) {
    ReminderMode.NONE -> "None"
    ReminderMode.SIMPLE -> "Simple"
    ReminderMode.VOICE -> "Voice"
    ReminderMode.ALARM -> "Alarm"
    ReminderMode.SMART -> "Smart"
}

private fun modeDescription(mode: ReminderMode): String = when (mode) {
    ReminderMode.NONE -> "No alert. Keep the item only in your creator queue."
    ReminderMode.SIMPLE -> "One clean notification at the chosen time."
    ReminderMode.VOICE -> "A spoken reminder with its own Voice screen. No alarm ringtone."
    ReminderMode.ALARM -> "A native alarm with your chosen system tone and a bounded ringing time."
    ReminderMode.SMART -> "FrameByNavin automatically chooses notification → voice → alarm stages based on priority and your response."
}

private fun formatDateTime(millis: Long): String =
    SimpleDateFormat("EEE, d MMM · h:mm a", Locale.getDefault()).format(Date(millis))

private fun dueLabel(millis: Long): String {
    val now = Calendar.getInstance()
    val selected = Calendar.getInstance().apply { timeInMillis = millis }
    val tomorrow = Calendar.getInstance().apply { add(Calendar.DAY_OF_YEAR, 1) }
    val time = SimpleDateFormat("h:mm a", Locale.getDefault()).format(Date(millis))
    return when {
        sameDayV061(now, selected) -> "Today · $time"
        sameDayV061(tomorrow, selected) -> "Tomorrow · $time"
        else -> SimpleDateFormat("EEE, d MMM · h:mm a", Locale.getDefault()).format(Date(millis))
    }
}

private fun sameDayV061(a: Calendar, b: Calendar): Boolean =
    a.get(Calendar.YEAR) == b.get(Calendar.YEAR) && a.get(Calendar.DAY_OF_YEAR) == b.get(Calendar.DAY_OF_YEAR)
