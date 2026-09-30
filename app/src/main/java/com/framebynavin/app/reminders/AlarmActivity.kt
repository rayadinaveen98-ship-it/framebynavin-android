package com.framebynavin.app.reminders

import android.app.DatePickerDialog
import android.app.NotificationManager
import android.app.TimePickerDialog
import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.lifecycle.lifecycleScope
import com.framebynavin.app.data.CreatorOsSettingsStore
import com.framebynavin.app.data.CreatorWorkflowEngine
import com.framebynavin.app.data.ProjectPulseEngine
import com.framebynavin.app.data.TaskStore
import com.framebynavin.app.ui.theme.FrameByNavinTheme
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.Calendar

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
                ReminderCheckInScreen(
                    kind = ReminderCheckInKind.ALARM,
                    title = task.title,
                    dueLabel = task.dueLabel,
                    stageLabel = CreatorWorkflowEngine.currentStage(task).label,
                    notes = task.notes,
                    snoozeMinutes = snoozeMinutes,
                    stageCheckIn = ProjectPulseEngine.isStageCheckIn(task),
                    onStageDone = { dispatchReminderAction(ReminderConstants.ACTION_STAGE_DONE, task.id) },
                    onWorking = { dispatchReminderAction(ReminderConstants.ACTION_STARTED, task.id) },
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
