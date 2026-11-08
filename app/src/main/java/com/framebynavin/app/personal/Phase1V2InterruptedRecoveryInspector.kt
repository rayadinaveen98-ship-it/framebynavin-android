package com.framebynavin.app.personal

import android.content.Context
import java.io.File
import java.io.InputStream
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Real device's interrupted v2 Personal state, checked under the same lock as
 * all owner DataStores and Room photo metadata. This is the repair INPUT, not
 * a repair or an import.
 */
internal data class Phase1V2InterruptedRecoveryEvidence(
    val checkpoint: Phase1V2RestoreCheckpoint,
    val domains: Phase1V2RestoreForensicAssessment,
    val photos: Phase1V2PhotoByteEvidence,
) {
    val mayRunWithoutOwnerApproval: Boolean get() = false
    val mayOverwriteOwnerData: Boolean get() = false
    val mayReleaseJournal: Boolean get() = false
    val mayAutoResume: Boolean get() = false
}

/**
 * Requires owner-supplied original escrow + intended encrypted target ZIPs,
 * including both passphrases. Never obtains a write permit, never installs
 * alarm leases and never deletes temporary or existing photo files.
 *
 * Any mismatch leaves the blocking v2 journal untouched for manual review.
 */
internal class Phase1V2InterruptedRecoveryInspector(context: Context) {
    private val app = context.applicationContext

    suspend fun inspect(
        escrowInput: InputStream,
        escrowPassword: CharArray,
        intendedInput: InputStream,
        intendedPassword: CharArray,
    ): Phase1V2InterruptedRecoveryEvidence =
        StabilizationPersonalMutationGate.withPendingV2RecoveryInspection(app) { journal ->
            withContext(Dispatchers.IO) {
                V300EncryptedPersonalArchive.stageAndVerify(
                    escrowInput, escrowPassword, app.cacheDir,
                ).use { original ->
                    V300EncryptedPersonalArchive.stageAndVerify(
                        intendedInput, intendedPassword, app.cacheDir,
                    ).use { imported ->
                        val oldPhotos = original.photos.map { it.photo }
                        val target = Phase1V2RestoreTargetPolicy.propose(
                            V301LocalRecoverySnapshot(
                                original.components, oldPhotos,
                                localIntegrityVerified = true,
                            ),
                            imported.components,
                            imported.photos.map { it.photo },
                        )
                        require(journal.originalDigest == target.baselineDigest &&
                            journal.targetDigest == target.targetDigest) {
                            "Authenticated escrow and target do not match v2 recovery journal"
                        }
                        val real = V301LocalRecoveryReader(app)
                            .readForInterruptedV2Recovery()
                        val domainEvidence = Phase1V2RestoreDomainForensics.inspect(
                            journal, original.components, target.combined, real.components,
                            oldPhotos, target.photos, real.photos,
                        )
                        val photoEvidence = Phase1V2PhotoByteForensics.inspect(
                            journal.step,
                            File(app.filesDir, "backlot-personal-space-photos"),
                            oldPhotos, target.photos, real.photos,
                        )
                        Phase1V2InterruptedRecoveryEvidence(
                            journal, domainEvidence, photoEvidence,
                        )
                    }
                }
            }
        }
}
