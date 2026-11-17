package com.framebynavin.app.cloud.ux09

/**
 * PURE, DISCONNECTED UX09-02A operation ordering prototype.
 *
 * This is a decision/state machine, NOT durable storage, networking, a WorkManager
 * job, a Project/Idea codec or a production application integration.
 *
 * A single chain represents one owner + domain + entity. Local edits are append-
 * only while waiting. At most one immutable cloud operation can be prepared at a
 * time. After an ambiguous network outcome the *same* exact intent must retry.
 *
 * Future durable integration MUST persist every new state before making an HTTP
 * request and MUST fence account/restore transitions before reading the queue.
 */
internal data class Ux09LocalEdit(
    val localEditId: String,
    val localBeforeSha256: String,
    val localAfterSha256: String,
    val canonicalPayloadJson: String,
    val tombstone: Boolean = false,
)

internal data class Ux09EntityChain(
    val accountId: String,
    val accountGeneration: Long,
    val restoreGeneration: Long,
    val domain: Ux09Domain,
    val entityId: String,
    val remoteRevision: Long,
    val lastAcknowledgedSequence: Long,
    val lastCommittedLocalSha256: String,
    val pendingEdits: List<Ux09LocalEdit> = emptyList(),
    val preparedRemoteOperation: Ux09Intent? = null,
    val serverConflict: Boolean = false,
    val remoteDeleted: Boolean = false,
)

internal object Ux09OfflineChainPolicy {
    private val SHA256 = Regex("[a-f0-9]{64}")

    fun enqueue(chain: Ux09EntityChain, edit: Ux09LocalEdit): Ux09EntityChain {
        validate(chain)
        require(!chain.serverConflict && !chain.remoteDeleted) {
            "A conflicted or permanently deleted remote entity cannot enqueue uploads"
        }
        Ux09Contract.requireUuid(edit.localEditId)
        require(SHA256.matches(edit.localBeforeSha256) && SHA256.matches(edit.localAfterSha256) &&
            edit.localBeforeSha256 != edit.localAfterSha256) {
            "Local before/after content hashes must describe a real change"
        }
        require(edit.canonicalPayloadJson == Ux09Contract.canonicalPayload(edit.canonicalPayloadJson)) {
            "Non-canonical draft payload cannot enter an offline chain"
        }
        if (edit.tombstone) require(edit.canonicalPayloadJson == "{}") {
            "Permanent tombstone must use an empty payload"
        }
        require(chain.pendingEdits.none { it.localEditId == edit.localEditId }) {
            "Duplicate local edit identity: never silently merge separate edits"
        }
        require(chain.pendingEdits.none { it.tombstone }) {
            "A permanent delete already terminates this entity's queued history"
        }
        val expectedBefore = chain.pendingEdits.lastOrNull()?.localAfterSha256
            ?: chain.lastCommittedLocalSha256
        require(edit.localBeforeSha256 == expectedBefore) {
            "Local edit chain has a missing/reordered state transition; keep the originals"
        }
        return chain.copy(pendingEdits = chain.pendingEdits + edit)
    }

    /**
     * Stage just one *immutable* RPC request. operationId must be persisted with
     * the chain before any future network adapter sends this request.
     * If already prepared, repeat returns the same body (never substitute an ID).
     */
    fun prepareNext(
        chain: Ux09EntityChain,
        operationId: String,
        currentAccountId: String,
        currentAccountGeneration: Long,
        currentRestoreGeneration: Long,
    ): Ux09EntityChain {
        validate(chain)
        requireOwner(chain, currentAccountId, currentAccountGeneration, currentRestoreGeneration)
        require(!chain.serverConflict && !chain.remoteDeleted) {
            "Conflict/deletion requires explicit reconciliation; never auto-overwrite"
        }
        val existing = chain.preparedRemoteOperation
        if (existing != null) return chain
        val next = chain.pendingEdits.firstOrNull() ?: return chain
        require(next.localBeforeSha256 == chain.lastCommittedLocalSha256) {
            "Cannot prepare a cloud write whose local predecessor is not acknowledged"
        }
        Ux09Contract.requireUuid(operationId)
        require(chain.remoteRevision < Long.MAX_VALUE - 1L) {
            "Revision ceiling reached"
        }
        val intent = Ux09Intent(
            accountId = chain.accountId,
            accountGeneration = chain.accountGeneration,
            restoreGeneration = chain.restoreGeneration,
            domain = chain.domain,
            entityId = chain.entityId,
            operationId = operationId,
            expectedRevision = chain.remoteRevision,
            localBeforeSha256 = next.localBeforeSha256,
            localAfterSha256 = next.localAfterSha256,
            payloadJson = next.canonicalPayloadJson,
            tombstone = next.tombstone,
        )
        Ux09Contract.validate(intent)
        return chain.copy(preparedRemoteOperation = intent)
    }

    fun retryExact(
        chain: Ux09EntityChain,
        currentAccountId: String,
        currentAccountGeneration: Long,
        currentRestoreGeneration: Long,
    ): Ux09Intent {
        validate(chain)
        requireOwner(chain, currentAccountId, currentAccountGeneration, currentRestoreGeneration)
        require(!chain.serverConflict && !chain.remoteDeleted) {
            "No retry after remote conflict/deletion without an explicit resolution"
        }
        return requireNotNull(chain.preparedRemoteOperation) {
            "Nothing has been prepared for a safe exact-body retry"
        }
    }

    /**
     * An applied response is trusted only after the adapter validates the
     * authenticated server identity and response shape. No network handling here.
     * The sequence may jump when other entities have generated events, but must
     * increase relative to this entity's last observed acknowledgement.
     */
    fun acknowledgeApplied(
        chain: Ux09EntityChain,
        operationId: String,
        serverRevision: Long,
        serverSequence: Long,
        deleted: Boolean,
    ): Ux09EntityChain {
        validate(chain)
        require(!chain.serverConflict) { "Conflict requires separate reconciliation" }
        val active = requireNotNull(chain.preparedRemoteOperation) {
            "Unprepared operation acknowledgement rejected"
        }
        require(active.operationId == operationId &&
            active.expectedRevision == chain.remoteRevision &&
            serverRevision == chain.remoteRevision + 1L &&
            serverSequence > chain.lastAcknowledgedSequence &&
            deleted == active.tombstone) {
            "Unexpected or stale server acknowledgement; preserve pending edits"
        }
        val first = chain.pendingEdits.firstOrNull()
        requireNotNull(first) { "Acknowledgement without a pending edit" }
        require(first.canonicalPayloadJson == active.payloadJson &&
            first.localBeforeSha256 == active.localBeforeSha256 &&
            first.localAfterSha256 == active.localAfterSha256) {
            "Server result does not belong to this exact local edit"
        }
        return chain.copy(
            remoteRevision = serverRevision,
            lastAcknowledgedSequence = serverSequence,
            lastCommittedLocalSha256 = first.localAfterSha256,
            pendingEdits = chain.pendingEdits.drop(1),
            preparedRemoteOperation = null,
            remoteDeleted = deleted,
        )
    }

    fun recordConflict(
        chain: Ux09EntityChain,
        operationId: String,
        reportedRemoteRevision: Long,
    ): Ux09EntityChain {
        validate(chain)
        requireNotNull(chain.preparedRemoteOperation).let {
            require(it.operationId == operationId) {
                "Conflict response belongs to an unknown operation"
            }
        }
        require(reportedRemoteRevision >= 0) { "Malformed conflict result" }
        // Keep the same prepared body and ALL local pending edits for owner review.
        // Never synthesize a winning revision or delete local contents here.
        return chain.copy(serverConflict = true)
    }

    private fun requireOwner(
        chain: Ux09EntityChain,
        accountId: String,
        accountGeneration: Long,
        restoreGeneration: Long,
    ) {
        require(accountId == chain.accountId &&
            accountGeneration == chain.accountGeneration &&
            restoreGeneration == chain.restoreGeneration) {
            "Signed-in identity or restore epoch changed: previous outbox remains quarantined"
        }
    }

    private fun validate(chain: Ux09EntityChain) {
        Ux09Contract.requireUuid(chain.accountId)
        Ux09Contract.requireUuid(chain.entityId)
        require(chain.accountGeneration >= 0 && chain.restoreGeneration >= 0)
        require(chain.remoteRevision in 0..(Long.MAX_VALUE - 2))
        require(chain.lastAcknowledgedSequence >= 0)
        require(SHA256.matches(chain.lastCommittedLocalSha256))
        if (chain.preparedRemoteOperation != null) {
            Ux09Contract.validate(chain.preparedRemoteOperation)
            require(chain.pendingEdits.isNotEmpty()) {
                "An in-flight request cannot exist without its unchanged local edit"
            }
            require(chain.preparedRemoteOperation.accountId == chain.accountId &&
                chain.preparedRemoteOperation.entityId == chain.entityId &&
                chain.preparedRemoteOperation.domain == chain.domain &&
                chain.preparedRemoteOperation.accountGeneration == chain.accountGeneration &&
                chain.preparedRemoteOperation.restoreGeneration == chain.restoreGeneration) {
                "Prepared operation owner or epoch mismatch"
            }
        }
        if (chain.remoteDeleted) {
            require(chain.pendingEdits.isEmpty() && chain.preparedRemoteOperation == null) {
                "Remote tombstone has unresolved local edits"
            }
        }
    }
}
