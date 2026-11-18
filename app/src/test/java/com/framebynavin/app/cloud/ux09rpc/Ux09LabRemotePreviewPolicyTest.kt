package com.framebynavin.app.cloud.ux09rpc

import com.framebynavin.app.cloud.ux09.Ux09Domain
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Test

/** Synthetic contract tests. Never open a session, read user data, or contact Lab. */
class Ux09LabRemotePreviewPolicyTest {
    private val projectId = "aaaaaaaa-aaaa-4aaa-8aaa-aaaaaaaaaaaa"
    private val ideaId = "bbbbbbbb-bbbb-4bbb-8bbb-bbbbbbbbbbbb"

    private fun change(
        sequence: Long,
        domain: Ux09Domain,
        deleted: Boolean = false,
    ) = Ux09LabRpcContract.Change(
        sequence = sequence,
        domain = domain,
        entityId = if (domain == Ux09Domain.PROJECT) projectId else ideaId,
        revision = 1L,
        // Private content only in memory; the UI-facing summary must never contain it.
        canonicalPayload = if (deleted) "{}" else "{\"title\":\"SECRET_FIXTURE_TITLE_734\"}",
        deleted = deleted,
    )

    @Test fun aMixedSignedPageExposesOnlyAggregatedCounts() {
        val page = Ux09LabRpcContract.Page(
            listOf(change(1, Ux09Domain.PROJECT),
                change(2, Ux09Domain.IDEA),
                change(3, Ux09Domain.IDEA, deleted = true)),
            3L, true,
        )
        val result = Ux09LabRemotePreviewPolicy.summarize(page)
        assertEquals(3, result.scannedChanges)
        assertEquals(1, result.projectChanges)
        assertEquals(2, result.ideaChanges)
        assertEquals(1, result.tombstones)
        assertTrue(result.hasMore)
        assertFalse(result.toString().contains("SECRET_FIXTURE_TITLE_734"))
        assertFalse(result.toString().contains(projectId))
        assertFalse(result.toString().contains(ideaId))
    }

    @Test fun emptyPageReportsZeroWithoutPretendingToBeTheAccountTotal() {
        val result = Ux09LabRemotePreviewPolicy.summarize(
            Ux09LabRpcContract.Page(emptyList(), 0L, false),
        )
        assertEquals(0, result.scannedChanges)
        assertFalse(result.hasMore)
    }

    @Test fun invalidCursorOrMoreThanTwentyCannotAdvance() {
        val good = Ux09LabRpcContract.Page(
            listOf(change(1, Ux09Domain.PROJECT)), 1L, false,
        )
        assertThrows(IllegalArgumentException::class.java) {
            Ux09LabRemotePreviewPolicy.summarize(good, requestedAfter = 1L)
        }
        assertThrows(IllegalArgumentException::class.java) {
            Ux09LabRemotePreviewPolicy.summarize(good, requestedLimit = 21)
        }
        assertThrows(IllegalArgumentException::class.java) {
            Ux09LabRemotePreviewPolicy.summarize(good, requestedLimit = 0)
        }
    }

    @Test fun nonmonotonicEventsAndMismatchedCursorAreRejected() {
        assertThrows(IllegalArgumentException::class.java) {
            Ux09LabRemotePreviewPolicy.summarize(Ux09LabRpcContract.Page(
                listOf(change(2, Ux09Domain.PROJECT), change(2, Ux09Domain.IDEA)),
                2L, false,
            ))
        }
        assertThrows(IllegalArgumentException::class.java) {
            Ux09LabRemotePreviewPolicy.summarize(Ux09LabRpcContract.Page(
                listOf(change(1, Ux09Domain.PROJECT)), 99L, false,
            ))
        }
    }

    @Test fun emptyPageClaimingMoreAndOverflowAreRejected() {
        assertThrows(IllegalArgumentException::class.java) {
            Ux09LabRemotePreviewPolicy.summarize(Ux09LabRpcContract.Page(
                emptyList(), 0L, true,
            ))
        }
        assertThrows(IllegalArgumentException::class.java) {
            Ux09LabRemotePreviewPolicy.summarize(Ux09LabRpcContract.Page(
                (1L..21L).map { change(it, Ux09Domain.IDEA) }, 21L, false,
            ))
        }
    }
}
