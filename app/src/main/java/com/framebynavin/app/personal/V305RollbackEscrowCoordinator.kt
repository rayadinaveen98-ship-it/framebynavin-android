package com.framebynavin.app.personal

import android.content.Context
import java.io.File
import java.io.InputStream
import java.io.OutputStream
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Only caller of the V305 escrow store for real Backlot data.
 *
 * The rollback copy is a fresh encrypted V300 ZIP of the PHONE'S OWN current
 * data, not the new import. It must be authenticated, contain every private
 * photo, and match the pre-escrow local inventory before it can be retained.
 *
 * The method never touches the imported archive and never writes live records.
 * A later crash requires the owner to provide the escrow passphrase again.
 */
internal class V305RollbackEscrowCoordinator(context: Context) {
    private val app = context.applicationContext
    private val reader = V301LocalRecoveryReader(app)
    private val source = V300PersonalArchiveSource(app)
    private val escrow = V305RollbackEscrowStore(
        File(app.filesDir, "backlot-personal-rollback-escrow-v1"),
    )

    suspend fun createOwnerApprovedEscrow(
        passphrase: CharArray,
        ownerRequested: Boolean,
    ): V305EscrowReceipt = withContext(Dispatchers.IO) {
        require(ownerRequested) {
            "The owner must explicitly request an independent recovery copy"
        }
        // Full local integrity first: missing private photo files, incomplete
        // Space metadata, corrupt Groups/Challenges all abort before export.
        val before = reader.read()
        val baseline = V304RestorePlanPolicy.fingerprint(
            before.components, before.photos,
        )
        suspend fun validateZip(input: InputStream) {
            V300EncryptedPersonalArchive.stageAndVerify(
                input, passphrase, app.cacheDir,
            ).use { verified ->
                V305RollbackMatchPolicy.requireSameSnapshot(
                    before, verified.components, verified.photos.map { it.photo },
                )
            }
        }
        val receipt = escrow.create(
            ownerRequested = true,
            baselineDigest = baseline,
            writer = { output: OutputStream -> source.export(passphrase, output) },
            verifier = { input: InputStream -> validateZip(input) },
        )
        try {
            // Verify the live device did not change while we copied photos.
            val now = reader.read()
            require(now.localIntegrityVerified &&
                V304RestorePlanPolicy.fingerprint(now.components, now.photos) == baseline) {
                "Personal data changed during rollback escrow creation"
            }
            // Another independent full read and authenticated ZIP validation.
            require(escrow.verify(receipt.id, ::validateZip) == receipt) {
                "Newly saved escrow differs from verified recovery copy"
            }
            receipt
        } catch (cancel: CancellationException) {
            escrow.discard(receipt.id)
            throw cancel
        } catch (failure: Throwable) {
            escrow.discard(receipt.id)
            throw failure
        }
    }

    /** Re-enter the passphrase after process death; NEVER auto-run live restore. */
    suspend fun verifyOwnerEscrow(
        id: String,
        passphrase: CharArray,
    ): V305EscrowReceipt = withContext(Dispatchers.IO) {
        escrow.verify(id) { input ->
            V300EncryptedPersonalArchive.stageAndVerify(
                input, passphrase, app.cacheDir,
            ).use { verified ->
                val personal = PersonalCompletionBackupCodec.decode(
                    verified.components.personal,
                )
                V303SpaceReceiptArchiveCodec.decode(verified.components.spaces, personal)
                // Full staged verification includes all photo bytes, receipts,
                // groups, challenges and cross-domain reminder links.
            }
        }
    }

    suspend fun inspectEscrowCandidates(): V305EscrowInventory =
        withContext(Dispatchers.IO) { escrow.inventory() }

    /** Destructive only to this one encrypted private escrow, never live data. */
    suspend fun discardAfterOwnerConfirmation(id: String) =
        withContext(Dispatchers.IO) { escrow.discard(id) }
}

/** Each domain must have EXACT value parity; no partial rollback escrow. */
internal object V305RollbackMatchPolicy {
    fun requireSameSnapshot(
        before: V301LocalRecoverySnapshot,
        archived: V300PersonalComponents,
        archivedPhotos: List<com.framebynavin.app.personal.spaces.V290SpacePhotoEntity>,
    ) {
        require(before.localIntegrityVerified) {
            "Existing owner data failed verification"
        }
        val report = V301RestoreDryRun.compare(before, archived, archivedPhotos)
        val diffs = listOf(
            report.reminders, report.completedActions, report.challenges,
            report.groups, report.spaces, report.spaceLinks,
            report.journalEntries, report.photos,
        )
        require(diffs.all {
            it.conflicting == 0 && it.localOnly == 0 && it.newRecords == 0
        } && report.localIntegrityVerified) {
            "Encrypted escrow does not represent all existing Personal records"
        }
    }
}
