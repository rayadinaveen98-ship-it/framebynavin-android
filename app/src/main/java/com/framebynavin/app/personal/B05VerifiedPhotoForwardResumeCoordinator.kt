package com.framebynavin.app.personal

import android.content.Context
import java.io.File
import java.io.InputStream
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * B5: owner-controlled FORWARD recovery when JPEG bytes are all authenticated
 * and there is NO torn, modified, symlinked or untracked photo evidence.
 *
 * Supports the precise crash gap "JPEG file fsynced, matching Room photo row
 * not yet committed". B03's no-overwrite installer rechecks SHA-256 and adds
 * only a missing Room row. Missing clean files may be created from freshly
 * authenticated incoming archive bytes. Any .restore.pending or corrupt
 * partial file remains blocked for explicit forensic owner review.
 *
 * Never auto-runs at boot or in cloud sync. A successful replay leaves the
 * forward journal in VERIFYING until separately consented owner finalization.
 */
internal class B05VerifiedPhotoForwardResumeCoordinator(context: Context) {
    private val app = context.applicationContext
    private val journalRoot = StabilizationPersonalMutationGate.journalRootFor(app)
    private val originalEscrow = V305RollbackEscrowStore(
        File(app.filesDir, "backlot-personal-rollback-escrow-v1"),
    )
    private val privatePhotos = File(app.filesDir, "backlot-personal-space-photos")

    suspend fun resumeOwnerApproved(
        incomingArchive: InputStream,
        incomingPassword: CharArray,
        escrowPassword: CharArray,
        consent: B04OwnerRecoveryConsent,
    ): B03RestoreExecution = withContext(Dispatchers.IO) {
        require(consent.selectedChoice == B04OwnerRecoveryChoice.RESUME_AUTHENTICATED_TARGET &&
            consent.ownerConfirmed && consent.acknowledgedDeviceLocalLimitations) {
            "Owner must separately confirm forward photo recovery and portable media limitations"
        }
        StabilizationPersonalMutationGate.withPendingRecoveryInspection(app) { journal ->
            require(consent.escrowId == journal.escrowId) {
                "Retained encrypted rollback copy is not this transaction's escrow"
            }
            lateinit var original: V300PersonalComponents
            val oldPhotos =
                ArrayList<com.framebynavin.app.personal.spaces.V290SpacePhotoEntity>()
            val escrow = originalEscrow.verify(journal.escrowId) { stream ->
                V300EncryptedPersonalArchive.stageAndVerify(
                    stream, escrowPassword, app.cacheDir,
                ).use { verified ->
                    original = verified.components
                    oldPhotos += verified.photos.map { it.photo }
                    require(V304RestorePlanPolicy.fingerprint(original,oldPhotos) ==
                        journal.originalDigest) {
                        "Authenticated original photo escrow changed since the restore"
                    }
                }
            }
            require(escrow.baselineDigest == journal.originalDigest) {
                "Encrypted original baseline differs from the journal"
            }
            V300EncryptedPersonalArchive.stageAndVerify(
                incomingArchive, incomingPassword, app.cacheDir,
            ).use { incoming ->
                val target = StabilizationRestoreTargetPolicy.calculate(
                    V301LocalRecoverySnapshot(original,oldPhotos,true),
                    incoming.components,incoming.photos.map { it.photo },
                )
                require(target.targetDigest == journal.targetDigest) {
                    "Incoming photo archive is not the owner-approved restore target"
                }
                val local = V301LocalRecoveryReader(app).readForInterruptedRecovery()
                val stores = B04RecoveryDomainCheckpointPolicy.inspect(
                    journal,original,target.merged,local.components,
                )
                val forensic = B04PrivatePhotoForensicPolicy.inspect(
                    privatePhotos,oldPhotos,target.photos,journal.step,
                )
                require(!forensic.demandsOwnerReview &&
                    forensic.originalVerified == oldPhotos.size &&
                    forensic.expectedOriginal == oldPhotos.size) {
                    "Private JPEG forensics found corruption, untracked bytes or a pending copy"
                }
                val photoDetails = B05PhotoCrashEvidencePolicy.inspect(
                    privatePhotos,oldPhotos,target.photos,local.photos,
                )
                require(!photoDetails.hasUnsafeEvidence &&
                    !photoDetails.grantsPhotoFileWrite &&
                    !photoDetails.grantsRoomWrite &&
                    !photoDetails.permitsAutomaticPendingDeletion) {
                    "Private photo replay requires manual forensic owner review"
                }
                if (journal.step.ordinal >=
                    StabilizationRestoreWriteStep.PHOTOS_WRITTEN.ordinal) {
                    require(photoDetails.allImportedPhotosInstalled) {
                        "A completed photo checkpoint lacks matching Room rows or verified JPEGs"
                    }
                }
                val plan = B04ForwardReplayPlanPolicy.propose(
                    journal,original,oldPhotos,target.merged,target.photos,
                    stores,forensic,ownerRequested = true,
                )
                // The strict owner baseline+target Photo/JPEG checks above
                // replace the normal full-file inventory, which cannot pass
                // while the imported JPEG has no Room metadata row yet.
                val assessed = B04InterruptedRecoveryPolicy.assess(
                    journal,escrow,local.copy(localIntegrityVerified = true),
                    ownerRequested = true,
                )
                val decision = B04OwnerRecoveryConsentPolicy.evaluateVerifiedForwardPlan(
                    journal,assessed,consent,plan,
                )
                StabilizationPersonalMutationGate.withOwnerApprovedForwardRecovery(
                    journalRoot,journal,decision,plan,
                ) {
                    val writer = StabilizationRestoreWriteAheadJournal(journalRoot)
                    suspend fun phase(
                        previous: StabilizationRestoreWriteStep,
                        before: StabilizationRestoreWriteStep,
                        after: StabilizationRestoreWriteStep,
                        write: suspend () -> Unit,
                    ) {
                        var current = checkNotNull(writer.inspectAfterRestart()).step
                        if (current.ordinal > after.ordinal) return
                        if (current == previous) {
                            writer.checkpoint(previous,before,System.currentTimeMillis())
                            current = before
                        }
                        check(current == before) {
                            "Photo restore checkpoint does not match $before"
                        }
                        write()
                        writer.checkpoint(before,after,System.currentTimeMillis())
                    }
                    phase(
                        StabilizationRestoreWriteStep.PREPARED,
                        StabilizationRestoreWriteStep.BEFORE_PERSONAL,
                        StabilizationRestoreWriteStep.PERSONAL_WRITTEN,
                    ) {
                        PersonalReminderStore(app).replaceFromApprovedImport(target.merged.personal)
                    }
                    phase(
                        StabilizationRestoreWriteStep.PERSONAL_WRITTEN,
                        StabilizationRestoreWriteStep.BEFORE_CHALLENGES,
                        StabilizationRestoreWriteStep.CHALLENGES_WRITTEN,
                    ) {
                        V297ChallengeStore(app).replaceFromApprovedImport(target.merged.challenges)
                    }
                    phase(
                        StabilizationRestoreWriteStep.CHALLENGES_WRITTEN,
                        StabilizationRestoreWriteStep.BEFORE_GROUPS,
                        StabilizationRestoreWriteStep.GROUPS_WRITTEN,
                    ) {
                        V298ReminderGroupStore(app).importMissingAfterApproval(target.merged.groups)
                    }
                    phase(
                        StabilizationRestoreWriteStep.GROUPS_WRITTEN,
                        StabilizationRestoreWriteStep.BEFORE_SPACES,
                        StabilizationRestoreWriteStep.SPACES_WRITTEN,
                    ) {
                        B03RestoreDomainWriter(app).writeSpaces(target.merged)
                    }
                    phase(
                        StabilizationRestoreWriteStep.SPACES_WRITTEN,
                        StabilizationRestoreWriteStep.BEFORE_PHOTOS,
                        StabilizationRestoreWriteStep.PHOTOS_WRITTEN,
                    ) {
                        // CREATE_NEW only; a completely installed authenticated
                        // JPEG missing its Room row is reattached idempotently.
                        B03RestoreDomainWriter(app).writePhotos(
                            target.photos,oldPhotos,incoming.photos,
                        )
                    }
                    val beforeVerification = checkNotNull(writer.inspectAfterRestart()).step
                    if (beforeVerification == StabilizationRestoreWriteStep.PHOTOS_WRITTEN) {
                        writer.checkpoint(
                            StabilizationRestoreWriteStep.PHOTOS_WRITTEN,
                            StabilizationRestoreWriteStep.VERIFYING,
                            System.currentTimeMillis(),
                        )
                    } else check(beforeVerification == StabilizationRestoreWriteStep.VERIFYING)
                    val actual = V301LocalRecoveryReader(app).read()
                    val actualDigest = V304RestorePlanPolicy.fingerprint(
                        actual.components,actual.photos,
                    )
                    require(actual.localIntegrityVerified &&
                        actualDigest == journal.targetDigest) {
                        "Complete post-replay Personal/Room/JPEG inventory mismatched target"
                    }
                    B03RestoreExecution(
                        transactionId = journal.transactionId,
                        escrowId = journal.escrowId,
                        expectedDigest = journal.targetDigest,
                        actualDigest = actualDigest,
                        stage = StabilizationRestoreWriteStep.VERIFYING,
                    )
                }
            }
        }
    }
}
