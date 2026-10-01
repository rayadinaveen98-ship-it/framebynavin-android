package com.framebynavin.app.youtube

/**
 * Cross-surface consumers such as Home should follow the most recently accepted analytics refresh,
 * not an arbitrary preferred window. Insights itself may still request a specific 7/28/90-day cache.
 */
internal fun freshestYouTubeSnapshot(
    candidates: Iterable<YouTubeAnalyticsSnapshot?>,
): YouTubeAnalyticsSnapshot? = candidates
    .filterNotNull()
    .maxWithOrNull(
        compareBy<YouTubeAnalyticsSnapshot> { it.fetchedAtMillis }
            .thenBy { it.windowDays },
    )
