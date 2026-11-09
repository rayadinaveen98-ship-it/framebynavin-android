package com.framebynavin.app.cloud

import android.content.Context
import com.framebynavin.app.data.CreatorBackupManager
import com.framebynavin.app.data.CreatorDataGate
import com.framebynavin.app.data.IdeaVaultStore
import com.framebynavin.app.data.PortableCreatorBackupManager
import com.framebynavin.app.data.TaskStore
import com.framebynavin.app.personal.Phase1V2OwnerRestoreCoordinator
import java.io.File
import kotlinx.coroutines.flow.first

/**
 * Explicit owner-controlled unified recovery for a NEW / empty Creator device.
 *
 * Creator's native ZIP importer replaces its workspace, so this path refuses
 * every phone with ANY existing local Creator work (including hidden weekly,
 * focus and publishing history). Personal uses its already-journaled additive
 * v2 importer, preserving existing receipts and refusing every ID collision.
 *
 * Before either native writer starts, an independently verified ENCRYPTED
 * snapshot of both domains is retained. A durable outer journal records
 * handoff states; a crash never silently replays or clears those records.
 * Native Creator/Personal importers retain their own escrow/rollback journals.
 *
 * Only owner-triggered, never run at startup, by Sync Now or a WorkManager job.
 */
internal class TrustUnifiedRestoreCoordinator(
    context: Context,
    private val reviewedCloud: TrustOwnerCloudReview =
        TrustOwnerCloudReview(context),
    private val localCopies: TrustOwnerEncryptedLocalCopy =
        TrustOwnerEncryptedLocalCopy(context),
) {
    private val app = context.applicationContext

    internal data class Finished(
        val ownerId: String,
        val cloudRevision: Long,
        val projectCount: Int,
        val ideaCount: Int,
        val personalAdditions: Int,
        val preferenceFields: Int,
        val escrowCopyId: String,
        val nativeJournalsFinalized: Boolean,
    )

    /**
     * Owner-controlled continuation after process death or a failed second
     * domain. Never repeat a Creator import or override edits made since the
     * first successful Creator commit.
     *
     * Native Personal v2 remains the authority for an interrupted Personal
     * journal. We do not guess completion if its native checkpoint vanished:
     * stop for explicit recovery inspection instead.
     */
    suspend fun resumeOwnerApproved(
        selected: TrustOwnerCloudReview.SourceCoverageReview,
        trustPassword: CharArray,
        escrowPassword: CharArray,
        exactConfirmation: String,
        acknowledgedNonPortableAlarmsAndMedia: Boolean,
    ): Finished {
        require(exactConfirmation == "RESUME VERIFIED RESTORE" &&
            acknowledgedNonPortableAlarmsAndMedia &&
            trustPassword.size in 12..512 &&
            escrowPassword.size in 12..512) {
            "Fresh owner consent and both encrypted source passwords required"
        }
        val pending = checkNotNull(TrustUnifiedRestoreJournal.inspect(app)) {
            "There is no interrupted Creator/Personal restore"
        }
        check(selected.ownerId == pending.ownerId &&
            selected.remoteRevision == pending.remoteRevision &&
            selected.encryptedLocalReviewId == pending.reviewedSourceId &&
            pending.stage.ordinal >=
                TrustUnifiedRestoreJournal.Stage.CREATOR_COMMITTED.ordinal) {
            "An unconfirmed Creator import needs manual native recovery first"
        }
        val savedEscrow = TrustOwnerCopyCatalog.inspect(
            File(app.noBackupFilesDir, "trust-fc13-owner-encrypted-v1"),
            pending.ownerId,
        ).receipts.singleOrNull {
            it.copyId == pending.encryptedEscrowCopyId
        } ?: error("Original encrypted phone escrow is missing")
        val original = File(
            app.noBackupFilesDir,
            "trust-fc13-owner-encrypted-v1/verified/copy-${savedEscrow.copyId}.enc",
        )
        check(original.isFile &&
            original.length() == savedEscrow.encryptedBytes) {
            "The original encrypted phone snapshot must be retained"
        }
        // Explicitly reauthenticate the ORIGINAL before resuming any write.
        original.inputStream().buffered().use { input ->
            TrustEncryptedStagingCodec.stageAndVerify(
                pending.ownerId, escrowPassword, input, app.cacheDir,
            ).use { originalSource ->
                val verified = TrustRestorableSourceReview.inspect(app, originalSource)
                check(verified.missingRequiredSources.isEmpty())
            }
        }

        val installed = reviewedCloud.withPendingReviewedRestoreSource(
            selected, pending, trustPassword,
        ) { verified, head ->
            val personal = Phase1V2OwnerRestoreCoordinator(app)
            check(!CreatorBackupManager(
                app, readOnlySourceValidation = true,
            ).hasPendingRestore()) {
                "Creator's native rollback journal needs recovery before a unified resume"
            }
            check(CreatorDataGate.generation(app) ==
                pending.committedCreatorGeneration) {
                "Creator work changed after the interrupted restore. No replay is allowed"
            }
            var current = pending
            var newlyImported = 0
            if (current.stage == TrustUnifiedRestoreJournal.Stage.CREATOR_COMMITTED) {
                check(!personal.hasPendingOwnerRecovery()) {
                    "The native Personal journal must be resumed first"
                }
                val incomingDiff = TrustPersonalRestoreBridge(app).previewAdditions(verified)
                check(incomingDiff.conflicts == 0) {
                    "Personal records changed since the interrupted restore"
                }
                val candidate = TrustPersonalRestoreBridge(app)
                    .prepareEncryptedV2Candidate(verified, escrowPassword)
                try {
                    current = TrustUnifiedRestoreJournal.advance(
                        app, current,
                        TrustUnifiedRestoreJournal.Stage.PERSONAL_STARTED,
                    )
                    val finished = candidate.inputStream().buffered().use { input ->
                        personal.restoreOwnerApproved(
                            selectedArchive = input,
                            selectedPassphrase = escrowPassword,
                            escrowPassphrase = escrowPassword,
                            ownerConfirmedAdditions = true,
                            acknowledgedNonPortableAlarmsAndSongs =
                                acknowledgedNonPortableAlarmsAndMedia,
                        )
                    }
                    check(finished.completedAndJournalArchived &&
                        finished.importedNewRecords == incomingDiff.proposedAdditions)
                    newlyImported = finished.importedNewRecords
                    current = TrustUnifiedRestoreJournal.advance(
                        app, current,
                        TrustUnifiedRestoreJournal.Stage.PERSONAL_COMMITTED,
                    )
                } finally {
                    check(candidate.delete() || !candidate.exists()) {
                        "Private Personal resume candidate cleanup needs attention"
                    }
                }
            } else if (current.stage ==
                TrustUnifiedRestoreJournal.Stage.PERSONAL_STARTED) {
                check(personal.hasPendingOwnerRecovery()) {
                    "Personal import may have already completed. Do not replay " +
                        "without native journal forensics and owner review"
                }
                val resumed = personal.resumeOwnerApproved(
                    escrowPassphrase = escrowPassword,
                    selectedPassphrase = escrowPassword,
                    ownerConfirmedResume = true,
                )
                check(resumed.completedAndJournalArchived)
                newlyImported = resumed.importedNewRecords
                current = TrustUnifiedRestoreJournal.advance(
                    app, current,
                    TrustUnifiedRestoreJournal.Stage.PERSONAL_COMMITTED,
                )
            }
            check(!personal.hasPendingOwnerRecovery()) {
                "Personal's native recovery journal is still unresolved"
            }
            if (current.stage ==
                TrustUnifiedRestoreJournal.Stage.PERSONAL_COMMITTED) {
                current = TrustUnifiedRestoreJournal.advance(
                    app, current,
                    TrustUnifiedRestoreJournal.Stage.PREFERENCES_STARTED,
                )
            }
            var preferences = 0
            if (current.stage ==
                TrustUnifiedRestoreJournal.Stage.PREFERENCES_STARTED) {
                val source = verified.parts.single {
                    it.id == "creator/config/persisted"
                }
                preferences = TrustPortablePreferenceRestore.restoreOwnerApproved(
                    app, source.file.readText(Charsets.UTF_8),
                    ownerApproved = true,
                ).writtenFields
                current = TrustUnifiedRestoreJournal.advance(
                    app, current,
                    TrustUnifiedRestoreJournal.Stage.PREFERENCES_COMMITTED,
                )
            }
            check(current.stage ==
                TrustUnifiedRestoreJournal.Stage.PREFERENCES_COMMITTED &&
                CreatorDataGate.generation(app) == current.committedCreatorGeneration)
            val projects = TaskStore(app).tasksFlow.first()
            val ideas = IdeaVaultStore(app).ideasFlow.first()
            current to Finished(
                ownerId = pending.ownerId,
                cloudRevision = head.revision,
                projectCount = projects.size,
                ideaCount = ideas.size,
                personalAdditions = newlyImported,
                preferenceFields = preferences,
                escrowCopyId = pending.encryptedEscrowCopyId,
                nativeJournalsFinalized = true,
            )
        }
        check(CloudSyncManager(app).localState().session?.userId == pending.ownerId &&
            CreatorDataGate.generation(app) ==
                installed.first.committedCreatorGeneration) {
            "Owner or Creator generation changed during recovery"
        }
        TrustUnifiedRestoreJournal.finish(
            app, installed.first,
            creatorVerified = true, personalVerified = true,
            ownerApproved = true,
        )
        return installed.second
    }

    suspend fun restoreOwnerApprovedOnFreshDevice(
        selected: TrustOwnerCloudReview.SourceCoverageReview,
        reviewed: TrustOwnerCloudReview.UnifiedRestoreReview,
        trustPassword: CharArray,
        escrowPassword: CharArray,
        exactConfirmation: String,
        acknowledgedNonPortableAlarmsAndMedia: Boolean,
    ): Finished {
        require(exactConfirmation == "RESTORE CREATOR AND PERSONAL" &&
            acknowledgedNonPortableAlarmsAndMedia) {
            "Owner must approve fresh-device recovery and device-only limitations"
        }
        require(trustPassword.size in 12..512 && escrowPassword.size in 12..512)
        check(selected.ownerId == reviewed.ownerId &&
            selected.remoteRevision == reviewed.remoteRevision &&
            reviewed.uncoveredPreferenceKeys.isEmpty() &&
            !reviewed.creator.hasExistingOwnerWork &&
            reviewed.creator.projectIdConflicts == 0 &&
            reviewed.creator.ideaIdConflicts == 0 &&
            reviewed.personal.conflicts == 0 &&
            TrustUnifiedRestoreJournal.inspect(app) == null) {
            "Existing owner work, conflicting IDs, or an interrupted restore block import"
        }
        // A fresh encrypted source escrow is created before live Creator or
        // Personal mutation. It is NEVER overwritten or erased on failure.
        val escrow = localCopies.createAfterOwnerRequest(escrowPassword)
        check(escrow.ownerId == selected.ownerId &&
            escrow.encryptedBytes > 0 && escrow.verifiedSources > 0) {
            "A verified same-account pre-restore escrow is required"
        }

        // This callback keeps the current cloud head, signed-in account and
        // decrypted owner parts valid while native importers do their work.
        val installed = reviewedCloud.withSameReviewedRestoreSource(
            selected, reviewed, trustPassword,
        ) { staged, head, currentDiff ->
            val creatorBridge = TrustCreatorRestoreBridge(app)
            val personalBridge = TrustPersonalRestoreBridge(app)
            val creatorCandidate = creatorBridge.preparePortableCandidateAfterOwnerApproval(
                staged, ownerApprovedPreparation = true,
            )
            var personalCandidate: File? = null
            try {
                personalCandidate = personalBridge.prepareEncryptedV2Candidate(
                    staged, escrowPassword,
                )
                val liveCreator = creatorBridge.preview(staged)
                val livePersonal = personalBridge.previewAdditions(staged)
                check(liveCreator == currentDiff.creator &&
                    livePersonal == currentDiff.personal &&
                    !liveCreator.hasExistingOwnerWork &&
                    liveCreator.missingCreatorSources.isEmpty() &&
                    livePersonal.conflicts == 0) {
                    "Phone data changed after backup was approved"
                }
                val before = CreatorDataGate.generation(app)
                var checkpoint = TrustUnifiedRestoreJournal.begin(
                    context = app,
                    ownerId = selected.ownerId,
                    remoteRevision = head.revision,
                    encryptedSourceSha256 = head.encryptedSha256,
                    reviewedSourceId = selected.encryptedLocalReviewId,
                    encryptedEscrowCopyId = escrow.copyId,
                    originalCreatorGeneration = before,
                )

                // Native Creator retains its own original recovery copy and
                // rolls back a failed import before this outer state can
                // advance to CREATOR_COMMITTED.
                checkpoint = TrustUnifiedRestoreJournal.advance(
                    app, checkpoint, TrustUnifiedRestoreJournal.Stage.CREATOR_STARTED,
                )
                check(CreatorDataGate.generation(app) == before) {
                    "Creator content changed before native restore"
                }
                val creatorNative = creatorCandidate.inputStream().buffered().use {
                    PortableCreatorBackupManager(app).restore(it)
                }
                val creatorGeneration = CreatorDataGate.generation(app)
                check(creatorNative.projectCount == liveCreator.incomingProjects &&
                    creatorNative.ideaCount == liveCreator.incomingIdeas) {
                    "The native Creator importer did not report exact incoming counts"
                }
                checkpoint = TrustUnifiedRestoreJournal.advance(
                    app, checkpoint,
                    TrustUnifiedRestoreJournal.Stage.CREATOR_COMMITTED,
                    committedCreatorGeneration = creatorGeneration,
                )

                // Personal v2 independently checks every challenge/Space
                // receipt, journals exact pre-import data, and only ADDS
                // records with nonconflicting IDs.
                checkpoint = TrustUnifiedRestoreJournal.advance(
                    app, checkpoint, TrustUnifiedRestoreJournal.Stage.PERSONAL_STARTED,
                )
                val personalNative = checkNotNull(personalCandidate)
                    .inputStream().buffered().use { input ->
                        Phase1V2OwnerRestoreCoordinator(app).restoreOwnerApproved(
                            selectedArchive = input,
                            selectedPassphrase = escrowPassword,
                            escrowPassphrase = escrowPassword,
                            ownerConfirmedAdditions = true,
                            acknowledgedNonPortableAlarmsAndSongs =
                                acknowledgedNonPortableAlarmsAndMedia,
                        )
                    }
                check(personalNative.completedAndJournalArchived &&
                    personalNative.importedNewRecords ==
                        livePersonal.proposedAdditions) {
                    "Personal v2 original recovery journal is incomplete"
                }
                checkpoint = TrustUnifiedRestoreJournal.advance(
                    app, checkpoint, TrustUnifiedRestoreJournal.Stage.PERSONAL_COMMITTED,
                )

                // The Creator native importer handles Creator settings;
                // restore only the other approved appearance/Spark keys.
                checkpoint = TrustUnifiedRestoreJournal.advance(
                    app, checkpoint, TrustUnifiedRestoreJournal.Stage.PREFERENCES_STARTED,
                )
                val preferencePart = staged.parts.single {
                    it.id == "creator/config/persisted"
                }
                val preferenceNative = TrustPortablePreferenceRestore.restoreOwnerApproved(
                    app,
                    preferencePart.file.readText(Charsets.UTF_8),
                    ownerApproved = true,
                )
                checkpoint = TrustUnifiedRestoreJournal.advance(
                    app, checkpoint, TrustUnifiedRestoreJournal.Stage.PREFERENCES_COMMITTED,
                )

                // Independent post-import observations. Native ZIP and v2
                // writers already verify their own checksum and recovery
                // receipts; here prove the two domains were both installed
                // under the unchanged Creator generation.
                val creatorProjects = TaskStore(app).tasksFlow.first()
                val creatorIdeas = IdeaVaultStore(app).ideasFlow.first()
                check(creatorProjects.size == liveCreator.incomingProjects &&
                    creatorIdeas.size == liveCreator.incomingIdeas &&
                    CreatorDataGate.generation(app) == creatorGeneration) {
                    "Creator data changed during the Personal portion of the restore"
                }
                Pair(
                    checkpoint,
                    Finished(
                        ownerId = selected.ownerId,
                        cloudRevision = head.revision,
                        projectCount = creatorProjects.size,
                        ideaCount = creatorIdeas.size,
                        personalAdditions = personalNative.importedNewRecords,
                        preferenceFields = preferenceNative.writtenFields,
                        escrowCopyId = escrow.copyId,
                        nativeJournalsFinalized = true,
                    ),
                )
            } finally {
                // Never retain an unencrypted Creator ZIP beyond the owner's
                // single transaction. Personal candidate is independently
                // encrypted, but also disposable after native v2 journaling.
                listOfNotNull(creatorCandidate, personalCandidate).forEach { candidate ->
                    check(candidate.delete() || !candidate.exists()) {
                        "Sensitive restore candidate cleanup needs attention"
                    }
                }
            }
        }
        // The cloud AccountGate generation check completed when the callback
        // returned. A late sign-out or Creator edit must never be success.
        check(CloudSyncManager(app).localState().session?.userId == selected.ownerId &&
            CreatorDataGate.generation(app) ==
                installed.first.committedCreatorGeneration) {
            "Owner or Creator data changed after native import. Preserve recovery journal"
        }
        TrustUnifiedRestoreJournal.finish(
            app, installed.first,
            creatorVerified = true, personalVerified = true, ownerApproved = true,
        )
        return installed.second
    }
}
