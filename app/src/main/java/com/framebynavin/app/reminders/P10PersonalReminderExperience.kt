package com.framebynavin.app.reminders

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import com.framebynavin.app.personal.PersonalAlert
import com.framebynavin.app.personal.PersonalReminder

/**
 * P10C visual-only integration for a valid Personal alert.
 *
 * The PersonalAlertActivity remains sole owner of posted occurrence validation,
 * lockscreen behavior, live Done/Snooze/Dismiss, audio stop and activity finish.
 * We do not create an additional full-screen intent or playback service.
 */
@Composable
internal fun P10PersonalReminderExperience(
    occurrenceKey: String,
    reminder: PersonalReminder?,
    invalid: Boolean,
    locked: Boolean,
    mainContent: @Composable () -> Unit,
) {
    val context = LocalContext.current
    val media = remember(context, occurrenceKey) {
        P1EntranceMediaStore.selected(context, P1ReminderScope.PERSONAL)
    }
    var revealed by remember(occurrenceKey) { mutableStateOf(false) }
    val ready = reminder != null && !invalid && !reminder.paused &&
        reminder.pendingToken.isNotBlank() &&
        reminder.pendingToken == reminder.presentedToken

    if (ready && !revealed) {
        val current = requireNotNull(reminder)
        P10ReminderEntranceStage(
            scope = P1ReminderScope.PERSONAL,
            kind = if (current.alert == PersonalAlert.VOICE)
                ReminderCheckInKind.VOICE else ReminderCheckInKind.ALARM,
            title = current.title,
            occurrenceKey = occurrenceKey,
            locked = locked,
            media = media,
            onOpen = { if (!locked) revealed = true },
        )
    } else {
        // Main screen has its own privacy gate and original exact response callbacks.
        mainContent()
    }
}
