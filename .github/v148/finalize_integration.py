from pathlib import Path


def replace_once(path: str, old: str, new: str) -> None:
    p = Path(path)
    source = p.read_text()
    count = source.count(old)
    if count != 1:
        raise SystemExit(f"{path}: expected one match, found {count}")
    p.write_text(source.replace(old, new, 1))


today = "app/src/main/java/com/framebynavin/app/ui/V18TodayScreen.kt"
replace_once(
    today,
    """            V18CreatorFocusCard(creatorProfile, personalization, onClick = { showWeeklyFocus = true })
            Spacer(Modifier.height(18.dp))""",
    """            V18CreatorFocusCard(creatorProfile, personalization, onClick = { showWeeklyFocus = true })
            V148HomeTopPerformersSection(onOpenInsights = onOpenInsights)
            Spacer(Modifier.height(18.dp))""",
)

insights = "app/src/main/java/com/framebynavin/app/ui/V172InsightsUi.kt"
p = Path(insights)
source = p.read_text()
marker = """    val videos = remember(snapshot) { YouTubeInsightEngine.videoPerformance(snapshot) }
    val detail = videos.firstOrNull { it.video.videoId == detailVideoId }
"""
replacement = """    val videos = remember(snapshot) { YouTubeInsightEngine.videoPerformance(snapshot) }
    val detail = videos.firstOrNull { it.video.videoId == detailVideoId }

    LaunchedEffect(V148InsightsRouteState.pendingVideoId, snapshot.fetchedAtMillis) {
        val requested = v148ResolveRequestedVideoId(
            availableVideoIds = videos.map { it.video.videoId },
            requestedVideoId = V148InsightsRouteState.pendingVideoId,
        )
        if (requested != null) {
            tabName = V172InsightsTab.CONTENT.name
            detailVideoId = requested
            V148InsightsRouteState.clear()
        }
    }
"""
if source.count(marker) != 1:
    raise SystemExit(f"{insights}: route marker mismatch ({source.count(marker)})")
source = source.replace(marker, replacement, 1)
old_row = "V172VideoRow(index + 1, performance, onVideo)"
if source.count(old_row) != 2:
    raise SystemExit(f"{insights}: expected two live video rows, found {source.count(old_row)}")
source = source.replace(old_row, "V148VideoPerformanceRow(index + 1, performance, onVideo)")
p.write_text(source)

v11 = "app/src/main/java/com/framebynavin/app/ui/V11YouTubeInsights.kt"
replace_once(
    v11,
    """    val personalization by remember(creatorProfile) {
        derivedStateOf { CreatorPersonalizationEngine.snapshot(creatorProfile, tasks) }
    }

    fun refreshCacheView() {""",
    """    val personalization by remember(creatorProfile) {
        derivedStateOf { CreatorPersonalizationEngine.snapshot(creatorProfile, tasks) }
    }

    LaunchedEffect(V148InsightsRouteState.pendingWindowDays) {
        val routedDays = V148InsightsRouteState.pendingWindowDays
        if (routedDays in listOf(7, 28, 90) && routedDays != windowDays) {
            windowDays = routedDays
            snapshot = store.load(routedDays) ?: store.loadAny()
        }
    }

    fun refreshCacheView() {""",
)

bridge = Path("app/src/main/java/com/framebynavin/app/ui/V148InsightsRouteBridge.kt")
bridge.write_text(
    """package com.framebynavin.app.ui

import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.framebynavin.app.youtube.YouTubeAnalyticsStore
import com.framebynavin.app.youtube.YouTubeInsightEngine

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
    val snapshot = remember(store) { store.loadAny() }
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
"""
)

test = Path("app/src/test/java/com/framebynavin/app/ui/V148InsightsRouteTest.kt")
test.parent.mkdir(parents=True, exist_ok=True)
test.write_text(
    """package com.framebynavin.app.ui

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class V148InsightsRouteTest {
    @Test
    fun resolvesTheExactRequestedVideoId() {
        assertEquals(
            "video-b",
            v148ResolveRequestedVideoId(
                availableVideoIds = listOf("video-a", "video-b", "video-c"),
                requestedVideoId = "video-b",
            ),
        )
    }

    @Test
    fun neverFallsBackToAWrongVideo() {
        assertNull(
            v148ResolveRequestedVideoId(
                availableVideoIds = listOf("video-a", "video-b"),
                requestedVideoId = "missing-video",
            ),
        )
    }
}
"""
)

gradle = Path("app/build.gradle.kts")
gradle_source = gradle.read_text()
old_version = 'versionName = "2.0.0-rc24-light-transition-polish"'
if gradle_source.count(old_version) != 1:
    raise SystemExit(f"app/build.gradle.kts: version marker mismatch ({gradle_source.count(old_version)})")
gradle.write_text(
    gradle_source.replace(
        old_version,
        'versionName = "2.0.0-rc25-reminder-insights-polish"',
        1,
    )
)

apk_workflow = Path(".github/workflows/android-apk.yml")
apk_source = apk_workflow.read_text()
apk_workflow.write_text(
    apk_source.replace(
        "name: Backlot-v147-Idea-Reminders",
        "name: Backlot-v148-Reminder-Insights-Polish",
        1,
    )
)
