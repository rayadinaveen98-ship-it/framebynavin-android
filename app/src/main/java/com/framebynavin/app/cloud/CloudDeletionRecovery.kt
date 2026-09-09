package com.framebynavin.app.cloud

import java.util.UUID

/** No credential or payload is stored in the deletion journal. */
internal interface CloudDeletionJournal {
    fun pendingUserId(): String?
    fun begin(userId: String)
    fun clear(userId: String)
}

internal interface CloudDeletionRemote {
    suspend fun deleteOwnedData(userId: String)
    suspend fun hasOwnedData(userId: String): Boolean
}

internal class CloudDeletionPending : IllegalStateException(
    "Cloud data deletion is unfinished. Retry it with the original account before starting another deletion."
)

internal class CloudDeletionVerificationFailed : IllegalStateException(
    "Cloud deletion could not be verified. The retry record was kept."
)

/**
 * A retry is deliberately explicit. There is no background deletion worker.
 * DELETE is idempotent for already-removed rows; a failed or cancelled request
 * leaves the journal intact. Only the original owner can clear it.
 */
internal class CloudDeletionRecovery(
    private val journal: CloudDeletionJournal,
    private val remote: CloudDeletionRemote,
) {
    suspend fun run(userId: String) {
        require(UUID.fromString(userId).toString() == userId) { "Invalid cloud account identity" }
        val pending = journal.pendingUserId()
        if (pending != null && pending != userId) throw CloudDeletionPending()
        journal.begin(userId)
        remote.deleteOwnedData(userId)
        if (remote.hasOwnedData(userId)) throw CloudDeletionVerificationFailed()
        journal.clear(userId)
    }
}
