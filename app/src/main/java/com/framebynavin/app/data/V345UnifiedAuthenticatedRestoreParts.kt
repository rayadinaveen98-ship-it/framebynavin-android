package com.framebynavin.app.data

import android.content.Context
import com.framebynavin.app.personal.V300PersonalArchiveSource
import java.io.File
import java.nio.file.Files
import java.nio.file.LinkOption
import java.security.MessageDigest
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * H3 read-only evidence for BOTH owner-retained unified archives.
 *
 * The selected TARGET and independently encrypted ORIGINAL escrow are sealed
 * ZIPs. A successful V345 proof authenticates every outer AES-GCM chunk and
 * SHA-256 digest, then independently verifies the embedded Creator portable
 * archive (including Voice Idea recordings and Best Frames) and the Personal
 * encrypted archive (including linked Space photos and Done receipts).
 *
 * The result deliberately exposes ONLY immutable SHA/count evidence; never
 * plaintext Files, recovery write permits or a Restore All callable.
 */
internal class V345UnifiedAuthenticatedRestoreParts(context: Context) {
    private val app = context.applicationContext
    private val source = V342UnifiedRestorePreparationStore(app)
    private val escrow = V343UnifiedRecoveryEscrow(app)
    private val creator = PortableCreatorBackupManager(app)
    private val personal = V300PersonalArchiveSource(app)

    suspend fun authenticate(
        ownerConfirmedReview: Boolean,
        passphrase: CharArray,
    ): V345UnifiedAuthenticatedPairProof =
        authenticateInternal(ownerConfirmedReview, passphrase, expectedRecovery = null)

    /**
     * Exceptionally, a READ-ONLY recovery inspector must authenticate the
     * original and target even though the combined journal freezes all normal
     * data-store reads. This method does not bypass either owner write gate;
     * it only opens the two sealed archive files in private storage.
     */
    suspend fun authenticateForInterruptedRecovery(
        ownerConfirmedReview: Boolean,
        passphrase: CharArray,
    ): V345UnifiedAuthenticatedPairProof {
        require(ownerConfirmedReview && passphrase.size >= 12) {
            "A fresh owner confirmation and passphrase are required"
        }
        val expected = V344UnifiedRestoreJournal(app).requireCheckpointReadOnly()
        return authenticateInternal(ownerConfirmedReview, passphrase, expected)
    }

    private suspend fun authenticateInternal(
        ownerConfirmedReview: Boolean,
        passphrase: CharArray,
        expectedRecovery: V344UnifiedRestoreCheckpoint?,
    ): V345UnifiedAuthenticatedPairProof = withContext(Dispatchers.IO) {
        require(ownerConfirmedReview && passphrase.size >= 12) {
            "Owner-selected review and 12+ character archive passphrase required"
        }
        requireExpectedRecovery(expectedRecovery)

        val selected = source.status() as?
            V342UnifiedRestorePreparationStore.Status.ChecksumMatched
            ?: error("Encrypted owner target is missing or changed")
        val original = escrow.inspectEscrowRecordReadOnly()
        check(selected.transactionId == original.selectedSourceId &&
            selected.sha256 == original.selectedSourceSha256) {
            "Escrow does not bind this exact selected target"
        }
        val targetFile = exactOwnerFile(
            "backlot-unified-owner-restore-preparation-v1",
            "incoming-unified-encrypted.zip",
        )
        val originalFile = exactOwnerFile(
            "backlot-unified-owner-recovery-escrow-v1",
            "original-owner-encrypted-unified.zip",
        )
        check(sha256(targetFile) == selected.sha256 &&
            sha256(originalFile) == original.rollbackSha256) {
            "An encrypted source changed after owner selection"
        }
        // Process one decrypted pair at a time. Holding BOTH unpacked
        // owner archives simultaneously can exceed private cache on modest
        // phones with large original photo and Voice Idea collections.
        val targetProof = targetFile.inputStream().buffered().use { input ->
            V341UnifiedOwnerArchive.stageAndVerify(input, passphrase, app.cacheDir)
        }.use { inspectCanonical(it, passphrase) }
        val originalProof = originalFile.inputStream().buffered().use { input ->
            V341UnifiedOwnerArchive.stageAndVerify(input, passphrase, app.cacheDir)
        }.use { inspectCanonical(it, passphrase) }

        requireExpectedRecovery(expectedRecovery)
        check(source.status() == selected &&
            escrow.inspectEscrowRecordReadOnly() == original) {
            "Recovery source or independent owner escrow changed during authentication"
        }
        val proof = V345UnifiedAuthenticatedPairProof(
            sourceId = selected.transactionId,
            sourceSha256 = selected.sha256,
            originalEscrowId = original.transactionId,
            originalEscrowSha256 = original.rollbackSha256,
            targetCreatorSha256 = targetProof.creatorSha,
            targetPersonalSha256 = targetProof.personalSha,
            originalCreatorSha256 = originalProof.creatorSha,
            originalPersonalSha256 = originalProof.personalSha,
            targetCreatorProjects = targetProof.projects,
            originalCreatorProjects = originalProof.projects,
            targetPersonalSummary = targetProof.personalSummary,
            originalPersonalSummary = originalProof.personalSummary,
            noMutationAuthority = true,
        )
        if (expectedRecovery != null) {
            check(V345UnifiedAuthenticatedPairPolicy.matchesRecoveryCheckpoint(
                proof, expectedRecovery,
            )) { "Retained encrypted sources do not match the interrupted restore journal" }
        }
        proof
    }

    /** Never reinterpret an unreadable journal as an absent/clean one. */
    private suspend fun requireExpectedRecovery(
        expected: V344UnifiedRestoreCheckpoint?,
    ) {
        if (expected == null) {
            check(!V344UnifiedRestoreJournal.hasPendingFor(app)) {
                "Interrupted restore requires the dedicated read-only recovery authentication"
            }
        } else {
            check(V344UnifiedRestoreJournal(app).requireCheckpointReadOnly() == expected) {
                "Combined recovery journal changed while authenticating owner archives"
            }
        }
    }

    private data class CanonicalInspection(
        val creatorSha: String,
        val personalSha: String,
        val projects: Int,
        val personalSummary: String,
    )

    private fun inspectCanonical(
        staged: V341UnifiedOwnerArchive.Verified,
        passphrase: CharArray,
    ): CanonicalInspection {
        val creatorPreview = staged.creator.inputStream().buffered().use(creator::validate)
        val personalPreview = staged.personal.inputStream().buffered().use {
            personal.verify(passphrase, it)
        }
        return CanonicalInspection(
            creatorSha = sha256(staged.creator),
            personalSha = sha256(staged.personal),
            projects = creatorPreview.projectCount,
            personalSummary = personalPreview.label,
        )
    }

    private fun exactOwnerFile(folder: String, filename: String): File {
        val root = File(app.filesDir, folder)
        check(root.isDirectory &&
            !Files.isSymbolicLink(root.toPath()) &&
            root.canonicalFile.parentFile == app.filesDir.canonicalFile) {
            "Untrusted encrypted archive directory"
        }
        val file = File(root, filename)
        check(file.isFile && !Files.isSymbolicLink(file.toPath()) &&
            file.canonicalFile.parentFile == root.canonicalFile) {
            "Untrusted encrypted archive file"
        }
        return file
    }

    private fun sha256(file: File): String {
        val md = MessageDigest.getInstance("SHA-256")
        file.inputStream().buffered().use { source ->
            val buf = ByteArray(64 * 1024)
            while (true) {
                val n = source.read(buf)
                if (n < 0) break
                if (n > 0) md.update(buf, 0, n)
            }
        }
        return md.digest().joinToString("") {
            "%02x".format(it.toInt() and 0xff)
        }
    }
}

internal data class V345UnifiedAuthenticatedPairProof(
    val sourceId: String,
    val sourceSha256: String,
    val originalEscrowId: String,
    val originalEscrowSha256: String,
    val targetCreatorSha256: String,
    val targetPersonalSha256: String,
    val originalCreatorSha256: String,
    val originalPersonalSha256: String,
    val targetCreatorProjects: Int,
    val originalCreatorProjects: Int,
    val targetPersonalSummary: String,
    val originalPersonalSummary: String,
    val noMutationAuthority: Boolean = true,
)

internal object V345UnifiedAuthenticatedPairPolicy {
    private val sha = Regex("[0-9a-f]{64}")
    fun matchesCheckpoint(
        pair: V345UnifiedAuthenticatedPairProof,
        candidate: V344UnifiedRestoreCheckpoint,
    ): Boolean =
        candidate.step == V344UnifiedRestoreStep.PREPARED_NO_WRITES &&
            matchesRecoveryCheckpoint(pair, candidate)

    /**
     * Source/escrow binding at ANY interrupted checkpoint. This is
     * authentication evidence only, never permission to continue writes.
     */
    fun matchesRecoveryCheckpoint(
        pair: V345UnifiedAuthenticatedPairProof,
        journal: V344UnifiedRestoreCheckpoint,
    ): Boolean =
        pair.noMutationAuthority &&
            runCatching { V344UnifiedRestorePolicy.validate(journal) }.isSuccess &&
            pair.sourceId == journal.sourceId &&
            pair.sourceSha256 == journal.sourceSha256 &&
            pair.originalEscrowId == journal.escrowId &&
            pair.originalEscrowSha256 == journal.escrowSha256 &&
            listOf(
                pair.targetCreatorSha256, pair.targetPersonalSha256,
                pair.originalCreatorSha256, pair.originalPersonalSha256,
            ).all(sha::matches)
}
