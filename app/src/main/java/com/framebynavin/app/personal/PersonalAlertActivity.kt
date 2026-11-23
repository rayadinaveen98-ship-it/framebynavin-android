package com.framebynavin.app.personal

import android.app.KeyguardManager
import android.animation.ValueAnimator
import android.os.PowerManager
import android.content.Context
import android.os.Build
import android.os.Bundle
import android.view.WindowManager
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
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.lifecycleScope
import com.framebynavin.app.ui.theme.FrameByNavinTheme
import com.framebynavin.app.reminders.ReminderCheckInKind
import com.framebynavin.app.reminders.V407ReminderCompanionMoment
import com.framebynavin.app.reminders.V407ReminderAccessDisclosure
import com.framebynavin.app.reminders.V407ReminderOwner
import com.framebynavin.app.reminders.P1ReminderScope
import com.framebynavin.app.reminders.P10ReminderFamilyStore
import com.framebynavin.app.reminders.P10ReminderIdentityEmblem
import com.framebynavin.app.reminders.P10ReminderMainAtmosphere
import com.framebynavin.app.reminders.P10PersonalReminderExperience
import com.framebynavin.app.reminders.actionPalette
import java.time.Instant
import java.time.format.DateTimeFormatter
import java.util.Locale
import java.lang.Math.PI
import kotlin.math.cos
import kotlin.math.sin
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * Can appear via a permission-checked Personal ALARM full-screen intent while
 * locked; VOICE and NOTIFICATION never request automatic full-screen access.
 * Hide titles and disable all response actions while the keyguard is showing.
 */
internal class PersonalAlertActivity : ComponentActivity() {
    private var keyguardShowing by mutableStateOf(false)

    private fun refreshLockState() {
        keyguardShowing = getSystemService(Context.KEYGUARD_SERVICE)
            .let { it as? KeyguardManager }?.isKeyguardLocked == true
    }

    override fun onResume() {
        super.onResume()
        refreshLockState()
    }

    override fun onWindowFocusChanged(hasFocus: Boolean) {
        super.onWindowFocusChanged(hasFocus)
        refreshLockState()
    }

    private val personalId get() = intent.getStringExtra(PersonalDeliveryContract.EXTRA_PERSONAL_ID).orEmpty()
    private val personalToken get() = intent.getStringExtra(PersonalDeliveryContract.EXTRA_TOKEN).orEmpty()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val id = personalId
        val token = personalToken
        if (id.isBlank() || token.isBlank()) { finish(); return }
        if (intent.getBooleanExtra(PersonalFullScreenPolicy.EXTRA_ALARM_VISUAL, false)) {
            // Do not set these for VOICE or ordinary Personal notifications.
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O_MR1) {
                setShowWhenLocked(true)
                setTurnScreenOn(true)
            } else {
                @Suppress("DEPRECATION")
                window.addFlags(
                    WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED or
                        WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON,
                )
            }
        }
        refreshLockState()
        PersonalAlertSurfaceRegistry.attach(this, id, token)
        setContent {
            FrameByNavinTheme {
                var reminder by remember { mutableStateOf<PersonalReminder?>(null) }
                var invalid by remember { mutableStateOf(false) }
                var saving by remember { mutableStateOf(false) }
                var error by remember { mutableStateOf<String?>(null) }
                LaunchedEffect(id, token) {
                    // Full-screen PendingIntent can launch before the receiver
                    // finishes acknowledgePosted(). Wait for that exact token,
                    // but NEVER accept another/stale/paused occurrence.
                    repeat(PersonalAlertPresentationPolicy.MAX_READ_ATTEMPTS) { attempt ->
                        val item = runCatching {
                            withContext(Dispatchers.IO) {
                                PersonalReminderStore(applicationContext).load()
                                    .firstOrNull { it.id == id }
                            }
                        }.getOrNull()
                        when (PersonalAlertPresentationPolicy.state(item, id, token)) {
                            PersonalAlertPresentationState.READY -> {
                                reminder = item
                                return@LaunchedEffect
                            }
                            PersonalAlertPresentationState.INVALID -> {
                                invalid = true
                                return@LaunchedEffect
                            }
                            PersonalAlertPresentationState.WAITING -> {
                                if (attempt + 1 < PersonalAlertPresentationPolicy.MAX_READ_ATTEMPTS) {
                                    delay(PersonalAlertPresentationPolicy.RETRY_DELAY_MILLIS)
                                }
                            }
                        }
                    }
                    invalid = true
                }
                fun respond(action: String) {
                    val current = reminder ?: return
                    if (saving || keyguardShowing) return
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
                        if (result == PersonalApplyResult.ACCEPTED ||
                            result == PersonalApplyResult.ACCEPTED_NEEDS_REARM) {
                            if (result == PersonalApplyResult.ACCEPTED_NEEDS_REARM) {
                                android.widget.Toast.makeText(
                                    applicationContext,
                                    "Response saved. Android alarm re-arm needs attention.",
                                    android.widget.Toast.LENGTH_LONG,
                                ).show()
                            }
                            PersonalPlaybackService.stopMatching(current.id, current.pendingToken)
                            finish()
                        } else {
                            error = "This reminder has changed. Please reopen it."
                            saving = false
                        }
                    }
                }
                P10PersonalReminderExperience(
                    occurrenceKey = id + ":" + token,
                    reminder = reminder,
                    invalid = invalid,
                    locked = keyguardShowing,
                ) { backgroundProvided ->
                V2233PersonalAlertScreen(
                    reminder = reminder,
                    invalid = invalid,
                    busy = saving,
                    error = error,
                    locked = keyguardShowing,
                    onDone = { respond(PersonalDeliveryContract.ACTION_DONE) },
                    onSnooze = { respond(PersonalDeliveryContract.ACTION_SNOOZE) },
                    onDismiss = { respond(PersonalDeliveryContract.ACTION_DISMISS) },
                    onClose = { finish() },
                    renderBackdrop = !backgroundProvided,
                )
                }
            }
        }
    }

    override fun onDestroy() {
        PersonalAlertSurfaceRegistry.detach(this)
        super.onDestroy()
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
    locked: Boolean = false,
    onDone: () -> Unit,
    onSnooze: () -> Unit,
    onDismiss: () -> Unit,
    onClose: () -> Unit,
    renderBackdrop: Boolean = true,
) {
    val context = LocalContext.current
    val family = remember(context) {
        P10ReminderFamilyStore.selected(context, P1ReminderScope.PERSONAL)
    }
    val look = family.actionPalette()
    val compact = LocalConfiguration.current.screenHeightDp < 720
    val scheduledLabel = reminder?.pendingOccurrenceMillis?.takeIf { it > 0L }?.let {
        DateTimeFormatter.ofPattern("EEE, d MMM · h:mm a", Locale.getDefault())
            .format(Instant.ofEpochMilli(it).atZone(reminder.zoneId))
    }
    // P10C: this main act uses the same living family as the new entrance.
    // All lock privacy, invalid/posted occurrence and action callbacks remain intact.
    Box(
        modifier = Modifier.fillMaxSize()
            .background(if (renderBackdrop) look.black else Color.Transparent)
            .testTag("v2233-personal-alert-screen"),
    ) {
        if (renderBackdrop) P10ReminderMainAtmosphere(
            scope = P1ReminderScope.PERSONAL,
            modifier = Modifier.fillMaxSize(),
        )
        Column(
            modifier = Modifier.fillMaxSize()
                .verticalScroll(rememberScrollState())
                .systemBarsPadding()
                .padding(horizontal = 22.dp, vertical = if (compact) 10.dp else 16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(if (compact) 9.dp else 13.dp),
        ) {
            Text(
                "BACKLOT  /  PERSONAL REMINDERS",
                color = look.ivory,
                fontSize = 10.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 1.7.sp,
                modifier = Modifier.align(Alignment.Start),
            )
            Text(
                when (reminder?.alert) {
                    PersonalAlert.VOICE -> "VOICE"
                    PersonalAlert.ALARM -> "ALARM"
                    else -> "PERSONAL"
                },
                color = look.gold,
                fontWeight = FontWeight.Bold,
                fontSize = 10.sp,
                letterSpacing = 3.sp,
            )
            if (locked) {
                // Keep existing over-keyguard privacy: no title, notes or
                // response actions until Android reports the keyguard unlocked.
                V2233PersonalParticleWave(PersonalAlert.ALARM)
                Text("PERSONAL ALARM", color = look.hot,
                    fontSize = 12.sp, fontWeight = FontWeight.Bold)
                Text("Unlock your phone to view and respond to this reminder.",
                    color = look.ivory, fontSize = 19.sp, lineHeight = 26.sp,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.testTag("v279-locked-generic-alarm"))
                Spacer(Modifier.height(12.dp))
                return@Column
            }
            if (reminder == null) {
                Spacer(Modifier.height(55.dp))
                if (invalid) {
                    Text("Reminder no longer active", color = look.ivory,
                        fontSize = 21.sp, fontWeight = FontWeight.Bold)
                    Text("It may have been completed, changed or paused.",
                        color = look.muted, textAlign = TextAlign.Center)
                    OutlinedButton(onClick = onClose) { Text("CLOSE") }
                } else {
                    CircularProgressIndicator(color = look.hot)
                }
                return@Column
            }
            // The optional companion appears only AFTER the existing
            // locked/invalid checks. Never show private decoration or
            // response affordances over the keyguard.
            Box(
                Modifier.fillMaxWidth()
                    .height(if (compact) 176.dp else 220.dp),
                contentAlignment = Alignment.Center,
            ) {
                V2233PersonalParticleWave(reminder.alert)
                V407ReminderCompanionMoment(
                    occurrenceKey = "personal-${reminder.id}-${reminder.pendingToken}",
                    modifier = Modifier.align(Alignment.BottomEnd)
                        .padding(end = 7.dp, bottom = 7.dp),
                )
            }
            Text(
                "PERSONAL REMINDER",
                color = look.hot,
                fontWeight = FontWeight.Bold, fontSize = 10.sp, letterSpacing = 2.4.sp,
            )
            Text(
                reminder.title, color = look.ivory,
                fontSize = if (compact) 22.sp else 26.sp,
                fontWeight = FontWeight.Bold, lineHeight = if (compact) 29.sp else 34.sp,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth().testTag("v2233-personal-alert-title"),
            )
            // P10C: one calm information stage; no two competing legacy cards.
            Surface(
                modifier = Modifier.fillMaxWidth().testTag("p10-personal-info-stage"),
                color = look.panel,
                border = BorderStroke(1.dp, look.line),
                shape = RoundedCornerShape(24.dp),
            ) {
                Column(Modifier.padding(horizontal = 19.dp, vertical = 16.dp)) {
                    if (scheduledLabel != null) {
                        Column(Modifier.testTag("v336-personal-schedule")) {
                            Text("SCHEDULED", color = look.hot,
                                fontSize = 10.sp, fontWeight = FontWeight.Bold,
                                letterSpacing = 1.5.sp)
                            Spacer(Modifier.height(6.dp))
                            Text(scheduledLabel, color = look.ivory,
                                fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
                        }
                        Spacer(Modifier.height(13.dp))
                        Box(Modifier.fillMaxWidth().height(1.dp)
                            .background(look.line.copy(alpha = .55f)))
                        Spacer(Modifier.height(13.dp))
                    }
                    Column(Modifier.testTag("v336-personal-reminder-status")) {
                        Text(
                            when (reminder.alert) {
                                PersonalAlert.ALARM -> "ALARM"
                                PersonalAlert.VOICE -> "ANNOUNCEMENT"
                                PersonalAlert.NOTIFICATION -> "NOTIFICATION"
                            },
                            color = look.hot,
                            fontSize = 10.sp, fontWeight = FontWeight.Bold,
                            letterSpacing = 1.5.sp,
                        )
                        Spacer(Modifier.height(6.dp))
                        Text(
                            when (reminder.alert) {
                                PersonalAlert.ALARM ->
                                    "Ringing stops after ${reminder.alarmDurationSeconds} seconds. Your response remains available."
                                PersonalAlert.VOICE ->
                                    "Spoken ${reminder.voiceRepeatCount} ${if (reminder.voiceRepeatCount == 1) "time" else "times"} if on-device speech is available."
                                PersonalAlert.NOTIFICATION -> "Your personal reminder is ready."
                            },
                            color = look.ivory, fontSize = 12.sp, lineHeight = 18.sp,
                        )
                        if (reminder.id.startsWith(PersonalDietFitnessPlanV1.ID_PREFIX) &&
                            reminder.notes.isNotBlank()
                        ) {
                            Spacer(Modifier.height(9.dp))
                            Text(reminder.notes, color = look.muted,
                                fontSize = 13.sp, lineHeight = 19.sp,
                                modifier = Modifier.testTag("v284-plan-alert-details"))
                        }
                    }
                }
            }
            Spacer(Modifier.height(if (compact) 3.dp else 7.dp))
            Button(
                onClick = onDone, enabled = !busy,
                modifier = Modifier.fillMaxWidth().heightIn(min = 54.dp)
                    .testTag("v2233-personal-done"),
                colors = ButtonDefaults.buttonColors(
                    containerColor = look.gold,
                    contentColor = look.black,
                ),
                shape = RoundedCornerShape(26.dp),
            ) {
                Text("DONE", color = look.black,
                    fontWeight = FontWeight.Bold, fontSize = 14.sp)
            }
            OutlinedButton(
                onClick = onSnooze, enabled = !busy,
                modifier = Modifier.fillMaxWidth().heightIn(min = 54.dp)
                    .testTag("v2233-personal-snooze"),
                border = BorderStroke(1.dp, look.gold.copy(alpha = .72f)),
                shape = RoundedCornerShape(26.dp),
            ) {
                Text(
                    "SNOOZE ${reminder.snoozeMinutes} MINUTES",
                    color = look.ivory,
                    fontWeight = FontWeight.SemiBold,
                )
            }
            TextButton(
                onClick = onDismiss, enabled = !busy,
                modifier = Modifier.testTag("v2233-personal-dismiss"),
            ) {
                Text("DISMISS WITHOUT COMPLETING",
                    color = look.hot, fontSize = 12.sp)
            }
            if (error != null) {
                Text(error, color = look.hot,
                    textAlign = TextAlign.Center)
            }
            Text(
                "Done records an explicit completion. Dismiss does not.",
                color = look.muted,
                fontSize = 11.sp,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth()
                    .testTag("ux06e-personal-action-meaning"),
            )
            if (reminder.alert != PersonalAlert.NOTIFICATION) {
                V407ReminderAccessDisclosure(
                    owner = V407ReminderOwner.PERSONAL,
                    kind = if (reminder.alert == PersonalAlert.VOICE)
                        ReminderCheckInKind.VOICE else ReminderCheckInKind.ALARM,
                    modifier = Modifier.fillMaxWidth(),
                )
            }
            Spacer(Modifier.height(18.dp))
        }
    }
}

/**
 * Kept under the existing public-internal name and UI tag so prior tests,
 * reminder actions and screen routing are completely unchanged.
 */
@Composable
internal fun V2233PersonalParticleWave(alert: PersonalAlert) {
    // The original UI tag is retained for test compatibility; the rendered
    // art is the new P10 optical-weight emblem, not the old V336 signal.
    val context = LocalContext.current
    val family = remember(context) {
        P10ReminderFamilyStore.selected(context, P1ReminderScope.PERSONAL)
    }
    val compact = LocalConfiguration.current.screenHeightDp < 720
    Box(
        modifier = Modifier.fillMaxWidth()
            .height(if (compact) 176.dp else 220.dp)
            .testTag("v2233-particle-wave"),
        contentAlignment = Alignment.Center,
    ) {
        P10ReminderIdentityEmblem(
            kind = if (alert == PersonalAlert.VOICE)
                ReminderCheckInKind.VOICE else ReminderCheckInKind.ALARM,
            family = family,
            modifier = Modifier.size(if (compact) 156.dp else 194.dp),
        )
    }
}
