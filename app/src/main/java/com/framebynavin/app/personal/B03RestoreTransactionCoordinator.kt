package com.framebynavin.app.personal

import android.content.Context
import com.framebynavin.app.personal.spaces.V289SpacesDatabase
import java.io.File
import java.io.InputStream
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Phase B3's first real, journaled multi-store additive writer.
 *
 * Not called from screens/Cloud Sync/alarms. It deliberately leaves a
 * VERIFYING journal even after a fully successful write: B4 must implement
 * independent authenticated recovery, finalization and owner sign-off.
 * A process death anywhere after PREPARED blocks ordinary writes.
 *
 * The incoming V300 bytes and independent V305 escrow are reauthenticated
 * while the same Phase A global lock excludes all normal domain mutations.
 * No caller-provided B01 metadata alone can trigger this transaction.
 */
internal class B03RestoreTransactionCoordinator(context: Context) {
    private val app = context.applicationContext
    private val reader = V301LocalRecoveryReader(app)
    private val preparation = V304RestorePreparationStore(
        File(app.filesDir, "backlot-personal-restore-preparation-v1"),
    )
    private val escrow = V305RollbackEscrowStore(
        File(app.filesDir, "backlot-personal-rollback-escrow-v1"),
    )
    private val journalRoot = StabilizationPersonalMutationGate.journalRootFor(app)

    suspend fun writeForOwnerApprovedRecovery(
        incomingArchive: InputStream,
        incomingPassword: CharArray,
        preparedId: String,
        escrowId: String,
        escrowPassword: CharArray,
        ownerRequested: Boolean,
        acknowledgedNonPortableState: Boolean,
    ): B03RestoreExecution = withContext(Dispatchers.IO) {
        require(ownerRequested) { "Explicit owner restore consent is required" }
        require(acknowledgedNonPortableState) {
            "Device alarm leases, custom songs and local URI access are not portable"
        }
        StabilizationPersonalMutationGate.withOwnerMutation(app) {
            val original = reader.read()
            val prep = preparation.inspect(preparedId)
            val verifiedEscrow = escrow.verify(escrowId) { stream ->
                V300EncryptedPersonalArchive.stageAndVerify(
                    stream, escrowPassword, app.cacheDir,
                ).use { old ->
                    V305RollbackMatchPolicy.requireSameSnapshot(
                        original, old.components, old.photos.map { it.photo },
                    )
                }
            }
            V300EncryptedPersonalArchive.stageAndVerify(
                incomingArchive, incomingPassword, app.cacheDir,
            ).use { authenticated ->
                val barrier = V305CommitBarrierPolicy.check(prep, verifiedEscrow, original)
                val target = StabilizationRestoreTargetPolicy.calculate(
                    original, authenticated.components,
                    authenticated.photos.map { it.photo },
                )
                val evidence = B01RestorePrecommitPolicy.inspect(
                    B01RestorePrecommitEvidence(
                        prepared = prep,
                        escrow = verifiedEscrow,
                        barrier = barrier,
                        liveLocal = original,
                        authenticatedIncoming = authenticated.components,
                        authenticatedIncomingPhotos = authenticated.photos.map { it.photo },
                        proposedTarget = target,
                        ownerRequested = true,
                        acknowledgedNonPortableState = true,
                    )
                )
                require(evidence.targetDigest == target.targetDigest &&
                    evidence.preparedId == preparedId && evidence.escrowId == escrowId) {
                    "Restore authorization identities changed"
                }
                // All checks and all WRITE steps share the same exclusive mutex.
                val journal = StabilizationRestoreWriteAheadJournal(journalRoot)
                val prepared = journal.prepare(
                    barrier = barrier,
                    targetDigest = target.targetDigest,
                    ownerApproved = true,
                    nowMillis = System.currentTimeMillis(),
                )
                StabilizationPersonalMutationGate.withPreparedRecovery(
                    journalRoot, prepared,
                ) {
                    suspend fun phase(
                        before: StabilizationRestoreWriteStep,
                        entered: StabilizationRestoreWriteStep,
                        finished: StabilizationRestoreWriteStep,
                        write: suspend () -> Unit,
                    ) {
                        journal.checkpoint(before, entered, System.currentTimeMillis())
                        write()
                        journal.checkpoint(entered, finished, System.currentTimeMillis())
                    }

                    phase(
                        StabilizationRestoreWriteStep.PREPARED,
                        StabilizationRestoreWriteStep.BEFORE_PERSONAL,
                        StabilizationRestoreWriteStep.PERSONAL_WRITTEN,
                    ) {
                        PersonalReminderStore(app).replaceFromApprovedImport(
                            target.merged.personal,
                        )
                    }
                    phase(
                        StabilizationRestoreWriteStep.PERSONAL_WRITTEN,
                        StabilizationRestoreWriteStep.BEFORE_CHALLENGES,
                        StabilizationRestoreWriteStep.CHALLENGES_WRITTEN,
                    ) {
                        V297ChallengeStore(app).replaceFromApprovedImport(
                            target.merged.challenges,
                        )
                    }
                    phase(
                        StabilizationRestoreWriteStep.CHALLENGES_WRITTEN,
                        StabilizationRestoreWriteStep.BEFORE_GROUPS,
                        StabilizationRestoreWriteStep.GROUPS_WRITTEN,
                    ) {
                        V298ReminderGroupStore(app).importMissingAfterApproval(
                            target.merged.groups,
                        )
                    }

                    val room = B03RestoreDomainWriter(app)
                    phase(
                        StabilizationRestoreWriteStep.GROUPS_WRITTEN,
                        StabilizationRestoreWriteStep.BEFORE_SPACES,
                        StabilizationRestoreWriteStep.SPACES_WRITTEN,
                    ) {
                        room.writeSpaces(target.merged)
                    }
                    phase(
                        StabilizationRestoreWriteStep.SPACES_WRITTEN,
                        StabilizationRestoreWriteStep.BEFORE_PHOTOS,
                        StabilizationRestoreWriteStep.PHOTOS_WRITTEN,
                    ) {
                        room.writePhotos(
                            target.photos, original.photos, authenticated.photos,
                        )
                    }
                    journal.checkpoint(
                        StabilizationRestoreWriteStep.PHOTOS_WRITTEN,
                        StabilizationRestoreWriteStep.VERIFYING,
                        System.currentTimeMillis(),
                    )
                    val actual = reader.read()
                    require(actual.localIntegrityVerified &&
                        V304RestorePlanPolicy.fingerprint(
                            actual.components, actual.photos,
                        ) == target.targetDigest) {
                        "Post-write Personal data differs from the authenticated target"
                    }
                    B03RestoreExecution(
                        transactionId = prepared.transactionId,
                        escrowId = verifiedEscrow.id,
                        expectedDigest = target.targetDigest,
                        actualDigest = V304RestorePlanPolicy.fingerprint(
                            actual.components, actual.photos,
                        ),
                        stage = StabilizationRestoreWriteStep.VERIFYING,
                    )
                }
            }
        }
    }
}

/** Success here means staged data matches; journal remains for B4 recovery. */
internal data class B03RestoreExecution(
    val transactionId: String,
    val escrowId: String,
    val expectedDigest: String,
    val actualDigest: String,
    val stage: StabilizationRestoreWriteStep,
) {
    val ownerRestoreFinished: Boolean get() = false
}
