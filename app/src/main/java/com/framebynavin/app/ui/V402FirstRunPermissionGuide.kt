package com.framebynavin.app.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.Alignment
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.framebynavin.app.ui.theme.BacklotUX10Tokens
import com.framebynavin.app.ui.theme.CinemaBlack
import com.framebynavin.app.ui.theme.MutedGold
import com.framebynavin.app.ui.theme.MutedText
import com.framebynavin.app.ui.theme.ProjectorIvory
import com.framebynavin.app.ui.theme.RecRed

/**
 * UX02: Explain existing Android alarm permissions without granting, enabling
 * or scheduling anything on behalf of the user. Every item is optional.
 * No additional permission or scheduler is introduced.
 */
@Composable
internal fun V402FirstRunPermissionGuide(
    notificationsReady: Boolean,
    preciseTimingReady: Boolean,
    fullScreenReady: Boolean,
    batteryReady: Boolean,
    onNotifications: () -> Unit,
    onPreciseTiming: () -> Unit,
    onFullScreen: () -> Unit,
    onBattery: () -> Unit,
    onContinue: () -> Unit,
) {
    Surface(Modifier.fillMaxSize().testTag("v402-permission-guide"), color = CinemaBlack) {
        Column(
            Modifier.fillMaxSize()
                .windowInsetsPadding(WindowInsets.safeDrawing)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = BacklotUX10Tokens.pageGutter, vertical = 20.dp),
            verticalArrangement = Arrangement.spacedBy(13.dp),
        ) {
            Text("BACKLOT / OPTIONAL SETUP", color = MutedGold, fontSize = 10.sp,
                fontWeight = FontWeight.Black, letterSpacing = 1.2.sp)
            Text("Make reminders dependable.", color = ProjectorIvory, fontSize = 28.sp,
                lineHeight = 34.sp, fontWeight = FontWeight.Black)
            Text(
                "Choose when to enable permissions. You can keep using Backlot without them; " +
                    "some notifications and full-screen reminders may be limited by Android.",
                color = MutedText, fontSize = 13.sp, lineHeight = 20.sp,
            )

            V402PermissionRow("Notifications", "Allow reminders to appear on your device.",
                notificationsReady, onNotifications)
            V402PermissionRow("Precise timing", "Improve delivery precision for time-sensitive reminders.",
                preciseTimingReady, onPreciseTiming)
            V402PermissionRow("Full-screen alerts", "Review Android's full-screen alert access for supported alarms.",
                fullScreenReady, onFullScreen)
            V402PermissionRow("Battery restrictions", "Review background restrictions that may delay reminder delivery.",
                batteryReady, onBattery)

            Text(
                "Backlot never guarantees exact delivery on every Android device. " +
                    "You can revisit these choices in Settings.",
                color = MutedText, fontSize = 11.sp, lineHeight = 17.sp,
                modifier = Modifier.padding(top = 5.dp),
            )
            Button(
                onClick = onContinue,
                modifier = Modifier.fillMaxWidth().heightIn(min = BacklotUX10Tokens.rowTarget)
                    .testTag("v402-permissions-continue"),
                colors = ButtonDefaults.buttonColors(containerColor = RecRed),
            ) {
                Text("CONTINUE TO BACKLOT", fontWeight = FontWeight.Black)
            }
        }
    }
}

@Composable
private fun V402PermissionRow(
    title: String,
    description: String,
    ready: Boolean,
    onOpen: () -> Unit,
) {
    val stacked = UX10BPresentationPolicy.stackedFirstRunChoices(
        screenWidthDp = LocalConfiguration.current.screenWidthDp,
        fontScale = LocalDensity.current.fontScale,
    )
    val details: @Composable () -> Unit = {
        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(title, color = ProjectorIvory, fontSize = BacklotUX10Tokens.body,
                fontWeight = FontWeight.SemiBold)
            Text(description, color = MutedText, fontSize = BacklotUX10Tokens.supporting,
                lineHeight = 17.sp)
            Text(if (ready) "Available" else "Optional · not enabled",
                color = if (ready) MutedGold else MutedText,
                fontSize = BacklotUX10Tokens.eyebrow)
        }
    }
    val reviewButton: @Composable (Modifier) -> Unit = { modifier ->
        OutlinedButton(
            onClick = onOpen,
            enabled = !ready,
            modifier = modifier.heightIn(min = BacklotUX10Tokens.minimumTouchTarget),
        ) {
            Text(if (ready) "Ready" else "Review",
                fontSize = BacklotUX10Tokens.supporting)
        }
    }
    if (stacked) {
        Column(Modifier.fillMaxWidth().padding(vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)) {
            details()
            reviewButton(Modifier.fillMaxWidth())
        }
    } else {
        Row(
            Modifier.fillMaxWidth().padding(vertical = 7.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(Modifier.weight(1f)) { details() }
            reviewButton(Modifier)
        }
    }
}
