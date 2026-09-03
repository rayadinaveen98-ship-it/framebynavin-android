package com.framebynavin.app.data

enum class TaskStatus { PLANNED, WORKING, DONE, SKIPPED }

data class CreatorTask(
    val id: String,
    val title: String,
    val platform: String,
    val contentType: String,
    val dueLabel: String,
    val status: TaskStatus = TaskStatus.PLANNED,
    val progress: Int = 0,
)
