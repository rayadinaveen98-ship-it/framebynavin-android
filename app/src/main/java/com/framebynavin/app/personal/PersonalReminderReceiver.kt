package com.framebynavin.app.personal

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

/**
 * Only the Personal domain is consulted. Creator reminder receiver and reward/
 * stage logic cannot be reached through this delivery path.
 */
internal class PersonalReminderReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != PersonalDeliveryContract.ACTION_FIRE) return
        val id = intent.getStringExtra(PersonalDeliveryContract.EXTRA_PERSONAL_ID).orEmpty()
        val occurrence = intent.getLongExtra(PersonalDeliveryContract.EXTRA_OCCURRENCE, 0L)
        val fireAt = intent.getLongExtra(PersonalDeliveryContract.EXTRA_FIRE_AT, 0L)
        val token = intent.getStringExtra(PersonalDeliveryContract.EXTRA_TOKEN).orEmpty()
        if (id.isBlank() || occurrence <= 0L || fireAt <= 0L || token.isBlank()) return
        val pending = goAsync()
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val app = context.applicationContext
                val delivery = PersonalDeliveryCoordinator(app)
                val reminder = delivery.currentLease(id, occurrence, fireAt, token)
                if (reminder != null && PersonalReminderNotifications.show(app, reminder)) {
                    if (delivery.acknowledgePosted(id, occurrence, fireAt, token) &&
                        reminder.alert != PersonalAlert.NOTIFICATION) {
                        PersonalPlaybackService.tryStart(
                            app, reminder.copy(presentedToken = token),
                        )
                    }
                }
            } catch (failure: Exception) {
                android.util.Log.e("BacklotPersonal", "Personal delivery handoff failed", failure)
            } finally {
                pending.finish()
            }
        }
    }
}

internal class PersonalReminderActionReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val action = intent.action ?: return
        if (action !in setOf(
                PersonalDeliveryContract.ACTION_DONE,
                PersonalDeliveryContract.ACTION_SNOOZE,
                PersonalDeliveryContract.ACTION_DISMISS,
                PersonalDeliveryContract.ACTION_RESCHEDULE,
            )) return
        val id = intent.getStringExtra(PersonalDeliveryContract.EXTRA_PERSONAL_ID).orEmpty()
        val occurrence = intent.getLongExtra(PersonalDeliveryContract.EXTRA_OCCURRENCE, 0L)
        val token = intent.getStringExtra(PersonalDeliveryContract.EXTRA_TOKEN).orEmpty()
        if (id.isBlank() || token.isBlank()) return
        val pending = goAsync()
        CoroutineScope(Dispatchers.IO).launch {
            try {
                PersonalDeliveryCoordinator(context.applicationContext).apply(
                    id = id,
                    originalOccurrence = occurrence,
                    token = token,
                    action = action,
                    rescheduleAtMillis = intent.getLongExtra(
                        PersonalDeliveryContract.EXTRA_RESCHEDULE_AT, 0L,
                    ),
                )
            } catch (failure: Exception) {
                android.util.Log.e("BacklotPersonal", "Personal action could not be committed", failure)
            } finally {
                pending.finish()
            }
        }
    }
}
