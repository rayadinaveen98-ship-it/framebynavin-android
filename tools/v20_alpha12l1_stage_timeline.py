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
    'versionCode = 100\n        versionName = "2.0.0-alpha1.2l-workflow-intelligence"',
    'versionCode = 101\n        versionName = "2.0.0-alpha1.2l.1-stage-timeline"',
    "version bump",
)

# TaskStore records durable stage residence only after a successful task write. Restores bypass
# transition recording so an imported snapshot cannot fabricate local workflow history.
task_store = "app/src/main/java/com/framebynavin/app/data/TaskStore.kt"
replace_once(
    task_store,
    '''class TaskStore(private val context: Context) {\n    private val tasksKey = stringPreferencesKey("tasks_json")''',
    '''class TaskStore(private val context: Context) {\n    private val workflowTimeline = CreatorWorkflowTimelineStore(context.applicationContext)\n    private val tasksKey = stringPreferencesKey("tasks_json")''',
    "task timeline dependency",
)
replace_once(
    task_store,
    '''    suspend fun load(): List<CreatorTask> = tasksFlow.first()''',
    '''    suspend fun load(): List<CreatorTask> {\n        val tasks = tasksFlow.first()\n        // Legacy/current projects begin as explicitly lower-bound observations. This sidecar must\n        // never make creator data unreadable if timeline persistence itself has a problem.\n        runCatching { workflowTimeline.seedCurrent(tasks) }\n        return tasks\n    }''',
    "task load timeline seed",
)
replace_once(
    task_store,
    '''    private suspend fun saveUnlocked(tasks: List<CreatorTask>) {\n        val encoded = encode(tasks)\n        context.creatorDataStore.edit { prefs ->\n            val current = prefs[tasksKey]\n            if (current != null) {\n                decode(current) // Never overwrite a damaged primary with an empty or stale snapshot.\n                prefs[tasksBackupKey] = current''',
    '''    private suspend fun saveUnlocked(tasks: List<CreatorTask>, recordWorkflowTimeline: Boolean = true) {\n        val encoded = encode(tasks)\n        var previous = emptyList<CreatorTask>()\n        context.creatorDataStore.edit { prefs ->\n            val current = prefs[tasksKey]\n            if (current != null) {\n                previous = decode(current) // Never overwrite a damaged primary with an empty or stale snapshot.\n                prefs[tasksBackupKey] = current''',
    "task save timeline capture",
)
replace_once(
    task_store,
    '''            prefs[tasksKey] = encoded\n        }\n        CreatorWidgetUpdater.updateAll(context, tasks)\n    }''',
    '''            prefs[tasksKey] = encoded\n        }\n        if (recordWorkflowTimeline) {\n            // Core task persistence stays authoritative. Timeline evidence is a best-effort sidecar.\n            runCatching { workflowTimeline.recordTransitions(previous, tasks) }\n        }\n        CreatorWidgetUpdater.updateAll(context, tasks)\n    }''',
    "task save timeline record",
)
replace_once(
    task_store,
    '''    suspend fun importJson(raw: String): List<CreatorTask> {\n        val decoded = decode(raw)\n        save(decoded)\n        return decoded\n    }''',
    '''    suspend fun importJson(raw: String): List<CreatorTask> {\n        val decoded = decode(raw)\n        CreatorDataGate.transaction {\n            creatorTaskMutationMutex.withLock { saveUnlocked(decoded, recordWorkflowTimeline = false) }\n        }\n        return decoded\n    }''',
    "restore bypass timeline",
)

# Workflow stage timeline is creator-owned learning history, so portable backups move to schema 8.
backup = "app/src/main/java/com/framebynavin/app/data/CreatorBackupManager.kt"
replace_once(
    backup,
    '''        val youtubePublishCheckpointsJson: String? = null,\n        val projectPulseHistoryJson: String? = null,\n    )''',
    '''        val youtubePublishCheckpointsJson: String? = null,\n        val projectPulseHistoryJson: String? = null,\n        val workflowTimelineJson: String? = null,\n    )''',
    "backup timeline snapshot field",
)
replace_once(
    backup,
    '''    private val projectPulseHistoryStore = ProjectPulseHistoryStore(appContext)\n    private val regularScheduler = ReminderScheduler(appContext)''',
    '''    private val projectPulseHistoryStore = ProjectPulseHistoryStore(appContext)\n    private val workflowTimelineStore = CreatorWorkflowTimelineStore(appContext)\n    private val regularScheduler = ReminderScheduler(appContext)''',
    "backup timeline store",
)
replace_once(
    backup,
    '''        optionalSection(root, "projectPulseHistory")?.let(projectPulseHistoryStore::validateJson)\n\n        val projectCount = taskStore.validateJson(tasksRaw)''',
    '''        optionalSection(root, "projectPulseHistory")?.let(projectPulseHistoryStore::validateJson)\n        if (schema >= 8) {\n            require(root.has("workflowStageTimeline")) { "Backup is missing workflow stage timeline" }\n        }\n        optionalSection(root, "workflowStageTimeline")?.let(workflowTimelineStore::validateJson)\n\n        val projectCount = taskStore.validateJson(tasksRaw)''',
    "backup timeline validation",
)
replace_once(
    backup,
    '''        youtubePublishCheckpointsJson = youtubePublishCheckpointsRaw(),\n        projectPulseHistoryJson = projectPulseHistoryStore.exportJson(),\n    )''',
    '''        youtubePublishCheckpointsJson = youtubePublishCheckpointsRaw(),\n        projectPulseHistoryJson = projectPulseHistoryStore.exportJson(),\n        workflowTimelineJson = workflowTimelineStore.exportJson(),\n    )''',
    "backup timeline export",
)
replace_once(
    backup,
    '''            .put("youtubePublishCheckpoints", requireNotNull(snapshot.youtubePublishCheckpointsJson))\n            .put("projectPulseHistory", requireNotNull(snapshot.projectPulseHistoryJson))\n            .put("manifest", backupManifest())''',
    '''            .put("youtubePublishCheckpoints", requireNotNull(snapshot.youtubePublishCheckpointsJson))\n            .put("projectPulseHistory", requireNotNull(snapshot.projectPulseHistoryJson))\n            .put("workflowStageTimeline", requireNotNull(snapshot.workflowTimelineJson))\n            .put("manifest", backupManifest())''',
    "backup timeline encode",
)
replace_once(
    backup,
    '''        val allKeys = when {\n            schema >= 7 -> keys + listOf("youtubeProjectLinks", "youtubeMilestones", "youtubePublishCheckpoints", "projectPulseHistory")\n            schema >= 6 -> keys + listOf("youtubeProjectLinks", "youtubeMilestones", "youtubePublishCheckpoints")''',
    '''        val allKeys = when {\n            schema >= 8 -> keys + listOf("youtubeProjectLinks", "youtubeMilestones", "youtubePublishCheckpoints", "projectPulseHistory", "workflowStageTimeline")\n            schema >= 7 -> keys + listOf("youtubeProjectLinks", "youtubeMilestones", "youtubePublishCheckpoints", "projectPulseHistory")\n            schema >= 6 -> keys + listOf("youtubeProjectLinks", "youtubeMilestones", "youtubePublishCheckpoints")''',
    "backup timeline fingerprint",
)
replace_once(
    backup,
    '''            youtubePublishCheckpointsJson = optionalSection(root, "youtubePublishCheckpoints"),\n            projectPulseHistoryJson = optionalSection(root, "projectPulseHistory"),\n        )''',
    '''            youtubePublishCheckpointsJson = optionalSection(root, "youtubePublishCheckpoints"),\n            projectPulseHistoryJson = optionalSection(root, "projectPulseHistory"),\n            workflowTimelineJson = optionalSection(root, "workflowStageTimeline"),\n        )''',
    "backup timeline decode",
)
replace_once(
    backup,
    '''        snapshot.youtubePublishCheckpointsJson?.let { importYoutubePublishCheckpoints(it) }\n        snapshot.projectPulseHistoryJson?.let { projectPulseHistoryStore.importJson(it) }\n        // A portable restore never imports OAuth state or derived analytics.''',
    '''        snapshot.youtubePublishCheckpointsJson?.let { importYoutubePublishCheckpoints(it) }\n        snapshot.projectPulseHistoryJson?.let { projectPulseHistoryStore.importJson(it) }\n        if (snapshot.workflowTimelineJson != null) {\n            workflowTimelineStore.importJson(snapshot.workflowTimelineJson)\n        } else {\n            // A pre-v101 restore must not retain timeline evidence from the workspace being replaced.\n            workflowTimelineStore.clear()\n            workflowTimelineStore.seedCurrent(taskStore.load())\n        }\n        // A portable restore never imports OAuth state or derived analytics.''',
    "backup timeline restore",
)
replace_once(
    backup,
    '''        const val SCHEMA_VERSION = 7\n        private val INCLUDED_SECTIONS_V5 = listOf(\n            "tasks", "ideas", "weeklySchedule", "settings", "smartEscalationConfig",\n            "postPublish", "rewards", "personalFrames", "youtubeProjectLinks", "youtubeMilestones",\n        )\n        private val INCLUDED_SECTIONS_V6 = INCLUDED_SECTIONS_V5 + "youtubePublishCheckpoints"\n        private val INCLUDED_SECTIONS = INCLUDED_SECTIONS_V6 + "projectPulseHistory"\n        private val ROOT_KEYS = setOf("format", "schemaVersion", "createdAtMillis", "manifest",\n            "payloadSha256") + INCLUDED_SECTIONS\n        private fun includedSectionsFor(schema: Int): List<String> = when {\n            schema >= 7 -> INCLUDED_SECTIONS\n            schema >= 6 -> INCLUDED_SECTIONS_V6\n            else -> INCLUDED_SECTIONS_V5\n        }''',
    '''        const val SCHEMA_VERSION = 8\n        private val INCLUDED_SECTIONS_V5 = listOf(\n            "tasks", "ideas", "weeklySchedule", "settings", "smartEscalationConfig",\n            "postPublish", "rewards", "personalFrames", "youtubeProjectLinks", "youtubeMilestones",\n        )\n        private val INCLUDED_SECTIONS_V6 = INCLUDED_SECTIONS_V5 + "youtubePublishCheckpoints"\n        private val INCLUDED_SECTIONS_V7 = INCLUDED_SECTIONS_V6 + "projectPulseHistory"\n        private val INCLUDED_SECTIONS = INCLUDED_SECTIONS_V7 + "workflowStageTimeline"\n        private val ROOT_KEYS = setOf("format", "schemaVersion", "createdAtMillis", "manifest",\n            "payloadSha256") + INCLUDED_SECTIONS\n        private fun includedSectionsFor(schema: Int): List<String> = when {\n            schema >= 8 -> INCLUDED_SECTIONS\n            schema >= 7 -> INCLUDED_SECTIONS_V7\n            schema >= 6 -> INCLUDED_SECTIONS_V6\n            else -> INCLUDED_SECTIONS_V5\n        }''',
    "backup schema v8",
)

print("Materialized v101 durable workflow stage timeline")
