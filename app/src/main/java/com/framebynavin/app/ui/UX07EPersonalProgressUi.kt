package com.framebynavin.app.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.ChevronRight
import androidx.compose.material.icons.outlined.EventNote
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.framebynavin.app.personal.Phase1HabitChallenge
import com.framebynavin.app.ui.theme.BacklotBorder
import com.framebynavin.app.ui.theme.BacklotPrimaryText
import com.framebynavin.app.ui.theme.BacklotSecondaryAccent
import com.framebynavin.app.ui.theme.BacklotSecondaryText
import com.framebynavin.app.ui.theme.BacklotSurface
import com.framebynavin.app.ui.theme.CinemaLine
import com.framebynavin.app.ui.theme.CinemaSurface
import com.framebynavin.app.ui.theme.MutedGold
import com.framebynavin.app.ui.theme.MutedText
import com.framebynavin.app.ui.theme.ProjectorIvory
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale

/**
 * UX07E read-only Personal summary. The Personal receipt journal and current
 * 21-day Challenge store are DISTINCT sources. An unavailable source is never
 * presented as zero progress. A chart is deliberately omitted: different
 * completion domains cannot produce a truthful combined "success" percentage.
 */
@Composable
internal fun UX07EPersonalProgressOverview(
    snapshot: UX07EPersonalProgressSnapshot,
    enabled: Boolean,
    onOpenChallenges: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier.fillMaxWidth().testTag("ux07e-personal-progress"),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Row(
            Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(
                Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(3.dp),
            ) {
                Text(
                    "YOUR RECENT PROGRESS",
                    color = MutedGold,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                )
                Text("Last 7 calendar days · based on saved records",
                    color = MutedText, fontSize = 11.sp)
            }
            TextButton(
                onClick = onOpenChallenges,
                enabled = enabled,
                modifier = Modifier.defaultMinSize(minHeight = 48.dp)
                    .testTag("ux07e-progress-open-challenges"),
            ) { Text("CHALLENGES", fontSize = 11.sp) }
        }
        Surface(
            color = CinemaSurface,
            shape = RoundedCornerShape(16.dp),
            modifier = Modifier.fillMaxWidth(),
        ) {
            Column(
                Modifier.padding(horizontal = 14.dp, vertical = 13.dp),
                verticalArrangement = Arrangement.spacedBy(11.dp),
            ) {
                Row(
                    Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    UX07EProgressMetric(
                        number = snapshot.personalDoneLastSeven,
                        label = "Personal Done receipts",
                        modifier = Modifier.weight(1f),
                    )
                    UX07EProgressMetric(
                        number = snapshot.personalActiveDaysLastSeven,
                        label = "Days with Personal Done",
                        modifier = Modifier.weight(1f),
                    )
                }
                androidx.compose.material3.HorizontalDivider(
                    color = CinemaLine.copy(alpha = 0.45f),
                )
                Row(
                    Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    UX07EProgressMetric(
                        number = snapshot.challengeDaysConfirmedLastSeven,
                        label = "Challenge dates confirmed",
                        modifier = Modifier.weight(1f),
                    )
                    UX07EProgressMetric(
                        number = snapshot.challengeReflectionDaysLastSeven,
                        label = "Challenge dates with notes",
                        modifier = Modifier.weight(1f),
                    )
                }
            }
        }
        Text(
            "Personal Done uses when you confirmed it. Challenge counts use the chosen challenge dates, including explicitly confirmed catch-up days. Notes do not mark a day Done.",
            color = MutedText,
            fontSize = 11.sp,
            modifier = Modifier.testTag("ux07e-progress-source-explanation"),
        )
    }
}

@Composable
private fun UX07EProgressMetric(number: Int?, label: String, modifier: Modifier = Modifier) {
    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Text(
            number?.toString() ?: "—",
            color = ProjectorIvory,
            fontSize = 22.sp,
            fontWeight = FontWeight.SemiBold,
        )
        Text(
            if (number == null) "$label · unavailable" else label,
            color = MutedText, fontSize = 11.sp, lineHeight = 14.sp,
        )
    }
}

/**
 * Context for saved daily notes in the original 21-day dashboard.
 * Clicking a row reopens the existing day dialog; it does not confirm a day.
 */
@Composable
internal fun UX07EChallengeReflections(
    challenge: Phase1HabitChallenge,
    onDaySelected: (LocalDate) -> Unit,
    modifier: Modifier = Modifier,
) {
    val entries = remember(challenge.dayNotes, challenge.confirmations) {
        UX07EPersonalProgressPolicy.recentReflections(challenge)
    }
    val noteCount = remember(challenge.dayNotes) {
        challenge.dayNotes.values.count { it.isNotBlank() }
    }
    Column(
        modifier = modifier.fillMaxWidth().testTag("ux07e-challenge-reflections"),
        verticalArrangement = Arrangement.spacedBy(9.dp),
    ) {
        Row(
            Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(
                Icons.Outlined.EventNote,
                contentDescription = null,
                tint = BacklotSecondaryAccent,
            )
            Column(Modifier.weight(1f).padding(start = 9.dp)) {
                Text(
                    "Your reflections", color = BacklotPrimaryText,
                    fontSize = 16.sp, fontWeight = FontWeight.SemiBold,
                )
                Text(
                    "$noteCount day notes saved · separate from Done",
                    color = BacklotSecondaryText, fontSize = 11.sp,
                    modifier = Modifier.testTag("ux07e-reflection-note-count"),
                )
            }
        }
        if (entries.isEmpty()) {
            Text(
                "No reflections yet. Select a date in your 21-day calendar to write a note, without marking the day complete.",
                color = BacklotSecondaryText, fontSize = 12.sp,
                modifier = Modifier.testTag("ux07e-reflections-empty"),
            )
        } else {
            entries.forEach { entry ->
                Surface(
                    onClick = { onDaySelected(entry.day) },
                    modifier = Modifier.fillMaxWidth()
                        .defaultMinSize(minHeight = 52.dp)
                        .testTag("ux07e-reflection-" + entry.day),
                    color = BacklotSurface,
                    shape = RoundedCornerShape(12.dp),
                    tonalElevation = 0.dp,
                ) {
                    Row(
                        Modifier.padding(11.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(9.dp),
                    ) {
                        Column(
                            Modifier.weight(1f),
                            verticalArrangement = Arrangement.spacedBy(3.dp),
                        ) {
                            Text(
                                entry.day.format(DateTimeFormatter.ofPattern(
                                    "EEE, d MMM", Locale.getDefault())),
                                color = BacklotSecondaryAccent,
                                fontSize = 11.sp, fontWeight = FontWeight.SemiBold,
                            )
                            Text(
                                entry.note,
                                color = BacklotPrimaryText, fontSize = 13.sp,
                                maxLines = 2, overflow = TextOverflow.Ellipsis,
                            )
                            Text(
                                if (entry.confirmedAt != null)
                                    "Day confirmed separately · view actual receipt"
                                else "Note saved · no Done receipt",
                                color = BacklotSecondaryText, fontSize = 11.sp,
                            )
                        }
                        Icon(
                            Icons.Outlined.ChevronRight,
                            contentDescription = "Open day " + entry.day,
                            tint = BacklotSecondaryAccent,
                        )
                    }
                }
            }
            if (noteCount > entries.size) {
                Text(
                    "Showing the latest ${entries.size} notes · select any date in the calendar for earlier reflections.",
                    color = BacklotSecondaryText, fontSize = 11.sp,
                )
            }
        }
        Text(
            if (challenge.archived)
                "Archived challenge · reflections are read-only until restored."
            else "Only a separate, explicit day confirmation changes progress.",
            color = BacklotSecondaryText, fontSize = 11.sp,
            modifier = Modifier.testTag("ux07e-reflections-owner-rule"),
        )
    }
}
