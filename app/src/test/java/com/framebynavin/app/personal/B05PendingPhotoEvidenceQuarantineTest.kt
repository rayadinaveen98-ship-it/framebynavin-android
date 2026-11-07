package com.framebynavin.app.personal

import com.framebynavin.app.personal.spaces.V290SpacePhotoEntity
import java.io.File
import java.nio.file.Files
import java.security.MessageDigest
import org.junit.Assert.*
import org.junit.Test

/** Temporary synthetic paths only; no Backlot device or real account. */
class B05PendingPhotoEvidenceQuarantineTest {
    private val txId = "e751c74e-8d47-4f69-bd46-c95c45d04c01"
    private val space = "afcd17ed-5fd6-4a60-b8cb-31484ee653e7"
    private val photoId = "729d2bfa-af37-4836-af9e-c14c3d836421"
    private val bytes = byteArrayOf(1, 2, 3, 4, 5)
    private val photo = V290SpacePhotoEntity(
        id=photoId, spaceId=space, takenAtMillis=100L, addedAtMillis=200L,
        fileName=photoId + ".jpg", sha256Hex="a".repeat(64),
        widthPx=16, heightPx=16, note="",
    )
    private val originalComponents = V300PersonalComponents(
        "owner-personal", "owner-challenges", "owner-groups", "owner-spaces"
    )
    private val targetComponents = V300PersonalComponents(
        "target-personal", "target-challenges", "target-groups", "target-spaces"
    )
    private val journal = StabilizationRestoreJournalRecord(
        transactionId=txId, preparedId=txId,
        escrowId="613c863a-b019-4085-9df3-5f38b84454ea",
        originalDigest=V304RestorePlanPolicy.fingerprint(originalComponents,emptyList()),
        targetDigest=V304RestorePlanPolicy.fingerprint(targetComponents,listOf(photo)),
        step=StabilizationRestoreWriteStep.BEFORE_PHOTOS,
        createdAtMillis=100L, updatedAtMillis=200L,
    )

    private fun sandbox(run: (File,File,File)->Unit) {
        val home=Files.createTempDirectory("backlot-pending-preservation-").toFile()
        try {
            val root=File(home,"photos")
            val folder=File(root,space)
            check(folder.mkdirs())
            run(root,File(home,"preserved-evidence"),folder)
        } finally { home.deleteRecursively() }
    }
    private fun pending(folder:File)=File(folder,"." + photoId + ".restore.pending")
    private fun sut(root:File,history:File)=B05PendingPhotoEvidenceQuarantine(root,history)

    @Test fun preservesExactPartialFileWithoutReplacingOwnersJpeg() = sandbox { root,history,folder ->
        pending(folder).writeBytes(bytes)
        val original=File(folder,photo.fileName)
        original.writeBytes(byteArrayOf(7,8,9))
        val kept=sut(root,history).preserve(
            journal,photo,originalComponents,emptyList(),targetComponents,listOf(photo),true
        )
        assertFalse(pending(folder).exists())
        assertArrayEquals(bytes,kept.evidencePath.readBytes())
        assertArrayEquals(byteArrayOf(7,8,9),original.readBytes())
        assertEquals(bytes.size.toLong(),kept.bytes)
        assertEquals(MessageDigest.getInstance("SHA-256")
            .digest(bytes).joinToString("") { "%02x".format(it) },kept.sha256)
        assertFalse(kept.authorizesRestore)
        assertFalse(kept.permitsDeletingEvidence)
        assertFalse(kept.permitsNormalWrites)
        assertTrue(kept.blockingMarkerPath.isFile)
        assertTrue(kept.blockingMarkerPath.readText().contains(txId))
        assertEquals(1,
            B04PrivatePhotoForensicPolicy.inspect(
                root,emptyList(),listOf(photo),
                StabilizationRestoreWriteStep.BEFORE_PHOTOS,
            ).untrackedEntries)
    }
    @Test fun rejectsMissingOwnerConfirmationWithoutMovingFile() = sandbox { root,history,folder ->
        pending(folder).writeBytes(bytes)
        assertThrows(IllegalArgumentException::class.java) {
            sut(root,history).preserve(
                journal,photo,originalComponents,emptyList(),targetComponents,listOf(photo),false)
        }
        assertArrayEquals(bytes,pending(folder).readBytes())
        assertFalse(history.exists())
    }
    @Test fun rejectsOriginalOwnerPhoto() = sandbox { root,history,folder ->
        pending(folder).writeBytes(bytes)
        assertThrows(IllegalArgumentException::class.java) {
            sut(root,history).preserve(
                journal.copy(originalDigest=V304RestorePlanPolicy.fingerprint(
                    originalComponents,listOf(photo))),
                photo,originalComponents,listOf(photo),targetComponents,listOf(photo),true)
        }
        assertTrue(pending(folder).exists())
    }
    @Test fun rejectsDifferentCheckpoint() = sandbox { root,history,folder ->
        pending(folder).writeBytes(bytes)
        assertThrows(IllegalArgumentException::class.java) {
            sut(root,history).preserve(
                journal.copy(step=StabilizationRestoreWriteStep.VERIFYING),
                photo,originalComponents,emptyList(),targetComponents,listOf(photo),true)
        }
    }
    @Test fun cannotEraseExistingForensicCopy() = sandbox { root,history,folder ->
        pending(folder).writeBytes(bytes)
        val retainedFolder=File(history,txId)
        check(retainedFolder.mkdirs())
        val retained=File(retainedFolder,photoId + ".restore.pending")
        retained.writeBytes(byteArrayOf(9))
        assertThrows(IllegalStateException::class.java) {
            sut(root,history).preserve(
            journal,photo,originalComponents,emptyList(),targetComponents,listOf(photo),true
        )
        }
        assertArrayEquals(bytes,pending(folder).readBytes())
        assertArrayEquals(byteArrayOf(9),retained.readBytes())
    }
    @Test fun refusesSymlinkSourceWithoutTouchingExternalBytes() = sandbox { root,history,folder ->
        val outside=File(root.parentFile,"outside")
        outside.writeBytes(bytes)
        Files.createSymbolicLink(pending(folder).toPath(),outside.toPath())
        assertThrows(IllegalStateException::class.java) {
            sut(root,history).preserve(
            journal,photo,originalComponents,emptyList(),targetComponents,listOf(photo),true
        )
        }
        assertArrayEquals(bytes,outside.readBytes())
    }
    @Test fun refusesSymlinkHistoryDirectory() = sandbox { root,history,folder ->
        pending(folder).writeBytes(bytes)
        check(history.mkdir())
        val elsewhere=File(root.parentFile,"elsewhere")
        check(elsewhere.mkdir())
        Files.createSymbolicLink(File(history,txId).toPath(),elsewhere.toPath())
        assertThrows(IllegalStateException::class.java) {
            sut(root,history).preserve(
            journal,photo,originalComponents,emptyList(),targetComponents,listOf(photo),true
        )
        }
        assertTrue(pending(folder).exists())
    }
    @Test fun emptyPendingCopyRequiresManualReview() = sandbox { root,history,folder ->
        pending(folder).writeBytes(byteArrayOf())
        assertThrows(IllegalStateException::class.java) {
            sut(root,history).preserve(
            journal,photo,originalComponents,emptyList(),targetComponents,listOf(photo),true
        )
        }
        assertTrue(pending(folder).exists())
    }
    @Test fun badTransactionIdentityIsRejected() = sandbox { root,history,folder ->
        pending(folder).writeBytes(bytes)
        assertThrows(IllegalArgumentException::class.java) {
            sut(root,history).preserve(
                journal.copy(transactionId="../untrusted"),photo,true,false)
        }
        assertArrayEquals(bytes,pending(folder).readBytes())
    }
    @Test fun missingPendingCopyCannotBePretendedPreserved() = sandbox { root,history,_ ->
        assertThrows(IllegalStateException::class.java) {
            sut(root,history).preserve(
            journal,photo,originalComponents,emptyList(),targetComponents,listOf(photo),true
        )
        }
    }
    @Test fun preservedEvidenceCannotMakeInterruptedRestoreAutoResume() = sandbox {
        root,history,folder ->
        pending(folder).writeBytes(bytes)
        val kept=sut(root,history).preserve(
            journal,photo,originalComponents,emptyList(),targetComponents,listOf(photo),true
        )
        assertTrue(kept.evidencePath.isFile)
        assertTrue(kept.blockingMarkerPath.isFile)
        val forensic=B04PrivatePhotoForensicPolicy.inspect(
            root,emptyList(),listOf(photo),
            StabilizationRestoreWriteStep.BEFORE_PHOTOS,
        )
        assertTrue(forensic.demandsOwnerReview)
        assertFalse(forensic.mayAutoResume)
        assertFalse(forensic.mayDeletePendingFiles)
    }

    @Test fun existingForensicReviewMarkerCannotBeOverwritten() = sandbox {
        root,history,folder ->
        pending(folder).writeBytes(bytes)
        val marker=File(folder,"." + photoId + ".restore.review")
        marker.writeText("owner evidence")
        assertThrows(IllegalStateException::class.java) {
            sut(root,history).preserve(
            journal,photo,originalComponents,emptyList(),targetComponents,listOf(photo),true
        )
        }
        assertEquals("owner evidence",marker.readText())
        assertArrayEquals(bytes,pending(folder).readBytes())
    }

}
