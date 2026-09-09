package com.framebynavin.app.data

import android.app.Application
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.framebynavin.app.reminders.ReminderConstants
import com.framebynavin.app.reminders.ReminderNotifications
import com.framebynavin.app.reminders.ReminderRecoveryEngine
import com.framebynavin.app.reminders.ReminderScheduler
import com.framebynavin.app.reminders.SmartEscalationConfigStore
import com.framebynavin.app.reminders.SmartEscalationPolicy
import com.framebynavin.app.reminders.SmartEscalationScheduler
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.channels.Channel
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
    private val postPublishStore = CreatorPostPublishStore(application)
    private val rewardStore = CreatorRewardStore(application)
    private val rewardBackfillStore = CreatorRewardBackfillStore(application)
    private val publicationRecovery = CreatorPublicationRecovery(application)
    private val publicationRecoveryReady = CompletableDeferred<Unit>()

    val tasks = mutableStateListOf<CreatorTask>()
    val weeklySlots = mutableStateListOf<WeeklyScheduleSlot>()
    val ideas = mutableStateListOf<CreatorIdea>()
    val postPublishCheckpoints = mutableStateListOf<PostPublishCheckpoint>()
    val rewardLedger = mutableStateListOf<CreatorRewardLedgerEntry>()
    private val rewardFeedbackQueue = mutableStateListOf<CreatorRewardLedgerEntry>()
    val rewardFeedback: CreatorRewardLedgerEntry? get() = rewardFeedbackQueue.firstOrNull()

    val rewardSummary: CreatorRewardSummary
        get() = CreatorRewardEngine.summary(rewardLedger)

    private var weeklyAutoPlanState by mutableStateOf(settingsStore.snapshot().weeklyAutoPlanEnabled)
    val weeklyAutoPlanEnabled: Boolean get() = weeklyAutoPlanState

    private data class PendingWrite(
        val batch: Long,
        val generation: Long,
        val apply: suspend (Long) -> Unit,
    )
    private val writes = Channel<PendingWrite>(Channel.UNLIMITED)
    private var writeBatch = 0L
    private val taskEffectBuffer = ThreadLocal<MutableList<() -> Unit>?>()
    private var pendingWrites by mutableStateOf(0)
    val canRecoverWrites: Boolean get() = pendingWrites == 0
    private var optimisticTasks = emptyList<CreatorTask>()
    private var optimisticIdeas = emptyList<CreatorIdea>()
    private var optimisticWeekly = emptyList<WeeklyScheduleSlot>()
    var writeError by mutableStateOf<String?>(null)
        private set

    fun dismissWriteError() {
        viewModelScope.launch {
            if (pendingWrites != 0) return@launch
            runCatching {
                publicationRecovery.recoverAll()
                refreshCanonicalState()
            }
                .onSuccess { writeError = null }
                .onFailure { writeError = it.message ?: "Could not reload the latest data" }
        }
    }

    private fun enqueueWrite(apply: suspend (Long) -> Unit) {
        if (writeError != null) return
        val result = writes.trySend(PendingWrite(writeBatch, CreatorDataGate.generation(getApplication()), apply))
        if (result.isSuccess) pendingWrites++
        else writeError = "The save queue is unavailable. Reload saved data before editing."
    }

    private suspend fun refreshCanonicalState() {
        val latest = CreatorDataGate.transaction {
            Triple(store.load(), ideaStore.load(), weeklyStore.loadOrSeed())
        }
        optimisticTasks = latest.first
        optimisticIdeas = latest.second
        optimisticWeekly = latest.third
        tasks.clear(); tasks.addAll(latest.first)
        ideas.clear(); ideas.addAll(latest.second.sortedByDescending { it.updatedAtMillis })
        weeklySlots.clear(); weeklySlots.addAll(latest.third)
        tasksLoaded = true
        weeklyLoaded = true
        reconcileSnapshot(latest.first)
    }

    private var tasksLoaded = false
    private var weeklyLoaded = false

    init {
        viewModelScope.launch {
            for (write in writes) {
                try {
                    publicationRecoveryReady.await()
                    if (writeError == null && write.batch == writeBatch && write.generation == CreatorDataGate.generation(getApplication())) {
                        write.apply(write.generation)
                    }
                } catch (error: Throwable) {
                    if (error is kotlinx.coroutines.CancellationException) throw error
                    writeError = error.message ?: "A creator edit could not be saved. Review the latest data."
                    writeBatch++ // Reject dependent queued snapshots rather than apply a stale chain.
                } finally {
                    pendingWrites--
                    if (pendingWrites == 0) {
                        try {
                            refreshCanonicalState()
                            if (weeklyAutoPlanEnabled && writeError == null) syncWeeklyScheduleInternal()
                        } catch (error: Throwable) {
                            if (error is kotlinx.coroutines.CancellationException) throw error
                            writeError = error.message ?: "Could not reload saved creator data"
                        }
                    }
                }
            }
        }

        viewModelScope.launch {
            try {
                CreatorDataGate.readyTransaction(getApplication()) {
                    publicationRecovery.recoverAll()
                    rewardBackfillStore.runOnce(
                        rewardStore = rewardStore,
                        tasks = store.load(),
                        ideas = ideaStore.load(),
                        checkpoints = postPublishStore.load(),
                    )
                    publicationRecovery.recoverAll()
                }
                publicationRecoveryReady.complete(Unit)
            } catch (error: Throwable) {
                if (error is kotlinx.coroutines.CancellationException) throw error
                writeError = error.message ?: "Publication recovery needs attention. Your saved data was retained."
                // Keep the queue locked by writeError, but allow an explicit recovery retry.
                publicationRecoveryReady.complete(Unit)
            }
        }

        viewModelScope.launch {
            val slots = weeklyStore.loadOrSeed()
            if (pendingWrites == 0) {
                optimisticWeekly = slots
                weeklySlots.clear()
                weeklySlots.addAll(slots)
            }
            weeklyLoaded = true
            if (tasksLoaded) {
                if (weeklyAutoPlanEnabled) syncWeeklyScheduleInternal() else removeUnstartedWeeklyProjects()
            }
        }

        viewModelScope.launch {
            ideaStore.ideasFlow.collectLatest { saved ->
                if (pendingWrites == 0) {
                    optimisticIdeas = saved
                    ideas.clear()
                    ideas.addAll(saved.sortedByDescending { it.updatedAtMillis })
                }
            }
        }

        viewModelScope.launch {
            postPublishStore.checkpointsFlow.collectLatest { saved ->
                postPublishCheckpoints.clear()
                postPublishCheckpoints.addAll(saved.sortedWith(compareBy<PostPublishCheckpoint> { it.status != PostPublishCheckpointStatus.PENDING }.thenBy { it.dueAtMillis }))
            }
        }

        viewModelScope.launch {
            rewardStore.ledgerFlow.collectLatest { saved ->
                rewardLedger.clear()
                rewardLedger.addAll(saved.sortedByDescending { it.occurredAtMillis })
            }
        }

        viewModelScope.launch {
            store.tasksFlow.collectLatest { saved ->
                publicationRecoveryReady.await()
                if (pendingWrites > 0 || writeError != null) return@collectLatest
                val cleaned = saved.filterNot {
                    it.id == "starter-frame-breakdown" ||
                        (it.origin == CreatorTaskOrigin.WEEKLY && WeeklyScheduleEngine.isLegacySeedSlot(it.scheduleSlotId)) ||
                        CreatorPostPublishEngine.isLegacyTask(it)
                }.map(ProjectPulseEngine::repairFalseSurfaceCompletion)
                if (cleaned != saved) {
                    // Re-evaluate against current storage. A stale collector must not replace newer data.
                    store.mutate { current ->
                        current.filterNot {
                            it.id == "starter-frame-breakdown" ||
                                (it.origin == CreatorTaskOrigin.WEEKLY && WeeklyScheduleEngine.isLegacySeedSlot(it.scheduleSlotId)) ||
                                CreatorPostPublishEngine.isLegacyTask(it)
                        }.map(ProjectPulseEngine::repairFalseSurfaceCompletion)
                    }
                    val legacy = saved.filter(CreatorPostPublishEngine::isLegacyTask)
                    if (legacy.isNotEmpty()) postPublishStore.migrateLegacy(legacy)
                    return@collectLatest
                }
                optimisticTasks = cleaned
                tasks.clear(); tasks.addAll(cleaned)
                reconcileSnapshot(cleaned)
                tasksLoaded = true
                if (weeklyLoaded && pendingWrites == 0 && writeError == null) {
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
        attentionPlan: ProjectAttentionPlan = ProjectAttentionPlan.CUSTOM,
    ): String? {
        if (title.isBlank()) return null
        val enabled = reminderMode != ReminderMode.NONE
        val normalizedReminderAt = if (enabled) reminderAtMillis else 0L
        val internalAlertType = if (reminderMode == ReminderMode.ALARM || reminderMode == ReminderMode.SMART)
            ReminderAlertType.ALARM else ReminderAlertType.NOTIFICATION
        val internalVoice = reminderMode == ReminderMode.VOICE || reminderMode == ReminderMode.SMART
        val internalSmart = reminderMode == ReminderMode.SMART

        if (id == null) {
            val baseTask = CreatorTask(
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
                attentionPlan = if (enabled) ProjectAttentionPlan.CUSTOM else ProjectAttentionPlan.OFF,
            )
            val task = if (attentionPlan == ProjectAttentionPlan.CUSTOM) {
                baseTask.copy(attentionPlan = if (enabled) ProjectAttentionPlan.CUSTOM else ProjectAttentionPlan.OFF)
            } else ProjectPulseEngine.applyAttentionPlan(baseTask, attentionPlan)
            if (task.reminderMode == ReminderMode.SMART) putSmartConfig(task)
            tasks.add(0, task)
            persist()
            return task.id
        }

        val index = tasks.indexOfFirst { it.id == id }
        if (index == -1) return null
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

        val configured = current.copy(
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
            attentionPlan = if (enabled) ProjectAttentionPlan.CUSTOM else ProjectAttentionPlan.OFF,
            pulseManagedReminder = false,
            acknowledgedCheckpointStageId = "",
            acknowledgedCheckpointDueAtMillis = 0L,
            checkpointStageId = "",
            checkpointAtMillis = 0L,
        )
        val updated = if (attentionPlan == ProjectAttentionPlan.CUSTOM) configured
        else ProjectPulseEngine.applyAttentionPlan(configured, attentionPlan)
        if (updated.reminderMode == ReminderMode.SMART) putSmartConfig(updated)
        tasks[index] = updated
        persist()
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
            attentionPlan = ProjectAttentionPlan.CUSTOM,
            pulseManagedReminder = false,
            acknowledgedCheckpointStageId = "",
            acknowledgedCheckpointDueAtMillis = 0L,
            checkpointStageId = "",
            checkpointAtMillis = 0L,
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
            attentionPlan = ProjectAttentionPlan.OFF,
            pulseManagedReminder = false,
            checkpointStageId = "",
            checkpointAtMillis = 0L,
            autoStageReminder = false,
        )
    }

    fun startTask(id: String) = updateTask(id) { task ->
        val isSmart = task.reminderMode == ReminderMode.SMART || task.smartEscalationEnabled
        val template = CreatorWorkflowEngine.templateFor(task)
        val stageIndex = CreatorWorkflowEngine.stageIndex(task)
        val started = task.copy(
            status = TaskStatus.WORKING,
            workflowStageIndex = stageIndex,
            progress = CreatorWorkflowEngine.progressForStage(stageIndex, template.stages.size),
            workingUntilMillis = if ((isSmart || task.pulseManagedReminder) && task.reminderEnabled)
                System.currentTimeMillis() + ReminderConstants.WORKING_QUIET_MINUTES * 60_000L
            else task.workingUntilMillis,
        )
        val updated = if (started.pulseManagedReminder) ProjectPulseEngine.refreshManagedReminder(started) else started
        scheduleTask(updated)
        updated
    }

    fun advanceTask(id: String) = advanceWorkflow(id)

    fun advanceWorkflow(id: String) {
        var stageReward: CreatorRewardLedgerEntry? = null
        updateTask(id, transform = { task ->
            if (task.status == TaskStatus.DONE) return@updateTask task
            val now = System.currentTimeMillis()
            val stage = CreatorWorkflowEngine.currentStage(task)
            stageReward = CreatorRewardEngine.stageCompleted(id, stage.id, stage.label, now)
            val updated = CreatorPublicationEngine.advance(task, now)
            if (updated.status == TaskStatus.DONE) {
                cancelTaskAlerts(task.id)
                updated
            } else {
                val nextIndex = CreatorWorkflowEngine.stageIndex(updated)
                val scheduled = if (updated.pulseManagedReminder) ProjectPulseEngine.refreshManagedReminder(updated)
                else applyAutoStageReminder(updated, nextIndex)
                if (scheduled.reminderMode == ReminderMode.SMART) putSmartConfig(scheduled)
                scheduleTask(scheduled)
                scheduled
            }
        }, recoveryStage = { _, _ -> stageReward }, recoverPublication = true)
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
            workingUntilMillis = 0L,
        )
        updated = if (updated.pulseManagedReminder) ProjectPulseEngine.refreshManagedReminder(updated)
        else applyAutoStageReminder(updated, previous)
        if (updated.reminderMode == ReminderMode.SMART) putSmartConfig(updated)
        scheduleTask(updated)
        updated
    }

    /** Finishing a project does not assert that it was published. */
    fun completeTask(id: String) = updateTask(id) { task ->
        if (task.status == TaskStatus.DONE) return@updateTask task
        cancelTaskAlerts(task.id)
        CreatorPublicationEngine.finish(task, System.currentTimeMillis())
    }

    /** Record publication independently of the remaining stages, without a duplicate reward. */
    fun markPublished(id: String, atMillis: Long = System.currentTimeMillis(), url: String = "") =
        correctPublication(id, atMillis, url)

    fun correctPublication(id: String, atMillis: Long, url: String = "") {
        updateTask(id, transform = { task ->
            CreatorPublicationEngine.correct(task, atMillis, url)
        }, recoverPublication = true)
    }

    fun skipTask(id: String) = updateTask(id) { task ->
        cancelTaskAlerts(task.id)
        task.copy(
            status = TaskStatus.SKIPPED,
            reminderEnabled = false,
            smartEscalationEnabled = false,
            voiceEnabled = false,
            reminderMode = ReminderMode.NONE,
            attentionPlan = ProjectAttentionPlan.OFF,
            pulseManagedReminder = false,
            checkpointStageId = "",
            checkpointAtMillis = 0L,
            workingUntilMillis = 0L,
            autoStageReminder = false,
        )
    }

    fun publishLate(id: String, delayMinutes: Int = 30) = updateTask(id) { task ->
        val now = System.currentTimeMillis()
        val due = now + delayMinutes.coerceIn(10, 180) * 60_000L
        val moved = task.copy(
            dueAtMillis = due,
            dueLabel = WeeklyScheduleEngine.dueLabel(due),
            reminderAtMillis = if (!task.pulseManagedReminder && task.reminderEnabled && task.reminderMode != ReminderMode.NONE) due else task.reminderAtMillis,
            snoozeCount = 0,
            workingUntilMillis = 0L,
            autoStageReminder = false,
        )
        val updated = if (moved.pulseManagedReminder) ProjectPulseEngine.refreshManagedReminder(moved) else moved
        if (updated.reminderMode == ReminderMode.SMART) putSmartConfig(updated)
        scheduleTask(updated)
        updated
    }

    fun rescheduleDeadline(id: String, atMillis: Long) = updateTask(id) { task ->
        if (atMillis <= System.currentTimeMillis()) return@updateTask task
        val rescheduled = task.copy(
            dueAtMillis = atMillis,
            dueLabel = WeeklyScheduleEngine.dueLabel(atMillis),
            reminderAtMillis = if (!task.pulseManagedReminder && task.reminderEnabled && task.reminderMode != ReminderMode.NONE) atMillis else task.reminderAtMillis,
            snoozeCount = 0,
            workingUntilMillis = 0L,
            autoStageReminder = false,
        )
        val updated = if (rescheduled.pulseManagedReminder) ProjectPulseEngine.refreshManagedReminder(rescheduled) else rescheduled
        if (updated.reminderMode == ReminderMode.SMART) putSmartConfig(updated)
        scheduleTask(updated)
        updated
    }

    /** Remove alert configuration only. Project/task data remains intact. */
    fun cancelReminders(ids: Set<String>) {
        if (ids.isEmpty()) return
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
                    attentionPlan = ProjectAttentionPlan.OFF,
                    pulseManagedReminder = false,
                    checkpointStageId = "",
                    checkpointAtMillis = 0L,
                    autoStageReminder = false,
                )
                changed = true
            }
        }
        if (changed) persist()
    }

    fun archiveTasks(ids: Set<String>) {
        if (ids.isEmpty()) return
        val now = System.currentTimeMillis()
        var changed = false
        tasks.indices.forEach { index ->
            val task = tasks[index]
            if (task.id in ids && task.status == TaskStatus.DONE && task.archivedAtMillis <= 0L) {
                tasks[index] = task.copy(archivedAtMillis = now)
                changed = true
            }
        }
        if (changed) persist()
    }

    fun archiveTask(id: String) = archiveTasks(setOf(id))

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
        viewModelScope.launch { postPublishStore.deleteForProjects(ids) }
        if (ideasChanged) persistIdeas()
    }

    fun deleteTask(id: String) = deleteTasks(setOf(id))

    fun saveIdea(idea: CreatorIdea): String? {
        if (idea.title.isBlank()) return null
        val now = System.currentTimeMillis()
        val isNewIdea = idea.id.isBlank() || ideas.none { it.id == idea.id }
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
        if (isNewIdea && normalized.title.length >= 3) {
            recordReward(CreatorRewardEngine.ideaDailyCapture(normalized.id, now))
        }
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
        val defaults = settingsStore.snapshot()
        val taskId = UUID.randomUUID().toString()
        val baseTask = CreatorTask(
            id = taskId,
            title = idea.title,
            platform = platform,
            contentType = contentType,
            dueLabel = WeeklyScheduleEngine.dueLabel(due),
            dueAtMillis = due,
            status = TaskStatus.PLANNED,
            progress = 0,
            workflowStageIndex = 0.coerceAtMost(template.stages.lastIndex),
            reminderEnabled = false,
            reminderAtMillis = 0L,
            priority = TaskPriority.IMPORTANT,
            notes = buildString {
                append("From Idea Vault")
                if (idea.topic.isNotBlank()) append(" · ${idea.topic}")
                if (idea.notes.isNotBlank()) append("\n${idea.notes}")
            },
            alertType = ReminderAlertType.NOTIFICATION,
            voiceEnabled = false,
            smartEscalationEnabled = false,
            reminderMode = ReminderMode.NONE,
            voicePersona = defaults.defaultVoicePersona,
            voiceRepeatIntervalSeconds = 10,
            alarmTimeoutSeconds = defaults.defaultAlarmTimeoutSeconds,
            origin = CreatorTaskOrigin.IDEA_VAULT,
            sourceRefId = idea.id,
        )
        val task = ProjectPulseEngine.applyAttentionPlan(baseTask, ProjectAttentionPlan.GUIDED, now)
        if (task.reminderMode == ReminderMode.SMART) putSmartConfig(task)
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
        recordReward(CreatorRewardEngine.ideaConverted(idea.id, taskId, now))
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
            if (mode == ReminderMode.SMART) putSmartConfig(task)
            created += task
        }

        created.asReversed().forEach { tasks.add(0, it) }
        if (created.isNotEmpty()) {
            persist()
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

    private fun ensurePostPublishCheckpoints(parent: CreatorTask) {
        if (parent.publishedAtMillis <= 0L) return
        viewModelScope.launch { postPublishStore.ensureFor(parent) }
    }

    fun completePostPublishCheckpoint(checkpointId: String) = updatePostPublishCheckpoint(checkpointId, PostPublishCheckpointStatus.DONE)

    fun skipPostPublishCheckpoint(checkpointId: String) = updatePostPublishCheckpoint(checkpointId, PostPublishCheckpointStatus.SKIPPED)

    private fun updatePostPublishCheckpoint(checkpointId: String, status: PostPublishCheckpointStatus) {
        enqueueWrite { epoch ->
            val (_, added) = publicationRecovery.updateCheckpoint(checkpointId, status, epoch)
            added.forEach(::enqueueRewardFeedback)
        }
    }

    private fun recordReward(entry: CreatorRewardLedgerEntry) {
        viewModelScope.launch {
            if (rewardStore.record(entry)) enqueueRewardFeedback(entry)
        }
    }

    private fun enqueueRewardFeedback(entry: CreatorRewardLedgerEntry) {
        if (rewardFeedbackQueue.any { it.eventKey == entry.eventKey }) return
        if (rewardFeedbackQueue.size >= 4) rewardFeedbackQueue.removeAt(0)
        rewardFeedbackQueue += entry
    }

    fun consumeRewardFeedback(eventKey: String) {
        val index = rewardFeedbackQueue.indexOfFirst { it.eventKey == eventKey }
        if (index >= 0) rewardFeedbackQueue.removeAt(index)
    }

    private fun removeUnstartedWeeklyProjects() {
        val removed = tasks.filter { it.origin == CreatorTaskOrigin.WEEKLY && it.status == TaskStatus.PLANNED }
        if (removed.isEmpty()) return
        tasks.removeAll { it.origin == CreatorTaskOrigin.WEEKLY && it.status == TaskStatus.PLANNED }
        persist()
    }

    private fun syncWeeklyScheduleInternal() {
        if (!weeklyAutoPlanEnabled || !tasksLoaded || !weeklyLoaded || pendingWrites > 0) return
        val occurrences = WeeklyScheduleEngine.upcomingOccurrences(weeklySlots, daysAhead = CreatorAutoPlanEngine.DEFAULT_HORIZON_DAYS)
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

        if (changed) persist()
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
        if (task.reminderMode == ReminderMode.SMART) putSmartConfig(task)
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
        if (updated.reminderMode == ReminderMode.SMART) putSmartConfig(updated)
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

    /** Reconciliation is recovery, not a new schedule. Boot, WorkManager and app resume share one engine. */
    private fun reconcileSnapshot(snapshot: List<CreatorTask>) {
        val now = System.currentTimeMillis()
        snapshot.forEach { task ->
            ReminderRecoveryEngine.reconcileTask(getApplication<Application>(), task, now)
        }
    }

    /** Intentional creator/project changes start a fresh schedule and therefore clear old Smart state. */
    private fun deferTaskEffect(effect: () -> Unit) {
        val buffer = taskEffectBuffer.get()
        if (buffer != null) buffer.add(effect) else effect()
    }

    private fun putSmartConfig(task: CreatorTask) = deferTaskEffect {
        smartConfigStore.put(task, SmartEscalationConfigStore.DEFAULT)
    }

    private fun scheduleTask(task: CreatorTask) = deferTaskEffect { scheduleTaskNow(task) }

    private fun scheduleTaskNow(task: CreatorTask) {
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

    private fun cancelTaskAlerts(taskId: String) = deferTaskEffect { cancelTaskAlertsNow(taskId) }

    private fun cancelTaskAlertsNow(taskId: String) {
        scheduler.cancel(taskId)
        smartScheduler.cancel(taskId)
        ReminderNotifications.cancel(getApplication<Application>(), taskId)
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

    private fun updateTask(
        id: String,
        after: suspend (CreatorTask) -> Unit = {},
        recoverPublication: Boolean = false,
        recoveryStage: (CreatorTask, CreatorTask) -> CreatorRewardLedgerEntry? = { _, _ -> null },
        transform: (CreatorTask) -> CreatorTask,
    ) {
        enqueueWrite { epoch -> CreatorDataGate.readyTransaction(getApplication()) {
            publicationRecovery.recoverPendingUnlocked()
            CreatorDataGate.checkGeneration(getApplication(), epoch)
            val before = store.load().firstOrNull { it.id == id } ?: return@readyTransaction
            val effects = mutableListOf<() -> Unit>()
            val previous = taskEffectBuffer.get()
            taskEffectBuffer.set(effects)
            val desired = try { transform(before) } finally { taskEffectBuffer.set(previous) }
            val evidence = if (recoverPublication) recoveryStage(before, desired) else null
            if (recoverPublication && desired != before) {
                publicationRecovery.begin(epoch, id, evidence, desired.workflowStageIndex, desired.completedAtMillis,
                    CreatorWorkflowEngine.stageIndex(before), desired.status.name)
            }
            val updated = store.updateTask(id, expectedGeneration = epoch) { current ->
                check(current == before) { "The project changed during its save" }
                desired
            }
            if (updated != null) {
                effects.forEach { it() }
                if (recoverPublication && desired != before) {
                    publicationRecovery.finish(epoch).forEach(::enqueueRewardFeedback)
                }
                after(updated)
            }
        } }
    }

    private fun persistWeeklySlots() {
        val base = optimisticWeekly
        val desired = weeklySlots.toList()
        optimisticWeekly = desired
        enqueueWrite { epoch -> weeklyStore.applyDelta(base, desired, epoch) }
    }

    private fun persistIdeas() {
        val base = optimisticIdeas
        val desired = ideas.toList()
        optimisticIdeas = desired
        enqueueWrite { epoch -> ideaStore.applyDelta(base, desired, epoch) }
    }

    private fun persist() {
        val base = optimisticTasks
        val desired = tasks.toList()
        optimisticTasks = desired
        enqueueWrite { epoch -> CreatorDataGate.transaction {
            val committed = store.applyDelta(base, desired, epoch)
            val before = base.associateBy { it.id }
            val after = committed.associateBy { it.id }
            (before.keys + after.keys).forEach { id ->
                val old = before[id]
                val current = after[id]
                if (old == current) return@forEach
                if (current == null || current.status == TaskStatus.DONE || current.status == TaskStatus.SKIPPED || !current.reminderEnabled) {
                    cancelTaskAlerts(id)
                } else if (old == null || old.reminderAtMillis != current.reminderAtMillis || old.reminderMode != current.reminderMode || old.reminderEnabled != current.reminderEnabled || old.status != current.status || old.checkpointStageId != current.checkpointStageId || old.dueAtMillis != current.dueAtMillis) {
                    if (current.reminderMode == ReminderMode.SMART) putSmartConfig(current)
                    scheduleTask(current)
                }
            }
        } }
    }
}
