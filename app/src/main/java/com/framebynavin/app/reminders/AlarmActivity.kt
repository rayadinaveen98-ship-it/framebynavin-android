package com.framebynavin.app.reminders

import android.app.DatePickerDialog
import android.app.NotificationManager
import android.app.TimePickerDialog
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
import com.framebynavin.app.data.TaskStatus
import com.framebynavin.app.data.TaskStore
import com.framebynavin.app.ui.theme.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

class AlarmActivity : ComponentActivity() {
    private val store by lazy { TaskStore(applicationContext) }
    private val scheduler by lazy { ReminderScheduler(applicationContext) }
    private val smartScheduler by lazy { SmartEscalationScheduler(applicationContext) }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setShowWhenLocked(true)
        setTurnScreenOn(true)
        window.addFlags(
            android.view.WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON or
                android.view.WindowManager.LayoutParams.FLAG_ALLOW_LOCK_WHILE_SCREEN_ON
        )

        val task = intent.toTask() ?: run {
            finish()
            return
        }
        val snoozeMinutes = CreatorOsSettingsStore(applicationContext).snapshot().snoozeMinutes

        setContent {
            FrameByNavinTheme {
                NativeAlarmScreen(
                    title = task.title,
                    dueLabel = task.dueLabel,
                    notes = task.notes,
                    snoozeMinutes = snoozeMinutes,
                    onDone = { acknowledgeDone(task.id) },
                    onWorking = { acknowledgeWorking(task.id) },
                    onSnooze = { snooze(task.id) },
                    onReschedule = { openReschedulePicker(task.id) },
                )
            }
        }
    }

    private fun acknowledgeDone(taskId: String) {
        lifecycleScope.launch(Dispatchers.IO) {
            store.updateTask(taskId) { task ->
                task.copy(
                    status = TaskStatus.DONE,
                    progress = 100,
                    reminderEnabled = false,
                    smartEscalationEnabled = false,
                    workingUntilMillis = 0L,
                )
            }
            scheduler.cancel(taskId)
            smartScheduler.cancel(taskId)
            finishAlarm()
        }
    }

    private fun acknowledgeWorking(taskId: String) {
        lifecycleScope.launch(Dispatchers.IO) {
            val updated = store.updateTask(taskId) { task ->
                task.copy(
                    status = TaskStatus.WORKING,
                    progress = maxOf(task.progress, 15),
                    workingUntilMillis = if (task.smartEscalationEnabled)
                        System.currentTimeMillis() + ReminderConstants.WORKING_QUIET_MINUTES * 60_000L
                    else task.workingUntilMillis,
                )
            }
            scheduler.cancel(taskId)
            smartScheduler.cancel(taskId)
            if (updated?.reminderEnabled == true) {
                if (updated.smartEscalationEnabled) smartScheduler.schedule(updated) else scheduler.schedule(updated)
            }
            finishAlarm()
        }
    }

    private fun snooze(taskId: String) {
        lifecycleScope.launch(Dispatchers.IO) {
            val snoozeMinutes = CreatorOsSettingsStore(applicationContext).snapshot().snoozeMinutes
            val updated = store.updateTask(taskId) { task ->
                task.copy(
                    reminderEnabled = true,
                    reminderAtMillis = System.currentTimeMillis() + snoozeMinutes * 60_000L,
                    snoozeCount = task.snoozeCount + 1,
                    workingUntilMillis = 0L,
                )
            }
            scheduler.cancel(taskId)
            smartScheduler.cancel(taskId)
            if (updated != null) {
                if (updated.smartEscalationEnabled) smartScheduler.schedule(updated) else scheduler.schedule(updated)
            }
            finishAlarm()
        }
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
                    initial.get(Calendar.HOUR_OF_DAY),
                    initial.get(Calendar.MINUTE),
                    false,
                ).show()
            },
            initial.get(Calendar.YEAR), initial.get(Calendar.MONTH), initial.get(Calendar.DAY_OF_MONTH)
        ).show()
    }

    private fun reschedule(taskId: String, atMillis: Long) {
        lifecycleScope.launch(Dispatchers.IO) {
            val updated = store.updateTask(taskId) { task ->
                task.copy(
                    reminderEnabled = true,
                    reminderAtMillis = atMillis,
                    snoozeCount = 0,
                    workingUntilMillis = 0L,
                    autoStageReminder = false,
                )
            }
            scheduler.cancel(taskId)
            smartScheduler.cancel(taskId)
            if (updated != null) {
                if (updated.smartEscalationEnabled) smartScheduler.schedule(updated) else scheduler.schedule(updated)
            }
            finishAlarm()
        }
    }

    private suspend fun finishAlarm() {
        AlarmRingingService.stop(applicationContext)
        getSystemService(NotificationManager::class.java).cancel(
            AlarmRingingService.notificationId(intent.getStringExtra(ReminderConstants.EXTRA_TASK_ID).orEmpty())
        )
        withContext(Dispatchers.Main) { finishAndRemoveTask() }
    }
}

@Composable
private fun NativeAlarmScreen(
    title: String,
    dueLabel: String,
    notes: String,
    snoozeMinutes: Int,
    onDone: () -> Unit,
    onWorking: () -> Unit,
    onSnooze: () -> Unit,
    onReschedule: () -> Unit,
) {
    BackHandler(enabled = true) { }

    Box(
        Modifier
            .fillMaxSize()
            .background(Brush.radialGradient(listOf(RecRed.copy(alpha = .13f), CinemaBlack), radius = 900f))
            .statusBarsPadding()
            .navigationBarsPadding()
            .padding(24.dp)
    ) {
        Text(
            SimpleDateFormat("h:mm a", Locale.getDefault()).format(Date()),
            color = MutedText,
            fontSize = 11.sp,
            modifier = Modifier.align(Alignment.TopCenter)
        )

        Column(
            Modifier.align(Alignment.Center),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Box(
                Modifier.size(92.dp).background(RecRed.copy(alpha = 0.13f), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Box(Modifier.size(62.dp).background(RecRed.copy(alpha = .12f), CircleShape), contentAlignment = Alignment.Center) {
                    Icon(Icons.Outlined.Alarm, null, tint = RecRed, modifier = Modifier.size(34.dp))
                }
            }
            Spacer(Modifier.height(25.dp))
            Text("DEADLINE REMINDER", color = RecRed, fontSize = 9.sp, letterSpacing = 1.5.sp, fontWeight = FontWeight.Black)
            Spacer(Modifier.height(10.dp))
            Text("This needs your attention.", color = MutedText, fontSize = 11.sp)
            Spacer(Modifier.height(7.dp))
            Text(title, color = ProjectorIvory, fontSize = 33.sp, lineHeight = 37.sp, fontWeight = FontWeight.Black, textAlign = TextAlign.Center)
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
            Spacer(Modifier.height(31.dp))

            Button(
                onClick = onWorking,
                modifier = Modifier.fillMaxWidth().height(56.dp),
                colors = ButtonDefaults.buttonColors(containerColor = RecRed),
                shape = RoundedCornerShape(17.dp)
            ) {
                Icon(Icons.Outlined.Work, null)
                Spacer(Modifier.width(8.dp))
                Text("I'M WORKING ON IT", fontWeight = FontWeight.Black)
            }
            Spacer(Modifier.height(10.dp))
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedButton(onClick = onSnooze, modifier = Modifier.weight(1f).height(52.dp), border = BorderStroke(1.dp, CinemaLine), shape = RoundedCornerShape(15.dp)) {
                    Icon(Icons.Outlined.Snooze, null, tint = ProjectorIvory); Spacer(Modifier.width(6.dp)); Text("SNOOZE ${snoozeMinutes}m", color = ProjectorIvory, fontSize = 9.5.sp)
                }
                OutlinedButton(onClick = onReschedule, modifier = Modifier.weight(1f).height(52.dp), border = BorderStroke(1.dp, CinemaLine), shape = RoundedCornerShape(15.dp)) {
                    Icon(Icons.Outlined.Schedule, null, tint = ProjectorIvory); Spacer(Modifier.width(6.dp)); Text("RESCHEDULE", color = ProjectorIvory, fontSize = 9.5.sp)
                }
            }
            Spacer(Modifier.height(9.dp))
            TextButton(onClick = onDone) {
                Icon(Icons.Outlined.Check, null, tint = SuccessGreen); Spacer(Modifier.width(6.dp)); Text("DONE", color = SuccessGreen, fontWeight = FontWeight.Bold)
            }
        }
    }
}
