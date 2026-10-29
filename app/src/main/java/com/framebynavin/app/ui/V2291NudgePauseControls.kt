package com.framebynavin.app.ui

import androidx.compose.foundation.layout.*
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.framebynavin.app.ui.theme.MutedGold
import com.framebynavin.app.ui.theme.MutedText

/** No new alerts. Explicit pause/resume over the existing opt-in worker state. */
@Composable
internal fun V2291NudgePauseControls(
    paused: Boolean,
    pausedUntilMillis: Long,
    onPauseWeek: () -> Unit,
    onResume: () -> Unit,
) {
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
        Text(
            if (paused) "Paused through ${v2291Time(pausedUntilMillis)}"
            else "Quiet 9 PM–9 AM · 24h global cooldown",
            color = MutedText, fontSize = 10.sp,
            modifier = Modifier.weight(1f).padding(top = 11.dp),
        )
        TextButton(
            onClick = if (paused) onResume else onPauseWeek,
            modifier = Modifier.testTag("v2291-nudge-pause-control"),
        ) {
            Text(
                if (paused) "RESUME" else "PAUSE 7 DAYS",
                color = MutedGold, fontSize = 10.sp, fontWeight = FontWeight.Bold,
            )
        }
    }
}

private fun v2291Time(millis: Long): String =
    java.text.SimpleDateFormat("EEE, d MMM · h:mm a", java.util.Locale.getDefault())
        .format(java.util.Date(millis))
