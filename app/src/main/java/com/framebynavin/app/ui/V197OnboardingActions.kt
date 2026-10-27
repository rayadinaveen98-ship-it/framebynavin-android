package com.framebynavin.app.ui

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.ArrowBack
import androidx.compose.material.icons.outlined.ArrowForward
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.framebynavin.app.ui.theme.RecRed

/**
 * Keeps first-run navigation reachable with narrow or high-font configurations.
 * Both arrangements invoke the same caller-owned callbacks; page validations, user profile
 * fields, save attempts and persistence remain entirely in V18CreatorOnboarding.
 */
@Composable
internal fun V197OnboardingActions(
    page: Int,
    hasProductionStyles: Boolean,
    enabled: Boolean,
    onBack: () -> Unit,
    onPrimary: () -> Unit,
    lastPage: Int = 4,
) {
    BoxWithConstraints(Modifier.fillMaxWidth()) {
        val stackActions = maxWidth < 370.dp || LocalDensity.current.fontScale >= 1.35f
        if (stackActions) {
            Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                V197OnboardingPrimary(
                    page = page, hasProductionStyles = hasProductionStyles,
                    enabled = enabled, onClick = onPrimary, modifier = Modifier.fillMaxWidth(),
                    lastPage = lastPage,
                )
                if (page > 0) V197OnboardingBack(onBack, Modifier.fillMaxWidth())
            }
        } else {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                if (page > 0) V197OnboardingBack(onBack)
                Spacer(Modifier.weight(1f))
                V197OnboardingPrimary(
                    page = page, hasProductionStyles = hasProductionStyles,
                    enabled = enabled, onClick = onPrimary, lastPage = lastPage,
                )
            }
        }
    }
}

@Composable
private fun V197OnboardingPrimary(
    page: Int,
    hasProductionStyles: Boolean,
    enabled: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    lastPage: Int = 4,
) {
    Button(
        onClick = onClick,
        enabled = enabled,
        colors = ButtonDefaults.buttonColors(containerColor = RecRed),
        shape = androidx.compose.foundation.shape.RoundedCornerShape(15.dp),
        modifier = modifier.heightIn(min = 50.dp).testTag("v197-onboarding-primary"),
    ) {
        Text(
            V149CreatorOnboardingPolicy.primaryActionLabel(page, hasProductionStyles),
            fontWeight = FontWeight.Black,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
        )
        Spacer(Modifier.width(5.dp))
        Icon(Icons.Outlined.ArrowForward, null, modifier = Modifier.size(17.dp))
    }
}

@Composable
private fun V197OnboardingBack(onClick: () -> Unit, modifier: Modifier = Modifier) {
    TextButton(
        onClick = onClick,
        modifier = modifier.heightIn(min = 48.dp).testTag("v197-onboarding-back"),
    ) {
        Icon(Icons.Outlined.ArrowBack, null, modifier = Modifier.size(17.dp))
        Spacer(Modifier.width(4.dp))
        Text("BACK", fontWeight = FontWeight.Bold)
    }
}
