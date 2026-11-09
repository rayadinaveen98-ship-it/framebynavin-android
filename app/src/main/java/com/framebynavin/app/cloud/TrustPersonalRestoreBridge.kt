package com.framebynavin.app.cloud

import android.content.Context
import com.framebynavin.app.personal.PersonalCompletionBackupCodec
import com.framebynavin.app.personal.Phase1HabitCodec
import com.framebynavin.app.personal.Phase1V2ArchiveDryRunPolicy
import com.framebynavin.app.personal.Phase1V2ArchiveDryRunReport
import com.framebynavin.app.personal.StabilizationPersonalMutationGate
import com.framebynavin.app.personal.V300EncryptedPersonalArchive
import com.framebynavin.app.personal.V300PersonalComponents
import com.framebynavin.app.personal.V300PhotoSource
import com.framebynavin.app.personal.V301LocalRecoveryReader
import com.framebynavin.app.personal.spaces.V290PhotoTimelinePolicy
import com.framebynavin.app.personal.spaces.V290SpacePhotoEntity
import java.io.File
import org.json.JSONArray

/**
 * Source-level bridge to the ALREADY EXISTING journaled, owner-approved v2
 * Personal restore authority. No new Personal writer or second alert scheduler.
 *
 * Rebuilds a standard encrypted V300 ZIP from authenticated Trust parts, so
 * reminders + exact Done receipts, 21-day challenges, Groups, Space notes,
 * links and original private photos can use the proven Personal importer.
 *
 * Neither preview nor prepare installs data. The eventual universal restore
 * coordinator must own the Creator+Personal pre-restore escrow and invoke the
 * existing Phase1V2OwnerRestoreCoordinator only after explicit owner approval.
 */
internal class TrustPersonalRestoreBridge(context: Context) {
    private val app = context.applicationContext

    internal data class PersonalCandidate(
        val components: V300PersonalComponents,
        val photos: List<V290SpacePhotoEntity>,
        val verifiedPhotoParts: List<TrustVerifiedStagingPart>,
    )

    fun extractVerified(staged: TrustVerifiedStagedBundle): PersonalCandidate {
        val sources = staged.parts.associateBy { it.id }
        require(sources.size == staged.parts.size)
        fun text(id: String): String {
            val part = requireNotNull(sources[id]) {
                "Missing authenticated Personal source: $id"
            }
            require(part.sizeBytes <= 24L * 1024 * 1024)
            return part.file.readText(Charsets.UTF_8)
        }
        val components = V300PersonalComponents(
            personal = text("personal/reminders-history"),
            challenges = text("personal/legacy-challenges"),
            groups = text("personal/reminder-groups"),
            spaces = text("personal/spaces-journal-links"),
            singleActivityChallenges = text("personal/single-activity-receipts"),
        )
        val reminders = PersonalCompletionBackupCodec.decode(components.personal)
        Phase1HabitCodec.decode(requireNotNull(components.singleActivityChallenges))
        val rawPhotos = JSONArray(text("personal/photo-metadata"))
        require(rawPhotos.length() <= 500)
        val photos = (0 until rawPhotos.length()).map { i ->
            val row = rawPhotos.getJSONObject(i)
            V290PhotoTimelinePolicy.validate(
                V290SpacePhotoEntity(
                    id = row.getString("id"),
                    spaceId = row.getString("spaceId"),
                    takenAtMillis = row.getLong("takenAtMillis"),
                    addedAtMillis = row.getLong("addedAtMillis"),
                    fileName = row.getString("fileName"),
                    sha256Hex = row.getString("sha256Hex"),
                    widthPx = row.getInt("widthPx"),
                    heightPx = row.getInt("heightPx"),
                    note = row.getString("note"),
                ),
            )
        }
        check(photos.map { it.id }.distinct().size == photos.size)
        val byteSources = photos.sortedBy { it.id }.mapIndexed { i, photo ->
            val key = "personal/photo-" + i.toString().padStart(4, '0')
            requireNotNull(sources[key]) {
                "Private original JPEG missing: $key"
            }.also { part ->
                require(part.sha256 == photo.sha256Hex && part.records == 1) {
                    "Private photo metadata and original ciphertext do not match"
                }
            }
        }
        require(staged.parts.count {
            it.id.startsWith("personal/photo-") && it.id != "personal/photo-metadata"
        } == photos.size) { "Unexpected private photo source" }
        // The V300 codec authenticates Space referential integrity separately.
        require(reminders.reminders.size >= 0)
        return PersonalCandidate(components, photos.sortedBy { it.id }, byteSources)
    }

    /** Read-only compare of verified incoming Personal data with this phone. */
    suspend fun previewAdditions(
        staged: TrustVerifiedStagedBundle,
    ): Phase1V2ArchiveDryRunReport =
        StabilizationPersonalMutationGate.withStableSnapshot(app) {
            val local = V301LocalRecoveryReader(app).readV2()
            check(local.localIntegrityVerified) {
                "An existing Personal recovery journal needs owner attention"
            }
            val incoming = extractVerified(staged)
            Phase1V2ArchiveDryRunPolicy.compare(
                local, incoming.components, incoming.photos,
            )
        }

    /**
     * Private, temporary ENCRYPTED v2 payload for the existing v2 restore
     * coordinator. Always verifies a decrypted readback. Caller owns the
     * returned temporary file and MUST erase it after the journaled import.
     */
    fun prepareEncryptedV2Candidate(
        staged: TrustVerifiedStagedBundle,
        v2Passphrase: CharArray,
    ): File {
        require(v2Passphrase.size in 12..512) {
            "The owner must provide an encrypted Personal restore passphrase"
        }
        val candidate = extractVerified(staged)
        val file = File.createTempFile("trust-personal-v2-candidate-", ".zip", app.cacheDir)
        try {
            val photoSources = candidate.photos.zip(candidate.verifiedPhotoParts)
                .map { (photo, part) ->
                    V300PhotoSource(
                        photo = photo,
                        sizeBytes = part.sizeBytes,
                        open = { part.file.inputStream() },
                    )
                }
            file.outputStream().buffered().use { output ->
                V300EncryptedPersonalArchive.write(
                    candidate.components, photoSources, v2Passphrase, output,
                )
            }
            V300EncryptedPersonalArchive.stageAndVerify(
                file.inputStream().buffered(), v2Passphrase, app.cacheDir,
            ).use { verified ->
                require(verified.components == candidate.components &&
                    verified.photos.size == candidate.photos.size &&
                    verified.photos.map { it.photo } == candidate.photos) {
                    "Repacked Personal ZIP did not match its authenticated input"
                }
            }
            return file
        } catch (failure: Throwable) {
            if (!file.delete() && file.exists()) failure.addSuppressed(
                IllegalStateException("Could not erase the unverified Personal candidate"),
            )
            throw failure
        }
    }
}
