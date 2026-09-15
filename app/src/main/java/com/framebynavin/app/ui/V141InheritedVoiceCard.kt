package com.framebynavin.app.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.PlayArrow
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material.icons.outlined.VolumeUp
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.framebynavin.app.data.VoicePersona
import com.framebynavin.app.reminders.VoicePersonaEngine
import com.framebynavin.app.ui.theme.CinemaLine
import com.framebynavin.app.ui.theme.CinemaSurface
import com.framebynavin.app.ui.theme.MutedGold
import com.framebynavin.app.ui.theme.MutedText
import com.framebynavin.app.ui.theme.ProjectorIvory
import com.framebynavin.app.ui.theme.RecRed

/**
 * New-project voice surface for v141.
 *
 * Voice selection belongs to Settings. New projects inherit that global default automatically;
 * this card makes the inheritance visible without introducing a second picker that can drift.
 */
@Composable
internal fun V141InheritedVoiceCard(
    voice: VoicePersona,
    onPreview: (VoicePersona) -> Unit,
    onOpenSettings: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(18.dp),
        color = CinemaSurface,
        border = BorderStroke(1.dp, CinemaLine),
    ) {
        Column(Modifier.padding(14.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier.size(38.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        Icons.Outlined.VolumeUp,
                        contentDescription = null,
                        tint = MutedGold,
                        modifier = Modifier.size(22.dp),
                    )
                }
                Spacer(Modifier.width(9.dp))
                Column(Modifier.weight(1f)) {
                    Text(
                        "VOICE · FROM SETTINGS",
                        color = MutedGold,
                        fontSize = 7.8.sp,
                        fontWeight = FontWeight.Black,
                        letterSpacing = .8.sp,
                    )
                    Text(
                        VoicePersonaEngine.label(voice),
                        color = ProjectorIvory,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                    )
                    Text(
                        VoicePersonaEngine.description(voice),
                        color = MutedText,
                        fontSize = 8.2.sp,
                    )
                }
                Surface(
                    shape = RoundedCornerShape(100.dp),
                    color = RecRed.copy(alpha = .10f),
                    border = BorderStroke(1.dp, RecRed.copy(alpha = .25f)),
                ) {
                    Text(
                        "DEFAULT",
                        color = RecRed,
                        fontSize = 7.sp,
                        fontWeight = FontWeight.Black,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 5.dp),
                    )
                }
            }

            Text(
                "New projects automatically use your Backlot voice preference. Change it once in Settings and future projects follow it.",
                color = MutedText,
                fontSize = 8.2.sp,
                lineHeight = 11.5.sp,
                modifier = Modifier.padding(top = 9.dp),
            )

            Row(
                modifier = Modifier.fillMaxWidth().padding(top = 10.dp),
                horizontalArrangement = Arrangement.spacedBy(7.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                OutlinedButton(
                    onClick = { onPreview(voice) },
                    modifier = Modifier.weight(1f),
                    border = BorderStroke(1.dp, CinemaLine),
                    shape = RoundedCornerShape(12.dp),
                ) {
                    Icon(Icons.Outlined.PlayArrow, null, tint = MutedGold, modifier = Modifier.size(15.dp))
                    Spacer(Modifier.width(4.dp))
                    Text("PREVIEW", color = MutedGold, fontSize = 8.sp, fontWeight = FontWeight.Black)
                }
                TextButton(
                    onClick = onOpenSettings,
                    modifier = Modifier.weight(1f),
                ) {
                    Icon(Icons.Outlined.Settings, null, tint = ProjectorIvory, modifier = Modifier.size(14.dp))
                    Spacer(Modifier.width(4.dp))
                    Text("CHANGE IN SETTINGS", color = ProjectorIvory, fontSize = 7.7.sp, fontWeight = FontWeight.Black)
                }
            }
        }
    }
}
