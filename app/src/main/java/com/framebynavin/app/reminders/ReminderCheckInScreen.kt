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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.framebynavin.app.ui.theme.BacklotAccent
import com.framebynavin.app.ui.theme.BacklotBackground
import com.framebynavin.app.ui.theme.BacklotBorder
import com.framebynavin.app.ui.theme.BacklotPrimaryText
import com.framebynavin.app.ui.theme.BacklotSecondaryAccent
import com.framebynavin.app.ui.theme.BacklotSecondaryText
import com.framebynavin.app.ui.theme.BacklotSuccess
import com.framebynavin.app.ui.theme.BacklotSurface
import com.framebynavin.app.ui.theme.BacklotSurfaceRaised

enum class ReminderCheckInKind { ALARM, VOICE }

/** Shared V148 reminder surface: one hierarchy and one appearance system for alarm + voice. */
@Composable
internal fun ReminderCheckInScreen(
    kind: ReminderCheckInKind,
    title: String,
    dueLabel: String,
    stageLabel: String,
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
) {
    BackHandler(enabled = true) { }
    val accent = if (kind == ReminderCheckInKind.VOICE) BacklotSecondaryAccent else BacklotAccent
    val scroll = rememberScrollState()

    Box(
        Modifier
            .fillMaxSize()
            .background(BacklotBackground)
            .statusBarsPadding()
            .navigationBarsPadding()
            .padding(horizontal = 20.dp, vertical = 14.dp),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .align(Alignment.Center)
                .verticalScroll(scroll),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(
                text = if (stageCheckIn) "STAGE CHECK-IN" else if (kind == ReminderCheckInKind.VOICE) "VOICE REMINDER" else "REMINDER",
                color = accent,
                fontSize = 10.sp,
                fontWeight = FontWeight.Black,
                letterSpacing = 1.3.sp,
            )
            Spacer(Modifier.height(8.dp))
            if (kind == ReminderCheckInKind.VOICE) V140VoicePulse(Modifier.size(88.dp))
            else V140AlarmPulse(Modifier.size(88.dp))
            Spacer(Modifier.height(10.dp))

            Text(
                text = title,
                color = BacklotPrimaryText,
                fontSize = 24.sp,
                lineHeight = 28.sp,
                fontWeight = FontWeight.Black,
                textAlign = TextAlign.Center,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
            Spacer(Modifier.height(8.dp))
            Text(
                text = if (stageCheckIn) "$stageLabel · $dueLabel" else dueLabel,
                color = BacklotSecondaryText,
                fontSize = 12.sp,
                fontWeight = FontWeight.SemiBold,
                textAlign = TextAlign.Center,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )

            if (notes.isNotBlank()) {
                Spacer(Modifier.height(12.dp))
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(14.dp),
                    color = BacklotSurface,
                    border = BorderStroke(1.dp, BacklotBorder),
                ) {
                    Text(
                        text = notes,
                        color = BacklotSecondaryText,
                        fontSize = 11.sp,
                        lineHeight = 16.sp,
                        textAlign = TextAlign.Center,
                        maxLines = 3,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
                    )
                }
            }

            Spacer(Modifier.height(16.dp))
            if (stageCheckIn) {
                Button(
                    onClick = onStageDone,
                    modifier = Modifier.fillMaxWidth().height(50.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = BacklotSuccess),
                    shape = RoundedCornerShape(14.dp),
                ) {
                    Icon(Icons.Outlined.Check, null)
                    Spacer(Modifier.size(7.dp))
                    Text("STAGE DONE", fontWeight = FontWeight.Black)
                }
                Spacer(Modifier.height(8.dp))
            }

            OutlinedButton(
                onClick = onWorking,
                modifier = Modifier.fillMaxWidth().height(48.dp),
                border = BorderStroke(1.dp, accent),
                colors = ButtonDefaults.outlinedButtonColors(containerColor = BacklotSurfaceRaised),
                shape = RoundedCornerShape(14.dp),
            ) {
                Icon(Icons.Outlined.Work, null, tint = accent)
                Spacer(Modifier.size(7.dp))
                Text("I'M WORKING ON IT", color = BacklotPrimaryText, fontWeight = FontWeight.Bold)
            }

            Spacer(Modifier.height(8.dp))
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedButton(
                    onClick = onSnooze,
                    modifier = Modifier.weight(1f).height(46.dp),
                    border = BorderStroke(1.dp, BacklotBorder),
                    shape = RoundedCornerShape(13.dp),
                ) {
                    Icon(Icons.Outlined.Snooze, null, tint = BacklotPrimaryText, modifier = Modifier.size(17.dp))
                    Spacer(Modifier.size(5.dp))
                    Text("${snoozeMinutes}m", color = BacklotPrimaryText, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                }
                if (stageCheckIn) {
                    OutlinedButton(
                        onClick = onPause,
                        modifier = Modifier.weight(1f).height(46.dp),
                        border = BorderStroke(1.dp, BacklotBorder),
                        shape = RoundedCornerShape(13.dp),
                    ) {
                        Icon(Icons.Outlined.PauseCircle, null, tint = BacklotPrimaryText, modifier = Modifier.size(17.dp))
                        Spacer(Modifier.size(5.dp))
                        Text("PAUSE 2H", color = BacklotPrimaryText, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                    }
                } else {
                    OutlinedButton(
                        onClick = onReschedule,
                        modifier = Modifier.weight(1f).height(46.dp),
                        border = BorderStroke(1.dp, BacklotBorder),
                        shape = RoundedCornerShape(13.dp),
                    ) {
                        Icon(Icons.Outlined.Schedule, null, tint = BacklotPrimaryText, modifier = Modifier.size(17.dp))
                        Spacer(Modifier.size(5.dp))
                        Text("CHOOSE TIME", color = BacklotPrimaryText, fontSize = 9.sp, fontWeight = FontWeight.Bold, maxLines = 1)
                    }
                }
            }

            if (onReplay != null) {
                Spacer(Modifier.height(4.dp))
                TextButton(onClick = onReplay) {
                    Icon(Icons.Outlined.Replay, null, tint = BacklotSecondaryAccent, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.size(5.dp))
                    Text("REPLAY VOICE", color = BacklotSecondaryAccent, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                }
            }

            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Center) {
                TextButton(onClick = onDismiss) {
                    Text("DISMISS", color = BacklotSecondaryText, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                }
                if (stageCheckIn) {
                    TextButton(onClick = onReschedule) {
                        Text("CHOOSE TIME", color = accent, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}
