package com.framebynavin.app.data

enum class TaskStatus { PLANNED, WORKING, DONE, SKIPPED }
enum class TaskPriority { NORMAL, IMPORTANT, CRITICAL }
enum class ReminderAlertType { NOTIFICATION, ALARM }

data class CreatorTask(
    val id: String,
    val title: String,
    val platform: String,
    val contentType: String,
    val dueLabel: String,
    val status: TaskStatus = TaskStatus.PLANNED,
    val progress: Int = 0,
    val reminderEnabled: Boolean = false,
    val reminderAtMillis: Long = 0L,
    val priority: TaskPriority = TaskPriority.IMPORTANT,
    val notes: String = "",
    val alertType: ReminderAlertType = ReminderAlertType.NOTIFICATION,
    val alarmSoundUri: String = "",
    val voiceEnabled: Boolean = false,
    val smartEscalationEnabled: Boolean = false,
    val snoozeCount: Int = 0,
    val workingUntilMillis: Long = 0L,
)
