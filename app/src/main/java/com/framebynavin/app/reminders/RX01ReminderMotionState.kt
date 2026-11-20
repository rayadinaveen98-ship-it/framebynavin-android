package com.framebynavin.app.reminders

import android.animation.ValueAnimator
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.PowerManager
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.ContextCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner

internal data class RX01ReminderMotionFlags(
    val motionEnabled: Boolean,
    val lowPower: Boolean,
)

/**
 * RX01-F: one lifecycle-aware and battery-aware motion source for all domains.
 * Suspended/stopped Activities MUST NOT keep painting animated fluid fields.
 *
 * Never affects notification delivery, voice/alarm service, OS wake policy,
 * Done/Snooze, alarm expiry or accessibility focus. Rendering may fall back
 * to a still, readable illustration without removing the double-tap gate.
 */
@Composable
internal fun RX01CurrentReminderMotion(): RX01ReminderMotionFlags {
    val context = LocalContext.current.applicationContext
    val lifecycle = LocalLifecycleOwner.current.lifecycle
    fun powerSave() = context.getSystemService(PowerManager::class.java)
        ?.isPowerSaveMode == true

    var inForeground by remember(lifecycle) {
        mutableStateOf(lifecycle.currentState.isAtLeast(Lifecycle.State.STARTED))
    }
    var animatorsOn by remember { mutableStateOf(ValueAnimator.areAnimatorsEnabled()) }
    var batterySaver by remember(context) { mutableStateOf(powerSave()) }

    DisposableEffect(lifecycle, context) {
        val observer = LifecycleEventObserver { _, _ ->
            inForeground = lifecycle.currentState.isAtLeast(Lifecycle.State.STARTED)
            if (inForeground) {
                animatorsOn = ValueAnimator.areAnimatorsEnabled()
                batterySaver = powerSave()
            }
        }
        lifecycle.addObserver(observer)
        val receiver = object : BroadcastReceiver() {
            override fun onReceive(context: Context?, intent: Intent?) {
                if (intent?.action == PowerManager.ACTION_POWER_SAVE_MODE_CHANGED) {
                    batterySaver = powerSave()
                }
            }
        }
        // Dynamically registered receiver only. Use non-exported registration;
        // no manifest receiver, permission or process-lifetime work is added.
        val registered = runCatching {
            ContextCompat.registerReceiver(
                context,
                receiver,
                IntentFilter(PowerManager.ACTION_POWER_SAVE_MODE_CHANGED),
                ContextCompat.RECEIVER_NOT_EXPORTED,
            )
        }.isSuccess
        onDispose {
            lifecycle.removeObserver(observer)
            if (registered) runCatching { context.unregisterReceiver(receiver) }
        }
    }
    return RX01ReminderMotionFlags(
        motionEnabled = inForeground && animatorsOn && !batterySaver,
        lowPower = batterySaver || !inForeground,
    )
}
