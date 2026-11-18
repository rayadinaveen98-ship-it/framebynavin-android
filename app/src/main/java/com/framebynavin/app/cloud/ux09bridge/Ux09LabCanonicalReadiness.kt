package com.framebynavin.app.cloud.ux09bridge

import android.content.Context
import com.framebynavin.app.BuildConfig
import com.framebynavin.app.cloud.CloudConfig
import com.framebynavin.app.data.CreatorDataGate
import com.framebynavin.app.data.IdeaVaultStore
import com.framebynavin.app.data.TaskStore
import kotlinx.coroutines.flow.first

/**
 * Actual TaskStore and IdeaVaultStore read-only bridge, not synthetic models.
 *
 * No hosted traffic, no edits, no backup restore, no media cleanup, no account
 * changes, and no existing editor or worker call site. Explicit invocation by
 * a future Lab QA screen/test only. Refuses non-Lab builds before reading data.
 */
internal class Ux09LabCanonicalReadiness(context: Context) {
    private val app = context.applicationContext
    private val taskStore = TaskStore(app)
    private val ideaStore = IdeaVaultStore(app)

    internal data class Summary(
        val creatorGeneration: Long,
        val projectCount: Int,
        val ideaCount: Int,
        val projectCandidates: Int,
        val ideaCandidates: Int,
        val reasons: Map<Ux09CanonicalReadinessPolicy.Blocker, Int>,
    ) {
        val blocked: Int get() = projectCount + ideaCount - projectCandidates - ideaCandidates
    }

    suspend fun inspect(): Summary {
        check(BuildConfig.BACKLOT_LAB &&
            CloudConfig.SUPABASE_URL == "https://vasleafrmsovdbdlrcsw.supabase.co") {
            "This read-only Project/Idea inspection is exclusive to Backlot Lab"
        }
        // Never use CreatorDataGate.readyTransaction here: that method may
        // repair pending restores and is explicitly a *writer* pathway.
        // Plain transaction serializes with Creator writers and denies pending
        // combined restore without importing, repairing or mutating records.
        return CreatorDataGate.transaction {
            val before = CreatorDataGate.generation(app)
            val projects = taskStore.tasksFlow.first()
            val ideas = ideaStore.ideasFlow.first()
            require(projects.size.toLong() + ideas.size.toLong() <= 500L) {
                "Large local collection needs paginated read-only readiness analysis"
            }
            val reasons = mutableMapOf<Ux09CanonicalReadinessPolicy.Blocker, Int>()
            var projectReady = 0
            var ideaReady = 0
            projects.forEach { task ->
                val result = Ux09CanonicalReadinessPolicy.inspectProject(
                    task, taskStore.encodeRecoveryTask(task),
                    taskStore::decodeRecoveryTask,
                )
                when (result) {
                    is Ux09CanonicalReadinessPolicy.Result.Ready -> projectReady++
                    is Ux09CanonicalReadinessPolicy.Result.Blocked ->
                        reasons.merge(result.reason, 1, Int::plus)
                }
            }
            ideas.forEach { idea ->
                val result = Ux09CanonicalReadinessPolicy.inspectIdea(
                    idea, ideaStore.encodeRecoveryIdea(idea),
                    ideaStore::decodeRecoveryIdea,
                )
                when (result) {
                    is Ux09CanonicalReadinessPolicy.Result.Ready -> ideaReady++
                    is Ux09CanonicalReadinessPolicy.Result.Blocked ->
                        reasons.merge(result.reason, 1, Int::plus)
                }
            }
            val after = CreatorDataGate.generation(app)
            check(before == after) {
                "Restore generation changed during read-only readiness inspection"
            }
            Summary(before, projects.size, ideas.size, projectReady, ideaReady,
                reasons.toSortedMap(compareBy { it.name }))
        }
    }
}
