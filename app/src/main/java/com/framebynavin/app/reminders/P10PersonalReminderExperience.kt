package com.framebynavin.app.reminders

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import com.framebynavin.app.personal.PersonalAlert
import com.framebynavin.app.personal.PersonalReminder

/**
 * P10D: single stable visual compositor across Personal entrance and main.
 * PersonalAlertActivity still owns posted-occurrence validation, keyguard
 * privacy, exact Done/Snooze/Dismiss receipt and activity finish.
 */
@Composable
internal fun P10PersonalReminderExperience(
    occurrenceKey: String,
    reminder: PersonalReminder?,
    invalid: Boolean,
    locked: Boolean,
    mainContent: @Composable (backgroundProvided: Boolean) -> Unit,
) {
    val context = LocalContext.current
    val media = remember(context, occurrenceKey) {
        P1EntranceMediaStore.selected(context, P1ReminderScope.PERSONAL)
    }
    val family = remember(context, occurrenceKey) {
        P10ReminderFamilyStore.selected(context, P1ReminderScope.PERSONAL)
    }
    val source = remember(context, occurrenceKey, media?.file?.absolutePath) {
        P10ReminderBackgroundStore.selected(
            context, P1ReminderScope.PERSONAL, galleryAvailable = media != null)
    }
    var revealed by remember(occurrenceKey) { mutableStateOf(false) }
    val ready = reminder != null && !invalid && !reminder.paused &&
        reminder.pendingToken.isNotBlank() &&
        reminder.pendingToken == reminder.presentedToken

    if (!ready) {
        // The original invalid/loading/locked safe-state screen keeps its own backdrop.
        mainContent(false)
        return
    }

    val current = requireNotNull(reminder)
    Box(Modifier.fillMaxSize()) {
        // The image/video player is composed exactly once for this occurrence.
        // No media restart on double-tap, and the same family underlies both acts.
        P10ReminderSceneBackground(
            scope = P1ReminderScope.PERSONAL,
            family = family, source = source,
            act = if (revealed) P10ReminderAct.MAIN else P10ReminderAct.ENTRANCE,
            media = media, locked = locked,
            modifier = Modifier.fillMaxSize(),
        )
        if (!revealed) {
            P10ReminderEntranceStage(
                scope = P1ReminderScope.PERSONAL,
                kind = if (current.alert == PersonalAlert.VOICE)
                    ReminderCheckInKind.VOICE else ReminderCheckInKind.ALARM,
                title = current.title,
                occurrenceKey = occurrenceKey,
                locked = locked,
                media = null,
                onOpen = { if (!locked) revealed = true },
                modifier = Modifier.fillMaxSize(),
                renderBackground = false,
            )
        } else {
            mainContent(true)
        }
    }
}
