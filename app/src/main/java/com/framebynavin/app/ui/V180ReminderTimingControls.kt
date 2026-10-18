package com.framebynavin.app.ui

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.framebynavin.app.ui.theme.MutedGold
import com.framebynavin.app.ui.theme.MutedText
import com.framebynavin.app.ui.theme.ProjectorIvory

/** Display options only. Actual bounds remain enforced by playback services. */
internal object V180ReminderTimingPolicy {
    val voiceAttempts = listOf(1, 2, 3)
    val voiceGapsSeconds = listOf(5, 10, 20, 30, 60)
    val alarmSeconds = listOf(30, 45, 60, 120, 180, 300)

    fun attempts(value: Int): Int = value.coerceIn(1, 3)
    fun gap(value: Int): Int = value.coerceIn(5, 60)
    fun alarm(value: Int): Int = value.coerceIn(30, 300)
    fun durationLabel(seconds: Int): String =
        if (seconds < 60) "${seconds}s"
        else if (seconds % 60 == 0) "${seconds / 60}m"
        else "${seconds / 60}m ${seconds % 60}s"

    fun summary(count: Int, gapSeconds: Int, alarmSeconds: Int): String =
        "Voice up to ${attempts(count)} ${if (attempts(count) == 1) "time" else "times"}" +
            " · ${gap(gapSeconds)}s gap · Alarm up to ${durationLabel(alarm(alarmSeconds))}"
}

/**
 * Shares exactly the same controls between Quick Create, full New Project, and Edit.
 * Values update the existing draft fields; no new project schema or setting migration.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
internal fun V180ReminderTimingControls(
    repeatCount: Int,
    onRepeatCount: (Int) -> Unit,
    repeatGapSeconds: Int,
    onRepeatGapSeconds: (Int) -> Unit,
    alarmTimeoutSeconds: Int,
    onAlarmTimeoutSeconds: (Int) -> Unit,
) {
    Column(Modifier.fillMaxWidth().testTag("v180-reminder-timing-controls")) {
        Text("VOICE & ALARM TIMING", color = MutedGold,
            fontSize = V173ProjectUiMetrics.supportingText, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(5.dp))
        Text("These apply when this project's alert uses Voice or Alarm. Smart may use both.",
            color = MutedText, fontSize = V173ProjectUiMetrics.supportingText)

        Spacer(Modifier.height(12.dp))
        Text("Speak this many times", color = ProjectorIvory, fontSize = V173ProjectUiMetrics.bodyText)
        FlowRow(
            modifier = Modifier.fillMaxWidth().testTag("v180-voice-repeats"),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            V180ReminderTimingPolicy.voiceAttempts.forEach { count ->
                FilterChip(
                    modifier = Modifier.heightIn(min = V173ProjectUiMetrics.touchTarget),
                    selected = repeatCount == count,
                    onClick = { onRepeatCount(count) },
                    label = { Text(if (count == 1) "Once" else "${count} times") },
                )
            }
        }

        Spacer(Modifier.height(10.dp))
        Text("Pause between spoken reminders", color = ProjectorIvory,
            fontSize = V173ProjectUiMetrics.bodyText)
        FlowRow(
            modifier = Modifier.fillMaxWidth().testTag("v180-voice-gap"),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            (V180ReminderTimingPolicy.voiceGapsSeconds + repeatGapSeconds.coerceIn(5, 60))
                .distinct().sorted().forEach { seconds ->
                    FilterChip(
                        modifier = Modifier.heightIn(min = V173ProjectUiMetrics.touchTarget),
                        selected = repeatGapSeconds == seconds,
                        onClick = { onRepeatGapSeconds(seconds) },
                        label = { Text("${seconds}s") },
                    )
                }
        }

        Spacer(Modifier.height(10.dp))
        Text("Stop alarm after", color = ProjectorIvory, fontSize = V173ProjectUiMetrics.bodyText)
        FlowRow(
            modifier = Modifier.fillMaxWidth().testTag("v180-alarm-duration"),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            (V180ReminderTimingPolicy.alarmSeconds + alarmTimeoutSeconds.coerceIn(30, 300))
                .distinct().sorted().forEach { seconds ->
                    FilterChip(
                        modifier = Modifier.heightIn(min = V173ProjectUiMetrics.touchTarget),
                        selected = alarmTimeoutSeconds == seconds,
                        onClick = { onAlarmTimeoutSeconds(seconds) },
                        label = { Text(V180ReminderTimingPolicy.durationLabel(seconds)) },
                    )
                }
        }
        Spacer(Modifier.height(7.dp))
        Text(
            "Voice ends when its last sentence finishes. An unanswered alert remains actionable " +
                "after sound stops; quiet follow-ups follow your Settings preference.",
            color = MutedText, fontSize = V173ProjectUiMetrics.supportingText,
        )
    }
}
