package com.framebynavin.app.ui

import android.animation.ValueAnimator
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.AutoAwesome
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.framebynavin.app.data.CreatorRewardLedgerEntry
import com.framebynavin.app.ui.theme.CinemaSurfaceRaised
import com.framebynavin.app.ui.theme.MutedGold
import com.framebynavin.app.ui.theme.MutedText
import com.framebynavin.app.ui.theme.ProjectorIvory
import kotlinx.coroutines.delay

/**
 * UX08C: Short, non-interactive Creator celebration.
 *
 * Input MUST be the in-memory new-credit feedback from CreatorViewModel,
 * never the restored ledger or Progress Journey projection. Nothing here
 * records or awards XP. No new notification, sound, haptics or persistent data.
 *
 * The overlay is always composed even with no reward: its bounded saveable
 * seen-key list survives Activity recreation and suppresses celebration replay.
 */
@Composable
internal fun UX08CCompanionCelebration(
    creditedFeedback: CreatorRewardLedgerEntry?,
    suppress: Boolean,
    modifier: Modifier = Modifier,
) {
    var seenKeys by rememberSaveable { mutableStateOf(arrayListOf<String>()) }
    var displayed by remember { mutableStateOf<CreatorRewardLedgerEntry?>(null) }
    var visible by remember { mutableStateOf(false) }

    LaunchedEffect(creditedFeedback?.eventKey, suppress) {
        visible = false
        displayed = null
        val entry = creditedFeedback ?: return@LaunchedEffect
        if (!UX08CCelebrationPolicy.verifiedFeedback(
                entry, System.currentTimeMillis()
            ) || entry.eventKey in seenKeys
        ) return@LaunchedEffect

        // Even when a modal reminder or guided tour has priority, mark this
        // event observed rather than suddenly replaying an old animation later.
        seenKeys = ArrayList(
            UX08CCelebrationPolicy.addSeenKey(seenKeys, entry.eventKey)
        )
        if (suppress) return@LaunchedEffect

        displayed = entry
        visible = true
        val animationsEnabled = ValueAnimator.areAnimatorsEnabled()
        // Reduced-motion mode still holds the static celebration for ~3 sec.
        delay(
            UX08CCelebrationPolicy.VISIBLE_HOLD_MS +
                if (animationsEnabled) 0L else UX08CCelebrationPolicy.EXIT_MS
        )
        visible = false
        if (animationsEnabled) delay(UX08CCelebrationPolicy.EXIT_MS)
        displayed = null
    }

    val animatorEnabled = ValueAnimator.areAnimatorsEnabled()
    val enter = if (animatorEnabled)
        fadeIn(tween(250)) + scaleIn(
            initialScale = 0.93f,
            animationSpec = tween(280, easing = FastOutSlowInEasing),
        )
    else EnterTransition.None
    val exit = if (animatorEnabled)
        fadeOut(tween(350)) + scaleOut(
            targetScale = 0.97f,
            animationSpec = tween(350, easing = FastOutSlowInEasing),
        )
    else ExitTransition.None

    AnimatedVisibility(
        visible = visible && displayed != null,
        enter = enter,
        exit = exit,
        modifier = modifier.testTag("ux08c-companion-celebration"),
    ) {
        displayed?.let { reward ->
            val companion = V144GuidePrefs.currentGuide
            Surface(
                modifier = Modifier.widthIn(max = 315.dp).semantics {
                    liveRegion = LiveRegionMode.Polite
                    contentDescription =
                        "${companion.displayName} celebrates: ${reward.label}; +${reward.xp} Creator XP"
                },
                color = CinemaSurfaceRaised.copy(alpha = 0.97f),
                shape = RoundedCornerShape(20.dp),
                border = BorderStroke(1.dp, MutedGold.copy(alpha = 0.35f)),
                shadowElevation = 12.dp,
            ) {
                Row(
                    Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    // Existing approved Spark/Mira CELEBRATE sprite. Use its
                    // static master instead if Android animations are disabled.
                    if (animatorEnabled) {
                        V144GuideCharacter(
                            guide = companion,
                            state = CinePulseState.CELEBRATE,
                            modifier = Modifier.size(width = 98.dp, height = 112.dp),
                        )
                    } else {
                        Image(
                            painter = painterResource(
                                v144GuideDrawable(companion, CinePulseState.CELEBRATE)
                            ),
                            contentDescription = null,
                            contentScale = ContentScale.Fit,
                            modifier = Modifier.size(width = 98.dp, height = 112.dp),
                        )
                    }
                    Column(
                        Modifier.weight(1f),
                        verticalArrangement = Arrangement.spacedBy(4.dp),
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp),
                        ) {
                            Icon(
                                Icons.Outlined.AutoAwesome,
                                contentDescription = null,
                                tint = MutedGold,
                                modifier = Modifier.size(15.dp),
                            )
                            Text(
                                "NICELY DONE",
                                color = MutedGold, fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                            )
                        }
                        Text(
                            reward.label,
                            color = ProjectorIvory,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.SemiBold,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis,
                        )
                        Text(
                            "+${reward.xp} Creator XP",
                            color = MutedText, fontSize = 11.sp,
                        )
                        Text(
                            "${companion.displayName} is celebrating with you",
                            color = MutedText, fontSize = 10.sp,
                            maxLines = 2,
                        )
                    }
                }
            }
        }
    }
}
