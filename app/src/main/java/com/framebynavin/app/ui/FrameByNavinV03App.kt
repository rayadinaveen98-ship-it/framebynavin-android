package com.framebynavin.app.ui

import android.Manifest
import android.app.DatePickerDialog
import android.app.TimePickerDialog
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.activity.ComponentActivity
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Alarm
import androidx.compose.material.icons.outlined.NotificationsActive
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
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.viewmodel.compose.viewModel
import com.framebynavin.app.data.CreatorTask
import com.framebynavin.app.data.CreatorViewModel
import com.framebynavin.app.data.TaskPriority
import com.framebynavin.app.reminders.ReminderScheduler
import com.framebynavin.app.ui.theme.*
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

@Composable
fun FrameByNavinV03App(vm: CreatorViewModel = viewModel()) {
    val context = LocalContext.current
    val activity = context as? ComponentActivity
    val scheduler = remember { ReminderScheduler(context.applicationContext) }
    var showReminderPanel by remember { mutableStateOf(false) }
    var notificationReady by remember { mutableStateOf(hasNotificationPermissionV03(context)) }
    var exactAlarmReady by remember { mutableStateOf(scheduler.canScheduleExact()) }

    val notificationPermissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted -> notificationReady = granted }

    DisposableEffect(activity) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                notificationReady = hasNotificationPermissionV03(context)
                exactAlarmReady = scheduler.canScheduleExact()
            }
        }
        activity?.lifecycle?.addObserver(observer)
        onDispose { activity?.lifecycle?.removeObserver(observer) }
    }

    Box(Modifier.fillMaxSize()) {
        FrameByNavinApp(vm)

        Surface(
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .navigationBarsPadding()
                .padding(end = 22.dp, bottom = 96.dp)
                .size(52.dp)
                .clickable { showReminderPanel = true },
            shape = CircleShape,
            color = RecRed,
            shadowElevation = 10.dp,
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(Icons.Outlined.Alarm, contentDescription = "Reminder center", tint = ProjectorIvory)
            }
        }
    }

    if (showReminderPanel) {
        ReminderCenterDialog(
            tasks = vm.tasks,
            notificationReady = notificationReady,
            exactAlarmReady = exactAlarmReady,
            onDismiss = { showReminderPanel = false },
            onRequestNotifications = {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                } else {
                    notificationReady = true
                }
            },
            onRequestExactAlarm = {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                    context.startActivity(
                        Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM).apply {
                            data = Uri.parse("package:${context.packageName}")
                        }
                    )
                } else {
                    exactAlarmReady = true
                }
            },
            onSaveReminder = { taskId, atMillis, priority, notes ->
                vm.setReminder(taskId, atMillis, priority, notes)
                showReminderPanel = false
            },
            onCancelReminder = { taskId ->
                vm.cancelReminder(taskId)
                showReminderPanel = false
            },
        )
    }
}

@Composable
private fun ReminderCenterDialog(
    tasks: List<CreatorTask>,
    notificationReady: Boolean,
    exactAlarmReady: Boolean,
    onDismiss: () -> Unit,
    onRequestNotifications: () -> Unit,
    onRequestExactAlarm: () -> Unit,
    onSaveReminder: (String, Long, TaskPriority, String) -> Unit,
    onCancelReminder: (String) -> Unit,
) {
    val context = LocalContext.current
    var selectedTaskId by remember(tasks) {
        mutableStateOf(tasks.firstOrNull { it.status.name != "DONE" }?.id ?: tasks.firstOrNull()?.id)
    }
    val selectedTask = tasks.firstOrNull { it.id == selectedTaskId }
    var reminderAtMillis by remember(selectedTaskId) {
        mutableLongStateOf(
            selectedTask?.reminderAtMillis?.takeIf { it > System.currentTimeMillis() }
                ?: (System.currentTimeMillis() + 5 * 60_000L)
        )
    }
    var priority by remember(selectedTaskId) { mutableStateOf(selectedTask?.priority ?: TaskPriority.IMPORTANT) }
    var notes by remember(selectedTaskId) { mutableStateOf(selectedTask?.notes.orEmpty()) }

    fun openCustomPicker() {
        val initial = Calendar.getInstance().apply { timeInMillis = reminderAtMillis }
        DatePickerDialog(
            context,
            { _, year, month, day ->
                TimePickerDialog(
                    context,
                    { _, hour, minute ->
                        reminderAtMillis = Calendar.getInstance().apply {
                            set(Calendar.YEAR, year)
                            set(Calendar.MONTH, month)
                            set(Calendar.DAY_OF_MONTH, day)
                            set(Calendar.HOUR_OF_DAY, hour)
                            set(Calendar.MINUTE, minute)
                            set(Calendar.SECOND, 0)
                            set(Calendar.MILLISECOND, 0)
                        }.timeInMillis
                    },
                    initial.get(Calendar.HOUR_OF_DAY),
                    initial.get(Calendar.MINUTE),
                    false,
                ).show()
            },
            initial.get(Calendar.YEAR),
            initial.get(Calendar.MONTH),
            initial.get(Calendar.DAY_OF_MONTH),
        ).show()
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = CinemaSurfaceRaised,
        titleContentColor = ProjectorIvory,
        textContentColor = MutedText,
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Outlined.Alarm, contentDescription = null, tint = RecRed)
                Spacer(Modifier.width(8.dp))
                Text("Reminder Core", fontWeight = FontWeight.Black)
            }
        },
        text = {
            Column(
                Modifier
                    .fillMaxWidth()
                    .heightIn(max = 620.dp)
                    .verticalScroll(rememberScrollState())
            ) {
                if (!notificationReady || !exactAlarmReady) {
                    PermissionSection(
                        notificationReady = notificationReady,
                        exactAlarmReady = exactAlarmReady,
                        onRequestNotifications = onRequestNotifications,
                        onRequestExactAlarm = onRequestExactAlarm,
                    )
                    Spacer(Modifier.height(16.dp))
                }

                Text("TASK", color = MutedText, fontSize = 9.sp, letterSpacing = 1.sp)
                Spacer(Modifier.height(6.dp))
                if (tasks.isEmpty()) {
                    Text("Create a task first using the + button.", color = MutedText, fontSize = 11.sp)
                } else {
                    tasks.forEach { task ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { selectedTaskId = task.id }
                                .padding(vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            RadioButton(
                                selected = selectedTaskId == task.id,
                                onClick = { selectedTaskId = task.id },
                                colors = RadioButtonDefaults.colors(selectedColor = RecRed),
                            )
                            Column(Modifier.weight(1f)) {
                                Text(task.title, color = ProjectorIvory, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                Text("${task.platform} · ${task.contentType} · ${task.dueLabel}", color = MutedText, fontSize = 9.5.sp)
                                if (task.reminderEnabled && task.reminderAtMillis > 0L) {
                                    Text(
                                        "Active reminder · ${formatReminderV03(task.reminderAtMillis)}",
                                        color = MutedGold,
                                        fontSize = 9.5.sp,
                                    )
                                }
                            }
                        }
                    }
                }

                if (selectedTask != null) {
                    Spacer(Modifier.height(14.dp))
                    Text("REMIND AT", color = MutedText, fontSize = 9.sp, letterSpacing = 1.sp)
                    Text(
                        formatReminderV03(reminderAtMillis),
                        color = ProjectorIvory,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold,
                    )
                    Spacer(Modifier.height(8.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        listOf(1L to "1 MIN", 5L to "5 MIN", 15L to "15 MIN").forEach { (minutes, label) ->
                            AssistChip(
                                onClick = { reminderAtMillis = System.currentTimeMillis() + minutes * 60_000L },
                                label = { Text(label, fontSize = 9.sp) },
                            )
                        }
                    }
                    Spacer(Modifier.height(6.dp))
                    OutlinedButton(
                        onClick = { openCustomPicker() },
                        modifier = Modifier.fillMaxWidth(),
                        border = androidx.compose.foundation.BorderStroke(1.dp, CinemaLine),
                        shape = RoundedCornerShape(13.dp),
                    ) {
                        Icon(Icons.Outlined.Schedule, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(6.dp))
                        Text("CHOOSE EXACT DATE & TIME", color = ProjectorIvory, fontSize = 9.5.sp)
                    }

                    Spacer(Modifier.height(14.dp))
                    Text("PRIORITY", color = MutedText, fontSize = 9.sp, letterSpacing = 1.sp)
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        TaskPriority.entries.forEach { option ->
                            FilterChip(
                                selected = priority == option,
                                onClick = { priority = option },
                                label = {
                                    Text(
                                        option.name.lowercase().replaceFirstChar { it.uppercase() },
                                        fontSize = 9.sp,
                                    )
                                },
                            )
                        }
                    }

                    Spacer(Modifier.height(10.dp))
                    OutlinedTextField(
                        value = notes,
                        onValueChange = { notes = it },
                        label = { Text("Reminder notes (optional)") },
                        minLines = 2,
                        maxLines = 4,
                        modifier = Modifier.fillMaxWidth(),
                    )

                    if (selectedTask.reminderEnabled) {
                        Spacer(Modifier.height(8.dp))
                        TextButton(onClick = { onCancelReminder(selectedTask.id) }) {
                            Text("CANCEL CURRENT REMINDER", color = MutedText, fontSize = 9.5.sp)
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    selectedTaskId?.let { onSaveReminder(it, reminderAtMillis, priority, notes) }
                },
                enabled = selectedTaskId != null &&
                    reminderAtMillis > System.currentTimeMillis() &&
                    notificationReady && exactAlarmReady,
                colors = ButtonDefaults.buttonColors(containerColor = RecRed),
            ) {
                Text("SET REMINDER")
            }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("CLOSE", color = MutedText) } },
    )
}

@Composable
private fun PermissionSection(
    notificationReady: Boolean,
    exactAlarmReady: Boolean,
    onRequestNotifications: () -> Unit,
    onRequestExactAlarm: () -> Unit,
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        color = Color(0xFF15120F),
        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF3A2B22)),
    ) {
        Column(Modifier.padding(14.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Outlined.NotificationsActive, contentDescription = null, tint = MutedGold)
                Spacer(Modifier.width(8.dp))
                Text("ANDROID SETUP", color = ProjectorIvory, fontSize = 11.sp, fontWeight = FontWeight.Bold)
            }
            Spacer(Modifier.height(8.dp))
            Text(
                "Both permissions are required for the V0.3 exact reminder test.",
                color = MutedText,
                fontSize = 10.sp,
            )
            Spacer(Modifier.height(10.dp))
            if (!notificationReady) {
                OutlinedButton(
                    onClick = onRequestNotifications,
                    modifier = Modifier.fillMaxWidth(),
                    border = androidx.compose.foundation.BorderStroke(1.dp, CinemaLine),
                    shape = RoundedCornerShape(12.dp),
                ) { Text("ENABLE NOTIFICATIONS", color = ProjectorIvory, fontSize = 9.5.sp) }
                Spacer(Modifier.height(7.dp))
            }
            if (!exactAlarmReady) {
                Button(
                    onClick = onRequestExactAlarm,
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(containerColor = RecRed),
                    shape = RoundedCornerShape(12.dp),
                ) { Text("ALLOW EXACT ALARMS", fontSize = 9.5.sp, fontWeight = FontWeight.Bold) }
            }
        }
    }
}

private fun hasNotificationPermissionV03(context: android.content.Context): Boolean =
    Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU ||
        context.checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED

private fun formatReminderV03(millis: Long): String =
    SimpleDateFormat("EEE, d MMM · h:mm a", Locale.getDefault()).format(Date(millis))
