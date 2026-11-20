package com.framebynavin.app.personal

import android.animation.ValueAnimator
import android.os.PowerManager
import androidx.activity.ComponentActivity
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import com.framebynavin.app.reminders.RX01ReminderDomain
import com.framebynavin.app.reminders.RX01ReminderExperienceGate
import com.framebynavin.app.reminders.RX01ReminderIconKind
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * RX01-D: non-exported Personal visual composition reached only through the
 * original PersonalAlertActivity, after its pending token reaches READY and
 * ONLY when keyguard is not showing. Does not grant Voice full-screen access.
 *
 * Every transition requests a fresh canonical store read on IO. There is no
 * hidden completion, extra activity, independent ringtone or second writer.
 */
@Composable
internal fun RX01PersonalLiveAlert(
    activity: ComponentActivity,
    reminder: PersonalReminder,
    onInvalidOccurrence: () -> Unit,
    mainContent: @Composable () -> Unit,
) {
    // The caller excludes NOTIFICATION and the locked/loading/invalid states.
    val mode = when (reminder.alert) {
        PersonalAlert.VOICE -> RX01ReminderIconKind.VOICE
        PersonalAlert.ALARM -> RX01ReminderIconKind.ALARM
        PersonalAlert.NOTIFICATION -> error("Notification-only Personal items use original UI")
    }
    val id = reminder.id
    val token = reminder.pendingToken
    val originalAt = reminder.pendingOccurrenceMillis
    val originalAlert = reminder.alert
    val motionEnabled = remember { ValueAnimator.areAnimatorsEnabled() }
    val lowPower = remember(activity) {
        activity.getSystemService(PowerManager::class.java)?.isPowerSaveMode == true
    }
    RX01ReminderExperienceGate(
        domain = RX01ReminderDomain.PERSONAL,
        mode = mode,
        occurrenceKey = "personal:$id:$token",
        occurrenceIsActive = id.isNotBlank() && token.isNotBlank() &&
            !activity.isFinishing && !activity.isDestroyed,
        validateOnOpen = {
            withContext(Dispatchers.IO) {
                val live = runCatching {
                    PersonalReminderStore(activity.applicationContext).load()
                        .firstOrNull { it.id == id }
                }.getOrNull()
                !activity.isFinishing && !activity.isDestroyed &&
                    live != null &&
                    live.alert == originalAlert &&
                    live.pendingOccurrenceMillis == originalAt &&
                    PersonalAlertPresentationPolicy.state(live, id, token) ==
                        PersonalAlertPresentationState.READY
            }
        },
        motionEnabled = motionEnabled,
        lowPower = lowPower,
        onInvalidOccurrence = onInvalidOccurrence,
        mainContent = mainContent,
    )
}
