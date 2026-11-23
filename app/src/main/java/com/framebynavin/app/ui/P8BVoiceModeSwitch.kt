package com.framebynavin.app.ui

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Mic
import androidx.compose.material.icons.outlined.RecordVoiceOver
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.framebynavin.app.ui.theme.ProjectorIvory
import com.framebynavin.app.ui.theme.RecRed

/** Only one voice input mode is visible at a time. No voice files are deleted. */
internal enum class P8BVoiceMode {
    OFF, DICTATE, RECORD,
}

internal object P8BVoiceModePolicy {
    fun choose(
        current: P8BVoiceMode,
        tapped: P8BVoiceMode,
        recordingActive: Boolean,
    ): P8BVoiceMode = when {
        recordingActive -> current
        tapped == current -> P8BVoiceMode.OFF
        else -> tapped
    }
}

/** Premium compact mode chips, not two independently-open recording boxes. */
@Composable
internal fun P8BVoiceModeSwitch(
    mode: P8BVoiceMode,
    recordingActive: Boolean,
    hasSavedRecording: Boolean,
    onModeChange: (P8BVoiceMode) -> Unit,
    dictateTag: String,
    recordTag: String,
) {
    Row(
        modifier = Modifier.horizontalScroll(rememberScrollState())
            .testTag("p8b-voice-mode-switch"),
        horizontalArrangement = Arrangement.spacedBy(9.dp),
    ) {
        FilterChip(
            selected = mode == P8BVoiceMode.DICTATE,
            enabled = !recordingActive,
            onClick = { onModeChange(
                P8BVoiceModePolicy.choose(mode, P8BVoiceMode.DICTATE, recordingActive)
            ) },
            modifier = Modifier.heightIn(min = 48.dp).testTag(dictateTag),
            label = { Text("Speak to text", fontSize = 12.sp,
                fontWeight = FontWeight.Medium) },
            leadingIcon = { Icon(Icons.Outlined.RecordVoiceOver, null) },
            colors = FilterChipDefaults.filterChipColors(
                selectedContainerColor = RecRed.copy(alpha = .18f),
                selectedLabelColor = ProjectorIvory,
            ),
        )
        FilterChip(
            selected = mode == P8BVoiceMode.RECORD,
            enabled = !recordingActive,
            onClick = { onModeChange(
                P8BVoiceModePolicy.choose(mode, P8BVoiceMode.RECORD, recordingActive)
            ) },
            modifier = Modifier.heightIn(min = 48.dp).testTag(recordTag),
            label = {
                Text(if (hasSavedRecording) "Recording saved" else "Record original",
                    fontSize = 12.sp, fontWeight = FontWeight.Medium)
            },
            leadingIcon = { Icon(Icons.Outlined.Mic, null) },
            colors = FilterChipDefaults.filterChipColors(
                selectedContainerColor = RecRed.copy(alpha = .18f),
                selectedLabelColor = ProjectorIvory,
            ),
        )
    }
}
