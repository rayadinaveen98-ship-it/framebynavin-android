package com.framebynavin.app.reminders

import android.content.Context

/**
 * Tiny synchronous ledger for the alarm that is currently authoritative per task.
 * This is intentionally separate from DataStore so a cold BroadcastReceiver can
 * reject stale/duplicate alarms without delaying the user-facing notification.
 *
 * It also keeps the source reminder time that was most recently delivered so
 * reboot/time-change recovery can catch up once without repeating the same alert.
 */
class AlarmLedger(context: Context) {
    private val prefs = context.getSharedPreferences("framebynavin_alarm_ledger_v04", Context.MODE_PRIVATE)

    fun markScheduled(taskId: String, scheduledAtMillis: Long) {
        prefs.edit().putLong(key(taskId), scheduledAtMillis).commit()
    }

    fun scheduledAt(taskId: String): Long? {
        val value = prefs.getLong(key(taskId), Long.MIN_VALUE)
        return value.takeUnless { it == Long.MIN_VALUE }
    }

    fun clear(taskId: String) {
        prefs.edit().remove(key(taskId)).commit()
    }

    fun consumeIfCurrent(taskId: String, scheduledAtMillis: Long): Boolean {
        if (scheduledAtMillis <= 0L) return false
        val entryKey = key(taskId)
        val current = prefs.getLong(entryKey, Long.MIN_VALUE)
        if (current != scheduledAtMillis) return false
        prefs.edit().remove(entryKey).commit()
        return true
    }

    fun markDelivered(taskId: String, sourceReminderAtMillis: Long) {
        if (sourceReminderAtMillis <= 0L) return
        prefs.edit().putLong(deliveredKey(taskId), sourceReminderAtMillis).commit()
    }

    fun wasDelivered(taskId: String, sourceReminderAtMillis: Long): Boolean {
        if (sourceReminderAtMillis <= 0L) return false
        return prefs.getLong(deliveredKey(taskId), Long.MIN_VALUE) == sourceReminderAtMillis
    }

    fun clearDelivered(taskId: String) {
        prefs.edit().remove(deliveredKey(taskId)).commit()
    }

    private fun key(taskId: String): String = "alarm_$taskId"
    private fun deliveredKey(taskId: String): String = "delivered_$taskId"
}
