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
import android.provider.Settings
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Alarm
import androidx.compose.material.icons.outlined.MusicNote
import androidx.compose.material.icons.outlined.RecordVoiceOver
import androidx.compose.material3.*
import androidx.compose.runtime.*
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
import com.framebynavin.app.data.ReminderAlertType
import com.framebynavin.app.data.TaskPriority
import com.framebynavin.app.reminders.ReminderScheduler
import com.framebynavin.app.ui.theme.*
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

@Composable
fun FrameByNavinV05App(vm: CreatorViewModel = viewModel()) {
    val context = LocalContext.current
    val activity = context as? ComponentActivity
    val scheduler = remember { ReminderScheduler(context.applicationContext) }
    val notificationManager = remember { context.getSystemService(NotificationManager::class.java) }

    var showAlarmCenter by remember { mutableStateOf(false) }
    var notificationReady by remember { mutableStateOf(hasNotificationPermissionV05(context)) }
    var exactAlarmReady by remember { mutableStateOf(scheduler.canScheduleExact()) }
    var fullScreenReady by remember {
        mutableStateOf(Build.VERSION.SDK_INT < 34 || notificationManager.canUseFullScreenIntent())
    }

    val notificationPermissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted -> notificationReady = granted }

    DisposableEffect(activity) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                notificationReady = hasNotificationPermissionV05(context)
                exactAlarmReady = scheduler.canScheduleExact()
                fullScreenReady = Build.VERSION.SDK_INT < 34 || notificationManager.canUseFullScreenIntent()
                vm.reconcileReminders()
            }
        }
        activity?.lifecycle?.addObserver(observer)
        onDispose { activity?.lifecycle?.removeObserver(observer) }
    }

    Box(Modifier.fillMaxSize()) {
        FrameByNavinV04App(vm)

        // Intercepts the existing V0 reminder button with the V0.5 alarm center.
        Box(
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .navigationBarsPadding()
                .padding(end = 14.dp, bottom = 88.dp)
                .size(68.dp)
                .clickable { showAlarmCenter = true }
        )
    }

    if (showAlarmCenter) {
        NativeAlarmCenterDialog(
            tasks = vm.tasks,
            notificationReady = notificationReady,
            exactAlarmReady = exactAlarmReady,
            fullScreenReady = fullScreenReady,
            onDismiss = { showAlarmCenter = false },
            onRequestNotifications = {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                } else notificationReady = true
            },
            onRequestExactAlarm = {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                    context.startActivity(
                        Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM).apply {
                            data = Uri.parse("package:${context.packageName}")
                        }
                    )
                } else exactAlarmReady = true
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
            onSave = { taskId, atMillis, priority, notes, alertType, soundUri, voiceEnabled ->
                vm.setReminder(
                    id = taskId,
                    reminderAtMillis = atMillis,
                    priority = priority,
                    notes = notes,
                    alertType = alertType,
                    alarmSoundUri = soundUri,
                    voiceEnabled = voiceEnabled,
                )
                showAlarmCenter = false
            },
            onCancel = { taskId ->
                vm.cancelReminder(taskId)
                showAlarmCenter = false
            },
        )
    }
}

@Composable
private fun NativeAlarmCenterDialog(
    tasks: List<CreatorTask>,
    notificationReady: Boolean,
    exactAlarmReady: Boolean,
    fullScreenReady: Boolean,
    onDismiss: () -> Unit,
    onRequestNotifications: () -> Unit,
    onRequestExactAlarm: () -> Unit,
    onRequestFullScreen: () -> Unit,
    onSave: (String, Long, TaskPriority, String, ReminderAlertType, String, Boolean) -> Unit,
    onCancel: (String) -> Unit,
) {
    val context = LocalContext.current
    var selectedTaskId by remember(tasks) {
        mutableStateOf(tasks.firstOrNull { it.status.name != "DONE" }?.id ?: tasks.firstOrNull()?.id)
    }
    val selectedTask = tasks.firstOrNull { it.id == selectedTaskId }

    var reminderAtMillis by remember(selectedTaskId) {
        mutableLongStateOf(selectedTask?.reminderAtMillis?.takeIf { it > System.currentTimeMillis() }
            ?: System.currentTimeMillis() + 5 * 60_000L)
    }
    var priority by remember(selectedTaskId) { mutableStateOf(selectedTask?.priority ?: TaskPriority.IMPORTANT) }
    var notes by remember(selectedTaskId) { mutableStateOf(selectedTask?.notes.orEmpty()) }
    var alertType by remember(selectedTaskId) { mutableStateOf(selectedTask?.alertType ?: ReminderAlertType.ALARM) }
    var voiceEnabled by remember(selectedTaskId) { mutableStateOf(selectedTask?.voiceEnabled ?: false) }
    var soundUri by remember(selectedTaskId) {
        mutableStateOf(
            selectedTask?.alarmSoundUri?.takeIf { it.isNotBlank() }
                ?: RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM).toString()
        )
    }

    LaunchedEffect(selectedTaskId, selectedTask?.reminderAtMillis) {
        selectedTask?.let { task ->
            reminderAtMillis = task.reminderAtMillis.takeIf { it > System.currentTimeMillis() }
                ?: System.currentTimeMillis() + 5 * 60_000L
            priority = task.priority
            notes = task.notes
            alertType = task.alertType
            voiceEnabled = task.voiceEnabled
            soundUri = task.alarmSoundUri.takeIf { it.isNotBlank() }
                ?: RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM).toString()
        }
    }

    val ringtoneLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        @Suppress("DEPRECATION")
        val picked = result.data?.getParcelableExtra<Uri>(RingtoneManager.EXTRA_RINGTONE_PICKED_URI)
        if (picked != null) soundUri = picked.toString()
    }

    fun chooseTone() {
        val existing = runCatching { Uri.parse(soundUri) }.getOrNull()
        ringtoneLauncher.launch(
            Intent(RingtoneManager.ACTION_RINGTONE_PICKER).apply {
                putExtra(RingtoneManager.EXTRA_RINGTONE_TYPE, RingtoneManager.TYPE_ALARM)
                putExtra(RingtoneManager.EXTRA_RINGTONE_SHOW_DEFAULT, true)
                putExtra(RingtoneManager.EXTRA_RINGTONE_SHOW_SILENT, false)
                putExtra(RingtoneManager.EXTRA_RINGTONE_EXISTING_URI, existing)
                putExtra(RingtoneManager.EXTRA_RINGTONE_TITLE, "Choose FrameByNavin alarm sound")
            }
        )
    }

    fun chooseDateTime() {
        val initial = Calendar.getInstance().apply { timeInMillis = reminderAtMillis }
        DatePickerDialog(
            context,
            { _, year, month, day ->
                TimePickerDialog(
                    context,
                    { _, hour, minute ->
                        reminderAtMillis = Calendar.getInstance().apply {
                            set(year, month, day, hour, minute, 0)
                            set(Calendar.MILLISECOND, 0)
                        }.timeInMillis
                    },
                    initial.get(Calendar.HOUR_OF_DAY),
                    initial.get(Calendar.MINUTE),
                    false,
                ).show()
            },
            initial.get(Calendar.YEAR), initial.get(Calendar.MONTH), initial.get(Calendar.DAY_OF_MONTH)
        ).show()
    }

    val toneTitle = remember(soundUri) {
        runCatching {
            RingtoneManager.getRingtone(context, Uri.parse(soundUri))?.getTitle(context)
        }.getOrNull() ?: "Default alarm"
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = CinemaSurfaceRaised,
        titleContentColor = ProjectorIvory,
        textContentColor = MutedText,
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Outlined.Alarm, null, tint = RecRed)
                Spacer(Modifier.width(8.dp))
                Text("Native Alarm", fontWeight = FontWeight.Black)
            }
        },
        text = {
            Column(
                Modifier.fillMaxWidth().heightIn(max = 640.dp).verticalScroll(rememberScrollState())
            ) {
                if (!notificationReady || !exactAlarmReady || (alertType == ReminderAlertType.ALARM && !fullScreenReady)) {
                    Surface(
                        Modifier.fillMaxWidth(), RoundedCornerShape(16.dp), Color(0xFF15120F),
                        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF3A2B22))
                    ) {
                        Column(Modifier.padding(14.dp)) {
                            Text("ANDROID ALARM SETUP", color = ProjectorIvory, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                            Spacer(Modifier.height(8.dp))
                            if (!notificationReady) {
                                OutlinedButton(onClick = onRequestNotifications, modifier = Modifier.fillMaxWidth()) { Text("ENABLE NOTIFICATIONS") }
                                Spacer(Modifier.height(6.dp))
                            }
                            if (!exactAlarmReady) {
                                Button(onClick = onRequestExactAlarm, modifier = Modifier.fillMaxWidth(), colors = ButtonDefaults.buttonColors(containerColor = RecRed)) { Text("ALLOW EXACT ALARMS") }
                                Spacer(Modifier.height(6.dp))
                            }
                            if (alertType == ReminderAlertType.ALARM && !fullScreenReady) {
                                OutlinedButton(onClick = onRequestFullScreen, modifier = Modifier.fillMaxWidth()) { Text("ALLOW FULL-SCREEN ALARMS") }
                            }
                        }
                    }
                    Spacer(Modifier.height(14.dp))
                }

                Text("TASK", color = MutedText, fontSize = 9.sp, letterSpacing = 1.sp)
                tasks.forEach { task ->
                    Row(
                        Modifier.fillMaxWidth().clickable { selectedTaskId = task.id }.padding(vertical = 5.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        RadioButton(selectedTaskId == task.id, { selectedTaskId = task.id }, colors = RadioButtonDefaults.colors(selectedColor = RecRed))
                        Column(Modifier.weight(1f)) {
                            Text(task.title, color = ProjectorIvory, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                            Text("${task.platform} · ${task.contentType} · ${task.dueLabel}", color = MutedText, fontSize = 9.5.sp)
                        }
                    }
                }

                if (selectedTask != null) {
                    Spacer(Modifier.height(12.dp))
                    Text("ALERT TYPE", color = MutedText, fontSize = 9.sp, letterSpacing = 1.sp)
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        FilterChip(alertType == ReminderAlertType.NOTIFICATION, { alertType = ReminderAlertType.NOTIFICATION }, { Text("Notification") })
                        FilterChip(alertType == ReminderAlertType.ALARM, { alertType = ReminderAlertType.ALARM }, { Text("Alarm") })
                    }

                    Spacer(Modifier.height(12.dp))
                    Text("REMIND AT", color = MutedText, fontSize = 9.sp, letterSpacing = 1.sp)
                    Text(formatAlarmTime(reminderAtMillis), color = ProjectorIvory, fontSize = 15.sp, fontWeight = FontWeight.Bold)
                    Spacer(Modifier.height(7.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        listOf(1L to "1 MIN", 5L to "5 MIN", 15L to "15 MIN").forEach { (minutes, label) ->
                            AssistChip(onClick = { reminderAtMillis = System.currentTimeMillis() + minutes * 60_000L }, label = { Text(label, fontSize = 9.sp) })
                        }
                    }
                    OutlinedButton(onClick = { chooseDateTime() }, modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(13.dp)) {
                        Text("CHOOSE EXACT DATE & TIME", color = ProjectorIvory, fontSize = 9.5.sp)
                    }

                    if (alertType == ReminderAlertType.ALARM) {
                        Spacer(Modifier.height(12.dp))
                        Text("ALARM SOUND", color = MutedText, fontSize = 9.sp, letterSpacing = 1.sp)
                        OutlinedButton(onClick = { chooseTone() }, modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(13.dp)) {
                            Icon(Icons.Outlined.MusicNote, null, modifier = Modifier.size(16.dp))
                            Spacer(Modifier.width(8.dp))
                            Text(toneTitle, color = ProjectorIvory, maxLines = 1, modifier = Modifier.weight(1f))
                            Text("CHANGE", color = RecRed, fontSize = 9.sp)
                        }

                        Spacer(Modifier.height(8.dp))
                        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Outlined.RecordVoiceOver, null, tint = MutedGold, modifier = Modifier.size(20.dp))
                            Spacer(Modifier.width(9.dp))
                            Column(Modifier.weight(1f)) {
                                Text("VOICE REMINDER", color = ProjectorIvory, fontSize = 10.5.sp, fontWeight = FontWeight.Bold)
                                Text("Speak the task while the alarm rings.", color = MutedText, fontSize = 9.5.sp)
                            }
                            Switch(voiceEnabled, { voiceEnabled = it }, colors = SwitchDefaults.colors(checkedTrackColor = RecRed))
                        }
                    }

                    Spacer(Modifier.height(12.dp))
                    Text("PRIORITY", color = MutedText, fontSize = 9.sp, letterSpacing = 1.sp)
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        TaskPriority.entries.forEach { option ->
                            FilterChip(priority == option, { priority = option }, { Text(option.name.lowercase().replaceFirstChar { it.uppercase() }, fontSize = 9.sp) })
                        }
                    }

                    Spacer(Modifier.height(8.dp))
                    OutlinedTextField(notes, { notes = it }, label = { Text("Notes (optional)") }, minLines = 2, maxLines = 4, modifier = Modifier.fillMaxWidth())

                    if (selectedTask.reminderEnabled) {
                        TextButton(onClick = { onCancel(selectedTask.id) }) { Text("CANCEL CURRENT ALERT", color = MutedText) }
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = { selectedTaskId?.let { onSave(it, reminderAtMillis, priority, notes, alertType, soundUri, voiceEnabled) } },
                enabled = selectedTaskId != null && reminderAtMillis > System.currentTimeMillis() && notificationReady && exactAlarmReady,
                colors = ButtonDefaults.buttonColors(containerColor = RecRed)
            ) { Text(if (alertType == ReminderAlertType.ALARM) "SET ALARM" else "SET REMINDER") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("CLOSE", color = MutedText) } }
    )
}

private fun hasNotificationPermissionV05(context: android.content.Context): Boolean =
    Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU ||
        context.checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED

private fun formatAlarmTime(millis: Long): String =
    SimpleDateFormat("EEE, d MMM · h:mm a", Locale.getDefault()).format(Date(millis))
