package com.framebynavin.app.reminders

import android.animation.ValueAnimator
import android.app.ActivityManager
import android.content.Context
import android.os.PowerManager
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.onClick
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/**
 * Staged P10 presentation-only host: has no notification, alarm, TTS, routing,
 * receipt, completion, permission or service ownership. Live call sites are
 * unchanged until a separate gated cutover.
 */
@Composable
internal fun P10ReminderEntranceStage(
    scope: P1ReminderScope,
    kind: ReminderCheckInKind,
    title: String,
    occurrenceKey: String,
    locked: Boolean,
    media: P1Media?,
    onOpen: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val family = remember(context, scope) { P10ReminderFamilyStore.selected(context, scope) }
    val animationsAllowed = remember { ValueAnimator.areAnimatorsEnabled() }
    val batterySaver = remember(context) {
        context.getSystemService(PowerManager::class.java)?.isPowerSaveMode == true
    }
    val lowRam = remember(context) {
        context.getSystemService(ActivityManager::class.java)?.isLowRamDevice == true
    }
    val mediaVisible = P10ReminderVisualPolicy.visibleMedia(locked, media != null)
    val domainName = when (scope) {
        P1ReminderScope.CREATOR -> "CREATOR REMINDER"
        P1ReminderScope.PERSONAL -> "PERSONAL REMINDER"
        P1ReminderScope.CHALLENGE_21 -> "21-DAY CHALLENGE"
    }
    Box(
        modifier.fillMaxSize()
            .background(family.pigments().dark)
            .testTag("p10-entrance-" + scope.name.lowercase())
            .pointerInput(occurrenceKey) {
                detectTapGestures(onDoubleTap = { onOpen() })
            }
            .semantics {
                role = Role.Button
                contentDescription = if (locked)
                    "Backlot reminder. Unlock to view details."
                    else "Open " + domainName.lowercase()
                onClick(label = "Open reminder") {
                    // Accessibility activation is an explicit single action.
                    onOpen()
                    true
                }
            },
    ) {
        if (mediaVisible) {
            P1EntranceBackdrop(
                domain = scope,
                motionEnabled = animationsAllowed,
                lowPower = batterySaver || lowRam,
                modifier = Modifier.fillMaxSize(),
                overrideMedia = requireNotNull(media),
            )
            Box(
                Modifier.fillMaxSize().background(
                    Brush.verticalGradient(
                        listOf(
                            Color.Black.copy(alpha = .32f),
                            family.pigments().dark.copy(alpha = .26f),
                            Color.Black.copy(alpha = .78f),
                        ),
                    ),
                ),
            )
        } else {
            P10LivingReminderBackdrop(
                family = family,
                act = P10ReminderAct.ENTRANCE,
                animationsAllowed = animationsAllowed,
                batterySaver = batterySaver,
                lowRam = lowRam,
                modifier = Modifier.fillMaxSize(),
            )
        }

        Column(
            modifier = Modifier.fillMaxSize().systemBarsPadding()
                .padding(horizontal = 24.dp, vertical = 28.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceBetween,
        ) {
            Text(
                "BACKLOT / " + domainName,
                color = Color.White,
                fontSize = 11.sp,
                fontWeight = FontWeight.SemiBold,
                letterSpacing = 1.2.sp,
                modifier = Modifier.fillMaxWidth(),
            )
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(14.dp),
            ) {
                P10ReminderIdentityEmblem(
                    kind = kind, family = family,
                    modifier = Modifier.size(206.dp),
                )
                Text(
                    if (locked) "Unlock to open your reminder" else title,
                    color = Color.White,
                    fontSize = 24.sp,
                    lineHeight = 30.sp,
                    fontWeight = FontWeight.SemiBold,
                    textAlign = TextAlign.Center,
                    maxLines = 3,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    if (locked) "YOUR REMINDER IS READY"
                    else if (kind == ReminderCheckInKind.VOICE) "VOICE REMINDER" else "ALARM REMINDER",
                    color = family.pigments().warm,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.SemiBold,
                    letterSpacing = 1.7.sp,
                )
            }
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    if (locked) "UNLOCK TO CONTINUE" else "DOUBLE TAP TO OPEN",
                    color = Color.White,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.7.sp,
                )
                Spacer(Modifier.height(8.dp))
                Text(
                    "Or activate Open reminder with accessibility controls",
                    color = Color.White.copy(alpha = .70f),
                    fontSize = 10.sp,
                    textAlign = TextAlign.Center,
                )
            }
        }
    }
}

/**
 * The action owners will place their existing, unchanged responsive controls on
 * top of this phase-two atmosphere during P10C cutover.
 */
@Composable
internal fun P10ReminderMainAtmosphere(
    scope: P1ReminderScope,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val family = remember(context, scope) { P10ReminderFamilyStore.selected(context, scope) }
    val animationsAllowed = remember { ValueAnimator.areAnimatorsEnabled() }
    val batterySaver = remember(context) {
        context.getSystemService(PowerManager::class.java)?.isPowerSaveMode == true
    }
    val lowRam = remember(context) {
        context.getSystemService(ActivityManager::class.java)?.isLowRamDevice == true
    }
    P10LivingReminderBackdrop(
        family = family,
        act = P10ReminderAct.MAIN,
        animationsAllowed = animationsAllowed,
        batterySaver = batterySaver,
        lowRam = lowRam,
        modifier = modifier.fillMaxSize(),
    )
}
