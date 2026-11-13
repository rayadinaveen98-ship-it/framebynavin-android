package com.framebynavin.app.data

import android.content.Context
import java.io.File
import java.nio.file.Files
import java.nio.file.LinkOption
import java.security.MessageDigest
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * H3 Task 2: authenticated, NO-WRITE Voice Idea crash provenance.
 *
 * A V365 allocation journal proves which paths were reserved before a
 * Creator import copied media. It does NOT independently prove those paths
 * were allocated for the audio in the owner-selected TARGET backup.
 *
 * This inspector reopens the exact encrypted TARGET and ORIGINAL archives,
 * authenticates both entire Creator/Personal pairs, independently validates
 * each inner portable Creator archive, and matches every planned Voice Idea
 * ID, length and SHA-256 against the TARGET media bytes. It then inventories
 * the exact installed copies. The ORIGINAL voice media is independently
 * verified as available for a future reverse-original recovery.
 *
 * No Creator/Personal records, existing recordings, Best Frames, journals,
 * alarms or cloud data are changed. Staged private-cache copies are removed.
 * A passing report is evidence for a future owner-reviewed recovery writer,
 * NEVER permission to replay, replace or delete media.
 */
internal data class V366VoiceBytes(
    val sizeBytes: Long,
    val sha256: String,
)

internal object V366CreatorVoiceRecoveryPolicy {
    private val hex = Regex("[a-f0-9]{64}")

    fun matchesTarget(
        plan: V365VoiceCopyPlan,
        authenticatedMedia: Map<String, V366VoiceBytes>,
    ): Boolean {
        if (plan.entries.size != authenticatedMedia.size ||
            plan.entries.map { it.ideaId }.toSet().size != plan.entries.size) return false
        return plan.entries.all { entry ->
            val actual = authenticatedMedia[entry.ideaId] ?: return@all false
            entry.ideaId.isNotBlank() &&
                entry.sizeBytes > 0 &&
                entry.sizeBytes == actual.sizeBytes &&
                hex.matches(entry.sha256) &&
                actual.sha256 == entry.sha256
        }
    }
}

internal data class V366CreatorVoiceRecoveryProof(
    val transactionId: String,
    val targetCreatorSha256: String,
    val originalCreatorSha256: String,
    val targetMediaCount: Int,
    val originalMediaCount: Int,
    /** Authenticated per-ID ORIGINAL media digests for native rollback preflight. */
    val originalMediaHashes: Map<String, V366VoiceBytes> = emptyMap(),
    val installedCopies: V365VoiceCopyAudit,
    val originalMediaAuthenticated: Boolean,
    val targetAllocationAuthenticated: Boolean,
    val noMutationAuthority: Boolean = true,
) {
    init {
        require(originalMediaAuthenticated && targetAllocationAuthenticated &&
            noMutationAuthority &&
            installedCopies.transactionId == transactionId &&
            installedCopies.planned == targetMediaCount &&
            originalMediaCount == originalMediaHashes.size) {
            "A source-only Voice Idea report cannot authorize recovery actions"
        }
    }
    val mayReplayCreator: Boolean get() = false
    val mayDeleteMedia: Boolean get() = false
    val mayReleaseOriginalEscrow: Boolean get() = false
}

internal class V366UnifiedCreatorVoiceRecoveryInspector(context: Context) {
    private val app = context.applicationContext
    private val h4 = V344UnifiedRestoreJournal(app)
    private val allocations = V365UnifiedCreatorVoiceInstallJournal(app)
    private val creator = PortableCreatorBackupManager(app)

    suspend fun inspectOwnerReviewed(
        ownerConfirmed: Boolean,
        passphrase: CharArray,
    ): V366CreatorVoiceRecoveryProof = withContext(Dispatchers.IO) {
        require(ownerConfirmed && passphrase.size >= 12) {
            "Fresh owner review and the encrypted backup passphrase are required"
        }
        val checkpoint = h4.requireCheckpointReadOnly()
        check(checkpoint.step == V344UnifiedRestoreStep.BEFORE_CREATOR) {
            "Voice recovery evidence is available only for interrupted Creator imports"
        }
        val plan = allocations.inspectReadOnly()
            ?: error("Missing durable Creator Voice Idea allocation")
        check(plan.transactionId == checkpoint.transactionId &&
            plan.sourceSha256 == checkpoint.sourceSha256 &&
            plan.escrowSha256 == checkpoint.escrowSha256) {
            "Creator Voice allocation belongs to another owner transaction"
        }

        val pair = V345UnifiedAuthenticatedRestoreParts(app)
            .authenticateForInterruptedRecovery(
                ownerConfirmedReview = true,
                passphrase = passphrase,
            )
        check(V345UnifiedAuthenticatedPairPolicy.matchesRecoveryCheckpoint(
            pair, checkpoint,
        )) { "The selected target or ORIGINAL encrypted recovery escrow changed" }

        val target = exactOwnerFile(
            "backlot-unified-owner-restore-preparation-v1",
            "incoming-unified-encrypted.zip",
        )
        val original = exactOwnerFile(
            "backlot-unified-owner-recovery-escrow-v1",
            "original-owner-encrypted-unified.zip",
        )

        // Stage and validate one archive at a time, so two large decrypted
        // voice collections never have to coexist in private cache.
        val targetBytes = inspectCreatorMedia(
            target, checkpoint.sourceSha256, pair.targetCreatorSha256, passphrase,
        )
        check(V366CreatorVoiceRecoveryPolicy.matchesTarget(plan, targetBytes)) {
            "Voice allocation differs from the authenticated TARGET recording bytes"
        }
        val originalBytes = inspectCreatorMedia(
            original, checkpoint.escrowSha256, pair.originalCreatorSha256, passphrase,
        )

        // The original package is a complete, independently authenticated
        // source of owned recordings, not a license to remove any current file.
        val firstAudit = allocations.auditReadOnly(plan)
        check(sha256(target) == checkpoint.sourceSha256 &&
            sha256(original) == checkpoint.escrowSha256 &&
            h4.requireCheckpointReadOnly() == checkpoint &&
            allocations.inspectReadOnly() == plan) {
            "Owner archives, Voice allocation or H4 stage changed during media review"
        }
        val lastAudit = allocations.auditReadOnly(plan)
        check(firstAudit == lastAudit) {
            "An allocated Voice Idea file changed during recovery inspection"
        }
        V366CreatorVoiceRecoveryProof(
            transactionId = checkpoint.transactionId,
            targetCreatorSha256 = pair.targetCreatorSha256,
            originalCreatorSha256 = pair.originalCreatorSha256,
            targetMediaCount = targetBytes.size,
            originalMediaCount = originalBytes.size,
            originalMediaHashes = originalBytes.toMap(),
            installedCopies = lastAudit,
            originalMediaAuthenticated = true,
            targetAllocationAuthenticated = true,
        )
    }

    private fun inspectCreatorMedia(
        encryptedFile: File,
        expectedOuterSha256: String,
        expectedCreatorSha256: String,
        passphrase: CharArray,
    ): Map<String, V366VoiceBytes> {
        check(sha256(encryptedFile) == expectedOuterSha256) {
            "Encrypted Creator source no longer matches the owner review"
        }
        encryptedFile.inputStream().buffered().use { input ->
            V341UnifiedOwnerArchive.stageAndVerify(
                input, passphrase, app.cacheDir,
            ).use { unified ->
                check(sha256(unified.creator) == expectedCreatorSha256) {
                    "Creator inner source changed after authentication"
                }
                unified.creator.inputStream().buffered().use {
                    creator.validate(it)
                }
                val staged = unified.creator.inputStream().buffered().use {
                    PortableBackupArchive.stage(it, app.cacheDir)
                }
                try {
                    check(staged.isArchive) {
                        "Combined Creator voice recovery requires a portable ZIP"
                    }
                    return staged.mediaByIdeaId.mapValues { (_, media) ->
                        check(media.isFile &&
                            !Files.isSymbolicLink(media.toPath()) &&
                            media.length() in 1L..(512L * 1024L * 1024L)) {
                            "Untrusted or oversized staged Creator voice media"
                        }
                        V366VoiceBytes(media.length(), sha256(media))
                    }
                } finally {
                    staged.cleanup()
                }
            }
        }
    }

    private fun exactOwnerFile(folderName: String, fileName: String): File {
        val folder = File(app.filesDir, folderName)
        check(folder.isDirectory && !Files.isSymbolicLink(folder.toPath()) &&
            folder.canonicalFile.parentFile == app.filesDir.canonicalFile) {
            "Untrusted owner recovery directory"
        }
        val file = File(folder, fileName)
        check(file.isFile && !Files.isSymbolicLink(file.toPath()) &&
            file.canonicalFile.parentFile == folder.canonicalFile) {
            "Untrusted owner recovery source file"
        }
        return file
    }

    private fun sha256(file: File): String {
        val digest = MessageDigest.getInstance("SHA-256")
        file.inputStream().buffered().use { input ->
            val buffer = ByteArray(64 * 1024)
            while (true) {
                val read = input.read(buffer)
                if (read < 0) break
                if (read > 0) digest.update(buffer, 0, read)
            }
        }
        return digest.digest().joinToString("") {
            "%02x".format(it.toInt() and 255)
        }
    }
}
