package com.framebynavin.app.personal

import android.content.Context
import com.framebynavin.app.personal.spaces.V289SpaceState
import com.framebynavin.app.personal.spaces.V289SpacesDatabase
import com.framebynavin.app.personal.spaces.V289SpacesMetadataCodec
import com.framebynavin.app.personal.spaces.V289SpacesMetadataRecovery
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
        withContext(Dispatchers.IO) {
            val personal = PersonalBackupTransfer(app).exportForCloud()
            val challenges = V297ChallengeStore(app).exportJson()
            val groups = V298ReminderGroupStore(app).exportJson()
            val (spaces, photos) = readLocalSpacesAndPhotoSources()
            val domains = V300PersonalComponents(
                personal = personal, challenges = challenges, groups = groups, spaces = spaces,
            )
            V300EncryptedPersonalArchive.write(domains, photos, password, output)
        }

    /** Checks and stages the archive. A future restore needs separate owner consent. */
    suspend fun verify(
        password: CharArray, input: InputStream,
    ): V300PersonalArchivePreview = withContext(Dispatchers.IO) {
        V300EncryptedPersonalArchive.stageAndVerify(input, password, app.cacheDir).use { staged ->
            val reminders = PersonalCompletionBackupCodec.decode(staged.components.personal)
            val spaces = V289SpacesMetadataCodec.decode(staged.components.spaces)
            V300PersonalArchivePreview(
                reminders = reminders.reminders.size,
                completions = reminders.completions.size,
                challenges = V297ChallengeCodec.decode(staged.components.challenges).size,
                groups = V298ReminderGroupCodec.decode(staged.components.groups).size,
                spaces = spaces.spaces.size,
                journalEntries = spaces.entries.size,
                spaceLinks = spaces.links.size,
                verifiedPhotoFiles = staged.photos.size,
            )
        }
    }

    private suspend fun readLocalSpacesAndPhotoSources():
        Pair<String, List<V300PhotoSource>> {
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
            val metadata = V289SpacesMetadataRecovery(db).prepareLocalMetadataExport()
            val dao = db.spaces()
            val rows = dao.loadSpaces().flatMap { space ->
                dao.photosForSpace(space.id)
            }
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
                V300PhotoSource(
                    photo = validated,
                    sizeBytes = file.length(),
                    open = { file.inputStream() },
                )
            }
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
) {
    init {
        require(listOf(
            reminders, completions, challenges, groups, spaces,
            journalEntries, spaceLinks, verifiedPhotoFiles,
        ).all { it >= 0 })
    }

    val label: String get() =
        "$reminders Personal reminders, $completions Done receipts, " +
            "$challenges challenges, $groups groups, $spaces Spaces, " +
            "$journalEntries journal entries, $spaceLinks links, " +
            "$verifiedPhotoFiles verified photos."
}
