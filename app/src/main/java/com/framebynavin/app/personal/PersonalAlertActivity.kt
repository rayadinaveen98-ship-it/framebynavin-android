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
import com.framebynavin.app.reminders.V336DirectorBackdrop
import com.framebynavin.app.reminders.V336DirectorSignalArt
import com.framebynavin.app.reminders.V336DirectorPalette
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
                )
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
) {
    val context = LocalContext.current
    val motionEnabled = remember { ValueAnimator.areAnimatorsEnabled() }
    val lowPower = remember(context) {
        context.getSystemService(PowerManager::class.java)?.isPowerSaveMode == true
    }
    val compact = LocalConfiguration.current.screenHeightDp < 720
    val scheduledLabel = reminder?.pendingOccurrenceMillis?.takeIf { it > 0L }?.let {
        DateTimeFormatter.ofPattern("EEE, d MMM · h:mm a", Locale.getDefault())
            .format(Instant.ofEpochMilli(it).atZone(reminder.zoneId))
    }
    // The existing locked/invalid/loading presentation is always retained.
    // Never expose Personal titles or actions over keyguard, including in V394.
    val premiereKind = if (reminder?.alert == PersonalAlert.VOICE)
        ReminderCheckInKind.VOICE else ReminderCheckInKind.ALARM
    com.framebynavin.app.reminders.V394PremierePrefs.initialize(context)
    val premiereEligible = reminder?.alert == PersonalAlert.VOICE ||
        reminder?.alert == PersonalAlert.ALARM
    if (com.framebynavin.app.reminders.V394PremierePresentationPolicy.personal(
            enabled = com.framebynavin.app.reminders.V394PremierePrefs.enabled(premiereKind),
            locked = locked,
            validReminder = reminder != null && !invalid && premiereEligible,
        )
    ) {
        val active = requireNotNull(reminder)
        com.framebynavin.app.reminders.V394PremiereReminderScreen(
            mode = com.framebynavin.app.reminders.V394PremiereMode.PERSONAL,
            kind = premiereKind,
            title = active.title,
            timeLabel = scheduledLabel.orEmpty(),
            contextLabel = if (scheduledLabel != null)
                "SCHEDULED  /  " + scheduledLabel else "PERSONAL REMINDER",
            supportingText = when (active.alert) {
                PersonalAlert.ALARM -> "Alarm · " + active.alarmDurationSeconds + " seconds"
                PersonalAlert.VOICE -> "Voice · " + active.voiceRepeatCount + " announcements"
                PersonalAlert.NOTIFICATION -> "Personal notification"
            },
            primaryLabel = "DONE",
            snoozeLabel = "SNOOZE " + active.snoozeMinutes + " MIN",
            onPrimary = onDone,
            onSnooze = onSnooze,
            onDismiss = onDismiss,
            error = error,
            busy = busy,
        )
        return
    }
    Box(
        modifier = Modifier.fillMaxSize()
            .background(V336DirectorPalette.black)
            .testTag("v2233-personal-alert-screen"),
    ) {
        com.framebynavin.app.reminders.V391ReminderBackdrop(
            kind = if (reminder?.alert == PersonalAlert.VOICE)
                ReminderCheckInKind.VOICE else ReminderCheckInKind.ALARM,
            motionEnabled = motionEnabled, lowPower = lowPower,
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
                color = V336DirectorPalette.ivory,
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
                color = V336DirectorPalette.gold,
                fontWeight = FontWeight.Bold,
                fontSize = 10.sp,
                letterSpacing = 3.sp,
            )
            if (locked) {
                // Keep existing over-keyguard privacy: no title, notes or
                // response actions until Android reports the keyguard unlocked.
                V2233PersonalParticleWave(PersonalAlert.ALARM)
                Text("PERSONAL ALARM", color = V336DirectorPalette.hot,
                    fontSize = 12.sp, fontWeight = FontWeight.Bold)
                Text("Unlock your phone to view and respond to this reminder.",
                    color = V336DirectorPalette.ivory, fontSize = 19.sp, lineHeight = 26.sp,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.testTag("v279-locked-generic-alarm"))
                Spacer(Modifier.height(12.dp))
                return@Column
            }
            if (reminder == null) {
                Spacer(Modifier.height(55.dp))
                if (invalid) {
                    Text("Reminder no longer active", color = V336DirectorPalette.ivory,
                        fontSize = 21.sp, fontWeight = FontWeight.Bold)
                    Text("It may have been completed, changed or paused.",
                        color = V336DirectorPalette.muted, textAlign = TextAlign.Center)
                    OutlinedButton(onClick = onClose) { Text("CLOSE") }
                } else {
                    CircularProgressIndicator(color = V336DirectorPalette.hot)
                }
                return@Column
            }
            // Native Canvas: visually matches Creator, with the existing
            // Personal alert type and exact response callbacks unchanged.
            V2233PersonalParticleWave(reminder.alert)
            Text(
                "PERSONAL REMINDER",
                color = V336DirectorPalette.hot,
                fontWeight = FontWeight.Bold, fontSize = 10.sp, letterSpacing = 2.4.sp,
            )
            Text(
                reminder.title, color = V336DirectorPalette.ivory,
                fontSize = if (compact) 22.sp else 26.sp,
                fontWeight = FontWeight.Bold, lineHeight = if (compact) 29.sp else 34.sp,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth().testTag("v2233-personal-alert-title"),
            )
            if (scheduledLabel != null) {
                Surface(
                    modifier = Modifier.fillMaxWidth().testTag("v336-personal-schedule"),
                    color = V336DirectorPalette.panel,
                    border = BorderStroke(1.dp, V336DirectorPalette.line),
                    shape = RoundedCornerShape(20.dp),
                ) {
                    Column(Modifier.padding(horizontal = 17.dp, vertical = 13.dp)) {
                        Text("SCHEDULED", color = V336DirectorPalette.hot,
                            fontSize = 10.sp, fontWeight = FontWeight.Bold,
                            letterSpacing = 1.5.sp)
                        Spacer(Modifier.height(5.dp))
                        Text(scheduledLabel, color = V336DirectorPalette.ivory,
                            fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
                    }
                }
            }
            Surface(
                modifier = Modifier.fillMaxWidth().testTag("v336-personal-reminder-status"),
                color = V336DirectorPalette.panel,
                border = BorderStroke(1.dp, V336DirectorPalette.line),
                shape = RoundedCornerShape(20.dp),
            ) {
                Column(Modifier.padding(horizontal = 17.dp, vertical = 13.dp)) {
                    Text(
                        if (reminder.alert == PersonalAlert.ALARM) "ALARM"
                        else if (reminder.alert == PersonalAlert.VOICE) "ANNOUNCEMENT"
                        else "NOTIFICATION",
                        color = V336DirectorPalette.hot,
                        fontSize = 10.sp, fontWeight = FontWeight.Bold,
                        letterSpacing = 1.5.sp,
                    )
                    Spacer(Modifier.height(5.dp))
                    Text(
                        when (reminder.alert) {
                            PersonalAlert.ALARM ->
                                "Ringing stops after ${reminder.alarmDurationSeconds} seconds. Your response remains available."
                            PersonalAlert.VOICE ->
                                "Spoken ${reminder.voiceRepeatCount} ${if (reminder.voiceRepeatCount == 1) "time" else "times"} if on-device speech is available."
                            PersonalAlert.NOTIFICATION -> "Your personal reminder is ready."
                        },
                        color = V336DirectorPalette.ivory, fontSize = 12.sp,
                        lineHeight = 18.sp,
                    )
                    if (reminder.id.startsWith(PersonalDietFitnessPlanV1.ID_PREFIX) &&
                        reminder.notes.isNotBlank()
                    ) {
                        Spacer(Modifier.height(9.dp))
                        Text(
                            reminder.notes, color = V336DirectorPalette.muted,
                            fontSize = 13.sp, lineHeight = 19.sp,
                            modifier = Modifier.testTag("v284-plan-alert-details"),
                        )
                    }
                }
            }
            Spacer(Modifier.height(if (compact) 3.dp else 7.dp))
            Button(
                onClick = onDone, enabled = !busy,
                modifier = Modifier.fillMaxWidth().heightIn(min = 54.dp)
                    .testTag("v2233-personal-done"),
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color(0xFFE8B88E),
                    contentColor = V336DirectorPalette.black,
                ),
                shape = RoundedCornerShape(26.dp),
            ) {
                Text("DONE", color = V336DirectorPalette.black,
                    fontWeight = FontWeight.Bold, fontSize = 14.sp)
            }
            OutlinedButton(
                onClick = onSnooze, enabled = !busy,
                modifier = Modifier.fillMaxWidth().heightIn(min = 54.dp)
                    .testTag("v2233-personal-snooze"),
                border = BorderStroke(1.dp, V336DirectorPalette.gold.copy(alpha = .72f)),
                shape = RoundedCornerShape(26.dp),
            ) {
                Text(
                    "SNOOZE ${reminder.snoozeMinutes} MINUTES",
                    color = V336DirectorPalette.ivory,
                    fontWeight = FontWeight.SemiBold,
                )
            }
            TextButton(
                onClick = onDismiss, enabled = !busy,
                modifier = Modifier.testTag("v2233-personal-dismiss"),
            ) {
                Text("DISMISS WITHOUT COMPLETING",
                    color = V336DirectorPalette.hot, fontSize = 12.sp)
            }
            if (error != null) {
                Text(error, color = V336DirectorPalette.hot,
                    textAlign = TextAlign.Center)
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
    val motionEnabled = remember { ValueAnimator.areAnimatorsEnabled() }
    val context = LocalContext.current
    val lowPower = remember(context) {
        context.getSystemService(PowerManager::class.java)?.isPowerSaveMode == true
    }
    V336DirectorSignalArt(
        kind = if (alert == PersonalAlert.VOICE) ReminderCheckInKind.VOICE
            else ReminderCheckInKind.ALARM,
        motionEnabled = motionEnabled,
        lowPower = lowPower,
        modifier = Modifier.fillMaxWidth()
            .height(if (LocalConfiguration.current.screenHeightDp < 720) 176.dp else 220.dp)
            .testTag("v2233-particle-wave"),
    )
}
