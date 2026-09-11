from pathlib import Path


def replace_once(path: str, old: str, new: str, label: str) -> None:
    p = Path(path)
    text = p.read_text()
    count = text.count(old)
    if count != 1:
        raise SystemExit(f"{label}: expected one anchor in {path}, got {count}")
    p.write_text(text.replace(old, new, 1))


replace_once(
    "app/build.gradle.kts",
    'versionCode = 98\n        versionName = "2.0.0-alpha1.2k-insights-foundation-2"',
    'versionCode = 99\n        versionName = "2.0.0-alpha1.2k.1-reach-reporting"',
    "version bump",
)

foundation = "app/src/main/java/com/framebynavin/app/youtube/YouTubeInsightsFoundation.kt"
replace_once(
    foundation,
    'enum class YouTubeDatasetState { READY, EMPTY, UNAVAILABLE, NOT_CONFIGURED }',
    'enum class YouTubeDatasetState { READY, EMPTY, PENDING, UNAVAILABLE, NOT_CONFIGURED }',
    "pending reach state",
)

insights = "app/src/main/java/com/framebynavin/app/ui/V11YouTubeInsights.kt"
replace_once(
    insights,
    '''    val foundationStore = remember { YouTubeInsightsFoundationStore(context.applicationContext) }\n    val checkpointStore = remember { YouTubePublishCheckpointStore(context.applicationContext) }''',
    '''    val foundationStore = remember { YouTubeInsightsFoundationStore(context.applicationContext) }\n    val checkpointStore = remember { YouTubePublishCheckpointStore(context.applicationContext) }\n    val reachStore = remember { YouTubeReachStore(context.applicationContext) }\n    val reachApi = remember { YouTubeReachReportingClient(reachStore) }''',
    "reach dependencies",
)
replace_once(
    insights,
    '''                if (store.save(fresh, request) && isActive(request)) {\n                    foundationStore.save(foundation)\n                    checkpointStore.captureFrom(fresh, store.links())\n                    snapshot = fresh\n                    links = store.links()\n                    selectedVideo = null\n                }''',
    '''                if (store.save(fresh, request) && isActive(request)) {\n                    val reach = withContext(Dispatchers.IO) {\n                        reachApi.sync(token, fresh.channel.channelId)\n                    }\n                    val withReach = foundation.copy(\n                        health = foundation.health\n                            .filterNot { it.dataset == YouTubeFoundationDataset.REACH } +\n                            YouTubeDatasetHealth(YouTubeFoundationDataset.REACH, reach.state, reach.note)\n                    )\n                    foundationStore.save(withReach)\n                    checkpointStore.captureFrom(fresh, store.links())\n                    snapshot = fresh\n                    links = store.links()\n                    selectedVideo = null\n                }''',
    "reach sync",
)
replace_once(
    insights,
    '''        store.disconnect()\n        foundationStore.clear()\n        activeRequest = null''',
    '''        store.disconnect()\n        foundationStore.clear()\n        reachStore.clear()\n        activeRequest = null''',
    "reach disconnect cleanup",
)

ui = "app/src/main/java/com/framebynavin/app/ui/V20InsightsFoundationUi.kt"
replace_once(
    ui,
    '''import com.framebynavin.app.youtube.YouTubeInsightsFoundationStore\n''',
    '''import com.framebynavin.app.youtube.YouTubeInsightsFoundationStore\nimport com.framebynavin.app.youtube.YouTubeReachStore\n''',
    "reach ui import",
)
replace_once(
    ui,
    '''    val reach = foundation.health(YouTubeFoundationDataset.REACH)?.state\n\n    Surface(''',
    '''    val reachHealth = foundation.health(YouTubeFoundationDataset.REACH)\n    val reach = reachHealth?.state\n    val reachSummary = remember(snapshot.channel.channelId, snapshot.startDate, snapshot.endDate, snapshot.fetchedAtMillis) {\n        YouTubeReachStore(context).summary(snapshot.startDate, snapshot.endDate)\n    }\n\n    Surface(''',
    "reach summary load",
)
replace_once(
    ui,
    '''                    statusText("Geography", country),\n                    statusText("Retention", retention),\n                ).joinToString("  ·  "),''',
    '''                    statusText("Geography", country),\n                    statusText("Retention", retention),\n                    statusText("Reach", reach),\n                ).joinToString("  ·  "),''',
    "reach status row",
)
replace_once(
    ui,
    '''            if (reach == YouTubeDatasetState.NOT_CONFIGURED) {\n                Spacer(Modifier.height(8.dp))\n                Text(\n                    "Reach/CTR is intentionally not guessed. It will appear after the YouTube Reporting API reach importer is connected.",\n                    color = MutedText,\n                    fontSize = 8.1.sp,\n                    lineHeight = 12.sp,\n                )\n            }''',
    '''            Spacer(Modifier.height(8.dp))\n            when {\n                reach == YouTubeDatasetState.READY && reachSummary != null -> {\n                    Text("THUMBNAIL REACH", color = MutedGold, fontSize = 7.5.sp, fontWeight = FontWeight.Black, letterSpacing = .7.sp)\n                    Text(\n                        "${compactFoundation(reachSummary.impressions)} impressions · ${String.format(java.util.Locale.US, "%.1f", reachSummary.ctrPercent)}% CTR",\n                        color = ProjectorIvory,\n                        fontSize = 9.sp,\n                        fontWeight = FontWeight.Bold,\n                    )\n                }\n                reach == YouTubeDatasetState.PENDING -> Text(\n                    reachHealth?.note ?: "Reach reporting is waiting for YouTube's first daily report.",\n                    color = MutedText,\n                    fontSize = 8.1.sp,\n                    lineHeight = 12.sp,\n                )\n                reach == YouTubeDatasetState.UNAVAILABLE -> Text(\n                    reachHealth?.note ?: "Reach reporting is unavailable right now. Normal Insights still works.",\n                    color = MutedText,\n                    fontSize = 8.1.sp,\n                    lineHeight = 12.sp,\n                )\n                reach == YouTubeDatasetState.NOT_CONFIGURED -> Text(\n                    "Reach/CTR is intentionally not guessed. It will appear only from YouTube's official reach reports.",\n                    color = MutedText,\n                    fontSize = 8.1.sp,\n                    lineHeight = 12.sp,\n                )\n            }''',
    "reach status details",
)
replace_once(
    ui,
    '''    YouTubeDatasetState.EMPTY -> "$label —"\n    YouTubeDatasetState.UNAVAILABLE -> "$label unavailable"''',
    '''    YouTubeDatasetState.EMPTY -> "$label —"\n    YouTubeDatasetState.PENDING -> "$label waiting"\n    YouTubeDatasetState.UNAVAILABLE -> "$label unavailable"''',
    "pending status label",
)

print("Materialized v98.1 Reach Reporting integration")
