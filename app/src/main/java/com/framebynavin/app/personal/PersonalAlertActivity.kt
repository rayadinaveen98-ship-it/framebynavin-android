package com.framebynavin.app.personal

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Alarm
import androidx.compose.material.icons.outlined.Mic
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.lifecycleScope
import com.framebynavin.app.ui.theme.FrameByNavinTheme
import java.lang.Math.PI
import kotlin.math.cos
import kotlin.math.sin
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * Only opened after a user taps their Personal reminder notification.
 * No full-screen intent and no showWhenLocked; Android lock screen remains
 * private, compliant with restricted full-screen alarm entitlement.
 */
internal class PersonalAlertActivity : ComponentActivity() {
    private val personalId get() = intent.getStringExtra(PersonalDeliveryContract.EXTRA_PERSONAL_ID).orEmpty()
    private val personalToken get() = intent.getStringExtra(PersonalDeliveryContract.EXTRA_TOKEN).orEmpty()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val id = personalId
        val token = personalToken
        if (id.isBlank() || token.isBlank()) { finish(); return }
        setContent {
            FrameByNavinTheme {
                var reminder by remember { mutableStateOf<PersonalReminder?>(null) }
                var invalid by remember { mutableStateOf(false) }
                var saving by remember { mutableStateOf(false) }
                var error by remember { mutableStateOf<String?>(null) }
                LaunchedEffect(id, token) {
                    val item = runCatching {
                        withContext(Dispatchers.IO) {
                            PersonalReminderStore(applicationContext).load()
                                .firstOrNull { it.id == id && !it.paused &&
                                    it.pendingToken == token && it.presentedToken == token }
                        }
                    }.getOrNull()
                    if (item == null) invalid = true else reminder = item
                }
                fun respond(action: String) {
                    val current = reminder ?: return
                    if (saving) return
                    saving = true
                    lifecycleScope.launch {
                        val result = runCatching {
                            withContext(Dispatchers.IO) {
                                PersonalDeliveryCoordinator(applicationContext).apply(
                                    current.id, current.pendingOccurrenceMillis,
                                    current.pendingToken, action,
                                )
                            }
                        }.getOrNull()
                        if (result == PersonalApplyResult.ACCEPTED) {
                            PersonalPlaybackService.stopMatching(current.id, current.pendingToken)
                            finish()
                        } else {
                            error = "This reminder has changed. Please reopen it."
                            saving = false
                        }
                    }
                }
                V2233PersonalAlertScreen(
                    reminder = reminder,
                    invalid = invalid,
                    busy = saving,
                    error = error,
                    onDone = { respond(PersonalDeliveryContract.ACTION_DONE) },
                    onSnooze = { respond(PersonalDeliveryContract.ACTION_SNOOZE) },
                    onDismiss = { respond(PersonalDeliveryContract.ACTION_DISMISS) },
                    onClose = { finish() },
                )
            }
        }
    }

    override fun onNewIntent(intent: android.content.Intent) {
        super.onNewIntent(intent)
        // A new notification must never reuse an old screen's state and actions.
        finish()
        startActivity(intent.addFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK))
    }
}

@Composable
internal fun V2233PersonalAlertScreen(
    reminder: PersonalReminder?,
    invalid: Boolean,
    busy: Boolean,
    error: String?,
    onDone: () -> Unit,
    onSnooze: () -> Unit,
    onDismiss: () -> Unit,
    onClose: () -> Unit,
) {
    val midnight = Color(0xFF080D20)
    val violet = Color(0xFF9B68FF)
    val blue = Color(0xFF30B8FF)
    val coral = Color(0xFFFF6C95)
    Surface(color = midnight, modifier = Modifier.fillMaxSize()
        .testTag("v2233-personal-alert-screen")) {
        Column(
            Modifier.fillMaxSize().verticalScroll(rememberScrollState())
                .systemBarsPadding().padding(horizontal = 22.dp, vertical = 16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(13.dp),
        ) {
            Text("BACKLOT  /  PERSONAL", color = blue,
                fontSize = 11.sp, fontWeight = FontWeight.Black,
                modifier = Modifier.align(Alignment.Start))
            Spacer(Modifier.height(4.dp))
            if (reminder == null) {
                Spacer(Modifier.height(55.dp))
                if (invalid) {
                    Text("Reminder no longer active", color = Color.White, fontSize = 21.sp)
                    Text("It may have been completed, changed or paused.",
                        color = Color(0xFFC2C6D6), textAlign = TextAlign.Center)
                    OutlinedButton(onClick = onClose) { Text("CLOSE") }
                } else {
                    CircularProgressIndicator(color = violet)
                }
                return@Column
            }
            // The art is a particle wave itself, with the symbol emerging from
            // the same luminous palette. No reused creator task icon/sticker.
            V2233PersonalParticleWave(reminder.alert)
            Text(if (reminder.alert == PersonalAlert.ALARM) "YOUR ALARM"
                else if (reminder.alert == PersonalAlert.VOICE) "VOICE REMINDER"
                else "YOUR REMINDER",
                color = blue, fontWeight = FontWeight.ExtraBold, fontSize = 12.sp)
            Text(reminder.title, color = Color.White, fontSize = 26.sp,
                fontWeight = FontWeight.Bold, lineHeight = 33.sp,
                textAlign = TextAlign.Center, modifier = Modifier.testTag("v2233-personal-alert-title"))
            Text(
                when (reminder.alert) {
                    PersonalAlert.ALARM -> "Ringing is limited to one minute. Your response remains available afterward."
                    PersonalAlert.VOICE -> "Spoken once when on-device speech is available. Notification remains as backup."
                    PersonalAlert.NOTIFICATION -> "Your personal reminder is ready."
                },
                color = Color(0xFFB6C3DB), fontSize = 12.sp, textAlign = TextAlign.Center,
            )
            Spacer(Modifier.height(14.dp))
            Button(
                onClick = onDone, enabled = !busy,
                modifier = Modifier.fillMaxWidth().heightIn(min = 54.dp)
                    .testTag("v2233-personal-done"),
                colors = ButtonDefaults.buttonColors(containerColor = violet),
                shape = RoundedCornerShape(16.dp),
            ) { Text("DONE", color = Color.White, fontWeight = FontWeight.Bold) }
            OutlinedButton(
                onClick = onSnooze, enabled = !busy,
                modifier = Modifier.fillMaxWidth().heightIn(min = 54.dp)
                    .testTag("v2233-personal-snooze"),
                border = BorderStroke(1.dp, blue.copy(alpha = .75f)),
                shape = RoundedCornerShape(16.dp),
            ) { Text("SNOOZE 10 MINUTES", color = Color.White) }
            TextButton(
                onClick = onDismiss, enabled = !busy,
                modifier = Modifier.testTag("v2233-personal-dismiss"),
            ) { Text("DISMISS WITHOUT COMPLETING", color = coral) }
            if (error != null) Text(error, color = coral, textAlign = TextAlign.Center)
            Spacer(Modifier.height(16.dp))
        }
    }
}

/** 100 small points, deterministic positions, a low-complexity animated ring. */
@Composable
internal fun V2233PersonalParticleWave(alert: PersonalAlert) {
    val transition = rememberInfiniteTransition(label = "personal-particle-wave")
    val phase by transition.animateFloat(
        initialValue = 0f, targetValue = (2.0 * PI).toFloat(),
        animationSpec = infiniteRepeatable(tween(6_800, easing = LinearEasing), RepeatMode.Restart),
        label = "personal-orbit",
    )
    val first = if (alert == PersonalAlert.ALARM) Color(0xFFFF7299) else Color(0xFF50CFFF)
    val second = Color(0xFFAA78FF)
    Canvas(Modifier.fillMaxWidth().height(252.dp).testTag("v2233-particle-wave")) {
        val center = Offset(size.width / 2f, size.height / 2f)
        val extent = minOf(size.width, size.height)
        drawCircle(
            Brush.radialGradient(
                listOf(first.copy(alpha = .25f), second.copy(alpha = .09f), Color.Transparent),
                center = center, radius = extent * .60f,
            ), radius = extent * .60f, center = center,
        )
        for (i in 0 until 100) {
            val position = i.toFloat()
            val ring = (i % 5).toFloat()
            val angle = (position * 2.399963f + phase * if (i % 2 == 0) 1f else -.55f)
            val radius = extent * (.19f + ring * .051f) *
                (1f + .035f * sin(phase + ring))
            val p = Offset(
                center.x + radius * cos(angle),
                center.y + radius * sin(angle) * .72f,
            )
            val color = if (i % 3 == 0) first else second
            drawCircle(color.copy(alpha = .46f + (i % 4) * .12f),
                radius = (1.1f + (i % 5) * .53f) * density,
                center = p,
            )
        }
        drawCircle(first.copy(alpha = .35f),
            radius = extent * .12f, center = center, style = Stroke(width = 1.8f * density))
        drawCircle(second.copy(alpha = .20f),
            radius = extent * .15f, center = center, style = Stroke(width = 1.0f * density))
    }
}
