package com.framebynavin.app.reminders

import android.app.NotificationManager
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Check
import androidx.compose.material.icons.outlined.Replay
import androidx.compose.material.icons.outlined.Snooze
import androidx.compose.material.icons.outlined.Work
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.lifecycleScope
import com.framebynavin.app.data.ReminderMode
import com.framebynavin.app.data.TaskStatus
import com.framebynavin.app.data.TaskStore
import com.framebynavin.app.ui.theme.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class VoiceReminderActivity : ComponentActivity() {
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

        setContent {
            FrameByNavinTheme {
                VoiceReminderScreen(
                    task = task,
                    onWorking = { working(task.id) },
                    onRepeat = { VoiceReminderService.start(applicationContext, task.copy(voiceRepeatCount = 1)) },
                    onSnooze = { snooze(task.id) },
                    onDone = { done(task.id) },
                )
            }
        }
    }

    private fun working(taskId: String) {
        lifecycleScope.launch(Dispatchers.IO) {
            val updated = store.updateTask(taskId) { task ->
                val isSmart = task.reminderMode == ReminderMode.SMART || task.smartEscalationEnabled
                task.copy(
                    status = TaskStatus.WORKING,
                    progress = maxOf(task.progress, 15),
                    workingUntilMillis = if (isSmart)
                        System.currentTimeMillis() + ReminderConstants.WORKING_QUIET_MINUTES * 60_000L
                    else task.workingUntilMillis,
                )
            }
            scheduler.cancel(taskId)
            smartScheduler.cancel(taskId)
            if (updated?.reminderEnabled == true) {
                if (updated.reminderMode == ReminderMode.SMART || updated.smartEscalationEnabled) smartScheduler.schedule(updated)
                else scheduler.schedule(updated)
            }
            finishVoice()
        }
    }

    private fun snooze(taskId: String) {
        lifecycleScope.launch(Dispatchers.IO) {
            val updated = store.updateTask(taskId) { task ->
                task.copy(
                    reminderEnabled = true,
                    reminderAtMillis = System.currentTimeMillis() + ReminderConstants.SNOOZE_MINUTES * 60_000L,
                    snoozeCount = task.snoozeCount + 1,
                    workingUntilMillis = 0L,
                )
            }
            scheduler.cancel(taskId)
            smartScheduler.cancel(taskId)
            if (updated != null) {
                if (updated.reminderMode == ReminderMode.SMART || updated.smartEscalationEnabled) smartScheduler.schedule(updated)
                else scheduler.schedule(updated)
            }
            finishVoice()
        }
    }

    private fun done(taskId: String) {
        lifecycleScope.launch(Dispatchers.IO) {
            store.updateTask(taskId) { task ->
                task.copy(
                    status = TaskStatus.DONE,
                    progress = 100,
                    reminderEnabled = false,
                    smartEscalationEnabled = false,
                    voiceEnabled = false,
                    reminderMode = ReminderMode.NONE,
                    workingUntilMillis = 0L,
                )
            }
            scheduler.cancel(taskId)
            smartScheduler.cancel(taskId)
            finishVoice()
        }
    }

    private suspend fun finishVoice() {
        VoiceReminderService.stop(applicationContext)
        getSystemService(NotificationManager::class.java).cancel(
            VoiceReminderService.notificationId(intent.getStringExtra(ReminderConstants.EXTRA_TASK_ID).orEmpty())
        )
        withContext(Dispatchers.Main) { finishAndRemoveTask() }
    }
}

@Composable
private fun VoiceReminderScreen(
    task: com.framebynavin.app.data.CreatorTask,
    onWorking: () -> Unit,
    onRepeat: () -> Unit,
    onSnooze: () -> Unit,
    onDone: () -> Unit,
) {
    BackHandler(enabled = true) { }
    val transition = rememberInfiniteTransition(label = "voice")
    val pulse = transition.animateFloat(
        initialValue = 0.82f,
        targetValue = 1.06f,
        animationSpec = infiniteRepeatable(tween(850), RepeatMode.Reverse),
        label = "voicePulse"
    )

    Box(
        Modifier
            .fillMaxSize()
            .background(CinemaBlack)
            .statusBarsPadding()
            .navigationBarsPadding()
            .padding(24.dp)
    ) {
        Column(
            Modifier.align(Alignment.Center),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Box(
                Modifier
                    .size(92.dp)
                    .scale(pulse.value)
                    .background(MutedGold.copy(alpha = 0.13f), CircleShape),
                contentAlignment = Alignment.Center,
            ) {
                Row(horizontalArrangement = Arrangement.spacedBy(5.dp), verticalAlignment = Alignment.CenterVertically) {
                    listOf(22.dp, 38.dp, 28.dp, 44.dp, 20.dp).forEach { height ->
                        Box(
                            Modifier
                                .width(4.dp)
                                .height(height)
                                .background(MutedGold, RoundedCornerShape(10.dp))
                        )
                    }
                }
            }

            Spacer(Modifier.height(24.dp))
            Text("FRAMEBYNAVIN · VOICE REMINDER", color = MutedGold, fontSize = 10.sp, letterSpacing = 1.4.sp, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(12.dp))
            Text(task.title, color = ProjectorIvory, fontSize = 32.sp, lineHeight = 36.sp, fontWeight = FontWeight.Black, textAlign = TextAlign.Center)
            Spacer(Modifier.height(8.dp))
            Text("${task.platform} · ${task.contentType}", color = MutedText, fontSize = 12.sp)
            Text(task.dueLabel, color = MutedGold, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
            Spacer(Modifier.height(8.dp))
            Text(
                "${VoicePersonaEngine.label(task.voicePersona)} voice · ${task.voiceRepeatCount} announcement${if (task.voiceRepeatCount == 1) "" else "s"}",
                color = Color(0xFF817B74),
                fontSize = 10.sp,
            )
            if (task.notes.isNotBlank()) {
                Spacer(Modifier.height(12.dp))
                Text(task.notes, color = MutedText, fontSize = 12.sp, lineHeight = 18.sp, textAlign = TextAlign.Center)
            }

            Spacer(Modifier.height(34.dp))
            Button(
                onClick = onWorking,
                modifier = Modifier.fillMaxWidth().height(56.dp),
                colors = ButtonDefaults.buttonColors(containerColor = RecRed),
                shape = RoundedCornerShape(16.dp),
            ) {
                Icon(Icons.Outlined.Work, null)
                Spacer(Modifier.width(8.dp))
                Text("I'M WORKING ON IT", fontWeight = FontWeight.Black)
            }
            Spacer(Modifier.height(10.dp))
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedButton(
                    onClick = onRepeat,
                    modifier = Modifier.weight(1f).height(52.dp),
                    border = androidx.compose.foundation.BorderStroke(1.dp, CinemaLine),
                    shape = RoundedCornerShape(14.dp),
                ) {
                    Icon(Icons.Outlined.Replay, null, tint = ProjectorIvory)
                    Spacer(Modifier.width(5.dp))
                    Text("REPEAT", color = ProjectorIvory, fontSize = 10.sp)
                }
                OutlinedButton(
                    onClick = onSnooze,
                    modifier = Modifier.weight(1f).height(52.dp),
                    border = androidx.compose.foundation.BorderStroke(1.dp, CinemaLine),
                    shape = RoundedCornerShape(14.dp),
                ) {
                    Icon(Icons.Outlined.Snooze, null, tint = ProjectorIvory)
                    Spacer(Modifier.width(5.dp))
                    Text("SNOOZE 10m", color = ProjectorIvory, fontSize = 10.sp)
                }
            }
            Spacer(Modifier.height(10.dp))
            TextButton(onClick = onDone) {
                Icon(Icons.Outlined.Check, null, tint = SuccessGreen)
                Spacer(Modifier.width(6.dp))
                Text("DONE", color = SuccessGreen, fontWeight = FontWeight.Bold)
            }
        }
    }
}
