package com.framebynavin.app.ui

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import com.framebynavin.app.personal.Phase1HabitVoicePlaybackPolicy
import com.framebynavin.app.reminders.ReminderMusicMode
import com.framebynavin.app.ui.theme.BacklotSecondaryText
import androidx.compose.ui.unit.dp

/**
 * UX06D per-challenge Voice styling. Never touches Creator defaults, music
 * device permissions globally, challenge completion, schedule or DataStore.
 * The owner composer saves these preferences only after the explicit Save.
 */
@Composable
internal fun V406ChallengeVoiceSettings(
    voiceRepeatCount: Int,
    onVoiceRepeatCount: (Int) -> Unit,
    voiceRepeatGapMillis: Long,
    onVoiceRepeatGapMillis: (Long) -> Unit,
    voiceMusicMode: ReminderMusicMode,
    voiceMusicUri: String,
    onVoiceMusicChange: (ReminderMusicMode, String) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier.testTag("ux06d-challenge-voice-settings"),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Text("VOICE ANNOUNCEMENTS", style = MaterialTheme.typography.labelMedium)
        Text(
            "One spoken announcement repeats only as selected. " +
                "A repeat never marks a challenge day Done.",
            color = BacklotSecondaryText,
            style = MaterialTheme.typography.bodySmall,
        )
        Row(
            Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            for (count in 1..5) {
                FilterChip(
                    selected = voiceRepeatCount == count,
                    onClick = { onVoiceRepeatCount(count) },
                    label = { Text("$count") },
                    modifier = Modifier.testTag("ux06d-challenge-repeat-$count"),
                )
            }
        }
        Text("PAUSE BETWEEN ANNOUNCEMENTS", style = MaterialTheme.typography.labelMedium)
        Row(
            Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            for (gap in Phase1HabitVoicePlaybackPolicy.ALLOWED_GAPS_MILLIS) {
                val seconds = (gap / 1_000L).toInt()
                FilterChip(
                    selected = voiceRepeatGapMillis == gap,
                    onClick = { onVoiceRepeatGapMillis(gap) },
                    label = { Text("${seconds}s") },
                    modifier = Modifier.testTag("ux06d-challenge-gap-$seconds"),
                )
            }
        }
        // Reuse the existing Android OpenDocument SAF grant, device-only
        // Piano/CUSTOM/OFF audio, preview and stop lifecycle. Crucially, never
        // expose V273's optional "apply to all Creator projects" action.
        V273ReminderMusicPicker(
            mode = voiceMusicMode,
            uri = voiceMusicUri,
            onChanged = onVoiceMusicChange,
            allowSetCreatorDefault = false,
            modifier = Modifier.testTag("ux06d-challenge-music"),
        )
        Text(
            "Music plays only for Voice reminders. Quiet, Alarm and Off " +
                "do not start background music. Existing challenges stay voice-only " +
                "unless you choose a backing track.",
            color = BacklotSecondaryText,
            style = MaterialTheme.typography.bodySmall,
        )
    }
}
