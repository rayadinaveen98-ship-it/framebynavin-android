package com.framebynavin.app.youtube

data class YouTubeChannelSnapshot(
    val channelId: String,
    val title: String,
    val subscribers: Long,
    val lifetimeViews: Long,
    val videoCount: Long,
    val uploadsPlaylistId: String,
)

data class YouTubeVideoSnapshot(
    val videoId: String,
    val title: String,
    val publishedAtMillis: Long,
    val periodViews: Long,
    val lifetimeViews: Long,
    val watchMinutes: Long,
    val averageViewDurationSeconds: Long,
    val subscribersGained: Long,
    val subscribersLost: Long,
    val likes: Long,
    val comments: Long,
) {
    val netSubscribers: Long get() = subscribersGained - subscribersLost
}

data class YouTubeTrendPoint(
    val date: String,
    val views: Long,
    val watchMinutes: Long,
    val subscribersGained: Long,
    val subscribersLost: Long,
) {
    val netSubscribers: Long get() = subscribersGained - subscribersLost
}

data class YouTubePeriodSnapshot(
    val startDate: String,
    val endDate: String,
    val views: Long,
    val watchMinutes: Long,
    val averageViewDurationSeconds: Long,
    val subscribersGained: Long,
    val subscribersLost: Long,
    val likes: Long,
    val comments: Long,
) {
    val netSubscribers: Long get() = subscribersGained - subscribersLost
}

/**
 * Distinguishes observed YouTube Analytics values from absent rows and fields.
 * The daily dates reflect returned rows, not guaranteed complete processing.
 * Null evidence on legacy cache means unverified.
 */
data class YouTubeReportEvidence(
    val summaryPresent: Boolean,
    val previousSummaryPresent: Boolean,
    val summaryMetricKeys: Set<String>,
    val previousMetricKeys: Set<String>,
    val returnedDailyDates: List<String>,
) {
    fun hasMetric(key: String): Boolean = summaryPresent && key in summaryMetricKeys
    fun canCompare(key: String): Boolean =
        hasMetric(key) && previousSummaryPresent && key in previousMetricKeys
    val firstReturnedDay: String? get() = returnedDailyDates.minOrNull()
    val lastReturnedDay: String? get() = returnedDailyDates.maxOrNull()
}

data class YouTubeAnalyticsSnapshot(
    val channel: YouTubeChannelSnapshot,
    val windowDays: Int,
    val startDate: String,
    val endDate: String,
    val views: Long,
    val watchMinutes: Long,
    val averageViewDurationSeconds: Long,
    val subscribersGained: Long,
    val subscribersLost: Long,
    val likes: Long,
    val comments: Long,
    val topVideos: List<YouTubeVideoSnapshot>,
    val recentVideos: List<YouTubeVideoSnapshot>,
    val trend: List<YouTubeTrendPoint>,
    val fetchedAtMillis: Long,
    val previousPeriod: YouTubePeriodSnapshot? = null,
    val reportEvidence: YouTubeReportEvidence? = null,
) {
    val netSubscribers: Long get() = subscribersGained - subscribersLost
}
