package com.framebynavin.app.data

import android.app.Application
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.framebynavin.app.reminders.ReminderConstants
import com.framebynavin.app.reminders.ReminderScheduler
import com.framebynavin.app.reminders.SmartEscalationConfigStore
import com.framebynavin.app.reminders.SmartEscalationPolicy
import com.framebynavin.app.reminders.SmartEscalationScheduler
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import java.util.UUID

class CreatorViewModel(application: Application) : AndroidViewModel(application) {
    private val store = TaskStore(application)
    private val scheduler = ReminderScheduler(application)
    private val smartScheduler = SmartEscalationScheduler(application)
    private val smartConfigStore = SmartEscalationConfigStore(application)
    private val weeklyStore = WeeklyScheduleStore(application)
    private val ideaStore = IdeaVaultStore(application)
    private val settingsStore = CreatorOsSettingsStore(application)

    val tasks = mutableStateListOf<CreatorTask>()
    val weeklySlots = mutableStateListOf<WeeklyScheduleSlot>()
    val ideas = mutableStateListOf<CreatorIdea>()

    private var weeklyAutoPlanState by mutableStateOf(settingsStore.snapshot().weeklyAutoPlanEnabled)
    val weeklyAutoPlanEnabled: Boolean get() = weeklyAutoPlanState

    private var tasksLoaded = false
    private var weeklyLoaded = false

    init {
        viewModelScope.launch {
            val slots = weeklyStore.loadOrSeed()
            weeklySlots.clear()
            weeklySlots.addAll(slots)
            weeklyLoaded = true
            if (tasksLoaded) {
                if (weeklyAutoPlanEnabled) syncWeeklyScheduleInternal() else removeUnstartedWeeklyProjects()
            }
        }

        viewModelScope.launch {
            ideaStore.ideasFlow.collectLatest { saved ->
                ideas.clear()
                ideas.addAll(saved.sortedByDescending { it.updatedAtMillis })
            }
        }

        viewModelScope.launch {
            store.tasksFlow.collectLatest { saved ->
                val cleaned = saved.filterNot { it.id == "starter-frame-breakdown" }
                tasks.clear()
                tasks.addAll(cleaned)
                reconcileSnapshot(cleaned)
                if (cleaned.size != saved.size) store.save(cleaned)
                tasksLoaded = true
                if (weeklyLoaded) {
                    if (weeklyAutoPlanEnabled) syncWeeklyScheduleInternal() else removeUnstartedWeeklyProjects()
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
        val defaults = settingsStore.snapshot()
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
            voicePersona = defaults.defaultVoicePersona,
            voiceRepeatCount = 3,
            voiceRepeatIntervalSeconds = 10,
            alarmTimeoutSeconds = defaults.defaultAlarmTimeoutSeconds,
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
                workflowStageIndex = 0,
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
                voiceRepeatIntervalSeconds = voiceRepeatIntervalSeconds.coerceIn(5, 60),
                alarmTimeoutSeconds = alarmTimeoutSeconds.coerceIn(30, 300),
                autoStageReminder = false,
                origin = CreatorTaskOrigin.MANUAL,
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
        val formatChanged = current.platform != platform || current.contentType != contentType
        val newTemplate = CreatorWorkflowEngine.templateFor(platform, contentType)
        val nextStageIndex = if (formatChanged) {
            CreatorWorkflowEngine.stageIndexFromProgress(current.progress, newTemplate.stages.size)
        } else {
            CreatorWorkflowEngine.stageIndex(current).coerceIn(0, newTemplate.stages.lastIndex)
        }
        val nextProgress = if (current.status == TaskStatus.DONE) 100
        else CreatorWorkflowEngine.progressForStage(nextStageIndex, newTemplate.stages.size)

        val updated = current.copy(
            title = title.trim(),
            platform = platform,
            contentType = contentType,
            dueLabel = dueLabel.ifBlank { current.dueLabel },
            dueAtMillis = dueAtMillis,
            progress = nextProgress,
            workflowStageIndex = nextStageIndex,
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
            voiceRepeatIntervalSeconds = voiceRepeatIntervalSeconds.coerceIn(5, 60),
            alarmTimeoutSeconds = alarmTimeoutSeconds.coerceIn(30, 300),
            autoStageReminder = false,
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
            autoStageReminder = false,
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
            autoStageReminder = false,
        )
    }

    fun startTask(id: String) = updateTask(id) { task ->
        val isSmart = task.reminderMode == ReminderMode.SMART || task.smartEscalationEnabled
        val template = CreatorWorkflowEngine.templateFor(task)
        val stageIndex = CreatorWorkflowEngine.stageIndex(task)
        val updated = task.copy(
            status = TaskStatus.WORKING,
            workflowStageIndex = stageIndex,
            progress = CreatorWorkflowEngine.progressForStage(stageIndex, template.stages.size),
            workingUntilMillis = if (isSmart && task.reminderEnabled)
                System.currentTimeMillis() + ReminderConstants.WORKING_QUIET_MINUTES * 60_000L
            else task.workingUntilMillis,
        )
        scheduleTask(updated)
        updated
    }

    fun advanceTask(id: String) = advanceWorkflow(id)

    fun advanceWorkflow(id: String) = updateTask(id) { task ->
        val template = CreatorWorkflowEngine.templateFor(task)
        val currentIndex = CreatorWorkflowEngine.stageIndex(task)
        if (currentIndex >= template.stages.lastIndex) {
            cancelTaskAlerts(task.id)
            return@updateTask task.copy(
                status = TaskStatus.DONE,
                progress = 100,
                workflowStageIndex = template.stages.lastIndex,
                reminderEnabled = false,
                smartEscalationEnabled = false,
                voiceEnabled = false,
                reminderMode = ReminderMode.NONE,
                workingUntilMillis = 0L,
                autoStageReminder = false,
            )
        }

        val nextIndex = currentIndex + 1
        var updated = task.copy(
            status = TaskStatus.WORKING,
            workflowStageIndex = nextIndex,
            progress = CreatorWorkflowEngine.progressForStage(nextIndex, template.stages.size),
        )
        updated = applyAutoStageReminder(updated, nextIndex)
        scheduleTask(updated)
        updated
    }

    fun moveWorkflowBack(id: String) = updateTask(id) { task ->
        if (task.status == TaskStatus.DONE) return@updateTask task
        val template = CreatorWorkflowEngine.templateFor(task)
        val currentIndex = CreatorWorkflowEngine.stageIndex(task)
        val previous = (currentIndex - 1).coerceAtLeast(0)
        var updated = task.copy(
            status = TaskStatus.WORKING,
            workflowStageIndex = previous,
            progress = CreatorWorkflowEngine.progressForStage(previous, template.stages.size),
        )
        updated = applyAutoStageReminder(updated, previous)
        scheduleTask(updated)
        updated
    }

    fun completeTask(id: String) = updateTask(id) { task ->
        cancelTaskAlerts(task.id)
        val template = CreatorWorkflowEngine.templateFor(task)
        task.copy(
            status = TaskStatus.DONE,
            progress = 100,
            workflowStageIndex = template.stages.lastIndex,
            reminderEnabled = false,
            smartEscalationEnabled = false,
            voiceEnabled = false,
            reminderMode = ReminderMode.NONE,
            workingUntilMillis = 0L,
            autoStageReminder = false,
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
            autoStageReminder = false,
        )
    }

    fun publishLate(id: String, delayMinutes: Int = 30) = updateTask(id) { task ->
        val now = System.currentTimeMillis()
        val due = now + delayMinutes.coerceIn(10, 180) * 60_000L
        val updated = task.copy(
            dueAtMillis = due,
            dueLabel = WeeklyScheduleEngine.dueLabel(due),
            reminderAtMillis = if (task.reminderEnabled && task.reminderMode != ReminderMode.NONE) due else 0L,
            snoozeCount = 0,
            workingUntilMillis = 0L,
            autoStageReminder = false,
        )
        scheduleTask(updated)
        updated
    }

    fun rescheduleDeadline(id: String, atMillis: Long) = updateTask(id) { task ->
        if (atMillis <= System.currentTimeMillis()) return@updateTask task
        val updated = task.copy(
            dueAtMillis = atMillis,
            dueLabel = WeeklyScheduleEngine.dueLabel(atMillis),
            reminderAtMillis = if (task.reminderEnabled && task.reminderMode != ReminderMode.NONE) atMillis else 0L,
            snoozeCount = 0,
            workingUntilMillis = 0L,
            autoStageReminder = false,
        )
        scheduleTask(updated)
        updated
    }

    /** Remove alert configuration only. Project/task data remains intact. */
    fun cancelReminders(ids: Set<String>) {
        if (ids.isEmpty()) return
        ids.forEach(::cancelTaskAlerts)
        var changed = false
        tasks.indices.forEach { index ->
            val task = tasks[index]
            if (task.id in ids && task.reminderEnabled) {
                tasks[index] = task.copy(
                    reminderEnabled = false,
                    reminderAtMillis = 0L,
                    smartEscalationEnabled = false,
                    voiceEnabled = false,
                    snoozeCount = 0,
                    workingUntilMillis = 0L,
                    reminderMode = ReminderMode.NONE,
                    autoStageReminder = false,
                )
                changed = true
            }
        }
        if (changed) persist()
    }

    fun archiveTask(id: String) = updateTask(id) { task ->
        if (task.status != TaskStatus.DONE) task
        else task.copy(archivedAtMillis = System.currentTimeMillis())
    }

    fun unarchiveTask(id: String) = updateTask(id) { task ->
        task.copy(archivedAtMillis = 0L)
    }

    /**
     * Manual/release/idea projects are hard-deleted. A weekly occurrence is retained as a hidden
     * tombstone (SKIPPED + archivedAtMillis=-1) so the weekly engine cannot immediately recreate
     * the same occurrence after the creator deliberately deletes it.
     */
    fun deleteTasks(ids: Set<String>) {
        if (ids.isEmpty()) return
        ids.forEach(::cancelTaskAlerts)
        val now = System.currentTimeMillis()
        val hardDelete = mutableSetOf<String>()
        tasks.indices.forEach { index ->
            val task = tasks[index]
            if (task.id !in ids) return@forEach
            if (task.origin == CreatorTaskOrigin.WEEKLY && task.scheduleOccurrenceKey.isNotBlank()) {
                tasks[index] = task.copy(
                    status = TaskStatus.SKIPPED,
                    reminderEnabled = false,
                    reminderAtMillis = 0L,
                    smartEscalationEnabled = false,
                    voiceEnabled = false,
                    reminderMode = ReminderMode.NONE,
                    workingUntilMillis = 0L,
                    autoStageReminder = false,
                    archivedAtMillis = -1L,
                )
            } else {
                hardDelete += task.id
            }
        }
        if (hardDelete.isNotEmpty()) tasks.removeAll { it.id in hardDelete }
        var ideasChanged = false
        ideas.indices.forEach { index ->
            if (ideas[index].projectTaskId in ids) {
                ideas[index] = ideas[index].copy(projectTaskId = "", updatedAtMillis = now)
                ideasChanged = true
            }
        }
        persist()
        if (ideasChanged) persistIdeas()
    }

    fun deleteTask(id: String) = deleteTasks(setOf(id))

    fun saveIdea(idea: CreatorIdea): String? {
        if (idea.title.isBlank()) return null
        val now = System.currentTimeMillis()
        val normalized = idea.copy(
            id = idea.id.ifBlank { UUID.randomUUID().toString() },
            title = idea.title.trim(),
            topic = idea.topic.trim(),
            notes = idea.notes.trim(),
            updatedAtMillis = now,
            createdAtMillis = idea.createdAtMillis.takeIf { it > 0L } ?: now,
        )
        val index = ideas.indexOfFirst { it.id == normalized.id }
        if (index >= 0) ideas[index] = normalized else ideas.add(0, normalized)
        persistIdeas()
        return normalized.id
    }

    fun deleteIdea(id: String) {
        ideas.removeAll { it.id == id }
        persistIdeas()
    }

    fun archiveIdea(id: String) {
        val index = ideas.indexOfFirst { it.id == id }
        if (index == -1) return
        ideas[index] = ideas[index].copy(status = IdeaStatus.ARCHIVED, updatedAtMillis = System.currentTimeMillis())
        persistIdeas()
    }

    fun convertIdeaToProject(id: String, platform: String, contentType: String, dueAtMillis: Long): String? {
        val ideaIndex = ideas.indexOfFirst { it.id == id }
        if (ideaIndex == -1) return null
        val idea = ideas[ideaIndex]
        if (idea.projectTaskId.isNotBlank()) {
            tasks.firstOrNull { it.id == idea.projectTaskId }?.let { return it.id }
        }

        val now = System.currentTimeMillis()
        val due = dueAtMillis.coerceAtLeast(now + 5 * 60_000L)
        val template = CreatorWorkflowEngine.templateFor(platform, contentType)
        val wantsSmart = platform == "YouTube" || (platform == "Instagram" && contentType == "Reel")
        val smartRequired = SmartEscalationPolicy.requiredWindowMinutes(TaskPriority.IMPORTANT, SmartEscalationConfigStore.DEFAULT)
        val canFitSmart = due - now > (smartRequired + 1) * 60_000L
        val mode = if (wantsSmart && canFitSmart) ReminderMode.SMART else ReminderMode.SIMPLE
        val reminderAt = if (mode == ReminderMode.SMART) due - smartRequired * 60_000L else due
        val defaults = settingsStore.snapshot()
        val taskId = UUID.randomUUID().toString()
        val task = CreatorTask(
            id = taskId,
            title = idea.title,
            platform = platform,
            contentType = contentType,
            dueLabel = WeeklyScheduleEngine.dueLabel(due),
            dueAtMillis = due,
            status = TaskStatus.PLANNED,
            progress = 0,
            workflowStageIndex = 0.coerceAtMost(template.stages.lastIndex),
            reminderEnabled = true,
            reminderAtMillis = reminderAt,
            priority = TaskPriority.IMPORTANT,
            notes = buildString {
                append("From Idea Vault")
                if (idea.topic.isNotBlank()) append(" · ${idea.topic}")
                if (idea.notes.isNotBlank()) append("\n${idea.notes}")
            },
            alertType = if (mode == ReminderMode.SMART) ReminderAlertType.ALARM else ReminderAlertType.NOTIFICATION,
            voiceEnabled = mode == ReminderMode.SMART,
            smartEscalationEnabled = mode == ReminderMode.SMART,
            reminderMode = mode,
            voicePersona = defaults.defaultVoicePersona,
            voiceRepeatIntervalSeconds = 10,
            alarmTimeoutSeconds = defaults.defaultAlarmTimeoutSeconds,
            origin = CreatorTaskOrigin.IDEA_VAULT,
            sourceRefId = idea.id,
        )
        if (mode == ReminderMode.SMART) smartConfigStore.put(task, SmartEscalationConfigStore.DEFAULT)
        tasks.add(0, task)
        ideas[ideaIndex] = idea.copy(
            status = IdeaStatus.CONVERTED,
            projectTaskId = taskId,
            platformHint = platform,
            formatHint = contentType,
            updatedAtMillis = now,
        )
        persist()
        persistIdeas()
        scheduleTask(task)
        return taskId
    }

    fun createReleaseBurst(request: ReleaseBurstRequest): ReleaseLaunchResult {
        if (request.topic.isBlank()) return ReleaseLaunchResult(0, false)
        val batchId = "release-${UUID.randomUUID()}"
        val now = System.currentTimeMillis()
        val created = mutableListOf<CreatorTask>()
        val defaults = settingsStore.snapshot()

        ReleaseDayEngine.specs(request).forEach { spec ->
            val due = now + spec.dueOffsetMinutes * 60_000L
            val template = CreatorWorkflowEngine.templateFor(spec.platform, spec.contentType)
            val stageIndex = spec.startStageIndex.coerceIn(0, template.stages.lastIndex)
            val requestedMode = spec.reminderMode
            val smartRequired = SmartEscalationPolicy.requiredWindowMinutes(spec.priority, SmartEscalationConfigStore.DEFAULT)
            val canFitSmart = spec.priority == TaskPriority.NORMAL || due - now > (smartRequired + 1) * 60_000L
            val mode = if (requestedMode == ReminderMode.SMART && !canFitSmart) ReminderMode.ALARM else requestedMode
            val reminderAt = if (mode == ReminderMode.SMART && smartRequired > 0) due - smartRequired * 60_000L else due
            val task = CreatorTask(
                id = UUID.randomUUID().toString(),
                title = "${request.topic} · ${spec.label}",
                platform = spec.platform,
                contentType = spec.contentType,
                dueLabel = WeeklyScheduleEngine.dueLabel(due),
                dueAtMillis = due,
                status = TaskStatus.PLANNED,
                progress = CreatorWorkflowEngine.progressForStage(stageIndex, template.stages.size),
                workflowStageIndex = stageIndex,
                reminderEnabled = mode != ReminderMode.NONE,
                reminderAtMillis = reminderAt,
                priority = spec.priority,
                notes = buildString {
                    append("Release Day · ${ReleaseDayEngine.eventLabel(request.eventType)}")
                    if (request.details.isNotBlank()) append("\n${request.details.trim()}")
                },
                alertType = if (mode == ReminderMode.ALARM || mode == ReminderMode.SMART) ReminderAlertType.ALARM else ReminderAlertType.NOTIFICATION,
                voiceEnabled = mode == ReminderMode.VOICE || mode == ReminderMode.SMART,
                smartEscalationEnabled = mode == ReminderMode.SMART,
                reminderMode = mode,
                voicePersona = defaults.defaultVoicePersona,
                voiceRepeatIntervalSeconds = 10,
                alarmTimeoutSeconds = defaults.defaultAlarmTimeoutSeconds,
                origin = CreatorTaskOrigin.RELEASE_DAY,
                sourceRefId = batchId,
            )
            if (mode == ReminderMode.SMART) smartConfigStore.put(task, SmartEscalationConfigStore.DEFAULT)
            created += task
        }

        created.asReversed().forEach { tasks.add(0, it) }
        if (created.isNotEmpty()) {
            persist()
            created.forEach(::scheduleTask)
        }

        var ideaSaved = false
        if (request.saveDeepDiveIdea) {
            ideas.add(
                0,
                CreatorIdea(
                    id = UUID.randomUUID().toString(),
                    title = "Deep dive · ${request.topic.trim()}",
                    topic = request.topic.trim(),
                    category = IdeaCategory.CINEMATIC_ANALYSIS,
                    status = IdeaStatus.INBOX,
                    potential = IdeaPotential.HIGH,
                    platformHint = "YouTube",
                    formatHint = "Long-form",
                    notes = buildString {
                        append("Saved from Release Day · ${ReleaseDayEngine.eventLabel(request.eventType)}")
                        if (request.details.isNotBlank()) append("\n${request.details.trim()}")
                    },
                    sourceRefId = batchId,
                )
            )
            persistIdeas()
            ideaSaved = true
        }

        return ReleaseLaunchResult(created.size, ideaSaved)
    }

    fun setWeeklyAutoPlanEnabled(enabled: Boolean) {
        weeklyAutoPlanState = enabled
        settingsStore.setWeeklyAutoPlanEnabled(enabled)
        if (enabled) syncWeeklyScheduleInternal() else removeUnstartedWeeklyProjects()
    }

    fun saveWeeklySlot(slot: WeeklyScheduleSlot) {
        val normalized = slot.copy(
            id = slot.id.ifBlank { "custom-${UUID.randomUUID()}" },
            title = slot.title.trim().ifBlank { "Creator Slot" },
            hour = slot.hour.coerceIn(0, 23),
            minute = slot.minute.coerceIn(0, 59),
        )
        val index = weeklySlots.indexOfFirst { it.id == normalized.id }
        if (index >= 0) weeklySlots[index] = normalized else weeklySlots += normalized
        persistWeeklySlots()
        if (weeklyAutoPlanEnabled) syncWeeklyScheduleInternal()
    }

    fun setWeeklySlotEnabled(id: String, enabled: Boolean) {
        val index = weeklySlots.indexOfFirst { it.id == id }
        if (index == -1) return
        weeklySlots[index] = weeklySlots[index].copy(enabled = enabled)
        persistWeeklySlots()
        if (weeklyAutoPlanEnabled && enabled) syncWeeklyScheduleInternal()
    }

    fun deleteWeeklySlot(id: String) {
        weeklySlots.removeAll { it.id == id }
        persistWeeklySlots()
    }

    fun resetWeeklySchedule() {
        weeklySlots.clear()
        weeklySlots.addAll(WeeklyScheduleEngine.defaultSlots())
        persistWeeklySlots()
        if (weeklyAutoPlanEnabled) syncWeeklyScheduleInternal()
    }

    fun refreshWeeklySchedule() {
        if (weeklyAutoPlanEnabled) syncWeeklyScheduleInternal()
    }

    fun reconcileReminders() = reconcileSnapshot(tasks.toList())

    private fun removeUnstartedWeeklyProjects() {
        val removed = tasks.filter { it.origin == CreatorTaskOrigin.WEEKLY && it.status == TaskStatus.PLANNED }
        if (removed.isEmpty()) return
        removed.forEach { cancelTaskAlerts(it.id) }
        tasks.removeAll { it.origin == CreatorTaskOrigin.WEEKLY && it.status == TaskStatus.PLANNED }
        persist()
    }

    private fun syncWeeklyScheduleInternal() {
        if (!weeklyAutoPlanEnabled || !tasksLoaded || !weeklyLoaded) return
        val occurrences = WeeklyScheduleEngine.upcomingOccurrences(weeklySlots, daysAhead = 8)
        var changed = false
        val toSchedule = mutableListOf<CreatorTask>()

        occurrences.forEach { occurrence ->
            val existingIndex = tasks.indexOfFirst { it.scheduleOccurrenceKey == occurrence.key }
            if (existingIndex == -1) {
                val created = buildScheduledTask(occurrence)
                tasks.add(0, created)
                toSchedule += created
                changed = true
            } else {
                val current = tasks[existingIndex]
                if (current.status != TaskStatus.DONE && current.status != TaskStatus.SKIPPED && current.autoStageReminder) {
                    val updated = syncScheduledTask(current, occurrence)
                    if (updated != current) {
                        tasks[existingIndex] = updated
                        toSchedule += updated
                        changed = true
                    }
                }
            }
        }

        if (changed) {
            persist()
            toSchedule.forEach(::scheduleTask)
        }
    }

    private fun buildScheduledTask(occurrence: ScheduleOccurrence): CreatorTask {
        val slot = occurrence.slot
        val template = CreatorWorkflowEngine.templateFor(slot.platform, slot.contentType)
        val stageIndex = WeeklyScheduleEngine.suggestedStageIndex(slot.platform, slot.contentType, occurrence.publishAtMillis)
        val progress = CreatorWorkflowEngine.progressForStage(stageIndex, template.stages.size)
        val enabled = slot.reminderMode != ReminderMode.NONE
        val internalAlert = if (slot.reminderMode == ReminderMode.ALARM || slot.reminderMode == ReminderMode.SMART) ReminderAlertType.ALARM else ReminderAlertType.NOTIFICATION
        val internalVoice = slot.reminderMode == ReminderMode.VOICE || slot.reminderMode == ReminderMode.SMART
        val internalSmart = slot.reminderMode == ReminderMode.SMART
        val defaults = settingsStore.snapshot()

        var task = CreatorTask(
            id = UUID.randomUUID().toString(),
            title = slot.title,
            platform = slot.platform,
            contentType = slot.contentType,
            dueLabel = WeeklyScheduleEngine.dueLabel(occurrence.publishAtMillis),
            dueAtMillis = occurrence.publishAtMillis,
            status = TaskStatus.PLANNED,
            progress = progress,
            workflowStageIndex = stageIndex,
            reminderEnabled = enabled,
            reminderAtMillis = 0L,
            priority = slot.priority,
            notes = "Weekly plan",
            alertType = internalAlert,
            voiceEnabled = internalVoice,
            smartEscalationEnabled = internalSmart,
            reminderMode = slot.reminderMode,
            voicePersona = defaults.defaultVoicePersona,
            voiceRepeatCount = 3,
            voiceRepeatIntervalSeconds = 10,
            alarmTimeoutSeconds = defaults.defaultAlarmTimeoutSeconds,
            scheduleSlotId = slot.id,
            scheduleOccurrenceKey = occurrence.key,
            autoStageReminder = enabled,
            origin = CreatorTaskOrigin.WEEKLY,
            sourceRefId = occurrence.key,
        )
        if (enabled) task = task.copy(reminderAtMillis = WeeklyScheduleEngine.reminderTargetForStage(task, stageIndex))
        if (task.reminderMode == ReminderMode.SMART) smartConfigStore.put(task, SmartEscalationConfigStore.DEFAULT)
        return task
    }

    private fun syncScheduledTask(task: CreatorTask, occurrence: ScheduleOccurrence): CreatorTask {
        val slot = occurrence.slot
        val formatChanged = task.platform != slot.platform || task.contentType != slot.contentType
        val template = CreatorWorkflowEngine.templateFor(slot.platform, slot.contentType)
        val stageIndex = if (formatChanged) {
            CreatorWorkflowEngine.stageIndexFromProgress(task.progress, template.stages.size)
        } else CreatorWorkflowEngine.stageIndex(task).coerceIn(0, template.stages.lastIndex)
        val mode = slot.reminderMode
        val enabled = mode != ReminderMode.NONE
        var updated = task.copy(
            title = slot.title,
            platform = slot.platform,
            contentType = slot.contentType,
            dueLabel = WeeklyScheduleEngine.dueLabel(occurrence.publishAtMillis),
            dueAtMillis = occurrence.publishAtMillis,
            workflowStageIndex = stageIndex,
            progress = CreatorWorkflowEngine.progressForStage(stageIndex, template.stages.size),
            priority = slot.priority,
            reminderMode = mode,
            reminderEnabled = enabled,
            alertType = if (mode == ReminderMode.ALARM || mode == ReminderMode.SMART) ReminderAlertType.ALARM else ReminderAlertType.NOTIFICATION,
            voiceEnabled = mode == ReminderMode.VOICE || mode == ReminderMode.SMART,
            smartEscalationEnabled = mode == ReminderMode.SMART,
            autoStageReminder = enabled,
            origin = CreatorTaskOrigin.WEEKLY,
            sourceRefId = occurrence.key,
        )
        updated = if (enabled) updated.copy(reminderAtMillis = WeeklyScheduleEngine.reminderTargetForStage(updated, stageIndex))
        else updated.copy(reminderAtMillis = 0L)
        if (updated.reminderMode == ReminderMode.SMART) smartConfigStore.put(updated, SmartEscalationConfigStore.DEFAULT)
        return updated
    }

    private fun applyAutoStageReminder(task: CreatorTask, stageIndex: Int): CreatorTask {
        if (!task.autoStageReminder || task.scheduleSlotId.isBlank() || task.reminderMode == ReminderMode.NONE) return task
        return task.copy(
            reminderEnabled = true,
            reminderAtMillis = WeeklyScheduleEngine.reminderTargetForStage(task, stageIndex),
            snoozeCount = 0,
            workingUntilMillis = 0L,
        )
    }

    /** Reconciliation is recovery, not a new schedule. Never erase an active Smart session here. */
    private fun reconcileSnapshot(snapshot: List<CreatorTask>) {
        val now = System.currentTimeMillis()
        snapshot.forEach { task ->
            val active = task.reminderEnabled &&
                task.reminderMode != ReminderMode.NONE &&
                task.status != TaskStatus.DONE &&
                task.status != TaskStatus.SKIPPED

            if (!active) {
                cancelTaskAlerts(task.id)
                return@forEach
            }

            if (task.reminderMode == ReminderMode.SMART || task.smartEscalationEnabled) {
                scheduler.cancel(task.id)
                smartScheduler.recover(task)
            } else if (task.reminderAtMillis > now) {
                smartScheduler.cancel(task.id)
                scheduler.schedule(task)
            } else {
                cancelTaskAlerts(task.id)
            }
        }
    }

    /** Intentional creator/project changes start a fresh schedule and therefore clear old Smart state. */
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

    private fun persistWeeklySlots() {
        val snapshot = weeklySlots.toList()
        viewModelScope.launch { weeklyStore.save(snapshot) }
    }

    private fun persistIdeas() {
        val snapshot = ideas.toList()
        viewModelScope.launch { ideaStore.save(snapshot) }
    }

    private fun persist() {
        val snapshot = tasks.toList()
        viewModelScope.launch { store.save(snapshot) }
    }
}
