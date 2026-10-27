package com.framebynavin.app.preview

import android.animation.ValueAnimator
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.framebynavin.app.reminders.ReminderCheckInKind
import com.framebynavin.app.reminders.V195ReminderParticleScene

/**
 * Safe Android phone viewer for the EXACT V218 production particle renderer.
 * Never posts notifications or schedules/removes reminders. No accounts/data.
 *
 * This intentionally does not display the production ReminderCheckInScreen
 * actions (which require the full production app). Those stay covered by
 * separate :app Android screenshot/interaction tests.
 */
class V218WaveReviewActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent { V218WaveReview() }
    }
}

@Composable
private fun V218WaveReview() {
    var kind by remember { mutableStateOf(ReminderCheckInKind.ALARM) }
    var lowPower by remember { mutableStateOf(false) }
    var motionRequested by remember { mutableStateOf(true) }
    var controlsShown by remember { mutableStateOf(false) }
    val motionEnabled = motionRequested && ValueAnimator.areAnimatorsEnabled()

    Box(Modifier.fillMaxSize().background(Color(0xFF0C0F1D))) {
        // Same real source as actual AlarmActivity and VoiceReminderActivity.
        // key restarts the one-off formation when switching scenarios.
        key(kind, lowPower, motionEnabled) {
            V195ReminderParticleScene(
                kind = kind,
                revealed = controlsShown,
                lowPower = lowPower,
                onReveal = { controlsShown = true },
                motionEnabled = motionEnabled,
                modifier = Modifier.fillMaxSize(),
            )
        }

        Column(
            Modifier.fillMaxWidth().align(Alignment.TopCenter)
                .statusBarsPadding().padding(horizontal = 18.dp, vertical = 16.dp),
            verticalArrangement = Arrangement.spacedBy(7.dp),
        ) {
            Text("BACKLOT / V218 WAVE LAB", color = Color(0xFFE6E9FF),
                fontSize = 13.sp, fontWeight = FontWeight.Black)
            Text("Offline preview. No reminders, sounds or user data.",
                color = Color(0xFFB6B9D6), fontSize = 11.sp)
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                if (kind == ReminderCheckInKind.ALARM) {
                    Button(onClick = { kind = ReminderCheckInKind.ALARM; controlsShown = false },
                        modifier = Modifier.weight(1f).testTag("v218-choice-alarm")) { Text("ALARM") }
                } else {
                    OutlinedButton(onClick = { kind = ReminderCheckInKind.ALARM; controlsShown = false },
                        modifier = Modifier.weight(1f).testTag("v218-choice-alarm")) { Text("ALARM", color = Color.White) }
                }
                if (kind == ReminderCheckInKind.VOICE) {
                    Button(onClick = { kind = ReminderCheckInKind.VOICE; controlsShown = false },
                        modifier = Modifier.weight(1f).testTag("v218-choice-voice")) { Text("VOICE") }
                } else {
                    OutlinedButton(onClick = { kind = ReminderCheckInKind.VOICE; controlsShown = false },
                        modifier = Modifier.weight(1f).testTag("v218-choice-voice")) { Text("VOICE", color = Color.White) }
                }
            }
        }

        Column(
            Modifier.fillMaxWidth().align(Alignment.BottomCenter)
                .navigationBarsPadding().padding(horizontal = 18.dp, vertical = 14.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(9.dp),
        ) {
            if (controlsShown) {
                Surface(
                    modifier = Modifier.fillMaxWidth().testTag("v218-review-revealed"),
                    color = Color(0xF01B2035),
                    shape = RoundedCornerShape(16.dp),
                ) {
                    Column(Modifier.padding(14.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("CONTROLS REVEALED", color = Color.White, fontWeight = FontWeight.Bold)
                        Text("Animation dims so genuine reminder actions remain readable.\n" +
                            "This review cannot snooze, dismiss or trigger real reminders.",
                            color = Color(0xFFD0D5EA), fontSize = 12.sp, textAlign = TextAlign.Center)
                    }
                }
            }
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedButton(onClick = { lowPower = !lowPower; controlsShown = false },
                    modifier = Modifier.weight(1f).testTag("v218-toggle-power")) {
                    Text(if (lowPower) "LOW POWER: ON" else "LOW POWER: OFF",
                        fontSize = 11.sp, maxLines = 1, color = Color.White)
                }
                OutlinedButton(onClick = { motionRequested = !motionRequested; controlsShown = false },
                    modifier = Modifier.weight(1f).testTag("v218-toggle-motion")) {
                    Text(if (motionRequested) "MOTION: ON" else "MOTION: OFF",
                        fontSize = 11.sp, maxLines = 1, color = Color.White)
                }
            }
            Button(
                onClick = { controlsShown = !controlsShown },
                modifier = Modifier.fillMaxWidth().height(48.dp).testTag("v218-toggle-controls")
                    .semantics { contentDescription = if (controlsShown) "Show wave" else "Reveal controls" },
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color(0xFFF6C86B), contentColor = Color(0xFF15151E)),
            ) {
                Text(if (controlsShown) "SHOW WAVE AGAIN" else "REVEAL CONTROLS",
                    fontWeight = FontWeight.Bold)
            }
            Text(if (motionEnabled) "Live flow  •  normal ${if (lowPower) 20 else 30}fps target" else
                "Static / reduced-motion mode",
                color = Color(0xFFB6B9D6), fontSize = 10.sp)
        }
    }
}
