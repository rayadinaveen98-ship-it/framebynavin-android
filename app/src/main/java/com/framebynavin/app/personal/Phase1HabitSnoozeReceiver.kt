package com.framebynavin.app.personal

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import java.time.Instant
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeout

/**
 * The owner's explicit 10-minute snooze from the ORIGINAL notification.
 * This non-exported PendingIntent target verifies the exact currently scheduled
 * challenge occurrence before mutating the existing challenge DataStore.
 * No Done receipt, extra reminder, or alternative scheduler is introduced.
 */
internal class Phase1HabitSnoozeReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent?) {
        if (intent?.action != Phase1HabitNotification.ACTION_SNOOZE) return
        val id = intent.getStringExtra(Phase1HabitNotification.ID)
            ?.takeIf { it.isNotBlank() } ?: return
        val at = intent.getLongExtra(Phase1HabitNotification.AT, 0L)
        val minutes = intent.getIntExtra(Phase1HabitNotification.SNOOZE_MINUTES, 0)
        if (at <= 0L || minutes != 10) return
        val pending = goAsync()
        CoroutineScope(Dispatchers.IO).launch {
            try {
                withTimeout(9_000L) {
                    val app = context.applicationContext
                    val existing = Phase1HabitStore(app).load().firstOrNull { it.id == id }
                        ?: return@withTimeout
                    val now = Instant.now()
                    check(Phase1HabitOccurrencePolicy.current(
                        existing, at, now.toEpochMilli(),
                    )) { "Stale challenge notification; snooze refused" }
                    // DataStore revalidates active day/Done receipts under mutation lock.
                    Phase1HabitStore(app).snoozeToday(id, now, minutes)
                    Phase1HabitVoiceService.stopMatching(id, at)
                    // Commit first, then reconcile the Android alarm and dismiss
                    // only this occurrence's posted notification.
                    Phase1HabitAlarmScheduler(app).reconcile()
                    Phase1HabitNotification.cancelPosted(app, existing, at)
                    Phase1HabitDeliveryLedger.recordSafely(app, id, at,
                        Phase1HabitDeliveryStage.SNOOZE_ACCEPTED)
                }
            } catch (failure: Exception) {
                Phase1HabitDeliveryLedger.recordSafely(context.applicationContext, id, at,
                    Phase1HabitDeliveryStage.SNOOZE_FAILED)
                Log.w("BacklotHabit", "Snooze needs attention; existing Done receipts intact",
                    failure)
            } finally {
                pending.finish()
            }
        }
    }
}
