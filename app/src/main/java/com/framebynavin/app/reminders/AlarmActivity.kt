package com.framebynavin.app.reminders

import android.app.DatePickerDialog
import android.app.NotificationManager
import android.app.TimePickerDialog
import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Alarm
import androidx.compose.material.icons.outlined.Check
import androidx.compose.material.icons.outlined.PauseCircle
import androidx.compose.material.icons.outlined.Schedule
import androidx.compose.material.icons.outlined.Snooze
import androidx.compose.material.icons.outlined.Work
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.lifecycleScope
import com.framebynavin.app.data.CreatorOsSettingsStore
import com.framebynavin.app.data.CreatorWorkflowEngine
import com.framebynavin.app.data.ProjectPulseEngine
import com.framebynavin.app.data.TaskStore
import com.framebynavin.app.ui.theme.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

class AlarmActivity : ComponentActivity() {
    private val store by lazy { TaskStore(applicationContext) }
    private var occurrenceId = ""
    private var taskId = ""

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O_MR1) {
            setShowWhenLocked(true)
            setTurnScreenOn(true)
        } else {
            @Suppress("DEPRECATION")
            window.addFlags(
                android.view.WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED or
                    android.view.WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON
            )
        }
        window.addFlags(
            android.view.WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON or
                android.view.WindowManager.LayoutParams.FLAG_ALLOW_LOCK_WHILE_SCREEN_ON
        )

        val task = intent.toTask() ?: run { finish(); return }
        taskId = task.id
        occurrenceId = intent.getStringExtra(ReminderConstants.EXTRA_OCCURRENCE_ID).orEmpty()
        if (!ReminderOccurrenceStore(applicationContext).matches(task, occurrenceId)) {
            finish()
            return
        }
        ReminderSurfaceRegistry.attachAlarm(this, taskId, occurrenceId)
        val snoozeMinutes = CreatorOsSettingsStore(applicationContext).snapshot().snoozeMinutes

        lifecycleScope.launch {
            delay(task.alarmTimeoutSeconds.coerceIn(30, 300) * 1000L + 750L)
            if (!isFinishing && !isDestroyed) {
                AlarmRingingService.stop(applicationContext, taskId, occurrenceId)
                if (!ReminderOccurrenceStore(applicationContext).matches(task, occurrenceId)) {
                    finishAndRemoveTask()
                    return@launch
                }
                getSystemService(NotificationManager::class.java).cancel(AlarmRingingService.notificationId(task.id))
                finishAndRemoveTask()
            }
        }

        setContent {
            FrameByNavinTheme {
                NativeAlarmScreen(
                    title = task.title,
                    dueLabel = task.dueLabel,
                    stageLabel = CreatorWorkflowEngine.currentStage(task).label,
                    notes = task.notes,
                    snoozeMinutes = snoozeMinutes,
                    stageCheckIn = ProjectPulseEngine.isStageCheckIn(task),
                    onStageDone = { dispatchReminderAction(ReminderConstants.ACTION_STAGE_DONE, task.id) },
                    onDismiss = { dispatchReminderAction(ReminderConstants.ACTION_DISMISS, task.id) },
                    onWorking = { dispatchReminderAction(ReminderConstants.ACTION_STARTED, task.id) },
                    onSnooze = { dispatchReminderAction(ReminderConstants.ACTION_SNOOZE, task.id) },
                    onPause = { dispatchReminderAction(ReminderConstants.ACTION_PAUSE, task.id) },
                    onReschedule = { openReschedulePicker(task.id) },
                )
            }
        }
    }

    override fun onResume() {
        super.onResume()
        if (taskId.isNotBlank() && occurrenceId.isNotBlank()) {
            lifecycleScope.launch(Dispatchers.IO) {
                val current = runCatching { store.load().firstOrNull { it.id == taskId } }.getOrNull()
                if (current == null || !ReminderOccurrenceStore(applicationContext).matches(current, occurrenceId)) {
                    withContext(Dispatchers.Main) { if (!isFinishing) finishAndRemoveTask() }
                }
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        if (intent.getStringExtra(ReminderConstants.EXTRA_OCCURRENCE_ID) != occurrenceId) finishAndRemoveTask()
    }

    override fun onDestroy() {
        ReminderSurfaceRegistry.detachAlarm(this)
        super.onDestroy()
    }

    private fun dispatchReminderAction(action: String, taskId: String) {
        sendBroadcast(Intent(this, ReminderActionReceiver::class.java).apply {
            this.action = action
            putExtra(ReminderConstants.EXTRA_TASK_ID, taskId)
            putExtra(ReminderConstants.EXTRA_OCCURRENCE_ID, occurrenceId)
        })
        lifecycleScope.launch { finishAlarm() }
    }

    private fun openReschedulePicker(taskId: String) {
        val initial = Calendar.getInstance().apply { add(Calendar.MINUTE, 15) }
        DatePickerDialog(
            this,
            { _, year, month, day ->
                TimePickerDialog(
                    this,
                    { _, hour, minute ->
                        val atMillis = Calendar.getInstance().apply {
                            set(year, month, day, hour, minute, 0)
                            set(Calendar.MILLISECOND, 0)
                        }.timeInMillis
                        if (atMillis > System.currentTimeMillis()) reschedule(taskId, atMillis)
                    },
                    initial.get(Calendar.HOUR_OF_DAY), initial.get(Calendar.MINUTE), false,
                ).show()
            },
            initial.get(Calendar.YEAR), initial.get(Calendar.MONTH), initial.get(Calendar.DAY_OF_MONTH)
        ).show()
    }

    private fun reschedule(taskId: String, atMillis: Long) {
        sendBroadcast(Intent(this, ReminderActionReceiver::class.java).apply {
            action = ReminderConstants.ACTION_RESCHEDULE
            putExtra(ReminderConstants.EXTRA_TASK_ID, taskId)
            putExtra(ReminderConstants.EXTRA_OCCURRENCE_ID, occurrenceId)
            putExtra(ReminderConstants.EXTRA_RESCHEDULE_AT, atMillis)
        })
        lifecycleScope.launch { finishAlarm() }
    }

    private suspend fun finishAlarm() {
        AlarmRingingService.stop(applicationContext, taskId, occurrenceId)
        withContext(Dispatchers.Main) { if (!isFinishing) finishAndRemoveTask() }
    }
}

@Composable
private fun NativeAlarmScreen(
    title: String,
    dueLabel: String,
    stageLabel: String,
    notes: String,
    snoozeMinutes: Int,
    stageCheckIn: Boolean,
    onStageDone: () -> Unit,
    onDismiss: () -> Unit,
    onWorking: () -> Unit,
    onSnooze: () -> Unit,
    onPause: () -> Unit,
    onReschedule: () -> Unit,
) {
    BackHandler(enabled = true) { }

    Box(
        Modifier.fillMaxSize()
            .background(Brush.radialGradient(listOf(RecRed.copy(alpha = .13f), CinemaBlack), radius = 900f))
            .statusBarsPadding().navigationBarsPadding().padding(24.dp)
    ) {
        Text(SimpleDateFormat("h:mm a", Locale.getDefault()).format(Date()), color = MutedText, fontSize = 11.sp, modifier = Modifier.align(Alignment.TopCenter))

        Column(Modifier.align(Alignment.Center), horizontalAlignment = Alignment.CenterHorizontally) {
            Box(Modifier.size(92.dp).background(RecRed.copy(alpha = 0.13f), CircleShape), contentAlignment = Alignment.Center) {
                Box(Modifier.size(62.dp).background(RecRed.copy(alpha = .12f), CircleShape), contentAlignment = Alignment.Center) {
                    Icon(Icons.Outlined.Alarm, null, tint = RecRed, modifier = Modifier.size(34.dp))
                }
            }
            Spacer(Modifier.height(25.dp))
            Text(if (stageCheckIn) "STAGE CHECK-IN" else "REMINDER", color = RecRed, fontSize = 9.sp, fontWeight = FontWeight.Black, letterSpacing = 1.4.sp)
            Spacer(Modifier.height(4.dp))
            Text(title, color = ProjectorIvory, fontSize = 33.sp, lineHeight = 37.sp, fontWeight = FontWeight.Black, textAlign = TextAlign.Center)
            if (stageCheckIn) {
                Spacer(Modifier.height(8.dp))
                Text("$stageLabel · How is this stage going?", color = MutedGold, fontSize = 13.sp, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center)
            }
            Spacer(Modifier.height(13.dp))
            Surface(shape = RoundedCornerShape(100.dp), color = Color(0xFF1A1110), border = BorderStroke(1.dp, RecRed.copy(alpha = .28f))) {
                Text(dueLabel, color = RecRed, fontSize = 10.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp))
            }
            if (notes.isNotBlank()) {
                Spacer(Modifier.height(18.dp))
                Surface(Modifier.fillMaxWidth(), RoundedCornerShape(17.dp), CinemaSurface, border = BorderStroke(1.dp, CinemaLine)) {
                    Text(notes, color = MutedText, fontSize = 11.sp, lineHeight = 17.sp, textAlign = TextAlign.Center, modifier = Modifier.padding(13.dp))
                }
            }
            Spacer(Modifier.height(25.dp))

            if (stageCheckIn) {
                Button(onClick = onStageDone, modifier = Modifier.fillMaxWidth().height(56.dp), colors = ButtonDefaults.buttonColors(containerColor = SuccessGreen), shape = RoundedCornerShape(17.dp)) {
                    Icon(Icons.Outlined.Check, null); Spacer(Modifier.width(8.dp)); Text("STAGE DONE", fontWeight = FontWeight.Black)
                }
                Spacer(Modifier.height(9.dp))
            }
            Button(onClick = onWorking, modifier = Modifier.fillMaxWidth().height(54.dp), colors = ButtonDefaults.buttonColors(containerColor = RecRed), shape = RoundedCornerShape(17.dp)) {
                Icon(Icons.Outlined.Work, null); Spacer(Modifier.width(8.dp)); Text("I'M WORKING ON IT", fontWeight = FontWeight.Black)
            }
            Spacer(Modifier.height(9.dp))
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedButton(onClick = onSnooze, modifier = Modifier.weight(1f).height(50.dp), border = BorderStroke(1.dp, CinemaLine), shape = RoundedCornerShape(15.dp)) {
                    Icon(Icons.Outlined.Snooze, null, tint = ProjectorIvory); Spacer(Modifier.width(4.dp)); Text("${snoozeMinutes}m", color = ProjectorIvory, fontSize = 9.sp)
                }
                if (stageCheckIn) {
                    OutlinedButton(onClick = onPause, modifier = Modifier.weight(1f).height(50.dp), border = BorderStroke(1.dp, CinemaLine), shape = RoundedCornerShape(15.dp)) {
                        Icon(Icons.Outlined.PauseCircle, null, tint = ProjectorIvory); Spacer(Modifier.width(4.dp)); Text("PAUSE 2H", color = ProjectorIvory, fontSize = 9.sp)
                    }
                } else {
                    OutlinedButton(onClick = onReschedule, modifier = Modifier.weight(1f).height(50.dp), border = BorderStroke(1.dp, CinemaLine), shape = RoundedCornerShape(15.dp)) {
                        Icon(Icons.Outlined.Schedule, null, tint = ProjectorIvory); Spacer(Modifier.width(4.dp)); Text("RESCHEDULE", color = ProjectorIvory, fontSize = 9.sp)
                    }
                }
            }
            Spacer(Modifier.height(6.dp))
            TextButton(onClick = onDismiss) {
                Text(if (stageCheckIn) "DISMISS FOR NOW" else "DISMISS REMINDER", color = MutedText, fontWeight = FontWeight.Bold)
            }
            if (stageCheckIn) {
                TextButton(onClick = onReschedule) {
                    Icon(Icons.Outlined.Schedule, null, tint = MutedGold, modifier = Modifier.size(16.dp)); Spacer(Modifier.width(5.dp)); Text("CHOOSE TIME", color = MutedGold, fontSize = 9.sp)
                }
            }
        }
    }
}
