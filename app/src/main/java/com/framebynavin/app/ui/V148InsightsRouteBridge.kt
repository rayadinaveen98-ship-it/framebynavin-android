package com.framebynavin.app.ui

import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.compose.ui.unit.dp
import com.framebynavin.app.youtube.YouTubeAnalyticsStore
import com.framebynavin.app.youtube.YouTubeInsightEngine
import com.framebynavin.app.youtube.freshestYouTubeSnapshot

/** One-shot navigation bridge from Home Top Performers into an exact Insights video. */
internal object V148InsightsRouteState {
    var pendingVideoId by mutableStateOf<String?>(null)
    var pendingWindowDays by mutableIntStateOf(0)

    fun request(videoId: String, windowDays: Int) {
        pendingVideoId = videoId.takeIf { it.isNotBlank() }
        pendingWindowDays = windowDays
    }

    fun clear() {
        pendingVideoId = null
        pendingWindowDays = 0
    }
}

internal fun v148ResolveRequestedVideoId(
    availableVideoIds: List<String>,
    requestedVideoId: String?,
): String? = requestedVideoId?.takeIf { requested ->
    requested.isNotBlank() && availableVideoIds.any { it == requested }
}

/** Uses the exact same YouTubeInsightEngine ranking source as Insights and hides itself without usable data. */
@Composable
internal fun V148HomeTopPerformersSection(onOpenInsights: () -> Unit) {
    val context = LocalContext.current.applicationContext
    val store = remember(context) { YouTubeAnalyticsStore(context) }
    // 3E: a newly accepted Insights sync / account disconnect may change cached
    // video truth without changing this Home composable's identity. Reread the
    // same owner-scoped cache when returning to Home. No network, writes or
    // fabricated zero metrics; absent/disconnected data hides the section.
    val owner = LocalLifecycleOwner.current
    var visibleCacheEpoch by remember { mutableIntStateOf(0) }
    DisposableEffect(owner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) visibleCacheEpoch += 1
        }
        owner.lifecycle.addObserver(observer)
        onDispose { owner.lifecycle.removeObserver(observer) }
    }
    val snapshot = remember(store, visibleCacheEpoch) {
        freshestYouTubeSnapshot(
            listOf(
                store.load(7),
                store.load(28),
                store.load(90),
            ),
        )
    }
    val performances = remember(snapshot) {
        snapshot?.let(YouTubeInsightEngine::videoPerformance).orEmpty()
    }
    if (snapshot == null || performances.isEmpty()) return

    Spacer(Modifier.height(14.dp))
    V148HomeTopPerformers(
        performances = performances,
        onOpenVideo = { videoId ->
            V148InsightsRouteState.request(videoId, snapshot.windowDays)
            onOpenInsights()
        },
    )
}
