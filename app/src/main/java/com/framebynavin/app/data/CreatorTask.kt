package com.framebynavin.app.data

enum class TaskStatus { PLANNED, WORKING, DONE, SKIPPED }
enum class TaskPriority { NORMAL, IMPORTANT, CRITICAL }
enum class ReminderAlertType { NOTIFICATION, ALARM }

enum class ReminderMode {
    NONE,
    SIMPLE,
    VOICE,
    ALARM,
    SMART,
}

enum class VoicePersona {
    WARM,
    YOUNG,
    MAN,
    WOMAN,
}

enum class CreatorTaskOrigin {
    MANUAL,
    WEEKLY,
    RELEASE_DAY,
    IDEA_VAULT,
}

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
    val reminderMode: ReminderMode = ReminderMode.SIMPLE,
    val voicePersona: VoicePersona = VoicePersona.WARM,
    val voiceRepeatCount: Int = 3,
    val voiceRepeatIntervalSeconds: Int = 20,
    val alarmTimeoutSeconds: Int = 120,
    val dueAtMillis: Long = 0L,
    val workflowStageIndex: Int = -1,
    val scheduleSlotId: String = "",
    val scheduleOccurrenceKey: String = "",
    val autoStageReminder: Boolean = false,
    val origin: CreatorTaskOrigin = CreatorTaskOrigin.MANUAL,
    val sourceRefId: String = "",
    /** Set when FrameByNavin itself observes completion. Legacy completed projects remain 0. */
    val completedAtMillis: Long = 0L,
    /** Non-zero means the project is hidden from active Plan/Studio but retained for history. */
    val archivedAtMillis: Long = 0L,
)
