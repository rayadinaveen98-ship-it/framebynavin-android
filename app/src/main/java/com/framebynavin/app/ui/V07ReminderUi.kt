package com.framebynavin.app.ui

import android.app.DatePickerDialog
import android.app.TimePickerDialog
import android.content.Context
import android.content.Intent
import android.media.RingtoneManager
import android.net.Uri
import android.os.Handler
import android.os.Looper
import android.speech.tts.TextToSpeech
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.framebynavin.app.data.*
import com.framebynavin.app.reminders.VoicePersonaEngine
import com.framebynavin.app.ui.theme.*
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

internal data class V07ReminderDraft(
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
internal fun V07ReminderCenter(
    tasks: List<CreatorTask>,
    onDismiss: () -> Unit,
    onNew: () -> Unit,
    onEdit: (String) -> Unit,
) {
    val active = tasks.filter { it.status == TaskStatus.PLANNED || it.status == TaskStatus.WORKING }
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = CinemaSurfaceRaised,
        title = {
            Column {
                Text("REMINDERS", color = RecRed, fontSize = 8.5.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.1.sp)
                Text("Reminder Center", color = ProjectorIvory, fontWeight = FontWeight.Black)
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
                    Text("NEW PROJECT", fontWeight = FontWeight.Black)
                }
                Spacer(Modifier.height(15.dp))
                if (active.isEmpty()) {
                    Text("No active reminders.", color = MutedText, fontSize = 11.sp)
                }
                active.forEach { task ->
                    Surface(
                        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp).clickable { onEdit(task.id) },
                        shape = RoundedCornerShape(15.dp),
                        color = CinemaSurface,
                        border = BorderStroke(1.dp, CinemaLine),
                    ) {
                        Row(Modifier.padding(13.dp), verticalAlignment = Alignment.CenterVertically) {
                            Column(Modifier.weight(1f)) {
                                Text(task.title, color = ProjectorIvory, fontSize = 12.5.sp, fontWeight = FontWeight.Bold)
                                Text("${task.platform} · ${task.contentType}", color = MutedText, fontSize = 9.3.sp)
                                Text(
                                    if (task.reminderEnabled && task.reminderMode != ReminderMode.NONE)
                                        "${v07ModeLabel(task.reminderMode)} · ${v07FormatDateTime(task.reminderAtMillis)}"
                                    else "No reminder",
                                    color = if (task.reminderEnabled) MutedGold else MutedText,
                                    fontSize = 9.2.sp,
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
internal fun V07ReminderComposer(
    task: CreatorTask?,
    notificationReady: Boolean,
    exactReady: Boolean,
    fullScreenReady: Boolean,
    onDismiss: () -> Unit,
    onRequestNotifications: () -> Unit,
    onRequestExact: () -> Unit,
    onRequestFullScreen: () -> Unit,
    onSave: (V07ReminderDraft) -> Unit,
    onRemoveReminder: () -> Unit,
) {
    val context = LocalContext.current
    val defaults = remember { CreatorOsSettingsStore(context.applicationContext).snapshot() }
    val now = System.currentTimeMillis()
    val fallbackDue = now + 60 * 60_000L

    var title by rememberSaveable(task?.id) { mutableStateOf(task?.title.orEmpty()) }
    var platform by rememberSaveable(task?.id) { mutableStateOf(task?.platform ?: "Instagram") }
    var contentType by rememberSaveable(task?.id) { mutableStateOf(task?.contentType ?: "Reel") }
    var dueAt by rememberSaveable(task?.id) { mutableLongStateOf(task?.dueAtMillis?.takeIf { it > now } ?: task?.reminderAtMillis?.takeIf { it > now } ?: fallbackDue) }
    var mode by rememberSaveable(task?.id) { mutableStateOf(task?.reminderMode ?: ReminderMode.NONE) }
    var reminderAt by rememberSaveable(task?.id) { mutableLongStateOf(task?.reminderAtMillis?.takeIf { it > now } ?: dueAt) }
    var priority by rememberSaveable(task?.id) { mutableStateOf(task?.priority ?: TaskPriority.IMPORTANT) }
    var notes by rememberSaveable(task?.id) { mutableStateOf(task?.notes.orEmpty()) }
    var soundUri by rememberSaveable(task?.id) {
        mutableStateOf(task?.alarmSoundUri?.takeIf { it.isNotBlank() } ?: RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM).toString())
    }
    var persona by rememberSaveable(task?.id) { mutableStateOf(task?.voicePersona ?: defaults.defaultVoicePersona) }
    var repeatCount by rememberSaveable(task?.id) { mutableIntStateOf(task?.voiceRepeatCount ?: 3) }
    var repeatGap by rememberSaveable(task?.id) { mutableIntStateOf(task?.voiceRepeatIntervalSeconds ?: 10) }
    var alarmTimeout by rememberSaveable(task?.id) { mutableIntStateOf(task?.alarmTimeoutSeconds ?: defaults.defaultAlarmTimeoutSeconds) }

    val formats = v07FormatsFor(platform)
    LaunchedEffect(platform) { if (contentType !in formats) contentType = formats.first() }

    fun pickDateTime(current: Long, onPicked: (Long) -> Unit) {
        val initial = Calendar.getInstance().apply { timeInMillis = current }
        DatePickerDialog(context, { _, y, m, d ->
            TimePickerDialog(context, { _, h, min ->
                onPicked(Calendar.getInstance().apply {
                    set(y, m, d, h, min, 0)
                    set(Calendar.MILLISECOND, 0)
                }.timeInMillis)
            }, initial.get(Calendar.HOUR_OF_DAY), initial.get(Calendar.MINUTE), false).show()
        }, initial.get(Calendar.YEAR), initial.get(Calendar.MONTH), initial.get(Calendar.DAY_OF_MONTH)).show()
    }

    val ringtoneLauncher = rememberLauncherForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        @Suppress("DEPRECATION")
        val picked = result.data?.getParcelableExtra<Uri>(RingtoneManager.EXTRA_RINGTONE_PICKED_URI)
        if (picked != null) soundUri = picked.toString()
    }
    fun chooseTone() {
        ringtoneLauncher.launch(Intent(RingtoneManager.ACTION_RINGTONE_PICKER).apply {
            putExtra(RingtoneManager.EXTRA_RINGTONE_TYPE, RingtoneManager.TYPE_ALARM)
            putExtra(RingtoneManager.EXTRA_RINGTONE_SHOW_DEFAULT, true)
            putExtra(RingtoneManager.EXTRA_RINGTONE_SHOW_SILENT, false)
            putExtra(RingtoneManager.EXTRA_RINGTONE_EXISTING_URI, Uri.parse(soundUri))
            putExtra(RingtoneManager.EXTRA_RINGTONE_TITLE, "Choose reminder sound")
        })
    }

    val toneTitle = remember(soundUri) {
        runCatching { RingtoneManager.getRingtone(context, Uri.parse(soundUri))?.getTitle(context) }.getOrNull() ?: "Default alarm"
    }
    val needsReminder = mode != ReminderMode.NONE
    val needsAttentionSetup = mode == ReminderMode.VOICE || mode == ReminderMode.ALARM || mode == ReminderMode.SMART
    val setupMissing = needsReminder && (!notificationReady || (needsAttentionSetup && (!exactReady || !fullScreenReady)))
    val timesReady = dueAt > now && (!needsReminder || reminderAt > now)

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false),
    ) {
        Surface(Modifier.fillMaxSize(), color = CinemaBlack) {
            Column(Modifier.fillMaxSize().statusBarsPadding().navigationBarsPadding()) {
                Row(
                    Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    IconButton(onClick = onDismiss) { Icon(Icons.Outlined.ArrowBack, "Back", tint = ProjectorIvory) }
                    Spacer(Modifier.width(4.dp))
                    Column(Modifier.weight(1f)) {
                        Text(if (task == null) "NEW PROJECT" else "EDIT PROJECT", color = RecRed, fontSize = 8.5.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.1.sp)
                        Text("Creator Setup", color = ProjectorIvory, fontSize = 22.sp, fontWeight = FontWeight.Black)
                    }
                }

                Column(
                    Modifier.weight(1f).verticalScroll(rememberScrollState()).padding(horizontal = 20.dp).padding(bottom = 22.dp)
                ) {
                    Spacer(Modifier.height(8.dp))
                    V07SimpleLabel("WHAT ARE YOU MAKING?")
                    OutlinedTextField(
                        value = title,
                        onValueChange = { title = it },
                        placeholder = { Text("Project title") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(15.dp),
                    )

                    Spacer(Modifier.height(22.dp))
                    V07SimpleLabel("WHERE ARE YOU PUBLISHING?")
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        listOf("Instagram", "YouTube", "X").forEach { value ->
                            FilterChip(
                                selected = platform == value,
                                onClick = { platform = value },
                                label = { Text(value) },
                            )
                        }
                    }

                    Spacer(Modifier.height(18.dp))
                    V07SimpleLabel("FORMAT")
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        formats.forEach { value ->
                            FilterChip(
                                selected = contentType == value,
                                onClick = { contentType = value },
                                label = { Text(value, fontSize = 10.sp) },
                            )
                        }
                    }

                    Spacer(Modifier.height(22.dp))
                    V07SimpleLabel("PUBLISH BY")
                    V07DateTimeButton(v07FormatDateTime(dueAt)) {
                        pickDateTime(dueAt) { picked ->
                            dueAt = picked
                            if (task == null || reminderAt <= now) reminderAt = picked
                        }
                    }

                    Spacer(Modifier.height(24.dp))
                    Text("REMIND ME", color = ProjectorIvory, fontSize = 16.sp, fontWeight = FontWeight.Black)
                    Text("Choose how much attention this project needs.", color = MutedText, fontSize = 9.5.sp)
                    Spacer(Modifier.height(9.dp))
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(7.dp), verticalArrangement = Arrangement.spacedBy(7.dp)) {
                        ReminderMode.entries.forEach { value ->
                            FilterChip(
                                selected = mode == value,
                                onClick = { mode = value },
                                label = { Text(v07ModeLabel(value), fontSize = 9.5.sp) },
                            )
                        }
                    }

                    if (needsReminder) {
                        Spacer(Modifier.height(18.dp))
                        V07SimpleLabel("REMIND AT")
                        V07DateTimeButton(v07FormatDateTime(reminderAt)) { pickDateTime(reminderAt) { reminderAt = it } }
                    }

                    if (mode == ReminderMode.SMART) {
                        Spacer(Modifier.height(20.dp))
                        V07SimpleLabel("IMPORTANCE")
                        FlowRow(horizontalArrangement = Arrangement.spacedBy(7.dp)) {
                            TaskPriority.entries.forEach { value ->
                                FilterChip(priority == value, { priority = value }, { Text(v07PriorityLabel(value), fontSize = 9.sp) })
                            }
                        }
                    }

                    if (mode == ReminderMode.VOICE || mode == ReminderMode.SMART) {
                        Spacer(Modifier.height(20.dp))
                        V07SimpleLabel("VOICE")
                        VoicePersona.entries.forEach { value ->
                            Surface(
                                modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp).clickable { persona = value },
                                shape = RoundedCornerShape(15.dp),
                                color = if (persona == value) Color(0xFF18150F) else CinemaSurface,
                                border = BorderStroke(1.dp, if (persona == value) MutedGold.copy(alpha = .55f) else CinemaLine),
                            ) {
                                Row(Modifier.padding(horizontal = 12.dp, vertical = 9.dp), verticalAlignment = Alignment.CenterVertically) {
                                    RadioButton(persona == value, { persona = value }, colors = RadioButtonDefaults.colors(selectedColor = MutedGold))
                                    Text(VoicePersonaEngine.label(value), color = ProjectorIvory, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
                                    TextButton(onClick = { v07PreviewVoice(context, value) }) {
                                        Icon(Icons.Outlined.PlayArrow, null, tint = RecRed, modifier = Modifier.size(15.dp))
                                        Spacer(Modifier.width(3.dp))
                                        Text("PREVIEW", color = RecRed, fontSize = 8.sp)
                                    }
                                }
                            }
                        }

                        if (mode == ReminderMode.VOICE) {
                            Spacer(Modifier.height(14.dp))
                            V07SimpleLabel("REPEAT")
                            FlowRow(horizontalArrangement = Arrangement.spacedBy(7.dp)) {
                                listOf(1, 2, 3).forEach { count ->
                                    FilterChip(repeatCount == count, { repeatCount = count }, { Text("$count×") })
                                }
                            }
                            Spacer(Modifier.height(12.dp))
                            V07SimpleLabel("REPEAT GAP")
                            FlowRow(horizontalArrangement = Arrangement.spacedBy(7.dp), verticalArrangement = Arrangement.spacedBy(7.dp)) {
                                listOf(5, 10, 15, 30, 60).forEach { seconds ->
                                    FilterChip(repeatGap == seconds, { repeatGap = seconds }, { Text("${seconds}s", fontSize = 9.sp) })
                                }
                            }
                        }
                    }

                    if (mode == ReminderMode.ALARM || mode == ReminderMode.SMART) {
                        Spacer(Modifier.height(20.dp))
                        V07SimpleLabel("ALARM SOUND")
                        OutlinedButton(onClick = { chooseTone() }, modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(14.dp)) {
                            Icon(Icons.Outlined.MusicNote, null, modifier = Modifier.size(16.dp))
                            Spacer(Modifier.width(7.dp))
                            Text(toneTitle, color = ProjectorIvory, modifier = Modifier.weight(1f), maxLines = 1)
                            Text("CHANGE", color = RecRed, fontSize = 8.sp)
                        }

                        if (mode == ReminderMode.ALARM) {
                            Spacer(Modifier.height(12.dp))
                            V07SimpleLabel("AUTO-STOP")
                            FlowRow(horizontalArrangement = Arrangement.spacedBy(7.dp), verticalArrangement = Arrangement.spacedBy(7.dp)) {
                                listOf(30 to "30 sec", 60 to "1 min", 120 to "2 min", 300 to "5 min").forEach { (seconds, label) ->
                                    FilterChip(alarmTimeout == seconds, { alarmTimeout = seconds }, { Text(label, fontSize = 8.8.sp) })
                                }
                            }
                        }
                    }

                    if (setupMissing) {
                        Spacer(Modifier.height(17.dp))
                        Surface(
                            Modifier.fillMaxWidth(),
                            RoundedCornerShape(14.dp),
                            Color(0xFF15120F),
                            border = BorderStroke(1.dp, Color(0xFF3A2B22)),
                        ) {
                            Row(Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Outlined.Info, null, tint = MutedGold, modifier = Modifier.size(17.dp))
                                Spacer(Modifier.width(8.dp))
                                Text("Finish reminder setup in Control → Settings for reliable alerts.", color = MutedText, fontSize = 9.2.sp, modifier = Modifier.weight(1f))
                            }
                        }
                    }

                    Spacer(Modifier.height(21.dp))
                    V07SimpleLabel("NOTES · OPTIONAL")
                    OutlinedTextField(
                        value = notes,
                        onValueChange = { notes = it },
                        minLines = 2,
                        maxLines = 4,
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(15.dp),
                        placeholder = { Text("Anything you want to remember") },
                    )

                    if (task?.reminderEnabled == true) {
                        Spacer(Modifier.height(8.dp))
                        TextButton(onClick = onRemoveReminder) { Text("REMOVE REMINDER", color = MutedText, fontSize = 9.sp) }
                    }
                }

                Surface(color = CinemaSurfaceRaised, tonalElevation = 3.dp) {
                    Row(
                        Modifier.fillMaxWidth().navigationBarsPadding().padding(horizontal = 20.dp, vertical = 12.dp),
                        horizontalArrangement = Arrangement.spacedBy(9.dp),
                    ) {
                        OutlinedButton(onClick = onDismiss, modifier = Modifier.weight(1f), border = BorderStroke(1.dp, CinemaLine)) {
                            Text("CANCEL", color = MutedText)
                        }
                        Button(
                            onClick = {
                                onSave(
                                    V07ReminderDraft(
                                        title = title,
                                        platform = platform,
                                        contentType = contentType,
                                        dueAtMillis = dueAt,
                                        mode = mode,
                                        reminderAtMillis = if (mode == ReminderMode.NONE) 0L else reminderAt,
                                        priority = priority,
                                        notes = notes,
                                        alarmSoundUri = soundUri,
                                        voicePersona = persona,
                                        voiceRepeatCount = repeatCount,
                                        voiceRepeatIntervalSeconds = repeatGap,
                                        alarmTimeoutSeconds = alarmTimeout,
                                    )
                                )
                            },
                            enabled = title.isNotBlank() && timesReady,
                            modifier = Modifier.weight(1.35f),
                            colors = ButtonDefaults.buttonColors(containerColor = RecRed),
                        ) {
                            Text(if (task == null) "CREATE PROJECT" else "SAVE", fontWeight = FontWeight.Black, fontSize = 9.5.sp)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun V07SimpleLabel(text: String) {
    Text(text, color = MutedText, fontSize = 8.4.sp, letterSpacing = .9.sp, fontWeight = FontWeight.Bold)
    Spacer(Modifier.height(7.dp))
}

@Composable
private fun V07DateTimeButton(text: String, onClick: () -> Unit) {
    OutlinedButton(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth().height(54.dp),
        border = BorderStroke(1.dp, CinemaLine),
        shape = RoundedCornerShape(14.dp),
    ) {
        Icon(Icons.Outlined.Schedule, null, modifier = Modifier.size(16.dp))
        Spacer(Modifier.width(8.dp))
        Text(text, color = ProjectorIvory, modifier = Modifier.weight(1f), fontSize = 10.5.sp, fontWeight = FontWeight.Bold)
        Text("CHANGE", color = RecRed, fontSize = 8.sp)
    }
}

private fun v07PreviewVoice(context: Context, persona: VoicePersona) {
    var engine: TextToSpeech? = null
    engine = TextToSpeech(context.applicationContext) { status ->
        if (status == TextToSpeech.SUCCESS) {
            engine?.language = Locale.getDefault()
            engine?.let { VoicePersonaEngine.apply(it, persona) }
            engine?.speak("FrameByNavin. This is ${VoicePersonaEngine.label(persona)}.", TextToSpeech.QUEUE_FLUSH, null, "voice-${persona.name}")
            Handler(Looper.getMainLooper()).postDelayed({ engine?.shutdown() }, 7_000L)
        } else engine?.shutdown()
    }
}

internal fun v07FormatsFor(platform: String): List<String> = when (platform) {
    "YouTube" -> listOf("Long-form", "Short", "Cinematic Moment")
    "X" -> listOf("Post", "Video", "Update")
    else -> listOf("Reel", "Post", "Story")
}

internal fun v07ModeLabel(mode: ReminderMode): String = when (mode) {
    ReminderMode.NONE -> "None"
    ReminderMode.SIMPLE -> "Simple"
    ReminderMode.VOICE -> "Voice"
    ReminderMode.ALARM -> "Alarm"
    ReminderMode.SMART -> "Smart"
}

private fun v07PriorityLabel(priority: TaskPriority): String = when (priority) {
    TaskPriority.NORMAL -> "Normal"
    TaskPriority.IMPORTANT -> "Important"
    TaskPriority.CRITICAL -> "Critical"
}

internal fun v07FormatDateTime(millis: Long): String =
    SimpleDateFormat("EEE, d MMM · h:mm a", Locale.getDefault()).format(Date(millis))

internal fun v07DueLabel(millis: Long): String {
    val now = Calendar.getInstance()
    val selected = Calendar.getInstance().apply { timeInMillis = millis }
    val tomorrow = Calendar.getInstance().apply { add(Calendar.DAY_OF_YEAR, 1) }
    val time = SimpleDateFormat("h:mm a", Locale.getDefault()).format(Date(millis))
    fun same(a: Calendar, b: Calendar) =
        a.get(Calendar.YEAR) == b.get(Calendar.YEAR) && a.get(Calendar.DAY_OF_YEAR) == b.get(Calendar.DAY_OF_YEAR)
    return when {
        same(now, selected) -> "Today · $time"
        same(tomorrow, selected) -> "Tomorrow · $time"
        else -> SimpleDateFormat("EEE, d MMM · h:mm a", Locale.getDefault()).format(Date(millis))
    }
}
