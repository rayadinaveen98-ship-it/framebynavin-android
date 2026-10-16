package com.framebynavin.app.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.PlayArrow
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
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

@Composable
internal fun V140VoiceStudioPicker(
    selected: VoicePersona,
    onSelected: (VoicePersona) -> Unit,
    onPreview: (VoicePersona) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(modifier.fillMaxWidth()) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text("DEFAULT REMINDER VOICE", color = MutedGold, fontSize = 10.sp, fontWeight = FontWeight.Black, letterSpacing = 1.sp)
                Text("Choose a voice", color = ProjectorIvory, fontSize = 15.sp, fontWeight = FontWeight.Bold)
            }
            Surface(
                shape = RoundedCornerShape(100.dp),
                color = MutedGold.copy(alpha = .10f),
                border = BorderStroke(1.dp, MutedGold.copy(alpha = .24f)),
            ) {
                Text("TAP TO SELECT", color = MutedGold, fontSize = 9.sp, fontWeight = FontWeight.Black, modifier = Modifier.padding(horizontal = 8.dp, vertical = 5.dp))
            }
        }
        Spacer(Modifier.height(10.dp))
        VoicePersona.entries.chunked(2).forEach { row ->
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                row.forEach { persona ->
                    val isSelected = selected == persona
                    Surface(
                        onClick = { onSelected(persona) },
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(16.dp),
                        color = if (isSelected) RecRed.copy(alpha = .10f) else CinemaSurface,
                        border = BorderStroke(1.dp, if (isSelected) RecRed.copy(alpha = .72f) else CinemaLine),
                    ) {
                        Column(Modifier.padding(11.dp)) {
                            Text(
                                VoicePersonaEngine.category(persona),
                                color = if (isSelected) RecRed else MutedGold,
                                fontSize = 9.5.sp,
                                fontWeight = FontWeight.Black,
                                letterSpacing = .7.sp,
                            )
                            Spacer(Modifier.height(3.dp))
                            Text(VoicePersonaEngine.label(persona), color = ProjectorIvory, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                            Spacer(Modifier.height(2.dp))
                            Text(
                                VoicePersonaEngine.description(persona),
                                color = MutedText,
                                fontSize = 10.5.sp,
                                lineHeight = 14.sp,
                                minLines = 2,
                            )
                            Spacer(Modifier.height(8.dp))
                            Surface(
                                onClick = { onPreview(persona) },
                                shape = RoundedCornerShape(100.dp),
                                color = if (isSelected) RecRed.copy(alpha = .16f) else MutedGold.copy(alpha = .08f),
                                border = BorderStroke(1.dp, if (isSelected) RecRed.copy(alpha = .32f) else CinemaLine),
                            ) {
                                Row(
                                    modifier = Modifier.defaultMinSize(minHeight = 40.dp).padding(horizontal = 9.dp, vertical = 6.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                ) {
                                    Icon(
                                        Icons.Outlined.PlayArrow,
                                        contentDescription = "Preview ${VoicePersonaEngine.label(persona)}",
                                        tint = if (isSelected) RecRed else MutedGold,
                                        modifier = Modifier.size(17.dp),
                                    )
                                    Spacer(Modifier.width(3.dp))
                                    Text("PREVIEW", color = if (isSelected) RecRed else MutedGold, fontSize = 10.sp, fontWeight = FontWeight.Black)
                                }
                            }
                        }
                    }
                }
                if (row.size == 1) Spacer(Modifier.weight(1f))
            }
            Spacer(Modifier.height(8.dp))
        }
        Text(
            "Voice options depend on your phone. Tap Preview to listen.",
            color = MutedText,
            fontSize = 11.sp,
            lineHeight = 16.sp,
        )
    }
}
