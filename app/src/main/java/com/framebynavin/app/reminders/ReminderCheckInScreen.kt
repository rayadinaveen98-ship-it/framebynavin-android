package com.framebynavin.app.reminders

import android.animation.ValueAnimator
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.framebynavin.app.data.ProjectAttentionPlan
import com.framebynavin.app.ui.theme.BacklotAccent
import com.framebynavin.app.ui.theme.BacklotBackground
import com.framebynavin.app.ui.theme.BacklotBorder
import com.framebynavin.app.ui.theme.BacklotPrimaryText
import com.framebynavin.app.ui.theme.BacklotSecondaryAccent
import com.framebynavin.app.ui.theme.BacklotSecondaryText
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
    val accent = if (kind == ReminderCheckInKind.VOICE) BacklotSecondaryAccent else BacklotAccent
    val scroll = rememberScrollState()
    val contextLine = listOf(stageLabel.trim(), dueLabel.trim())
        .filter { it.isNotBlank() }
        .joinToString(" · ")
    val nextAction = V200ReminderLayoutPolicy.supportingAction(suggestedAction, title)
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
    val context = LocalContext.current
    val motionEnabled = remember { ValueAnimator.areAnimatorsEnabled() }
    val lowPower = remember(context) {
        context.getSystemService(PowerManager::class.java)?.isPowerSaveMode == true
    }
    // Visual appearance is independent of reminder delivery, audio and actions.
    val visualStore = remember(context) { V329ReminderVisualStore(context) }
    val visualChannel = if (kind == ReminderCheckInKind.ALARM)
        V329ReminderChannel.ALARM else V329ReminderChannel.VOICE
    val appearance = remember(visualChannel) {
        visualStore.get(V329ReminderDomain.CREATOR, visualChannel)
    }
    // The user, not an intro timer, chooses when to reveal controls.
    // When OS animations are disabled, accessible controls appear immediately.
    var revealed by rememberSaveable(kind, title) {
        mutableStateOf(V197ReminderRevealPolicy.startRevealed(motionEnabled))
    }
    LaunchedEffect(motionEnabled) {
        if (!motionEnabled) revealed = true
    }
    val revealAlpha by animateFloatAsState(
        targetValue = if (revealed) 1f else 0f,
        animationSpec = tween(durationMillis = 650),
        label = "v195-reminder-controls",
    )

    Box(
        Modifier
            .fillMaxSize()
            .background(BacklotBackground),
    ) {
        // V329: the user's chosen visual world builds the clock/mic from real particles.
        // Single tap reacts; only double tap reveals unchanged controls. The
        // existing post-reveal screen and V231 backdrop remain intact.
        Crossfade(
            targetState = revealed,
            animationSpec = tween(durationMillis = if (motionEnabled) 480 else 0),
            label = "v231-reminder-backdrop-transition",
        ) { showingActions ->
            if (showingActions) {
                V331RevealBackdrop(Modifier.fillMaxSize())
            } else {
                V330ReminderPrototypeScene(
                    domain = V329ReminderDomain.CREATOR,
                    channel = visualChannel,
                    appearance = appearance,
                    onReveal = { revealed = true },
                    modifier = Modifier.fillMaxSize(),
                    motionOverride = motionEnabled,
                    lowPowerOverride = lowPower,
                )
            }
        }
        // Only render actionable controls after manual reveal; no ghost actions under the intro.
        if (revealed) Column(
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
            // V331 restores the approved prototype's centered icon plaque,
            // title, large reminder time and quiet metadata chips. Real task
            // values only; no fake prototype reminder content.
            V331PrototypeRevealHero(
                domain = V329ReminderDomain.CREATOR,
                channel = visualChannel,
                title = title,
                subtitle = nextAction ?: "Your next move starts now.",
                // Show scheduled time only; displaying the current clock here
                // incorrectly represented it as the reminder's due time.
                timeLabel = V331RevealStyle.timeFromCreatorDueLabel(dueLabel),
                chips = listOf(stageLabel, dueLabel).map { it.take(25) },
                compact = compact,
                titleTag = "v200-reminder-title",
            )
            Spacer(Modifier.height(if (compact) 21.dp else 56.dp))


            // Same callbacks, same actions and existing UI test tags.
            // Only their positioning/appearance follows the V1 prototype:
            // Snooze to the left and the dominant action to the right.
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                OutlinedButton(
                    onClick = onSnooze,
                    interactionSource = snoozePress,
                    modifier = Modifier.weight(1f).height(56.dp)
                        .v201PressFeedback(snoozePress, motionEnabled),
                    border = BorderStroke(1.dp, V331RevealStyle.line),
                    colors = androidx.compose.material3.ButtonDefaults.outlinedButtonColors(
                        containerColor = V331RevealStyle.surface,
                    ),
                    shape = RoundedCornerShape(17.dp),
                ) {
                    Text("SNOOZE ${snoozeMinutes}M",
                        color = V331RevealStyle.foreground,
                        fontSize = if (compact) 11.sp else 13.sp,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1)
                }
                Button(
                    onClick = if (stageCheckIn) onStageDone else onWorking,
                    interactionSource = primaryPress,
                    modifier = Modifier.weight(1f).height(56.dp)
                        .v201PressFeedback(primaryPress, motionEnabled, prominent = true)
                        .testTag("v200-primary-reminder-action"),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (stageCheckIn)
                            BacklotSuccess else V331RevealStyle.red,
                        contentColor = V331RevealStyle.foreground,
                    ),
                    shape = RoundedCornerShape(17.dp),
                ) {
                    Text(
                        if (stageCheckIn) "STEP DONE" else "I'M WORKING",
                        fontSize = if (compact) 11.sp else 13.sp,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                    )
                }
            }
            if (stageCheckIn) {
                Spacer(Modifier.height(9.dp))
                OutlinedButton(
                    onClick = onWorking,
                    interactionSource = workingPress,
                    modifier = Modifier.fillMaxWidth().height(49.dp)
                        .v201PressFeedback(workingPress, motionEnabled),
                    border = BorderStroke(1.dp, V331RevealStyle.line),
                    shape = RoundedCornerShape(16.dp),
                ) {
                    Text("I'M WORKING", color = V331RevealStyle.foreground,
                        fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                }
            }
            Spacer(Modifier.height(5.dp))
            TextButton(
                onClick = { moreOpen = !moreOpen },
                interactionSource = morePress,
                modifier = Modifier.fillMaxWidth().height(47.dp)
                    .v201PressFeedback(morePress, motionEnabled),
            ) {
                Text(
                    if (moreOpen) "HIDE OPTIONS" else "MORE OPTIONS",
                    color = V331RevealStyle.secondary,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold,
                )
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
                    color = BacklotBackground.copy(alpha = .95f),
                    border = BorderStroke(1.dp, BacklotBorder),
                ) {
                    Column(
                        Modifier.padding(horizontal = 12.dp, vertical = 12.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(5.dp),
                    ) {
                        if (notes.isNotBlank()) {
                            Text(
                                text = notes,
                                color = BacklotSecondaryText,
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
                                Icon(Icons.Outlined.Replay, null, tint = BacklotSecondaryAccent)
                                Spacer(Modifier.size(8.dp))
                                Text("REPLAY VOICE", color = BacklotSecondaryAccent,
                                    fontSize = 12.sp)
                            }
                        }
                        TextButton(
                            onClick = onDismiss,
                            interactionSource = dismissPress,
                            modifier = Modifier.fillMaxWidth().height(48.dp)
                                .v201PressFeedback(dismissPress, motionEnabled),
                        ) {
                            Text("DISMISS FOR NOW", color = BacklotSecondaryText,
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
