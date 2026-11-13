package com.framebynavin.app.personal

import android.content.Context
import com.framebynavin.app.personal.spaces.V289SpaceState
import com.framebynavin.app.personal.spaces.V289SpacesDatabase
import com.framebynavin.app.personal.spaces.V289SpacesMetadataCodec
import com.framebynavin.app.personal.spaces.V290PhotoTimelinePolicy
import java.io.File
import java.io.InputStream
import java.io.OutputStream
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Explicit export data source for the V300 archive format.
 *
 * This is deliberately NOT called by the existing Sync Now implementation.
 * Nothing is scheduled, removed or restored. No owner-facing export button
 * is enabled before a tested preview and recovery flow exist.
 */
internal class V300PersonalArchiveSource(context: Context) {
    private val app = context.applicationContext

    suspend fun export(password: CharArray, output: OutputStream) =
        StabilizationPersonalMutationGate.withStableSnapshot(app) {
        withContext(Dispatchers.IO) {
            // Versioned v2 archive includes EXACT single-activity Done instants;
            // it is still a preview/export codec, not an approved restore path.
            val singleActivityChallenges = Phase1HabitStore(app).exportJson()
            val personal = PersonalBackupTransfer(app).exportForCloud()
            val challenges = V297ChallengeStore(app).exportJson()
            val groups = V298ReminderGroupStore(app).exportJson()
            val (spaces, photos) = readLocalSpacesAndPhotoSources(
                PersonalCompletionBackupCodec.decode(personal))
            val domains = V300PersonalComponents(
                personal = personal, challenges = challenges, groups = groups, spaces = spaces,
                singleActivityChallenges = singleActivityChallenges,
            )
            // H3 Task 5: selected transfer bytes must contain neither
            // active Android delivery leases nor source-device music grants.
            // This must NOT be applied to V300's exact ORIGINAL restore escrow.
            V382UnifiedPortableSelectedPolicy.inspectSelected(domains)
            V300EncryptedPersonalArchive.write(domains, photos, password, output)
        }
    }

    /** Checks and stages the archive. A future restore needs separate owner consent. */
    suspend fun verify(
        password: CharArray,
        input: InputStream,
        requireUnifiedPortable: Boolean = false,
    ): V300PersonalArchivePreview = withContext(Dispatchers.IO) {
        V300EncryptedPersonalArchive.stageAndVerify(input, password, app.cacheDir).use { staged ->
            if (requireUnifiedPortable) {
                V382UnifiedPortableSelectedPolicy.inspectSelected(staged.components)
            }
            val reminders = PersonalCompletionBackupCodec.decode(staged.components.personal)
            val spaces = V303SpaceReceiptArchiveCodec.decode(staged.components.spaces, reminders)
            val singleActivity = staged.components.singleActivityChallenges?.let {
                Phase1HabitCodec.decode(it)
            }.orEmpty()
            V300PersonalArchivePreview(
                reminders = reminders.reminders.size,
                completions = reminders.completions.size,
                challenges = V297ChallengeCodec.decode(staged.components.challenges).size,
                groups = V298ReminderGroupCodec.decode(staged.components.groups).size,
                spaces = spaces.spaces.size,
                journalEntries = spaces.entries.size,
                spaceLinks = spaces.links.size,
                verifiedPhotoFiles = staged.photos.size,
                singleActivityChallenges = singleActivity.size,
                singleActivityDoneReceipts = singleActivity.sumOf { it.confirmations.size },
            )
        }
    }

    private suspend fun readLocalSpacesAndPhotoSources(
        personal: PersonalCompletionBackupSnapshot,
    ): Pair<String, List<V300PhotoSource>> {
        val existing = app.getDatabasePath(V289SpacesDatabase.FILE_NAME)
        val photoRoot = File(app.filesDir, "backlot-personal-space-photos")
        if (!existing.exists()) {
            // An orphan photo directory is not proof of an empty Personal space.
            check(!photoRoot.exists()) {
                "Local Space photo files exist without their database; export stopped"
            }
            return V289SpacesMetadataCodec.encode(V289SpaceState()) to emptyList()
        }
        // Explicit export may open a legitimate older v1 Room database with
        // its normal, non-destructive v1->v2 migration. It never creates a
        // database when none exists. This is NOT an implicit app-home action.
        val db = V289SpacesDatabase.open(app)
        try {
            val snapshot = V303SpaceSnapshotReader(db).readWithPhotos()
            val metadata = V303SpaceReceiptArchiveCodec.encode(
                snapshot.state, personal)
            val rows = snapshot.photos
            check(rows.size <= 500) { "Too many private photos for one archive" }
            val expectedFiles = HashSet<String>()
            val inputs = rows.map { photo ->
                val validated = V290PhotoTimelinePolicy.validate(photo)
                val root = photoRoot.canonicalFile
                val spaceFolder = File(root, validated.spaceId).canonicalFile
                val file = File(spaceFolder, validated.fileName).canonicalFile
                check(spaceFolder.parentFile == root &&
                    file.parentFile == spaceFolder &&
                    file.name == validated.fileName) {
                    "Unsafe private photo file location"
                }
                check(file.isFile && file.canRead()) {
                    "A private photo is missing; no complete archive was produced"
                }
                check(expectedFiles.add(file.absolutePath)) {
                    "Duplicate private photo file reference"
                }
                V300PhotoSource(
                    photo = validated,
                    sizeBytes = file.length(),
                    open = { file.inputStream() },
                )
            }
            // Do not silently exclude orphan JPEGs or interrupted .partial files.
            // They may contain the owner's only recoverable private photos.
            val recordedFiles = HashSet<String>()
            if (photoRoot.exists()) {
                for (file in photoRoot.walkTopDown()) {
                    if (!file.isFile) continue
                    require(recordedFiles.size < 501) {
                        "Unexpected number of private photo files"
                    }
                    recordedFiles.add(file.canonicalPath)
                }
            }
            V300PrivatePhotoInventoryPolicy.requireExactFiles(expectedFiles, recordedFiles)
            return metadata to inputs
        } finally {
            db.close()
        }
    }
}

internal data class V300PersonalArchivePreview(
    val reminders: Int,
    val completions: Int,
    val challenges: Int,
    val groups: Int,
    val spaces: Int,
    val journalEntries: Int,
    val spaceLinks: Int,
    val verifiedPhotoFiles: Int,
    val singleActivityChallenges: Int = 0,
    val singleActivityDoneReceipts: Int = 0,
) {
    init {
        require(listOf(
            reminders, completions, challenges, groups, spaces,
            journalEntries, spaceLinks, verifiedPhotoFiles,
            singleActivityChallenges, singleActivityDoneReceipts,
        ).all { it >= 0 })
    }

    val label: String get() =
        "$reminders Personal reminders, $completions Done receipts, " +
            "$challenges challenges, $groups groups, $spaces Spaces, " +
            "$journalEntries journal entries, $spaceLinks links, " +
            "$verifiedPhotoFiles verified photos, " +
            "$singleActivityChallenges single-activity challenges, " +
            "$singleActivityDoneReceipts challenge Done receipts."
}

/**
 * Missing files, extra files, and untracked interrupted writes all block a
 * purported complete archive. No private paths are printed in error copy.
 */
internal object V300PrivatePhotoInventoryPolicy {
    fun requireExactFiles(expected: Set<String>, onDisk: Set<String>) {
        require(expected == onDisk) {
            "Private photo inventory does not match its records. " +
                "Resolve missing or untracked photos before exporting."
        }
    }
}
