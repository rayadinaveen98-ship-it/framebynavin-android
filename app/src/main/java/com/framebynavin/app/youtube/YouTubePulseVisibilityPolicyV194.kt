package com.framebynavin.app.youtube

/**
 * Last-line guard before cached 24h Pulse evidence is shown anywhere in Insights.
 * A singleton may outlive a selected-account change, an offline period, or restore.
 */
internal object YouTubePulseVisibilityPolicy {
    private const val HOUR_MS = 3_600_000L
    private const val MAX_AGE_MS = 36L * HOUR_MS
    private const val FUTURE_TOLERANCE_MS = 5L * 60_000L

    fun visible(
        report: YouTube24HourReport?,
        selectedChannelId: String,
        selectedSnapshotAtMillis: Long,
        nowMillis: Long = System.currentTimeMillis(),
    ): YouTube24HourReport? {
        if (report == null || selectedChannelId.isBlank() ||
            report.channelId != selectedChannelId ||
            selectedSnapshotAtMillis <= 0L ||
            report.currentCapturedAtMillis <= 0L ||
            report.baselineCapturedAtMillis >= report.currentCapturedAtMillis ||
            report.currentCapturedAtMillis > selectedSnapshotAtMillis ||
            nowMillis - report.currentCapturedAtMillis > MAX_AGE_MS ||
            report.currentCapturedAtMillis - nowMillis > FUTURE_TOLERANCE_MS
        ) return null
        return report
    }
}
