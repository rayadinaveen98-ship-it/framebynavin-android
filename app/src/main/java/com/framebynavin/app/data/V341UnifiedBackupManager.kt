package com.framebynavin.app.data

import android.content.Context
import com.framebynavin.app.personal.V300PersonalArchivePreview
import com.framebynavin.app.personal.Phase1V2ArchiveDryRun
import com.framebynavin.app.personal.Phase1V2ArchiveDryRunReport
import com.framebynavin.app.personal.V300PersonalArchiveSource
import java.io.File
import java.io.InputStream
import java.io.OutputStream
import java.nio.file.Files
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * The existing portable Creator and encrypted Personal codecs remain canonical.
 * This manager creates ONE owner-selected, passphrase-protected transfer file,
 * with all current supported owned media bytes preserved in its two sections.
 *
 * It does not represent either backup as a full snapshot of external project
 * attachments, third-party account credentials, or content-provider-only
 * URIs. Those are not app-owned media and cannot be copied without consent.
 *
 * The two independent source snapshots are produced sequentially. Do not
 * claim an app-wide atomic instant or enable a combined restore before a
 * separate two-domain write-ahead recovery protocol is verified.
 */
internal class V341UnifiedBackupManager(context: Context) {
    private val app = context.applicationContext
    private val creator = PortableCreatorBackupManager(app)
    private val personal = V300PersonalArchiveSource(app)

    suspend fun export(passphrase: CharArray, output: OutputStream): V341UnifiedBackupPreview =
        withContext(Dispatchers.IO) {
            require(passphrase.size >= 12) { "Use a passphrase with at least 12 characters" }
            runCatching { V341UnifiedTempCleanup.prune(app.cacheDir) }
            val staging = createStageDirectory("backlot-unified-export-")
            try {
                // Creator and Personal are captured sequentially. Refuse an
                // apparently complete export when their source records drift
                // between the start and end of the two-domain read. This is
                // a fail-closed consistency check, not an atomic app-wide
                // snapshot or a replacement for staged-media SHA validation.
                val baselineReader = V343UnifiedBaselineReader(app)
                val beforeSnapshot = baselineReader.capture()
                val creatorFile = File(staging, "creator.fbnbackup")
                val personalFile = File(staging, "personal-encrypted.zip")

                creatorFile.outputStream().buffered().use { output ->
                    creator.writeBackup(output)
                }
                // One password is chosen by the owner, with INDEPENDENT salt
                // and authenticated encryption for Personal's inner archive.
                personalFile.outputStream().buffered().use {
                    personal.export(passphrase, it)
                }

                val creatorPreview = creatorFile.inputStream().buffered().use(creator::validate)
                val personalPreview = personalFile.inputStream().buffered().use {
                    personal.verify(passphrase, it, requireUnifiedPortable = true)
                }
                check(V343UnifiedBaselinePolicy.noDrift(
                    beforeSnapshot, baselineReader.capture(),
                )) {
                    "Creator or Personal data changed during unified backup; retry the export"
                }
                V341UnifiedOwnerArchive.write(
                    creatorFile, personalFile, passphrase, output,
                )
                V341UnifiedBackupPreview(
                    creatorPreview, personalPreview,
                    creatorFile.length(), personalFile.length(),
                )
            } finally {
                staging.deleteRecursively()
            }
        }

    /**
     * Read every encrypted chunk and independently authenticate BOTH canonical
     * formats. No live Creator/Personal data, schedules or photos are modified.
     */
    suspend fun verify(passphrase: CharArray, input: InputStream): V341UnifiedBackupPreview =
        withContext(Dispatchers.IO) {
            runCatching { V341UnifiedTempCleanup.prune(app.cacheDir) }
            V341UnifiedOwnerArchive.stageAndVerify(input, passphrase, app.cacheDir).use { staged ->
                val creatorPreview = staged.creator.inputStream().buffered().use(creator::validate)
                val personalPreview = staged.personal.inputStream().buffered().use {
                    personal.verify(passphrase, it, requireUnifiedPortable = true)
                }
                V341UnifiedBackupPreview(
                    creatorPreview, personalPreview,
                    staged.creatorBytes, staged.personalBytes,
                )
            }
        }

    /**
     * Restore-planning is READ ONLY. After authenticating both parts, it
     * compares incoming Personal records/photos with the CURRENT phone using
     * the existing v2 nine-domain dry-run engine. Creator data is displayed
     * as a replacement preview only. Never return a write authorization.
     */
    suspend fun inspectRestore(
        passphrase: CharArray,
        input: InputStream,
    ): V341UnifiedRestoreInspection = withContext(Dispatchers.IO) {
        runCatching { V341UnifiedTempCleanup.prune(app.cacheDir) }
        V341UnifiedOwnerArchive.stageAndVerify(input, passphrase, app.cacheDir).use { staged ->
            val creatorPreview = staged.creator.inputStream().buffered().use(creator::validate)
            val personalPreview = staged.personal.inputStream().buffered().use {
                personal.verify(passphrase, it, requireUnifiedPortable = true)
            }
            val personalDiff = staged.personal.inputStream().buffered().use {
                Phase1V2ArchiveDryRun(app).check(it, passphrase)
            }
            V341UnifiedRestoreInspection(
                backup = V341UnifiedBackupPreview(
                    creatorPreview, personalPreview, staged.creatorBytes, staged.personalBytes,
                ),
                personalDiff = personalDiff,
            )
        }
    }

    private fun createStageDirectory(prefix: String): File {
        require(app.cacheDir.isDirectory) { "Private staging cache is not available" }
        return Files.createTempDirectory(app.cacheDir.toPath(), prefix).toFile()
    }
}

internal data class V341UnifiedBackupPreview(
    val creator: CreatorBackupManager.BackupPreview,
    val personal: V300PersonalArchivePreview,
    val creatorBytes: Long,
    val personalBytes: Long,
)

/** No code in this data object permits a unified write or scheduled alarm. */
internal data class V341UnifiedRestoreInspection(
    val backup: V341UnifiedBackupPreview,
    val personalDiff: Phase1V2ArchiveDryRunReport,
) {
    val canCommitUnifiedRestore: Boolean get() = false
}
