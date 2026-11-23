package com.framebynavin.app.reminders

import android.app.KeyguardManager
import android.content.Context
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.framebynavin.app.personal.Phase1HabitAlertKind
import com.framebynavin.app.personal.Phase1HabitChallenge
import com.framebynavin.app.personal.Phase1HabitStore
import java.time.LocalDate
import java.time.ZoneId
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull

/** Pure presentation gate; actual reminder delivery is unchanged. */
internal object P1GalleryGatePolicy {
    fun shouldOffer(hasMedia: Boolean, unlocked: Boolean, current: Boolean, revealed: Boolean) =
        hasMedia && unlocked && current && !revealed

    fun challengeEligible(plan: Phase1HabitChallenge, today: LocalDate): Boolean =
        !plan.paused && !plan.archived &&
            (plan.alertKind == Phase1HabitAlertKind.ALARM ||
             plan.alertKind == Phase1HabitAlertKind.VOICE) &&
            !today.isBefore(plan.startDate) && !today.isAfter(plan.endDate)
}

/** MainActivity continues to handle original challenge PendingIntent and Personal navigation. */
@Composable
internal fun P1ChallengeNotificationEntrance(
    challengeId: String?,
    launchNonce: Long,
    modifier: Modifier = Modifier,
) {
    if (challengeId.isNullOrBlank()) return
    val context = LocalContext.current
    val owner = LocalLifecycleOwner.current
    // P10C: a valid user-opened challenge notification gets the new built-in
    // entrance even with no private gallery media selected.
    val media = remember(challengeId, launchNonce) {
        P1EntranceMediaStore.selected(context, P1ReminderScope.CHALLENGE_21)
    }

    var unlocked by remember(challengeId, launchNonce) { mutableStateOf(false) }
    DisposableEffect(context, owner, challengeId, launchNonce) {
        val keyguard = context.getSystemService(Context.KEYGUARD_SERVICE) as? KeyguardManager
        fun refresh() { unlocked = keyguard?.isKeyguardLocked == false }
        refresh()
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_START || event == Lifecycle.Event.ON_RESUME) refresh()
            if (event == Lifecycle.Event.ON_STOP) unlocked = false
        }
        owner.lifecycle.addObserver(observer)
        onDispose { owner.lifecycle.removeObserver(observer) }
    }
    var candidate by remember(challengeId, launchNonce) {
        mutableStateOf<Phase1HabitChallenge?>(null)
    }
    var validated by remember(challengeId, launchNonce) { mutableStateOf<Boolean?>(null) }
    var revealed by remember(challengeId, launchNonce) { mutableStateOf(false) }
    LaunchedEffect(challengeId, launchNonce) {
        val found = withContext(Dispatchers.IO) {
            withTimeoutOrNull(1_500L) {
                runCatching {
                    Phase1HabitStore(context.applicationContext).load()
                        .firstOrNull { it.id == challengeId }
                        ?.takeIf { plan ->
                            P1GalleryGatePolicy.challengeEligible(
                                plan, LocalDate.now(ZoneId.of(plan.zoneId)))
                        }
                }.getOrNull()
            }
        }
        candidate = found
        validated = found != null
    }
    if (revealed || !unlocked || validated == false) return
    if (validated == null) {
        Box(modifier.fillMaxSize().background(
            P10ReminderFamilyStore.selected(context, P1ReminderScope.CHALLENGE_21).pigments().dark
        ).testTag("p1-challenge-validating"))
        return
    }
    val active = candidate ?: return
    P10ReminderEntranceStage(
        scope = P1ReminderScope.CHALLENGE_21,
        title = active.activity,
        kind = if (active.alertKind == Phase1HabitAlertKind.VOICE)
            ReminderCheckInKind.VOICE else ReminderCheckInKind.ALARM,
        occurrenceKey = challengeId + ":" + launchNonce,
        locked = false,
        media = media,
        onOpen = { revealed = true },
        modifier = modifier,
    )
}

