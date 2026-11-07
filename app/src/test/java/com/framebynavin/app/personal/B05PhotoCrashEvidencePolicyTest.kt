package com.framebynavin.app.personal

import com.framebynavin.app.personal.spaces.V290SpacePhotoEntity
import java.io.File
import java.nio.file.Files
import java.security.MessageDigest
import org.junit.Assert.*
import org.junit.Test

class B05PhotoCrashEvidencePolicyTest {
    private val space = "afcd17ed-5fd6-4a60-b8cb-31484ee653e7"
    private val oldId = "729d2bfa-af37-4836-af9e-c14c3d836421"
    private val freshId = "7fa4f5ce-2a88-4df0-a199-cc246d4937a2"
    private val originalBytes = byteArrayOf(0xff.toByte(),0xd8.toByte(),1,2,0xd9.toByte())
    private val importedBytes = byteArrayOf(0xff.toByte(),0xd8.toByte(),3,4,0xd9.toByte())
    private fun photo(id:String,bytes:ByteArray) = V290SpacePhotoEntity(
        id=id,spaceId=space,takenAtMillis=100L,addedAtMillis=200L,
        fileName="$id.jpg",
        sha256Hex=MessageDigest.getInstance("SHA-256").digest(bytes)
            .joinToString("") { "%02x".format(it) },
        widthPx=16,heightPx=16,note="",
    )
    private val old=photo(oldId,originalBytes)
    private val imported=photo(freshId,importedBytes)
    private fun sandbox(test:(File)->Unit) {
        val dir=Files.createTempDirectory("backlot-b05-photos-").toFile()
        try {test(File(dir,"live-photos"))}
        finally {dir.deleteRecursively()}
    }
    private fun write(root:File,photo:V290SpacePhotoEntity,bytes:ByteArray) {
        val dir=File(root,photo.spaceId)
        check(dir.isDirectory||dir.mkdirs())
        File(dir,photo.fileName).writeBytes(bytes)
    }
    private fun inspect(
        root:File,
        room:List<V290SpacePhotoEntity> = listOf(old),
        baseline:List<V290SpacePhotoEntity> = listOf(old),
        target:List<V290SpacePhotoEntity> = listOf(old,imported),
    ) = B05PhotoCrashEvidencePolicy.inspect(root,baseline,target,room)

    @Test fun missingNewFileAndRowIsIdentifiedWithoutPermission() = sandbox { root ->
        write(root,old,originalBytes)
        val report=inspect(root)
        assertEquals(B05ImportedPhotoCrashState.MISSING_FILE_AND_ROOM_ROW,
            report.imported.single().state)
        assertFalse(report.hasUnsafeEvidence)
        assertFalse(report.grantsRoomWrite)
        assertFalse(report.grantsPhotoFileWrite)
        assertFalse(report.independentlyProvesNoUnknownFiles)
    }
    @Test fun copiedVerifiedFileWithoutRoomRowIsRecognized() = sandbox { root ->
        write(root,old,originalBytes);write(root,imported,importedBytes)
        assertEquals(B05ImportedPhotoCrashState.VERIFIED_FILE_MISSING_ROOM_ROW,
            inspect(root).imported.single().state)
    }
    @Test fun copiedFileAndMatchingRoomRowAreFullyVerified() = sandbox { root ->
        write(root,old,originalBytes);write(root,imported,importedBytes)
        val report=inspect(root,room=listOf(old,imported))
        assertEquals(B05ImportedPhotoCrashState.VERIFIED_FILE_AND_ROOM_ROW,
            report.imported.single().state)
        assertTrue(report.allImportedPhotosInstalled)
        assertFalse(report.grantsRoomWrite)
    }
    @Test fun pendingCopyRequiresManualReviewEvenIfFinalFileIsValid() = sandbox { root ->
        write(root,old,originalBytes);write(root,imported,importedBytes)
        File(File(root,space),".${freshId}.restore.pending").writeBytes(importedBytes)
        val report=inspect(root,room=listOf(old,imported))
        assertEquals(B05ImportedPhotoCrashState.PENDING_COPY_REQUIRES_OWNER_REVIEW,
            report.imported.single().state)
        assertTrue(report.hasUnsafeEvidence)
        assertFalse(report.permitsAutomaticPendingDeletion)
    }
    @Test fun corruptNewBytesNeverCountAsVerified() = sandbox { root ->
        write(root,old,originalBytes);write(root,imported,originalBytes)
        val report=inspect(root,room=listOf(old))
        assertEquals(B05ImportedPhotoCrashState.CORRUPT_OR_MISSING_VERIFIED_FILE,
            report.imported.single().state)
        assertTrue(report.hasUnsafeEvidence)
    }
    @Test fun RoomRowWithMissingJpegIsUnsafe() = sandbox { root ->
        write(root,old,originalBytes)
        assertEquals(B05ImportedPhotoCrashState.CORRUPT_OR_MISSING_VERIFIED_FILE,
            inspect(root,room=listOf(old,imported)).imported.single().state)
    }
    @Test fun conflictingImportedRoomMetadataCannotBeRepairedAutomatically() = sandbox { root ->
        write(root,old,originalBytes);write(root,imported,importedBytes)
        val altered=imported.copy(note="changed by a different writer")
        assertEquals(B05ImportedPhotoCrashState.CONFLICTING_ROOM_METADATA,
            inspect(root,room=listOf(old,altered)).imported.single().state)
    }
    @Test fun missingOriginalOwnerPhotoIsNeverAccepted() = sandbox { root ->
        assertThrows(IllegalStateException::class.java) {inspect(root)}
    }
    @Test fun alteredOriginalPhotoInTargetIsRejected() = sandbox { root ->
        write(root,old,originalBytes)
        assertThrows(IllegalArgumentException::class.java) {
            inspect(root,target=listOf(old.copy(note="changed"),imported))
        }
    }
    @Test fun unknownRoomPhotoIsRejected() = sandbox { root ->
        write(root,old,originalBytes)
        val unknown=photo("a48f2efc-ad87-428e-87d9-6486faf41a19",importedBytes)
        assertThrows(IllegalArgumentException::class.java) {
            inspect(root,room=listOf(old,unknown))
        }
    }
    @Test fun symbolicLinkImportedPhotoIsNeverFollowed() = sandbox { root ->
        write(root,old,originalBytes)
        val elsewhere=File(root.parentFile,"external.jpg")
        elsewhere.writeBytes(importedBytes)
        val linked=File(File(root,space),imported.fileName)
        Files.createSymbolicLink(linked.toPath(),elsewhere.toPath())
        assertEquals(B05ImportedPhotoCrashState.CORRUPT_OR_MISSING_VERIFIED_FILE,
            inspect(root).imported.single().state)
        assertArrayEquals(importedBytes,elsewhere.readBytes())
    }
    @Test fun symlinkPrivatePhotoRootIsRejected() = sandbox { root ->
        val elsewhere=File(root.parentFile,"elsewhere")
        check(elsewhere.mkdir())
        Files.createSymbolicLink(root.toPath(),elsewhere.toPath())
        assertThrows(IllegalStateException::class.java) {inspect(root)}
    }
    @Test fun duplicatePhotoIdentityFailsClosed() = sandbox { root ->
        assertThrows(IllegalArgumentException::class.java) {
            inspect(root,room=listOf(old,old))
        }
    }
    @Test fun noImportedPhotosNeedsNoLiveWrite() = sandbox { root ->
        write(root,old,originalBytes)
        val report=inspect(root,target=listOf(old))
        assertTrue(report.imported.isEmpty())
        assertTrue(report.allImportedPhotosInstalled)
        assertFalse(report.grantsRoomWrite)
        assertFalse(report.grantsPhotoFileWrite)
    }
};
