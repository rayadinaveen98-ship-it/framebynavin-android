package com.framebynavin.app.data

import android.content.Context
import com.framebynavin.app.personal.Phase1V2OwnerRestoreCoordinator
import com.framebynavin.app.personal.Phase1V2RestoreFingerprint
import com.framebynavin.app.personal.StabilizationPersonalMutationGate
import com.framebynavin.app.personal.V301LocalRecoveryReader
import java.io.File
import java.nio.file.Files
import java.nio.file.LinkOption
import java.security.MessageDigest
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * INTERNAL H3 combined Personal ADDITIVE forward stage. UNCALLED by any
 * production UI, startup, automation or alarm path.
 *
 * The Creator stage must already have durably reached CREATOR_WRITTEN. The
 * original Personal data must still match the authenticated original escrow,
 * including exact 21-day Done receipts and live private JPEG SHA integrity.
 * BEFORE_PERSONAL is fsynced before any Personal store or photo is written.
 *
 * Reuses the EXISTING Phase1V2OwnerRestoreCoordinator: its six journaled
 * Personal stages, encrypted live-state escrow, authenticated photo staging,
 * conflict rejection, exact Done receipt coverage and final v2 verification.
 * This class does NOT invent a second Personal store writer.
 *
 * A crash after BEFORE_PERSONAL keeps the combined journal blocking ordinary
 * edits. Its independent v2 recovery journal and encrypted source files are
 * preserved if the inner restore is interrupted. If v2 finishes but the
 * outer PERSONAL_WRITTEN checkpoint is interrupted, H4 still requires
 * owner-reviewed recovery; no replay is attempted automatically.
 *
 * This class cannot finish or release a combined restore. Actual owner-
 * reviewed partial-write forward/rollback still must be implemented first.
 */
internal class V358UnifiedPersonalForwardStage(context: Context) {
    private val app = context.applicationContext
    private val journal = V344UnifiedRestoreJournal(app)
    private val ownerArchives = V346UnifiedInterruptedRecoveryReview(app)
    private val currentReader = V353UnifiedLiveCrashForensics(app)

    suspend fun executeInternalOnly(
        passphrase: CharArray,
        ownerConfirmedPersonalAdditions: Boolean,
        acknowledgedNonPortableAlarmsAndSongs: Boolean,
    ): V358PersonalForwardReceipt = withContext(Dispatchers.IO) {
        require(ownerConfirmedPersonalAdditions &&
            acknowledgedNonPortableAlarmsAndSongs && passphrase.size >= 12) {
            "Personal additions, device-only alarm/music limits and passphrase need owner consent"
        }
        val expected = journal.requireCheckpointReadOnly()
        require(expected.step == V344UnifiedRestoreStep.CREATOR_WRITTEN) {
            "Personal forward stage needs completed Creator checkpoint, not partial writes"
        }
        val archives = ownerArchives.inspect(
            ownerConfirmedReview = true, passphrase = passphrase,
        )
        check(archives.journal == expected &&
            !archives.creatorStandaloneRecoveryMarker &&
            !archives.personalStandaloneRecoveryMarker &&
            !V347OwnerRecoveryDecisionJournal.hasPendingFor(app)) {
            "Selected Personal source or independent recovery decision differs from H4"
        }
        val live = currentReader.inspect(
            ownerConfirmed = true, passphrase = passphrase,
        )
        check(V358PersonalForwardStartPolicy.mayBegin(
            expected, archives, live,
            ownerConfirmedPersonalAdditions,
            acknowledgedNonPortableAlarmsAndSongs,
        )) {
            "Current original Personal data/photos or authenticated target are not safe for additive stage"
        }

        val selected = selectedSource()
        val originalSourceHash = archives.sealedArchives.sourceSha256
        check(sha256(selected) == originalSourceHash) {
            "Encrypted target changed since the owner approved its source"
        }
        selected.inputStream().buffered().use { encrypted ->
            V341UnifiedOwnerArchive.stageAndVerify(
                encrypted, passphrase, app.cacheDir,
            ).use { unpacked ->
                check(sha256(unpacked.personal) ==
                    archives.sealedArchives.targetPersonalSha256) {
                    "Personal encrypted inner archive differs from the owner-approved source"
                }
                check(sha256(selected) == originalSourceHash &&
                    journal.requireCheckpointReadOnly() == expected) {
                    "Personal source or cross-domain journal changed before writing"
                }

                val applied = StabilizationPersonalMutationGate
                    .withCombinedPersonalForwardFence(
                        app, expected, ownerConfirmed = true,
                    ) {
                        // Re-read all nine Personal domains, INCLUDING exact
                        // 21-day completion instants, inside the Personal
                        // mutex immediately before ANY new write is possible.
                        val current = V301LocalRecoveryReader(app).readV2()
                        val currentDigest = Phase1V2RestoreFingerprint.calculate(
                            current.components, current.photos,
                        )
                        check(current.localIntegrityVerified &&
                            currentDigest == live.intendedPersonalRecipe.originalDigest &&
                            current.photos.size ==
                                live.intendedPersonalRecipe.originalPhotoCount &&
                            !V347OwnerRecoveryDecisionJournal.hasPendingFor(app)) {
                            "Personal original state changed since encrypted recovery review"
                        }

                        // Once this marker is durable, NEVER return to the
                        // ordinary PREPARED cancellation or silently restart.
                        val before = journal.checkpoint(
                            expected, V344UnifiedRestoreStep.BEFORE_PERSONAL,
                            nextStamp(expected.updatedAtMillis),
                            otherRecoveryPending = {
                                V347OwnerRecoveryDecisionJournal.hasPendingFor(app)
                            },
                        )
                        val result = CreatorDataGate.withPendingCombinedReadInspection(
                            app, before,
                        ) {
                            unpacked.personal.inputStream().buffered().use { personalZip ->
                                Phase1V2OwnerRestoreCoordinator(app).restoreOwnerApproved(
                                    selectedArchive = personalZip,
                                    selectedPassphrase = passphrase,
                                    escrowPassphrase = passphrase,
                                    ownerConfirmedAdditions = true,
                                    acknowledgedNonPortableAlarmsAndSongs = true,
                                    deferPostRestoreReconcileForUnified = true,
                                    combinedCheckpointForUnified = before,
                                )
                            }
                        }
                        check(result.originalDigest ==
                            live.intendedPersonalRecipe.originalDigest &&
                            result.installedDigest ==
                            live.intendedPersonalRecipe.mergedTargetDigest &&
                            result.importedNewRecords ==
                            live.intendedPersonalRecipe.incomingAdditions &&
                            result.completedAndJournalArchived) {
                            "Existing nine-domain Personal v2 engine did not verify the merged owner target"
                        }
                        journal.checkpoint(
                            before, V344UnifiedRestoreStep.PERSONAL_WRITTEN,
                            nextStamp(before.updatedAtMillis),
                            otherRecoveryPending = {
                                V347OwnerRecoveryDecisionJournal.hasPendingFor(app)
                            },
                        )
                        result
                    }
                V358PersonalForwardReceipt(
                    transactionId = expected.transactionId,
                    originalPersonalDigest = applied.originalDigest,
                    installedPersonalDigest = applied.installedDigest,
                    importedNewRecords = applied.importedNewRecords,
                    personalJournalArchived = applied.completedAndJournalArchived,
                    checkpointReached = V344UnifiedRestoreStep.PERSONAL_WRITTEN,
                    combinedRestoreComplete = false,
                )
            }
        }
    }

    private fun selectedSource(): File {
        val root = File(app.filesDir,
            "backlot-unified-owner-restore-preparation-v1")
        check(root.isDirectory && !Files.isSymbolicLink(root.toPath()) &&
            root.canonicalFile.parentFile == app.filesDir.canonicalFile) {
            "Selected Personal owner archive is not in trusted private storage"
        }
        val file = File(root, "incoming-unified-encrypted.zip")
        check(file.isFile && !Files.isSymbolicLink(file.toPath()) &&
            file.canonicalFile.parentFile == root.canonicalFile) {
            "Selected Personal encrypted archive path is untrusted"
        }
        return file
    }

    private fun nextStamp(previous: Long): Long =
        maxOf(System.currentTimeMillis(), previous + 1L)

    private fun sha256(file: File): String {
        val digest = MessageDigest.getInstance("SHA-256")
        file.inputStream().buffered().use { input ->
            val buffer = ByteArray(64 * 1024)
            while (true) {
                val n = input.read(buffer)
                if (n < 0) break
                if (n > 0) digest.update(buffer, 0, n)
            }
        }
        return digest.digest().joinToString("") {
            "%02x".format(it.toInt() and 0xff)
        }
    }
}

/** No Personal-only stage receipt can authorize combined journal release. */
internal data class V358PersonalForwardReceipt(
    val transactionId: String,
    val originalPersonalDigest: String,
    val installedPersonalDigest: String,
    val importedNewRecords: Int,
    val personalJournalArchived: Boolean,
    val checkpointReached: V344UnifiedRestoreStep,
    val combinedRestoreComplete: Boolean,
) {
    init {
        require(checkpointReached == V344UnifiedRestoreStep.PERSONAL_WRITTEN &&
            importedNewRecords >= 0 &&
            originalPersonalDigest.matches(Regex("[0-9a-f]{64}")) &&
            installedPersonalDigest.matches(Regex("[0-9a-f]{64}")) &&
            personalJournalArchived && !combinedRestoreComplete) {
            "Personal forward stage is not a completed Creator+Personal transaction"
        }
    }
    val mayReleaseOriginalEscrow: Boolean get() = false
    val mayEnableOrdinaryReminders: Boolean get() = false
}

internal object V358PersonalForwardStartPolicy {
    fun mayBegin(
        checkpoint: V344UnifiedRestoreCheckpoint,
        proof: V346InterruptedRecoveryProof,
        live: V353UnifiedLiveForensicReport,
        ownerApproved: Boolean,
        acknowledgedDeviceOnlyMusicAndAlarms: Boolean,
    ): Boolean =
        ownerApproved && acknowledgedDeviceOnlyMusicAndAlarms &&
            checkpoint.step == V344UnifiedRestoreStep.CREATOR_WRITTEN &&
            V346InterruptedRecoveryProofPolicy.matches(proof, checkpoint) &&
            !proof.creatorStandaloneRecoveryMarker &&
            !proof.personalStandaloneRecoveryMarker &&
            live.transactionId == checkpoint.transactionId &&
            live.checkpointStep == V344UnifiedRestoreStep.CREATOR_WRITTEN &&
            live.personalLiveMatchesOriginalMetadata &&
            live.livePhotoAudit.currentPhotoFilesMatchTheirLiveRows &&
            live.intendedPersonalRecipe.allOriginalPhotoReferencesPreserved &&
            live.intendedPersonalRecipe.noMutationAuthority &&
            !live.mayWriteLiveData && !live.mayReleaseRecoveryJournals
}
