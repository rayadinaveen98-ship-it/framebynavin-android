package com.framebynavin.app.reminders

import android.content.Context

/**
 * Tiny synchronous ledger for the alarm that is currently authoritative per task.
 * This is intentionally separate from DataStore so a cold BroadcastReceiver can
 * reject stale/duplicate alarms without delaying the user-facing notification.
 */
class AlarmLedger(context: Context) {
    private val prefs = context.getSharedPreferences("framebynavin_alarm_ledger_v04", Context.MODE_PRIVATE)

    fun markScheduled(taskId: String, scheduledAtMillis: Long) {
        prefs.edit().putLong(key(taskId), scheduledAtMillis).commit()
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

    private fun key(taskId: String): String = "alarm_$taskId"
}
