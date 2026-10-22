package com.framebynavin.app.youtube

import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

/**
 * A top-earning-video report is NOT an exhaustive format-revenue dataset.
 * The channel-wide estimatedRevenue comes only from the summary query; video
 * groups always represent the returned up-to-50 ranked content rows.
 */
internal object V207RevenuePresentation {
    internal data class Group(
        val label: String,
        val rows: Int,
        val views: Long,
        val estimatedRevenue: Double,
    )

    fun formatGroups(snapshot: YouTubeRevenueSnapshot): List<Group> {
        if (snapshot.evidence?.contentState != YouTubeDatasetState.READY) return emptyList()
        val keys = listOf("SHORTS", "VIDEOS", "LIVE", "OTHER")
        return snapshot.content.groupBy { row ->
            when (row.creatorContentType) {
                "SHORTS" -> "SHORTS"
                "VIDEO_ON_DEMAND" -> "VIDEOS"
                "LIVE_STREAM" -> "LIVE"
                else -> "OTHER"
            }
        }.let { groups ->
            keys.mapNotNull { key ->
                val rows = groups[key]?.takeIf { it.isNotEmpty() } ?: return@mapNotNull null
                Group(
                    label = key,
                    rows = rows.size,
                    views = rows.sumOf { it.views },
                    estimatedRevenue = rows.sumOf { it.estimatedRevenue },
                )
            }
        }
    }

    fun sampleLabel(snapshot: YouTubeRevenueSnapshot): String {
        if (snapshot.evidence?.contentState == YouTubeDatasetState.UNAVAILABLE) {
            return "Video earnings details unavailable"
        }
        if (snapshot.content.isEmpty()) return "No video earnings details for this period"
        return "Top ${snapshot.content.size.coerceAtMost(50)} returned video rows · sample, not a full channel format split"
    }

    /** No numeric zero or fabricated earning when the summary metric is absent. */
    fun hasEstimatedTotal(snapshot: YouTubeRevenueSnapshot): Boolean =
        snapshot.evidence?.hasSummary("estimatedRevenue") == true &&
            snapshot.estimatedRevenue.isFinite()

    fun refreshLabel(snapshot: YouTubeRevenueSnapshot, zone: ZoneId = ZoneId.systemDefault()): String =
        if (snapshot.fetchedAtMillis <= 0) "Refresh time unavailable"
        else runCatching {
            val at = Instant.ofEpochMilli(snapshot.fetchedAtMillis).atZone(zone)
            "Updated " + DateTimeFormatter.ofPattern("d MMM, h:mm a", Locale.getDefault()).format(at)
        }.getOrDefault("Refresh time unavailable")
}
