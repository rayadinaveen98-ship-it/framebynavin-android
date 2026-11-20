package com.framebynavin.app.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.ArrowBack
import androidx.compose.material.icons.outlined.ArrowForward
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.framebynavin.app.ui.theme.MutedText
import com.framebynavin.app.ui.theme.ProjectorIvory
import com.framebynavin.app.ui.theme.RecRed

/**
 * UX11-F: a non-numbered, deliberately compact presentation for the EXISTING
 * four-state PersonalReminder editor. No new state, draft, persistence,
 * validation, notification, account or API. Existing editor owns all of them.
 */
internal object UX11FComposerPresentationPolicy {
    val titles = listOf("Purpose", "Schedule", "Alert", "Review")
    val prompts = listOf(
        "Start with one thing worth remembering.",
        "Choose when Backlot should remind you.",
        "Choose how your reminder reaches you.",
        "Check the details before saving.",
    )
    fun canReturn(index: Int, current: Int, saving: Boolean): Boolean =
        !saving && current in titles.indices && index in 0 until current
}

/**
 * Replace the old numbered 1/2/3/4 chips AND STEP n OF 4 copy.
 * Four simple text labels, a quiet progress line and readable step guidance.
 * A visited label can return to its section; future labels never skip
 * required validation, and the current label does not reset editor state.
 */
@Composable
internal fun UX11FPersonalComposerProgress(
    currentStep: Int,
    saving: Boolean,
    onReturnTo: (Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    require(currentStep in UX11FComposerPresentationPolicy.titles.indices)
    val titles = UX11FComposerPresentationPolicy.titles
    val softRose = Color(0xFFEAC3BB)
    val rail = Color(0xFF4F3A42)
    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(11.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth()
                .testTag("ux11f-personal-progress-rail"),
            horizontalArrangement = Arrangement.spacedBy(5.dp),
        ) {
            titles.forEachIndexed { index, _ ->
                Box(
                    Modifier.weight(1f).height(3.dp)
                        .padding(horizontal = 0.dp)
                        .then(
                            Modifier
                                .testTag("ux11f-progress-segment-" + titles[index].lowercase())
                        ),
                ) {
                    Surface(
                        modifier = Modifier.fillMaxWidth().height(3.dp),
                        color = if (index <= currentStep) RecRed else rail,
                        shape = RoundedCornerShape(4.dp),
                    ) {}
                }
            }
        }
        Row(
            modifier = Modifier.fillMaxWidth()
                .horizontalScroll(rememberScrollState())
                .testTag("ux11f-personal-text-only-steps"),
            horizontalArrangement = Arrangement.spacedBy(7.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            titles.forEachIndexed { index, title ->
                val isCurrent = index == currentStep
                val canReturn = UX11FComposerPresentationPolicy.canReturn(
                    index, currentStep, saving
                )
                val base = Modifier
                    .heightIn(min = 48.dp)
                    .semantics {
                        stateDescription = if (isCurrent) "Current section"
                            else if (index < currentStep) "Completed section"
                            else "Upcoming section"
                    }
                    .testTag("ux11f-section-" + title.lowercase())
                Surface(
                    modifier = if (canReturn) base.clickable { onReturnTo(index) } else base,
                    shape = RoundedCornerShape(13.dp),
                    color = if (isCurrent) Color(0xFF342329) else Color(0xFF211E23),
                    border = BorderStroke(
                        1.dp,
                        if (isCurrent) RecRed.copy(alpha = .82f)
                        else Color(0xFF4C4044).copy(alpha = .55f)
                    ),
                ) {
                    Box(
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
                        contentAlignment = Alignment.Center,
                    ) {
                        Text(
                            title,
                            color = if (isCurrent) ProjectorIvory
                                else if (index < currentStep) softRose else MutedText,
                            fontWeight = if (isCurrent) FontWeight.Bold
                                else FontWeight.Medium,
                            fontSize = 12.sp,
                            maxLines = 1,
                        )
                    }
                }
            }
        }
        Text(
            UX11FComposerPresentationPolicy.prompts[currentStep],
            color = softRose.copy(alpha = .90f),
            fontSize = 12.sp,
            lineHeight = 18.sp,
            modifier = Modifier.testTag("ux11f-step-context"),
        )
    }
}

/**
 * Owner callback semantics and existing test tags remain identical to
 * V296PersonalStepActions; only footer spacing/visual hierarchy changes.
 * Continue remains the one primary action. Back (Cancel on first section)
 * remains fully accessible rather than a huge competing bordered button.
 */
@Composable
internal fun UX11FPersonalComposerFooter(
    step: Int,
    saving: Boolean,
    onBack: () -> Unit,
    onContinue: () -> Unit,
) {
    require(step in 0..2)
    Column(
        modifier = Modifier.fillMaxWidth().padding(top = 11.dp)
            .testTag("v296-composer-footer"),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(5.dp),
    ) {
        Button(
            onClick = onContinue,
            enabled = !saving,
            shape = RoundedCornerShape(16.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = RecRed,
                contentColor = Color.White,
            ),
            modifier = Modifier.fillMaxWidth().heightIn(min = 54.dp)
                .testTag("v296-composer-next"),
        ) {
            Text(
                if (saving) "WORKING…" else "CONTINUE",
                fontWeight = FontWeight.Bold,
                fontSize = 13.sp,
            )
            Spacer(Modifier.padding(horizontal = 4.dp))
            Icon(Icons.Outlined.ArrowForward, contentDescription = null)
        }
        TextButton(
            onClick = onBack,
            enabled = !saving,
            modifier = Modifier.heightIn(min = 48.dp)
                .testTag("v296-composer-back"),
        ) {
            if (step > 0) {
                Icon(Icons.Outlined.ArrowBack, contentDescription = null)
            }
            Text(
                if (step == 0) "CANCEL" else "BACK",
                color = ProjectorIvory,
                fontWeight = FontWeight.SemiBold,
                fontSize = 12.sp,
            )
        }
    }
}
