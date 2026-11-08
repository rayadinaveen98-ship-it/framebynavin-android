package com.framebynavin.app.personal

import com.framebynavin.app.personal.spaces.V290PhotoTimelinePolicy
import com.framebynavin.app.personal.spaces.V290SpacePhotoEntity
import java.io.File
import java.nio.file.Files
import java.nio.file.LinkOption

/**
 * Independent, READ-ONLY evidence for the v2 photo write stage.
 *
 * The checkpoint's authentic original/target SHA and the remaining five
 * stores MUST have already been independently verified by the caller. This
 * policy validates metadata against real app-private bytes, including files
 * installed ahead of the Room insert and interrupted .restore.pending copies.
 * It NEVER cleans or adopts those files, completes recovery or grants authority.
 *
 * Run under the global owner recovery inspection mutex, against a stable
 * journal; a filesystem inspection alone cannot authorize a live restore.
 */
internal data class Phase1V2PhotoByteEvidence(
    val checkpoint: Phase1V2RestoreStep,
    val verifiedFinalFiles: Int,
    val verifiedNewFiles: Int,
    val pendingCopies: Int,
    val observedRecords: Int,
    val requiresOwnerReview: Boolean,
) {
    val mayWriteLiveData: Boolean get() = false
    val mayAutoResume: Boolean get() = false
    val mayDeleteAnyFile: Boolean get() = false
    val mayReleaseJournal: Boolean get() = false
}

internal object Phase1V2PhotoByteForensics {
    private const val MAX_RECORDS = 10_000
    private val noFollow = LinkOption.NOFOLLOW_LINKS

    fun inspect(
        checkpoint: Phase1V2RestoreStep,
        privatePhotoRoot: File,
        baseline: List<V290SpacePhotoEntity>,
        target: List<V290SpacePhotoEntity>,
        observed: List<V290SpacePhotoEntity>,
    ): Phase1V2PhotoByteEvidence {
        fun index(rows: List<V290SpacePhotoEntity>): Map<String, V290SpacePhotoEntity> {
            require(rows.size <= MAX_RECORDS) { "Too many private photos to inspect" }
            rows.forEach { V290PhotoTimelinePolicy.validate(it) }
            require(rows.map { it.id }.toSet().size == rows.size &&
                rows.map { it.spaceId to it.fileName }.toSet().size == rows.size) {
                "Duplicate private photo ID or destination path"
            }
            return rows.associateBy { it.id }
        }
        val previous = index(baseline)
        val intended = index(target)
        val current = index(observed)
        require(previous.all { (id, photo) -> intended[id] == photo }) {
            "V2 target cannot silently remove or rewrite an existing private photo"
        }
        require(current.all { (id, photo) -> intended[id] == photo } &&
            previous.all { (id, photo) -> current[id] == photo }) {
            "Observed photo records differ from authenticated original or target"
        }
        val atPhotoStage = checkpoint == Phase1V2RestoreStep.BEFORE_PHOTOS
        val beforePhotoStage = checkpoint.ordinal <
            Phase1V2RestoreStep.BEFORE_PHOTOS.ordinal
        if (beforePhotoStage) {
            require(current == previous) {
                "Private photo Room rows changed before durable v2 photo checkpoint"
            }
        } else if (!atPhotoStage) {
            require(current == intended) {
                "Private photo Room rows missing after v2 photo checkpoint"
            }
        }

        val verified = HashSet<String>()
        val pending = HashSet<String>()
        val destinations = intended.values.associateBy { it.spaceId + "/" + it.fileName }
        val interrupted = intended.values.filter { it.id !in previous }
            .associateBy { it.spaceId + "/." + it.id + ".restore.pending" }
        val validSpaces = intended.values.mapTo(HashSet()) { it.spaceId }
        val rootPath = privatePhotoRoot.toPath()
        if (Files.exists(rootPath, noFollow)) {
            require(Files.isDirectory(rootPath, noFollow) &&
                !Files.isSymbolicLink(rootPath)) {
                "Private photo root has unsafe file type"
            }
            for (space in requireNotNull(privatePhotoRoot.listFiles()) {
                "Cannot enumerate private photo root"
            }) {
                require(Files.isDirectory(space.toPath(), noFollow) &&
                    !Files.isSymbolicLink(space.toPath()) &&
                    space.name in validSpaces) {
                    "Unexpected private Space directory or symbolic link"
                }
                for (file in requireNotNull(space.listFiles()) {
                    "Cannot enumerate private Space photo files"
                }) {
                    require(Files.isRegularFile(file.toPath(), noFollow) &&
                        !Files.isSymbolicLink(file.toPath())) {
                        "Private photo is a link, folder or nonregular file"
                    }
                    val relative = space.name + "/" + file.name
                    val photo = destinations[relative]
                    if (photo != null) {
                        require(!beforePhotoStage || photo.id in previous) {
                            "New photo bytes appeared before durable photo stage"
                        }
                        require(file.length() in 1L..V299PhotoIntegrityPolicy.MAX_BYTES &&
                            file.inputStream().use {
                                V299PhotoIntegrityPolicy.matches(it, photo.sha256Hex)
                            }) {
                            "Private photo bytes do not match authenticated SHA-256"
                        }
                        require(verified.add(relative)) { "Duplicate final photo path" }
                    } else {
                        val unfinished = interrupted[relative]
                        require(atPhotoStage && unfinished != null &&
                            file.length() in 0L..V299PhotoIntegrityPolicy.MAX_BYTES &&
                            pending.add(relative)) {
                            "Unexpected or out-of-stage recovery photo file"
                        }
                    }
                }
            }
        }
        require(previous.values.all {
            it.spaceId + "/" + it.fileName in verified
        }) { "Original owner photo bytes missing or unverified" }
        require(current.values.all {
            it.spaceId + "/" + it.fileName in verified
        }) { "Room photo row points to incomplete or missing JPEG bytes" }
        if (!beforePhotoStage && !atPhotoStage) {
            require(pending.isEmpty() && intended.values.all {
                it.spaceId + "/" + it.fileName in verified
            }) { "Photo restore has not completed its verified byte stage" }
        }
        return Phase1V2PhotoByteEvidence(
            checkpoint = checkpoint,
            verifiedFinalFiles = verified.size,
            verifiedNewFiles = intended.values.count {
                it.id !in previous && it.spaceId + "/" + it.fileName in verified
            },
            pendingCopies = pending.size,
            observedRecords = current.size,
            requiresOwnerReview = atPhotoStage || pending.isNotEmpty(),
        )
    }
}
