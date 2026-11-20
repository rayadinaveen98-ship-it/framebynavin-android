package com.framebynavin.app.personal

import android.animation.ValueAnimator
import android.app.KeyguardManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.PowerManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.lifecycleScope
import com.framebynavin.app.MainActivity
import com.framebynavin.app.reminders.RX01FluidReminderBackdrop
import com.framebynavin.app.reminders.RX01FluidTheme
import com.framebynavin.app.reminders.RX01ReminderDomain
import com.framebynavin.app.reminders.RX01ReminderExperienceGate
import com.framebynavin.app.reminders.RX01ReminderIconKind
import com.framebynavin.app.reminders.RX01SurfacePhase
import com.framebynavin.app.ui.theme.FrameByNavinTheme
import java.time.Instant
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * RX01-E: a NON-EXPORTED presenter for an existing, actually posted Challenge
 * notification. No scheduling, audio, day-confirmation, streak, or cloud writes.
 * User-opened Voice and eligible user-opted-in ALARM FSI can reach the same UI.
 *
 * Quiet notifications retain the original in-app deep link.
 * Locked mode must not render challenge activity title, notes, or Done actions.
 */
internal class RX01ChallengeAlertActivity : ComponentActivity() {
    private var locked by mutableStateOf(true)
    private var occurrenceActive by mutableStateOf(true)
    private val id: String get() = intent.getStringExtra(Phase1HabitNotification.ID).orEmpty()
    private val at: Long get() = intent.getLongExtra(Phase1HabitNotification.AT, 0L)

    private fun refreshLocked() {
        locked = (getSystemService(Context.KEYGUARD_SERVICE) as? KeyguardManager)
            ?.isKeyguardLocked != false
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (!id.matches(Regex("[A-Za-z0-9_-]{1,100}")) || at <= 0L ||
            intent.action != ACTION_OPEN_VISUAL) {
            finish()
            return
        }
        // Only the notification publisher may request this extra on an
        // OS-authorized ALARM full-screen PendingIntent. Never for Voice.
        if (intent.getBooleanExtra(EXTRA_ALARM_FSI, false)) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O_MR1) {
                setShowWhenLocked(true)
                setTurnScreenOn(true)
            }
        }
        refreshLocked()
        val requestedId = id
        val requestedAt = at

        setContent {
            FrameByNavinTheme {
                var loaded by remember(requestedId, requestedAt) {
                    mutableStateOf<Phase1HabitChallenge?>(null)
                }
                var checking by remember(requestedId, requestedAt) { mutableStateOf(true) }
                var invalid by remember(requestedId, requestedAt) { mutableStateOf(false) }
                var requestingSnooze by remember { mutableStateOf(false) }
                var snoozeError by remember { mutableStateOf(false) }
                LaunchedEffect(requestedId, requestedAt) {
                    val current = withContext(Dispatchers.IO) {
                        runCatching {
                            Phase1HabitStore(applicationContext).load()
                                .firstOrNull { it.id == requestedId }
                                ?.takeIf {
                                    RX01ChallengePresentationPolicy.eligibleForVisual(
                                        requireNotNull(it.alertKind), requestedAt
                                    ) && Phase1HabitOccurrencePolicy.current(
                                        it, requestedAt, System.currentTimeMillis()
                                    )
                                }
                        }.getOrNull()
                    }
                    loaded = current
                    checking = false
                    invalid = current == null
                    occurrenceActive = current != null
                }

                val challenge = loaded
                when {
                    locked -> RX01ChallengeSafeSurface(
                        text = "Unlock your phone to open the 21-day reminder.",
                        onClose = { finish() },
                        showClose = true,
                    )
                    checking -> RX01ChallengeSafeSurface(
                        text = "Checking today's challenge reminder…",
                        onClose = { finish() },
                        showClose = false,
                    )
                    invalid || !occurrenceActive || challenge == null ->
                        RX01ChallengeSafeSurface(
                            text = "This 21-day reminder is no longer active.",
                            onClose = { finish() },
                            showClose = true,
                        )
                    else -> RX01ReminderExperienceGate(
                        domain = RX01ReminderDomain.CHALLENGE_21,
                        mode = if (challenge.alertKind == Phase1HabitAlertKind.ALARM)
                            RX01ReminderIconKind.ALARM else RX01ReminderIconKind.VOICE,
                        occurrenceKey = "challenge:" + requestedId + ":" + requestedAt,
                        occurrenceIsActive = occurrenceActive && !locked &&
                            !isFinishing && !isDestroyed,
                        validateOnOpen = {
                            withContext(Dispatchers.IO) {
                                val current = runCatching {
                                    Phase1HabitStore(applicationContext).load()
                                        .firstOrNull { it.id == requestedId }
                                }.getOrNull()
                                !locked && !isFinishing && !isDestroyed &&
                                    current == challenge &&
                                    current != null &&
                                    Phase1HabitOccurrencePolicy.current(
                                        current, requestedAt, System.currentTimeMillis()
                                    )
                            }
                        },
                        motionEnabled = ValueAnimator.areAnimatorsEnabled(),
                        lowPower = remember {
                            getSystemService(PowerManager::class.java)?.isPowerSaveMode == true
                        },
                        onInvalidOccurrence = {
                            invalid = true
                            loaded = null
                            occurrenceActive = false
                        },
                    ) {
                        RX01ChallengeMain(
                            challenge = challenge,
                            savingSnooze = requestingSnooze,
                            snoozeError = snoozeError,
                            onOpenChallenge = {
                                // The ORIGINAL challenge page still owns every
                                // Done receipt, day note and progress action.
                                startActivity(Intent(this@RX01ChallengeAlertActivity,
                                    MainActivity::class.java)
                                    .setAction(PersonalDeliveryContract.ACTION_OPEN_PERSONAL)
                                    .setData(Uri.parse("backlot-habit://open/" +
                                        Uri.encode(requestedId)))
                                    .addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP or
                                        Intent.FLAG_ACTIVITY_SINGLE_TOP))
                                finish()
                            },
                            onSnooze = {
                                if (!requestingSnooze) {
                                    requestingSnooze = true
                                    lifecycleScope.launch {
                                        val stillCurrent = withContext(Dispatchers.IO) {
                                            runCatching {
                                                Phase1HabitStore(applicationContext).load()
                                                    .firstOrNull { it.id == requestedId }
                                                    ?.let {
                                                        it == challenge &&
                                                            Phase1HabitOccurrencePolicy.current(
                                                                it, requestedAt,
                                                                System.currentTimeMillis()
                                                            )
                                                    } ?: false
                                            }.getOrDefault(false)
                                        }
                                        if (stillCurrent && !locked && !isFinishing) {
                                            // Request the exact EXISTING Snooze
                                            // receiver, which owns final CAS,
                                            // scheduling, notification and audio.
                                            sendBroadcast(Intent(
                                                this@RX01ChallengeAlertActivity,
                                                Phase1HabitSnoozeReceiver::class.java,
                                            ).setAction(Phase1HabitNotification.ACTION_SNOOZE)
                                                .setData(Uri.parse("backlot-habit://snooze/" +
                                                    Uri.encode(requestedId) + "/" + requestedAt))
                                                .putExtra(Phase1HabitNotification.ID, requestedId)
                                                .putExtra(Phase1HabitNotification.AT, requestedAt)
                                                .putExtra(Phase1HabitNotification.SNOOZE_MINUTES, 10))
                                            // Broadcast dispatch is NOT a saved
                                            // snooze receipt. Receiver verifies.
                                            finish()
                                        } else {
                                            requestingSnooze = false
                                            snoozeError = true
                                            invalid = true
                                            loaded = null
                                            occurrenceActive = false
                                        }
                                    }
                                }
                            },
                            onClose = { finish() },
                        )
                    }
                }
            }
        }
    }

    override fun onResume() {
        super.onResume()
        refreshLocked()
        val currentId = id
        val currentAt = at
        if (currentId.isNotBlank() && currentAt > 0L) {
            lifecycleScope.launch {
                val valid = withContext(Dispatchers.IO) {
                    runCatching {
                        Phase1HabitStore(applicationContext).load()
                            .firstOrNull { it.id == currentId }?.let {
                                Phase1HabitOccurrencePolicy.current(
                                    it, currentAt, System.currentTimeMillis()
                                )
                            } ?: false
                    }.getOrDefault(false)
                }
                if (!valid) occurrenceActive = false
            }
        }
    }

    override fun onWindowFocusChanged(hasFocus: Boolean) {
        super.onWindowFocusChanged(hasFocus)
        refreshLocked()
    }

    companion object {
        internal const val ACTION_OPEN_VISUAL =
            "com.framebynavin.app.personal.RX01_CHALLENGE_VISUAL"
        internal const val EXTRA_ALARM_FSI = "rx01.challenge.alarm_fsi"
    }
}

/** Generic even when a notification appears while the phone is locked. */
@Composable
private fun RX01ChallengeSafeSurface(
    text: String,
    onClose: () -> Unit,
    showClose: Boolean,
) {
    Box(Modifier.fillMaxSize().background(Color(0xFF120817))
        .testTag("rx01-challenge-privacy-state")) {
        RX01FluidReminderBackdrop(
            domain = RX01ReminderDomain.CHALLENGE_21,
            phase = RX01SurfacePhase.ENTRANCE,
            motionEnabled = false,
            lowPower = true,
        )
        Column(
            modifier = Modifier.align(Alignment.Center)
                .safeDrawingPadding().padding(horizontal = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text("21-DAY CHALLENGE", color = Color(0xFFFFD9AA),
                fontSize = 12.sp, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(24.dp))
            if (!showClose) CircularProgressIndicator(color = Color(0xFFFFB85D))
            Text(text, color = Color.White, fontSize = 20.sp,
                lineHeight = 28.sp, textAlign = TextAlign.Center,
                modifier = Modifier.padding(vertical = 22.dp))
            if (showClose) OutlinedButton(onClick = onClose) { Text("CLOSE") }
        }
    }
}

@Composable
private fun RX01ChallengeMain(
    challenge: Phase1HabitChallenge,
    savingSnooze: Boolean,
    snoozeError: Boolean,
    onOpenChallenge: () -> Unit,
    onSnooze: () -> Unit,
    onClose: () -> Unit,
) {
    val palette = RX01FluidTheme.palette(RX01ReminderDomain.CHALLENGE_21,
        RX01SurfacePhase.MAIN)
    Column(
        modifier = Modifier.fillMaxSize().safeDrawingPadding()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 24.dp, vertical = 20.dp)
            .testTag("rx01-challenge-main-actions"),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Text("BACKLOT / 21-DAY CHALLENGE", color = palette.iconHalo,
            fontSize = 11.sp, fontWeight = FontWeight.Bold,
            letterSpacing = 1.5.sp, textAlign = TextAlign.Center)
        Spacer(Modifier.height(28.dp))
        val number = challenge.dayNumber(Instant.now())
        Text(if (number == null) "YOUR COMMITMENT" else "DAY " + number + " OF 21",
            color = palette.iconTint, fontSize = 15.sp,
            fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(14.dp))
        Text(challenge.activity, color = palette.ink, fontSize = 27.sp,
            lineHeight = 35.sp, fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth())
        Spacer(Modifier.height(14.dp))
        Text("A reminder is not a completed day. Open your challenge to confirm your progress.",
            color = palette.ink.copy(alpha = .84f), fontSize = 13.sp,
            lineHeight = 20.sp, textAlign = TextAlign.Center)
        Spacer(Modifier.height(38.dp))
        Button(
            onClick = onOpenChallenge,
            modifier = Modifier.fillMaxWidth().heightIn(min = 54.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = Color(0xFFFFBA6D),
                contentColor = Color(0xFF231019),
            ),
        ) { Text("OPEN CHALLENGE", fontWeight = FontWeight.Bold) }
        Spacer(Modifier.height(12.dp))
        OutlinedButton(
            onClick = onSnooze,
            enabled = !savingSnooze,
            modifier = Modifier.fillMaxWidth().heightIn(min = 54.dp),
        ) { Text(if (savingSnooze) "CHECKING REMINDER…" else "SNOOZE 10 MIN") }
        if (snoozeError) {
            Text("The reminder changed. Please reopen it.",
                color = palette.iconTint, modifier = Modifier.padding(top = 10.dp))
        }
        Spacer(Modifier.height(10.dp))
        OutlinedButton(onClick = onClose) { Text("CLOSE REMINDER SCREEN") }
    }
}
