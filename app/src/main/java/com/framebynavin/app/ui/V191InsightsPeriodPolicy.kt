package com.framebynavin.app.ui

import com.framebynavin.app.youtube.YouTubeAnalyticsSnapshot

/**
 * The selected range must match the cached report before any metric is displayed.
 * An older range may remain in memory only to preserve channel navigation and retry.
 */
internal object V191InsightsPeriodPolicy {
    fun isSelectedRangeReady(selectedDays: Int, report: YouTubeAnalyticsSnapshot?): Boolean =
        selectedDays in setOf(7, 28, 90) &&
            report != null &&
            report.windowDays == selectedDays &&
            report.startDate.isNotBlank() &&
            report.endDate.isNotBlank()
}
