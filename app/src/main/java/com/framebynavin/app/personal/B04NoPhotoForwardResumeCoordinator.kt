package com.framebynavin.app.personal

import android.content.Context
import java.io.File
import java.io.InputStream
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * First REAL owner-mediated B4 FORWARD resume, deliberately limited to
 * restorations without ANY existing or incoming private JPEGs.
 *
 * The no-photo limit is a hard safety boundary: interrupted photo copies and
 * pending JPEG files cannot yet be replayed without forensic owner repair.
 * This method never runs at app startup and is not linked from any screen.
 * Even after a verified resume, an active VERIFYING journal remains until
 * separately authenticated owner finalization archives it.
 */
internal class B04NoPhotoForwardResumeCoordinator(context: Context) {
    private val app = context.applicationContext
    private val journalRoot = StabilizationPersonalMutationGate.journalRootFor(app)
    private val escrowStore = V305RollbackEscrowStore(
        File(app.filesDir, "backlot-personal-rollback-escrow-v1"),
    )

    suspend fun resumeOwnerApproved(
        incomingArchive: InputStream,
        incomingPassword: CharArray,
        escrowPassphrase: CharArray,
        consent: B04OwnerRecoveryConsent,
    ): B03RestoreExecution = withContext(Dispatchers.IO) {
        require(consent.selectedChoice == B04OwnerRecoveryChoice.RESUME_AUTHENTICATED_TARGET &&
            consent.ownerConfirmed && consent.acknowledgedDeviceLocalLimitations) {
            "Owner must separately request and confirm forward replay"
        }
        StabilizationPersonalMutationGate.withPendingRecoveryInspection(app) { journal ->
            require(journal.escrowId == consent.escrowId) {
                "Wrong retained rollback copy selected"
            }
            lateinit var original: V300PersonalComponents
            var originalPhotos = emptyList<com.framebynavin.app.personal.spaces.V290SpacePhotoEntity>()
            val verifiedEscrow = escrowStore.verify(journal.escrowId) { encrypted ->
                V300EncryptedPersonalArchive.stageAndVerify(
                    encrypted, escrowPassphrase, app.cacheDir,
                ).use { stage ->
                    original = stage.components
                    originalPhotos = stage.photos.map { it.photo }
                    require(V304RestorePlanPolicy.fingerprint(original, originalPhotos) ==
                        journal.originalDigest) {
                        "Owner's escrow does not match this interrupted transaction"
                    }
                }
            }
            require(verifiedEscrow.baselineDigest == journal.originalDigest) {
                "Independent escrow mismatch"
            }
            V300EncryptedPersonalArchive.stageAndVerify(
                incomingArchive, incomingPassword, app.cacheDir,
            ).use { stage ->
                val proposed = StabilizationRestoreTargetPolicy.calculate(
                    V301LocalRecoverySnapshot(original, originalPhotos, true),
                    stage.components, stage.photos.map { it.photo },
                )
                require(proposed.targetDigest == journal.targetDigest) {
                    "Owner's imported archive is different from the interrupted target"
                }
                // First cut is strict: no owner photos, imported photos or
                // untracked JPEGs; later photo repair needs separate B5 proof.
                require(originalPhotos.isEmpty() && proposed.photos.isEmpty() &&
                    stage.photos.isEmpty()) {
                    "Private-photo crash repair is not yet safely supported"
                }
                val local = V301LocalRecoveryReader(app).readForInterruptedRecovery()
                require(local.photos.isEmpty()) { "Unexpected Room photo record" }
                val domainProof = B04RecoveryDomainCheckpointPolicy.inspect(
                    journal, original, proposed.merged, local.components,
                )
                val photoProof = B04PrivatePhotoForensicPolicy.inspect(
                    File(app.filesDir, "backlot-personal-space-photos"),
                    originalPhotos, proposed.photos, journal.step,
                )
                val replay = B04ForwardReplayPlanPolicy.propose(
                    journal, original, originalPhotos, proposed.merged,
                    proposed.photos, domainProof, photoProof, true,
                )
                val reviewedLocal = local.copy(localIntegrityVerified = true)
                val assessment = B04InterruptedRecoveryPolicy.assess(
                    journal, verifiedEscrow, reviewedLocal, true,
                )
                val decision = B04OwnerRecoveryConsentPolicy.evaluateVerifiedForwardPlan(
                    journal, assessment, consent, replay,
                )
                StabilizationPersonalMutationGate.withOwnerApprovedForwardRecovery(
                    journalRoot, journal, decision, replay,
                ) {
                    val writeAhead = StabilizationRestoreWriteAheadJournal(journalRoot)
                    suspend fun phase(
                        previous: StabilizationRestoreWriteStep,
                        before: StabilizationRestoreWriteStep,
                        after: StabilizationRestoreWriteStep,
                        write: suspend () -> Unit,
                    ) {
                        var liveStep = checkNotNull(writeAhead.inspectAfterRestart()).step
                        if (liveStep.ordinal > after.ordinal) return
                        if (liveStep == previous) {
                            writeAhead.checkpoint(previous, before, System.currentTimeMillis())
                            liveStep = before
                        }
                        check(liveStep == before) { "Unexpected interrupted checkpoint in $before" }
                        // The B04 domain classifier proved that an existing
                        // post-crash value is ORIGINAL or TARGET, never third data.
                        // All these store writers are idempotent on TARGET.
                        write()
                        writeAhead.checkpoint(before, after, System.currentTimeMillis())
                    }
                    phase(
                        StabilizationRestoreWriteStep.PREPARED,
                        StabilizationRestoreWriteStep.BEFORE_PERSONAL,
                        StabilizationRestoreWriteStep.PERSONAL_WRITTEN,
                    ) {
                        PersonalReminderStore(app).replaceFromApprovedImport(proposed.merged.personal)
                    }
                    phase(
                        StabilizationRestoreWriteStep.PERSONAL_WRITTEN,
                        StabilizationRestoreWriteStep.BEFORE_CHALLENGES,
                        StabilizationRestoreWriteStep.CHALLENGES_WRITTEN,
                    ) {
                        V297ChallengeStore(app).replaceFromApprovedImport(proposed.merged.challenges)
                    }
                    phase(
                        StabilizationRestoreWriteStep.CHALLENGES_WRITTEN,
                        StabilizationRestoreWriteStep.BEFORE_GROUPS,
                        StabilizationRestoreWriteStep.GROUPS_WRITTEN,
                    ) {
                        V298ReminderGroupStore(app).importMissingAfterApproval(proposed.merged.groups)
                    }
                    phase(
                        StabilizationRestoreWriteStep.GROUPS_WRITTEN,
                        StabilizationRestoreWriteStep.BEFORE_SPACES,
                        StabilizationRestoreWriteStep.SPACES_WRITTEN,
                    ) {
                        B03RestoreDomainWriter(app).writeSpaces(proposed.merged)
                    }
                    // No JPEGs exist in this proof-bound recovery. Still
                    // fsync every BEFORE/AFTER photo checkpoint for consistency.
                    phase(
                        StabilizationRestoreWriteStep.SPACES_WRITTEN,
                        StabilizationRestoreWriteStep.BEFORE_PHOTOS,
                        StabilizationRestoreWriteStep.PHOTOS_WRITTEN,
                    ) {
                        require(proposed.photos.isEmpty() && stage.photos.isEmpty())
                    }
                    val beforeVerify = checkNotNull(writeAhead.inspectAfterRestart()).step
                    if (beforeVerify == StabilizationRestoreWriteStep.PHOTOS_WRITTEN) {
                        writeAhead.checkpoint(
                            StabilizationRestoreWriteStep.PHOTOS_WRITTEN,
                            StabilizationRestoreWriteStep.VERIFYING,
                            System.currentTimeMillis(),
                        )
                    } else check(beforeVerify == StabilizationRestoreWriteStep.VERIFYING)
                    val actual = V301LocalRecoveryReader(app).read()
                    val digest = V304RestorePlanPolicy.fingerprint(
                        actual.components, actual.photos,
                    )
                    require(actual.localIntegrityVerified &&
                        digest == proposed.targetDigest) {
                        "Forward recovery failed full independent post-write verification"
                    }
                    B03RestoreExecution(
                        transactionId = journal.transactionId,
                        escrowId = journal.escrowId,
                        expectedDigest = proposed.targetDigest,
                        actualDigest = digest,
                        stage = StabilizationRestoreWriteStep.VERIFYING,
                    )
                }
            }
        }
    }
}
