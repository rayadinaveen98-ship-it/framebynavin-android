package com.framebynavin.app.data

import android.content.Context
import com.framebynavin.app.personal.StabilizationPersonalMutationGate
import com.framebynavin.app.personal.V301LocalRecoveryReader
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * H3 LIVE DOMAIN READ-ONLY CRASH FORENSICS.
 *
 * After a combined journal appears, normal Creator and Personal gates
 * deliberately freeze writers and even ordinary snapshots. This reader
 * acquires Personal THEN Creator snapshot mutexes, with a SPECIAL read-only
 * inspection marker in each gate; it does not open a general data writer.
 *
 * Creator: uses no-replay/no-timeline-seeding export.
 * Personal: V301's strict SQLite READONLY forensic metadata snapshot, with
 * exact single-activity challenge Done receipts. Private photos may be
 * partially installed after process death and are NOT crypto-authenticated
 * by the live reader; comparison is useful evidence, NOT an executable plan.
 */
internal class V353UnifiedLiveCrashForensics(context: Context) {
    private val app = context.applicationContext
    private val archives = V346UnifiedInterruptedRecoveryReview(app)
    private val journal = V344UnifiedRestoreJournal(app)

    suspend fun inspect(
        ownerConfirmed: Boolean,
        passphrase: CharArray,
    ): V353UnifiedLiveForensicReport = withContext(Dispatchers.IO) {
        require(ownerConfirmed && passphrase.size >= 12) {
            "Fresh owner approval and the encrypted archive passphrase are required"
        }
        // Authenticate BOTH original and selected encrypted archives before
        // reading potentially damaged local owner records.
        val proof = archives.inspect(
            ownerConfirmedReview = true,
            passphrase = passphrase,
        )
        val checkpoint = proof.journal
        check(!proof.creatorStandaloneRecoveryMarker &&
            !proof.personalStandaloneRecoveryMarker) {
            "Independent Creator or Personal restore evidence needs its own review"
        }
        // Derive the actual intended ADDITIVE Personal target from the
        // untouched ORIGINAL escrow and selected backup, not partially
        // written live records. Reuse the already authenticated V346 proof.
        val intendedPersonal = V356UnifiedPersonalRecoveryRecipeReader(app)
            .inspectVerified(proof, passphrase).personalRecipe

        val (current, photoAudit) = StabilizationPersonalMutationGate
            .withPendingCombinedReadInspection(app, checkpoint) {
                CreatorDataGate.withPendingCombinedReadInspection(app, checkpoint) {
                    val creatorJson = CreatorBackupManager(app)
                        .createCloudBackupForUnifiedReadOnlyBaselineInsideOwnerGate()
                    val personal = V301LocalRecoveryReader(app)
                        .readForInterruptedV2Recovery()
                    // The Personal V301 crash reader deliberately reports
                    // integrity=false; do not reclassify it as a verified
                    // source or a safe rollback snapshot.
                    check(!personal.localIntegrityVerified) {
                        "Interrupted Personal snapshot cannot imply proven photo integrity"
                    }
                    val fingerprints = V343UnifiedBaselineReader(app)
                        .fingerprintUnverifiedInterrupted(creatorJson, personal)
                    // SHA-256 each current private JPEG under the SAME
                    // Personal mutex. Orphan *.pending and unmatched files
                    // remain intact and are reported, never auto-removed.
                    val photos = V354UnifiedLiveSpacePhotoAudit(app)
                        .inspect(personal.photos)
                    fingerprints to photos
                }
            }

        check(journal.requireCheckpointReadOnly() == checkpoint) {
            "Combined recovery checkpoint changed during live-data inspection"
        }
        V353UnifiedLiveForensicReport(
            transactionId = checkpoint.transactionId,
            checkpointStep = checkpoint.step,
            creatorLiveMatchesOriginalMetadata =
                current.creatorSha256 == checkpoint.creatorOriginalSha256,
            personalLiveMatchesOriginalMetadata =
                current.personalSha256 == checkpoint.personalOriginalSha256,
            creatorCurrentMetadataSha256 = current.creatorSha256,
            personalCurrentMetadataSha256 = current.personalSha256,
            originalCreatorMetadataSha256 = checkpoint.creatorOriginalSha256,
            originalPersonalMetadataSha256 = checkpoint.personalOriginalSha256,
            livePhotoAudit = photoAudit,
            intendedPersonalRecipe = intendedPersonal,
            privatePhotoByteIntegrityProven = false,
            mediaRollbackProven = false,
            mayWriteLiveData = false,
            mayReleaseRecoveryJournals = false,
        )
    }
}

internal data class V353UnifiedLiveForensicReport(
    val transactionId: String,
    val checkpointStep: V344UnifiedRestoreStep,
    val creatorLiveMatchesOriginalMetadata: Boolean,
    val personalLiveMatchesOriginalMetadata: Boolean,
    val creatorCurrentMetadataSha256: String,
    val personalCurrentMetadataSha256: String,
    val originalCreatorMetadataSha256: String,
    val originalPersonalMetadataSha256: String,
    val livePhotoAudit: V354SpacePhotoAuditResult,
    val intendedPersonalRecipe: V355UnifiedPersonalRecoveryRecipe,
    val privatePhotoByteIntegrityProven: Boolean,
    val mediaRollbackProven: Boolean,
    val mayWriteLiveData: Boolean,
    val mayReleaseRecoveryJournals: Boolean,
) {
    init {
        require(intendedPersonalRecipe.noMutationAuthority &&
            !privatePhotoByteIntegrityProven &&
            !mediaRollbackProven &&
            !mayWriteLiveData &&
            !mayReleaseRecoveryJournals) {
            "Unverified live crash metadata cannot authorize restore or rollback"
        }
    }
}

internal object V353UnifiedLiveForensicsPolicy {
    /**
     * Even if every current metadata fingerprint equals the original, a
     * BEFORE_* journal can hide partially replaced PHOTO FILE BYTES. Do not
     * silently declare an interrupted transaction complete or revertible.
     */
    fun allowsRecoveryWrites(@Suppress("UNUSED_PARAMETER")
        report: V353UnifiedLiveForensicReport): Boolean = false

    fun allowsJournalRelease(@Suppress("UNUSED_PARAMETER")
        report: V353UnifiedLiveForensicReport): Boolean = false
}
