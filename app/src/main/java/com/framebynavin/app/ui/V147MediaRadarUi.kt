package com.framebynavin.app.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.AutoAwesome
import androidx.compose.material.icons.outlined.CalendarMonth
import androidx.compose.material.icons.outlined.FactCheck
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.framebynavin.app.data.CreatorMediaReleaseBucket
import com.framebynavin.app.data.CreatorMediaSignal
import com.framebynavin.app.data.CreatorMediaSignalStore
import com.framebynavin.app.data.CreatorMediaVerification
import com.framebynavin.app.data.CreatorOsSettingsStore
import com.framebynavin.app.data.CreatorOttReleaseBoardEngine
import com.framebynavin.app.data.CreatorOttReleaseEntry
import com.framebynavin.app.data.CreatorStoryIntelligenceEngine
import com.framebynavin.app.data.CreatorStoryPriority
import com.framebynavin.app.data.CreatorStoryRecommendation
import com.framebynavin.app.ui.theme.CinemaLine
import com.framebynavin.app.ui.theme.CinemaSurface
import com.framebynavin.app.ui.theme.MutedGold
import com.framebynavin.app.ui.theme.MutedText
import com.framebynavin.app.ui.theme.ProjectorIvory
import com.framebynavin.app.ui.theme.RecRed
import com.framebynavin.app.ui.theme.SuccessGreen
import java.time.LocalDate
import java.util.Locale

/**
 * V147 media discovery surface. The host deliberately renders only evidence-backed cached signals;
 * no placeholder headlines or guessed release dates are manufactured when a feed is empty.
 */
@Composable
internal fun V147MediaRadarHost() {
    val context = LocalContext.current.applicationContext
    val mediaStore = remember { CreatorMediaSignalStore(context) }
    val settingsStore = remember { CreatorOsSettingsStore(context) }
    val signals by mediaStore.signalsFlow.collectAsState(initial = emptyList())
    val profile = settingsStore.snapshot().creatorProfile
    val preferredLanguages = profile.preferredMediaLanguages
    val visibleSignals = remember(signals, preferredLanguages) {
        v147FilterByLanguage(signals, preferredLanguages)
    }
    val today = LocalDate.now()
    val nowMillis = System.currentTimeMillis()
    val radar = remember(visibleSignals, today, nowMillis / 60_000L) {
        CreatorStoryIntelligenceEngine.build(visibleSignals, today, nowMillis)
    }
    val ott = remember(signals, today, preferredLanguages) {
        CreatorOttReleaseBoardEngine.build(signals, today, preferredLanguages)
    }

    Column(Modifier.fillMaxWidth()) {
        V147SectionHeader(
            icon = Icons.Outlined.AutoAwesome,
            kicker = "MEDIA RADAR",
            title = "What is worth covering",
            subtitle = if (signals.isEmpty()) {
                "Evidence-backed stories from your selected media sources will appear here after refresh."
            } else {
                "Freshness, evidence strength, timing and audience impact decide what rises first."
            },
        )
        Spacer(Modifier.height(10.dp))

        if (signals.isEmpty()) {
            V147EmptyCard(
                title = "Radar is ready for sources",
                body = "Your discovery preferences are saved. Backlot will not invent headlines: this space stays empty until the source refresh supplies evidence-backed signals.",
            )
        } else {
            V147StoryLane("COVER NOW", radar.coverNow, SuccessGreen)
            V147StoryLane("PREPARE", radar.prepare, MutedGold)
            V147StoryLane("VERIFY FIRST", radar.verifyFirst, RecRed)
        }

        Spacer(Modifier.height(22.dp))
        V147SectionHeader(
            icon = Icons.Outlined.CalendarMonth,
            kicker = "OTT RELEASES",
            title = "Release board",
            subtitle = "Weekend first, then today, then the next 30 days. Future dates never enter the released lane.",
        )
        Spacer(Modifier.height(10.dp))
        V147OttLane("EVERY WEEKEND", ott.weekend)
        V147OttLane("TODAY", ott.today)
        V147OttLane("UPCOMING · 30 DAYS", ott.upcoming)

        if (ott.weekend.isEmpty() && ott.today.isEmpty() && ott.upcoming.isEmpty()) {
            V147EmptyCard(
                title = "No confirmed OTT dates yet",
                body = "Backlot is keeping uncertain or unscheduled titles out of these three release lanes until the date evidence is strong enough.",
            )
        }
    }
}

@Composable
private fun V147SectionHeader(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    kicker: String,
    title: String,
    subtitle: String,
) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(
            Modifier.size(42.dp).background(MutedGold.copy(alpha = .11f), RoundedCornerShape(14.dp)),
            contentAlignment = Alignment.Center,
        ) {
            Icon(icon, null, tint = MutedGold, modifier = Modifier.size(20.dp))
        }
        Spacer(Modifier.size(11.dp))
        Column(Modifier.weight(1f)) {
            Text(kicker, color = MutedGold, fontSize = 8.sp, fontWeight = FontWeight.Black, letterSpacing = 1.sp)
            Text(title, color = ProjectorIvory, fontSize = 16.sp, fontWeight = FontWeight.Black)
            Text(subtitle, color = MutedText, fontSize = 8.7.sp, lineHeight = 12.sp)
        }
    }
}

@Composable
private fun V147StoryLane(
    label: String,
    stories: List<CreatorStoryRecommendation>,
    accent: Color,
) {
    if (stories.isEmpty()) return
    Text(label, color = accent, fontSize = 8.2.sp, fontWeight = FontWeight.Black, letterSpacing = 1.sp)
    Spacer(Modifier.height(7.dp))
    stories.take(4).forEach { story ->
        V147StoryCard(story, accent)
        Spacer(Modifier.height(7.dp))
    }
    Spacer(Modifier.height(7.dp))
}

@Composable
private fun V147StoryCard(story: CreatorStoryRecommendation, accent: Color) {
    Surface(
        Modifier.fillMaxWidth(),
        RoundedCornerShape(18.dp),
        CinemaSurface,
        border = BorderStroke(1.dp, accent.copy(alpha = .25f)),
    ) {
        Column(Modifier.padding(14.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                V147VerificationBadge(story.verification)
                Spacer(Modifier.weight(1f))
                Text("${story.confidence}% CONF", color = MutedText, fontSize = 7.4.sp, fontWeight = FontWeight.Bold)
            }
            Spacer(Modifier.height(7.dp))
            Text(
                story.title,
                color = ProjectorIvory,
                fontSize = 13.sp,
                fontWeight = FontWeight.Black,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
            if (story.summary.isNotBlank()) {
                Spacer(Modifier.height(4.dp))
                Text(story.summary, color = MutedText, fontSize = 9.sp, lineHeight = 13.sp, maxLines = 3, overflow = TextOverflow.Ellipsis)
            }
            Spacer(Modifier.height(7.dp))
            Text(story.whyNow, color = MutedText, fontSize = 8.3.sp, lineHeight = 12.sp, maxLines = 3, overflow = TextOverflow.Ellipsis)
            Spacer(Modifier.height(8.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
                V147MetaChip("FIT ${story.score}", accent)
                V147MetaChip(v147BucketLabel(story.releaseBucket), MutedGold)
                V147MetaChip("${story.evidence.size} EVIDENCE", ProjectorIvory)
            }
        }
    }
}

@Composable
private fun V147OttLane(label: String, releases: List<CreatorOttReleaseEntry>) {
    if (releases.isEmpty()) return
    Text(label, color = MutedGold, fontSize = 8.2.sp, fontWeight = FontWeight.Black, letterSpacing = 1.sp)
    Spacer(Modifier.height(7.dp))
    releases.take(8).forEach { release ->
        V147OttRow(release)
        Spacer(Modifier.height(7.dp))
    }
    Spacer(Modifier.height(7.dp))
}

@Composable
private fun V147OttRow(release: CreatorOttReleaseEntry) {
    Surface(
        Modifier.fillMaxWidth(),
        RoundedCornerShape(17.dp),
        CinemaSurface,
        border = BorderStroke(1.dp, CinemaLine),
    ) {
        Row(Modifier.padding(13.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(
                Modifier.size(38.dp).background(MutedGold.copy(alpha = .10f), RoundedCornerShape(12.dp)),
                contentAlignment = Alignment.Center,
            ) {
                Icon(Icons.Outlined.FactCheck, null, tint = MutedGold, modifier = Modifier.size(18.dp))
            }
            Spacer(Modifier.size(10.dp))
            Column(Modifier.weight(1f)) {
                Text(release.title, color = ProjectorIvory, fontSize = 11.5.sp, fontWeight = FontWeight.Bold, maxLines = 2, overflow = TextOverflow.Ellipsis)
                Text(
                    buildString {
                        append(release.releaseDate.toString())
                        if (release.platform.isNotBlank()) append(" · ${release.platform}")
                    },
                    color = MutedText,
                    fontSize = 8.4.sp,
                )
            }
            Spacer(Modifier.size(8.dp))
            V147VerificationBadge(release.verification)
        }
    }
}

@Composable
private fun V147VerificationBadge(verification: CreatorMediaVerification) {
    val (label, accent) = when (verification) {
        CreatorMediaVerification.VERIFIED -> "VERIFIED" to SuccessGreen
        CreatorMediaVerification.DEVELOPING -> "DEVELOPING" to MutedGold
        CreatorMediaVerification.RUMOR -> "RUMOR" to RecRed
    }
    Surface(shape = RoundedCornerShape(100.dp), color = accent.copy(alpha = .10f), border = BorderStroke(1.dp, accent.copy(alpha = .28f))) {
        Text(label, color = accent, fontSize = 6.8.sp, fontWeight = FontWeight.Black, modifier = Modifier.padding(horizontal = 7.dp, vertical = 4.dp))
    }
}

@Composable
private fun V147MetaChip(label: String, accent: Color) {
    Surface(shape = RoundedCornerShape(100.dp), color = accent.copy(alpha = .08f)) {
        Text(label, color = accent, fontSize = 6.6.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp))
    }
}

@Composable
private fun V147EmptyCard(title: String, body: String) {
    Surface(
        Modifier.fillMaxWidth(),
        RoundedCornerShape(18.dp),
        Color(0xFF151517),
        border = BorderStroke(1.dp, CinemaLine),
    ) {
        Column(Modifier.padding(15.dp)) {
            Text(title, color = ProjectorIvory, fontSize = 12.5.sp, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(4.dp))
            Text(body, color = MutedText, fontSize = 8.8.sp, lineHeight = 13.sp)
        }
    }
}

private fun v147FilterByLanguage(
    signals: List<CreatorMediaSignal>,
    preferredLanguages: Set<String>,
): List<CreatorMediaSignal> {
    val preferred = preferredLanguages
        .map { it.trim().lowercase(Locale.US) }
        .filter(String::isNotBlank)
        .toSet()
    if (preferred.isEmpty()) return signals
    return signals.filter { signal ->
        val languages = signal.languages
            .map { it.trim().lowercase(Locale.US) }
            .filter(String::isNotBlank)
            .toSet()
        languages.isEmpty() || languages.any(preferred::contains)
    }
}

private fun v147BucketLabel(bucket: CreatorMediaReleaseBucket): String = when (bucket) {
    CreatorMediaReleaseBucket.TODAY -> "TODAY"
    CreatorMediaReleaseBucket.THIS_WEEKEND -> "WEEKEND"
    CreatorMediaReleaseBucket.UPCOMING -> "UPCOMING"
    CreatorMediaReleaseBucket.RELEASED -> "RELEASED"
    CreatorMediaReleaseBucket.LATER -> "LATER"
    CreatorMediaReleaseBucket.UNSCHEDULED -> "NO DATE"
}
