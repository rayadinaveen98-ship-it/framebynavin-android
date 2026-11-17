package com.framebynavin.app.cloud.ux09

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Test

/** Synthetic, pure in-memory tests: NO account, DataStore, network or APK writes. */
class Ux09OfflineChainPolicyTest {
    private val a = "aaaaaaaa-aaaa-4aaa-8aaa-aaaaaaaaaaaa"
    private val b = "bbbbbbbb-bbbb-4bbb-8bbb-bbbbbbbbbbbb"
    private val project = "cccccccc-cccc-4ccc-8ccc-cccccccccccc"
    private val op1 = "dddddddd-dddd-4ddd-8ddd-dddddddddddd"
    private val op2 = "eeeeeeee-eeee-4eee-8eee-eeeeeeeeeeee"
    private val edit1 = "11111111-1111-4111-8111-111111111111"
    private val edit2 = "22222222-2222-4222-8222-222222222222"
    private val baseline = hash("before")
    private val v1 = hash("after first edit")
    private val v2 = hash("after second edit")

    private fun hash(value: String) = Ux09Contract.sha256(value.toByteArray())
    private fun chain(account: String = a, rev: Long = 0L) =
        Ux09EntityChain(account, 10, 20, Ux09Domain.PROJECT, project,
            rev, 0L, baseline)
    private fun edit(
        id: String = edit1,
        before: String = baseline,
        after: String = v1,
        json: String = """{"title":"First offline edit"}""",
        deleted: Boolean = false,
    ) = Ux09LocalEdit(id, before, after, Ux09Contract.canonicalPayload(json), deleted)
    private fun enqueue(chain: Ux09EntityChain, edit: Ux09LocalEdit) =
        Ux09OfflineChainPolicy.enqueue(chain, edit)
    private fun stage(chain: Ux09EntityChain, id: String = op1) =
        Ux09OfflineChainPolicy.prepareNext(chain, id,
            chain.accountId, chain.accountGeneration, chain.restoreGeneration)

    @Test fun gateIsDisabledAndNoNetworkWorkIsCreated() {
        assertFalse(Ux09LabFeatureGate.ENABLED)
    }

    @Test fun firstEditPreparesExpectedRevisionZero() {
        val queued = enqueue(chain(), edit())
        val staged = stage(queued)
        val remote = requireNotNull(staged.preparedRemoteOperation)
        assertEquals(0L, remote.expectedRevision)
        assertEquals(op1, remote.operationId)
        assertEquals(Ux09Domain.PROJECT, remote.domain)
        assertEquals(1, staged.pendingEdits.size)
    }

    @Test fun multipleOfflineEditsAdvanceCASRevisionOnlyAfterAcknowledgement() {
        val queued = enqueue(enqueue(chain(), edit()),
            edit(id = edit2, before = v1, after = v2,
                json = """{"title":"Second offline edit"}"""))
        assertEquals(2, queued.pendingEdits.size)
        val first = stage(queued, op1)
        assertEquals(0L, first.preparedRemoteOperation?.expectedRevision)
        // A different proposed operation ID cannot replace the outstanding request.
        assertEquals(first, stage(first, op2))
        val acknowledged = Ux09OfflineChainPolicy.acknowledgeApplied(
            first, op1, serverRevision = 1L, serverSequence = 1L, deleted = false)
        assertEquals(1L, acknowledged.remoteRevision)
        assertEquals(v1, acknowledged.lastCommittedLocalSha256)
        assertEquals(1, acknowledged.pendingEdits.size)
        assertEquals(null, acknowledged.preparedRemoteOperation)
        val second = stage(acknowledged, op2)
        assertEquals(1L, second.preparedRemoteOperation?.expectedRevision)
        assertEquals(op2, second.preparedRemoteOperation?.operationId)
        val done = Ux09OfflineChainPolicy.acknowledgeApplied(
            second, op2, serverRevision = 2L, serverSequence = 3L, deleted = false)
        assertEquals(2L, done.remoteRevision)
        assertEquals(v2, done.lastCommittedLocalSha256)
        assertTrue(done.pendingEdits.isEmpty())
    }

    @Test fun ambiguousNetworkSuccessRetriesExactOperationAndImmutableBody() {
        val active = stage(enqueue(chain(), edit()))
        val before = Ux09OfflineChainPolicy.retryExact(active, a, 10, 20)
        val after = Ux09OfflineChainPolicy.retryExact(active, a, 10, 20)
        assertEquals(before, after)
        assertEquals(op1, before.operationId)
        assertEquals(before.rpcFingerprint(), after.rpcFingerprint())
    }

    @Test fun accountSwitchAndRestoreEpochNeverReplayOldPendingEdits() {
        val active = stage(enqueue(chain(), edit()))
        assertThrows(IllegalArgumentException::class.java) {
            Ux09OfflineChainPolicy.retryExact(active, b, 10, 20)
        }
        assertThrows(IllegalArgumentException::class.java) {
            Ux09OfflineChainPolicy.retryExact(active, a, 11, 20)
        }
        assertThrows(IllegalArgumentException::class.java) {
            Ux09OfflineChainPolicy.prepareNext(active, op2, a, 10, 21)
        }
        assertEquals(1, active.pendingEdits.size)
    }

    @Test fun conflictFreezesQueueAndRetainsAllLocalEdits() {
        val two = enqueue(enqueue(chain(), edit()),
            edit(id = edit2, before = v1, after = v2,
                json = """{"title":"Second"}"""))
        val active = stage(two)
        val blocked = Ux09OfflineChainPolicy.recordConflict(active, op1, 4L)
        assertTrue(blocked.serverConflict)
        assertEquals(2, blocked.pendingEdits.size)
        assertEquals(active.preparedRemoteOperation, blocked.preparedRemoteOperation)
        assertThrows(IllegalArgumentException::class.java) {
            Ux09OfflineChainPolicy.retryExact(blocked, a, 10, 20)
        }
        assertThrows(IllegalArgumentException::class.java) {
            Ux09OfflineChainPolicy.enqueue(blocked,
                edit(id = "33333333-3333-4333-8333-333333333333",
                    before = v2, after = hash("third")))
        }
    }

    @Test fun permanentDeletionTerminatesChainAndCannotResurrect() {
        val deleted = stage(enqueue(chain(),
            edit(json = "{}", deleted = true)))
        val ack = Ux09OfflineChainPolicy.acknowledgeApplied(
            deleted, op1, 1L, 1L, deleted = true)
        assertTrue(ack.remoteDeleted)
        assertThrows(IllegalArgumentException::class.java) {
            enqueue(ack, edit())
        }
    }

    @Test fun cannotAppendAfterPendingPermanentTombstone() {
        val pendingDelete = enqueue(chain(), edit(json = "{}", deleted = true))
        assertThrows(IllegalArgumentException::class.java) {
            enqueue(pendingDelete, edit(id = edit2, before = v1, after = v2))
        }
    }

    @Test fun duplicateEditsAndBrokenLocalHashChainAreRejected() {
        val first = enqueue(chain(), edit())
        assertThrows(IllegalArgumentException::class.java) { enqueue(first, edit()) }
        assertThrows(IllegalArgumentException::class.java) {
            enqueue(first, edit(id = edit2, before = baseline, after = v2))
        }
        assertEquals(1, first.pendingEdits.size)
    }

    @Test fun wrongAndOutOfOrderServerAckMustNotDiscardEdit() {
        val active = stage(enqueue(chain(), edit()))
        val bad = listOf(
            { Ux09OfflineChainPolicy.acknowledgeApplied(active, op2, 1L, 1L, false) },
            { Ux09OfflineChainPolicy.acknowledgeApplied(active, op1, 2L, 1L, false) },
            { Ux09OfflineChainPolicy.acknowledgeApplied(active, op1, 1L, 0L, false) },
            { Ux09OfflineChainPolicy.acknowledgeApplied(active, op1, 1L, 1L, true) },
        )
        for (run in bad) assertThrows(IllegalArgumentException::class.java) { run() }
        assertEquals(1, active.pendingEdits.size)
    }

    @Test fun recoveredQueueGapOrForgedPreparedBodyFailsClosed() {
        val first = enqueue(chain(), edit())
        val broken = first.copy(pendingEdits = listOf(
            edit().copy(localBeforeSha256 = hash("unexpected baseline"))
        ))
        assertThrows(IllegalArgumentException::class.java) { stage(broken) }
        val valid = stage(first)
        val forged = valid.copy(preparedRemoteOperation =
            requireNotNull(valid.preparedRemoteOperation).copy(expectedRevision = 9L))
        assertThrows(IllegalArgumentException::class.java) {
            Ux09OfflineChainPolicy.retryExact(forged, a, 10, 20)
        }
    }

    @Test fun separateOwnersNeverExchangeQueuedOperations() {
        val firstA = stage(enqueue(chain(a), edit()))
        val firstB = stage(enqueue(chain(b), edit()))
        assertEquals(a, firstA.preparedRemoteOperation?.accountId)
        assertEquals(b, firstB.preparedRemoteOperation?.accountId)
        assertThrows(IllegalArgumentException::class.java) {
            Ux09OfflineChainPolicy.retryExact(firstA, b, 10, 20)
        }
        assertEquals(op1, firstB.preparedRemoteOperation?.operationId)
    }

    @Test fun invalidCanonicalOrOversizedPayloadRefusedAtEnqueue() {
        assertThrows(IllegalArgumentException::class.java) {
            enqueue(chain(), edit(json = """{"filePath":"/data/user/0/secret"}"""))
        }
        assertThrows(IllegalArgumentException::class.java) {
            enqueue(chain(), edit(json = """{"title":"""" + "x".repeat(17000) + "\"}"))
        }
        assertThrows(IllegalArgumentException::class.java) {
            enqueue(chain(), edit(id = "legacy-id"))
        }
    }

    private fun Ux09Intent.rpcFingerprint(): String = Ux09Contract
        .sha256(Ux09Contract.rpcApplyBody(this).toString().toByteArray())
}
