package com.framebynavin.app.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Check
import androidx.compose.material.icons.outlined.ChevronRight
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.PlayArrow
import androidx.compose.material.icons.outlined.RecordVoiceOver
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.framebynavin.app.data.VoicePersona
import com.framebynavin.app.reminders.VoicePersonaEngine
import com.framebynavin.app.ui.theme.CinemaLine
import com.framebynavin.app.ui.theme.CinemaSurface
import com.framebynavin.app.ui.theme.CinemaSurfaceRaised
import com.framebynavin.app.ui.theme.MutedGold
import com.framebynavin.app.ui.theme.MutedText
import com.framebynavin.app.ui.theme.ProjectorIvory
import com.framebynavin.app.ui.theme.RecRed

/**
 * Project-only voice editor. Keep the existing VoicePersona and TTS preview callbacks
 * intact while moving the eight choices out of the inline reminder form.
 */
@Composable
internal fun V207ProjectVoiceSelector(
    selected: VoicePersona,
    onSelected: (VoicePersona) -> Unit,
    onPreview: (VoicePersona) -> Unit,
    modifier: Modifier = Modifier,
) {
    var pickerOpen by rememberSaveable { mutableStateOf(false) }

    Surface(
        onClick = { pickerOpen = true },
        modifier = modifier.fillMaxWidth().testTag("v207-voice-collapsed"),
        shape = RoundedCornerShape(16.dp),
        color = CinemaSurface,
        border = BorderStroke(1.dp, CinemaLine),
    ) {
        Row(
            Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(Icons.Outlined.RecordVoiceOver, null, tint = MutedGold, modifier = Modifier.size(24.dp))
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text("REMINDER VOICE", color = MutedText, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                Text(VoicePersonaEngine.label(selected), color = ProjectorIvory, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                Text(
                    VoicePersonaEngine.description(selected),
                    color = MutedText, fontSize = 11.sp,
                    maxLines = 1, overflow = TextOverflow.Ellipsis,
                )
            }
            IconButton(
                onClick = { onPreview(selected) },
                modifier = Modifier.testTag("v207-voice-quick-preview"),
            ) {
                Icon(Icons.Outlined.PlayArrow, "Preview selected voice", tint = MutedGold)
            }
            Icon(Icons.Outlined.ChevronRight, "Change voice", tint = ProjectorIvory)
        }
    }

    if (pickerOpen) {
        Dialog(onDismissRequest = { pickerOpen = false }) {
            Surface(
                modifier = Modifier.fillMaxWidth().heightIn(max = 560.dp).testTag("v207-voice-dialog"),
                shape = RoundedCornerShape(22.dp),
                color = CinemaSurfaceRaised,
                border = BorderStroke(1.dp, CinemaLine),
            ) {
                Column(Modifier.padding(16.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Column(Modifier.weight(1f)) {
                            Text("Reminder voice", color = ProjectorIvory, fontSize = 19.sp, fontWeight = FontWeight.Bold)
                            Text("Choose and preview", color = MutedText, fontSize = 12.sp)
                        }
                        IconButton(onClick = { pickerOpen = false }) {
                            Icon(Icons.Outlined.Close, "Close voice picker", tint = ProjectorIvory)
                        }
                    }
                    Spacer(Modifier.size(8.dp))
                    Column(Modifier.weight(1f, fill = false).verticalScroll(rememberScrollState())) {
                        VoicePersona.entries.forEach { persona ->
                            val active = persona == selected
                            Surface(
                                onClick = {
                                    onSelected(persona)
                                    pickerOpen = false
                                },
                                modifier = Modifier.fillMaxWidth().padding(vertical = 3.dp)
                                    .testTag("v207-voice-option-${persona.name}"),
                                shape = RoundedCornerShape(12.dp),
                                color = if (active) RecRed.copy(alpha = .12f) else CinemaSurface,
                                border = BorderStroke(1.dp, if (active) RecRed else CinemaLine),
                            ) {
                                Row(
                                    Modifier.fillMaxWidth().padding(start = 11.dp, end = 5.dp, top = 6.dp, bottom = 6.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                ) {
                                    Column(Modifier.weight(1f)) {
                                        Text(VoicePersonaEngine.label(persona), color = ProjectorIvory, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                                        Text(VoicePersonaEngine.description(persona), color = MutedText, fontSize = 11.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                    }
                                    if (active) Icon(Icons.Outlined.Check, "Selected", tint = RecRed, modifier = Modifier.size(18.dp))
                                    IconButton(
                                        onClick = { onPreview(persona) },
                                        modifier = Modifier.testTag("v207-voice-option-preview-${persona.name}"),
                                    ) {
                                        Icon(Icons.Outlined.PlayArrow, "Preview ${VoicePersonaEngine.label(persona)}", tint = MutedGold)
                                    }
                                }
                            }
                        }
                    }
                    TextButton(onClick = { pickerOpen = false }, modifier = Modifier.align(Alignment.End)) {
                        Text("DONE", color = ProjectorIvory)
                    }
                }
            }
        }
    }
}
