package com.framebynavin.app.reminders

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import com.framebynavin.app.MainActivity
import com.framebynavin.app.data.CreatorTask
import com.framebynavin.app.data.ReminderAlertType
import com.framebynavin.app.data.TaskPriority

class SmartEscalationScheduler(private val context: Context) {
    private val alarmManager = context.getSystemService(AlarmManager::class.java)
    private val ledger = AlarmLedger(context)

    enum class Stage { SOFT, VOICE, ALARM, CRITICAL }

    fun schedule(task: CreatorTask) {
        cancel(task.id)
        if (!task.smartEscalationEnabled || !task.reminderEnabled) return
        val now = System.currentTimeMillis()
        if (task.reminderAtMillis <= now) return

        buildPlan(task, now).forEach { item ->
            if (item.atMillis <= now) return@forEach
            scheduleStage(task, item.stage, item.atMillis)
        }
    }

    fun cancel(taskId: String) {
        Stage.entries.forEach { stage ->
            existingPendingIntent(taskId, stage)?.let { alarmManager.cancel(it) }
            ledger.clear(ledgerKey(taskId, stage))
        }
    }

    private fun scheduleStage(task: CreatorTask, stage: Stage, atMillis: Long) {
        val pendingIntent = stagePendingIntent(task, stage, atMillis)
        val key = ledgerKey(task.id, stage)

        runCatching {
            if ((stage == Stage.ALARM || stage == Stage.CRITICAL) &&
                task.alertType == ReminderAlertType.ALARM &&
                canScheduleExact()
            ) {
                val showIntent = PendingIntent.getActivity(
                    context,
                    requestCode(task.id, stage) xor 0x4400,
                    Intent(context, MainActivity::class.java).apply {
                        flags = Intent.FLAG_ACTIVITY_NEW_TASK or
                            Intent.FLAG_ACTIVITY_CLEAR_TOP or
                            Intent.FLAG_ACTIVITY_SINGLE_TOP
                    },
                    PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
                )
                alarmManager.setAlarmClock(
                    AlarmManager.AlarmClockInfo(atMillis, showIntent),
                    pendingIntent,
                )
            } else if (canScheduleExact()) {
                alarmManager.setExactAndAllowWhileIdle(
                    AlarmManager.RTC_WAKEUP,
                    atMillis,
                    pendingIntent,
                )
            } else {
                alarmManager.setAndAllowWhileIdle(
                    AlarmManager.RTC_WAKEUP,
                    atMillis,
                    pendingIntent,
                )
            }
        }.onSuccess {
            ledger.markScheduled(key, atMillis)
        }.onFailure {
            ledger.clear(key)
        }
    }

    private fun buildPlan(task: CreatorTask, now: Long): List<PlanItem> {
        val target = task.reminderAtMillis
        val remaining = (target - now).coerceAtLeast(1L)
        val effectivePriority = when {
            task.snoozeCount >= 2 && task.priority == TaskPriority.NORMAL -> TaskPriority.IMPORTANT
            task.snoozeCount >= 2 && task.priority == TaskPriority.IMPORTANT -> TaskPriority.CRITICAL
            else -> task.priority
        }

        val raw = when (effectivePriority) {
            TaskPriority.NORMAL -> listOf(
                PlanItem(Stage.SOFT, target),
            )
            TaskPriority.IMPORTANT -> if (remaining >= 30 * 60_000L) {
                listOf(
                    PlanItem(Stage.SOFT, target - 20 * 60_000L),
                    PlanItem(Stage.VOICE, target - 10 * 60_000L),
                    PlanItem(Stage.ALARM, target),
                )
            } else {
                listOf(
                    PlanItem(Stage.SOFT, now + (remaining * 20 / 100)),
                    PlanItem(Stage.VOICE, now + (remaining * 55 / 100)),
                    PlanItem(Stage.ALARM, target),
                )
            }
            TaskPriority.CRITICAL -> if (remaining >= 45 * 60_000L) {
                listOf(
                    PlanItem(Stage.SOFT, target - 30 * 60_000L),
                    PlanItem(Stage.VOICE, target - 15 * 60_000L),
                    PlanItem(Stage.ALARM, target - 5 * 60_000L),
                    PlanItem(Stage.CRITICAL, target),
                )
            } else {
                listOf(
                    PlanItem(Stage.SOFT, now + (remaining * 15 / 100)),
                    PlanItem(Stage.VOICE, now + (remaining * 40 / 100)),
                    PlanItem(Stage.ALARM, now + (remaining * 70 / 100)),
                    PlanItem(Stage.CRITICAL, target),
                )
            }
        }

        return raw
            .filter { task.workingUntilMillis <= now || it.atMillis > task.workingUntilMillis }
            .filter { item ->
                task.alertType == ReminderAlertType.ALARM ||
                    (item.stage != Stage.ALARM && item.stage != Stage.CRITICAL)
            }
            .distinctBy { it.stage }
    }

    private fun stagePendingIntent(task: CreatorTask, stage: Stage, atMillis: Long): PendingIntent {
        val intent = Intent(context, EscalationReceiver::class.java)
            .putExtra(ReminderConstants.EXTRA_TASK_ID, task.id)
            .putExtra(ReminderConstants.EXTRA_TITLE, task.title)
            .putExtra(ReminderConstants.EXTRA_PLATFORM, task.platform)
            .putExtra(ReminderConstants.EXTRA_CONTENT_TYPE, task.contentType)
            .putExtra(ReminderConstants.EXTRA_DUE_LABEL, task.dueLabel)
            .putExtra(ReminderConstants.EXTRA_PRIORITY, task.priority.name)
            .putExtra(ReminderConstants.EXTRA_NOTES, task.notes)
            .putExtra(ReminderConstants.EXTRA_SCHEDULED_AT, atMillis)
            .putExtra(ReminderConstants.EXTRA_TARGET_AT, task.reminderAtMillis)
            .putExtra(ReminderConstants.EXTRA_ALERT_TYPE, task.alertType.name)
            .putExtra(ReminderConstants.EXTRA_ALARM_SOUND_URI, task.alarmSoundUri)
            .putExtra(ReminderConstants.EXTRA_VOICE_ENABLED, task.voiceEnabled)
            .putExtra(ReminderConstants.EXTRA_ESCALATION_STAGE, stage.name)

        return PendingIntent.getBroadcast(
            context,
            requestCode(task.id, stage),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
    }

    private fun existingPendingIntent(taskId: String, stage: Stage): PendingIntent? =
        PendingIntent.getBroadcast(
            context,
            requestCode(taskId, stage),
            Intent(context, EscalationReceiver::class.java),
            PendingIntent.FLAG_NO_CREATE or PendingIntent.FLAG_IMMUTABLE,
        )

    private fun requestCode(taskId: String, stage: Stage): Int =
        ("$taskId:${stage.name}").hashCode()

    private fun ledgerKey(taskId: String, stage: Stage): String = "$taskId#${stage.name}"

    private fun canScheduleExact(): Boolean =
        Build.VERSION.SDK_INT < Build.VERSION_CODES.S || alarmManager.canScheduleExactAlarms()

    private data class PlanItem(val stage: Stage, val atMillis: Long)
}
