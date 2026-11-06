package com.framebynavin.app.personal

import android.content.Context
import java.io.File
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Final READ-ONLY two-sided safety check.
 *
 * A candidate archive's prepared photos AND the original phone's encrypted
 * rollback escrow must agree on the same baseline. The escrow is verified
 * with the owner's passphrase, not only a metadata checksum. There is
 * deliberately no live COMMIT interface in this class.
 */
internal data class V305CommitBarrierReceipt(
    val preparedId: String,
    val rollbackEscrowId: String,
    val localBaselineSha256: String,
    val incomingArchiveSha256: String,
    val rollbackArchiveSha256: String,
) {
    val authorizedToCommit: Boolean get() = false
}

internal object V305CommitBarrierPolicy {
    private val sha64 = Regex("[0-9a-f]{64}")
    fun check(
        prepared: V304PreparedTransfer,
        escrow: V305EscrowReceipt,
        current: V301LocalRecoverySnapshot,
    ): V305CommitBarrierReceipt {
        require(current.localIntegrityVerified &&
            prepared.state == "READY" &&
            sha64.matches(prepared.baselineDigest) &&
            sha64.matches(prepared.archiveDigest) &&
            sha64.matches(escrow.baselineDigest) &&
            sha64.matches(escrow.archiveSha256) &&
            escrow.archiveBytes > 0L) {
            "Restore preparation or rollback escrow is incomplete"
        }
        val latest = V304RestorePlanPolicy.fingerprint(current.components, current.photos)
        require(prepared.baselineDigest == escrow.baselineDigest &&
            prepared.baselineDigest == latest) {
            "Original phone data differs from prepared recovery or rollback copy"
        }
        return V305CommitBarrierReceipt(
            preparedId = prepared.transactionId,
            rollbackEscrowId = escrow.id,
            localBaselineSha256 = latest,
            incomingArchiveSha256 = prepared.archiveDigest,
            rollbackArchiveSha256 = escrow.archiveSha256,
        )
    }
}

internal class V305RestoreCommitBarrier(context: Context) {
    private val app = context.applicationContext
    private val local = V301LocalRecoveryReader(app)
    private val staged = V304RestorePreparationStore(
        File(app.filesDir, "backlot-personal-restore-preparation-v1"),
    )
    private val escrow = V305RollbackEscrowCoordinator(app)

    /**
     * Owner must enter the escrow password. Re-authenticates ALL old JPEG
     * bytes, reads every prepared incoming JPEG again, and rechecks current
     * live records. The returned report is NOT authority to write.
     */
    suspend fun inspectOnly(
        preparedId: String,
        rollbackEscrowId: String,
        escrowPassphrase: CharArray,
    ): V305CommitBarrierReceipt = withContext(Dispatchers.IO) {
        val current = local.read()
        val prep = staged.inspect(preparedId)
        val old = escrow.verifyOwnerEscrow(rollbackEscrowId, escrowPassphrase)
        val receipt = V305CommitBarrierPolicy.check(prep, old, current)
        val latest = local.read()
        require(V304RestorePlanPolicy.fingerprint(
            latest.components, latest.photos,
        ) == receipt.localBaselineSha256) {
            "Phone data changed while inspecting recovery readiness"
        }
        receipt
    }
}
