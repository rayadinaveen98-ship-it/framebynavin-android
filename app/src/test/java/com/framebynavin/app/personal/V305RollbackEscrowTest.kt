package com.framebynavin.app.personal

import com.framebynavin.app.personal.spaces.V289SpaceState
import com.framebynavin.app.personal.spaces.V289SpacesMetadataCodec
import java.io.File
import java.io.InputStream
import java.io.OutputStream
import java.time.LocalDate
import java.time.LocalTime
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Test

class V305RollbackEscrowTest {
    private val original = "synthetic authenticated encrypted-archive data".toByteArray()
    private val digest = "a".repeat(64)

    private fun root(tag: String) = File(
        System.getProperty("java.io.tmpdir"),
        "v305-$tag-${System.nanoTime()}",
    )

    private suspend fun create(
        store: V305RollbackEscrowStore,
        owner: Boolean = true,
        writer: suspend (OutputStream) -> Unit = { it.write(original) },
        verifier: suspend (InputStream) -> Unit = { input ->
            assertArrayEquals(original, input.readBytes())
        },
    ) = store.create(owner, digest, writer, verifier)

    @Test fun independentEscrowSurvivesReopenWithExactlyVerifiedBytes() {
        val root = root("valid")
        try {
            val store = V305RollbackEscrowStore(root)
            var verifications = 0
            val receipt = runBlocking {
                create(store, verifier = {
                    verifications++
                    assertArrayEquals(original, it.readBytes())
                })
            }
            assertEquals(2, verifications) // before sealing + after seal
            assertFalse(receipt.canWriteLiveOwnerData)
            assertEquals(V305EscrowInventory(1, 0, 0), store.inventory())
            assertEquals(receipt, runBlocking {
                store.verify(receipt.id) {
                    verifications++
                    assertArrayEquals(original, it.readBytes())
                }
            })
            assertEquals(3, verifications)
            store.discard(receipt.id)
            assertEquals(V305EscrowInventory(0, 0, 0), store.inventory())
        } finally { root.deleteRecursively() }
    }

    @Test fun absenceOfOwnerConsentCannotCreateAnEscrow() {
        val root = root("no-consent")
        try {
            val store = V305RollbackEscrowStore(root)
            assertThrows(IllegalArgumentException::class.java) {
                runBlocking { create(store, owner = false) }
            }
            assertFalse(root.exists())
        } finally { root.deleteRecursively() }
    }

    @Test fun failedAuthenticationDoesNotLeaveASealedRecoveryCopy() {
        val root = root("auth-fails")
        try {
            val store = V305RollbackEscrowStore(root)
            assertThrows(IllegalArgumentException::class.java) {
                runBlocking {
                    create(store, verifier = {
                        it.readBytes()
                        throw IllegalArgumentException("Invalid authenticated archive")
                    })
                }
            }
            assertTrue(root.listFiles().orEmpty().isEmpty())
        } finally { root.deleteRecursively() }
    }

    @Test fun partialWriterFailureIsNeverListedAsAValidEscrow() {
        val root = root("partial-write")
        try {
            val store = V305RollbackEscrowStore(root)
            assertThrows(IllegalStateException::class.java) {
                runBlocking {
                    create(store, writer = {
                        it.write(original.copyOf(9))
                        throw IllegalStateException("storage interrupted")
                    })
                }
            }
            assertTrue(root.listFiles().orEmpty().isEmpty())
        } finally { root.deleteRecursively() }
    }

    @Test fun changedArchiveBytesAreDetectedBeforeDecryptCallback() {
        val root = root("modified")
        try {
            val store = V305RollbackEscrowStore(root)
            val receipt = runBlocking { create(store) }
            File(root, "${receipt.id}/archive.zip").writeBytes(
                original.copyOf().apply { this[3] = (this[3].toInt() xor 7).toByte() }
            )
            var called = false
            assertThrows(IllegalArgumentException::class.java) {
                runBlocking { store.verify(receipt.id) { called = true } }
            }
            assertFalse(called)
            assertEquals(V305EscrowInventory(0, 1, 0), store.inventory())
            assertTrue(File(root, receipt.id).exists())
        } finally { root.deleteRecursively() }
    }

    @Test fun journalDamageOrExtraFilesAreNotSilentlyRepaired() {
        val root = root("journal")
        try {
            val store = V305RollbackEscrowStore(root)
            val receipt = runBlocking { create(store) }
            File(root, "${receipt.id}/unknown.partial").writeText("interrupted")
            assertEquals(V305EscrowInventory(0, 1, 0), store.inventory())
            assertThrows(IllegalArgumentException::class.java) {
                runBlocking { store.verify(receipt.id) { it.readBytes() } }
            }
            File(root, "${receipt.id}/unknown.partial").delete()
            File(root, "${receipt.id}/journal.json").writeText("{}")
            assertEquals(V305EscrowInventory(0, 1, 0), store.inventory())
            assertTrue(File(root, receipt.id).exists())
        } finally { root.deleteRecursively() }
    }

    @Test fun interruptedAndUnrecognizedFoldersStayForManualReview() {
        val root = root("interrupted")
        try {
            assertTrue(root.mkdirs())
            val id = "c4ed038e-8f80-4fa3-a79e-f06e0d99d678"
            assertTrue(File(root, id).mkdir())
            File(root, "$id/archive.partial").writeText("unfinished bytes")
            File(root, "unknown-recovery.tmp").writeText("not a transaction")
            val store = V305RollbackEscrowStore(root)
            assertEquals(V305EscrowInventory(0, 1, 1), store.inventory())
            assertTrue(File(root, "$id/archive.partial").isFile)
            assertTrue(File(root, "unknown-recovery.tmp").isFile)
        } finally { root.deleteRecursively() }
    }

    @Test fun invalidIdCannotDeleteAnyOtherPrivateOrExternalFile() {
        val root = root("paths")
        val unrelated = File(root.parentFile, "keep-v305-${System.nanoTime()}.txt")
        try {
            unrelated.writeText("owner content")
            val store = V305RollbackEscrowStore(root)
            assertThrows(IllegalArgumentException::class.java) {
                store.discard("../${unrelated.name}")
            }
            assertEquals("owner content", unrelated.readText())
        } finally { unrelated.delete(); root.deleteRecursively() }
    }

    private fun components(reminders: List<PersonalReminder> = emptyList()) =
        V300PersonalComponents(
            PersonalCompletionBackupCodec.encode(reminders, emptyList()),
            V297ChallengeCodec.encode(emptyList()),
            V298ReminderGroupCodec.encode(emptyList()),
            V289SpacesMetadataCodec.encode(V289SpaceState()),
        )

    @Test fun escrowContentsMustMatchEveryExistingOwnerRecord() {
        val empty = components()
        val snapshot = V301LocalRecoverySnapshot(empty, emptyList(), true)
        V305RollbackMatchPolicy.requireSameSnapshot(snapshot, empty, emptyList())
        val inserted = PersonalReminder(
            id = "new-reminder", title = "Cannot omit this",
            startDate = LocalDate.of(2026, 11, 15),
            time = LocalTime.of(8, 30),
            createdAtMillis = 100L, updatedAtMillis = 100L,
        )
        val owner = V301LocalRecoverySnapshot(
            components(listOf(inserted)), emptyList(), true,
        )
        assertThrows(IllegalArgumentException::class.java) {
            V305RollbackMatchPolicy.requireSameSnapshot(
                owner, empty, emptyList(),
            )
        }
    }

    @Test fun unverifiedLocalPhotoInventoryMustFailClosed() {
        val empty = components()
        assertThrows(IllegalArgumentException::class.java) {
            V305RollbackMatchPolicy.requireSameSnapshot(
                V301LocalRecoverySnapshot(empty, emptyList(), false),
                empty, emptyList(),
            )
        }
    }
}
