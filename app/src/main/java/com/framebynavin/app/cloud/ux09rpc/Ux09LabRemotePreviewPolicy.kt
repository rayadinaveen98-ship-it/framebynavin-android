package com.framebynavin.app.cloud.ux09rpc

import com.framebynavin.app.cloud.ux09.Ux09Domain

/**
 * UX09-02D: a privacy-minimizing description of ONE validated signed delta page.
 *
 * Never imports Project/Idea data, stores a cursor, acknowledges tombstones,
 * modifies the old snapshot, or queues any upload. Only counts escape this class.
 * A page is not the total remote library; hasMore explicitly identifies truncation.
 */
internal data class Ux09LabRemotePreview(
    val scannedChanges: Int,
    val projectChanges: Int,
    val ideaChanges: Int,
    val tombstones: Int,
    val hasMore: Boolean,
)

internal object Ux09LabRemotePreviewPolicy {
    const val PAGE_LIMIT = 20

    fun summarize(
        page: Ux09LabRpcContract.Page,
        requestedAfter: Long = 0L,
        requestedLimit: Int = PAGE_LIMIT,
    ): Ux09LabRemotePreview {
        // This first pilot may only inspect the first page. No durable cursor exists.
        require(requestedAfter == 0L && requestedLimit in 1..PAGE_LIMIT) {
            "Remote read-only preview cannot paginate or commit a cloud cursor"
        }
        require(page.changes.size <= requestedLimit)
        var prior = requestedAfter
        page.changes.forEach { change ->
            require(change.sequence > prior) {
                "Unexpected delta order; refuse even a summarized result"
            }
            prior = change.sequence
        }
        require(page.nextCursor == prior)
        require(!page.hasMore || page.changes.isNotEmpty()) {
            "A truncated empty remote page cannot be safely previewed"
        }
        return Ux09LabRemotePreview(
            scannedChanges = page.changes.size,
            projectChanges = page.changes.count { it.domain == Ux09Domain.PROJECT },
            ideaChanges = page.changes.count { it.domain == Ux09Domain.IDEA },
            tombstones = page.changes.count { it.deleted },
            hasMore = page.hasMore,
        )
    }
}
