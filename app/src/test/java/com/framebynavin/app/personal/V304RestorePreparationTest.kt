package com.framebynavin.app.personal

import com.framebynavin.app.personal.spaces.V289PersonalSpace
import com.framebynavin.app.personal.spaces.V289SpaceState
import com.framebynavin.app.personal.spaces.V289SpaceType
import com.framebynavin.app.personal.spaces.V289SpacesMetadataCodec
import com.framebynavin.app.personal.spaces.V290SpacePhotoEntity
import java.io.File
import java.security.MessageDigest
import java.time.LocalDate
import java.time.LocalTime
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Test

class V304RestorePreparationTest {
    private val spaceId = "12537c2d-521d-40db-bf25-2d79bf0f1c2a"
    private val photoId = "ecb00ac2-134d-4195-b80b-7a41111a3021"
    private val photoBytes = ByteArray(8192) { (it * 3 % 251).toByte() }

    private fun hash(bytes: ByteArray): String = MessageDigest.getInstance("SHA-256")
        .digest(bytes).joinToString("") { "%02x".format(it) }

    private fun components(withSpace: Boolean = false): V300PersonalComponents {
        val spaces = if (withSpace) listOf(
            V289PersonalSpace(
                id = spaceId, title = "Owner's Space",
                type = V289SpaceType.CUSTOM, createdAtMillis = 1000L,
            )
        ) else emptyList()
        return V300PersonalComponents(
            personal = PersonalCompletionBackupCodec.encode(emptyList(), emptyList()),
            challenges = V297ChallengeCodec.encode(emptyList()),
            groups = V298ReminderGroupCodec.encode(emptyList()),
            spaces = V289SpacesMetadataCodec.encode(V289SpaceState(spaces = spaces)),
        )
    }

    private fun photo() = V290SpacePhotoEntity(
        id = photoId, spaceId = spaceId,
        takenAtMillis = 1100L, addedAtMillis = 1200L,
        fileName = "$photoId.jpg", sha256Hex = hash(photoBytes),
        widthPx = 400, heightPx = 600, note = "Private reflection",
    )

    private fun plan(): V304RestorePlan =
        V304RestorePlanPolicy.propose(
            local = V301LocalRecoverySnapshot(components(), emptyList(), true),
            incoming = components(withSpace = true),
            incomingPhotos = listOf(photo()),
        )

    private fun directory(label: String) =
        File(System.getProperty("java.io.tmpdir"), "v304-$label-${System.nanoTime()}")

    private fun staged(root: File, bytes: ByteArray = photoBytes): V300VerifiedPhoto {
        val file = File(root, "authenticated-source.jpg")
        check(root.mkdirs())
        file.writeBytes(bytes)
        return V300VerifiedPhoto(photo(), bytes.size.toLong(), file)
    }

    @Test fun ownerRequestedPreparationCopiesVerifiedPhotoOutsideLiveDirectories() {
        val root = directory("valid")
        val inputRoot = directory("input")
        try {
            val source = staged(inputRoot)
            val store = V304RestorePreparationStore(root)
            val prepared = store.prepare(plan(), listOf(source), requestedByOwner = true)
            assertEquals(1, prepared.photoCount)
            assertEquals("READY", prepared.state)
            assertFalse(prepared.canCommitToLiveStorage)
            assertFalse(plan().canWriteLiveOwnerData)
            assertEquals(prepared, store.inspect(prepared.transactionId))
            assertTrue(File(root, "${prepared.transactionId}/part-00000.jpg").isFile)
            assertFalse(File(root.parentFile, "backlot-personal-space-photos").exists())
            assertEquals(photoBytes.toList(),
                File(root, "${prepared.transactionId}/part-00000.jpg").readBytes().toList())
            store.discard(prepared.transactionId)
            assertFalse(File(root, prepared.transactionId).exists())
            assertTrue(source.stagedFile.exists())
        } finally { root.deleteRecursively(); inputRoot.deleteRecursively() }
    }

    @Test fun noOwnerRequestNeverCreatesAnyPrivatePreparation() {
        val root = directory("no-consent")
        val inputRoot = directory("input")
        try {
            val source = staged(inputRoot)
            val store = V304RestorePreparationStore(root)
            assertThrows(IllegalArgumentException::class.java) {
                store.prepare(plan(), listOf(source), requestedByOwner = false)
            }
            assertFalse(root.exists())
        } finally { root.deleteRecursively(); inputRoot.deleteRecursively() }
    }

    @Test fun alteredPhotoAfterArchiveVerificationFailsAndRemovesStaging() {
        val root = directory("tamper")
        val inputRoot = directory("input")
        try {
            val source = staged(inputRoot, "wrong bytes after verification".toByteArray())
            val store = V304RestorePreparationStore(root)
            assertThrows(IllegalArgumentException::class.java) {
                store.prepare(plan(), listOf(source), requestedByOwner = true)
            }
            assertTrue(root.listFiles().orEmpty().isEmpty())
            assertTrue(source.stagedFile.exists())
        } finally { root.deleteRecursively(); inputRoot.deleteRecursively() }
    }

    @Test fun extraFileInPreparedFolderInvalidatesReadiness() {
        val root = directory("extra")
        val inputRoot = directory("input")
        try {
            val store = V304RestorePreparationStore(root)
            val prepared = store.prepare(plan(), listOf(staged(inputRoot)), true)
            File(root, "${prepared.transactionId}/unexpected.partial").writeText("orphan")
            assertThrows(IllegalArgumentException::class.java) {
                store.inspect(prepared.transactionId)
            }
            store.discard(prepared.transactionId)
        } finally { root.deleteRecursively(); inputRoot.deleteRecursively() }
    }

    @Test fun changedPreparedPhotoFailsChecksumWithoutTouchingOriginalArchiveStage() {
        val root = directory("photo-corrupt")
        val inputRoot = directory("input")
        try {
            val source = staged(inputRoot)
            val store = V304RestorePreparationStore(root)
            val prepared = store.prepare(plan(), listOf(source), true)
            File(root, "${prepared.transactionId}/part-00000.jpg")
                .writeBytes("now damaged".toByteArray())
            assertThrows(IllegalArgumentException::class.java) {
                store.inspect(prepared.transactionId)
            }
            assertTrue(source.stagedFile.exists())
            store.discard(prepared.transactionId)
        } finally { root.deleteRecursively(); inputRoot.deleteRecursively() }
    }

    @Test fun journalTamperingOrInterruptedStateCannotBeCalledReady() {
        val root = directory("journal")
        val inputRoot = directory("input")
        try {
            val store = V304RestorePreparationStore(root)
            val prepared = store.prepare(plan(), listOf(staged(inputRoot)), true)
            val journal = File(root, "${prepared.transactionId}/journal.json")
            journal.writeText(journal.readText().replace("READY", "PREPARING"))
            assertThrows(IllegalArgumentException::class.java) {
                store.inspect(prepared.transactionId)
            }
            store.discard(prepared.transactionId)
        } finally { root.deleteRecursively(); inputRoot.deleteRecursively() }
    }

    @Test fun crashInventoryReportsInterruptedOrUnknownFilesWithoutDeletingThem() {
        val root = directory("crash-list")
        val source = directory("input")
        try {
            val store = V304RestorePreparationStore(root)
            assertEquals(V304PreparationInventory(0, 0, 0), store.inspectInventory())
            val prepared = store.prepare(plan(), listOf(staged(source)), true)
            assertEquals(V304PreparationInventory(1, 0, 0), store.inspectInventory())
            val journal = File(root, "${prepared.transactionId}/journal.json")
            journal.writeText(journal.readText().replace("READY", "PREPARING"))
            val unknown = File(root, "not-a-transaction.partial")
            unknown.writeText("untracked private preparation file")
            assertEquals(V304PreparationInventory(0, 1, 1), store.inspectInventory())
            assertTrue(File(root, "${prepared.transactionId}/part-00000.jpg").exists())
            assertTrue(unknown.exists())
            store.discard(prepared.transactionId)
            assertTrue(unknown.exists())
        } finally { root.deleteRecursively(); source.deleteRecursively() }
    }

    @Test fun changingDestinationSincePreviewInvalidatesBaseline() {
        val proposed = plan()
        val changed = components().copy(
            personal = PersonalCompletionBackupCodec.encode(
                listOf(PersonalReminder(
                    id = "new-on-phone", title = "Newly added",
                    startDate = LocalDate.of(2026, 11, 15),
                    time = LocalTime.of(9, 0),
                    createdAtMillis = 5000L, updatedAtMillis = 5000L,
                )), emptyList(),
            )
        )
        assertThrows(IllegalArgumentException::class.java) {
            V304RestorePlanPolicy.checkStillSame(
                proposed, V301LocalRecoverySnapshot(changed, emptyList(), true),
            )
        }
        V304RestorePlanPolicy.checkStillSame(
            proposed, V301LocalRecoverySnapshot(components(), emptyList(), true),
        )
    }

    @Test fun unknownOrPathTraversalIdsCannotDeleteOtherFiles() {
        val root = directory("unsafe-id")
        val unrelated = File(root.parentFile, "user-kept-${System.nanoTime()}.txt")
        try {
            unrelated.writeText("keep this")
            val store = V304RestorePreparationStore(root)
            assertThrows(IllegalArgumentException::class.java) {
                store.discard("../${unrelated.name}")
            }
            assertTrue(unrelated.isFile)
            assertEquals("keep this", unrelated.readText())
        } finally { unrelated.delete(); root.deleteRecursively() }
    }

    @Test fun conflictedArchiveCannotReachPreparationPlan() {
        val before = PersonalReminder(
            id = "same", title = "Keep local",
            startDate = LocalDate.of(2026, 11, 15),
            time = LocalTime.of(9, 0),
            createdAtMillis = 100L, updatedAtMillis = 100L,
        )
        val local = components().copy(
            personal = PersonalCompletionBackupCodec.encode(listOf(before), emptyList()),
        )
        val incoming = components().copy(
            personal = PersonalCompletionBackupCodec.encode(
                listOf(before.copy(title = "Different archived value")), emptyList(),
            ),
        )
        assertThrows(IllegalArgumentException::class.java) {
            V304RestorePlanPolicy.propose(
                V301LocalRecoverySnapshot(local, emptyList(), true),
                incoming, emptyList(),
            )
        }
    }
}
