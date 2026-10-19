package com.framebynavin.app.reminders

import android.animation.ValueAnimator
import android.os.PowerManager
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.activity.compose.BackHandler
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
    val clockLabel = remember { reminderClockLabel(System.currentTimeMillis()) }
    val contextLine = listOf(stageLabel.trim(), dueLabel.trim())
        .filter { it.isNotBlank() }
        .joinToString(" · ")
    val nextAction = V200ReminderLayoutPolicy.supportingAction(suggestedAction, title)
    val screen = LocalConfiguration.current
    val compact = V200ReminderLayoutPolicy.isCompact(screen.screenHeightDp)
    val wideActions = V200ReminderLayoutPolicy.useHorizontalActions(screen.screenWidthDp)
    var moreOpen by rememberSaveable { mutableStateOf(false) }

    // Screen controls always work: animation scale zero means immediate reveal.
    // Battery saver reduces the number of dots rather than affecting actions.
    val context = LocalContext.current
    val motionEnabled = remember { ValueAnimator.areAnimatorsEnabled() }
    val lowPower = remember(context) {
        context.getSystemService(PowerManager::class.java)?.isPowerSaveMode == true
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
        // The ambient field persists even after controls are visible.
        V195ReminderParticleScene(
            kind = kind,
            revealed = revealed,
            lowPower = lowPower,
            onReveal = { revealed = true },
            modifier = Modifier.fillMaxSize(),
            motionEnabled = motionEnabled,
        )
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
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = when {
                        stageCheckIn -> "STAGE CHECK-IN"
                        kind == ReminderCheckInKind.VOICE -> "VOICE"
                        else -> "ALARM"
                    },
                    color = accent,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.3.sp,
                )
                Text(
                    text = clockLabel,
                    color = BacklotSecondaryText,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold,
                )
            }

            Spacer(Modifier.height(if (compact) 7.dp else 18.dp))

            V199ReminderSpark(
                kind = kind,
                motionEnabled = motionEnabled,
                lowPower = lowPower,
                modifier = Modifier
                    .size(if (compact) 104.dp else 126.dp)
                    .testTag(if (kind == ReminderCheckInKind.VOICE) "v195-voice-guide" else "v195-alarm-guide"),
            )
            Spacer(Modifier.height(if (compact) 8.dp else 13.dp))

            Text(
                text = title,
                color = BacklotPrimaryText,
                fontSize = if (compact) 22.sp else 24.sp,
                lineHeight = if (compact) 27.sp else 30.sp,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center,
                maxLines = 3,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.fillMaxWidth().testTag("v200-reminder-title"),
            )
            if (contextLine.isNotBlank()) {
                Spacer(Modifier.height(6.dp))
                Text(
                    text = contextLine,
                    color = BacklotSecondaryText,
                    fontSize = 12.sp,
                    lineHeight = 17.sp,
                    textAlign = TextAlign.Center,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            if (nextAction != null) {
                Spacer(Modifier.height(if (compact) 8.dp else 12.dp))
                Text(
                    text = nextAction,
                    color = BacklotPrimaryText.copy(alpha = .85f),
                    fontSize = 13.sp,
                    lineHeight = 18.sp,
                    textAlign = TextAlign.Center,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
            }

            Spacer(Modifier.height(if (compact) 18.dp else 26.dp))

            // ONE dominant answer; Stage Done takes priority only for check-ins.
            Button(
                onClick = if (stageCheckIn) onStageDone else onWorking,
                modifier = Modifier.fillMaxWidth().height(54.dp)
                    .testTag("v200-primary-reminder-action"),
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (stageCheckIn) BacklotSuccess else accent,
                    contentColor = Color(0xFF15151E),
                ),
                shape = RoundedCornerShape(16.dp),
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
                    modifier = Modifier.fillMaxWidth().height(50.dp),
                    border = BorderStroke(1.dp, accent.copy(alpha = .72f)),
                    shape = RoundedCornerShape(15.dp),
                ) {
                    Icon(Icons.Outlined.Work, null, tint = accent, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.size(8.dp))
                    Text("I'M WORKING", color = BacklotPrimaryText,
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
                        modifier = Modifier.weight(1f).height(50.dp),
                        border = BorderStroke(1.dp, BacklotBorder),
                        shape = RoundedCornerShape(15.dp),
                    ) {
                        Icon(Icons.Outlined.Snooze, null, tint = BacklotPrimaryText,
                            modifier = Modifier.size(18.dp))
                        Spacer(Modifier.size(7.dp))
                        Text("SNOOZE ${snoozeMinutes} MIN", color = BacklotPrimaryText,
                            fontSize = 12.sp, maxLines = 1, fontWeight = FontWeight.SemiBold)
                    }
                    TextButton(
                        onClick = { moreOpen = !moreOpen },
                        modifier = Modifier.height(50.dp),
                    ) {
                        Text(if (moreOpen) "HIDE OPTIONS" else "MORE OPTIONS",
                            color = BacklotPrimaryText, fontSize = 11.sp,
                            fontWeight = FontWeight.SemiBold, maxLines = 1)
                    }
                }
            } else {
                OutlinedButton(
                    onClick = onSnooze,
                    modifier = Modifier.fillMaxWidth().height(50.dp),
                    border = BorderStroke(1.dp, BacklotBorder),
                    shape = RoundedCornerShape(15.dp),
                ) {
                    Icon(Icons.Outlined.Snooze, null, tint = BacklotPrimaryText,
                        modifier = Modifier.size(18.dp))
                    Spacer(Modifier.size(8.dp))
                    Text("SNOOZE ${snoozeMinutes} MIN", color = BacklotPrimaryText,
                        fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                }
                TextButton(
                    onClick = { moreOpen = !moreOpen },
                    modifier = Modifier.fillMaxWidth().height(48.dp),
                ) {
                    Text(if (moreOpen) "HIDE OPTIONS" else "MORE OPTIONS",
                        color = BacklotPrimaryText, fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold)
                }
            }

            if (moreOpen) {
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
                                modifier = Modifier.fillMaxWidth().height(48.dp),
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
                            modifier = Modifier.fillMaxWidth().height(48.dp),
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
                                modifier = Modifier.fillMaxWidth().height(48.dp),
                            ) {
                                Icon(Icons.Outlined.Replay, null, tint = BacklotSecondaryAccent)
                                Spacer(Modifier.size(8.dp))
                                Text("REPLAY VOICE", color = BacklotSecondaryAccent,
                                    fontSize = 12.sp)
                            }
                        }
                        TextButton(
                            onClick = onDismiss,
                            modifier = Modifier.fillMaxWidth().height(48.dp),
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

internal fun reminderClockLabel(
    nowMillis: Long,
    zoneId: ZoneId = ZoneId.systemDefault(),
    locale: Locale = Locale.getDefault(),
): String = DateTimeFormatter.ofPattern("h:mm a", locale)
    .format(Instant.ofEpochMilli(nowMillis).atZone(zoneId))
