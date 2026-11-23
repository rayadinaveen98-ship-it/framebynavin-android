package com.framebynavin.app.reminders

import android.animation.ValueAnimator
import android.app.KeyguardManager
import android.content.Context
import android.os.PowerManager
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.Crossfade
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Check
import androidx.compose.material.icons.outlined.PauseCircle
import androidx.compose.material.icons.outlined.Replay
import androidx.compose.material.icons.outlined.Schedule
import androidx.compose.material.icons.outlined.Snooze
import androidx.compose.material.icons.outlined.Work
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.framebynavin.app.data.ProjectAttentionPlan
import com.framebynavin.app.ui.theme.BacklotBorder
import com.framebynavin.app.ui.theme.BacklotSuccess
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

enum class ReminderCheckInKind { ALARM, VOICE }

/** Shared RC26 reminder surface: one hierarchy and one appearance system for alarm + voice. */
@Composable
internal fun ReminderCheckInScreen(
    kind: ReminderCheckInKind,
    title: String,
    dueLabel: String,
    stageLabel: String,
    suggestedAction: String,
    notes: String,
    snoozeMinutes: Int,
    stageCheckIn: Boolean,
    // Lab UX06A Smart-stage reminder semantics stay intact.
    smartStageSnooze: Boolean = false,
    onStageDone: () -> Unit,
    onWorking: () -> Unit,
    onSnooze: () -> Unit,
    onPause: () -> Unit,
    onReschedule: () -> Unit,
    onDismiss: () -> Unit,
    onReplay: (() -> Unit)? = null,
    attentionPlan: ProjectAttentionPlan = ProjectAttentionPlan.CUSTOM,
) {
    BackHandler(enabled = true) { }
    val context = LocalContext.current
    val family = remember(context) {
        P10ReminderFamilyStore.selected(context, P1ReminderScope.CREATOR)
    }
    val look = family.actionPalette()
    val accent = look.hot
    val scroll = rememberScrollState()
    val clockLabel = remember { reminderClockLabel(System.currentTimeMillis()) }
    val contextLine = listOf(stageLabel.trim(), dueLabel.trim())
        .filter { it.isNotBlank() }
        .joinToString(" · ")
    val nextAction = V200ReminderLayoutPolicy.supportingAction(suggestedAction, title)
    val snoozeLabel = if (smartStageSnooze) "REMIND IN ${snoozeMinutes}m"
        else "SNOOZE ${snoozeMinutes} MIN"
    val smartSnoozeHint =
        if (smartStageSnooze) "Repeats this Smart stage; your final alert time stays unchanged."
        else null
    val screen = LocalConfiguration.current
    val compact = V200ReminderLayoutPolicy.isCompact(screen.screenHeightDp)
    val wideActions = V200ReminderLayoutPolicy.useHorizontalActions(screen.screenWidthDp)
    var moreOpen by rememberSaveable { mutableStateOf(false) }
    val primaryPress = remember { MutableInteractionSource() }
    val workingPress = remember { MutableInteractionSource() }
    val snoozePress = remember { MutableInteractionSource() }
    val morePress = remember { MutableInteractionSource() }
    val pausePress = remember { MutableInteractionSource() }
    val reschedulePress = remember { MutableInteractionSource() }
    val replayPress = remember { MutableInteractionSource() }
    val dismissPress = remember { MutableInteractionSource() }

    // Screen controls always work: animation scale zero means immediate reveal.
    // Battery saver reduces the number of dots rather than affecting actions.
    val motionEnabled = remember { ValueAnimator.areAnimatorsEnabled() }
    val lowPower = remember(context) {
        context.getSystemService(PowerManager::class.java)?.isPowerSaveMode == true
    }
    // P10C: existing Creator response callbacks remain authoritative.
    // Touch must double-tap to open Act II; TalkBack offers an explicit action.
    // Preserve V337 Creator's over-keyguard response availability; custom
    // gallery imagery remains locked behind the existing P1 privacy gate.
    val owner = LocalLifecycleOwner.current
    var keyguardLocked by remember { mutableStateOf(true) }
    DisposableEffect(owner, context) {
        val keyguard = context.getSystemService(Context.KEYGUARD_SERVICE) as? KeyguardManager
        fun refresh() { keyguardLocked = keyguard?.isKeyguardLocked == true }
        refresh()
        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_START, Lifecycle.Event.ON_RESUME -> refresh()
                Lifecycle.Event.ON_STOP -> keyguardLocked = true
                else -> Unit
            }
        }
        owner.lifecycle.addObserver(observer)
        onDispose { owner.lifecycle.removeObserver(observer) }
    }
    val galleryMedia = remember(context) {
        P1EntranceMediaStore.selected(context, P1ReminderScope.CREATOR)
    }
    val backgroundSource = remember(context, galleryMedia?.file?.absolutePath) {
        P10ReminderBackgroundStore.selected(context, P1ReminderScope.CREATOR,
            galleryAvailable = galleryMedia != null)
    }
    var revealed by rememberSaveable(kind, title, dueLabel) {
        mutableStateOf(P10ReminderVisualPolicy.startRevealed(motionEnabled))
    }
    LaunchedEffect(motionEnabled) {
        if (!motionEnabled) revealed = true
    }
    val showMain = revealed
    val revealAlpha by animateFloatAsState(
        targetValue = if (showMain) 1f else 0f,
        animationSpec = tween(durationMillis = if (motionEnabled) 650 else 0),
        label = "p10-reminder-controls",
    )

    Box(
        Modifier
            .fillMaxSize()
            .background(look.black),
    ) {
        // P10D: one living/gallery compositor persists across both acts.
        // A single Media3 player survives the entrance -> main crossfade.
        P10ReminderSceneBackground(
            scope = P1ReminderScope.CREATOR,
            family = family, source = backgroundSource,
            act = if (showMain) P10ReminderAct.MAIN else P10ReminderAct.ENTRANCE,
            media = galleryMedia, locked = keyguardLocked,
            modifier = Modifier.fillMaxSize(),
        )
        Crossfade(
            targetState = showMain,
            animationSpec = tween(durationMillis = if (motionEnabled) 500 else 0),
            label = "p10-reminder-act-transition",
        ) { showingActions ->
            if (!showingActions) {
                P10ReminderEntranceStage(
                    scope = P1ReminderScope.CREATOR,
                    kind = kind,
                    title = title,
                    occurrenceKey = title + ":" + dueLabel + ":" + stageLabel,
                    // Preserve V337 Creator response access over keyguard.
                    // Parent media compositor separately withholds private gallery media.
                    locked = false,
                    media = null,
                    onOpen = { revealed = true },
                    renderBackground = false,
                    modifier = Modifier.fillMaxSize(),
                )
            }
        }
        // Only render actionable controls after manual reveal; no ghost actions under the intro.
        if (showMain) Column(
            modifier = Modifier
                .align(Alignment.Center)
                .testTag("v200-reminder-controls")
                .widthIn(max = 440.dp)
                .fillMaxWidth()
                .statusBarsPadding()
                .navigationBarsPadding()
                .verticalScroll(scroll)
                .padding(horizontal = 22.dp, vertical = if (compact) 12.dp else 22.dp)
                .graphicsLayer { alpha = revealAlpha },
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    "BACKLOT  /  CREATOR REMINDERS",
                    color = look.ivory,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.5.sp,
                )
                Text(
                    clockLabel, color = look.muted,
                    fontSize = 12.sp, fontWeight = FontWeight.SemiBold,
                )
            }
            Spacer(Modifier.height(8.dp))
            Text(
                text = if (kind == ReminderCheckInKind.VOICE) "VOICE" else "ALARM",
                color = look.gold, fontWeight = FontWeight.Bold,
                fontSize = 10.sp, letterSpacing = 3.sp,
            )
            // P10: new authored Voice/Alarm mark, no old dotted signal artwork.
            Box(
                modifier = Modifier.fillMaxWidth()
                    .height(if (compact) 170.dp else 220.dp)
                    .testTag(if (kind == ReminderCheckInKind.VOICE)
                        "v195-voice-guide" else "v195-alarm-guide"),
                contentAlignment = Alignment.Center,
            ) {
                P10ReminderIdentityEmblem(
                    kind = kind,
                    family = family,
                    modifier = Modifier.size(if (compact) 162.dp else 196.dp),
                )
                // Preserve Lab UX06E selected-companion moment, decorative only.
                V407ReminderCompanionMoment(
                    occurrenceKey = "creator-${kind.name}-$title-$dueLabel-$stageLabel",
                    modifier = Modifier.align(Alignment.BottomEnd)
                        .padding(end = 7.dp, bottom = 7.dp),
                )
            }
            Spacer(Modifier.height(if (compact) 3.dp else 7.dp))
            Text(
                if (stageCheckIn) "CREATOR STAGE CHECK-IN" else "CREATOR REMINDER",
                color = look.hot, fontWeight = FontWeight.Bold,
                fontSize = 10.sp, letterSpacing = 2.3.sp,
            )
            Spacer(Modifier.height(7.dp))
            Text(
                text = title,
                color = look.ivory,
                fontSize = if (compact) 22.sp else 26.sp,
                lineHeight = if (compact) 28.sp else 34.sp,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center,
                maxLines = 3,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.fillMaxWidth().testTag("v200-reminder-title"),
            )
            if (contextLine.isNotBlank() || nextAction != null) {
                Spacer(Modifier.height(12.dp))
                // One editorial stage panel instead of two stacked V336-era cards.
                Surface(
                    modifier = Modifier.fillMaxWidth().testTag("p10-creator-context-stage"),
                    color = look.panel,
                    shape = RoundedCornerShape(24.dp),
                    border = BorderStroke(1.dp, look.line),
                ) {
                    Column(Modifier.padding(horizontal = 19.dp, vertical = 16.dp)) {
                        if (contextLine.isNotBlank()) {
                            Column(Modifier.testTag("v336-creator-reminder-context")) {
                                Text("ON YOUR SCHEDULE", color = look.hot,
                                    fontSize = 10.sp, fontWeight = FontWeight.Bold,
                                    letterSpacing = 1.5.sp)
                                Spacer(Modifier.height(6.dp))
                                Text(contextLine, color = look.ivory,
                                    fontSize = 13.sp, lineHeight = 19.sp,
                                    maxLines = 3, overflow = TextOverflow.Ellipsis)
                            }
                        }
                        if (nextAction != null) {
                            if (contextLine.isNotBlank()) {
                                Spacer(Modifier.height(13.dp))
                                Box(Modifier.fillMaxWidth().height(1.dp)
                                    .background(look.line.copy(alpha = .55f)))
                                Spacer(Modifier.height(13.dp))
                            }
                            Column(Modifier.testTag("v336-creator-reminder-next-step")) {
                                Text("NEXT MOVE", color = look.hot,
                                    fontSize = 10.sp, fontWeight = FontWeight.Bold,
                                    letterSpacing = 1.5.sp)
                                Spacer(Modifier.height(6.dp))
                                Text(nextAction, color = look.ivory,
                                    fontSize = 13.sp, lineHeight = 19.sp,
                                    maxLines = 4, overflow = TextOverflow.Ellipsis)
                            }
                        }
                    }
                }
            }
            Spacer(Modifier.height(if (compact) 13.dp else 22.dp))

            // ONE dominant answer; Stage Done takes priority only for check-ins.
            Button(
                onClick = if (stageCheckIn) onStageDone else onWorking,
                interactionSource = primaryPress,
                modifier = Modifier.fillMaxWidth().height(54.dp)
                    .v201PressFeedback(primaryPress, motionEnabled, prominent = true)
                    .testTag("v200-primary-reminder-action"),
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (stageCheckIn)
                        look.gold else Color(0xFFE8B88E),
                    contentColor = look.black,
                ),
                shape = RoundedCornerShape(26.dp),
            ) {
                Icon(
                    if (stageCheckIn) Icons.Outlined.Check else Icons.Outlined.Work,
                    contentDescription = null,
                    modifier = Modifier.size(20.dp),
                )
                Spacer(Modifier.size(9.dp))
                Text(
                    if (stageCheckIn) "STEP DONE" else "I'M WORKING",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                )
            }
            if (stageCheckIn) {
                Spacer(Modifier.height(9.dp))
                OutlinedButton(
                    onClick = onWorking,
                    interactionSource = workingPress,
                    modifier = Modifier.fillMaxWidth().height(50.dp)
                        .v201PressFeedback(workingPress, motionEnabled),
                    border = BorderStroke(1.dp, accent.copy(alpha = .72f)),
                    shape = RoundedCornerShape(24.dp),
                ) {
                    Icon(Icons.Outlined.Work, null, tint = accent, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.size(8.dp))
                    Text("I'M WORKING", color = look.ivory,
                        fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                }
            }

            Spacer(Modifier.height(9.dp))
            if (wideActions) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    OutlinedButton(
                        onClick = onSnooze,
                        interactionSource = snoozePress,
                        modifier = Modifier.weight(1f).height(50.dp)
                            .v201PressFeedback(snoozePress, motionEnabled),
                        border = BorderStroke(1.dp, look.gold.copy(alpha = .64f)),
                        shape = RoundedCornerShape(24.dp),
                    ) {
                        Icon(Icons.Outlined.Snooze, null, tint = look.ivory,
                            modifier = Modifier.size(18.dp))
                        Spacer(Modifier.size(7.dp))
                        Text(snoozeLabel, color = look.ivory,
                            fontSize = 12.sp, maxLines = 1, fontWeight = FontWeight.SemiBold)
                    }
                    TextButton(
                        onClick = { moreOpen = !moreOpen },
                        interactionSource = morePress,
                        modifier = Modifier.height(50.dp)
                            .v201PressFeedback(morePress, motionEnabled),
                    ) {
                        Text(if (moreOpen) "HIDE OPTIONS" else "MORE OPTIONS",
                            color = look.ivory, fontSize = 11.sp,
                            fontWeight = FontWeight.SemiBold, maxLines = 1)
                    }
                }
            } else {
                OutlinedButton(
                    onClick = onSnooze,
                    interactionSource = snoozePress,
                    modifier = Modifier.fillMaxWidth().height(50.dp)
                        .v201PressFeedback(snoozePress, motionEnabled),
                    border = BorderStroke(1.dp, look.gold.copy(alpha = .64f)),
                    shape = RoundedCornerShape(24.dp),
                ) {
                    Icon(Icons.Outlined.Snooze, null, tint = look.ivory,
                        modifier = Modifier.size(18.dp))
                    Spacer(Modifier.size(8.dp))
                    Text(snoozeLabel, color = look.ivory,
                        fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                }
                TextButton(
                    onClick = { moreOpen = !moreOpen },
                    interactionSource = morePress,
                    modifier = Modifier.fillMaxWidth().height(48.dp)
                        .v201PressFeedback(morePress, motionEnabled),
                ) {
                    Text(if (moreOpen) "HIDE OPTIONS" else "MORE OPTIONS",
                        color = look.ivory, fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold)
                }
            }

            if (smartSnoozeHint != null) {
                Spacer(Modifier.height(7.dp))
                Text(smartSnoozeHint, color = look.muted, fontSize = 11.sp,
                    lineHeight = 15.sp, textAlign = TextAlign.Center)
            }

            AnimatedVisibility(
                visible = moreOpen,
                enter = expandVertically(
                    animationSpec = tween(if (motionEnabled) V201ReminderMotionPolicy.OPTIONS_ENTER_MILLIS else 0),
                    expandFrom = Alignment.Top,
                ) + fadeIn(animationSpec = tween(if (motionEnabled) V201ReminderMotionPolicy.FADE_MILLIS else 0)),
                exit = shrinkVertically(
                    animationSpec = tween(if (motionEnabled) V201ReminderMotionPolicy.OPTIONS_EXIT_MILLIS else 0),
                    shrinkTowards = Alignment.Top,
                ) + fadeOut(animationSpec = tween(if (motionEnabled) V201ReminderMotionPolicy.FADE_MILLIS else 0)),
            ) {
                Column {
                Spacer(Modifier.height(10.dp))
                Surface(
                    modifier = Modifier.fillMaxWidth().testTag("v200-more-options"),
                    shape = RoundedCornerShape(18.dp),
                    color = look.panel,
                    border = BorderStroke(1.dp, look.line),
                ) {
                    Column(
                        Modifier.padding(horizontal = 12.dp, vertical = 12.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(5.dp),
                    ) {
                        if (notes.isNotBlank()) {
                            Text(
                                text = notes,
                                color = look.muted,
                                fontSize = 12.sp,
                                lineHeight = 17.sp,
                                modifier = Modifier.fillMaxWidth().padding(bottom = 6.dp),
                            )
                        }
                        if (stageCheckIn) {
                            OutlinedButton(
                                onClick = onPause,
                                interactionSource = pausePress,
                                modifier = Modifier.fillMaxWidth().height(48.dp)
                                    .v201PressFeedback(pausePress, motionEnabled),
                                border = BorderStroke(1.dp, BacklotBorder),
                                shape = RoundedCornerShape(13.dp),
                            ) {
                                Icon(Icons.Outlined.PauseCircle, null, modifier = Modifier.size(19.dp))
                                Spacer(Modifier.size(8.dp))
                                Text("PAUSE CHECK-INS", fontSize = 12.sp)
                            }
                        }
                        OutlinedButton(
                            onClick = onReschedule,
                            interactionSource = reschedulePress,
                            modifier = Modifier.fillMaxWidth().height(48.dp)
                                .v201PressFeedback(reschedulePress, motionEnabled),
                            border = BorderStroke(1.dp, BacklotBorder),
                            shape = RoundedCornerShape(13.dp),
                        ) {
                            Icon(Icons.Outlined.Schedule, null, modifier = Modifier.size(19.dp))
                            Spacer(Modifier.size(8.dp))
                            Text("RESCHEDULE", fontSize = 12.sp)
                        }
                        if (onReplay != null) {
                            TextButton(
                                onClick = onReplay,
                                interactionSource = replayPress,
                                modifier = Modifier.fillMaxWidth().height(48.dp)
                                    .v201PressFeedback(replayPress, motionEnabled),
                            ) {
                                Icon(Icons.Outlined.Replay, null, tint = look.hot)
                                Spacer(Modifier.size(8.dp))
                                Text("REPLAY VOICE", color = look.hot,
                                    fontSize = 12.sp)
                            }
                        }
                        TextButton(
                            onClick = onDismiss,
                            interactionSource = dismissPress,
                            modifier = Modifier.fillMaxWidth().height(48.dp)
                                .v201PressFeedback(dismissPress, motionEnabled),
                        ) {
                            Text("DISMISS FOR NOW", color = look.muted,
                                fontSize = 12.sp)
                        }
                    }
                }
            }
            }
        }
    }
}

internal fun reminderClockLabel(
    nowMillis: Long,
    zoneId: ZoneId = ZoneId.systemDefault(),
    locale: Locale = Locale.getDefault(),
): String = DateTimeFormatter.ofPattern("h:mm a", locale)
    .format(Instant.ofEpochMilli(nowMillis).atZone(zoneId))
