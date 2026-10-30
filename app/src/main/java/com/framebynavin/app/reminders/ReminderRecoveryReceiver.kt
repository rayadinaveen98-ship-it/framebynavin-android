package com.framebynavin.app.reminders

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeout

/**
 * Boot / timezone / exact-permission recovery. Failed Creator recovery must not
 * starve Personal; a single lane's error does not crash this system receiver.
 */
class ReminderRecoveryReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action !in SUPPORTED_ACTIONS) return

        val pendingResult = goAsync()
        CoroutineScope(Dispatchers.IO).launch {
            try {
                withTimeout(9_000L) {
                    val app = context.applicationContext
                    runCatching { ReminderHealthScheduler.ensurePeriodic(app) }
                        .onFailure {
                            android.util.Log.w("BacklotReminderHealth", "Could not register periodic health", it)
                        }
                    val outcome = V225RecoveryLanes.run(
                        creator = { ReminderRecoveryEngine.reconcile(app) },
                        personal = { com.framebynavin.app.personal.PersonalDeliveryCoordinator(app).reconcile() },
                        ideas = { IdeaReminderScheduler(app).reconcile() },
                    )
                    if (!outcome.allSucceeded) {
                        android.util.Log.w(
                            "BacklotReminderHealth",
                            "Boot recovery partial failure: creator=${outcome.creatorRecovered}, " +
                                "personal=${outcome.personalRecovered}, ideas=${outcome.ideasRecovered}",
                        )
                    }
                }
            } catch (error: Exception) {
                android.util.Log.e(
                    "BacklotReminderHealth",
                    "Boot recovery interrupted; periodic health will retry when available",
                    error,
                )
            } finally {
                pendingResult.finish()
            }
        }
    }

    companion object {
        private val SUPPORTED_ACTIONS = setOf(
            Intent.ACTION_BOOT_COMPLETED,
            Intent.ACTION_MY_PACKAGE_REPLACED,
            Intent.ACTION_TIME_CHANGED,
            Intent.ACTION_TIMEZONE_CHANGED,
            "android.app.action.SCHEDULE_EXACT_ALARM_PERMISSION_STATE_CHANGED",
        )
    }
}
