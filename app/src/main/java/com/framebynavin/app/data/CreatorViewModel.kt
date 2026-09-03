package com.framebynavin.app.data

import android.app.Application
import androidx.compose.runtime.mutableStateListOf
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.framebynavin.app.reminders.ReminderScheduler
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import java.util.UUID

class CreatorViewModel(application: Application) : AndroidViewModel(application) {
    private val store = TaskStore(application)
    private val scheduler = ReminderScheduler(application)

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
        )
        tasks.add(0, task)
        persist()
        if (task.reminderEnabled) scheduler.schedule(task)
    }

    fun setReminder(
        id: String,
        reminderAtMillis: Long,
        priority: TaskPriority,
        notes: String,
    ) = updateTask(id) { task ->
        val updated = task.copy(
            reminderEnabled = true,
            reminderAtMillis = reminderAtMillis,
            priority = priority,
            notes = notes.trim(),
        )
        scheduler.cancel(task.id)
        scheduler.schedule(updated)
        updated
    }

    fun cancelReminder(id: String) = updateTask(id) { task ->
        scheduler.cancel(task.id)
        task.copy(reminderEnabled = false, reminderAtMillis = 0L)
    }

    fun startTask(id: String) = updateTask(id) { task ->
        task.copy(status = TaskStatus.WORKING, progress = maxOf(task.progress, 15))
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
        )
        if (next >= 100) scheduler.cancel(task.id)
        updated
    }

    fun completeTask(id: String) = updateTask(id) { task ->
        scheduler.cancel(task.id)
        task.copy(status = TaskStatus.DONE, progress = 100, reminderEnabled = false)
    }

    fun skipTask(id: String) = updateTask(id) { task ->
        scheduler.cancel(task.id)
        task.copy(status = TaskStatus.SKIPPED, reminderEnabled = false)
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
