package com.framebynavin.app.personal

import com.framebynavin.app.personal.spaces.V290PhotoTimelinePolicy
import com.framebynavin.app.personal.spaces.V290SpacePhotoEntity
import java.io.File
import java.nio.file.Files
import java.nio.file.LinkOption

/**
 * Evidence classification for an interrupted B3 private-photo stage.
 * This is READ-ONLY and does not trust an unverified JPEG by its filename.
 * The caller must independently authenticate both encrypted archive sets
 * and inspect all untracked photo-root entries with B04 forensic policy.
 */
internal enum class B05ImportedPhotoCrashState {
    MISSING_FILE_AND_ROOM_ROW,
    VERIFIED_FILE_MISSING_ROOM_ROW,
    VERIFIED_FILE_AND_ROOM_ROW,
    PENDING_COPY_REQUIRES_OWNER_REVIEW,
    CORRUPT_OR_MISSING_VERIFIED_FILE,
    CONFLICTING_ROOM_METADATA,
}

internal data class B05PhotoCrashCandidate(
    val photoId: String,
    val state: B05ImportedPhotoCrashState,
)

internal data class B05PhotoCrashReport(
    val imported: List<B05PhotoCrashCandidate>,
) {
    val hasUnsafeEvidence: Boolean get() = imported.any {
        it.state in setOf(
            B05ImportedPhotoCrashState.PENDING_COPY_REQUIRES_OWNER_REVIEW,
            B05ImportedPhotoCrashState.CORRUPT_OR_MISSING_VERIFIED_FILE,
            B05ImportedPhotoCrashState.CONFLICTING_ROOM_METADATA,
        )
    }
    val allImportedPhotosInstalled: Boolean get() = imported.all {
        it.state == B05ImportedPhotoCrashState.VERIFIED_FILE_AND_ROOM_ROW
    }
    val grantsPhotoFileWrite: Boolean get() = false
    val grantsRoomWrite: Boolean get() = false
    val permitsAutomaticPendingDeletion: Boolean get() = false
    val independentlyProvesNoUnknownFiles: Boolean get() = false
}

internal object B05PhotoCrashEvidencePolicy {
    private val noLinks = LinkOption.NOFOLLOW_LINKS

    fun inspect(
        photoRoot: File,
        original: List<V290SpacePhotoEntity>,
        target: List<V290SpacePhotoEntity>,
        liveRoomRows: List<V290SpacePhotoEntity>,
    ): B05PhotoCrashReport {
        original.forEach(V290PhotoTimelinePolicy::validate)
        target.forEach(V290PhotoTimelinePolicy::validate)
        liveRoomRows.forEach(V290PhotoTimelinePolicy::validate)
        require(original.size <= 500 && target.size <= 500 &&
            liveRoomRows.size <= 500) {
            "Private photo inventory exceeds B05 recovery safety limit"
        }
        fun keyed(list: List<V290SpacePhotoEntity>, description: String):
            Map<String,V290SpacePhotoEntity> {
            val map = list.associateBy { it.id }
            require(map.size == list.size) { "Repeated $description photo identity" }
            return map
        }
        val originalById = keyed(original, "original")
        val targetById = keyed(target, "target")
        val roomById = keyed(liveRoomRows, "live Room")
        require(originalById.all { (id, photo) -> targetById[id] == photo }) {
            "Imported target altered original private photo metadata"
        }
        require(roomById.keys.all { it in targetById } &&
            target.map { it.spaceId to it.fileName }.distinct().size == target.size) {
            "Unknown Room photo or aliased destination path"
        }
        check(!Files.isSymbolicLink(photoRoot.toPath()) &&
            (!Files.exists(photoRoot.toPath(), noLinks) ||
                Files.isDirectory(photoRoot.toPath(), noLinks))) {
            "Unsafe private JPEG root"
        }
        fun files(photo: V290SpacePhotoEntity): Pair<File,File> {
            val folder = File(photoRoot, photo.spaceId)
            check(!Files.isSymbolicLink(folder.toPath()) &&
                (!Files.exists(folder.toPath(), noLinks) ||
                    Files.isDirectory(folder.toPath(), noLinks))) {
                "Unsafe private Space photo directory"
            }
            return File(folder, photo.fileName) to
                File(folder, ".${photo.id}.restore.pending")
        }
        fun verified(file: File, sha: String): Boolean {
            if (Files.isSymbolicLink(file.toPath()) ||
                !Files.isRegularFile(file.toPath(), noLinks) ||
                file.length() !in 1L..V299PhotoIntegrityPolicy.MAX_BYTES) return false
            return file.inputStream().use {
                V299PhotoIntegrityPolicy.matches(it,sha)
            }
        }
        for (old in original) {
            require(roomById[old.id] == old) {
                "Original owner's Room photo metadata is missing or changed"
            }
            val (file,pending) = files(old)
            check(!Files.exists(pending.toPath(), noLinks) &&
                verified(file,old.sha256Hex)) {
                "Original owner JPEG failed independent byte authentication"
            }
        }
        return B05PhotoCrashReport(
            target.filter { it.id !in originalById }.map { photo ->
                val (file,pending) = files(photo)
                val room = roomById[photo.id]
                val state = when {
                    Files.exists(pending.toPath(), noLinks) ->
                        B05ImportedPhotoCrashState.PENDING_COPY_REQUIRES_OWNER_REVIEW
                    room != null && room != photo ->
                        B05ImportedPhotoCrashState.CONFLICTING_ROOM_METADATA
                    Files.exists(file.toPath(), noLinks) && !verified(file,photo.sha256Hex) ->
                        B05ImportedPhotoCrashState.CORRUPT_OR_MISSING_VERIFIED_FILE
                    !Files.exists(file.toPath(), noLinks) && room != null ->
                        B05ImportedPhotoCrashState.CORRUPT_OR_MISSING_VERIFIED_FILE
                    !Files.exists(file.toPath(), noLinks) ->
                        B05ImportedPhotoCrashState.MISSING_FILE_AND_ROOM_ROW
                    room == null ->
                        B05ImportedPhotoCrashState.VERIFIED_FILE_MISSING_ROOM_ROW
                    else ->
                        B05ImportedPhotoCrashState.VERIFIED_FILE_AND_ROOM_ROW
                }
                B05PhotoCrashCandidate(photo.id,state)
            }
        )
    }
}
