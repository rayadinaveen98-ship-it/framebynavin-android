package com.framebynavin.app.personal.spaces

import android.content.Context
import androidx.room.withTransaction
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * V293: A fail-closed PRE-FLIGHT for a future owner-consented encrypted backup.
 *
 * This class does not export bytes or communicate with cloud services. In
 * particular, successful validation is NOT a recoverable Backlot backup.
 * The eventual writer must verify image bytes AGAIN immediately before each
 * encrypted write, because files can change after this snapshot is taken.
 */
internal data class V293PhotoBackupCandidate(
    val metadataJson: String,
    val photos: List<V290SpacePhotoEntity>,
) {
    val photoCount: Int get() = photos.size
}

internal object V293PhotoBackupPolicy {
    const val MAX_PHOTOS = 256

    /**
     * Never allow an incomplete archive to masquerade as a complete backup.
     * Each JPEG must be valid, attached to an existing Space, unique, within
     * storage bounds and cryptographically verified on the source device.
     * Validation only; the given verifier MUST NOT write/delete any file.
     */
    fun validate(
        metadataJson: String,
        photos: List<V290SpacePhotoEntity>,
        verifyPrivateCopy: (V290SpacePhotoEntity) -> Boolean,
    ): V293PhotoBackupCandidate {
        val state = V289SpacesMetadataCodec.decode(metadataJson)
        require(photos.size <= MAX_PHOTOS) {
            "Too many private photos for the initial portable backup format"
        }
        val spaces = state.spaces.mapTo(HashSet()) { it.id }
        val ids = HashSet<String>()
        val spaceNames = HashSet<Pair<String, String>>()
        val ordered = photos.sortedWith(
            compareBy<V290SpacePhotoEntity> { it.spaceId }
                .thenBy { it.takenAtMillis }.thenBy { it.id }
        )
        for (photo in ordered) {
            V290PhotoTimelinePolicy.validate(photo)
            require(photo.fileName == "${photo.id}.jpg") {
                "Private photo name differs from its verified identity"
            }
            require(photo.spaceId in spaces) {
                "Private photo refers to a missing Space"
            }
            require(ids.add(photo.id)) {
                "Duplicate private photo id"
            }
            require(spaceNames.add(photo.spaceId to photo.fileName)) {
                "Duplicate private photo name"
            }
            check(verifyPrivateCopy(photo)) {
                "A private photo is missing or changed: backup cannot proceed"
            }
        }
        return V293PhotoBackupCandidate(metadataJson, ordered)
    }
}

/**
 * Read-only owner-invoked local inspection. Not called by Home or by cloud
 * sync. The existing metadata export remains unchanged and intentionally
 * excludes confirmed-Done receipts; it is not a replacement for Personal sync.
 */
internal class V293PhotoBackupPreflight(
    context: Context,
    private val db: V289SpacesDatabase,
) {
    private val app = context.applicationContext
    suspend fun inspect(): V293PhotoBackupCandidate = withContext(Dispatchers.IO) {
        // Capture metadata + photo rows in one Room transaction. File bytes
        // are verified afterwards without holding a long database write lock.
        val (metadata, photos) = db.withTransaction {
            V289SpacesMetadataRecovery(db).prepareLocalMetadataExport() to
                db.spaces().allPhotos()
        }
        try {
            V293PhotoBackupPolicy.validate(metadata, photos) { row ->
                V290PrivatePhotoFiles.verifyPrivateBytes(app, row)
            }
        } catch (cancel: CancellationException) {
            throw cancel
        }
    }
}
