package com.framebynavin.app.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.framebynavin.app.ui.theme.*

/**
 * V221.5: the live SettingsHub uses this controlled preference view.
 * Persistence and Home updates are owned by the Activity composable, never
 * by this renderer. This is separate from Android reminder permissions.
 */
@Composable
internal fun V2215CompanionControls(
    enabled: Boolean,
    onEnabledChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
) {
    Surface(
        modifier = modifier.fillMaxWidth().testTag("v2215-companion-preference"),
        color = CinemaSurface,
        shape = RoundedCornerShape(16.dp),
        border = BorderStroke(1.dp, CinemaLine),
    ) {
        Row(
            modifier = Modifier.padding(14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    "Helpful companion suggestions",
                    color = ProjectorIvory, fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                )
                Spacer(Modifier.height(5.dp))
                Text(
                    if (enabled) "On · one useful tip on Home when there is real activity."
                    else "Off · reminders and your chosen companion remain unchanged.",
                    color = MutedText, fontSize = 11.sp, lineHeight = 16.sp,
                )
            }
            Switch(
                checked = enabled,
                onCheckedChange = onEnabledChange,
                modifier = Modifier.testTag("v2215-companion-switch").semantics {
                    contentDescription = "Helpful companion suggestions"
                    stateDescription = if (enabled) "On" else "Off"
                },
                colors = SwitchDefaults.colors(checkedTrackColor = RecRed),
            )
        }
    }
}
