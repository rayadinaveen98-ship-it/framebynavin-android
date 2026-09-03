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
        smartEscalationEnabled: Boolean = true,
    ) {
        if (title.isBlank()) return
        val task = CreatorTask(
            id = UUID.randomUUID().toString(),
            title = title.trim(),
            platform = platform,
            contentType = contentType,
            dueLabel = dueLabel.ifBlank { "Today" },
            status = TaskStatus.PLANNED,
            progress = 0,
            reminderEnabled = reminderEnabled,
            reminderAtMillis = if (reminderEnabled) reminderAtMillis else 0L,
            priority = priority,
            notes = notes.trim(),
            alertType = alertType,
            alarmSoundUri = alarmSoundUri,
            voiceEnabled = voiceEnabled,
            smartEscalationEnabled = reminderEnabled && smartEscalationEnabled,
        )
        tasks.add(0, task)
        persist()
        scheduleTask(task)
    }

    fun setReminder(
        id: String,
        reminderAtMillis: Long,
        priority: TaskPriority,
        notes: String,
        alertType: ReminderAlertType = ReminderAlertType.NOTIFICATION,
        alarmSoundUri: String = "",
        voiceEnabled: Boolean = false,
        smartEscalationEnabled: Boolean = true,
    ) = updateTask(id) { task ->
        val updated = task.copy(
            reminderEnabled = true,
            reminderAtMillis = reminderAtMillis,
            priority = priority,
            notes = notes.trim(),
            alertType = alertType,
            alarmSoundUri = alarmSoundUri,
            voiceEnabled = voiceEnabled,
            smartEscalationEnabled = smartEscalationEnabled,
            snoozeCount = 0,
            workingUntilMillis = 0L,
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
            snoozeCount = 0,
            workingUntilMillis = 0L,
        )
    }

    fun startTask(id: String) = updateTask(id) { task ->
        val updated = task.copy(
            status = TaskStatus.WORKING,
            progress = maxOf(task.progress, 15),
            workingUntilMillis = if (task.smartEscalationEnabled && task.reminderEnabled)
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
            workingUntilMillis = 0L,
        )
    }

    fun skipTask(id: String) = updateTask(id) { task ->
        cancelTaskAlerts(task.id)
        task.copy(
            status = TaskStatus.SKIPPED,
            reminderEnabled = false,
            smartEscalationEnabled = false,
            workingUntilMillis = 0L,
        )
    }

    fun reconcileReminders() = reconcileSnapshot(tasks.toList())

    private fun reconcileSnapshot(snapshot: List<CreatorTask>) {
        val now = System.currentTimeMillis()
        snapshot.forEach { task ->
            val shouldBeScheduled = task.reminderEnabled &&
                task.reminderAtMillis > now &&
                task.status != TaskStatus.DONE &&
                task.status != TaskStatus.SKIPPED
            if (shouldBeScheduled) scheduleTask(task) else cancelTaskAlerts(task.id)
        }
    }

    private fun scheduleTask(task: CreatorTask) {
        scheduler.cancel(task.id)
        smartScheduler.cancel(task.id)
        if (!task.reminderEnabled || task.reminderAtMillis <= System.currentTimeMillis()) return
        if (task.smartEscalationEnabled) smartScheduler.schedule(task) else scheduler.schedule(task)
    }

    private fun cancelTaskAlerts(taskId: String) {
        scheduler.cancel(taskId)
        smartScheduler.cancel(taskId)
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
