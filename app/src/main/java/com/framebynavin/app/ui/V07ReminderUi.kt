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
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material.icons.outlined.MusicNote
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
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Outlined.Tune, null, tint = RecRed)
                Spacer(Modifier.width(8.dp))
                Column {
                    Text("Reminder Center", color = ProjectorIvory, fontWeight = FontWeight.Black)
                    Text("One editor for every reminder mode", color = MutedText, fontSize = 9.5.sp)
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
                Spacer(Modifier.height(16.dp))
                Text("ACTIVE QUEUE", color = MutedText, fontSize = 9.sp, letterSpacing = 1.sp)
                Spacer(Modifier.height(7.dp))
                if (active.isEmpty()) Text("Nothing active yet.", color = MutedText, fontSize = 11.sp)
                active.forEach { task ->
                    Surface(
                        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp).clickable { onEdit(task.id) },
                        shape = RoundedCornerShape(15.dp),
                        color = CinemaSurface,
                        border = androidx.compose.foundation.BorderStroke(1.dp, CinemaLine),
                    ) {
                        Row(Modifier.padding(13.dp), verticalAlignment = Alignment.CenterVertically) {
                            Column(Modifier.weight(1f)) {
                                Text(task.title, color = ProjectorIvory, fontSize = 12.5.sp, fontWeight = FontWeight.Bold)
                                Text("${task.platform} · ${task.contentType} · ${CreatorWorkflowEngine.currentStage(task).label}", color = MutedText, fontSize = 9.3.sp)
                                Text(
                                    if (task.reminderEnabled && task.reminderMode != ReminderMode.NONE)
                                        "${v07ModeLabel(task.reminderMode)} · ${v07FormatDateTime(task.reminderAtMillis)}"
                                    else "No reminder",
                                    color = if (task.reminderEnabled) MutedGold else Color(0xFF6F6A65),
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
    val creatorDefaults = remember { CreatorOsSettingsStore(context.applicationContext).snapshot() }
    val now = System.currentTimeMillis()
    val fallbackDue = now + 60 * 60_000L

    var title by rememberSaveable(task?.id) { mutableStateOf(task?.title.orEmpty()) }
    var platform by rememberSaveable(task?.id) { mutableStateOf(task?.platform ?: "Instagram") }
    var contentType by rememberSaveable(task?.id) { mutableStateOf(task?.contentType ?: "Reel") }
    var dueAt by rememberSaveable(task?.id) { mutableLongStateOf(task?.dueAtMillis?.takeIf { it > now } ?: task?.reminderAtMillis?.takeIf { it > now } ?: fallbackDue) }
    var mode by rememberSaveable(task?.id) { mutableStateOf(task?.reminderMode ?: ReminderMode.SMART) }
    var reminderAt by rememberSaveable(task?.id) { mutableLongStateOf(task?.reminderAtMillis?.takeIf { it > now } ?: dueAt) }
    var priority by rememberSaveable(task?.id) { mutableStateOf(task?.priority ?: TaskPriority.IMPORTANT) }
    var notes by rememberSaveable(task?.id) { mutableStateOf(task?.notes.orEmpty()) }
    var soundUri by rememberSaveable(task?.id) { mutableStateOf(task?.alarmSoundUri?.takeIf { it.isNotBlank() } ?: RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM).toString()) }
    var persona by rememberSaveable(task?.id) { mutableStateOf(task?.voicePersona ?: creatorDefaults.defaultVoicePersona) }
    var repeatCount by rememberSaveable(task?.id) { mutableIntStateOf(task?.voiceRepeatCount ?: 3) }
    var repeatGap by rememberSaveable(task?.id) { mutableIntStateOf(task?.voiceRepeatIntervalSeconds ?: 20) }
    var alarmTimeout by rememberSaveable(task?.id) { mutableIntStateOf(task?.alarmTimeoutSeconds ?: creatorDefaults.defaultAlarmTimeoutSeconds) }

    val formats = v07FormatsFor(platform)
    fun changePlatform(value: String) {
        platform = value
        contentType = v07FormatsFor(value).first()
    }

    fun pickDateTime(current: Long, onPicked: (Long) -> Unit) {
        val initial = Calendar.getInstance().apply { timeInMillis = current }
        DatePickerDialog(context, { _, y, m, d ->
            TimePickerDialog(context, { _, h, min ->
                onPicked(Calendar.getInstance().apply { set(y, m, d, h, min, 0); set(Calendar.MILLISECOND, 0) }.timeInMillis)
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
            putExtra(RingtoneManager.EXTRA_RINGTONE_TITLE, "Choose FrameByNavin alarm sound")
        })
    }

    val toneTitle = remember(soundUri) {
        runCatching { RingtoneManager.getRingtone(context, Uri.parse(soundUri))?.getTitle(context) }.getOrNull() ?: "Default alarm"
    }
    val template = remember(platform, contentType) { CreatorWorkflowEngine.templateFor(platform, contentType) }
    val needsReminder = mode != ReminderMode.NONE
    val needsExact = mode == ReminderMode.VOICE || mode == ReminderMode.ALARM || mode == ReminderMode.SMART
    val needsFullScreen = needsExact
    val permissionReady = !needsReminder || (notificationReady && (!needsExact || exactReady) && (!needsFullScreen || fullScreenReady))
    val timesReady = dueAt > now && (!needsReminder || reminderAt > now)

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = CinemaSurfaceRaised,
        title = {
            Column {
                Text(if (task == null) "CREATE PROJECT" else "EDIT PROJECT", color = RecRed, fontSize = 9.sp, letterSpacing = 1.1.sp, fontWeight = FontWeight.Bold)
                Text("Creator Setup", color = ProjectorIvory, fontWeight = FontWeight.Black)
            }
        },
        text = {
            Column(Modifier.fillMaxWidth().heightIn(max = 670.dp).verticalScroll(rememberScrollState())) {
                OutlinedTextField(title, { title = it }, label = { Text("Task / content title") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                Spacer(Modifier.height(13.dp)); V07TinyLabel("PLATFORM")
                FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    listOf("Instagram", "YouTube", "X").forEach { value -> FilterChip(platform == value, { changePlatform(value) }, { Text(value) }) }
                }
                Spacer(Modifier.height(11.dp)); V07TinyLabel("FORMAT")
                FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    formats.forEach { value -> FilterChip(contentType == value, { contentType = value }, { Text(value, fontSize = if (value.length > 12) 9.5.sp else 11.sp) }) }
                }

                Spacer(Modifier.height(13.dp))
                Surface(Modifier.fillMaxWidth(), RoundedCornerShape(14.dp), Color(0xFF10100F), border = androidx.compose.foundation.BorderStroke(1.dp, CinemaLine)) {
                    Column(Modifier.padding(12.dp)) {
                        Text("AUTO WORKFLOW · ${template.label.uppercase()}", color = MutedGold, fontSize = 8.5.sp, fontWeight = FontWeight.Bold)
                        Text(template.stages.joinToString(" → ") { it.label }, color = MutedText, fontSize = 8.8.sp, lineHeight = 13.sp)
                    }
                }

                Spacer(Modifier.height(14.dp)); V07TinyLabel("PUBLISH / DUE TIME")
                V07DateTimeButton(v07FormatDateTime(dueAt)) { pickDateTime(dueAt) { picked -> dueAt = picked; if (task == null) reminderAt = picked } }

                Spacer(Modifier.height(15.dp)); V07TinyLabel("REMINDER MODE")
                FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    ReminderMode.entries.forEach { value -> FilterChip(mode == value, { mode = value }, { Text(v07ModeLabel(value), fontSize = 10.sp) }) }
                }
                Text(v07ModeDescription(mode), color = MutedText, fontSize = 9.5.sp, lineHeight = 14.sp)

                if (needsReminder) {
                    Spacer(Modifier.height(13.dp))
                    V07PermissionCard(notificationReady, exactReady, fullScreenReady, needsExact, needsFullScreen, onRequestNotifications, onRequestExact, onRequestFullScreen)
                    Spacer(Modifier.height(13.dp)); V07TinyLabel(if (mode == ReminderMode.SMART) "SMART TARGET" else "REMIND AT")
                    V07DateTimeButton(v07FormatDateTime(reminderAt)) { pickDateTime(reminderAt) { reminderAt = it } }
                    Spacer(Modifier.height(5.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(5.dp)) {
                        listOf(1L to "1 MIN", 3L to "3 MIN", 10L to "10 MIN").forEach { (mins, label) -> AssistChip(onClick = { reminderAt = System.currentTimeMillis() + mins * 60_000L }, label = { Text(label, fontSize = 8.sp) }) }
                    }
                    Spacer(Modifier.height(12.dp)); V07TinyLabel("PRIORITY")
                    Row(horizontalArrangement = Arrangement.spacedBy(5.dp)) {
                        TaskPriority.entries.forEach { value -> FilterChip(priority == value, { priority = value }, { Text(value.name.lowercase().replaceFirstChar { it.uppercase() }, fontSize = 8.5.sp) }) }
                    }
                }

                if (mode == ReminderMode.VOICE || mode == ReminderMode.SMART) {
                    Spacer(Modifier.height(13.dp)); V07TinyLabel("VOICE PERSONA")
                    VoicePersona.entries.forEach { value ->
                        Surface(
                            modifier = Modifier.fillMaxWidth().padding(vertical = 3.dp).clickable { persona = value },
                            shape = RoundedCornerShape(13.dp),
                            color = if (persona == value) Color(0xFF1B1711) else CinemaSurface,
                            border = androidx.compose.foundation.BorderStroke(1.dp, if (persona == value) MutedGold.copy(.6f) else CinemaLine),
                        ) {
                            Row(Modifier.padding(10.dp), verticalAlignment = Alignment.CenterVertically) {
                                RadioButton(persona == value, { persona = value }, colors = RadioButtonDefaults.colors(selectedColor = MutedGold))
                                Column(Modifier.weight(1f)) {
                                    Text(VoicePersonaEngine.label(value), color = ProjectorIvory, fontSize = 10.5.sp, fontWeight = FontWeight.Bold)
                                    if (value == creatorDefaults.defaultVoicePersona) Text("Default", color = MutedGold, fontSize = 8.sp)
                                }
                                TextButton(onClick = { v07PreviewVoice(context, value) }) { Text("PREVIEW", color = RecRed, fontSize = 8.sp) }
                            }
                        }
                    }
                    Spacer(Modifier.height(8.dp)); V07TinyLabel("REPEATS")
                    Row(horizontalArrangement = Arrangement.spacedBy(5.dp)) { listOf(1, 2, 3).forEach { c -> FilterChip(repeatCount == c, { repeatCount = c }, { Text("$c×") }) } }
                    Spacer(Modifier.height(5.dp)); V07TinyLabel("REPEAT GAP")
                    Row(horizontalArrangement = Arrangement.spacedBy(5.dp)) { listOf(20, 30, 60).forEach { s -> FilterChip(repeatGap == s, { repeatGap = s }, { Text("${s}s", fontSize = 8.5.sp) }) } }
                }

                if (mode == ReminderMode.ALARM || mode == ReminderMode.SMART) {
                    Spacer(Modifier.height(13.dp)); V07TinyLabel("ALARM")
                    OutlinedButton(onClick = { chooseTone() }, modifier = Modifier.fillMaxWidth()) {
                        Icon(Icons.Outlined.MusicNote, null, modifier = Modifier.size(16.dp)); Spacer(Modifier.width(7.dp)); Text(toneTitle, color = ProjectorIvory, modifier = Modifier.weight(1f), maxLines = 1); Text("CHANGE", color = RecRed, fontSize = 8.sp)
                    }
                    Spacer(Modifier.height(7.dp)); V07TinyLabel("AUTO-STOP")
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(5.dp)) {
                        listOf(30 to "30 sec", 60 to "1 min", 120 to "2 min", 300 to "5 min").forEach { (seconds, label) -> FilterChip(alarmTimeout == seconds, { alarmTimeout = seconds }, { Text(label, fontSize = 8.5.sp) }) }
                    }
                }

                if (mode == ReminderMode.SMART) {
                    Spacer(Modifier.height(12.dp))
                    Surface(Modifier.fillMaxWidth(), RoundedCornerShape(14.dp), Color(0xFF15120F), border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF3A2B22))) {
                        Text("Smart owns the route: notification → Voice UI → native alarm → critical alarm, adjusted by priority and acknowledgement.", color = MutedText, fontSize = 9.3.sp, lineHeight = 14.sp, modifier = Modifier.padding(12.dp))
                    }
                }

                Spacer(Modifier.height(13.dp))
                OutlinedTextField(notes, { notes = it }, label = { Text("Notes (optional)") }, minLines = 2, maxLines = 4, modifier = Modifier.fillMaxWidth())
                if (task?.reminderEnabled == true) TextButton(onClick = onRemoveReminder) { Text("REMOVE REMINDER ONLY", color = MutedText, fontSize = 9.sp) }
            }
        },
        confirmButton = {
            Button(
                onClick = { onSave(V07ReminderDraft(title, platform, contentType, dueAt, mode, if (mode == ReminderMode.NONE) 0L else reminderAt, priority, notes, soundUri, persona, repeatCount, repeatGap, alarmTimeout)) },
                enabled = title.isNotBlank() && timesReady && permissionReady,
                colors = ButtonDefaults.buttonColors(containerColor = RecRed),
            ) { Text(if (task == null) "CREATE" else "SAVE") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("CANCEL", color = MutedText) } },
    )
}

@Composable
private fun V07PermissionCard(
    notificationReady: Boolean,
    exactReady: Boolean,
    fullScreenReady: Boolean,
    needsExact: Boolean,
    needsFullScreen: Boolean,
    onRequestNotifications: () -> Unit,
    onRequestExact: () -> Unit,
    onRequestFullScreen: () -> Unit,
) {
    val ready = notificationReady && (!needsExact || exactReady) && (!needsFullScreen || fullScreenReady)
    Surface(Modifier.fillMaxWidth(), RoundedCornerShape(14.dp), if (ready) Color(0xFF101511) else Color(0xFF15120F), border = androidx.compose.foundation.BorderStroke(1.dp, if (ready) SuccessGreen.copy(.25f) else Color(0xFF3A2B22))) {
        Column(Modifier.padding(11.dp)) {
            Text(if (ready) "ANDROID READY" else "ANDROID SETUP", color = if (ready) SuccessGreen else ProjectorIvory, fontSize = 8.5.sp, fontWeight = FontWeight.Bold)
            if (!notificationReady) TextButton(onClick = onRequestNotifications) { Text("ENABLE NOTIFICATIONS", color = RecRed, fontSize = 9.sp) }
            if (needsExact && !exactReady) TextButton(onClick = onRequestExact) { Text("ALLOW EXACT ALARMS", color = RecRed, fontSize = 9.sp) }
            if (needsFullScreen && !fullScreenReady) TextButton(onClick = onRequestFullScreen) { Text("ALLOW FULL-SCREEN ALERTS", color = RecRed, fontSize = 9.sp) }
        }
    }
}

@Composable
private fun V07TinyLabel(text: String) {
    Text(text, color = MutedText, fontSize = 8.3.sp, letterSpacing = 1.sp, fontWeight = FontWeight.SemiBold)
    Spacer(Modifier.height(4.dp))
}

@Composable
private fun V07DateTimeButton(text: String, onClick: () -> Unit) {
    OutlinedButton(onClick = onClick, modifier = Modifier.fillMaxWidth().height(52.dp), border = androidx.compose.foundation.BorderStroke(1.dp, CinemaLine), shape = RoundedCornerShape(13.dp)) {
        Icon(Icons.Outlined.Schedule, null, modifier = Modifier.size(16.dp)); Spacer(Modifier.width(7.dp)); Text(text, color = ProjectorIvory, modifier = Modifier.weight(1f), fontSize = 10.5.sp, fontWeight = FontWeight.Bold); Text("CHANGE", color = RecRed, fontSize = 8.sp)
    }
}

private fun v07PreviewVoice(context: Context, persona: VoicePersona) {
    var engine: TextToSpeech? = null
    engine = TextToSpeech(context.applicationContext) { status ->
        if (status == TextToSpeech.SUCCESS) {
            engine?.language = Locale.getDefault()
            engine?.let { VoicePersonaEngine.apply(it, persona) }
            engine?.speak("FrameByNavin. Your creator task is due soon.", TextToSpeech.QUEUE_FLUSH, null, "v07-${persona.name}")
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

private fun v07ModeDescription(mode: ReminderMode): String = when (mode) {
    ReminderMode.NONE -> "No alert. Keep it only in your creator workflow."
    ReminderMode.SIMPLE -> "One clean notification at the chosen time."
    ReminderMode.VOICE -> "Spoken reminder with the dedicated Voice screen."
    ReminderMode.ALARM -> "Native alarm with system tone and bounded ringing time."
    ReminderMode.SMART -> "FrameByNavin controls notification → voice → alarm stages automatically."
}

internal fun v07FormatDateTime(millis: Long): String = SimpleDateFormat("EEE, d MMM · h:mm a", Locale.getDefault()).format(Date(millis))

internal fun v07DueLabel(millis: Long): String {
    val now = Calendar.getInstance(); val selected = Calendar.getInstance().apply { timeInMillis = millis }; val tomorrow = Calendar.getInstance().apply { add(Calendar.DAY_OF_YEAR, 1) }; val time = SimpleDateFormat("h:mm a", Locale.getDefault()).format(Date(millis))
    fun same(a: Calendar, b: Calendar) = a.get(Calendar.YEAR) == b.get(Calendar.YEAR) && a.get(Calendar.DAY_OF_YEAR) == b.get(Calendar.DAY_OF_YEAR)
    return when { same(now, selected) -> "Today · $time"; same(tomorrow, selected) -> "Tomorrow · $time"; else -> SimpleDateFormat("EEE, d MMM · h:mm a", Locale.getDefault()).format(Date(millis)) }
}
