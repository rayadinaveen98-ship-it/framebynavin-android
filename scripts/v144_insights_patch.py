from pathlib import Path


def replace_once(text: str, old: str, new: str, label: str) -> str:
    count = text.count(old)
    if count != 1:
        raise SystemExit(f"{label}: expected 1 match, found {count}")
    return text.replace(old, new, 1)


p = Path("app/src/main/java/com/framebynavin/app/ui/V11YouTubeInsights.kt")
text = p.read_text()
text = replace_once(
    text,
    "import kotlinx.coroutines.CancellationException\nimport kotlinx.coroutines.Dispatchers",
    "import kotlinx.coroutines.CancellationException\nimport kotlinx.coroutines.Dispatchers\nimport kotlinx.coroutines.async",
    "V11 async import",
)
text = replace_once(
    text,
    '    var autoRefreshKey by rememberSaveable { mutableStateOf("") }\n',
    '    var autoRefreshKey by rememberSaveable { mutableStateOf("") }\n    var foundationRevision by remember { mutableIntStateOf(0) }\n',
    "V11 foundation revision state",
)
start_marker = "    fun syncWithToken(token: String, request: YouTubeCacheRequest, days: Int) {"
end_marker = "    val resolutionLauncher = rememberLauncherForActivityResult"
if text.count(start_marker) != 1 or text.count(end_marker) != 1:
    raise SystemExit("V11 sync markers were not unique")
start = text.index(start_marker)
end = text.index(end_marker)
new_sync = '''    fun syncWithToken(token: String, request: YouTubeCacheRequest, days: Int) {
        if (!isActive(request)) return
        scope.launch {
            syncing = true
            authError = null
            try {
                // Priority refresh: publish creator-facing data as soon as the core reports finish.
                val fresh = withContext(Dispatchers.IO) { api.sync(token, days) }
                if (store.save(fresh, request) && isActive(request)) {
                    snapshot = fresh
                    links = store.links()
                    selectedVideo = null
                    withContext(Dispatchers.IO) {
                        checkpointStore.captureFrom(fresh, store.links())
                    }

                    // Deep refresh stays in the background. Audience/retention and reach are
                    // independent and should not make cached/core Insights feel blocked.
                    val (foundationResult, reachResult) = withContext(Dispatchers.IO) {
                        val foundationDeferred = async { runCatching { foundationApi.sync(token, fresh) } }
                        val reachDeferred = async { runCatching { reachApi.sync(token, fresh.channel.channelId) } }
                        foundationDeferred.await() to reachDeferred.await()
                    }
                    val foundation = foundationResult.getOrNull()
                    if (foundation != null && isActive(request)) {
                        val reach = reachResult.getOrNull()
                        val reachHealth = if (reach != null) {
                            YouTubeDatasetHealth(YouTubeFoundationDataset.REACH, reach.state, reach.note)
                        } else {
                            YouTubeDatasetHealth(
                                YouTubeFoundationDataset.REACH,
                                YouTubeDatasetState.UNAVAILABLE,
                                "Reach could not be refreshed right now. Core Insights are up to date.",
                            )
                        }
                        val withReach = foundation.copy(
                            health = foundation.health
                                .filterNot { it.dataset == YouTubeFoundationDataset.REACH } + reachHealth,
                        )
                        withContext(Dispatchers.IO) { foundationStore.save(withReach) }
                        if (isActive(request)) foundationRevision += 1
                    }
                }
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (error: Throwable) {
                if (isActive(request)) {
                    if (error is YouTubeApiException && error.httpCode == 401) {
                        store.disconnect()
                        snapshot = null
                        selectedVideo = null
                    }
                    authError = ytFriendlyError(error)
                }
            } finally {
                if (activeRequest == request) {
                    activeRequest = null
                    pendingResolution = null
                    syncing = false
                }
            }
        }
    }

'''
text = text[:start] + new_sync + text[end:]
text = replace_once(
    text,
    "                    links = links,\n                    onLinkVideo = { selectedVideo = it },",
    "                    links = links,\n                    foundationRevision = foundationRevision,\n                    onLinkVideo = { selectedVideo = it },",
    "V11 pass foundation revision",
)
text = replace_once(
    text,
    '                    Text("${ytCompact(data.channel.subscribers)} subscribers · ${data.channel.videoCount} videos", color = MutedText, fontSize = 8.8.sp)\n',
    '                    Text("${ytCompact(data.channel.subscribers)} subscribers · ${data.channel.videoCount} videos", color = MutedText, fontSize = 8.8.sp)\n                    if (syncing) Text("Updating in background…", color = MutedGold, fontSize = 8.sp, fontWeight = FontWeight.Bold)\n',
    "V11 updating indicator",
)
p.write_text(text)

p = Path("app/src/main/java/com/framebynavin/app/ui/V172InsightsUi.kt")
text = p.read_text()
text = replace_once(
    text,
    "    links: Map<String, String>,\n    onLinkVideo: (YouTubeVideoSnapshot) -> Unit,\n) {",
    "    links: Map<String, String>,\n    foundationRevision: Int = 0,\n    onLinkVideo: (YouTubeVideoSnapshot) -> Unit,\n) {",
    "V172 body revision parameter",
)
text = replace_once(
    text,
    "            links = links,\n            onVideo = { detailVideoId = it.videoId },",
    "            links = links,\n            foundationRevision = foundationRevision,\n            onVideo = { detailVideoId = it.videoId },",
    "V172 overview revision call",
)
text = replace_once(
    text,
    "    links: Map<String, String>,\n    onVideo: (YouTubeVideoSnapshot) -> Unit,\n    onDetail: (V20InsightsDrilldownRequest) -> Unit,\n) {\n    V172PulseCard(snapshot, onDetail)\n    Spacer(Modifier.height(10.dp))\n    V20InsightsFoundationCard(snapshot)",
    "    links: Map<String, String>,\n    foundationRevision: Int,\n    onVideo: (YouTubeVideoSnapshot) -> Unit,\n    onDetail: (V20InsightsDrilldownRequest) -> Unit,\n) {\n    V172PulseCard(snapshot, onDetail)\n    Spacer(Modifier.height(10.dp))\n    V20InsightsFoundationCard(snapshot, foundationRevision)",
    "V172 overview signature and foundation call",
)
p.write_text(text)

p = Path("app/src/main/java/com/framebynavin/app/ui/V20InsightsFoundationUi.kt")
text = p.read_text()
text = replace_once(
    text,
    "internal fun V20InsightsFoundationCard(snapshot: YouTubeAnalyticsSnapshot) {",
    "internal fun V20InsightsFoundationCard(snapshot: YouTubeAnalyticsSnapshot, refreshRevision: Int = 0) {",
    "V20 revision parameter",
)
text = replace_once(
    text,
    "    val foundation = remember(snapshot.channel.channelId, snapshot.windowDays, snapshot.fetchedAtMillis) {",
    "    val foundation = remember(snapshot.channel.channelId, snapshot.windowDays, snapshot.fetchedAtMillis, refreshRevision) {",
    "V20 foundation cache revision",
)
text = replace_once(
    text,
    "    val reachSummary = remember(snapshot.channel.channelId, snapshot.startDate, snapshot.endDate, snapshot.fetchedAtMillis) {",
    "    val reachSummary = remember(snapshot.channel.channelId, snapshot.startDate, snapshot.endDate, snapshot.fetchedAtMillis, refreshRevision) {",
    "V20 reach cache revision",
)
p.write_text(text)
