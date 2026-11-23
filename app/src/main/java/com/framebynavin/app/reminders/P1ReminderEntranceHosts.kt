package com.framebynavin.app.reminders

import android.animation.ValueAnimator
import android.app.KeyguardManager
import android.content.Context
import android.os.PowerManager
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.onClick
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.framebynavin.app.personal.PersonalAlert
import com.framebynavin.app.personal.PersonalReminder
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

/** Compose the unchanged Personal alert only after the gallery entrance is dismissed. */
@Composable
internal fun P1PersonalGalleryEntrance(
    occurrenceKey: String,
    reminder: PersonalReminder?,
    invalid: Boolean,
    locked: Boolean,
    mainContent: @Composable () -> Unit,
) {
    val context = LocalContext.current
    val media = remember(occurrenceKey) {
        P1EntranceMediaStore.selected(context, P1ReminderScope.PERSONAL)
    }
    var revealed by remember(occurrenceKey) { mutableStateOf(false) }
    val current = reminder != null && !invalid && !reminder.paused &&
        reminder.pendingToken.isNotBlank() && reminder.presentedToken == reminder.pendingToken
    if (P1GalleryGatePolicy.shouldOffer(media != null, !locked, current, revealed)) {
        val live = requireNotNull(reminder)
        P1GalleryEntranceVisual(
            domain = P1ReminderScope.PERSONAL,
            title = live.title,
            kind = if (live.alert == PersonalAlert.VOICE) ReminderCheckInKind.VOICE
                else ReminderCheckInKind.ALARM,
            occurrenceKey = occurrenceKey,
            media = requireNotNull(media),
            onDoubleTap = { revealed = true },
        )
    } else mainContent()
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

/** This surface has no Done/Snooze/Dismiss controls, receipts, service or alarm ownership. */
@Composable
private fun P1GalleryEntranceVisual(
    domain: P1ReminderScope,
    title: String,
    kind: ReminderCheckInKind,
    occurrenceKey: String,
    media: P1Media,
    onDoubleTap: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val motion = remember { ValueAnimator.areAnimatorsEnabled() }
    val lowPower = remember(context) {
        context.getSystemService(PowerManager::class.java)?.isPowerSaveMode == true
    }
    Box(modifier.fillMaxSize().background(Color(0xFF080408))
        .testTag("p1-gallery-entrance-" + domain.name.lowercase())) {
        P1EntranceBackdrop(
            domain = domain, motionEnabled = motion, lowPower = lowPower,
            modifier = Modifier.fillMaxSize(), overrideMedia = media,
        )
        Column(
            Modifier.fillMaxSize().systemBarsPadding().padding(horizontal = 24.dp, vertical = 28.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceBetween,
        ) {
            Text("BACKLOT / " + when (domain) {
                P1ReminderScope.PERSONAL -> "PERSONAL REMINDER"
                P1ReminderScope.CREATOR -> "CREATOR REMINDER"
                P1ReminderScope.CHALLENGE_21 -> "21-DAY CHALLENGE"
            }, color = Color.White, fontSize = 11.sp,
                fontWeight = FontWeight.SemiBold, letterSpacing = 1.sp)
            Column(horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(12.dp)) {
                V336DirectorSignalArt(kind = kind, motionEnabled = motion,
                    lowPower = lowPower,
                    modifier = Modifier.size(width = 220.dp, height = 170.dp))
                Text(title, color = Color.White, fontSize = 20.sp,
                    fontWeight = FontWeight.Bold, maxLines = 3,
                    textAlign = TextAlign.Center)
                Text("DOUBLE TAP TO OPEN", color = Color.White, fontSize = 12.sp)
            }
            Spacer(Modifier.height(24.dp))
        }
        Box(
            Modifier.fillMaxSize().testTag("p1-entrance-double-tap-" + domain.name.lowercase())
                .pointerInput(occurrenceKey) {
                    detectTapGestures(onDoubleTap = { onDoubleTap() })
                }
                .semantics {
                    role = Role.Button
                    contentDescription = "Open Backlot reminder"
                    onClick(label = "Open reminder") { onDoubleTap(); true }
                }
        )
    }
}
