package com.framebynavin.app.data

import android.app.Application
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.launch
import java.util.UUID

class CreatorViewModel(application: Application) : AndroidViewModel(application) {
    private val store = TaskStore(application)

    val tasks = mutableStateListOf<CreatorTask>()
    var isLoaded by mutableStateOf(false)
        private set

    init {
        viewModelScope.launch {
            val saved = store.load()
            if (saved.isEmpty()) {
                tasks += CreatorTask(
                    id = "starter-frame-breakdown",
                    title = "Frame Breakdown",
                    platform = "Instagram",
                    contentType = "Reel",
                    dueLabel = "Today · 7:00 PM",
                    status = TaskStatus.WORKING,
                    progress = 72,
                )
                persist()
            } else {
                tasks += saved
            }
            isLoaded = true
        }
    }

    fun addTask(title: String, platform: String, contentType: String, dueLabel: String) {
        if (title.isBlank()) return
        tasks.add(
            0,
            CreatorTask(
                id = UUID.randomUUID().toString(),
                title = title.trim(),
                platform = platform,
                contentType = contentType,
                dueLabel = dueLabel.ifBlank { "Today" },
                status = TaskStatus.PLANNED,
                progress = 0,
            )
        )
        persist()
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
        task.copy(
            status = if (next >= 100) TaskStatus.DONE else TaskStatus.WORKING,
            progress = next,
        )
    }

    fun completeTask(id: String) = updateTask(id) { task ->
        task.copy(status = TaskStatus.DONE, progress = 100)
    }

    fun skipTask(id: String) = updateTask(id) { task ->
        task.copy(status = TaskStatus.SKIPPED)
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
