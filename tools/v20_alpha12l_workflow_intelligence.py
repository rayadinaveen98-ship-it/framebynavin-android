from pathlib import Path


def replace_once(path: str, old: str, new: str, label: str) -> None:
    p = Path(path)
    text = p.read_text()
    count = text.count(old)
    if count != 1:
        raise SystemExit(f"{label}: expected exactly one anchor in {path}, got {count}")
    p.write_text(text.replace(old, new, 1))


replace_once(
    "app/build.gradle.kts",
    'versionCode = 99\n        versionName = "2.0.0-alpha1.2k.1-reach-reporting"',
    'versionCode = 100\n        versionName = "2.0.0-alpha1.2l-workflow-intelligence"',
    "version bump",
)

history = "app/src/main/java/com/framebynavin/app/data/ProjectPulseHistoryStore.kt"
replace_once(
    history,
    '''    fun clear() = synchronized(lock) {\n        check(prefs.edit().clear().commit()) { "Could not clear Project Pulse history" }\n    }\n\n    private fun encode(events: List<ProjectPulseHistoryEvent>): String = JSONArray().apply {''',
    '''    fun clear() = synchronized(lock) {\n        check(prefs.edit().clear().commit()) { "Could not clear Project Pulse history" }\n    }\n\n    fun loadAll(): List<ProjectPulseHistoryEvent> = synchronized(lock) {\n        prefs.all.keys.flatMap { taskId -> load(taskId) }.sortedBy { it.atMillis }\n    }\n\n    /** Creator-owned workflow evidence is portable; active reminder delivery state is not. */\n    fun exportJson(): String = synchronized(lock) {\n        JSONObject().apply {\n            prefs.all.forEach { (taskId, value) ->\n                if (value is String) {\n                    decode(taskId, value) // validate before exporting\n                    put(taskId, JSONArray(value))\n                }\n            }\n        }.toString()\n    }\n\n    fun validateJson(raw: String): Int {\n        val root = JSONObject(raw)\n        var count = 0\n        root.keys().forEach { taskId ->\n            val encoded = root.getJSONArray(taskId).toString()\n            count += decode(taskId, encoded).size\n        }\n        return count\n    }\n\n    fun importJson(raw: String) = synchronized(lock) {\n        validateJson(raw)\n        val root = JSONObject(raw)\n        val editor = prefs.edit().clear()\n        root.keys().forEach { taskId ->\n            editor.putString(taskId, root.getJSONArray(taskId).toString())\n        }\n        check(editor.commit()) { "Could not restore Project Pulse history" }\n    }\n\n    private fun encode(events: List<ProjectPulseHistoryEvent>): String = JSONArray().apply {''',
    "history portability",
)

backup = "app/src/main/java/com/framebynavin/app/data/CreatorBackupManager.kt"
replace_once(
    backup,
    '''        val youtubeMilestonesJson: String? = null,\n        val youtubePublishCheckpointsJson: String? = null,\n    )''',
    '''        val youtubeMilestonesJson: String? = null,\n        val youtubePublishCheckpointsJson: String? = null,\n        val projectPulseHistoryJson: String? = null,\n    )''',
    "backup pulse snapshot field",
)
replace_once(
    backup,
    '''    private val rewardStore = CreatorRewardStore(appContext)\n    private val regularScheduler = ReminderScheduler(appContext)''',
    '''    private val rewardStore = CreatorRewardStore(appContext)\n    private val projectPulseHistoryStore = ProjectPulseHistoryStore(appContext)\n    private val regularScheduler = ReminderScheduler(appContext)''',
    "backup pulse store",
)
replace_once(
    backup,
    '''        optionalSection(root, "youtubePublishCheckpoints")?.let { JSONArray(it) }\n\n        val projectCount = taskStore.validateJson(tasksRaw)''',
    '''        optionalSection(root, "youtubePublishCheckpoints")?.let { JSONArray(it) }\n        if (schema >= 7) {\n            require(root.has("projectPulseHistory")) { "Backup is missing Project Pulse workflow history" }\n        }\n        optionalSection(root, "projectPulseHistory")?.let(projectPulseHistoryStore::validateJson)\n\n        val projectCount = taskStore.validateJson(tasksRaw)''',
    "backup pulse validation",
)
replace_once(
    backup,
    '''        youtubeMilestonesJson = youtubeMilestonesRaw(),\n        youtubePublishCheckpointsJson = youtubePublishCheckpointsRaw(),\n    )''',
    '''        youtubeMilestonesJson = youtubeMilestonesRaw(),\n        youtubePublishCheckpointsJson = youtubePublishCheckpointsRaw(),\n        projectPulseHistoryJson = projectPulseHistoryStore.exportJson(),\n    )''',
    "backup pulse export",
)
replace_once(
    backup,
    '''            .put("youtubeMilestones", requireNotNull(snapshot.youtubeMilestonesJson))\n            .put("youtubePublishCheckpoints", requireNotNull(snapshot.youtubePublishCheckpointsJson))\n            .put("manifest", backupManifest())''',
    '''            .put("youtubeMilestones", requireNotNull(snapshot.youtubeMilestonesJson))\n            .put("youtubePublishCheckpoints", requireNotNull(snapshot.youtubePublishCheckpointsJson))\n            .put("projectPulseHistory", requireNotNull(snapshot.projectPulseHistoryJson))\n            .put("manifest", backupManifest())''',
    "backup pulse encode",
)
replace_once(
    backup,
    '''        val allKeys = when {\n            schema >= 6 -> keys + listOf("youtubeProjectLinks", "youtubeMilestones", "youtubePublishCheckpoints")\n            schema >= 4 -> keys + listOf("youtubeProjectLinks", "youtubeMilestones")''',
    '''        val allKeys = when {\n            schema >= 7 -> keys + listOf("youtubeProjectLinks", "youtubeMilestones", "youtubePublishCheckpoints", "projectPulseHistory")\n            schema >= 6 -> keys + listOf("youtubeProjectLinks", "youtubeMilestones", "youtubePublishCheckpoints")\n            schema >= 4 -> keys + listOf("youtubeProjectLinks", "youtubeMilestones")''',
    "backup pulse fingerprint",
)
replace_once(
    backup,
    '''            youtubeMilestonesJson = optionalSection(root, "youtubeMilestones"),\n            youtubePublishCheckpointsJson = optionalSection(root, "youtubePublishCheckpoints"),\n        )''',
    '''            youtubeMilestonesJson = optionalSection(root, "youtubeMilestones"),\n            youtubePublishCheckpointsJson = optionalSection(root, "youtubePublishCheckpoints"),\n            projectPulseHistoryJson = optionalSection(root, "projectPulseHistory"),\n        )''',
    "backup pulse decode",
)
replace_once(
    backup,
    '''        snapshot.youtubeMilestonesJson?.let { importYoutubeMilestones(it) }\n        snapshot.youtubePublishCheckpointsJson?.let { importYoutubePublishCheckpoints(it) }\n        // A portable restore never imports OAuth state or derived analytics.''',
    '''        snapshot.youtubeMilestonesJson?.let { importYoutubeMilestones(it) }\n        snapshot.youtubePublishCheckpointsJson?.let { importYoutubePublishCheckpoints(it) }\n        snapshot.projectPulseHistoryJson?.let { projectPulseHistoryStore.importJson(it) }\n        // A portable restore never imports OAuth state or derived analytics.''',
    "backup pulse restore",
)
replace_once(
    backup,
    '''        const val SCHEMA_VERSION = 6\n        private val INCLUDED_SECTIONS_V5 = listOf(\n            "tasks", "ideas", "weeklySchedule", "settings", "smartEscalationConfig",\n            "postPublish", "rewards", "personalFrames", "youtubeProjectLinks", "youtubeMilestones",\n        )\n        private val INCLUDED_SECTIONS = INCLUDED_SECTIONS_V5 + "youtubePublishCheckpoints"\n        private val ROOT_KEYS = setOf("format", "schemaVersion", "createdAtMillis", "manifest",\n            "payloadSha256") + INCLUDED_SECTIONS\n        private fun includedSectionsFor(schema: Int): List<String> =\n            if (schema >= 6) INCLUDED_SECTIONS else INCLUDED_SECTIONS_V5''',
    '''        const val SCHEMA_VERSION = 7\n        private val INCLUDED_SECTIONS_V5 = listOf(\n            "tasks", "ideas", "weeklySchedule", "settings", "smartEscalationConfig",\n            "postPublish", "rewards", "personalFrames", "youtubeProjectLinks", "youtubeMilestones",\n        )\n        private val INCLUDED_SECTIONS_V6 = INCLUDED_SECTIONS_V5 + "youtubePublishCheckpoints"\n        private val INCLUDED_SECTIONS = INCLUDED_SECTIONS_V6 + "projectPulseHistory"\n        private val ROOT_KEYS = setOf("format", "schemaVersion", "createdAtMillis", "manifest",\n            "payloadSha256") + INCLUDED_SECTIONS\n        private fun includedSectionsFor(schema: Int): List<String> = when {\n            schema >= 7 -> INCLUDED_SECTIONS\n            schema >= 6 -> INCLUDED_SECTIONS_V6\n            else -> INCLUDED_SECTIONS_V5\n        }''',
    "backup schema v7",
)

ui = "app/src/main/java/com/framebynavin/app/ui/V172InsightsUi.kt"
replace_once(
    ui,
    '''    Spacer(Modifier.height(18.dp))\n    val formats = YouTubeInsightEngine.formatPerformance(snapshot, tasks, links)''',
    '''    Spacer(Modifier.height(10.dp))\n    V20WorkflowIntelligenceCard(tasks)\n\n    Spacer(Modifier.height(18.dp))\n    val formats = YouTubeInsightEngine.formatPerformance(snapshot, tasks, links)''',
    "workflow intelligence card",
)

print("Materialized v100 Workflow Intelligence measurement foundation")
