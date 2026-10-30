package com.framebynavin.app.data

import android.content.Context
import org.json.JSONObject
import java.util.UUID

internal data class IdeaProjectConversionRequest(
    val ideaId: String,
    val platform: String,
    val contentType: String,
    val dueAtMillis: Long,
    val attentionPlan: ProjectAttentionPlan,
    val generation: Long,
)

/** Roll-forward journal for the existing independent stores. Never an optimistic project save.
 * All replay happens inside CreatorDataGate; old restore generations cannot create projects.
 */
internal class IdeaProjectConversion(context: Context, private val afterWrite: (String) -> Unit = {}) {
    private val context = context.applicationContext
    private val prefs = this.context.getSharedPreferences("idea_conversion_v185", Context.MODE_PRIVATE)
    private val tasks = TaskStore(this.context)
    private val ideas = IdeaVaultStore(this.context)
    private val rewards = CreatorRewardStore(this.context)

    fun rememberedPlan(): ProjectAttentionPlan = runCatching {
        ProjectAttentionPlan.valueOf(prefs.getString("last_plan", ProjectAttentionPlan.OFF.name)!!)
    }.getOrDefault(ProjectAttentionPlan.OFF).takeIf { it in supportedPlans } ?: ProjectAttentionPlan.OFF

    suspend fun convert(request: IdeaProjectConversionRequest): String = CreatorDataGate.readyTransaction(context) {
        CreatorDataGate.checkGeneration(context, request.generation)
        require(request.attentionPlan in supportedPlans)
        require(request.platform.isNotBlank() && request.contentType.isNotBlank())
        val idea = ideas.load().firstOrNull { it.id == request.ideaId }
            ?: error("This idea is no longer available. Your other ideas were kept.")
        check(!idea.isTrashed && idea.status != IdeaStatus.ARCHIVED) { "Restore or unarchive this idea before creating a project." }
        val currentTasks = tasks.load()
        if (idea.projectTaskId.isNotBlank()) {
            return@readyTransaction currentTasks.firstOrNull { it.id == idea.projectTaskId }?.id
                ?: error("The linked project is missing. No second project was created.")
        }
        check(idea.status != IdeaStatus.CONVERTED) { "This idea was already converted. Review its linked project." }
        // Repair an older interrupted conversion without making another project.
        val prior = currentTasks.filter { it.origin == CreatorTaskOrigin.IDEA_VAULT && it.sourceRefId == idea.id }
        check(prior.size <= 1) { "More than one project is linked to this idea. Review them before continuing." }
        val now = System.currentTimeMillis()
        val task = prior.singleOrNull() ?: buildTask(idea, request, now)
        val marker = JSONObject().put("version", 1).put("generation", request.generation)
            .put("ideaId", idea.id).put("task", tasks.encodeRecoveryTask(task))
            .put("atMillis", now)
        check(prefs.edit().putString("pending", marker.toString()).commit()) { "Could not prepare this project. Your idea was kept." }
        afterWrite("prepared")
        recoverPendingUnlocked()
        task.id
    }

    suspend fun recoverPendingUnlocked() = CreatorDataGate.transaction {
        val raw = prefs.getString("pending", null) ?: return@transaction
        val marker = JSONObject(raw)
        if (marker.getLong("generation") != CreatorDataGate.generation(context)) {
            clearPending()
            return@transaction
        }
        require(marker.getInt("version") == 1) { "Unsupported idea conversion recovery record" }
        val task = tasks.decodeRecoveryTask(marker.getString("task"))
        val ideaId = marker.getString("ideaId")
        check(task.origin == CreatorTaskOrigin.IDEA_VAULT && task.sourceRefId == ideaId)
        val idea = ideas.load().firstOrNull { it.id == ideaId }
        val existing = tasks.load().firstOrNull { it.id == task.id }
        if (existing == null && (idea == null || idea.isTrashed || idea.status == IdeaStatus.ARCHIVED)) {
            clearPending() // No project was committed; never resurrect a removed idea.
            return@transaction
        }
        check(idea != null && !idea.isTrashed && idea.status != IdeaStatus.ARCHIVED) {
            "Project recovery needs its original idea. Saved work has been kept."
        }
        check(idea.projectTaskId.isBlank() || idea.projectTaskId == task.id) { "This idea is linked to a different project. Saved work has been kept." }
        if (existing == null) {
            tasks.mutate { current ->
                check(current.none { it.origin == CreatorTaskOrigin.IDEA_VAULT && it.sourceRefId == ideaId })
                listOf(task) + current
            }
        } else check(existing.origin == CreatorTaskOrigin.IDEA_VAULT && existing.sourceRefId == ideaId)
        afterWrite("project")
        ideas.mutate { current -> current.map { source ->
            if (source.id != ideaId) source else source.copy(
                status = IdeaStatus.CONVERTED, projectTaskId = task.id,
                platformHint = task.platform, formatHint = task.contentType,
                reminderAtMillis = 0L, reminderCadence = IdeaReminderCadence.ONCE,
                updatedAtMillis = maxOf(source.updatedAtMillis, marker.getLong("atMillis")),
            )
        } }
        afterWrite("link")
        rewards.record(CreatorRewardEngine.ideaConverted(ideaId, task.id, marker.getLong("atMillis")))
        afterWrite("reward")
        check(prefs.edit().putString("last_plan", task.attentionPlan.name).remove("pending").commit()) {
            "Project saved. Reopen the app to finish recovery."
        }
    }

    private fun clearPending() {
        check(prefs.edit().remove("pending").commit()) { "Could not clear idea conversion recovery" }
    }

    private fun buildTask(idea: CreatorIdea, request: IdeaProjectConversionRequest, now: Long): CreatorTask {
        require(request.dueAtMillis >= now + 60_000L) { "Choose a deadline at least a minute from now." }
        val defaults = CreatorOsSettingsStore(context).snapshot()
        val task = CreatorTask(
            id = UUID.randomUUID().toString(), title = idea.title,
            platform = request.platform, contentType = request.contentType,
            dueLabel = WeeklyScheduleEngine.dueLabel(request.dueAtMillis), dueAtMillis = request.dueAtMillis,
            status = TaskStatus.PLANNED, progress = 0, workflowStageIndex = 0,
            reminderEnabled = false, reminderAtMillis = 0L, priority = TaskPriority.IMPORTANT,
            notes = IdeaProjectBridge.projectNotes(idea), alertType = ReminderAlertType.NOTIFICATION,
            voiceEnabled = false, smartEscalationEnabled = false, reminderMode = ReminderMode.NONE,
            voicePersona = defaults.defaultVoicePersona, voiceRepeatIntervalSeconds = 10,
            alarmTimeoutSeconds = defaults.defaultAlarmTimeoutSeconds,
            origin = CreatorTaskOrigin.IDEA_VAULT, sourceRefId = idea.id,
        )
        return ProjectPulseEngine.applyAttentionPlan(task, request.attentionPlan, now)
    }

    companion object {
        val supportedPlans = listOf(ProjectAttentionPlan.OFF, ProjectAttentionPlan.LIGHT, ProjectAttentionPlan.GUIDED, ProjectAttentionPlan.URGENT)
    }
}
