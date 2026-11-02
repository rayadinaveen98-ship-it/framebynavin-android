package com.framebynavin.app.reminders

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.util.Log
import com.framebynavin.app.data.CreatorDataGate
import com.framebynavin.app.data.CreatorTask
import com.framebynavin.app.data.ReminderMode
import com.framebynavin.app.data.TaskStore
import org.json.JSONObject

/**
 * Bounded, opt-in Creator audible retries. This is separate from Smart/Project
 * Pulse escalation and from the existing quiet WorkManager follow-up feature.
 *
 * Local receipt data is deliberately not part of Creator cloud backups: a
 * restored phone must never inherit another phone's pending alarm.
 */
internal object V283CreatorAutoRetryPolicy {
    val INTERVAL_CHOICES = setOf(5, 10, 15, 30)
    const val MAX_ATTEMPTS = 3
    const val MAX_LATE_MILLIS = 10L * 60_000L
    private const val MAX_OCCURRENCE_AGE_MILLIS = 24L * 60L * 60_000L

    fun eligible(task: CreatorTask, nowMillis: Long): Boolean =
        task.autoRetryEnabled && task.reminderEnabled &&
            task.reminderMode in setOf(ReminderMode.VOICE, ReminderMode.ALARM) &&
            !task.smartEscalationEnabled && !task.pulseManagedReminder &&
            ReminderActionSafety.isActionable(task) &&
            task.reminderAtMillis > 0L && nowMillis >= task.reminderAtMillis &&
            nowMillis - task.reminderAtMillis < MAX_OCCURRENCE_AGE_MILLIS

    fun nextAt(task: CreatorTask, alreadyPlayedRetries: Int, nowMillis: Long): Long? {
        if (!eligible(task, nowMillis) ||
            task.autoRetryMinutes !in INTERVAL_CHOICES ||
            task.autoRetryMaxAttempts !in 1..MAX_ATTEMPTS ||
            alreadyPlayedRetries !in 0 until task.autoRetryMaxAttempts) return null
        val target = nowMillis + task.autoRetryMinutes * 60_000L
        return target.takeIf { it > nowMillis &&
            it - task.reminderAtMillis < MAX_OCCURRENCE_AGE_MILLIS }
    }

    fun mayDeliver(task: CreatorTask, alreadyPlayedRetries: Int, scheduledAt: Long,
                   nowMillis: Long): Boolean =
        eligible(task, nowMillis) && scheduledAt > 0L &&
            nowMillis >= scheduledAt && nowMillis - scheduledAt <= MAX_LATE_MILLIS &&
            alreadyPlayedRetries in 0 until task.autoRetryMaxAttempts
}

internal object V283CreatorAutoRetry {
    const val ACTION_RETRY = "com.framebynavin.app.creator.AUTO_AUDIBLE_RETRY"
    const val EXTRA_TARGET = "backlot.creator.retry.target"
    const val EXTRA_TOKEN = "backlot.creator.retry.token"
    private const val PREF_NAME = "backlot-v283-creator-audible-retry"
    private val lock = Any()

    private data class Receipt(
        val token: String,
        val epoch: Long,
        val deliveredCount: Int,
        val scheduledAt: Long,
    )

    private fun prefs(context: Context) =
        context.applicationContext.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)

    private fun read(context: Context, id: String): Receipt? = synchronized(lock) {
        val raw = prefs(context).getString(id, null) ?: return@synchronized null
        runCatching {
            val obj = JSONObject(raw)
            Receipt(obj.getString("token"), obj.getLong("epoch"),
                obj.getInt("deliveredCount"), obj.getLong("scheduledAt"))
        }.getOrNull()
    }

    private fun save(context: Context, id: String, receipt: Receipt?): Boolean =
        synchronized(lock) {
            val editor = prefs(context).edit()
            if (receipt == null) editor.remove(id)
            else editor.putString(id, JSONObject()
                .put("token", receipt.token)
                .put("epoch", receipt.epoch)
                .put("deliveredCount", receipt.deliveredCount)
                .put("scheduledAt", receipt.scheduledAt)
                .toString())
            editor.commit()
        }

    private fun pending(context: Context, id: String, token: String,
                        at: Long, flags: Int): PendingIntent? =
        PendingIntent.getBroadcast(
            context.applicationContext, id.hashCode() xor 0x283A,
            Intent(context.applicationContext, ReminderReceiver::class.java)
                .setAction(ACTION_RETRY)
                .setData(Uri.parse("backlot-creator://audible-retry/${Uri.encode(id)}"))
                .putExtra(ReminderConstants.EXTRA_TASK_ID, id)
                .putExtra(EXTRA_TOKEN, token)
                .putExtra(EXTRA_TARGET, at),
            flags,
        )

    private fun armAlarm(context: Context, id: String, token: String,
                         targetMillis: Long, nowMillis: Long) {
        val manager = context.getSystemService(AlarmManager::class.java)
        val pi = pending(context, id, token, targetMillis,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE) ?: return
        val at = maxOf(targetMillis, nowMillis + 1_000L)
        runCatching {
            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.S || manager.canScheduleExactAlarms())
                manager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, at, pi)
            else manager.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, at, pi)
        }.onFailure { Log.w("BacklotCreatorRetry", "Could not schedule optional audible retry", it) }
    }

    private fun cancelAlarm(context: Context, id: String) {
        val pi = pending(context, id, "", 0L,
            PendingIntent.FLAG_NO_CREATE or PendingIntent.FLAG_IMMUTABLE)
        if (pi != null) context.getSystemService(AlarmManager::class.java).cancel(pi)
    }

    fun cancel(context: Context, taskId: String) {
        if (taskId.isBlank()) return
        save(context, taskId, null)
        cancelAlarm(context, taskId)
    }

    /** Called ONLY after the complete voice cycle / normal alarm sound timeout. */
    suspend fun armAfterPlayback(context: Context, taskId: String, token: String,
                                 nowMillis: Long = System.currentTimeMillis()): Boolean {
        if (taskId.isBlank() || token.isBlank()) return false
        val app = context.applicationContext
        return CreatorDataGate.readyTransaction(app) {
            val task = TaskStore(app).load().firstOrNull { it.id == taskId }
                ?: return@readyTransaction false
            if (!ReminderOccurrenceStore(app).matches(task, token)) return@readyTransaction false
            val ack = ReminderAcknowledgementStore(app).awaitingResponse(task)
            if (ack == null || ack.occurrenceId != token ||
                ack.state != ReminderAckState.UNANSWERED) return@readyTransaction false
            val epoch = CreatorDataGate.generation(app)
            val previous = read(app, taskId)?.takeIf { it.epoch == epoch && it.token == token }
            if (previous != null && previous.scheduledAt != 0L) return@readyTransaction false
            val count = previous?.deliveredCount ?: 0
            val target = V283CreatorAutoRetryPolicy.nextAt(task, count, nowMillis)
                ?: return@readyTransaction false
            if (!save(app, taskId, Receipt(token, epoch, count, target))) return@readyTransaction false
            armAlarm(app, taskId, token, target, nowMillis)
            true
        }
    }

    /** CAS consumes the local pending receipt before requesting any new sound. */
    suspend fun deliver(context: Context, taskId: String, token: String,
                        targetMillis: Long, nowMillis: Long = System.currentTimeMillis()): Boolean {
        if (taskId.isBlank() || token.isBlank() || targetMillis <= 0L) return false
        val app = context.applicationContext
        return CreatorDataGate.readyTransaction(app) {
            val task = TaskStore(app).load().firstOrNull { it.id == taskId }
                ?: return@readyTransaction false
            if (!ReminderOccurrenceStore(app).matches(task, token)) return@readyTransaction false
            val ack = ReminderAcknowledgementStore(app).awaitingResponse(task)
            if (ack == null || ack.occurrenceId != token ||
                ack.state != ReminderAckState.UNANSWERED) return@readyTransaction false
            val receipt = read(app, taskId) ?: return@readyTransaction false
            if (receipt.token != token || receipt.epoch != CreatorDataGate.generation(app) ||
                receipt.scheduledAt != targetMillis ||
                !V283CreatorAutoRetryPolicy.mayDeliver(
                    task, receipt.deliveredCount, targetMillis, nowMillis)) return@readyTransaction false
            val consumed = receipt.copy(
                deliveredCount = receipt.deliveredCount + 1, scheduledAt = 0L)
            if (!save(app, taskId, consumed)) return@readyTransaction false
            // A sound-start failure consumes the attempt; at-most-once is safer
            // than retrying an FGS launch indefinitely on Android restrictions.
            runCatching {
                when (task.reminderMode) {
                    ReminderMode.VOICE -> VoiceReminderService.start(app, task, token, forceRestart = true)
                    ReminderMode.ALARM -> AlarmRingingService.start(app, task, occurrenceId = token)
                    else -> return@readyTransaction false
                }
            }.onFailure { Log.w("BacklotCreatorRetry", "Audible retry unavailable; notification remains", it) }
                .isSuccess
        }
    }

    /** Re-arm only still-valid future local receipts after reboot/app recovery. */
    fun reconcile(context: Context, task: CreatorTask, nowMillis: Long) {
        val receipt = read(context, task.id) ?: return
        if (receipt.epoch != CreatorDataGate.generation(context) ||
            !V283CreatorAutoRetryPolicy.eligible(task, nowMillis) ||
            !ReminderOccurrenceStore(context).matches(task, receipt.token) ||
            receipt.deliveredCount !in 0 until task.autoRetryMaxAttempts ||
            receipt.scheduledAt <= 0L ||
            nowMillis > receipt.scheduledAt + V283CreatorAutoRetryPolicy.MAX_LATE_MILLIS) {
            cancel(context, task.id)
            return
        }
        armAlarm(context, task.id, receipt.token, receipt.scheduledAt, nowMillis)
    }
}
