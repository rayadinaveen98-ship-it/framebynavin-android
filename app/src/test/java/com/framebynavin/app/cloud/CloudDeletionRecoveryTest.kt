package com.framebynavin.app.cloud

import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Test
import java.util.UUID

class CloudDeletionRecoveryTest {
    private val owner = UUID.randomUUID().toString()
    private val other = UUID.randomUUID().toString()

    private class Journal : CloudDeletionJournal {
        var pending: String? = null
        var expected: Long? = null
        var failClear = false
        override fun pendingUserId() = pending
        override fun pendingGeneration() = expected
        override fun begin(userId: String, expectedGeneration: Long) {
            check(pending == null)
            pending = userId
            expected = expectedGeneration
        }
        override fun clear(userId: String) {
            check(pending == userId)
            if (failClear) error("Synthetic journal failure")
            pending = null
            expected = null
        }
    }

    private class Remote : CloudDeletionRemote {
        val calls = mutableListOf<String>()
        var state = CloudLifecycleState("active", 0)
        var remaining = true
        var failDelete = false
        var failVerification = false
        var cancelled = false
        override suspend fun status(userId: String): CloudLifecycleState {
            calls += "status:$userId"
            return state
        }
        override suspend fun deleteOwnedData(userId: String, expectedGeneration: Long): CloudLifecycleState {
            calls += "delete:$userId:$expectedGeneration"
            if (cancelled) throw CancellationException("Synthetic cancellation")
            if (failDelete) error("Synthetic network failure")
            check(state.active && state.generation == expectedGeneration)
            state = CloudLifecycleState("deleted", expectedGeneration + 1)
            remaining = false
            return state
        }
        override suspend fun hasOwnedData(userId: String): Boolean {
            calls += "verify:$userId"
            if (failVerification) error("Synthetic verification failure")
            return remaining
        }
    }

    @Test fun failedDeleteRetainsOriginalEpochForRetry() = runBlocking {
        val journal = Journal()
        val remote = Remote().apply { failDelete = true }
        val recovery = CloudDeletionRecovery(journal, remote)
        assertThrows(IllegalStateException::class.java) { runBlocking { recovery.run(owner, 0) } }
        assertEquals(owner, journal.pending)
        assertEquals(0L, journal.expected)
        remote.failDelete = false
        assertEquals(CloudLifecycleState("deleted", 1), recovery.run(owner, 0))
        assertNull(journal.pending)
        assertEquals(2, remote.calls.count { it.startsWith("delete:") })
    }

    @Test fun responseLostAfterAtomicDeleteCanOnlyVerifyOriginalGeneration() = runBlocking {
        val journal = Journal()
        val remote = Remote().apply { failVerification = true }
        assertThrows(IllegalStateException::class.java) { runBlocking { CloudDeletionRecovery(journal, remote).run(owner, 0) } }
        assertEquals(CloudLifecycleState("deleted", 1), remote.state)
        remote.failVerification = false
        assertEquals(remote.state, CloudDeletionRecovery(journal, remote).run(owner, 0))
        assertNull(journal.pending)
        assertEquals(1, remote.calls.count { it.startsWith("delete:") })
    }

    @Test fun oldRetryCannotDeleteNewlyReactivatedHistory() = runBlocking {
        val journal = Journal().apply { begin(owner, 0) }
        val remote = Remote().apply { state = CloudLifecycleState("active", 2) }
        assertThrows(CloudLifecycleChanged::class.java) { runBlocking { CloudDeletionRecovery(journal, remote).run(owner, 2) } }
        assertEquals(owner, journal.pending)
        assertEquals(0L, journal.expected)
        assertTrue(remote.calls.none { it.startsWith("delete:") })
    }

    @Test fun oldRetryCannotAcknowledgeASecondDeletionGeneration() = runBlocking {
        val journal = Journal().apply { begin(owner, 0) }
        val remote = Remote().apply { state = CloudLifecycleState("deleted", 3); remaining = false }
        assertThrows(CloudLifecycleChanged::class.java) { runBlocking { CloudDeletionRecovery(journal, remote).run(owner, 0) } }
        assertEquals(owner, journal.pending)
    }

    @Test fun legacyJournalNeverStartsAnotherDeletion() = runBlocking {
        val journal = Journal().apply { pending = owner }
        val remote = Remote()
        assertThrows(CloudDeletionPending::class.java) { runBlocking { CloudDeletionRecovery(journal, remote).run(owner, 0) } }
        assertTrue(remote.calls.none { it.startsWith("delete:") })
        remote.state = CloudLifecycleState("deleted", 1)
        remote.remaining = false
        CloudDeletionRecovery(journal, remote).run(owner, 0)
        assertNull(journal.pending)
    }

    @Test fun otherAccountCannotInheritPendingDeletion() = runBlocking {
        val journal = Journal().apply { begin(owner, 0) }
        val remote = Remote()
        assertThrows(CloudDeletionPending::class.java) { runBlocking { CloudDeletionRecovery(journal, remote).run(other, 0) } }
        assertTrue(remote.calls.isEmpty())
        assertEquals(owner, journal.pending)
    }

    @Test fun cancellationAndFailedVerificationKeepJournal() = runBlocking {
        val journal = Journal()
        val remote = Remote().apply { cancelled = true }
        val failure = runCatching { CloudDeletionRecovery(journal, remote).run(owner, 0) }.exceptionOrNull()
        assertTrue(failure is CancellationException)
        assertEquals(owner, journal.pending)
        remote.cancelled = false
        remote.failVerification = true
        assertThrows(IllegalStateException::class.java) { runBlocking { CloudDeletionRecovery(journal, remote).run(owner, 0) } }
        assertEquals(owner, journal.pending)
    }

    @Test fun nonEmptyVerificationCannotMarkDeletionComplete() = runBlocking {
        val journal = Journal()
        val remote = Remote().apply { remaining = true; state = CloudLifecycleState("deleted", 1) }
        journal.begin(owner, 0)
        assertThrows(CloudDeletionVerificationFailed::class.java) {
            runBlocking { CloudDeletionRecovery(journal, remote).run(owner, 0) }
        }
        assertEquals(owner, journal.pending)
    }

    @Test fun failedJournalCommitCannotEraseRetry() = runBlocking {
        val journal = Journal().apply { failClear = true }
        val remote = Remote()
        assertThrows(IllegalStateException::class.java) { runBlocking { CloudDeletionRecovery(journal, remote).run(owner, 0) } }
        assertEquals(owner, journal.pending)
        journal.failClear = false
        CloudDeletionRecovery(journal, remote).run(owner, 0)
        assertNull(journal.pending)
        assertEquals(1, remote.calls.count { it.startsWith("delete:") })
    }

    @Test fun invalidIdentityAndUnknownOrChangedConfirmationCannotDelete() = runBlocking {
        val journal = Journal()
        val remote = Remote()
        assertThrows(IllegalArgumentException::class.java) { runBlocking { CloudDeletionRecovery(journal, remote).run("invalid-id", 0) } }
        assertThrows(CloudDeletionPending::class.java) { runBlocking { CloudDeletionRecovery(journal, remote).run(owner, null) } }
        remote.state = CloudLifecycleState("active", 2)
        assertThrows(CloudLifecycleChanged::class.java) { runBlocking { CloudDeletionRecovery(journal, remote).run(owner, 0) } }
        assertNull(journal.pending)
        assertTrue(remote.calls.none { it.startsWith("delete:") })
    }

    @Test fun lifecycleStateRejectsMalformedValues() {
        assertThrows(IllegalArgumentException::class.java) { CloudLifecycleState("unknown", 0) }
        assertThrows(IllegalArgumentException::class.java) { CloudLifecycleState("active", -1) }
        assertFalse(CloudLifecycleState("deleted", 1).active)
    }
}
