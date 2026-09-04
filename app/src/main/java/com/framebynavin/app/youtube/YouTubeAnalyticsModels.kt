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
) {
    val netSubscribers: Long get() = subscribersGained - subscribersLost
}
