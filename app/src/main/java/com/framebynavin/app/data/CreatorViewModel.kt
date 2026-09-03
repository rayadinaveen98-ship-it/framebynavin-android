package com.framebynavin.app.data

import android.app.Application
import androidx.compose.runtime.mutableStateListOf
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.framebynavin.app.reminders.ReminderConstants
import com.framebynavin.app.reminders.ReminderScheduler
import com.framebynavin.app.reminders.SmartEscalationScheduler
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import java.util.UUID

class CreatorViewModel(application: Application) : AndroidViewModel(application) {
    private val store = TaskStore(application)
    private val scheduler = ReminderScheduler(application)
    private val smartScheduler = SmartEscalationScheduler(application)

    val tasks = mutableStateListOf<CreatorTask>()

    init {
        viewModelScope.launch {
            store.tasksFlow.collectLatest { saved ->
                if (saved.isEmpty() && tasks.isEmpty()) {
                    val starter = CreatorTask(
                        id = "starter-frame-breakdown",
                        title = "Frame Breakdown",
                        platform = "Instagram",
                        contentType = "Reel",
                        dueLabel = "Today · 7:00 PM",
                        status = TaskStatus.WORKING,
                        progress = 72,
                        reminderMode = ReminderMode.NONE,
                    )
                    tasks += starter
                    store.save(tasks.toList())
                } else {
                    tasks.clear()
                    tasks.addAll(saved)
                    reconcileSnapshot(saved)
                }
            }
        }
    }

    fun addTask(title: String, platform: String, contentType: String, dueLabel: String) {
        addTask(
            title = title,
            platform = platform,
            contentType = contentType,
            dueLabel = dueLabel,
            reminderEnabled = false,
            reminderAtMillis = 0L,
            priority = TaskPriority.IMPORTANT,
            notes = "",
            smartEscalationEnabled = false,
        )
    }

    fun addTask(
        title: String,
        platform: String,
        contentType: String,
        dueLabel: String,
        reminderEnabled: Boolean,
        reminderAtMillis: Long,
        priority: TaskPriority,
        notes: String,
        alertType: ReminderAlertType = ReminderAlertType.NOTIFICATION,
        alarmSoundUri: String = "",
        voiceEnabled: Boolean = false,
        smartEscalationEnabled: Boolean = false,
    ) {
        val mode = legacyMode(reminderEnabled, alertType, voiceEnabled, smartEscalationEnabled)
        saveTaskConfiguration(
            id = null,
            title = title,
            platform = platform,
            contentType = contentType,
            dueLabel = dueLabel,
            dueAtMillis = reminderAtMillis,
            reminderMode = mode,
            reminderAtMillis = reminderAtMillis,
            priority = priority,
            notes = notes,
            alarmSoundUri = alarmSoundUri,
            voicePersona = VoicePersona.WARM,
            voiceRepeatCount = 3,
            voiceRepeatIntervalSeconds = 20,
            alarmTimeoutSeconds = 120,
        )
    }

    fun saveTaskConfiguration(
        id: String?,
        title: String,
        platform: String,
        contentType: String,
        dueLabel: String,
        dueAtMillis: Long,
        reminderMode: ReminderMode,
        reminderAtMillis: Long,
        priority: TaskPriority,
        notes: String,
        alarmSoundUri: String,
        voicePersona: VoicePersona,
        voiceRepeatCount: Int,
        voiceRepeatIntervalSeconds: Int,
        alarmTimeoutSeconds: Int,
    ): String? {
        if (title.isBlank()) return null
        val enabled = reminderMode != ReminderMode.NONE
        val normalizedReminderAt = if (enabled) reminderAtMillis else 0L
        val internalAlertType = if (reminderMode == ReminderMode.ALARM || reminderMode == ReminderMode.SMART)
            ReminderAlertType.ALARM else ReminderAlertType.NOTIFICATION
        val internalVoice = reminderMode == ReminderMode.VOICE || reminderMode == ReminderMode.SMART
        val internalSmart = reminderMode == ReminderMode.SMART

        if (id == null) {
            val task = CreatorTask(
                id = UUID.randomUUID().toString(),
                title = title.trim(),
                platform = platform,
                contentType = contentType,
                dueLabel = dueLabel.ifBlank { "Today" },
                dueAtMillis = dueAtMillis,
                status = TaskStatus.PLANNED,
                progress = 0,
                reminderEnabled = enabled,
                reminderAtMillis = normalizedReminderAt,
                priority = priority,
                notes = notes.trim(),
                alertType = internalAlertType,
                alarmSoundUri = alarmSoundUri,
                voiceEnabled = internalVoice,
                smartEscalationEnabled = internalSmart,
                reminderMode = reminderMode,
                voicePersona = voicePersona,
                voiceRepeatCount = voiceRepeatCount.coerceIn(1, 3),
                voiceRepeatIntervalSeconds = voiceRepeatIntervalSeconds.coerceIn(10, 60),
                alarmTimeoutSeconds = alarmTimeoutSeconds.coerceIn(30, 300),
            )
            tasks.add(0, task)
            persist()
            scheduleTask(task)
            return task.id
        }

        val index = tasks.indexOfFirst { it.id == id }
        if (index == -1) return null
        cancelTaskAlerts(id)
        val current = tasks[index]
        val updated = current.copy(
            title = title.trim(),
            platform = platform,
            contentType = contentType,
            dueLabel = dueLabel.ifBlank { current.dueLabel },
            dueAtMillis = dueAtMillis,
            reminderEnabled = enabled,
            reminderAtMillis = normalizedReminderAt,
            priority = priority,
            notes = notes.trim(),
            alertType = internalAlertType,
            alarmSoundUri = alarmSoundUri,
            voiceEnabled = internalVoice,
            smartEscalationEnabled = internalSmart,
            snoozeCount = 0,
            workingUntilMillis = 0L,
            reminderMode = reminderMode,
            voicePersona = voicePersona,
            voiceRepeatCount = voiceRepeatCount.coerceIn(1, 3),
            voiceRepeatIntervalSeconds = voiceRepeatIntervalSeconds.coerceIn(10, 60),
            alarmTimeoutSeconds = alarmTimeoutSeconds.coerceIn(30, 300),
        )
        tasks[index] = updated
        persist()
        scheduleTask(updated)
        return updated.id
    }

    fun setReminder(
        id: String,
        reminderAtMillis: Long,
        priority: TaskPriority,
        notes: String,
        alertType: ReminderAlertType = ReminderAlertType.NOTIFICATION,
        alarmSoundUri: String = "",
        voiceEnabled: Boolean = false,
        smartEscalationEnabled: Boolean = false,
    ) = updateTask(id) { task ->
        val mode = legacyMode(true, alertType, voiceEnabled, smartEscalationEnabled)
        val updated = task.copy(
            reminderEnabled = true,
            reminderAtMillis = reminderAtMillis,
            priority = priority,
            notes = notes.trim(),
            alertType = if (mode == ReminderMode.ALARM || mode == ReminderMode.SMART) ReminderAlertType.ALARM else ReminderAlertType.NOTIFICATION,
            alarmSoundUri = alarmSoundUri,
            voiceEnabled = mode == ReminderMode.VOICE || mode == ReminderMode.SMART,
            smartEscalationEnabled = mode == ReminderMode.SMART,
            snoozeCount = 0,
            workingUntilMillis = 0L,
            reminderMode = mode,
        )
        scheduleTask(updated)
        updated
    }

    fun cancelReminder(id: String) = updateTask(id) { task ->
        cancelTaskAlerts(task.id)
        task.copy(
            reminderEnabled = false,
            reminderAtMillis = 0L,
            smartEscalationEnabled = false,
            voiceEnabled = false,
            snoozeCount = 0,
            workingUntilMillis = 0L,
            reminderMode = ReminderMode.NONE,
        )
    }

    fun startTask(id: String) = updateTask(id) { task ->
        val isSmart = task.reminderMode == ReminderMode.SMART || task.smartEscalationEnabled
        val updated = task.copy(
            status = TaskStatus.WORKING,
            progress = maxOf(task.progress, 15),
            workingUntilMillis = if (isSmart && task.reminderEnabled)
                System.currentTimeMillis() + ReminderConstants.WORKING_QUIET_MINUTES * 60_000L
            else task.workingUntilMillis,
        )
        scheduleTask(updated)
        updated
    }

    fun advanceTask(id: String) = updateTask(id) { task ->
        val next = when {
            task.progress < 20 -> 20
            task.progress < 40 -> 40
            task.progress < 55 -> 55
            task.progress < 70 -> 70
            task.progress < 85 -> 85
            task.progress < 95 -> 95
            else -> 100
        }
        val updated = task.copy(
            status = if (next >= 100) TaskStatus.DONE else TaskStatus.WORKING,
            progress = next,
            reminderEnabled = if (next >= 100) false else task.reminderEnabled,
            smartEscalationEnabled = if (next >= 100) false else task.smartEscalationEnabled,
            reminderMode = if (next >= 100) ReminderMode.NONE else task.reminderMode,
            workingUntilMillis = if (next >= 100) 0L else task.workingUntilMillis,
        )
        if (next >= 100) cancelTaskAlerts(task.id) else scheduleTask(updated)
        updated
    }

    fun completeTask(id: String) = updateTask(id) { task ->
        cancelTaskAlerts(task.id)
        task.copy(
            status = TaskStatus.DONE,
            progress = 100,
            reminderEnabled = false,
            smartEscalationEnabled = false,
            voiceEnabled = false,
            reminderMode = ReminderMode.NONE,
            workingUntilMillis = 0L,
        )
    }

    fun skipTask(id: String) = updateTask(id) { task ->
        cancelTaskAlerts(task.id)
        task.copy(
            status = TaskStatus.SKIPPED,
            reminderEnabled = false,
            smartEscalationEnabled = false,
            voiceEnabled = false,
            reminderMode = ReminderMode.NONE,
            workingUntilMillis = 0L,
        )
    }

    fun reconcileReminders() = reconcileSnapshot(tasks.toList())

    private fun reconcileSnapshot(snapshot: List<CreatorTask>) {
        val now = System.currentTimeMillis()
        snapshot.forEach { task ->
            val shouldBeScheduled = task.reminderEnabled &&
                task.reminderMode != ReminderMode.NONE &&
                task.reminderAtMillis > now &&
                task.status != TaskStatus.DONE &&
                task.status != TaskStatus.SKIPPED
            if (shouldBeScheduled) scheduleTask(task) else cancelTaskAlerts(task.id)
        }
    }

    private fun scheduleTask(task: CreatorTask) {
        scheduler.cancel(task.id)
        smartScheduler.cancel(task.id)
        if (!task.reminderEnabled || task.reminderMode == ReminderMode.NONE || task.reminderAtMillis <= System.currentTimeMillis()) return
        if (task.reminderMode == ReminderMode.SMART || task.smartEscalationEnabled) {
            smartScheduler.schedule(task.copy(
                smartEscalationEnabled = true,
                alertType = ReminderAlertType.ALARM,
                voiceEnabled = true,
            ))
        } else {
            scheduler.schedule(task)
        }
    }

    private fun cancelTaskAlerts(taskId: String) {
        scheduler.cancel(taskId)
        smartScheduler.cancel(taskId)
    }

    private fun legacyMode(
        enabled: Boolean,
        alertType: ReminderAlertType,
        voiceEnabled: Boolean,
        smart: Boolean,
    ): ReminderMode = when {
        !enabled -> ReminderMode.NONE
        smart -> ReminderMode.SMART
        alertType == ReminderAlertType.ALARM -> ReminderMode.ALARM
        voiceEnabled -> ReminderMode.VOICE
        else -> ReminderMode.SIMPLE
    }

    private fun updateTask(id: String, transform: (CreatorTask) -> CreatorTask) {
        val index = tasks.indexOfFirst { it.id == id }
        if (index == -1) return
        tasks[index] = transform(tasks[index])
        persist()
    }

    private fun persist() {
        val snapshot = tasks.toList()
        viewModelScope.launch { store.save(snapshot) }
    }
}
