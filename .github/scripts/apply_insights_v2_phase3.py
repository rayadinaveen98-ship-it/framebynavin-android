from pathlib import Path


def replace_exact(path: str, old: str, new: str):
    p = Path(path)
    text = p.read_text()
    count = text.count(old)
    if count != 1:
        raise SystemExit(f"Expected exactly one match in {path}, found {count}: {old[:100]!r}")
    p.write_text(text.replace(old, new, 1))


v172 = "app/src/main/java/com/framebynavin/app/ui/V172InsightsUi.kt"
replace_exact(
    v172,
    "    foundationRevision: Int = 0,\n    foundationLoading: Boolean = false,\n    onLinkVideo: (YouTubeVideoSnapshot) -> Unit,\n",
    "    foundationRevision: Int = 0,\n    foundationLoading: Boolean = false,\n    onCreateProject: () -> Unit = {},\n    onLinkVideo: (YouTubeVideoSnapshot) -> Unit,\n",
)
replace_exact(
    v172,
    "            foundationRevision = foundationRevision,\n            foundationLoading = foundationLoading,\n            onVideo = { detailVideoId = it.videoId },\n",
    "            foundationRevision = foundationRevision,\n            foundationLoading = foundationLoading,\n            onCreateProject = onCreateProject,\n            onVideo = { detailVideoId = it.videoId },\n",
)
replace_exact(
    v172,
    "    foundationRevision: Int,\n    foundationLoading: Boolean,\n    onVideo: (YouTubeVideoSnapshot) -> Unit,\n",
    "    foundationRevision: Int,\n    foundationLoading: Boolean,\n    onCreateProject: () -> Unit,\n    onVideo: (YouTubeVideoSnapshot) -> Unit,\n",
)
replace_exact(
    v172,
    "    V20InsightsFoundationCard(snapshot, foundationRevision, loading = foundationLoading)\n",
    "    V20InsightsFoundationCard(snapshot, foundationRevision, loading = foundationLoading, onCreateProject = onCreateProject)\n",
)
replace_exact(
    v172,
    "            Text(signal.body, color = MutedText, fontSize = 9.sp, lineHeight = 13.sp)\n",
    """            Text(signal.body, color = MutedText, fontSize = 9.sp, lineHeight = 13.sp)
            Spacer(Modifier.height(7.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("${signal.confidence}% CONFIDENCE", color = accent.copy(alpha = .88f), fontSize = 6.8.sp, fontWeight = FontWeight.Black, modifier = Modifier.weight(1f))
                Icon(Icons.Outlined.ChevronRight, "Open evidence", tint = MutedText, modifier = Modifier.size(14.dp))
            }
            signal.action?.let { action ->
                Spacer(Modifier.height(4.dp))
                Text("NEXT · $action", color = ProjectorIvory.copy(alpha = .86f), fontSize = 8.1.sp, lineHeight = 11.5.sp, fontWeight = FontWeight.Medium)
            }
""",
)
replace_exact(
    v172,
    "    val video = performance.video\n    AlertDialog(\n",
    "    val video = performance.video\n    var showDeepAnalytics by rememberSaveable(video.videoId) { mutableStateOf(false) }\n    AlertDialog(\n",
)
replace_exact(
    v172,
    """                linkedTask?.let { task ->
                    Spacer(Modifier.height(12.dp))
                    V20VideoPostmortemCard(task = task, video = video, windowDays = windowDays)
                }
""",
    """                linkedTask?.let { task ->
                    Spacer(Modifier.height(12.dp))
                    Surface(Modifier.fillMaxWidth(), RoundedCornerShape(14.dp), Color(0xFF171719), border = BorderStroke(1.dp, MutedGold.copy(alpha = .25f))) {
                        Column(Modifier.padding(12.dp)) {
                            Text("QUICK READ", color = MutedGold, fontSize = 7.2.sp, fontWeight = FontWeight.Black)
                            Spacer(Modifier.height(3.dp))
                            Text(
                                when {
                                    performance.baselineMultiple >= 1.5 -> "This is clearly outperforming your recent-video baseline. Study the promise, opening and packaging before making a related follow-up."
                                    performance.baselineMultiple in 0.01..0.75 -> "This is below your recent-video baseline. Check packaging and early pacing before repeating the same approach."
                                    video.netSubscribers > 0 -> "Performance is near your baseline, but it is converting some viewers into subscribers. Preserve what earns that commitment."
                                    else -> "Performance is near your current baseline. Collect more evidence before making a large strategy change."
                                },
                                color = ProjectorIvory,
                                fontSize = 8.7.sp,
                                lineHeight = 12.5.sp,
                            )
                            TextButton(onClick = { showDeepAnalytics = !showDeepAnalytics }, contentPadding = PaddingValues(0.dp)) {
                                Text(if (showDeepAnalytics) "HIDE DEEP ANALYTICS" else "VIEW DEEP ANALYTICS", color = MutedGold, fontSize = 7.5.sp, fontWeight = FontWeight.Black)
                            }
                        }
                    }
                    if (showDeepAnalytics) {
                        Spacer(Modifier.height(8.dp))
                        V20VideoPostmortemCard(task = task, video = video, windowDays = windowDays)
                    }
                }
""",
)

v11 = "app/src/main/java/com/framebynavin/app/ui/V11YouTubeInsights.kt"
replace_exact(
    v11,
    "                    foundationLoading = deepInsightsRefreshing,\n                    onLinkVideo = { selectedVideo = it },\n",
    "                    foundationLoading = deepInsightsRefreshing,\n                    onCreateProject = onAdd,\n                    onLinkVideo = { selectedVideo = it },\n",
)

foundation = "app/src/main/java/com/framebynavin/app/ui/V20InsightsFoundationUi.kt"
replace_exact(
    foundation,
    "    loading: Boolean = false,\n) {",
    "    loading: Boolean = false,\n    onCreateProject: () -> Unit = {},\n) {",
)
p = Path(foundation)
text = p.read_text()
old = "V20OpportunityEngineInsightsCard(snapshot)"
if text.count(old) != 2:
    raise SystemExit(f"Expected two opportunity card calls, found {text.count(old)}")
p.write_text(text.replace(old, "V20OpportunityEngineInsightsCard(snapshot, onCreateProject)"))

opportunity = "app/src/main/java/com/framebynavin/app/ui/V20OpportunityEngineUi.kt"
replace_exact(
    opportunity,
    "internal fun V20OpportunityEngineInsightsCard(analytics: YouTubeAnalyticsSnapshot) {",
    "internal fun V20OpportunityEngineInsightsCard(analytics: YouTubeAnalyticsSnapshot, onCreateProject: (() -> Unit)? = null) {",
)
replace_exact(
    opportunity,
    "        onBrainControl = null,\n        onAction = null,\n    )\n}",
    "        onBrainControl = null,\n        actionLabelOverride = if (onCreateProject != null) \"START THIS MOVE\" else null,\n        onAction = onCreateProject?.let { create -> { _: CreatorOpportunity -> create() } },\n    )\n}",
)
replace_exact(
    opportunity,
    "    onBrainControl: ((String, CreatorBrainLearningControl) -> Unit)?,\n    onAction: ((CreatorOpportunity) -> Unit)?,\n) {",
    "    onBrainControl: ((String, CreatorBrainLearningControl) -> Unit)?,\n    onAction: ((CreatorOpportunity) -> Unit)?,\n    actionLabelOverride: String? = null,\n) {",
)
replace_exact(
    opportunity,
    "                    Text(primary.actionLabel, fontSize = 9.sp, fontWeight = FontWeight.Black)\n",
    "                    Text(actionLabelOverride ?: primary.actionLabel, fontSize = 9.sp, fontWeight = FontWeight.Black)\n",
)
