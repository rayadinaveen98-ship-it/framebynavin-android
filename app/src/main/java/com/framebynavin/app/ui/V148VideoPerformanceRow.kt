package com.framebynavin.app.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.ChevronRight
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.framebynavin.app.ui.theme.CinemaLine
import com.framebynavin.app.ui.theme.CinemaSurface
import com.framebynavin.app.ui.theme.MutedGold
import com.framebynavin.app.ui.theme.MutedText
import com.framebynavin.app.ui.theme.ProjectorIvory
import com.framebynavin.app.ui.theme.RecRed
import com.framebynavin.app.ui.theme.SuccessGreen
import com.framebynavin.app.youtube.YouTubeVideoPerformance
import com.framebynavin.app.youtube.YouTubeVideoSnapshot
import java.util.Locale
import kotlin.math.abs

@Composable
internal fun V148VideoPerformanceRow(
    rank: Int,
    performance: YouTubeVideoPerformance,
    onVideo: (YouTubeVideoSnapshot) -> Unit,
) {
    val accent = when {
        performance.baselineMultiple >= 1.5 -> SuccessGreen
        performance.baselineMultiple >= 1.05 -> MutedGold
        performance.baselineMultiple in 0.01..0.75 -> RecRed
        else -> MutedText
    }
    val baseline = when {
        performance.baselineMultiple <= 0 -> "Learning your usual"
        performance.baselineMultiple >= 1.0 -> "${((performance.baselineMultiple - 1.0) * 100).toInt()}% above usual"
        else -> "${abs(((performance.baselineMultiple - 1.0) * 100).toInt())}% below usual"
    }

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = 8.dp)
            .clickable { onVideo(performance.video) },
        shape = RoundedCornerShape(16.dp),
        color = CinemaSurface,
        border = BorderStroke(1.dp, CinemaLine),
    ) {
        Row(
            modifier = Modifier.padding(10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            V148YouTubeThumbnail(
                videoId = performance.video.videoId,
                rank = rank,
                modifier = Modifier.width(112.dp).aspectRatio(16f / 9f),
            )

            Column(Modifier.weight(1f)) {
                Text(
                    text = performance.video.title,
                    color = ProjectorIvory,
                    fontSize = 10.5.sp,
                    lineHeight = 13.5.sp,
                    fontWeight = FontWeight.Bold,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
                Spacer(Modifier.width(1.dp))
                Text(
                    text = "${v148Compact(performance.video.periodViews)} views · ${v148Watch(performance.video.watchMinutes)} · ${v148Signed(performance.video.netSubscribers)} subs",
                    color = MutedText,
                    fontSize = 7.5.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    text = "$baseline · ${performance.viewSharePercent}% share",
                    color = accent,
                    fontSize = 7.4.sp,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }

            Icon(
                Icons.Outlined.ChevronRight,
                contentDescription = "Open video insights",
                tint = MutedText,
                modifier = Modifier.width(16.dp),
            )
        }
    }
}

private fun v148Compact(value: Long): String = when {
    abs(value) >= 1_000_000_000L -> String.format(Locale.US, "%.1fB", value / 1_000_000_000.0)
    abs(value) >= 1_000_000L -> String.format(Locale.US, "%.1fM", value / 1_000_000.0)
    abs(value) >= 1_000L -> String.format(Locale.US, "%.1fK", value / 1_000.0)
    else -> value.toString()
}

private fun v148Watch(minutes: Long): String {
    val hours = minutes / 60.0
    return if (abs(hours) >= 1000) String.format(Locale.US, "%.1fK h", hours / 1000.0)
    else String.format(Locale.US, "%.1f h", hours)
}

private fun v148Signed(value: Long): String = if (value > 0) "+${v148Compact(value)}" else v148Compact(value)
