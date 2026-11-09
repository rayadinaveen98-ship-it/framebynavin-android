package com.framebynavin.app.cloud

import android.content.Context
import com.framebynavin.app.data.CreatorDataGate
import com.framebynavin.app.data.IdeaCaptureType
import com.framebynavin.app.data.IdeaVaultStore
import com.framebynavin.app.data.TaskStore
import kotlinx.coroutines.flow.first
import org.json.JSONArray

/**
 * READ-ONLY Creator half of one Backlot owner restore review.
 *
 * Do not use CreatorBackupManager.restoreCloud directly on an occupied phone:
 * its legacy semantics REPLACE Creator data. The universal restore must build
 * one owner-approved replacement/merge plan and durable pre-restore escrow
 * across both domains. This preflight cannot install or delete anything.
 */
internal class TrustCreatorRestoreBridge(context: Context) {
    private val app = context.applicationContext

    internal data class Review(
        val incomingProjects: Int,
        val existingProjects: Int,
        val projectIdConflicts: Int,
        val incomingIdeas: Int,
        val existingIdeas: Int,
        val ideaIdConflicts: Int,
        val incomingVoiceOriginals: Int,
        val incomingBestFrameOriginals: Int,
        val missingCreatorSources: Set<String>,
    ) {
        val hasExistingOwnerWork: Boolean
            get() = existingProjects > 0 || existingIdeas > 0
        val canRestoreWithoutReplacingExistingWork: Boolean
            get() = missingCreatorSources.isEmpty() &&
                projectIdConflicts == 0 && ideaIdConflicts == 0 &&
                !hasExistingOwnerWork
        // There is no signed Creator-side full backup/rollback authority yet.
        // No caller may convert this dry-run into mutation permission.
        val grantsRestorePermission: Boolean get() = false
    }

    suspend fun preview(staged: TrustVerifiedStagedBundle): Review =
        CreatorDataGate.transaction {
            val originals = staged.parts.associateBy { it.id }
            check(originals.size == staged.parts.size)
            val core = setOf(
                "creator/projects", "creator/ideas",
                "creator/voice-index", "creator/best-frame-order",
                "creator/history/rewards", "creator/config/persisted",
                "creator/history/workflow", "creator/history/pulse",
                "creator/history/post-publish",
            )
            val missing = core - originals.keys
            if (missing.isNotEmpty()) {
                return@transaction Review(
                    0, 0, 0, 0, 0, 0, 0, 0, missing,
                )
            }
            fun source(id: String): String {
                val part = requireNotNull(originals[id])
                require(part.sizeBytes <= 24L * 1024 * 1024)
                return part.file.readText(Charsets.UTF_8)
            }
            val creator = TaskStore(app)
            val ideaStore = IdeaVaultStore(app)
            val incomingProjects = creator.inspectJsonForTrustReadOnly(
                source("creator/projects"),
            )
            val incomingIdeas = ideaStore.inspectJsonForTrustReadOnly(
                source("creator/ideas"),
            )
            val existingProjects = creator.tasksFlow.first()
            val existingIdeas = ideaStore.ideasFlow.first()
            val existingProjectIds = existingProjects.mapTo(HashSet()) { it.id }
            val existingIdeaIds = existingIdeas.mapTo(HashSet()) { it.id }
            val sourceVoiceIds = (0 until JSONArray(source("creator/voice-index")).length())
                .map { index ->
                    JSONArray(source("creator/voice-index"))
                        .getJSONObject(index).getString("ideaId")
                }.toSet()
            val expectedVoiceIds = incomingIdeas.filter {
                it.captureType == IdeaCaptureType.VOICE
            }.mapTo(HashSet()) { it.id }
            require(sourceVoiceIds == expectedVoiceIds) {
                "Creator Voice Idea originals do not match saved incoming ideas"
            }
            val sourceFrameCount = JSONArray(source("creator/best-frame-order")).length()
            Review(
                incomingProjects = incomingProjects.size,
                existingProjects = existingProjects.size,
                projectIdConflicts = incomingProjects.count { it.id in existingProjectIds },
                incomingIdeas = incomingIdeas.size,
                existingIdeas = existingIdeas.size,
                ideaIdConflicts = incomingIdeas.count { it.id in existingIdeaIds },
                incomingVoiceOriginals = expectedVoiceIds.size,
                incomingBestFrameOriginals = sourceFrameCount,
                missingCreatorSources = emptySet(),
            )
        }
}
