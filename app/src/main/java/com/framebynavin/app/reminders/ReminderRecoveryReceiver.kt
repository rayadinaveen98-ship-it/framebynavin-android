package com.framebynavin.app.reminders

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class ReminderRecoveryReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action !in SUPPORTED_ACTIONS) return

        val pendingResult = goAsync()
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val appContext = context.applicationContext
                ReminderHealthScheduler.ensurePeriodic(appContext)
                ReminderRecoveryEngine.reconcile(appContext)
                IdeaReminderScheduler(appContext).reconcile()
                // Personal data is isolated; a Personal failure must not block
                // creator reminder recovery.
                runCatching {
                    com.framebynavin.app.personal.PersonalDeliveryCoordinator(appContext).reconcile()
                }.onFailure {
                    android.util.Log.e("BacklotPersonal", "Personal recovery reconcile failed", it)
                }
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
