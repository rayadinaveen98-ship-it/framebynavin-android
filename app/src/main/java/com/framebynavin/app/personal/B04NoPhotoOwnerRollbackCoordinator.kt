package com.framebynavin.app.personal

import android.content.Context
import java.io.File
import java.io.InputStream
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Real B4 owner-approved rollback of *additive* imported records with NO
 * private photos on either side. The original owner's existing record IDs,
 * reminder device leases and custom media URI fields are retained verbatim
 * in the live DataStore; imported IDs alone are removed.
 *
 * A separate fsynced reverse action journal makes repeated attempts
 * idempotent after death at any BEFORE/AFTER rollback checkpoint. The original
 * forward journal remains active to freeze ordinary UI writes until the owner
 * separately confirms that the fully verified rollback may be released.
 * No production screen or startup handler invokes this code.
 */
internal class B04NoPhotoOwnerRollbackCoordinator(context: Context) {
    private val app = context.applicationContext
    private val forwardRoot = StabilizationPersonalMutationGate.journalRootFor(app)
    private val escrowStore = V305RollbackEscrowStore(
        File(app.filesDir, "backlot-personal-rollback-escrow-v1"),
    )
    private val rollbackRoot = File(
        app.filesDir, "backlot-personal-no-photo-rollback-actions-v1",
    )

    suspend fun rollbackOwnerApproved(
        importedArchive: InputStream,
        importedPassphrase: CharArray,
        escrowPassphrase: CharArray,
        consent: B04OwnerRecoveryConsent,
    ): B04NoPhotoRollbackExecution = withContext(Dispatchers.IO) {
        require(consent.selectedChoice ==
            B04OwnerRecoveryChoice.ROLLBACK_TO_AUTHENTICATED_ESCROW &&
            consent.ownerConfirmed && consent.acknowledgedDeviceLocalLimitations) {
            "Owner must separately approve encrypted rollback and device-local limitations"
        }
        StabilizationPersonalMutationGate.withPendingRecoveryInspection(app) { forward ->
            require(forward.escrowId == consent.escrowId) {
                "Selected recovery escrow differs from the active owner journal"
            }
            lateinit var original: V300PersonalComponents
            val oldPhotos = ArrayList<com.framebynavin.app.personal.spaces.V290SpacePhotoEntity>()
            val escrow = escrowStore.verify(forward.escrowId) { encrypted ->
                V300EncryptedPersonalArchive.stageAndVerify(
                    encrypted, escrowPassphrase, app.cacheDir,
                ).use { verified ->
                    original = verified.components
                    oldPhotos += verified.photos.map { it.photo }
                    require(V304RestorePlanPolicy.fingerprint(original, oldPhotos) ==
                        forward.originalDigest) {
                        "Encrypted original escrow has different Personal data"
                    }
                }
            }
            require(escrow.baselineDigest == forward.originalDigest) {
                "Independent rollback escrow is not bound to this restore"
            }
            V300EncryptedPersonalArchive.stageAndVerify(
                importedArchive, importedPassphrase, app.cacheDir,
            ).use { incoming ->
                val target = StabilizationRestoreTargetPolicy.calculate(
                    V301LocalRecoverySnapshot(original, oldPhotos, true),
                    incoming.components, incoming.photos.map { it.photo },
                )
                require(target.targetDigest == forward.targetDigest) {
                    "Imported ZIP is not the archive originally approved for this restore"
                }
                val delta = B04NoPhotoRollbackDeltaPolicy.calculate(
                    forward, original, target.merged, oldPhotos, target.photos,
                )
                val local = V301LocalRecoveryReader(app).readForInterruptedRecovery()
                require(local.photos.isEmpty()) {
                    "Private photo records must be handled by separate reviewed recovery"
                }
                val photoReview = B04PrivatePhotoForensicPolicy.inspect(
                    File(app.filesDir, "backlot-personal-space-photos"),
                    oldPhotos, target.photos, forward.step,
                )
                require(!photoReview.demandsOwnerReview &&
                    photoReview.expectedOriginal == 0 &&
                    photoReview.expectedIncoming == 0 &&
                    photoReview.originalVerified == 0) {
                    "Private JPEG evidence is not safe for no-photo rollback"
                }
                val actionRoot = File(rollbackRoot, forward.transactionId)
                val reverseJournal = B04NoPhotoRollbackActionJournal(actionRoot)
                val prior = reverseJournal.inspect()
                if (prior == null) {
                    // Initial write requires proof of forward chronology; a
                    // repeated rollback uses its independent REVERSE journal.
                    B04RecoveryDomainCheckpointPolicy.inspect(
                        forward, original, target.merged, local.components,
                    )
                } else {
                    require(prior.transactionId == forward.transactionId &&
                        prior.escrowId == forward.escrowId &&
                        prior.forwardStepAtApproval == forward.step &&
                        prior.originalDigest == forward.originalDigest &&
                        prior.targetDigest == forward.targetDigest) {
                        "Rollback action journal does not match its original approval"
                    }
                    B04RecoveryDomainCheckpointPolicy.inspectRollback(
                        prior, original, target.merged, local.components,
                    )
                }
                // Each record remains ORIGINAL or TARGET; never invent a
                // healthy baseline from an unreadable SQLite/Datastore.
                val assessed = B04InterruptedRecoveryPolicy.assess(
                    forward, escrow, local.copy(localIntegrityVerified = true),
                    ownerRequested = true,
                )
                val decision = B04OwnerRecoveryConsentPolicy.evaluate(
                    forward, assessed, consent,
                )
                val actionRecord = prior ?: reverseJournal.prepare(
                    forward, ownerApproved = true, nowMillis = System.currentTimeMillis(),
                )
                StabilizationPersonalMutationGate.withOwnerApprovedRollbackRecovery(
                    forwardRoot, forward, decision, delta, actionRoot, actionRecord,
                ) {
                    suspend fun reverse(
                        previous: B04RollbackWriteStep,
                        before: B04RollbackWriteStep,
                        after: B04RollbackWriteStep,
                        write: suspend () -> Unit,
                    ) {
                        var current = checkNotNull(reverseJournal.inspect()).step
                        if (current.ordinal > after.ordinal) return
                        if (current == previous) {
                            reverseJournal.checkpoint(previous, before, System.currentTimeMillis())
                            current = before
                        }
                        check(current == before) {
                            "Interrupted rollback cannot skip phase $before"
                        }
                        write()
                        reverseJournal.checkpoint(before, after, System.currentTimeMillis())
                    }
                    reverse(
                        B04RollbackWriteStep.PREPARED,
                        B04RollbackWriteStep.BEFORE_SPACES,
                        B04RollbackWriteStep.SPACES_REVERTED,
                    ) {
                        B04NoPhotoRollbackDomainWriter(app).removeOnlyImportedSpaces(
                            target.merged, delta,
                        )
                    }
                    reverse(
                        B04RollbackWriteStep.SPACES_REVERTED,
                        B04RollbackWriteStep.BEFORE_GROUPS,
                        B04RollbackWriteStep.GROUPS_REVERTED,
                    ) {
                        V298ReminderGroupStore(app).mutate { current ->
                            current.filterNot { it.id in delta.newlyAddedGroupIds }
                        }
                    }
                    reverse(
                        B04RollbackWriteStep.GROUPS_REVERTED,
                        B04RollbackWriteStep.BEFORE_CHALLENGES,
                        B04RollbackWriteStep.CHALLENGES_REVERTED,
                    ) {
                        V297ChallengeStore(app).mutate { current ->
                            current.filterNot { it.id in delta.newlyAddedChallengeIds }
                        }
                    }
                    reverse(
                        B04RollbackWriteStep.CHALLENGES_REVERTED,
                        B04RollbackWriteStep.BEFORE_PERSONAL,
                        B04RollbackWriteStep.PERSONAL_REVERTED,
                    ) {
                        val store = PersonalReminderStore(app)
                        val existingHistory = store.loadCompletionHistory()
                        val retainedHistory = existingHistory.filterNot {
                            it.stableKey in delta.newlyAddedCompletionKeys
                        }
                        store.mutate(replacementHistory = retainedHistory) { current ->
                            current.filterNot { it.id in delta.newlyAddedReminderIds }
                        }
                    }
                    val currentStep = checkNotNull(reverseJournal.inspect()).step
                    if (currentStep == B04RollbackWriteStep.PERSONAL_REVERTED) {
                        reverseJournal.checkpoint(
                            B04RollbackWriteStep.PERSONAL_REVERTED,
                            B04RollbackWriteStep.VERIFYING,
                            System.currentTimeMillis(),
                        )
                    } else check(currentStep == B04RollbackWriteStep.VERIFYING ||
                        currentStep == B04RollbackWriteStep.VERIFIED)
                    val actual = V301LocalRecoveryReader(app).read()
                    val observed = V304RestorePlanPolicy.fingerprint(
                        actual.components, actual.photos,
                    )
                    require(actual.localIntegrityVerified &&
                        observed == forward.originalDigest) {
                        "Reverse restore did not recreate the exact owner baseline"
                    }
                    val state = checkNotNull(reverseJournal.inspect()).step
                    if (state == B04RollbackWriteStep.VERIFYING) {
                        reverseJournal.checkpoint(
                            B04RollbackWriteStep.VERIFYING,
                            B04RollbackWriteStep.VERIFIED,
                            System.currentTimeMillis(),
                        )
                    }
                    B04NoPhotoRollbackExecution(
                        transactionId = forward.transactionId,
                        escrowId = forward.escrowId,
                        expectedOriginalDigest = forward.originalDigest,
                        actualOriginalDigest = observed,
                        actionState = B04RollbackWriteStep.VERIFIED,
                    )
                }
            }
        }
    }
}

internal data class B04NoPhotoRollbackExecution(
    val transactionId: String,
    val escrowId: String,
    val expectedOriginalDigest: String,
    val actualOriginalDigest: String,
    val actionState: B04RollbackWriteStep,
) {
    /** Final release requires a separate owner-authenticated verification. */
    val ownerRollbackFinalized: Boolean get() = false
}
