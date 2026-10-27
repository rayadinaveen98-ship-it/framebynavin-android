package com.framebynavin.app.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.framebynavin.app.data.V221CompanionDecision
import com.framebynavin.app.data.V221CompanionExpression
import com.framebynavin.app.data.V221CompanionMoment
import com.framebynavin.app.ui.theme.CinemaLine
import com.framebynavin.app.ui.theme.CinemaSurface
import com.framebynavin.app.ui.theme.MutedGold
import com.framebynavin.app.ui.theme.MutedText
import com.framebynavin.app.ui.theme.ProjectorIvory

internal data class V221CompanionPresentation(
    val label: String,
    val state: CinePulseState,
    val message: String,
    val reason: String,
)

/** A different tone per selected companion, but the same verifiable facts. */
internal object V221CompanionPresentationPolicy {
    fun present(
        decision: V221CompanionDecision,
        identity: V144GuideIdentity,
    ): V221CompanionPresentation {
        val label = when (identity) {
            V144GuideIdentity.FRAME -> "NEXT STEP"
            V144GuideIdentity.NAVI -> "WORTH A LOOK"
            V144GuideIdentity.FUNNY -> "ON YOUR RADAR"
            V144GuideIdentity.CUTE -> "A GENTLE NUDGE"
        }
        val state = when (decision.expression) {
            V221CompanionExpression.ATTENTION -> CinePulseState.LISTEN
            V221CompanionExpression.FOCUS -> CinePulseState.PRESENT
            V221CompanionExpression.THINK -> CinePulseState.THINK
            V221CompanionExpression.CELEBRATE -> CinePulseState.SUCCESS
        }
        // Critical warnings do not change wording for a more playful identity.
        return V221CompanionPresentation(label, state, decision.message, decision.reason)
    }
}

/**
 * Reusable production Compose surface for V221.4 screen integration.
 * It does NOT appear in Home or Settings automatically in V221.2.
 * Parent routes the action; this card never schedules or edits creator data.
 */
@Composable
internal fun V221CompanionSuggestionCard(
    decision: V221CompanionDecision,
    identity: V144GuideIdentity,
    onAction: (V221CompanionDecision) -> Unit,
    onDismiss: (V221CompanionDecision) -> Unit,
    modifier: Modifier = Modifier,
) {
    val display = V221CompanionPresentationPolicy.present(decision, identity)
    var showReason by remember(decision.id) { mutableStateOf(false) }
    Surface(
        modifier = modifier.fillMaxWidth().testTag("v2212-companion-card"),
        color = CinemaSurface,
        shape = RoundedCornerShape(18.dp),
        border = BorderStroke(1.dp, CinemaLine),
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(7.dp),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                V144GuideCharacter(
                    guide = identity,
                    state = display.state,
                    modifier = Modifier.size(54.dp).testTag("v2212-companion-character"),
                )
                Spacer(Modifier.width(10.dp))
                Column(Modifier.weight(1f)) {
                    Text(
                        text = "${identity.displayName.uppercase()} · ${display.label}",
                        color = MutedGold,
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Bold,
                    )
                    Text(
                        text = display.message,
                        color = ProjectorIvory,
                        fontSize = 12.sp,
                        lineHeight = 17.sp,
                        maxLines = 3,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.testTag("v2212-companion-message"),
                    )
                }
            }
            AnimatedVisibility(visible = showReason) {
                Text(
                    text = display.reason,
                    color = MutedText,
                    fontSize = 10.sp,
                    lineHeight = 14.sp,
                    modifier = Modifier.testTag("v2212-companion-reason"),
                )
            }
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(5.dp),
            ) {
                if (decision.actionLabel != null) {
                    OutlinedButton(
                        onClick = { onAction(decision) },
                        modifier = Modifier.testTag("v2212-companion-action"),
                    ) { Text(decision.actionLabel, fontSize = 10.sp) }
                }
                TextButton(
                    onClick = { showReason = !showReason },
                    modifier = Modifier.testTag("v2212-companion-why"),
                ) { Text(if (showReason) "Less" else "Why?", fontSize = 10.sp) }
                Spacer(Modifier.weight(1f))
                TextButton(
                    onClick = { onDismiss(decision) },
                    modifier = Modifier.testTag("v2212-companion-dismiss"),
                ) { Text("Not now", fontSize = 10.sp) }
            }
        }
    }
}
