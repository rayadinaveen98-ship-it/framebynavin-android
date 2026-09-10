package com.framebynavin.app.reminders

import android.app.DatePickerDialog
import android.app.NotificationManager
import android.app.TimePickerDialog
import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Check
import androidx.compose.material.icons.outlined.PauseCircle
import androidx.compose.material.icons.outlined.Replay
import androidx.compose.material.icons.outlined.Snooze
import androidx.compose.material.icons.outlined.Work
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.lifecycleScope
import com.framebynavin.app.data.CreatorOsSettingsStore
import com.framebynavin.app.data.CreatorTask
import com.framebynavin.app.data.CreatorWorkflowEngine
import com.framebynavin.app.data.ProjectPulseEngine
import com.framebynavin.app.data.TaskStore
import com.framebynavin.app.ui.theme.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.Calendar

class VoiceReminderActivity : ComponentActivity() {
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
        ReminderSurfaceRegistry.attachVoice(this, taskId, occurrenceId)
        val snoozeMinutes = CreatorOsSettingsStore(applicationContext).snapshot().snoozeMinutes

        lifecycleScope.launch {
            delay(VoiceReminderService.totalWindowMillis(task) + 750L)
            if (!isFinishing && !isDestroyed) {
                VoiceReminderService.stop(applicationContext, taskId, occurrenceId)
                if (!ReminderOccurrenceStore(applicationContext).matches(task, occurrenceId)) {
                    finishAndRemoveTask()
                    return@launch
                }
                getSystemService(NotificationManager::class.java).cancel(VoiceReminderService.notificationId(task.id))
                finishAndRemoveTask()
            }
        }

        setContent {
            FrameByNavinTheme {
                VoiceReminderScreen(
                    task = task,
                    snoozeMinutes = snoozeMinutes,
                    stageCheckIn = ProjectPulseEngine.isStageCheckIn(task),
                    onStageDone = { dispatchReminderAction(ReminderConstants.ACTION_STAGE_DONE, task.id) },
                    onWorking = { dispatchReminderAction(ReminderConstants.ACTION_STARTED, task.id) },
                    onRepeat = { VoiceReminderService.start(applicationContext, task.copy(voiceRepeatCount = 1), occurrenceId) },
                    onSnooze = { dispatchReminderAction(ReminderConstants.ACTION_SNOOZE, task.id) },
                    onPause = { dispatchReminderAction(ReminderConstants.ACTION_PAUSE, task.id) },
                    onReschedule = { openReschedulePicker(task.id) },
                    onDismiss = { dispatchReminderAction(ReminderConstants.ACTION_DISMISS, task.id) },
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
        ReminderSurfaceRegistry.detachVoice(this)
        super.onDestroy()
    }

    private fun dispatchReminderAction(action: String, taskId: String) {
        sendBroadcast(Intent(this, ReminderActionReceiver::class.java).apply {
            this.action = action
            putExtra(ReminderConstants.EXTRA_TASK_ID, taskId)
            putExtra(ReminderConstants.EXTRA_OCCURRENCE_ID, occurrenceId)
        })
        lifecycleScope.launch { finishVoice() }
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
                        if (atMillis > System.currentTimeMillis()) {
                            sendBroadcast(Intent(this, ReminderActionReceiver::class.java).apply {
                                action = ReminderConstants.ACTION_RESCHEDULE
                                putExtra(ReminderConstants.EXTRA_TASK_ID, taskId)
                                putExtra(ReminderConstants.EXTRA_OCCURRENCE_ID, occurrenceId)
                                putExtra(ReminderConstants.EXTRA_RESCHEDULE_AT, atMillis)
                            })
                            lifecycleScope.launch { finishVoice() }
                        } else {
                            Toast.makeText(this, "Choose a future time", Toast.LENGTH_SHORT).show()
                        }
                    },
                    initial.get(Calendar.HOUR_OF_DAY), initial.get(Calendar.MINUTE), false,
                ).show()
            },
            initial.get(Calendar.YEAR), initial.get(Calendar.MONTH), initial.get(Calendar.DAY_OF_MONTH),
        ).show()
    }

    private suspend fun finishVoice() {
        VoiceReminderService.stop(applicationContext, taskId, occurrenceId)
        withContext(Dispatchers.Main) { if (!isFinishing) finishAndRemoveTask() }
    }
}

@Composable
private fun VoiceReminderScreen(
    task: CreatorTask,
    snoozeMinutes: Int,
    stageCheckIn: Boolean,
    onStageDone: () -> Unit,
    onWorking: () -> Unit,
    onRepeat: () -> Unit,
    onSnooze: () -> Unit,
    onPause: () -> Unit,
    onReschedule: () -> Unit,
    onDismiss: () -> Unit,
) {
    BackHandler(enabled = true) { }
    val transition = rememberInfiniteTransition(label = "voice")
    val pulse = transition.animateFloat(
        initialValue = 0.88f,
        targetValue = 1.05f,
        animationSpec = infiniteRepeatable(tween(900), RepeatMode.Reverse),
        label = "voicePulse"
    )
    Box(
        Modifier.fillMaxSize()
            .background(Brush.radialGradient(listOf(MutedGold.copy(alpha = .10f), CinemaBlack), radius = 900f))
            .statusBarsPadding().navigationBarsPadding().padding(24.dp)
    ) {
        Column(Modifier.align(Alignment.Center), horizontalAlignment = Alignment.CenterHorizontally) {
            Box(
                Modifier.size(104.dp).scale(pulse.value).background(MutedGold.copy(alpha = 0.10f), CircleShape),
                contentAlignment = Alignment.Center,
            ) {
                Row(horizontalArrangement = Arrangement.spacedBy(5.dp), verticalAlignment = Alignment.CenterVertically) {
                    listOf(20.dp, 37.dp, 27.dp, 46.dp, 24.dp).forEach { height ->
                        Box(Modifier.width(4.dp).height(height).background(MutedGold, RoundedCornerShape(10.dp)))
                    }
                }
            }

            Spacer(Modifier.height(24.dp))
            Text(if (stageCheckIn) "STAGE CHECK-IN" else "VOICE REMINDER", color = MutedGold, fontSize = 9.sp, fontWeight = FontWeight.Black, letterSpacing = 1.3.sp)
            Spacer(Modifier.height(4.dp))
            Text(task.title, color = ProjectorIvory, fontSize = 31.sp, lineHeight = 35.sp, fontWeight = FontWeight.Black, textAlign = TextAlign.Center)
            if (stageCheckIn) {
                Spacer(Modifier.height(8.dp))
                Text("${CreatorWorkflowEngine.currentStage(task).label} · How is this stage going?", color = MutedGold, fontSize = 13.sp, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center)
            }
            Spacer(Modifier.height(13.dp))

            if (task.notes.isNotBlank()) {
                Surface(Modifier.fillMaxWidth(), RoundedCornerShape(17.dp), CinemaSurface, border = BorderStroke(1.dp, CinemaLine)) {
                    Text(task.notes, color = MutedText, fontSize = 11.sp, lineHeight = 17.sp, textAlign = TextAlign.Center, modifier = Modifier.padding(13.dp))
                }
                Spacer(Modifier.height(16.dp))
            }

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
                OutlinedButton(onClick = onRepeat, modifier = Modifier.weight(1f).height(50.dp), border = BorderStroke(1.dp, CinemaLine), shape = RoundedCornerShape(15.dp)) {
                    Icon(Icons.Outlined.Replay, null, tint = ProjectorIvory); Spacer(Modifier.width(4.dp)); Text("REPLAY", color = ProjectorIvory, fontSize = 9.sp, maxLines = 1)
                }
                OutlinedButton(onClick = onSnooze, modifier = Modifier.weight(1f).height(50.dp), border = BorderStroke(1.dp, CinemaLine), shape = RoundedCornerShape(15.dp)) {
                    Icon(Icons.Outlined.Snooze, null, tint = ProjectorIvory); Spacer(Modifier.width(4.dp)); Text("SNOOZE ${snoozeMinutes}M", color = ProjectorIvory, fontSize = 9.sp, maxLines = 1)
                }
            }
            Spacer(Modifier.height(8.dp))
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                if (stageCheckIn) {
                    OutlinedButton(onClick = onPause, modifier = Modifier.weight(1f).height(48.dp), border = BorderStroke(1.dp, CinemaLine), shape = RoundedCornerShape(15.dp)) {
                        Icon(Icons.Outlined.PauseCircle, null, tint = ProjectorIvory); Spacer(Modifier.width(4.dp)); Text("PAUSE 2H", color = ProjectorIvory, fontSize = 9.sp, maxLines = 1)
                    }
                }
                OutlinedButton(onClick = onReschedule, modifier = Modifier.weight(1f).height(48.dp), border = BorderStroke(1.dp, MutedGold.copy(alpha = .6f)), shape = RoundedCornerShape(15.dp)) {
                    Text("CHOOSE TIME", color = MutedGold, fontSize = 9.sp, fontWeight = FontWeight.Bold, maxLines = 1)
                }
            }
            Spacer(Modifier.height(6.dp))
            TextButton(onClick = onDismiss) {
                Text(if (stageCheckIn) "DISMISS FOR NOW" else "DISMISS REMINDER", color = MutedText, fontWeight = FontWeight.Bold)
            }
        }
    }
}
