from pathlib import Path


def replace_once(path: str, old: str, new: str, label: str) -> None:
    p = Path(path)
    text = p.read_text()
    count = text.count(old)
    if count != 1:
        raise SystemExit(f"{label}: expected exactly one anchor in {path}, got {count}")
    p.write_text(text.replace(old, new, 1))


# Installable milestone identity.
replace_once(
    "app/build.gradle.kts",
    'versionCode = 97\n        versionName = "2.0.0-alpha1.2j-dna-blueprints"',
    'versionCode = 98\n        versionName = "2.0.0-alpha1.2k-insights-foundation-2"',
    "version bump",
)

# YouTube sync: base analytics stays authoritative; deeper datasets are optional and saved only
# after the request epoch is accepted. Post-publish checkpoints are creator-owned learning history.
replace_once(
    "app/src/main/java/com/framebynavin/app/ui/V11YouTubeInsights.kt",
    '''    val store = remember { YouTubeAnalyticsStore(context.applicationContext) }\n    val api = remember { YouTubeApiClient() }''',
    '''    val store = remember { YouTubeAnalyticsStore(context.applicationContext) }\n    val api = remember { YouTubeApiClient() }\n    val foundationApi = remember { YouTubeInsightsFoundationClient() }\n    val foundationStore = remember { YouTubeInsightsFoundationStore(context.applicationContext) }\n    val checkpointStore = remember { YouTubePublishCheckpointStore(context.applicationContext) }''',
    "insights foundation dependencies",
)
replace_once(
    "app/src/main/java/com/framebynavin/app/ui/V11YouTubeInsights.kt",
    '''                val fresh = withContext(Dispatchers.IO) { api.sync(token, days) }\n                if (store.save(fresh, request) && isActive(request)) {\n                    snapshot = fresh\n                    links = store.links()\n                    selectedVideo = null\n                }''',
    '''                val (fresh, foundation) = withContext(Dispatchers.IO) {\n                    val base = api.sync(token, days)\n                    base to foundationApi.sync(token, base)\n                }\n                if (store.save(fresh, request) && isActive(request)) {\n                    foundationStore.save(foundation)\n                    checkpointStore.captureFrom(fresh, store.links())\n                    snapshot = fresh\n                    links = store.links()\n                    selectedVideo = null\n                }''',
    "deeper sync integration",
)
replace_once(
    "app/src/main/java/com/framebynavin/app/ui/V11YouTubeInsights.kt",
    '''        store.disconnect()\n        activeRequest = null''',
    '''        store.disconnect()\n        foundationStore.clear()\n        activeRequest = null''',
    "disconnect foundation cache",
)

# Additive proof surface; do not redesign the accepted Insights page yet.
replace_once(
    "app/src/main/java/com/framebynavin/app/ui/V172InsightsUi.kt",
    '''    V172PulseCard(snapshot)\n    Spacer(Modifier.height(18.dp))''',
    '''    V172PulseCard(snapshot)\n    Spacer(Modifier.height(10.dp))\n    V20InsightsFoundationCard(snapshot)\n    Spacer(Modifier.height(18.dp))''',
    "foundation status card",
)

# Derived foundation cache follows analytics identity lifecycle. Checkpoints deliberately do not.
replace_once(
    "app/src/main/java/com/framebynavin/app/youtube/YouTubeAnalyticsStore.kt",
    '''        YouTubePulseStore(appContext).clear()\n        latest24HourReport = null''',
    '''        YouTubePulseStore(appContext).clear()\n        YouTubeInsightsFoundationStore(appContext).clear()\n        latest24HourReport = null''',
    "analytics cache invalidation",
)

# Creator backup schema v6 adds durable post-publish checkpoints while preserving v1-v5 validation.
backup = "app/src/main/java/com/framebynavin/app/data/CreatorBackupManager.kt"
replace_once(
    backup,
    '''        val youtubeLinksJson: String? = null,\n        val youtubeMilestonesJson: String? = null,\n    )''',
    '''        val youtubeLinksJson: String? = null,\n        val youtubeMilestonesJson: String? = null,\n        val youtubePublishCheckpointsJson: String? = null,\n    )''',
    "backup snapshot field",
)
replace_once(
    backup,
    'if (schema >= 5) validateManifest(root)',
    'if (schema >= 5) validateManifest(root, schema)',
    "version-aware manifest validation",
)
replace_once(
    backup,
    '''        optionalSection(root, "youtubeProjectLinks")?.let { JSONObject(it) }\n        optionalSection(root, "youtubeMilestones")?.let { JSONObject(it) }''',
    '''        optionalSection(root, "youtubeProjectLinks")?.let { JSONObject(it) }\n        optionalSection(root, "youtubeMilestones")?.let { JSONObject(it) }\n        if (schema >= 6) {\n            require(root.has("youtubePublishCheckpoints")) { "Backup is missing YouTube publish checkpoints" }\n        }\n        optionalSection(root, "youtubePublishCheckpoints")?.let { JSONArray(it) }''',
    "checkpoint validation",
)
replace_once(
    backup,
    '''        youtubeLinksJson = youtubeLinksRaw(),\n        youtubeMilestonesJson = youtubeMilestonesRaw(),\n    )''',
    '''        youtubeLinksJson = youtubeLinksRaw(),\n        youtubeMilestonesJson = youtubeMilestonesRaw(),\n        youtubePublishCheckpointsJson = youtubePublishCheckpointsRaw(),\n    )''',
    "checkpoint snapshot export",
)
replace_once(
    backup,
    '''            .put("youtubeProjectLinks", requireNotNull(snapshot.youtubeLinksJson))\n            .put("youtubeMilestones", requireNotNull(snapshot.youtubeMilestonesJson))\n            .put("manifest", backupManifest())''',
    '''            .put("youtubeProjectLinks", requireNotNull(snapshot.youtubeLinksJson))\n            .put("youtubeMilestones", requireNotNull(snapshot.youtubeMilestonesJson))\n            .put("youtubePublishCheckpoints", requireNotNull(snapshot.youtubePublishCheckpointsJson))\n            .put("manifest", backupManifest())''',
    "checkpoint encoded section",
)
replace_once(
    backup,
    '''        val allKeys = if (schema >= 4) keys + listOf("youtubeProjectLinks", "youtubeMilestones") else keys\n        (if (schema >= 5) allKeys + "manifest" else allKeys).forEach { key ->''',
    '''        val allKeys = when {\n            schema >= 6 -> keys + listOf("youtubeProjectLinks", "youtubeMilestones", "youtubePublishCheckpoints")\n            schema >= 4 -> keys + listOf("youtubeProjectLinks", "youtubeMilestones")\n            else -> keys\n        }\n        (if (schema >= 5) allKeys + "manifest" else allKeys).forEach { key ->''',
    "schema-compatible fingerprint",
)
replace_once(
    backup,
    '''    private fun validateManifest(root: JSONObject) {\n        require(INCLUDED_SECTIONS.all { root.has(it) && !root.isNull(it) }) {''',
    '''    private fun validateManifest(root: JSONObject, schema: Int) {\n        val includedSections = includedSectionsFor(schema)\n        val rootKeys = setOf("format", "schemaVersion", "createdAtMillis", "manifest", "payloadSha256") + includedSections\n        require(includedSections.all { root.has(it) && !root.isNull(it) }) {''',
    "manifest dynamic sections",
)
replace_once(
    backup,
    '''        require(root.keys().asSequence().toSet() == ROOT_KEYS) {''',
    '''        require(root.keys().asSequence().toSet() == rootKeys) {''',
    "manifest dynamic root keys",
)
replace_once(
    backup,
    '''        require((0 until included.length()).map(included::getString) == INCLUDED_SECTIONS) {''',
    '''        require((0 until included.length()).map(included::getString) == includedSections) {''',
    "manifest dynamic included list",
)
replace_once(
    backup,
    '''            youtubeLinksJson = optionalSection(root, "youtubeProjectLinks"),\n            youtubeMilestonesJson = optionalSection(root, "youtubeMilestones"),\n        )''',
    '''            youtubeLinksJson = optionalSection(root, "youtubeProjectLinks"),\n            youtubeMilestonesJson = optionalSection(root, "youtubeMilestones"),\n            youtubePublishCheckpointsJson = optionalSection(root, "youtubePublishCheckpoints"),\n        )''',
    "checkpoint decode",
)
replace_once(
    backup,
    '''        snapshot.youtubeLinksJson?.let { importYoutubeLinks(it) }\n        snapshot.youtubeMilestonesJson?.let { importYoutubeMilestones(it) }\n        // A portable restore never imports OAuth state or derived analytics.''',
    '''        snapshot.youtubeLinksJson?.let { importYoutubeLinks(it) }\n        snapshot.youtubeMilestonesJson?.let { importYoutubeMilestones(it) }\n        snapshot.youtubePublishCheckpointsJson?.let { importYoutubePublishCheckpoints(it) }\n        // A portable restore never imports OAuth state or derived analytics.''',
    "checkpoint restore",
)
replace_once(
    backup,
    '''    private fun stopAndCancel(tasks: List<CreatorTask>) {''',
    '''    private fun youtubePublishCheckpointsRaw(): String =\n        com.framebynavin.app.youtube.YouTubePublishCheckpointStore(appContext).exportJson().toString()\n\n    private fun importYoutubePublishCheckpoints(raw: String) {\n        com.framebynavin.app.youtube.YouTubePublishCheckpointStore(appContext).importJson(JSONArray(raw))\n    }\n\n    private fun stopAndCancel(tasks: List<CreatorTask>) {''',
    "checkpoint backup helpers",
)
replace_once(
    backup,
    '''        const val SCHEMA_VERSION = 5\n        private val INCLUDED_SECTIONS = listOf(\n            "tasks", "ideas", "weeklySchedule", "settings", "smartEscalationConfig",\n            "postPublish", "rewards", "personalFrames", "youtubeProjectLinks", "youtubeMilestones",\n        )\n        private val ROOT_KEYS = setOf("format", "schemaVersion", "createdAtMillis", "manifest",\n            "payloadSha256") + INCLUDED_SECTIONS''',
    '''        const val SCHEMA_VERSION = 6\n        private val INCLUDED_SECTIONS_V5 = listOf(\n            "tasks", "ideas", "weeklySchedule", "settings", "smartEscalationConfig",\n            "postPublish", "rewards", "personalFrames", "youtubeProjectLinks", "youtubeMilestones",\n        )\n        private val INCLUDED_SECTIONS = INCLUDED_SECTIONS_V5 + "youtubePublishCheckpoints"\n        private val ROOT_KEYS = setOf("format", "schemaVersion", "createdAtMillis", "manifest",\n            "payloadSha256") + INCLUDED_SECTIONS\n        private fun includedSectionsFor(schema: Int): List<String> =\n            if (schema >= 6) INCLUDED_SECTIONS else INCLUDED_SECTIONS_V5''',
    "backup schema v6 constants",
)

print("Materialized v98 Insights Foundation 2.0 integration")
