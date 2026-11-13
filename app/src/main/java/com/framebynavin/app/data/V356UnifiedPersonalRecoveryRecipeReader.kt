package com.framebynavin.app.data

import android.content.Context
import com.framebynavin.app.personal.V300EncryptedPersonalArchive
import com.framebynavin.app.personal.V300PersonalComponents
import com.framebynavin.app.personal.spaces.V290SpacePhotoEntity
import java.io.File
import java.nio.file.Files
import java.nio.file.LinkOption
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Reconstruct the exact ADDITIVE Personal target from two AUTHENTICATED sealed
 * unified ZIPs, never from damaged live Personal/Room/photo state.
 *
 * A crash can leave Personal partially changed. The ORIGINAL archive carries
 * a copy of the previously preserved owner records; the incoming SELECTED
 * archive may add compatible records. The merged target must therefore be
 * computed from ORIGINAL + SELECTED, and not from the damaged current device.
 *
 * This stage does not create an owner's recovery direction or any write
 * permission. It does not copy, restore, remove, replace, rename, or schedule
 * an actual app record. Temporary decrypted files are cleaned by use().
 */
internal class V356UnifiedPersonalRecoveryRecipeReader(context: Context) {
    private val app = context.applicationContext
    private val journal = V344UnifiedRestoreJournal(app)

    suspend fun inspect(
        ownerConfirmedReview: Boolean,
        passphrase: CharArray,
    ): V356UnifiedPersonalRecoveryReceipt {
        require(ownerConfirmedReview && passphrase.size >= 12) {
            "Fresh owner consent and encrypted owner archive passphrase required"
        }
        val authenticated = V346UnifiedInterruptedRecoveryReview(app).inspect(
            ownerConfirmedReview = true,
            passphrase = passphrase,
        )
        return inspectVerified(authenticated, passphrase)
    }

    /**
     * Reuse the already passphrase-authenticated V346 proof when called from
     * the live crash review screen. No proof can grant write permission.
     */
    internal suspend fun inspectVerified(
        prior: V346InterruptedRecoveryProof,
        passphrase: CharArray,
    ): V356UnifiedPersonalRecoveryReceipt = withContext(Dispatchers.IO) {
        require(passphrase.size >= 12) { "Encrypted owner archive passphrase required" }
        val checkpoint = journal.requireCheckpointReadOnly()
        check(V346InterruptedRecoveryProofPolicy.matches(prior, checkpoint)) {
            "Original+target archive proof does not match active combined journal"
        }
        val authenticated = prior.sealedArchives

        // Process the two large encrypted ZIPs in sequence to keep peak disk
        // usage bounded. Only retain immutable component text and photo ROWS
        // in memory; NEVER retain staged raw JPEG files or write any owner data.
        val original = readPersonalSection(
            "backlot-unified-owner-recovery-escrow-v1",
            "original-owner-encrypted-unified.zip",
            passphrase,
        )
        val incoming = readPersonalSection(
            "backlot-unified-owner-restore-preparation-v1",
            "incoming-unified-encrypted.zip",
            passphrase,
        )
        val merged = V355UnifiedPersonalRecoveryRecipePolicy.build(
            original.components, original.photos,
            incoming.components, incoming.photos,
        )
        check(journal.requireCheckpointReadOnly() == checkpoint &&
            V345UnifiedAuthenticatedPairPolicy.matchesRecoveryCheckpoint(
                authenticated, checkpoint,
            )) {
            "Combined recovery source or checkpoint changed while deriving Personal recipe"
        }
        V356UnifiedPersonalRecoveryReceipt(
            checkpointId = checkpoint.transactionId,
            selectedArchiveSha256 = authenticated.sourceSha256,
            originalArchiveSha256 = authenticated.originalEscrowSha256,
            personalRecipe = merged,
            noWritePermission = true,
        )
    }

    private data class PersonalSection(
        val components: V300PersonalComponents,
        val photos: List<V290SpacePhotoEntity>,
    )

    private fun readPersonalSection(
        folder: String,
        filename: String,
        passphrase: CharArray,
    ): PersonalSection {
        val root = File(app.filesDir, folder)
        check(root.isDirectory && !Files.isSymbolicLink(root.toPath()) &&
            root.canonicalFile.parentFile == app.filesDir.canonicalFile) {
            "Owner archive directory is missing or untrusted"
        }
        val file = File(root, filename)
        check(file.isFile && !Files.isSymbolicLink(file.toPath()) &&
            file.canonicalFile.parentFile == root.canonicalFile) {
            "Owner archive source file is missing or untrusted"
        }
        return file.inputStream().buffered().use { source ->
            V341UnifiedOwnerArchive.stageAndVerify(
                source, passphrase, app.cacheDir,
            ).use { verified ->
                verified.personal.inputStream().buffered().use { personalInput ->
                    V300EncryptedPersonalArchive.stageAndVerify(
                        personalInput, passphrase, app.cacheDir,
                    ).use { inner ->
                        PersonalSection(
                            components = inner.components,
                            photos = inner.photos.map { it.photo },
                        )
                    }
                }
            }
        }
    }
}

internal data class V356UnifiedPersonalRecoveryReceipt(
    val checkpointId: String,
    val selectedArchiveSha256: String,
    val originalArchiveSha256: String,
    val personalRecipe: V355UnifiedPersonalRecoveryRecipe,
    val noWritePermission: Boolean,
) {
    val canCommit: Boolean get() = false
    val canRollback: Boolean get() = false
    init {
        require(noWritePermission && personalRecipe.noMutationAuthority &&
            personalRecipe.allOriginalPhotoReferencesPreserved) {
            "A Personal recovery recipe is never a live data write permit"
        }
    }
}
