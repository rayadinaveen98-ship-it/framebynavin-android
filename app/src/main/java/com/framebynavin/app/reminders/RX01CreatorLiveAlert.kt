package com.framebynavin.app.reminders

import androidx.activity.ComponentActivity
import androidx.compose.runtime.Composable
import com.framebynavin.app.data.TaskStore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * RX01-D isolated visual host for the already-authorized Creator Activity.
 * Does not start a foreground service, AlarmManager, notification, new
 * Activity or reminder receipt. The existing onCreate, onResume, receiver,
 * watchdog and action closures in the original Activity remain the owners.
 *
 * The same task+occurrence token is verified by reloading TaskStore on IO
 * at the real double-tap and AGAIN before any MAIN control is composed.
 */
@Composable
internal fun RX01CreatorLiveAlert(
    activity: ComponentActivity,
    taskId: String,
    occurrenceId: String,
    kind: ReminderCheckInKind,
    mainContent: @Composable () -> Unit,
) {
    val visual = RX01CurrentReminderMotion()
    RX01ReminderExperienceGate(
        domain = RX01ReminderDomain.CREATOR,
        mode = RX01ReminderIconPolicy.creator(kind),
        occurrenceKey = "creator:${kind.name}:$taskId:$occurrenceId",
        occurrenceIsActive = taskId.isNotBlank() &&
            occurrenceId.isNotBlank() &&
            !activity.isFinishing && !activity.isDestroyed,
        validateOnOpen = {
            withContext(Dispatchers.IO) {
                val live = runCatching {
                    TaskStore(activity.applicationContext).load()
                        .firstOrNull { it.id == taskId }
                }.getOrNull()
                live != null &&
                    !activity.isFinishing && !activity.isDestroyed &&
                    ReminderOccurrenceStore(activity.applicationContext)
                        .matches(live, occurrenceId)
            }
        },
        motionEnabled = visual.motionEnabled,
        lowPower = visual.lowPower,
        onInvalidOccurrence = {
            if (!activity.isFinishing && !activity.isDestroyed) {
                activity.finishAndRemoveTask()
            }
        },
        mainContent = mainContent,
    )
}
