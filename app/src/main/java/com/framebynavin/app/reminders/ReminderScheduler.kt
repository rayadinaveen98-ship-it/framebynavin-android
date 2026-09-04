package com.framebynavin.app.reminders

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import com.framebynavin.app.MainActivity
import com.framebynavin.app.data.CreatorTask
import com.framebynavin.app.data.ReminderAlertType
import com.framebynavin.app.data.ReminderMode

class ReminderScheduler(private val context: Context) {
    private val alarmManager = context.getSystemService(AlarmManager::class.java)
    private val ledger = AlarmLedger(context)

    fun canScheduleExact(): Boolean =
        Build.VERSION.SDK_INT < Build.VERSION_CODES.S || alarmManager.canScheduleExactAlarms()

    fun schedule(task: CreatorTask) {
        if (!task.reminderEnabled || task.reminderMode == ReminderMode.NONE || task.reminderAtMillis <= System.currentTimeMillis()) {
            cancel(task.id)
            return
        }

        val exactDelivery = canScheduleExact()
        val pendingIntent = alarmPendingIntent(task, exactDelivery)
        val isNativeAlarm = task.reminderMode == ReminderMode.ALARM || task.alertType == ReminderAlertType.ALARM

        if (isNativeAlarm) {
            runCatching {
                if (exactDelivery) {
                    val showIntent = PendingIntent.getActivity(
                        context,
                        task.id.hashCode() xor 0x51A1,
                        Intent(context, MainActivity::class.java).apply {
                            flags = Intent.FLAG_ACTIVITY_NEW_TASK or
                                Intent.FLAG_ACTIVITY_CLEAR_TOP or
                                Intent.FLAG_ACTIVITY_SINGLE_TOP
                        },
                        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
                    )

                    alarmManager.setAlarmClock(
                        AlarmManager.AlarmClockInfo(task.reminderAtMillis, showIntent),
                        pendingIntent,
                    )
                } else {
                    // Android 12+ can revoke exact-alarm access. Never silently drop an
                    // Alarm-mode reminder: keep a best-effort RTC_WAKEUP fallback scheduled
                    // until exact access is granted again. ReminderRecoveryReceiver will
                    // upgrade it back to an exact alarm when permission state changes.
                    alarmManager.setAndAllowWhileIdle(
                        AlarmManager.RTC_WAKEUP,
                        task.reminderAtMillis,
                        pendingIntent,
                    )
                }
            }.onSuccess {
                ledger.markScheduled(task.id, task.reminderAtMillis)
            }.onFailure {
                ledger.clear(task.id)
            }
            return
        }

        runCatching {
            if (exactDelivery) {
                alarmManager.setExactAndAllowWhileIdle(
                    AlarmManager.RTC_WAKEUP,
                    task.reminderAtMillis,
                    pendingIntent,
                )
            } else {
                alarmManager.setAndAllowWhileIdle(
                    AlarmManager.RTC_WAKEUP,
                    task.reminderAtMillis,
                    pendingIntent,
                )
            }
        }.onSuccess {
            ledger.markScheduled(task.id, task.reminderAtMillis)
        }.onFailure {
            ledger.clear(task.id)
        }
    }

    fun cancel(taskId: String) {
        existingPendingIntent(taskId)?.let { alarmManager.cancel(it) }
        ledger.clear(taskId)
    }

    private fun alarmPendingIntent(task: CreatorTask, exactDelivery: Boolean): PendingIntent {
        val intent = Intent(context, ReminderReceiver::class.java)
            .putExtra(ReminderConstants.EXTRA_TASK_ID, task.id)
            .putExtra(ReminderConstants.EXTRA_TITLE, task.title)
            .putExtra(ReminderConstants.EXTRA_PLATFORM, task.platform)
            .putExtra(ReminderConstants.EXTRA_CONTENT_TYPE, task.contentType)
            .putExtra(ReminderConstants.EXTRA_DUE_LABEL, task.dueLabel)
            .putExtra(ReminderConstants.EXTRA_DUE_AT, task.dueAtMillis)
            .putExtra(ReminderConstants.EXTRA_PRIORITY, task.priority.name)
            .putExtra(ReminderConstants.EXTRA_PROGRESS, task.progress)
            .putExtra(ReminderConstants.EXTRA_NOTES, task.notes)
            .putExtra(ReminderConstants.EXTRA_SCHEDULED_AT, task.reminderAtMillis)
            .putExtra(ReminderConstants.EXTRA_ALERT_TYPE, task.alertType.name)
            .putExtra(ReminderConstants.EXTRA_ALARM_SOUND_URI, task.alarmSoundUri)
            .putExtra(ReminderConstants.EXTRA_VOICE_ENABLED, task.voiceEnabled)
            .putExtra(ReminderConstants.EXTRA_REMINDER_MODE, task.reminderMode.name)
            .putExtra(ReminderConstants.EXTRA_VOICE_PERSONA, task.voicePersona.name)
            .putExtra(ReminderConstants.EXTRA_VOICE_REPEAT_COUNT, task.voiceRepeatCount)
            .putExtra(ReminderConstants.EXTRA_VOICE_REPEAT_INTERVAL, task.voiceRepeatIntervalSeconds)
            .putExtra(ReminderConstants.EXTRA_ALARM_TIMEOUT_SECONDS, task.alarmTimeoutSeconds)
            .putExtra(ReminderConstants.EXTRA_EXACT_DELIVERY, exactDelivery)

        return PendingIntent.getBroadcast(
            context,
            task.id.hashCode(),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
    }

    private fun existingPendingIntent(taskId: String): PendingIntent? {
        val intent = Intent(context, ReminderReceiver::class.java)
            .putExtra(ReminderConstants.EXTRA_TASK_ID, taskId)
        return PendingIntent.getBroadcast(
            context,
            taskId.hashCode(),
            intent,
            PendingIntent.FLAG_NO_CREATE or PendingIntent.FLAG_IMMUTABLE,
        )
    }
}
