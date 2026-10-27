package com.framebynavin.app.ui

import com.framebynavin.app.youtube.YouTubeAnalyticsSnapshot
import com.framebynavin.app.youtube.YouTubeReportEvidence

internal data class V191VerifiedMetric(
    val current: Long?,
    val previous: Long?,
    val changePercent: Int?,
)

/** Only source-confirmed figures may reach a metric drilldown. Null is not zero. */
internal object V191InsightMetricPolicy {
    private fun requiredKeys(kind: V20InsightsDetailKind): Set<String> = when (kind) {
        V20InsightsDetailKind.WATCH -> setOf("estimatedMinutesWatched")
        V20InsightsDetailKind.SUBS -> setOf("subscribersGained", "subscribersLost")
        V20InsightsDetailKind.AVG_VIEW -> setOf("averageViewDuration")
        else -> setOf("views")
    }

    fun evaluate(report: YouTubeAnalyticsSnapshot, kind: V20InsightsDetailKind): V191VerifiedMetric {
        val evidence: YouTubeReportEvidence = report.reportEvidence
            ?: return V191VerifiedMetric(null, null, null)
        val keys = requiredKeys(kind)
        if (!keys.all(evidence::hasMetric)) return V191VerifiedMetric(null, null, null)
        val current = when (kind) {
            V20InsightsDetailKind.WATCH -> report.watchMinutes
            V20InsightsDetailKind.SUBS -> report.netSubscribers
            V20InsightsDetailKind.AVG_VIEW -> report.averageViewDurationSeconds
            else -> report.views
        }
        val previous = report.previousPeriod?.takeIf {
            keys.all(evidence::canCompare)
        }?.let {
            when (kind) {
                V20InsightsDetailKind.WATCH -> it.watchMinutes
                V20InsightsDetailKind.SUBS -> it.netSubscribers
                V20InsightsDetailKind.AVG_VIEW -> it.averageViewDurationSeconds
                else -> it.views
            }
        }
        val change = previous?.takeIf { it != 0L }?.let {
            (((current.toDouble() - it.toDouble()) * 100.0) /
                kotlin.math.abs(it.toDouble())).toInt()
        }
        return V191VerifiedMetric(current, previous, change)
    }
}
