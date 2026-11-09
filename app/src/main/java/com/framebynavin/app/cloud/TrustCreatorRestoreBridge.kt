package com.framebynavin.app.cloud

import android.content.Context
import com.framebynavin.app.data.CreatorBackupManager
import com.framebynavin.app.data.PortableBackupArchive
import com.framebynavin.app.data.PortableBackupMedia
import com.framebynavin.app.data.PortableBestFrame
import com.framebynavin.app.data.PortableBestFrameMedia
import com.framebynavin.app.data.PortableCreatorBackupManager
import com.framebynavin.app.data.CreatorDataGate
import com.framebynavin.app.data.IdeaCaptureType
import com.framebynavin.app.data.IdeaVaultStore
import com.framebynavin.app.data.TaskStore
import kotlinx.coroutines.flow.first
import java.io.File
import org.json.JSONObject
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

    /**
     * Prepare exactly the existing Creator v10 portable ZIP, from *already
     * authenticated* owner Trust partitions rather than live Creator data.
     *
     * Only an explicit owner-approved restore preparation may call this;
     * Both native backup validators run in read-only mode, so preparing the
     * candidate neither seeds guided-tour state nor imports owner data.
     * Caller must delete the returned plaintext private temporary ZIP after
     * the existing Creator restore coordinator consumes or rejects it.
     */
    suspend fun preparePortableCandidateAfterOwnerApproval(
        staged: TrustVerifiedStagedBundle,
        ownerApprovedPreparation: Boolean,
    ): File = CreatorDataGate.transaction {
        require(ownerApprovedPreparation) {
            "Owner must explicitly approve private Creator restore preparation"
        }
        val verified = TrustRestorableSourceReview.inspect(app, staged)
        check(verified.missingRequiredSources.isEmpty() &&
            verified.uncoveredPreferenceKeys.isEmpty()) {
            "Missing Creator/Personal or unknown saved preference sources"
        }
        val indexed = staged.parts.associateBy { it.id }
        check(indexed.size == staged.parts.size) {
            "Duplicate authenticated Creator source partitions"
        }
        fun text(id: String): String {
            val part = requireNotNull(indexed[id]) {
                "Encrypted backup is missing Creator source: $id"
            }
            require(part.sizeBytes in 0..(24L * 1024 * 1024))
            return part.file.readText(Charsets.UTF_8)
        }

        val expectedParts = setOf(
            "creator/projects", "creator/ideas", "creator/focus",
            "creator/config/weekly-slots", "creator/history/workflow",
            "creator/history/pulse", "creator/history/post-publish",
            "creator/history/rewards", "creator/config/automation",
            "creator/history/youtube-links", "creator/history/youtube-milestones",
            "creator/history/youtube-checkpoints",
        )
        val settings = TrustCreatorSettingsTranslator.translate(
            text("creator/config/persisted"),
        )
        val ordered = JSONArray(text("creator/best-frame-order"))
        require(ordered.length() <= 10)
        val frames = (0 until ordered.length()).map { index ->
            val media = requireNotNull(indexed[
                "creator/best-frame-" + index.toString().padStart(4, '0')
            ]) { "Saved original Best Frame is missing" }
            PortableBestFrame(
                name = ordered.getString(index),
                sha256 = media.sha256,
                file = media.file,
            )
        }
        val preparedFrames = PortableBestFrameMedia.prepareFromVerifiedSourceFiles(frames)
        val signed = CreatorBackupManager(
            app, readOnlySourceValidation = true,
        ).assembleTrustedSourceEnvelope(
            raw = expectedParts.associateWith(::text),
            nativeSettings = settings.creatorSettingsJson,
            nativeEscalation = settings.smartEscalationJson,
            preparedFrames = preparedFrames,
        )

        val voiceManifest = JSONArray(text("creator/voice-index"))
        val media = (0 until voiceManifest.length()).map { index ->
            val row = voiceManifest.getJSONObject(index)
            val expectedId = "creator/voice-" + index.toString().padStart(4, '0')
            require(row.getString("partId") == expectedId)
            val source = requireNotNull(indexed[expectedId]) {
                "Saved Voice Idea original missing from encrypted source"
            }
            PortableBackupMedia(
                ideaId = row.getString("ideaId"),
                file = source.file,
            )
        }
        // Existing portable importer independently verifies every Voice Idea
        // mapping and frame hash before it writes or schedules anything.
        val output = File.createTempFile(
            "trust-creator-reviewed-", ".fbnbackup", app.cacheDir,
        )
        try {
            output.outputStream().buffered().use { destination ->
                PortableBackupArchive.write(
                    backupJson = signed,
                    media = media,
                    output = destination,
                    frames = preparedFrames.frames,
                )
            }
            require(output.length() in 1L..(1024L * 1024 * 1024))
            output.inputStream().buffered().use {
                PortableCreatorBackupManager(
                    app, readOnlyValidation = true,
                ).validate(it)
            }
            output
        } catch (failure: Throwable) {
            if (!output.delete() && output.exists()) {
                failure.addSuppressed(IllegalStateException(
                    "Could not erase the unapproved Creator restore candidate",
                ))
            }
            throw failure
        }
    }

}
