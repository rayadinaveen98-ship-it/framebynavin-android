package com.framebynavin.app.data

import android.content.Context
import org.json.JSONObject

/** Crash recovery across the existing independent task, checkpoint and reward stores.
 * The marker is execution state, not a second publication model or portable backup section.
 * Every operation re-reads current authority under CreatorDataGate.
 */
class CreatorPublicationRecovery(context: Context) {
    private val appContext = context.applicationContext
    private val prefs = appContext.getSharedPreferences("creator_publication_recovery_v183", Context.MODE_PRIVATE)
    private val tasks = TaskStore(appContext)
    private val checkpoints = CreatorPostPublishStore(appContext)
    private val rewards = CreatorRewardStore(appContext)

    private fun pending(): JSONObject? = prefs.getString("pending", null)?.let(::JSONObject)

    private fun clear() {
        check(prefs.edit().remove("pending").commit()) { "Could not clear publication recovery marker" }
    }

    /** Called under the data gate before a source-of-truth mutation. */
    suspend fun begin(expectedGeneration: Long, subjectId: String,
                      stageEvidence: CreatorRewardLedgerEntry? = null,
                      expectedStageIndex: Int = -1, expectedCompletedAt: Long = 0L,
                      sourceStageIndex: Int = -1, expectedStatus: String = "") {
        CreatorDataGate.transaction {
            recoverPendingUnlocked()
            CreatorDataGate.checkGeneration(appContext, expectedGeneration)
            require(subjectId.isNotBlank())
            val marker = JSONObject().put("version", 1).put("generation", expectedGeneration)
                .put("subjectId", subjectId).put("expectedStageIndex", expectedStageIndex)
                .put("expectedCompletedAt", expectedCompletedAt).put("sourceStageIndex", sourceStageIndex)
                .put("expectedStatus", expectedStatus)
            stageEvidence?.let { event ->
                require(event.type == CreatorRewardEventType.PROJECT_STAGE_COMPLETED)
                require(event.projectId == subjectId && event.subjectId.isNotBlank())
                require(sourceStageIndex >= 0 && expectedStageIndex >= 0)
                marker.put("stageEvent", JSONObject()
                    .put("stageId", event.subjectId).put("label", event.label)
                    .put("occurredAt", event.occurredAtMillis))
            }
            check(prefs.edit().putString("pending", marker.toString()).commit()) {
                "Could not prepare publication recovery"
            }
        }
    }

    /** Reconcile first; clear the marker only after all durable writes succeed. */
    suspend fun finish(expectedGeneration: Long): List<CreatorRewardLedgerEntry> = CreatorDataGate.transaction {
        CreatorDataGate.checkGeneration(appContext, expectedGeneration)
        val added = reconcileUnlocked(pending())
        clear()
        added
    }

    /** Startup and explicit retry. A previous-generation marker must never replay after restore. */
    suspend fun recoverAll(): List<CreatorRewardLedgerEntry> = CreatorDataGate.readyTransaction(appContext) {
        val marker = pending()
        if (marker != null && marker.getLong("generation") != CreatorDataGate.generation(appContext)) {
            clear()
            reconcileUnlocked(null)
        } else {
            val added = reconcileUnlocked(marker)
            if (marker != null) clear()
            added
        }
    }

    suspend fun recoverPendingUnlocked(): List<CreatorRewardLedgerEntry> = CreatorDataGate.transaction {
        val marker = pending() ?: return@transaction emptyList()
        if (marker.getLong("generation") != CreatorDataGate.generation(appContext)) {
            clear()
            return@transaction emptyList()
        }
        val added = reconcileUnlocked(marker)
        clear()
        added
    }

    /** Called only after restore has imported every authoritative section, inside its gate.
     * Never calls readyTransaction: the restore journal is still present at this point.
     */
    suspend fun reconcileRestoredState(): List<CreatorRewardLedgerEntry> = CreatorDataGate.transaction {
        val added = reconcileUnlocked(null)
        if (prefs.contains("pending")) clear()
        added
    }

    private suspend fun reconcileUnlocked(marker: JSONObject?): List<CreatorRewardLedgerEntry> {
        if (marker != null) require(marker.getInt("version") == 1) { "Unsupported publication recovery marker" }
        val currentTasks = tasks.load()
        val currentCheckpoints = checkpoints.reconcilePublications(currentTasks.filterNot(CreatorPostPublishEngine::isLegacyTask))
        val stageEvidence = marker?.takeIf { it.has("stageEvent") }?.getJSONObject("stageEvent")?.let { event ->
            val projectId = marker.getString("subjectId")
            val task = currentTasks.firstOrNull { it.id == projectId }
            val expectedIndex = marker.getInt("expectedStageIndex")
            val sourceIndex = marker.getInt("sourceStageIndex")
            val expectedCompleted = marker.getLong("expectedCompletedAt")
            val expectedStatus = marker.getString("expectedStatus")
            val validStage = task != null && CreatorWorkflowEngine.templateFor(task).stages
                .getOrNull(sourceIndex)?.id == event.getString("stageId")
            val reached = task != null && validStage && when (expectedStatus) {
                TaskStatus.DONE.name -> expectedCompleted > 0L && task.status == TaskStatus.DONE &&
                    task.completedAtMillis >= expectedCompleted
                TaskStatus.WORKING.name -> expectedIndex > sourceIndex && task.workflowStageIndex >= expectedIndex
                else -> false
            }
            if (!reached) null else CreatorRewardEngine.stageCompleted(
                projectId, event.getString("stageId"), event.getString("label"), event.getLong("occurredAt"))
        }
        return rewards.reconcileAuthoritative(currentTasks, currentCheckpoints, stageEvidence)
    }

    suspend fun updateCheckpoint(checkpointId: String, status: PostPublishCheckpointStatus,
                                 expectedGeneration: Long, atMillis: Long = System.currentTimeMillis()):
        Pair<PostPublishCheckpoint?, List<CreatorRewardLedgerEntry>> = CreatorDataGate.readyTransaction(appContext) {
        recoverPendingUnlocked()
        CreatorDataGate.checkGeneration(appContext, expectedGeneration)
        val existing = checkpoints.load().firstOrNull { it.id == checkpointId }
            ?: return@readyTransaction null to emptyList()
        begin(expectedGeneration, existing.projectId)
        val updated = checkpoints.updateStatus(checkpointId, status, atMillis)
        updated to finish(expectedGeneration)
    }
}
