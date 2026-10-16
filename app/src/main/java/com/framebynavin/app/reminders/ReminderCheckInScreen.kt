package com.framebynavin.app.reminders

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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.framebynavin.app.data.ProjectAttentionPlan
import com.framebynavin.app.data.ProjectPulseResponse
import com.framebynavin.app.ui.theme.BacklotAccent
import com.framebynavin.app.ui.theme.BacklotBackground
import com.framebynavin.app.ui.theme.BacklotBorder
import com.framebynavin.app.ui.theme.BacklotPrimaryText
import com.framebynavin.app.ui.theme.BacklotSecondaryAccent
import com.framebynavin.app.ui.theme.BacklotSecondaryText
import com.framebynavin.app.ui.theme.BacklotSuccess
import com.framebynavin.app.ui.theme.BacklotSurface
import com.framebynavin.app.ui.theme.BacklotSurfaceRaised
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
    val nextAction = suggestedAction.trim().ifBlank { "Continue the current stage" }
    var moreOpen by rememberSaveable { mutableStateOf(false) }

    Box(
        Modifier
            .fillMaxSize()
            .background(BacklotBackground)
            .statusBarsPadding()
            .navigationBarsPadding()
            .padding(horizontal = 20.dp, vertical = 12.dp),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .align(Alignment.Center)
                .verticalScroll(scroll),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = when {
                        stageCheckIn -> "STAGE CHECK-IN"
                        kind == ReminderCheckInKind.VOICE -> "VOICE REMINDER"
                        else -> "REMINDER"
                    },
                    color = accent,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Black,
                    letterSpacing = 1.2.sp,
                )
                Text(
                    text = clockLabel,
                    color = BacklotPrimaryText,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Black,
                )
            }

            Spacer(Modifier.height(10.dp))
            if (kind == ReminderCheckInKind.VOICE) V140VoicePulse(Modifier.size(68.dp))
            else V140AlarmPulse(Modifier.size(68.dp))
            Spacer(Modifier.height(8.dp))

            Text(
                text = title,
                color = BacklotPrimaryText,
                fontSize = 21.sp,
                lineHeight = 25.sp,
                fontWeight = FontWeight.Black,
                textAlign = TextAlign.Center,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
            if (contextLine.isNotBlank()) {
                Spacer(Modifier.height(6.dp))
                Text(
                    text = contextLine,
                    color = BacklotSecondaryText,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.SemiBold,
                    textAlign = TextAlign.Center,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
            }

            Spacer(Modifier.height(12.dp))
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(14.dp),
                color = BacklotSurfaceRaised,
                border = BorderStroke(1.dp, accent.copy(alpha = 0.45f)),
            ) {
                Column(Modifier.padding(horizontal = 14.dp, vertical = 11.dp)) {
                    Text(
                        text = "UP NEXT",
                        color = accent,
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Black,
                        letterSpacing = 1.sp,
                    )
                    Spacer(Modifier.height(4.dp))
                    Text(
                        text = nextAction,
                        color = BacklotPrimaryText,
                        fontSize = 13.sp,
                        lineHeight = 18.sp,
                        fontWeight = FontWeight.SemiBold,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }

            if (notes.isNotBlank()) {
                Spacer(Modifier.height(8.dp))
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(13.dp),
                    color = BacklotSurface,
                    border = BorderStroke(1.dp, BacklotBorder),
                ) {
                    Text(
                        text = notes,
                        color = BacklotSecondaryText,
                        fontSize = 11.sp,
                        lineHeight = 16.sp,
                        maxLines = 3,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 9.dp),
                    )
                }
            }

            Spacer(Modifier.height(12.dp))
            if (stageCheckIn) {
                Button(
                    onClick = onStageDone,
                    modifier = Modifier.fillMaxWidth().height(48.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = BacklotSuccess),
                    shape = RoundedCornerShape(14.dp),
                ) {
                    Icon(Icons.Outlined.Check, null)
                    Spacer(Modifier.size(7.dp))
                    Text("STEP DONE", fontWeight = FontWeight.Black)
                }
                Spacer(Modifier.height(7.dp))
            }

            OutlinedButton(
                onClick = onWorking,
                modifier = Modifier.fillMaxWidth().height(49.dp),
                border = BorderStroke(1.dp, accent),
                colors = ButtonDefaults.outlinedButtonColors(containerColor = BacklotSurfaceRaised),
                shape = RoundedCornerShape(14.dp),
            ) {
                Icon(Icons.Outlined.Work, null, tint = accent)
                Spacer(Modifier.size(8.dp))
                Text("I'M WORKING", color = BacklotPrimaryText, fontSize = 13.sp, fontWeight = FontWeight.Bold)
            }

            Spacer(Modifier.height(8.dp))
            OutlinedButton(
                onClick = onSnooze,
                modifier = Modifier.fillMaxWidth().height(49.dp),
                border = BorderStroke(1.dp, BacklotBorder),
                shape = RoundedCornerShape(14.dp),
            ) {
                Icon(Icons.Outlined.Snooze, null, tint = BacklotPrimaryText, modifier = Modifier.size(20.dp))
                Spacer(Modifier.size(8.dp))
                Text("SNOOZE ${snoozeMinutes} MIN", color = BacklotPrimaryText,
                    fontSize = 13.sp, fontWeight = FontWeight.Bold)
            }

            Spacer(Modifier.height(9.dp))
            OutlinedButton(
                onClick = { moreOpen = !moreOpen },
                modifier = Modifier.fillMaxWidth().height(47.dp),
                border = BorderStroke(1.dp, BacklotBorder),
                shape = RoundedCornerShape(14.dp),
            ) {
                Text(if (moreOpen) "HIDE OPTIONS" else "MORE OPTIONS",
                    color = BacklotSecondaryText, fontSize = 12.sp, fontWeight = FontWeight.Bold)
            }

            if (moreOpen) {
                Spacer(Modifier.height(10.dp))
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(14.dp),
                    color = BacklotSurface,
                    border = BorderStroke(1.dp, BacklotBorder),
                ) {
                    Column(Modifier.padding(12.dp)) {
                        Text("WHAT HAPPENS NEXT", color = accent,
                            fontSize = 11.sp, fontWeight = FontWeight.Black)
                        Spacer(Modifier.height(5.dp))
                        Text(
                            "I'm working: " + ReminderResponseCopy.followUp(
                                attentionPlan, ProjectPulseResponse.WORKING, stageCheckIn,
                            ),
                            color = BacklotSecondaryText, fontSize = 12.sp, lineHeight = 18.sp,
                        )
                        Spacer(Modifier.height(5.dp))
                        Text(
                            "Dismiss: " + ReminderResponseCopy.followUp(
                                attentionPlan, ProjectPulseResponse.DISMISSED, stageCheckIn,
                            ),
                            color = BacklotSecondaryText, fontSize = 12.sp, lineHeight = 18.sp,
                        )
                        Spacer(Modifier.height(9.dp))
                        if (stageCheckIn) {
                            OutlinedButton(
                                onClick = onPause,
                                modifier = Modifier.fillMaxWidth().heightIn(min = 44.dp),
                                border = BorderStroke(1.dp, BacklotBorder),
                                shape = RoundedCornerShape(12.dp),
                            ) {
                                Icon(Icons.Outlined.PauseCircle, null)
                                Spacer(Modifier.size(7.dp))
                                Text("PAUSE CHECK-INS · UP TO 2H", fontSize = 12.sp)
                            }
                        }
                        OutlinedButton(
                            onClick = onReschedule,
                            modifier = Modifier.fillMaxWidth().heightIn(min = 44.dp),
                            border = BorderStroke(1.dp, BacklotBorder),
                            shape = RoundedCornerShape(12.dp),
                        ) {
                            Icon(Icons.Outlined.Schedule, null)
                            Spacer(Modifier.size(7.dp))
                            Text("CHOOSE ANOTHER TIME", fontSize = 12.sp)
                        }
                        if (onReplay != null) {
                            TextButton(onClick = onReplay, modifier = Modifier.fillMaxWidth()) {
                                Icon(Icons.Outlined.Replay, null, tint = BacklotSecondaryAccent)
                                Spacer(Modifier.size(7.dp))
                                Text("REPLAY VOICE", color = BacklotSecondaryAccent, fontSize = 12.sp)
                            }
                        }
                        TextButton(onClick = onDismiss, modifier = Modifier.fillMaxWidth()) {
                            Text("DISMISS FOR NOW", color = BacklotSecondaryText, fontSize = 12.sp)
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
