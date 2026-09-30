package com.framebynavin.app.ui

import androidx.compose.animation.AnimatedContent
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.framebynavin.app.ui.theme.CinemaLine
import com.framebynavin.app.ui.theme.CinemaSurface
import com.framebynavin.app.ui.theme.MutedGold
import com.framebynavin.app.ui.theme.MutedText
import com.framebynavin.app.ui.theme.ProjectorIvory
import com.framebynavin.app.ui.theme.SuccessGreen
import com.framebynavin.app.youtube.YouTubeVideoPerformance
import java.util.Locale
import kotlin.math.abs

/**
 * Calm, user-driven Top 3 carousel for Home. Ranking is supplied by the same
 * YouTubeInsightEngine.videoPerformance source used by Insights.
 */
@Composable
internal fun V148HomeTopPerformers(
    performances: List<YouTubeVideoPerformance>,
    onOpenVideo: (String) -> Unit,
) {
    val top = performances.take(3)
    if (top.isEmpty()) return

    var index by remember(top.map { it.video.videoId }) { mutableIntStateOf(0) }
    var drag by remember { mutableFloatStateOf(0f) }
    if (index > top.lastIndex) index = top.lastIndex.coerceAtLeast(0)

    Column(Modifier.fillMaxWidth()) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text("TOP PERFORMERS", color = MutedGold, fontSize = 8.5.sp, fontWeight = FontWeight.Black, letterSpacing = 1.1.sp)
                Text("Your strongest videos in the current Insights data.", color = MutedText, fontSize = 8.5.sp)
            }
            Text("${index + 1}/${top.size}", color = MutedGold, fontSize = 8.sp, fontWeight = FontWeight.Bold)
        }
        Spacer(Modifier.height(8.dp))

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .pointerInput(top.map { it.video.videoId }) {
                    detectHorizontalDragGestures(
                        onDragStart = { drag = 0f },
                        onHorizontalDrag = { _, amount -> drag += amount },
                        onDragEnd = {
                            if (drag < -48f && index < top.lastIndex) index += 1
                            if (drag > 48f && index > 0) index -= 1
                            drag = 0f
                        },
                        onDragCancel = { drag = 0f },
                    )
                },
        ) {
            AnimatedContent(targetState = index, label = "v148TopPerformers") { targetIndex ->
                val performance = top[targetIndex.coerceIn(0, top.lastIndex)]
                V148HomePerformerCard(
                    rank = targetIndex + 1,
                    performance = performance,
                    onClick = { onOpenVideo(performance.video.videoId) },
                )
            }
        }

        if (top.size > 1) {
            Spacer(Modifier.height(7.dp))
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Center) {
                top.indices.forEach { dot ->
                    Box(
                        Modifier
                            .padding(horizontal = 3.dp)
                            .size(if (dot == index) 7.dp else 5.dp)
                            .background(
                                if (dot == index) MutedGold else CinemaLine,
                                CircleShape,
                            ),
                    )
                }
            }
        }
    }
}

@Composable
private fun V148HomePerformerCard(
    rank: Int,
    performance: YouTubeVideoPerformance,
    onClick: () -> Unit,
) {
    Surface(
        modifier = Modifier.fillMaxWidth().clickable(onClick = onClick),
        shape = RoundedCornerShape(17.dp),
        color = CinemaSurface,
        border = BorderStroke(1.dp, CinemaLine),
    ) {
        Row(
            modifier = Modifier.padding(10.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            V148YouTubeThumbnail(
                videoId = performance.video.videoId,
                rank = rank,
                modifier = Modifier.width(126.dp).aspectRatio(16f / 9f),
            )
            Spacer(Modifier.width(10.dp))
            Column(Modifier.weight(1f)) {
                Text(
                    performance.video.title,
                    color = ProjectorIvory,
                    fontSize = 10.5.sp,
                    lineHeight = 13.sp,
                    fontWeight = FontWeight.Bold,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
                Spacer(Modifier.height(5.dp))
                Text(
                    "${v148HomeCompact(performance.video.periodViews)} views · ${v148HomeSigned(performance.video.netSubscribers)} subs",
                    color = MutedText,
                    fontSize = 7.6.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                val baseline = when {
                    performance.baselineMultiple <= 0 -> "Learning your usual"
                    performance.baselineMultiple >= 1.0 -> "${((performance.baselineMultiple - 1.0) * 100).toInt()}% above usual"
                    else -> "${abs(((performance.baselineMultiple - 1.0) * 100).toInt())}% below usual"
                }
                Text(
                    baseline,
                    color = if (performance.baselineMultiple >= 1.05) SuccessGreen else MutedGold,
                    fontSize = 7.3.sp,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                )
            }
        }
    }
}

private fun v148HomeCompact(value: Long): String = when {
    abs(value) >= 1_000_000_000L -> String.format(Locale.US, "%.1fB", value / 1_000_000_000.0)
    abs(value) >= 1_000_000L -> String.format(Locale.US, "%.1fM", value / 1_000_000.0)
    abs(value) >= 1_000L -> String.format(Locale.US, "%.1fK", value / 1_000.0)
    else -> value.toString()
}

private fun v148HomeSigned(value: Long): String = if (value > 0) "+${v148HomeCompact(value)}" else v148HomeCompact(value)
