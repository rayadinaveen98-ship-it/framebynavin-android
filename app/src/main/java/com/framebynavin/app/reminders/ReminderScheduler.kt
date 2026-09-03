package com.framebynavin.app.reminders

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import com.framebynavin.app.data.CreatorTask

class ReminderScheduler(private val context: Context) {
    private val alarmManager = context.getSystemService(AlarmManager::class.java)
    private val ledger = AlarmLedger(context)

    fun canScheduleExact(): Boolean =
        Build.VERSION.SDK_INT < Build.VERSION_CODES.S || alarmManager.canScheduleExactAlarms()

    fun schedule(task: CreatorTask) {
        if (!task.reminderEnabled || task.reminderAtMillis <= System.currentTimeMillis()) {
            cancel(task.id)
            return
        }

        val pendingIntent = alarmPendingIntent(task)
        ledger.markScheduled(task.id, task.reminderAtMillis)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S && !alarmManager.canScheduleExactAlarms()) {
            alarmManager.setAndAllowWhileIdle(
                AlarmManager.RTC_WAKEUP,
                task.reminderAtMillis,
                pendingIntent,
            )
        } else {
            alarmManager.setExactAndAllowWhileIdle(
                AlarmManager.RTC_WAKEUP,
                task.reminderAtMillis,
                pendingIntent,
            )
        }
    }

    fun cancel(taskId: String) {
        existingPendingIntent(taskId)?.let { alarmManager.cancel(it) }
        ledger.clear(taskId)
    }

    private fun alarmPendingIntent(task: CreatorTask): PendingIntent {
        val intent = Intent(context, ReminderReceiver::class.java)
            .putExtra(ReminderConstants.EXTRA_TASK_ID, task.id)
            .putExtra(ReminderConstants.EXTRA_TITLE, task.title)
            .putExtra(ReminderConstants.EXTRA_PLATFORM, task.platform)
            .putExtra(ReminderConstants.EXTRA_CONTENT_TYPE, task.contentType)
            .putExtra(ReminderConstants.EXTRA_DUE_LABEL, task.dueLabel)
            .putExtra(ReminderConstants.EXTRA_PRIORITY, task.priority.name)
            .putExtra(ReminderConstants.EXTRA_NOTES, task.notes)
            .putExtra(ReminderConstants.EXTRA_SCHEDULED_AT, task.reminderAtMillis)

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
