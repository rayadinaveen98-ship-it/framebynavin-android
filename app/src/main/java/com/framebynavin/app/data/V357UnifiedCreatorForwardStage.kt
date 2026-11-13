package com.framebynavin.app.data

import android.content.Context
import com.framebynavin.app.personal.StabilizationPersonalMutationGate
import java.io.File
import java.nio.file.Files
import java.nio.file.LinkOption
import java.security.MessageDigest
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * H3 INTERNAL Creator forward executor. No production UI or startup path
 * invokes this class until combined Personal forward, inverse media recovery
 * and final-release verification are implemented.
 *
 * This is the first real, FIXED-SOURCE restore write integration: only an
 * already PREPARED combined transaction, the precisely authenticated target
 * Creator portable archive, and completely unchanged original Creator +
 * Personal records can enter its Creator stage. No generic "write any data"
 * callback, independent restoration, cloud sync or automatic retry exists.
 *
 * BEFORE_CREATOR is durably written BEFORE the Creator replacement begins.
 * A death during any Creator DataStore/Best Frame/Voice Idea write leaves
 * BEFORE_CREATOR in the combined journal. The caller MUST NOT invoke this
 * method again at that stage; separate forensic owner-approved recovery is
 * required. On successful Creator restoration CREATOR_WRITTEN is fsynced.
 * Personal data is never touched by this stage; all ordinary writers remain
 * blocked until the full owner recovery protocol eventually completes.
 */
internal class V357UnifiedCreatorForwardStage(context: Context) {
    private val app = context.applicationContext
    private val journal = V344UnifiedRestoreJournal(app)
    private val verifiedSource = V346UnifiedInterruptedRecoveryReview(app)
    private val inspector = V353UnifiedLiveCrashForensics(app)

    suspend fun executeInternalOnly(
        ownerConfirmedStart: Boolean,
        passphrase: CharArray,
    ): V357CreatorForwardReceipt = withContext(Dispatchers.IO) {
        require(ownerConfirmedStart && passphrase.size >= 12) {
            "Explicit owner consent and encrypted archive passphrase are required"
        }
        val originalCheckpoint = journal.requireCheckpointReadOnly()
        require(originalCheckpoint.step == V344UnifiedRestoreStep.PREPARED_NO_WRITES) {
            "Creator import may begin only from a never-started combined transaction"
        }

        // Verify both sealed archives and the exact checkpoint/source IDs.
        val proven = verifiedSource.inspect(
            ownerConfirmedReview = true, passphrase = passphrase,
        )
        check(proven.journal == originalCheckpoint &&
            !proven.creatorStandaloneRecoveryMarker &&
            !proven.personalStandaloneRecoveryMarker) {
            "Competing restore or stale encrypted Creator owner evidence"
        }

        // While the combined marker is PREPARED, original data must still be
        // byte-for-byte equivalent to the recovery baseline. Even one
        // unrelated current Space JPEG orphan invalidates this first write.
        val live = inspector.inspect(
            ownerConfirmed = true, passphrase = passphrase,
        )
        check(V357CreatorForwardStartPolicy.mayBegin(
            checkpoint = originalCheckpoint,
            authenticated = proven,
            live = live,
            freshOwnerConsent = ownerConfirmedStart,
        )) {
            "The original Creator, Personal or photo state changed, or owner " +
                "recovery evidence is not eligible to begin the Creator stage"
        }

        val selectedFile = exactSelectedSourceFile()
        val expectedSourceHash = proven.sealedArchives.sourceSha256
        check(sha256(selectedFile) == expectedSourceHash) {
            "Selected owner archive changed during Creator stage review"
        }
        selectedFile.inputStream().buffered().use { source ->
            V341UnifiedOwnerArchive.stageAndVerify(
                source, passphrase, app.cacheDir,
            ).use { unpacked ->
                // The complete portable Creator ZIP has been authenticated
                // already by V346 and will be validated AGAIN inside the
                // existing, independently journaled portable restore engine.
                check(sha256(unpacked.creator) ==
                    proven.sealedArchives.targetCreatorSha256) {
                    "Creator portable inner archive differs from owner-approved source"
                }
                check(sha256(selectedFile) == expectedSourceHash &&
                    journal.requireCheckpointReadOnly() == originalCheckpoint) {
                    "Creator source or journal changed before first stage write"
                }
                val preview = StabilizationPersonalMutationGate
                    .withCombinedCreatorStageFence(app, originalCheckpoint) {
                        // This durable marker linearizes the point at which
                        // partial Creator writes become POSSIBLE. From here
                        // the PREPARED-only cancel path is forever denied.
                        val beforeCreator = journal.checkpoint(
                            originalCheckpoint,
                            V344UnifiedRestoreStep.BEFORE_CREATOR,
                            nextStamp(originalCheckpoint.updatedAtMillis),
                        )
                        val result = CreatorDataGate.withJournalBoundCreatorWrite(
                            app, beforeCreator, ownerConfirmed = true,
                        ) {
                            unpacked.creator.inputStream().buffered().use { creatorZip ->
                                PortableCreatorBackupManager(app).restore(creatorZip)
                            }
                        }
                        journal.checkpoint(
                            beforeCreator,
                            V344UnifiedRestoreStep.CREATOR_WRITTEN,
                            nextStamp(beforeCreator.updatedAtMillis),
                        )
                        result
                    }
                V357CreatorForwardReceipt(
                    combinedTransactionId = originalCheckpoint.transactionId,
                    sourceSha256 = expectedSourceHash,
                    checkpointReached = V344UnifiedRestoreStep.CREATOR_WRITTEN,
                    restoredCreatorProjects = preview.projectCount,
                    personalRestored = false,
                    combinedRestoreComplete = false,
                )
            }
        }
    }

    private fun exactSelectedSourceFile(): File {
        val root = File(app.filesDir, "backlot-unified-owner-restore-preparation-v1")
        check(root.isDirectory &&
            !Files.isSymbolicLink(root.toPath()) &&
            root.canonicalFile.parentFile == app.filesDir.canonicalFile) {
            "Untrusted combined encrypted target directory"
        }
        val source = File(root, "incoming-unified-encrypted.zip")
        check(source.isFile && !Files.isSymbolicLink(source.toPath()) &&
            source.canonicalFile.parentFile == root.canonicalFile) {
            "Untrusted selected encrypted source file"
        }
        return source
    }

    private fun nextStamp(previous: Long): Long =
        maxOf(System.currentTimeMillis(), previous + 1L)

    private fun sha256(file: File): String {
        val md = MessageDigest.getInstance("SHA-256")
        file.inputStream().buffered().use { stream ->
            val buffer = ByteArray(64 * 1024)
            while (true) {
                val count = stream.read(buffer)
                if (count < 0) break
                if (count > 0) md.update(buffer, 0, count)
            }
        }
        return md.digest().joinToString("") {
            "%02x".format(it.toInt() and 0xff)
        }
    }
}

/** A Creator-stage receipt MUST NOT be represented as a combined restore. */
internal data class V357CreatorForwardReceipt(
    val combinedTransactionId: String,
    val sourceSha256: String,
    val checkpointReached: V344UnifiedRestoreStep,
    val restoredCreatorProjects: Int,
    val personalRestored: Boolean,
    val combinedRestoreComplete: Boolean,
) {
    init {
        require(checkpointReached == V344UnifiedRestoreStep.CREATOR_WRITTEN &&
            restoredCreatorProjects >= 0 && !personalRestored &&
            !combinedRestoreComplete) {
            "A Creator-only forward stage cannot claim an all-domain restore"
        }
    }

    val mayReleaseRecoveryEscrow: Boolean get() = false
    val mayNotifyOwnerCombinedSuccess: Boolean get() = false
}

/**
 * Pure, deny-by-default Creator execution precondition. No callback or Token
 * is returned. Only the actual V357 internal source-bound method can open a
 * Creator writer after this policy, under its two owner-domain mutexes.
 */
internal object V357CreatorForwardStartPolicy {
    fun mayBegin(
        checkpoint: V344UnifiedRestoreCheckpoint,
        authenticated: V346InterruptedRecoveryProof,
        live: V353UnifiedLiveForensicReport,
        freshOwnerConsent: Boolean,
    ): Boolean =
        freshOwnerConsent &&
            checkpoint.step == V344UnifiedRestoreStep.PREPARED_NO_WRITES &&
            V346InterruptedRecoveryProofPolicy.matches(authenticated, checkpoint) &&
            !authenticated.creatorStandaloneRecoveryMarker &&
            !authenticated.personalStandaloneRecoveryMarker &&
            live.transactionId == checkpoint.transactionId &&
            live.checkpointStep == V344UnifiedRestoreStep.PREPARED_NO_WRITES &&
            live.creatorLiveMatchesOriginalMetadata &&
            live.personalLiveMatchesOriginalMetadata &&
            live.livePhotoAudit.currentPhotoFilesMatchTheirLiveRows &&
            live.intendedPersonalRecipe.allOriginalPhotoReferencesPreserved &&
            live.intendedPersonalRecipe.noMutationAuthority &&
            !live.mayWriteLiveData &&
            !live.mayReleaseRecoveryJournals
}
