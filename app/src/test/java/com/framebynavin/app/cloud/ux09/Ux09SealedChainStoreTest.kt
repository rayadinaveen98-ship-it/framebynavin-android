package com.framebynavin.app.cloud.ux09

import org.json.JSONObject
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.io.File
import javax.crypto.SecretKey
import javax.crypto.spec.SecretKeySpec

/** Synthetic JVM-only AES-GCM/atomic disk tests; never calls AndroidKeyStore or Supabase. */
class Ux09SealedChainStoreTest {
    @get:Rule val tmp = TemporaryFolder()

    private val accountA = "aaaaaaaa-aaaa-4aaa-8aaa-aaaaaaaaaaaa"
    private val accountB = "bbbbbbbb-bbbb-4bbb-8bbb-bbbbbbbbbbbb"
    private val entity = "cccccccc-cccc-4ccc-8ccc-cccccccccccc"
    private val editId = "dddddddd-dddd-4ddd-8ddd-dddddddddddd"
    private val operationId = "eeeeeeee-eeee-4eee-8eee-eeeeeeeeeeee"
    private val original = hash("original local state")
    private val changed = hash("new local state")
    private val fakeKeyA = SecretKeySpec(ByteArray(32) { 0x31.toByte() }, "AES")
    private val fakeKeyB = SecretKeySpec(ByteArray(32) { 0x52.toByte() }, "AES")

    private fun hash(text: String) = Ux09Contract.sha256(text.toByteArray())
    private fun keyFor(owner: String): SecretKey = when (owner) {
        accountA -> fakeKeyA
        accountB -> fakeKeyB
        else -> throw IllegalArgumentException("Test key not available")
    }
    private fun store() = Ux09SealedChainStore(tmp.root, ::keyFor)
    private fun file(owner: String = accountA) =
        File(File(tmp.root, owner), "creator_project-$entity.sealed")

    private fun initial(owner: String = accountA) = Ux09EntityChain(
        accountId = owner,
        accountGeneration = 5,
        restoreGeneration = 7,
        domain = Ux09Domain.PROJECT,
        entityId = entity,
        remoteRevision = 0L,
        lastAcknowledgedSequence = 0L,
        lastCommittedLocalSha256 = original,
    )
    private fun edited(owner: String = accountA): Ux09EntityChain {
        val draft = Ux09LocalEdit(
            localEditId = editId, localBeforeSha256 = original,
            localAfterSha256 = changed,
            canonicalPayloadJson = Ux09Contract.canonicalPayload(
                """{"title":"Synthetic screenplay ✨","stage":"outline"}"""
            ),
        )
        return Ux09OfflineChainPolicy.enqueue(initial(owner), draft)
    }
    private fun prepared(owner: String = accountA): Ux09EntityChain =
        Ux09OfflineChainPolicy.prepareNext(edited(owner), operationId, owner, 5, 7)

    private fun put(state: Ux09EntityChain, expected: Long? = null) =
        store().write(state, state.accountId, 5, 7, expected)
    private fun read(owner: String = accountA, gen: Long = 5, restore: Long = 7) =
        store().read(owner, gen, restore, Ux09Domain.PROJECT, entity)

    @Test fun featureRemainsOffAndNoRealCloudCaller() {
        assertFalse(Ux09LabFeatureGate.ENABLED)
    }

    @Test fun encryptedFileNeverContainsSyntheticTitleOrRawJson() {
        val state = prepared()
        val saved = put(state)
        assertEquals(1L, saved.storageRevision)
        val disk = file().readBytes()
        val utf8 = String(disk, Charsets.ISO_8859_1)
        assertFalse(utf8.contains("Synthetic screenplay"))
        assertFalse(utf8.contains("\"pendingEdits\""))
        assertFalse(utf8.contains("\"accountId\""))
        assertTrue(disk.size > 50)
        assertEquals(state, read()?.state)
    }

    @Test fun rebootReopensExactQueuedOperationIdForRetry() {
        val original = prepared()
        put(original)
        val restored = requireNotNull(read())
        val req = Ux09OfflineChainPolicy.retryExact(restored.state, accountA, 5, 7)
        assertEquals(operationId, req.operationId)
        assertEquals(0L, req.expectedRevision)
        assertEquals(original.preparedRemoteOperation, req)
    }

    @Test fun writeRequiresMatchingOptimisticStorageRevision() {
        val first = put(prepared())
        assertEquals(1L, first.storageRevision)
        assertThrows(IllegalArgumentException::class.java) {
            put(prepared())
        }
        val second = put(prepared(), expected = 1L)
        assertEquals(2L, second.storageRevision)
        assertThrows(IllegalArgumentException::class.java) {
            put(prepared(), expected = 1L)
        }
        assertEquals(2L, read()?.storageRevision)
    }

    @Test fun changingRestoreOrAccountGenerationMustNotDecryptOldQueue() {
        put(prepared())
        assertThrows(IllegalStateException::class.java) { read(gen = 6) }
        assertThrows(IllegalStateException::class.java) { read(restore = 8) }
        assertEquals(1L, read()?.storageRevision)
    }

    @Test fun appCannotWriteUnderWrongActiveAccountOrRestoreEpoch() {
        val state = prepared()
        assertThrows(IllegalArgumentException::class.java) {
            store().write(state, accountB, 5, 7, null)
        }
        assertThrows(IllegalArgumentException::class.java) {
            store().write(state, accountA, 6, 7, null)
        }
        assertThrows(IllegalArgumentException::class.java) {
            store().write(state, accountA, 5, 8, null)
        }
        assertFalse(file().exists())
    }

    @Test fun differentEncryptionKeyCannotReadExistingQueue() {
        put(prepared())
        val different = Ux09SealedChainStore(tmp.root) {
            SecretKeySpec(ByteArray(32) { 0x12.toByte() }, "AES")
        }
        assertThrows(IllegalStateException::class.java) {
            different.read(accountA, 5, 7, Ux09Domain.PROJECT, entity)
        }
        assertTrue(file().exists())
    }

    @Test fun ciphertextCorruptionMustFailClosedWithoutDeletingOriginal() {
        put(prepared())
        val f = file()
        val tampered = f.readBytes()
        tampered[tampered.lastIndex] = (tampered.last().toInt() xor 1).toByte()
        f.writeBytes(tampered)
        assertThrows(IllegalStateException::class.java) { read() }
        assertArrayEquals(tampered, f.readBytes())
    }

    @Test fun crossAccountCiphertextTransplantIsRejected() {
        put(prepared())
        val transplanted = file().readBytes()
        val target = file(accountB)
        assertTrue(target.parentFile.mkdirs())
        target.writeBytes(transplanted)
        assertThrows(IllegalStateException::class.java) {
            read(accountB)
        }
        assertArrayEquals(transplanted, target.readBytes())
    }

    @Test fun sealedDataFromAnotherEntityCannotBeRelabelled() {
        put(prepared())
        val target = File(File(tmp.root, accountA),
            "creator_idea-$entity.sealed")
        target.writeBytes(file().readBytes())
        assertThrows(IllegalStateException::class.java) {
            store().read(accountA, 5, 7, Ux09Domain.IDEA, entity)
        }
        assertTrue(target.exists())
    }

    @Test fun unfinishedTempFileRefusesWriteAndPreservesPriorState() {
        put(prepared())
        val tmpFile = File(file().parentFile, file().name + ".tmp")
        tmpFile.writeText("partial write")
        assertThrows(IllegalArgumentException::class.java) {
            put(prepared(), expected = 1L)
        }
        assertTrue(tmpFile.exists())
        assertEquals(1L, read()?.storageRevision)
    }

    @Test fun preservedConflictRetainsAllPendingEditsOnRestart() {
        val active = prepared()
        val conflict = Ux09OfflineChainPolicy.recordConflict(active, operationId, 3L)
        put(conflict)
        val recovered = requireNotNull(read())
        assertTrue(recovered.state.serverConflict)
        assertEquals(1, recovered.state.pendingEdits.size)
        assertEquals(active.preparedRemoteOperation, recovered.state.preparedRemoteOperation)
        assertThrows(IllegalArgumentException::class.java) {
            Ux09OfflineChainPolicy.retryExact(recovered.state, accountA, 5, 7)
        }
    }

    @Test fun terminalTombstoneRoundTripsWithoutResurrection() {
        val deleted = Ux09OfflineChainPolicy.enqueue(initial(), Ux09LocalEdit(
            editId, original, changed, "{}", tombstone = true,
        ))
        val sent = Ux09OfflineChainPolicy.prepareNext(deleted, operationId, accountA, 5, 7)
        val ack = Ux09OfflineChainPolicy.acknowledgeApplied(sent, operationId,
            1L, 1L, true)
        put(ack)
        val restore = requireNotNull(read())
        assertTrue(restore.state.remoteDeleted)
        assertTrue(restore.state.pendingEdits.isEmpty())
        assertThrows(IllegalArgumentException::class.java) {
            Ux09OfflineChainPolicy.enqueue(restore.state,
                Ux09LocalEdit(editId, changed, hash("resurrect"), "{}"))
        }
    }

    @Test fun noncanonicalOrForgedRecoveredChainIsRefused() {
        val bad = prepared().copy(preparedRemoteOperation =
            prepared().preparedRemoteOperation?.copy(expectedRevision = 3L))
        assertThrows(IllegalArgumentException::class.java) { put(bad) }
        assertFalse(file().exists())
        val payload = Ux09SealedChainJson.encode(prepared())
        val edited = JSONObject(String(payload, Charsets.UTF_8))
            .put("unreviewed_extra", "must not be silently dropped")
        assertThrows(IllegalArgumentException::class.java) {
            Ux09SealedChainJson.decode(edited.toString().toByteArray())
        }
    }

    @Test fun ciphertextDifferentOnTwoWritesWithFreshGcmNonce() {
        put(prepared())
        val first = file().readBytes()
        put(prepared(), expected = 1L)
        val second = file().readBytes()
        assertNotEquals(first.toList(), second.toList())
        assertEquals(prepared(), read()?.state)
    }

    @Test fun rollbackToPriorAuthenticatedRevisionRequiresCallerCheckpoint() {
        put(prepared())
        val revisionOne = file().readBytes()
        put(prepared(), expected = 1L)
        file().writeBytes(revisionOne)
        // AES-GCM authentication alone CANNOT identify a correctly signed old
        // file copied back over a newer one. The caller must retain a distinct,
        // independently trusted version checkpoint before enabling live sync.
        assertEquals(1L, read()?.storageRevision)
        assertThrows(IllegalArgumentException::class.java) {
            put(prepared(), expected = 2L)
        }
    }

    @Test fun symlinkTargetIsNotOverwritten() {
        val originalFile = File(tmp.root, "external.txt").apply { writeText("external") }
        val accountDir = File(tmp.root, accountA)
        assertTrue(accountDir.mkdirs())
        val symlink = file()
        try {
            java.nio.file.Files.createSymbolicLink(symlink.toPath(), originalFile.toPath())
        } catch (_: UnsupportedOperationException) {
            return // Unsupported only on some test host file systems.
        }
        assertThrows(IllegalArgumentException::class.java) { put(prepared()) }
        assertEquals("external", originalFile.readText())
    }
}
