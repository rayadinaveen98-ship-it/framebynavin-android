package com.framebynavin.app.reminders

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.focusable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.onClick
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.semantics.testTag
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/**
 * RX01-C visual/gesture state machine; it is not an Android delivery engine.
 *
 * A real domain owner in RX01-D/E must first validate the current occurrence
 * and pass a cheap, synchronous validateOnOpen check. This module NEVER
 * creates a reminder or supplies a synthetic ID as proof of delivery.
 */
internal enum class RX01GatePhase {
    ENTRANCE,
    VERIFYING,
    EXITING,
    MAIN,
}

internal object RX01EntryGatePolicy {
    const val EXIT_DURATION_MS = 320

    fun canStart(
        occurrenceKey: String,
        active: Boolean,
        phase: RX01GatePhase,
        freshlyVerified: Boolean,
    ): Boolean =
        occurrenceKey.isNotBlank() &&
            active &&
            phase == RX01GatePhase.ENTRANCE &&
            freshlyVerified

    fun canFinish(
        occurrenceKey: String,
        active: Boolean,
        phase: RX01GatePhase,
        freshlyVerified: Boolean,
    ): Boolean =
        occurrenceKey.isNotBlank() &&
            active &&
            phase == RX01GatePhase.EXITING &&
            freshlyVerified

    fun exitDurationMillis(motionEnabled: Boolean, lowPower: Boolean): Int =
        if (motionEnabled && !lowPower) EXIT_DURATION_MS else 0

    fun accessibilityAction(domain: RX01ReminderDomain, mode: RX01ReminderIconKind): String =
        RX01ReminderIconPolicy.domainLabel(domain)
            .lowercase()
            .replaceFirstChar { it.uppercaseChar() } +
            ". " + RX01ReminderIconPolicy.accessibilityLabel(mode) +
            ". Open reminder."
}

/**
 * New reusable RX01-C visual wrapper, NOT called by current live Activities.
 *
 * Important invariants:
 * - ENTRANCE/EXITING compose NO mainContent at all; hidden Done/Snooze controls
 *   cannot receive touches or TalkBack actions before the gate succeeds.
 * - Standard touch requires a real double tap. Compose accessibility onClick
 *   is exposed only as a semantic action for TalkBack/switch access.
 *   Keyboard Enter/Space uses that same explicit accessible action.
 * - After a valid double tap the entrance scales and fades into a dark stage.
 *   Only AFTER the transition and a second current-occurrence check do the
 *   independently coloured MAIN backdrop and original mainContent appear.
 * - A temporary VERIFYING state blocks additional taps while IO confirms
 *   the current canonical token. No synchronous store reads block Compose.
 * - The current owner remains responsible for authentic occurrence validity,
 *   cancellation, foreground-service watchdog, notification fallback, audio,
 *   lock-screen privacy and all response semantics.
 * - An unverified/blank occurrence cannot enter MAIN.
 * - Recreating the Activity conservatively returns to ENTRANCE, even for the
 *   same occurrence; it never restores a possibly stale unlocked MAIN.
 */
@Composable
internal fun RX01ReminderExperienceGate(
    domain: RX01ReminderDomain,
    mode: RX01ReminderIconKind,
    occurrenceKey: String,
    occurrenceIsActive: Boolean,
    validateOnOpen: suspend () -> Boolean,
    motionEnabled: Boolean,
    lowPower: Boolean,
    onInvalidOccurrence: () -> Unit,
    modifier: Modifier = Modifier,
    mainContent: @Composable () -> Unit,
) {
    var phase by remember(occurrenceKey) { mutableStateOf(RX01GatePhase.ENTRANCE) }
    val currentValidator by rememberUpdatedState(validateOnOpen)
    val currentInvalidHandler by rememberUpdatedState(onInvalidOccurrence)
    val scope = rememberCoroutineScope()

    // Do not run storage reads inside a draw or every recomposition.
    // A suspend owner-supplied validator re-reads canonical state on IO, and
    // guards against stale tokens before and after the transition.
    val verify: suspend () -> Boolean = {
        occurrenceIsActive &&
            occurrenceKey.isNotBlank() &&
            runCatching { currentValidator() }.getOrDefault(false)
    }

    val requestOpen: () -> Unit = {
        // No storage I/O on the UI thread. The source owner re-reads canonical
        // data asynchronously before allowing an animated transition.
        if (phase == RX01GatePhase.ENTRANCE && occurrenceKey.isNotBlank() && occurrenceIsActive) {
            phase = RX01GatePhase.VERIFYING
            scope.launch {
                val current = verify()
                if (phase == RX01GatePhase.VERIFYING && occurrenceIsActive) {
                    if (RX01EntryGatePolicy.canStart(
                            occurrenceKey = occurrenceKey,
                            active = occurrenceIsActive,
                            phase = RX01GatePhase.ENTRANCE,
                            freshlyVerified = current,
                        )
                    ) {
                        phase = RX01GatePhase.EXITING
                    } else {
                        phase = RX01GatePhase.ENTRANCE
                        currentInvalidHandler()
                    }
                }
            }
        }
    }
    val currentRequestOpen by rememberUpdatedState(requestOpen)

    // An owner invalidation while the entrance or exit animation is displayed
    // must prevent the main UI from appearing. No completion receipt is issued.
    LaunchedEffect(occurrenceKey, occurrenceIsActive) {
        if (occurrenceKey.isBlank() || !occurrenceIsActive) {
            phase = RX01GatePhase.ENTRANCE
            currentInvalidHandler()
        }
    }

    val exitMillis = RX01EntryGatePolicy.exitDurationMillis(motionEnabled, lowPower)
    LaunchedEffect(occurrenceKey, phase, occurrenceIsActive, exitMillis) {
        if (phase == RX01GatePhase.EXITING && occurrenceIsActive) {
            if (exitMillis > 0) delay(exitMillis.toLong())
            if (RX01EntryGatePolicy.canFinish(
                    occurrenceKey = occurrenceKey,
                    active = occurrenceIsActive,
                    phase = phase,
                    freshlyVerified = verify(),
                )
            ) {
                phase = RX01GatePhase.MAIN
            } else {
                phase = RX01GatePhase.ENTRANCE
                currentInvalidHandler()
            }
        }
    }

    val exitAlpha by animateFloatAsState(
        targetValue = if (phase == RX01GatePhase.EXITING) 0f else 1f,
        animationSpec = tween(durationMillis = exitMillis),
        label = "rx01-entrance-fade",
    )
    val exitScale by animateFloatAsState(
        targetValue = if (phase == RX01GatePhase.EXITING) 1.055f else 1f,
        animationSpec = tween(durationMillis = exitMillis),
        label = "rx01-entrance-expand",
    )

    val mainBase = RX01FluidTheme.palette(domain, RX01SurfacePhase.MAIN).baseTop
    Box(
        modifier = modifier.fillMaxSize()
            .background(mainBase)
            .testTag("rx01-gate-" + domain.name.lowercase()),
    ) {
        when {
            !occurrenceIsActive || occurrenceKey.isBlank() -> {
                // Safe empty stage while the parent closes or displays its
                // original notification-fallback route.
            }
            phase == RX01GatePhase.MAIN -> {
                // Main content is instantiated only after BOTH validity checks.
                // Nothing in this wrapper fabricates/authorizes Done or Snooze.
                RX01FluidReminderBackdrop(
                    domain = domain,
                    phase = RX01SurfacePhase.MAIN,
                    motionEnabled = motionEnabled,
                    lowPower = lowPower,
                )
                mainContent()
            }
            else -> {
                Box(
                    modifier = Modifier.fillMaxSize()
                        .graphicsLayer {
                            alpha = exitAlpha
                            scaleX = exitScale
                            scaleY = exitScale
                        }
                        .testTag("rx01-gate-entrance-visual"),
                ) {
                    // RX02 swaps BACKGROUND ONLY. Original labels, verified
                    // Voice/Alarm optic and double-tap interaction remain
                    // entirely in RX01ReminderEntranceHero and this gate.
                    RX02EntranceBackdrop(
                        domain = domain,
                        motionEnabled = motionEnabled,
                        lowPower = lowPower,
                    )
                    RX01ReminderEntranceHero(
                        domain = domain,
                        mode = mode,
                        motionEnabled = motionEnabled,
                        lowPower = lowPower,
                    )
                }

                if (phase == RX01GatePhase.ENTRANCE) {
                    // This sits ABOVE all decorative layers. No hidden main
                    // response controls are even composed in this phase.
                    Box(
                        modifier = Modifier.fillMaxSize()
                            .pointerInput(occurrenceKey) {
                                detectTapGestures(onDoubleTap = {
                                    currentRequestOpen()
                                })
                            }
                            .onKeyEvent {
                                if (it.type == KeyEventType.KeyUp &&
                                    (it.key == Key.Enter ||
                                        it.key == Key.Spacebar ||
                                        it.key == Key.DirectionCenter)
                                ) {
                                    currentRequestOpen()
                                    true
                                } else {
                                    false
                                }
                            }
                            .focusable()
                            .clearAndSetSemantics {
                                // Put the test tag in the CLEARED semantics
                                // node itself: a prior testTag modifier would
                                // be erased, weakening UI regression evidence.
                                testTag = "rx01-entry-double-tap-surface"
                                role = Role.Button
                                contentDescription =
                                    RX01EntryGatePolicy.accessibilityAction(domain, mode)
                                stateDescription = "Ready to open"
                                onClick(label = "Open reminder") {
                                    currentRequestOpen()
                                    true
                                }
                            },
                    )
                }
            }
        }
    }
}
