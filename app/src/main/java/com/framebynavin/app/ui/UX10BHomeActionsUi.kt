package com.framebynavin.app.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.ArrowForward
import androidx.compose.material.icons.outlined.Timer
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.framebynavin.app.ui.theme.BacklotUX10Tokens
import com.framebynavin.app.ui.theme.CinemaLine
import com.framebynavin.app.ui.theme.ProjectorIvory
import com.framebynavin.app.ui.theme.RecRed

/**
 * UX10B layout-only replacement for the original Home Continue/Focus row.
 * Neither tap changes project status here: original onContinue/onFocus owners
 * decide their navigation and haptic semantics at the callsite.
 */
@Composable
internal fun UX10BHomeProjectActions(
    projectTitle: String,
    onContinue: () -> Unit,
    onFocus: () -> Unit,
) {
    val fontScale = LocalDensity.current.fontScale
    val stacked = UX10BPresentationPolicy.stackedHomeActions(
        screenWidthDp = LocalConfiguration.current.screenWidthDp,
        fontScale = fontScale,
    )
    val minimumHeight = if (fontScale >= 1.5f) 72.dp
        else BacklotUX10Tokens.rowTarget
    if (stacked) {
        Column(
            Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            UX10BContinueButton(
                projectTitle = projectTitle,
                onClick = onContinue,
                minimumHeight = minimumHeight,
                modifier = Modifier.fillMaxWidth(),
            )
            UX10BFocusButton(
                projectTitle = projectTitle,
                onClick = onFocus,
                minimumHeight = minimumHeight,
                modifier = Modifier.fillMaxWidth(),
            )
        }
    } else {
        Row(
            Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            UX10BContinueButton(
                projectTitle = projectTitle,
                onClick = onContinue,
                minimumHeight = minimumHeight,
                modifier = Modifier.weight(1.35f),
            )
            UX10BFocusButton(
                projectTitle = projectTitle,
                onClick = onFocus,
                minimumHeight = minimumHeight,
                modifier = Modifier.weight(1f),
            )
        }
    }
}

@Composable
private fun UX10BContinueButton(
    projectTitle: String,
    onClick: () -> Unit,
    minimumHeight: androidx.compose.ui.unit.Dp,
    modifier: Modifier,
) {
    Button(
        onClick = onClick,
        modifier = modifier.heightIn(min = minimumHeight)
            .testTag("phase3-home-continue")
            .semantics { contentDescription = "Continue work on $projectTitle" },
        colors = ButtonDefaults.buttonColors(containerColor = RecRed),
        shape = RoundedCornerShape(BacklotUX10Tokens.surfaceRadius),
    ) {
        Icon(Icons.Outlined.ArrowForward, null)
        Spacer(Modifier.width(5.dp))
        Text(
            "CONTINUE WORK",
            fontSize = BacklotUX10Tokens.supporting,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center,
            maxLines = 2,
        )
    }
}

@Composable
private fun UX10BFocusButton(
    projectTitle: String,
    onClick: () -> Unit,
    minimumHeight: androidx.compose.ui.unit.Dp,
    modifier: Modifier,
) {
    OutlinedButton(
        onClick = onClick,
        modifier = modifier.heightIn(min = minimumHeight)
            .testTag("phase3-home-focus")
            .semantics { contentDescription = "Focus on $projectTitle" },
        border = BorderStroke(1.dp, CinemaLine),
        shape = RoundedCornerShape(BacklotUX10Tokens.surfaceRadius),
    ) {
        Icon(Icons.Outlined.Timer, null, tint = ProjectorIvory)
        Spacer(Modifier.width(5.dp))
        Text(
            "FOCUS",
            color = ProjectorIvory,
            fontSize = BacklotUX10Tokens.supporting,
            fontWeight = FontWeight.Bold,
        )
    }
}
