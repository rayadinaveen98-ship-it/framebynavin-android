package com.framebynavin.app.data

import android.content.Context
import com.framebynavin.app.reminders.AlarmRingingService
import com.framebynavin.app.reminders.ReminderNotifications
import com.framebynavin.app.reminders.ReminderScheduler
import com.framebynavin.app.reminders.SmartEscalationConfigStore
import com.framebynavin.app.reminders.SmartEscalationScheduler
import com.framebynavin.app.reminders.SmartSessionStore
import com.framebynavin.app.reminders.VoiceReminderService
import org.json.JSONArray
import org.json.JSONObject
import java.io.File

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
        val smartConfigJson: String,
    )

    private val appContext = context.applicationContext
    private val taskStore = TaskStore(appContext)
    private val ideaStore = IdeaVaultStore(appContext)
    private val weeklyStore = WeeklyScheduleStore(appContext)
    private val settingsStore = CreatorOsSettingsStore(appContext)
    private val smartConfigStore = SmartEscalationConfigStore(appContext)
    private val regularScheduler = ReminderScheduler(appContext)
    private val smartScheduler = SmartEscalationScheduler(appContext)
    private val smartSessions = SmartSessionStore(appContext)

    suspend fun createBackup(): String {
        val snapshot = snapshot()
        return encode(snapshot, System.currentTimeMillis())
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
        val smartRaw = root.optString("smartEscalationConfig", "{}")

        val projectCount = taskStore.validateJson(tasksRaw)
        val ideaCount = ideaStore.validateJson(ideasRaw)
        val weeklyCount = weeklyStore.validateJson(weeklyRaw)
        settingsStore.validateJson(settingsRaw)
        JSONObject(smartRaw)

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

    /**
     * Replace restore. The previous complete local state is written to cache first and also kept
     * in memory. Any failure rolls every store back before the exception escapes.
     */
    suspend fun restore(raw: String): BackupPreview {
        val preview = validate(raw)
        val before = snapshot()
        val rollbackFile = File(appContext.cacheDir, "framebynavin-pre-restore.fbnbackup")
        rollbackFile.writeText(encode(before, System.currentTimeMillis()))

        val currentTasks = taskStore.load()
        stopAndCancel(currentTasks)
        smartSessions.clearAll()

        try {
            importSnapshot(decodeSnapshot(raw))
            val restoredTasks = taskStore.load()
            scheduleFuture(restoredTasks)
            rollbackFile.delete()
            return preview
        } catch (error: Throwable) {
            runCatching {
                stopAndCancel(taskStore.load())
                smartSessions.clearAll()
                importSnapshot(before)
                scheduleFuture(taskStore.load())
            }
            throw error
        }
    }

    private suspend fun snapshot(): Snapshot = Snapshot(
        tasksJson = taskStore.exportJson(),
        ideasJson = ideaStore.exportJson(),
        weeklyJson = weeklyStore.exportJson(),
        settingsJson = settingsStore.exportJson(),
        smartConfigJson = smartConfigStore.exportJson(),
    )

    private fun encode(snapshot: Snapshot, createdAtMillis: Long): String = JSONObject()
        .put("format", FORMAT)
        .put("schemaVersion", SCHEMA_VERSION)
        .put("createdAtMillis", createdAtMillis)
        .put("tasks", snapshot.tasksJson)
        .put("ideas", snapshot.ideasJson)
        .put("weeklySchedule", snapshot.weeklyJson)
        .put("settings", snapshot.settingsJson)
        .put("smartEscalationConfig", snapshot.smartConfigJson)
        .toString()

    private fun decodeSnapshot(raw: String): Snapshot {
        val root = JSONObject(raw)
        return Snapshot(
            tasksJson = root.getString("tasks"),
            ideasJson = root.getString("ideas"),
            weeklyJson = root.getString("weeklySchedule"),
            settingsJson = root.getString("settings"),
            smartConfigJson = root.optString("smartEscalationConfig", "{}"),
        )
    }

    private suspend fun importSnapshot(snapshot: Snapshot) {
        // Each component has already been validated before this path for user-provided backups.
        taskStore.importJson(snapshot.tasksJson)
        ideaStore.importJson(snapshot.ideasJson)
        weeklyStore.importJson(snapshot.weeklyJson)
        settingsStore.importJson(snapshot.settingsJson)
        smartConfigStore.importJson(snapshot.smartConfigJson)
    }

    private fun stopAndCancel(tasks: List<CreatorTask>) {
        AlarmRingingService.stop(appContext)
        VoiceReminderService.stop(appContext)
        tasks.forEach { task ->
            regularScheduler.cancel(task.id)
            smartScheduler.cancel(task.id)
            ReminderNotifications.cancel(appContext, task.id)
        }
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
        const val SCHEMA_VERSION = 1
    }
}
