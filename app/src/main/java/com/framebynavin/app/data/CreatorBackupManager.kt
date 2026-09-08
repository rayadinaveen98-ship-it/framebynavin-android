package com.framebynavin.app.data

import android.content.Context
import com.framebynavin.app.reminders.AlarmLedger
import com.framebynavin.app.reminders.AlarmRingingService
import com.framebynavin.app.reminders.ReminderNotifications
import com.framebynavin.app.reminders.ReminderOccurrenceStore
import com.framebynavin.app.reminders.ReminderSurfaceRegistry
import com.framebynavin.app.reminders.ReminderScheduler
import com.framebynavin.app.reminders.SmartEscalationConfigStore
import com.framebynavin.app.reminders.SmartEscalationScheduler
import com.framebynavin.app.reminders.SmartSessionStore
import com.framebynavin.app.reminders.VoiceReminderService
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import android.util.AtomicFile
import java.security.MessageDigest
import java.util.UUID

/** Versioned, offline-only backup/restore for the local Creator OS. */
class CreatorBackupManager(private val context: Context) {
    data class BackupPreview(
        val schemaVersion: Int,
        val createdAtMillis: Long,
        val projectCount: Int,
        val ideaCount: Int,
        val weeklySlotCount: Int,
        val activeReminderCount: Int,
        val settingsIncluded: Boolean,
    )

    private data class Snapshot(
        val tasksJson: String,
        val ideasJson: String,
        val weeklyJson: String,
        val settingsJson: String,
        val smartConfigJson: String? = null,
        val postPublishJson: String? = null,
        val rewardsJson: String? = null,
        val heroJson: String? = null,
        val youtubeLinksJson: String? = null,
        val youtubeMilestonesJson: String? = null,
    )

    private val appContext = context.applicationContext
    private val taskStore = TaskStore(appContext)
    private val ideaStore = IdeaVaultStore(appContext)
    private val weeklyStore = WeeklyScheduleStore(appContext)
    private val settingsStore = CreatorOsSettingsStore(appContext)
    private val smartConfigStore = SmartEscalationConfigStore(appContext)
    private val postPublishStore = CreatorPostPublishStore(appContext)
    private val rewardStore = CreatorRewardStore(appContext)
    private val regularScheduler = ReminderScheduler(appContext)
    private val smartScheduler = SmartEscalationScheduler(appContext)
    private val smartSessions = SmartSessionStore(appContext)

    suspend fun createBackup(): String = CreatorDataGate.transaction {
        recoverPendingRestoreUnlocked()
        encode(snapshot(), System.currentTimeMillis())
    }

    fun validate(raw: String): BackupPreview {
        val root = JSONObject(raw)
        require(root.optString("format") == FORMAT) { "This is not a FrameByNavin backup." }
        val schema = root.optInt("schemaVersion", -1)
        require(schema in 1..SCHEMA_VERSION) { "Unsupported backup version." }

        val tasksRaw = root.getString("tasks")
        val ideasRaw = root.getString("ideas")
        val weeklyRaw = root.getString("weeklySchedule")
        val settingsRaw = root.getString("settings")
        // An absent legacy section means preserve the current local value, not delete it.
        val smartRaw = optionalSection(root, "smartEscalationConfig")
        val postPublishRaw = optionalSection(root, "postPublish")
        val rewardsRaw = optionalSection(root, "rewards")
        if (schema >= 5) validateManifest(root)
        if (schema >= 3) {
            require(root.has("payloadSha256")) { "Backup integrity information is missing" }
            require(sha256(fingerprint(root, schema)) == root.getString("payloadSha256")) { "Backup integrity check failed" }
        }
        optionalSection(root, "personalFrames")?.let(CreatorHeroArchive::validate)
        if (schema >= 4) {
            require(root.has("youtubeProjectLinks") && root.has("youtubeMilestones")) {
                "Backup is missing YouTube project metadata"
            }
        }
        optionalSection(root, "youtubeProjectLinks")?.let { JSONObject(it) }
        optionalSection(root, "youtubeMilestones")?.let { JSONObject(it) }

        val projectCount = taskStore.validateJson(tasksRaw)
        val ideaCount = ideaStore.validateJson(ideasRaw)
        val weeklyCount = weeklyStore.validateJson(weeklyRaw)
        settingsStore.validateJson(settingsRaw)
        smartRaw?.let { JSONObject(it) }
        postPublishRaw?.let(postPublishStore::validateJson)
        rewardsRaw?.let(rewardStore::validateJson)

        val taskArray = JSONArray(tasksRaw)
        var activeReminders = 0
        for (i in 0 until taskArray.length()) {
            val item = taskArray.getJSONObject(i)
            if (item.optBoolean("reminderEnabled", false) &&
                item.optString("reminderMode", ReminderMode.NONE.name) != ReminderMode.NONE.name &&
                item.optString("status", TaskStatus.PLANNED.name) !in setOf(TaskStatus.DONE.name, TaskStatus.SKIPPED.name)
            ) {
                activeReminders++
            }
        }

        return BackupPreview(
            schemaVersion = schema,
            createdAtMillis = root.optLong("createdAtMillis", 0L),
            projectCount = projectCount,
            ideaCount = ideaCount,
            weeklySlotCount = weeklyCount,
            activeReminderCount = activeReminders,
            settingsIncluded = true,
        )
    }

    /** Restore is serialized with local writes. A durable pre-restore journal survives process death. */
    suspend fun restore(raw: String): BackupPreview = CreatorDataGate.transaction {
        val preview = validate(raw)
        recoverPendingRestoreUnlocked()
        val before = encode(snapshot(), System.currentTimeMillis())
        val recovery = retainRecovery(before)
        val marker = JSONObject().put("file", recovery.name).put("sha256", sha256(before))
        atomicWrite(pendingFile(), marker.toString())
        CreatorDataGate.invalidate(appContext)
        try {
            stopAndCancel(taskStore.load())
            smartSessions.clearAll()
            importSnapshot(decodeSnapshot(raw))
            scheduleFuture(taskStore.load())
            clearPending()
            preview
        } catch (error: Throwable) {
            CreatorDataGate.nonCancellable {
                try {
                    stopAndCancel(taskStore.load())
                    smartSessions.clearAll()
                    importSnapshot(decodeSnapshot(before))
                    scheduleFuture(taskStore.load())
                    clearPending()
                } catch (rollbackError: Throwable) {
                    error.addSuppressed(rollbackError)
                    // Do not discard the journal when recovery is incomplete.
                }
            }
            throw error
        }
    }

    /** Called before normal app startup and by maintenance workers. Never silently discard a journal. */
    suspend fun recoverPendingRestore(): Boolean = CreatorDataGate.transaction {
        recoverPendingRestoreUnlocked()
    }

    private suspend fun recoverPendingRestoreUnlocked(): Boolean {
        val pending = pendingFile()
        if (!pending.isFile) return false
        val marker = JSONObject(AtomicFile(pending).openRead().bufferedReader().use { it.readText() })
        val name = marker.getString("file")
        require(name.matches(Regex("pre-restore-[a-zA-Z0-9_-]+\\.fbnbackup"))) { "Invalid recovery filename" }
        val recovery = File(recoveryDir(), name)
        val raw = AtomicFile(recovery).openRead().bufferedReader().use { it.readText() }
        require(sha256(raw) == marker.getString("sha256")) { "Recovery integrity check failed" }
        validate(raw)
        CreatorDataGate.invalidate(appContext)
        stopAndCancel(taskStore.load())
        smartSessions.clearAll()
        importSnapshot(decodeSnapshot(raw))
        scheduleFuture(taskStore.load())
        clearPending()
        return true
    }

    private fun recoveryDir(): File = File(appContext.filesDir, "creator_restore_recovery_v181").apply { mkdirs() }
    private fun pendingFile() = File(recoveryDir(), "pending.json")
    private fun retainRecovery(raw: String): File {
        val file = File(recoveryDir(), "pre-restore-${UUID.randomUUID()}.fbnbackup")
        atomicWrite(file, raw)
        return file
    }
    private fun clearPending() { AtomicFile(pendingFile()).delete() }
    private fun atomicWrite(file: File, raw: String) {
        val atomic = AtomicFile(file)
        var output: java.io.FileOutputStream? = null
        try {
            output = atomic.startWrite()
            output.write(raw.toByteArray(Charsets.UTF_8))
            atomic.finishWrite(output)
        } catch (error: Throwable) {
            output?.let(atomic::failWrite)
            throw error
        }
    }

    fun recoveryCopies(): List<File> = recoveryDir().listFiles().orEmpty()
        .filter { it.name.startsWith("pre-restore-") && it.name.endsWith(".fbnbackup") }
        .sortedByDescending { it.lastModified() }

    private suspend fun snapshot(): Snapshot = Snapshot(
        tasksJson = taskStore.exportJson(),
        ideasJson = ideaStore.exportJson(),
        weeklyJson = weeklyStore.exportJson(),
        settingsJson = settingsStore.exportJson(),
        smartConfigJson = smartConfigStore.exportJson(),
        postPublishJson = postPublishStore.exportJson(),
        rewardsJson = rewardStore.exportJson(),
        heroJson = CreatorHeroArchive.export(appContext),
        youtubeLinksJson = youtubeLinksRaw(),
        youtubeMilestonesJson = youtubeMilestonesRaw(),
    )

    private fun encode(snapshot: Snapshot, createdAtMillis: Long): String {
        val root = JSONObject()
            .put("format", FORMAT)
            .put("schemaVersion", SCHEMA_VERSION)
            .put("createdAtMillis", createdAtMillis)
            .put("tasks", snapshot.tasksJson)
            .put("ideas", snapshot.ideasJson)
            .put("weeklySchedule", snapshot.weeklyJson)
            .put("settings", snapshot.settingsJson)
            .put("smartEscalationConfig", requireNotNull(snapshot.smartConfigJson))
            .put("postPublish", requireNotNull(snapshot.postPublishJson))
            .put("rewards", requireNotNull(snapshot.rewardsJson))
            .put("personalFrames", requireNotNull(snapshot.heroJson))
            .put("youtubeProjectLinks", requireNotNull(snapshot.youtubeLinksJson))
            .put("youtubeMilestones", requireNotNull(snapshot.youtubeMilestonesJson))
            .put("manifest", backupManifest())
        return root.put("payloadSha256", sha256(fingerprint(root, SCHEMA_VERSION))).toString()
    }

    /** Schema 1–4 fingerprint order is deliberately unchanged for existing backups. */
    private fun fingerprint(root: JSONObject, schema: Int): String = buildString {
        val keys = listOf("format", "schemaVersion", "createdAtMillis", "tasks", "ideas", "weeklySchedule",
            "settings", "smartEscalationConfig", "postPublish", "rewards", "personalFrames")
        val allKeys = if (schema >= 4) keys + listOf("youtubeProjectLinks", "youtubeMilestones") else keys
        (if (schema >= 5) allKeys + "manifest" else allKeys).forEach { key ->
            val value = root.optString(key, "")
            append(key.length).append(':').append(key).append(value.length).append(':').append(value)
        }
    }

    private fun optionalSection(root: JSONObject, key: String): String? =
        if (root.has(key) && !root.isNull(key)) root.getString(key) else null

    private fun backupManifest(): JSONObject = JSONObject()
        .put("manifestVersion", 1)
        .put("included", JSONArray(INCLUDED_SECTIONS))
        .put("excluded", JSONArray(EXCLUDED_SECTIONS))
        .put("checksum", "SHA-256")
        .put("authenticated", false)

    private fun validateManifest(root: JSONObject) {
        require(INCLUDED_SECTIONS.all { root.has(it) && !root.isNull(it) }) {
            "Backup is missing a required creator-data section"
        }
        require(root.keys().asSequence().toSet() == ROOT_KEYS) {
            "Backup contains unexpected or missing schema sections"
        }
        val manifest = root.getJSONObject("manifest")
        require(manifest.optInt("manifestVersion") == 1) { "Unsupported backup manifest" }
        val included = manifest.getJSONArray("included")
        require((0 until included.length()).map(included::getString) == INCLUDED_SECTIONS) {
            "Backup inclusion manifest does not match its schema"
        }
        val excluded = manifest.getJSONArray("excluded")
        require((0 until excluded.length()).map(excluded::getString) == EXCLUDED_SECTIONS) {
            "Backup exclusion manifest does not match its schema"
        }
        require(manifest.optString("checksum") == "SHA-256" && !manifest.optBoolean("authenticated", true)) {
            "Unsupported backup integrity declaration"
        }
    }

    private fun sha256(raw: String): String = MessageDigest.getInstance("SHA-256")
        .digest(raw.toByteArray(Charsets.UTF_8)).joinToString("") { "%02x".format(it) }

    private fun decodeSnapshot(raw: String): Snapshot {
        val root = JSONObject(raw)
        return Snapshot(
            tasksJson = root.getString("tasks"),
            ideasJson = root.getString("ideas"),
            weeklyJson = root.getString("weeklySchedule"),
            settingsJson = root.getString("settings"),
            smartConfigJson = optionalSection(root, "smartEscalationConfig"),
            postPublishJson = optionalSection(root, "postPublish"),
            rewardsJson = optionalSection(root, "rewards"),
            heroJson = optionalSection(root, "personalFrames"),
            youtubeLinksJson = optionalSection(root, "youtubeProjectLinks"),
            youtubeMilestonesJson = optionalSection(root, "youtubeMilestones"),
        )
    }

    private suspend fun importSnapshot(snapshot: Snapshot) {
        // Each component has already been validated before this path for user-provided backups.
        taskStore.importJson(snapshot.tasksJson)
        ideaStore.importJson(snapshot.ideasJson)
        weeklyStore.importJson(snapshot.weeklyJson)
        settingsStore.importJson(snapshot.settingsJson)
        snapshot.smartConfigJson?.let { smartConfigStore.importJson(it) }
        snapshot.postPublishJson?.let { postPublishStore.importJson(it) }
        snapshot.rewardsJson?.let { rewardStore.importJson(it) }
        snapshot.heroJson?.let { CreatorHeroArchive.import(appContext, it) }
        snapshot.youtubeLinksJson?.let { importYoutubeLinks(it) }
        snapshot.youtubeMilestonesJson?.let { importYoutubeMilestones(it) }
    }

    /** Upgrade an already validated legacy snapshot while preserving its original checksum check. */
    fun attachLegacyYoutubeData(raw: String, links: String, milestones: String): String {
        validate(raw)
        JSONObject(links)
        JSONObject(milestones)
        val root = JSONObject(raw)
        if (root.optInt("schemaVersion") >= 4) return raw
        // Keep the original schema: adding external metadata cannot turn a partial
        // historical snapshot into a complete current-schema backup.
        val schema = root.getInt("schemaVersion")
        if (!root.has("youtubeProjectLinks")) root.put("youtubeProjectLinks", links)
        if (!root.has("youtubeMilestones")) root.put("youtubeMilestones", milestones)
        if (schema >= 3) root.put("payloadSha256", sha256(fingerprint(root, schema)))
        return root.toString().also(::validate)
    }

    private fun youtubeLinksRaw(): String = appContext
        .getSharedPreferences("youtube_analytics_v11", Context.MODE_PRIVATE)
        .getString("video_project_links", "{}").orEmpty()

    private fun importYoutubeLinks(raw: String) {
        JSONObject(raw)
        check(appContext.getSharedPreferences("youtube_analytics_v11", Context.MODE_PRIVATE)
            .edit().putString("video_project_links", raw).commit()) {
            "Could not restore YouTube project links"
        }
    }

    private fun youtubeMilestonesRaw(): String = JSONObject().apply {
        appContext.getSharedPreferences("youtube_milestones_v12", Context.MODE_PRIVATE)
            .all.forEach { (key, value) -> if (value is String) put(key, value) }
    }.toString()

    private fun importYoutubeMilestones(raw: String) {
        val obj = JSONObject(raw)
        val editor = appContext.getSharedPreferences("youtube_milestones_v12", Context.MODE_PRIVATE)
            .edit().clear()
        obj.keys().forEach { key -> editor.putString(key, obj.getString(key)) }
        check(editor.commit()) { "Could not restore YouTube milestones" }
    }

    private fun stopAndCancel(tasks: List<CreatorTask>) {
        // Restore is the only global reset: orphaned occurrences may belong to
        // tasks absent from either snapshot. No pre-restore button may act afterward.
        val occurrences = ReminderOccurrenceStore(appContext)
        val ledger = AlarmLedger(appContext)
        val taskIds = tasks.map { it.id }.toSet() + occurrences.taskIds() + ledger.trackedTaskIds()
        occurrences.invalidateAll()
        ReminderSurfaceRegistry.closeAll()
        AlarmRingingService.stop(appContext)
        VoiceReminderService.stop(appContext)
        taskIds.forEach { taskId ->
            regularScheduler.cancel(taskId)
            smartScheduler.cancel(taskId)
            ReminderNotifications.cancel(appContext, taskId)
        }
        ledger.clearAll()
    }

    private fun scheduleFuture(tasks: List<CreatorTask>) {
        val now = System.currentTimeMillis()
        tasks.forEach { task ->
            val active = task.reminderEnabled &&
                task.reminderMode != ReminderMode.NONE &&
                task.reminderAtMillis > now &&
                task.status != TaskStatus.DONE &&
                task.status != TaskStatus.SKIPPED
            if (!active) return@forEach
            if (task.reminderMode == ReminderMode.SMART || task.smartEscalationEnabled) smartScheduler.schedule(task)
            else regularScheduler.schedule(task)
        }
    }

    companion object {
        const val FORMAT = "FrameByNavinBackup"
        const val SCHEMA_VERSION = 5
        private val INCLUDED_SECTIONS = listOf(
            "tasks", "ideas", "weeklySchedule", "settings", "smartEscalationConfig",
            "postPublish", "rewards", "personalFrames", "youtubeProjectLinks", "youtubeMilestones",
        )
        private val ROOT_KEYS = setOf("format", "schemaVersion", "createdAtMillis", "manifest",
            "payloadSha256") + INCLUDED_SECTIONS
        private val EXCLUDED_SECTIONS = listOf(
            "accountCredentials", "oauthTokens", "cloudSessions", "youtubeAnalyticsCache",
            "reminderOccurrenceTokens", "alarmDeliveryLedger", "activeSmartSessions", "activeMediaServices",
        )
    }
}
