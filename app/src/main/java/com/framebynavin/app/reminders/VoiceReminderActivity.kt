package com.framebynavin.app.reminders

import android.app.NotificationManager
import android.content.Intent
import android.net.Uri
import android.os.Bundle
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
import androidx.compose.material.icons.outlined.Replay
import androidx.compose.material.icons.outlined.Snooze
import androidx.compose.material.icons.outlined.Work
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.lifecycleScope
import com.framebynavin.app.data.CreatorOsSettingsStore
import com.framebynavin.app.data.CreatorWorkflowEngine
import com.framebynavin.app.data.ReminderMode
import com.framebynavin.app.data.TaskStatus
import com.framebynavin.app.data.TaskStore
import com.framebynavin.app.ui.theme.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class VoiceReminderActivity : ComponentActivity() {
    private val store by lazy { TaskStore(applicationContext) }
    private val scheduler by lazy { ReminderScheduler(applicationContext) }
    private val smartScheduler by lazy { SmartEscalationScheduler(applicationContext) }
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

        val task = intent.toTask() ?: run {
            finish()
            return
        }
        taskId = task.id
        occurrenceId = intent.getStringExtra(ReminderConstants.EXTRA_OCCURRENCE_ID).orEmpty()
        if (!ReminderOccurrenceStore(applicationContext).matches(task, occurrenceId)) {
            finish()
            return
        }
        ReminderSurfaceRegistry.attachVoice(this, taskId, occurrenceId)
        val snoozeMinutes = CreatorOsSettingsStore(applicationContext).snapshot().snoozeMinutes

        // Keep the full-screen UI lifecycle aligned with the TTS service lifecycle.
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
                    onWorking = { dispatchReminderAction(ReminderConstants.ACTION_STARTED, task.id) },
                    onRepeat = { VoiceReminderService.start(applicationContext, task.copy(voiceRepeatCount = 1), occurrenceId) },
                    onSnooze = { dispatchReminderAction(ReminderConstants.ACTION_SNOOZE, task.id) },
                    onDone = { dispatchReminderAction(ReminderConstants.ACTION_DONE, task.id) },
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
        // Never silently rebind an older full-screen action to a new occurrence.
        if (intent.getStringExtra(ReminderConstants.EXTRA_OCCURRENCE_ID) != occurrenceId) finishAndRemoveTask()
    }

    override fun onDestroy() {
        ReminderSurfaceRegistry.detachVoice(this)
        super.onDestroy()
    }

    private fun dispatchReminderAction(action: String, taskId: String) {
        sendBroadcast(
            Intent(this, ReminderActionReceiver::class.java).apply {
                this.action = action
                putExtra(ReminderConstants.EXTRA_TASK_ID, taskId)
                putExtra(ReminderConstants.EXTRA_OCCURRENCE_ID, occurrenceId)
            }
        )
        lifecycleScope.launch { finishVoice() }
    }

    private suspend fun finishVoice() {
        VoiceReminderService.stop(applicationContext, taskId, occurrenceId)
        withContext(Dispatchers.Main) { if (!isFinishing) finishAndRemoveTask() }
    }
}

@Composable
private fun VoiceReminderScreen(
    task: com.framebynavin.app.data.CreatorTask,
    snoozeMinutes: Int,
    onWorking: () -> Unit,
    onRepeat: () -> Unit,
    onSnooze: () -> Unit,
    onDone: () -> Unit,
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
        Modifier
            .fillMaxSize()
            .background(Brush.radialGradient(listOf(MutedGold.copy(alpha = .10f), CinemaBlack), radius = 900f))
            .statusBarsPadding()
            .navigationBarsPadding()
            .padding(24.dp)
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

            Spacer(Modifier.height(26.dp))
            Spacer(Modifier.height(4.dp))
            Text(task.title, color = ProjectorIvory, fontSize = 31.sp, lineHeight = 35.sp, fontWeight = FontWeight.Black, textAlign = TextAlign.Center)
            Spacer(Modifier.height(13.dp))

            Surface(shape = RoundedCornerShape(100.dp), color = Color(0xFF17130F), border = BorderStroke(1.dp, MutedGold.copy(alpha = .28f))) {
                Text(task.dueLabel, color = MutedGold, fontSize = 9.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(horizontal = 9.dp, vertical = 5.dp))
            }

            if (task.notes.isNotBlank()) {
                Spacer(Modifier.height(18.dp))
                Surface(Modifier.fillMaxWidth(), RoundedCornerShape(17.dp), CinemaSurface, border = BorderStroke(1.dp, CinemaLine)) {
                    Text(task.notes, color = MutedText, fontSize = 11.sp, lineHeight = 17.sp, textAlign = TextAlign.Center, modifier = Modifier.padding(13.dp))
                }
            }

            Spacer(Modifier.height(31.dp))
            Button(onClick = onWorking, modifier = Modifier.fillMaxWidth().height(56.dp), colors = ButtonDefaults.buttonColors(containerColor = RecRed), shape = RoundedCornerShape(17.dp)) {
                Icon(Icons.Outlined.Work, null); Spacer(Modifier.width(8.dp)); Text("I'M WORKING ON IT", fontWeight = FontWeight.Black)
            }
            Spacer(Modifier.height(10.dp))
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedButton(onClick = onRepeat, modifier = Modifier.weight(1f).height(52.dp), border = BorderStroke(1.dp, CinemaLine), shape = RoundedCornerShape(15.dp)) {
                    Icon(Icons.Outlined.Replay, null, tint = ProjectorIvory); Spacer(Modifier.width(5.dp)); Text("REPEAT", color = ProjectorIvory, fontSize = 9.5.sp)
                }
                OutlinedButton(onClick = onSnooze, modifier = Modifier.weight(1f).height(52.dp), border = BorderStroke(1.dp, CinemaLine), shape = RoundedCornerShape(15.dp)) {
                    Icon(Icons.Outlined.Snooze, null, tint = ProjectorIvory); Spacer(Modifier.width(5.dp)); Text("SNOOZE ${snoozeMinutes}m", color = ProjectorIvory, fontSize = 9.5.sp)
                }
            }
            Spacer(Modifier.height(9.dp))
            TextButton(onClick = onDone) {
                Icon(Icons.Outlined.Check, null, tint = SuccessGreen); Spacer(Modifier.width(6.dp)); Text("DISMISS REMINDER", color = SuccessGreen, fontWeight = FontWeight.Bold)
            }
        }
    }
}
