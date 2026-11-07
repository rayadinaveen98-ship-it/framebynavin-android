package com.framebynavin.app.personal

import com.framebynavin.app.personal.spaces.V290SpacePhotoEntity
import java.io.File
import java.nio.file.Files
import java.security.MessageDigest
import org.junit.Assert.*
import org.junit.Test

class B05RetainedPendingEvidencePolicyTest {
    private val txId = "e751c74e-8d47-4f69-bd46-c95c45d04c01"
    private val space = "afcd17ed-5fd6-4a60-b8cb-31484ee653e7"
    private val photoId = "729d2bfa-af37-4836-af9e-c14c3d836421"
    private val bytes = byteArrayOf(1, 9, 2, 8)
    private val hash get() = MessageDigest.getInstance("SHA-256")
        .digest(bytes).joinToString("") { "%02x".format(it) }
    private val photo = V290SpacePhotoEntity(
        id = photoId, spaceId = space, takenAtMillis = 100L, addedAtMillis = 200L,
        fileName = photoId + ".jpg", sha256Hex = "a".repeat(64),
        widthPx = 16, heightPx = 16, note = "",
    )
    private val journal = StabilizationRestoreJournalRecord(
        transactionId = txId, preparedId = txId,
        escrowId = "613c863a-b019-4085-9df3-5f38b84454ea",
        originalDigest = "a".repeat(64), targetDigest = "b".repeat(64),
        step = StabilizationRestoreWriteStep.BEFORE_PHOTOS,
        createdAtMillis = 100L, updatedAtMillis = 200L,
    )
    private fun sandbox(action:(File,File,File)->Unit) {
        val home = Files.createTempDirectory("backlot-b5-evidence-check-").toFile()
        try {
            val root = File(home, "photo-root")
            val dir = File(root, space)
            check(dir.mkdirs())
            action(root, File(home, "history"), dir)
        } finally { home.deleteRecursively() }
    }
    private fun marker(folder:File) = File(folder, "." + photoId + ".restore.review")
    private fun pending(folder:File) = File(folder, "." + photoId + ".restore.pending")
    private fun saved(root:File) = File(File(root, txId), photoId + ".restore.pending")
    private fun addMarker(folder:File, digest:String = hash, count:Long=bytes.size.toLong()) {
        marker(folder).writeText(
            "backlot.pending-photo-review.v1\n" + txId + "\n" + photoId +
                "\n" + count + "\n" + digest + "\n"
        )
    }
    private fun inspect(live:File,history:File) =
        B05RetainedPendingEvidencePolicy.inspect(journal,photo,live,history)

    @Test fun markedButNotMovedStaysBlocked() = sandbox { live,history,folder ->
        pending(folder).writeBytes(bytes);addMarker(folder)
        val report = inspect(live,history)
        assertEquals(B05RetainedPendingEvidenceState.MARKED_BEFORE_MOVE, report.state)
        assertTrue(report.requiresOwnerReview)
        assertFalse(report.permitsAutomaticRestore)
        assertFalse(report.permitsMarkerRemoval)
    }
    @Test fun retainedBytesAreValidatedAndStayBlocked() = sandbox { live,history,folder ->
        addMarker(folder)
        check(saved(history).parentFile.mkdirs())
        saved(history).writeBytes(bytes)
        val report = inspect(live,history)
        assertEquals(B05RetainedPendingEvidenceState.RETAINED_AND_MATCHED, report.state)
        assertEquals(hash, report.expectedSha256)
        assertFalse(report.permitsDiscardingRetainedBytes)
    }
    @Test fun missingMarkerNeverMakesEvidenceTrusted() = sandbox { live,history,folder ->
        pending(folder).writeBytes(bytes)
        assertThrows(IllegalStateException::class.java) { inspect(live,history) }
    }
    @Test fun incompleteMarkerCannotBeAssumedEmpty() = sandbox { live,history,folder ->
        pending(folder).writeBytes(bytes);marker(folder).writeText("bad")
        assertThrows(IllegalArgumentException::class.java) { inspect(live,history) }
    }
    @Test fun modifiedPendingBytesAreRejected() = sandbox { live,history,folder ->
        pending(folder).writeBytes(byteArrayOf(8, 9, 2, 1));addMarker(folder)
        assertThrows(IllegalStateException::class.java) { inspect(live,history) }
    }
    @Test fun modifiedRetainedBytesAreRejected() = sandbox { live,history,folder ->
        addMarker(folder);check(saved(history).parentFile.mkdirs())
        saved(history).writeBytes(byteArrayOf(8, 9, 2, 1))
        assertThrows(IllegalStateException::class.java) { inspect(live,history) }
    }
    @Test fun bothCopiesPresentCannotBeMistakenForComplete() = sandbox { live,history,folder ->
        addMarker(folder);pending(folder).writeBytes(bytes)
        check(saved(history).parentFile.mkdirs());saved(history).writeBytes(bytes)
        assertThrows(IllegalStateException::class.java) { inspect(live,history) }
    }
    @Test fun absentBothCopiesCannotBeMistakenForComplete() = sandbox { live,history,folder ->
        addMarker(folder)
        assertThrows(IllegalStateException::class.java) { inspect(live,history) }
    }
    @Test fun wrongTransactionCheckpointCannotBeReused() = sandbox { live,history,folder ->
        pending(folder).writeBytes(bytes);addMarker(folder)
        assertThrows(IllegalArgumentException::class.java) {
            B05RetainedPendingEvidencePolicy.inspect(
                journal.copy(step=StabilizationRestoreWriteStep.VERIFYING),
                photo,live,history)
        }
    }
    @Test fun symlinkedRetainedEvidenceCannotBeRead() = sandbox { live,history,folder ->
        addMarker(folder);check(saved(history).parentFile.mkdirs())
        val external=File(live.parentFile,"external.jpg")
        external.writeBytes(bytes)
        Files.createSymbolicLink(saved(history).toPath(),external.toPath())
        assertThrows(IllegalStateException::class.java) { inspect(live,history) }
    }
    @Test fun forgedMarkerHashFailsClosed() = sandbox { live,history,folder ->
        pending(folder).writeBytes(bytes);addMarker(folder,"b".repeat(64))
        assertThrows(IllegalStateException::class.java) { inspect(live,history) }
    }
}
