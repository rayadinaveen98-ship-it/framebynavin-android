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
        var failClear = false
        override fun pendingUserId() = pending
        override fun begin(userId: String) {
            check(pending == null || pending == userId)
            pending = userId
        }
        override fun clear(userId: String) {
            check(pending == userId)
            if (failClear) error("Synthetic journal failure")
            pending = null
        }
    }

    private class Remote : CloudDeletionRemote {
        val calls = mutableListOf<String>()
        var remaining = true
        var failDelete = false
        var failVerification = false
        var cancelled = false
        override suspend fun deleteOwnedData(userId: String) {
            calls += "delete:$userId"
            if (cancelled) throw CancellationException("Synthetic cancellation")
            if (failDelete) error("Synthetic partial deletion")
            remaining = false
        }
        override suspend fun hasOwnedData(userId: String): Boolean {
            calls += "verify:$userId"
            if (failVerification) error("Synthetic verification failure")
            return remaining
        }
    }

    @Test fun partialFailureKeepsOwnerAndRetryIsIdempotent(): Unit = runBlocking {
        val journal = Journal()
        val remote = Remote().apply { failDelete = true }
        val recovery = CloudDeletionRecovery(journal, remote)
        assertThrows(IllegalStateException::class.java) { runBlocking { recovery.run(owner) } }
        assertEquals(owner, journal.pending)
        remote.failDelete = false
        recovery.run(owner)
        assertNull(journal.pending)
        assertEquals(listOf("delete:$owner", "delete:$owner", "verify:$owner"), remote.calls)
    }

    @Test fun nonEmptyVerificationCannotMarkDeletionComplete(): Unit = runBlocking {
        val journal = Journal()
        val remote = Remote()
        val recovery = CloudDeletionRecovery(journal, remote)
        remote.remaining = true
        remote.failVerification = true
        assertThrows(IllegalStateException::class.java) { runBlocking { recovery.run(owner) } }
        assertEquals(owner, journal.pending)
        remote.failVerification = false
        recovery.run(owner)
        assertNull(journal.pending)
    }

    @Test fun anotherAccountCannotInheritPendingDeletion(): Unit = runBlocking {
        val journal = Journal().apply { pending = owner }
        val remote = Remote()
        assertThrows(CloudDeletionPending::class.java) {
            runBlocking { CloudDeletionRecovery(journal, remote).run(other) }
        }
        assertEquals(owner, journal.pending)
        assertTrue(remote.calls.isEmpty())
    }

    @Test fun cancellationPreservesRetryAndNeverVerifies(): Unit = runBlocking {
        val journal = Journal()
        val remote = Remote().apply { cancelled = true }
        val failure = runCatching { CloudDeletionRecovery(journal, remote).run(owner) }.exceptionOrNull()
        assertTrue(failure is CancellationException)
        assertEquals(owner, journal.pending)
        assertEquals(listOf("delete:$owner"), remote.calls)
    }

    @Test fun failedJournalCommitCannotEraseRetry(): Unit = runBlocking {
        val journal = Journal().apply { failClear = true }
        val remote = Remote()
        val recovery = CloudDeletionRecovery(journal, remote)
        assertThrows(IllegalStateException::class.java) { runBlocking { recovery.run(owner) } }
        assertEquals(owner, journal.pending)
        journal.failClear = false
        recovery.run(owner)
        assertNull(journal.pending)
    }

    @Test fun constructionAndIdentityValidationNeverTouchRemote(): Unit = runBlocking {
        val journal = Journal()
        val remote = Remote()
        val recovery = CloudDeletionRecovery(journal, remote)
        assertTrue(remote.calls.isEmpty())
        assertThrows(IllegalArgumentException::class.java) { runBlocking { recovery.run("invalid-id") } }
        assertNull(journal.pending)
        assertTrue(remote.calls.isEmpty())
    }
}
