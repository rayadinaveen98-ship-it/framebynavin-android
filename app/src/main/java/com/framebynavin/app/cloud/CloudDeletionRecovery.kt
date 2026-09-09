package com.framebynavin.app.cloud

import java.util.UUID

/** Server generation is distinct from the phone's account/restore generation. */
data class CloudLifecycleState(val phase: String, val generation: Long) {
    init {
        require(phase == "active" || phase == "deleted") { "Invalid cloud lifecycle phase" }
        require(generation >= 0) { "Invalid cloud lifecycle generation" }
    }
    val active: Boolean get() = phase == "active"
}

/** No credential or creator payload is stored in the deletion journal. */
internal interface CloudDeletionJournal {
    fun pendingUserId(): String?
    fun pendingGeneration(): Long?
    fun begin(userId: String, expectedGeneration: Long)
    fun clear(userId: String)
}

internal interface CloudDeletionRemote {
    suspend fun status(userId: String): CloudLifecycleState
    suspend fun deleteOwnedData(userId: String, expectedGeneration: Long): CloudLifecycleState
    suspend fun hasOwnedData(userId: String): Boolean
}

internal class CloudDeletionPending : IllegalStateException(
    "Cloud deletion is unfinished. Review the original account before creating another backup."
)
internal class CloudDeletionVerificationFailed : IllegalStateException(
    "Cloud deletion could not be verified. The retry record was kept."
)
internal class CloudLifecycleChanged : IllegalStateException(
    "Cloud account state changed. Refresh it and review the action again. No deletion was started."
)

/**
 * The durable journal fences retries to the original owner and pre-deletion epoch.
 * A legacy RC7 journal has no epoch: it may verify an already-deleted server but
 * cannot automatically start another deletion. No background deletion is scheduled.
 */
internal class CloudDeletionRecovery(
    private val journal: CloudDeletionJournal,
    private val remote: CloudDeletionRemote,
) {
    suspend fun run(userId: String, confirmedGeneration: Long?): CloudLifecycleState {
        require(UUID.fromString(userId).toString() == userId) { "Invalid cloud account identity" }
        val pending = journal.pendingUserId()
        if (pending != null && pending != userId) throw CloudDeletionPending()
        val expected = if (pending != null) journal.pendingGeneration() else confirmedGeneration
        val state = remote.status(userId)
        if (!state.active) {
            // Never replay an old deletion into a new lifecycle. A completed
            // tombstone can only be acknowledged by a matching pending request.
            if (pending != null && expected != null && state.generation != expected + 1L)
                throw CloudLifecycleChanged()
            if (pending == null && confirmedGeneration != null && confirmedGeneration != state.generation)
                throw CloudLifecycleChanged()
            if (remote.hasOwnedData(userId)) throw CloudDeletionVerificationFailed()
            if (pending != null) journal.clear(userId)
            return state
        }
        if (expected == null) throw CloudDeletionPending() // legacy unknown-generation retry
        if (expected != state.generation) throw CloudLifecycleChanged()
        if (pending == null) journal.begin(userId, expected)
        val deleted = remote.deleteOwnedData(userId, expected)
        if (deleted.phase != "deleted" || deleted.generation != expected + 1L)
            throw CloudDeletionVerificationFailed()
        if (remote.hasOwnedData(userId)) throw CloudDeletionVerificationFailed()
        journal.clear(userId)
        return deleted
    }
}
