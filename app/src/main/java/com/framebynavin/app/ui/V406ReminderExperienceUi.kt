package com.framebynavin.app.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.framebynavin.app.ui.theme.BacklotAccent
import com.framebynavin.app.ui.theme.BacklotBorder
import com.framebynavin.app.ui.theme.BacklotPrimaryText
import com.framebynavin.app.ui.theme.BacklotSecondaryText
import com.framebynavin.app.ui.theme.BacklotSurface
import com.framebynavin.app.ui.theme.BacklotSurfaceSelected

/**
 * UX06B: presentation-only navigation across each domain's real reminder agenda.
 * The host computes its counts from the existing Creator or Personal+Challenge
 * projection, and owns the callback. This component never reads or writes stores,
 * schedules alarms, acknowledges actions, or fabricates a unified account feed.
 */
internal class V406ReminderScopeAction(
    val id: String,
    val label: String,
    val count: Int?,
    val selected: Boolean,
    val onSelect: () -> Unit,
)

@Composable
internal fun V406ReminderScopeNavigator(
    actions: List<V406ReminderScopeAction>,
    modifier: Modifier = Modifier,
) {
    if (actions.isEmpty()) return
    Column(modifier.testTag("ux06b-scope-navigator"),
        verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(
            "FIND WHAT NEEDS YOU",
            color = BacklotSecondaryText,
            fontWeight = FontWeight.Bold,
            fontSize = 10.sp,
        )
        Row(
            Modifier.horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            actions.forEach { action ->
                // The selected/current tab is not a fake action; all other
                // options preserve their original owner's navigation callbacks.
                val tile: @Composable () -> Unit = {
                    Column(
                        Modifier.padding(horizontal = 13.dp, vertical = 10.dp)
                            .widthIn(min = 82.dp),
                        verticalArrangement = Arrangement.spacedBy(2.dp),
                    ) {
                        Text(
                            action.count?.toString() ?: "VIEW",
                            color = if (action.selected) BacklotAccent else BacklotPrimaryText,
                            fontSize = if (action.count != null) 19.sp else 11.sp,
                            fontWeight = FontWeight.Bold,
                        )
                        Text(
                            action.label,
                            color = if (action.selected) BacklotPrimaryText else BacklotSecondaryText,
                            fontWeight = if (action.selected) FontWeight.Bold else FontWeight.Medium,
                            fontSize = 11.sp,
                            maxLines = 1,
                        )
                    }
                }
                if (action.selected) {
                    Surface(
                        color = BacklotSurfaceSelected,
                        shape = RoundedCornerShape(15.dp),
                        border = BorderStroke(1.dp, BacklotAccent),
                        modifier = Modifier.heightIn(min = 64.dp)
                            .testTag("ux06b-scope-${action.id}"),
                        content = tile,
                    )
                } else {
                    Surface(
                        onClick = action.onSelect,
                        color = BacklotSurface,
                        shape = RoundedCornerShape(15.dp),
                        border = BorderStroke(1.dp, BacklotBorder),
                        modifier = Modifier.heightIn(min = 64.dp)
                            .testTag("ux06b-scope-${action.id}"),
                        content = tile,
                    )
                }
            }
        }
    }
}

/**
 * An accessible progress index for the existing four-step Personal composer.
 * Back-navigation to a completed step is allowed; skipping validation into a
 * future step is not. Edits still use the existing full-field editor unchanged.
 */
@Composable
internal fun V406PersonalComposerProgress(
    currentStep: Int,
    onReturnTo: (Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    if (currentStep !in 0..3) return
    val stages = listOf("Purpose", "Schedule", "Alert", "Review")
    Column(modifier.testTag("ux06b-personal-composer-progress"),
        verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Text(
            "STEP ${currentStep + 1} OF 4",
            color = BacklotSecondaryText,
            fontSize = 11.sp,
            fontWeight = FontWeight.SemiBold,
        )
        Row(
            Modifier.horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(7.dp),
        ) {
            stages.forEachIndexed { index, title ->
                val isCurrent = index == currentStep
                val content: @Composable () -> Unit = {
                    Text(
                        "${index + 1}  $title",
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
                        color = if (isCurrent) BacklotPrimaryText else BacklotSecondaryText,
                        fontWeight = if (isCurrent) FontWeight.Bold else FontWeight.Medium,
                        fontSize = 11.sp,
                        maxLines = 1,
                    )
                }
                if (index < currentStep) {
                    Surface(
                        onClick = { onReturnTo(index) },
                        color = BacklotSurface,
                        shape = RoundedCornerShape(12.dp),
                        border = BorderStroke(1.dp, BacklotBorder),
                        modifier = Modifier.heightIn(min = 44.dp)
                            .testTag("ux06b-composer-step-$index"),
                        content = content,
                    )
                } else {
                    Surface(
                        color = if (isCurrent) BacklotSurfaceSelected else BacklotSurface,
                        shape = RoundedCornerShape(12.dp),
                        border = BorderStroke(
                            1.dp, if (isCurrent) BacklotAccent else BacklotBorder,
                        ),
                        modifier = Modifier.heightIn(min = 44.dp)
                            .testTag("ux06b-composer-step-$index"),
                        content = content,
                    )
                }
            }
        }
    }
}
