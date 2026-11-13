package com.framebynavin.app.personal

import android.content.Context
import com.framebynavin.app.personal.spaces.V290PhotoTimelinePolicy
import com.framebynavin.app.personal.spaces.V290SpacePhotoEntity
import java.io.File
import java.io.FileOutputStream
import java.io.InputStream
import java.nio.channels.FileChannel
import java.nio.file.Files
import java.nio.file.LinkOption
import java.nio.file.StandardCopyOption
import java.nio.file.StandardOpenOption
import java.util.UUID
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Owner-approved, actual v2 Personal import.
 *
 * Only additive IDs without ANY conflicts are allowed. Every domain and photo
 * is authenticated up front. The existing owner's FULL raw (including exact
 * Done timestamps) encrypted escrow and imported ZIP are fsynced BEFORE any
 * durable v2 write-ahead journal is prepared. The journal freezes ordinary
 * writes after any crash; interrupted v2 transactions require owner consent
 * and BOTH ZIP passwords to resume. No background auto-recovery.
 */
internal class Phase1V2OwnerRestoreCoordinator(context: Context) {
    private val app = context.applicationContext
    private val reader = V301LocalRecoveryReader(app)
    private val legacyRoot = StabilizationPersonalMutationGate.journalRootFor(app)
    private val v2Root = Phase1V2RecoveryRoots.alongsideLegacy(legacyRoot)
    private val transactions = File(app.filesDir, "backlot-personal-v2-encrypted-transactions")
    private val completed = File(app.filesDir, "backlot-personal-v2-completed-journals")
    private val photos = File(app.filesDir, "backlot-personal-space-photos")

    /** A pending journal blocks all normal edits until owner-reviewed recovery. */
    fun hasPendingOwnerRecovery(): Boolean =
        Files.exists(v2Root.toPath(), LinkOption.NOFOLLOW_LINKS)

    internal data class Finished(
        val transactionId: String,
        val importedNewRecords: Int,
        val originalDigest: String,
        val installedDigest: String,
        val completedAndJournalArchived: Boolean = true,
    )

    /**
     * DOES NOT use a cloud token, alarms or auto-import actions.
     * Owner selects the encrypted file and explicitly approves ADDITION only.
     * A same-ID content collision in any of nine domains stops before writing.
     */
    suspend fun restoreOwnerApproved(
        selectedArchive: InputStream,
        selectedPassphrase: CharArray,
        escrowPassphrase: CharArray,
        ownerConfirmedAdditions: Boolean,
        acknowledgedNonPortableAlarmsAndSongs: Boolean,
        deferPostRestoreReconcileForUnified: Boolean = false,
        combinedCheckpointForUnified:
            com.framebynavin.app.data.V344UnifiedRestoreCheckpoint? = null,
    ): Finished = withContext(Dispatchers.IO) {
        require(ownerConfirmedAdditions && acknowledgedNonPortableAlarmsAndSongs) {
            "Owner must review all Personal changes and device-only alarm/music limitations"
        }
        require(selectedPassphrase.size >= 12 && escrowPassphrase.size >= 12) {
            "Encrypted archive and private recovery escrow require passphrases"
        }
        if (deferPostRestoreReconcileForUnified) {
            check(combinedCheckpointForUnified != null &&
                combinedCheckpointForUnified.step ==
                    com.framebynavin.app.data.V344UnifiedRestoreStep.BEFORE_PERSONAL) {
                "Unified Personal restore requires exact BEFORE_PERSONAL owner checkpoint"
            }
            check(com.framebynavin.app.data.V344UnifiedRestoreJournal.hasPendingFor(app)) {
                "Suppress Personal alarm reconciliation only under a combined recovery journal"
            }
        }
        val finished = StabilizationPersonalMutationGate.withOwnerMutation(app) {
            val baseline = reader.readV2()
            val txn = UUID.randomUUID().toString()
            val preparedId = UUID.randomUUID().toString()
            val escrowId = UUID.randomUUID().toString()
            val dir = ensureNewTransactionDirectory(txn)
            var journalPrepared = false
            try {
                val incomingFile = File(dir, "incoming-encrypted.zip")
                val escrowFile = File(dir, "original-escrow-encrypted.zip")
                copyVerifiedInputDurably(selectedArchive, incomingFile)
                V300EncryptedPersonalArchive.stageAndVerify(
                    incomingFile.inputStream(), selectedPassphrase, app.cacheDir,
                ).use { staged ->
                    val incomingPhotos = staged.photos.map { it.photo }
                    val target = Phase1V2RestoreTargetPolicy.propose(
                        baseline, staged.components, incomingPhotos,
                    )
                    require(target.preview.conflicts == 0) {
                        "Owner must first review conflicting Personal records"
                    }
                    writeExactOwnerEscrow(baseline, escrowFile, escrowPassphrase)
                    V300EncryptedPersonalArchive.stageAndVerify(
                        escrowFile.inputStream(), escrowPassphrase, app.cacheDir,
                    ).use { original ->
                        require(Phase1V2RestoreFingerprint.calculate(
                            original.components, original.photos.map { it.photo },
                        ) == target.baselineDigest) {
                            "Encrypted rollback escrow does not match exact original phone data"
                        }
                    }
                    // No live domain write has occurred before this marker.
                    val stamp = System.currentTimeMillis().coerceAtLeast(1L)
                    val journal = Phase1V2RestoreWriteAheadJournal(v2Root)
                    val expectedV2 = Phase1V2RestoreCheckpoint(
                        transactionId = txn,
                        preparedId = preparedId,
                        escrowId = escrowId,
                        originalDigest = target.baselineDigest,
                        targetDigest = target.targetDigest,
                        step = Phase1V2RestoreStep.PREPARED,
                        createdAtMillis = stamp, updatedAtMillis = stamp,
                    )
                    if (combinedCheckpointForUnified != null) {
                        // Durably bind the native v2 identity and exact
                        // original/target digests to BEFORE_PERSONAL BEFORE
                        // the v2 data-writing journal is ever prepared.
                        // A death here retains both ZIPs for owner review.
                        com.framebynavin.app.data.V359UnifiedPersonalV2HandoffJournal(app)
                            .recordBeforeV2Prepare(combinedCheckpointForUnified, expectedV2)
                    }
                    val record = journal.prepare(expectedV2, ownerRequested = true)
                    journalPrepared = true
                    StabilizationPersonalMutationGate.withPreparedV2OwnerRecovery(
                        legacyRoot, record,
                    ) {
                        applyAndVerify(journal, target, baseline.photos, staged.photos)
                    }
                    Finished(
                        txn, target.preview.proposedAdditions,
                        target.baselineDigest, target.targetDigest,
                    )
                }
            } catch (failure: Throwable) {
                // Before the marker, no domain has been touched. After it,
                // NEVER erase any ZIP or journal needed for owner recovery.
                if (!journalPrepared && !v2Root.exists() &&
                    !com.framebynavin.app.data.V359UnifiedPersonalV2HandoffJournal(app)
                        .hasBlockingEvidence()
                ) dir.deleteRecursively()
                throw failure
            }
        }
        // Personal v2 completion is NOT combined transaction completion.
        // Do not re-enable reminders/21-day alarms before the final combined
        // journal is verified and released for both data domains.
        if (!deferPostRestoreReconcileForUnified) {
            runCatching {
                PersonalDeliveryCoordinator(app).reconcile()
                Phase1HabitAlarmScheduler(app).reconcile()
            }
        }
        finished
    }

    /**
     * Recovery resumes only if the user supplies BOTH passwords again.
     * The original AND target ZIPs are fully decrypted and every byte is
     * checked against the durable v2 journal and live interrupted state.
     */
    suspend fun resumeOwnerApproved(
        escrowPassphrase: CharArray,
        selectedPassphrase: CharArray,
        ownerConfirmedResume: Boolean,
    ): Finished = withContext(Dispatchers.IO) {
        require(ownerConfirmedResume && escrowPassphrase.size >= 12 &&
            selectedPassphrase.size >= 12) {
            "Fresh owner consent and both archive passphrases are required"
        }
        val finished = StabilizationPersonalMutationGate
            .withPendingV2RecoveryInspection(app) { checkpoint ->
                resumeInspectedV2(checkpoint, escrowPassphrase, selectedPassphrase)
            }
        runCatching {
            PersonalDeliveryCoordinator(app).reconcile()
            Phase1HabitAlarmScheduler(app).reconcile()
        }
        finished
    }

    /**
     * Combined H4 recovery already holds the exclusive Personal v2 INSPECTION
     * mutex and supplies its exact native checkpoint. Do not acquire the same
     * process mutex a second time, and do not reschedule reminders before the
     * combined transaction has been completely verified and released.
     *
     * This method is callable only by the separate H4-bound resume fence;
     * the native v2 writer still enforces its authenticated forward-recovery
     * permit, exact journals and media byte checks.
     */
    internal suspend fun resumeInspectedForCombined(
        checkpoint: Phase1V2RestoreCheckpoint,
        escrowPassphrase: CharArray,
        selectedPassphrase: CharArray,
    ): Finished {
        require(escrowPassphrase.size >= 12 && selectedPassphrase.size >= 12) {
            "Both Personal recovery passphrases must be authenticated again"
        }
        check(com.framebynavin.app.data.V344UnifiedRestoreJournal.hasPendingFor(app)) {
            "Combined Personal resume requires an active owner recovery journal"
        }
        return resumeInspectedV2(checkpoint, escrowPassphrase, selectedPassphrase)
    }

    private suspend fun resumeInspectedV2(
        checkpoint: Phase1V2RestoreCheckpoint,
        escrowPassphrase: CharArray,
        selectedPassphrase: CharArray,
    ): Finished {
                val dir = requireKnownTransactionDirectory(checkpoint.transactionId)
                val originalFile = File(dir, "original-escrow-encrypted.zip")
                val incomingFile = File(dir, "incoming-encrypted.zip")
                V300EncryptedPersonalArchive.stageAndVerify(
                    originalFile.inputStream(), escrowPassphrase, app.cacheDir,
                ).use { original ->
                    V300EncryptedPersonalArchive.stageAndVerify(
                        incomingFile.inputStream(), selectedPassphrase, app.cacheDir,
                    ).use { staged ->
                        val originalPhotos = original.photos.map { it.photo }
                        val baseline = V301LocalRecoverySnapshot(
                            original.components, originalPhotos, localIntegrityVerified = true,
                        )
                        val target = Phase1V2RestoreTargetPolicy.propose(
                            baseline, staged.components, staged.photos.map { it.photo },
                        )
                        require(checkpoint.originalDigest == target.baselineDigest &&
                            checkpoint.targetDigest == target.targetDigest) {
                            "Owner's source and recovery ZIPs do not match interrupted journal"
                        }
                        val live = reader.readForInterruptedV2Recovery()
                        val domains = Phase1V2RestoreDomainForensics.inspect(
                            checkpoint, original.components, target.combined, live.components,
                            originalPhotos, target.photos, live.photos,
                        )
                        val photoProof = Phase1V2PhotoByteForensics.inspect(
                            checkpoint.step, photos, originalPhotos, target.photos, live.photos,
                        )
                        val evidence = Phase1V2InterruptedRecoveryEvidence(
                            checkpoint, domains, photoProof,
                        )
                        StabilizationPersonalMutationGate.withApprovedV2ForwardRecovery(
                            legacyRoot, checkpoint, evidence, ownerApproved = true,
                        ) {
                            reconcileInterruptedPendingCopies(
                                checkpoint.step, originalPhotos, target.photos, staged.photos,
                            )
                            applyAndVerify(
                                Phase1V2RestoreWriteAheadJournal(v2Root), target,
                                originalPhotos, staged.photos,
                            )
                        }
                        Finished(
                            checkpoint.transactionId, target.preview.proposedAdditions,
                            target.baselineDigest, target.targetDigest,
                        )
                    }
                }
    }

    /** Reapplying any BEFORE_* stage is idempotent; after WRITTEN skip it. */
    private suspend fun applyAndVerify(
        journal: Phase1V2RestoreWriteAheadJournal,
        target: Phase1V2RestoreTarget,
        originalPhotos: List<V290SpacePhotoEntity>,
        importedPhotos: List<V300VerifiedPhoto>,
    ) {
        suspend fun phase(
            before: Phase1V2RestoreStep,
            done: Phase1V2RestoreStep,
            write: suspend () -> Unit,
        ) {
            val current = checkNotNull(journal.inspectAfterRestart()).step
            if (current.ordinal >= done.ordinal) return
            if (current.ordinal < before.ordinal) {
                require(current.ordinal == before.ordinal - 1) {
                    "V2 writer cannot skip a durable restore stage"
                }
                journal.checkpoint(current, before, System.currentTimeMillis())
            } else {
                require(current == before) {
                    "Unexpected v2 owner recovery checkpoint"
                }
            }
            write()
            journal.checkpoint(before, done, System.currentTimeMillis())
        }

        phase(Phase1V2RestoreStep.BEFORE_PERSONAL,
            Phase1V2RestoreStep.PERSONAL_WRITTEN) {
            PersonalReminderStore(app).replaceFromApprovedImport(target.combined.personal)
        }
        phase(Phase1V2RestoreStep.BEFORE_LEGACY_CHALLENGES,
            Phase1V2RestoreStep.LEGACY_CHALLENGES_WRITTEN) {
            V297ChallengeStore(app).replaceFromApprovedImport(target.combined.challenges)
        }
        phase(Phase1V2RestoreStep.BEFORE_SINGLE_ACTIVITY,
            Phase1V2RestoreStep.SINGLE_ACTIVITY_WRITTEN) {
            Phase1HabitStore(app).importMissingAfterApproval(
                requireNotNull(target.combined.singleActivityChallenges))
        }
        phase(Phase1V2RestoreStep.BEFORE_GROUPS,
            Phase1V2RestoreStep.GROUPS_WRITTEN) {
            V298ReminderGroupStore(app).importMissingAfterApproval(target.combined.groups)
        }
        val writer = B03RestoreDomainWriter(app)
        phase(Phase1V2RestoreStep.BEFORE_SPACES,
            Phase1V2RestoreStep.SPACES_WRITTEN) {
            writer.writeSpaces(target.combined)
        }
        phase(Phase1V2RestoreStep.BEFORE_PHOTOS,
            Phase1V2RestoreStep.PHOTOS_WRITTEN) {
            writer.writePhotos(target.photos, originalPhotos, importedPhotos)
        }

        val now = checkNotNull(journal.inspectAfterRestart())
        require(now.step == Phase1V2RestoreStep.PHOTOS_WRITTEN ||
            now.step == Phase1V2RestoreStep.VERIFYING) {
            "V2 owner restore must finish ALL data stages before verification"
        }
        val current = reader.readV2()
        require(current.localIntegrityVerified &&
            Phase1V2RestoreFingerprint.calculate(
                current.components, current.photos,
            ) == target.targetDigest) {
            "V2 owner restore final state differs from authenticated ZIP target"
        }
        if (now.step == Phase1V2RestoreStep.PHOTOS_WRITTEN) {
            journal.checkpoint(now.step, Phase1V2RestoreStep.VERIFYING,
                System.currentTimeMillis())
        }
        releaseVerifiedJournal(journal)
    }

    /**
     * Only exact incoming photo-stage .pending files may be removed on an
     * explicitly authenticated owner resume. Original photos never qualify.
     * A corrupted FINAL JPEG is not overwritten, even with owner consent.
     */
    private suspend fun reconcileInterruptedPendingCopies(
        checkpoint: Phase1V2RestoreStep,
        originals: List<V290SpacePhotoEntity>,
        intended: List<V290SpacePhotoEntity>,
        staged: List<V300VerifiedPhoto>,
    ) {
        if (checkpoint != Phase1V2RestoreStep.BEFORE_PHOTOS) return
        StabilizationPersonalMutationGate.requireActiveRecovery(app)
        val originalIds = originals.mapTo(HashSet()) { it.id }
        val newSources = staged.associateBy { it.photo.id }
        for (photo in intended.filter { it.id !in originalIds }) {
            val folder = File(photos, photo.spaceId)
            val pending = File(folder, "." + photo.id + ".restore.pending")
            if (!Files.exists(pending.toPath(), LinkOption.NOFOLLOW_LINKS)) continue
            require(Files.isRegularFile(pending.toPath(), LinkOption.NOFOLLOW_LINKS) &&
                !Files.isSymbolicLink(folder.toPath()) &&
                newSources[photo.id]?.photo == photo) {
                "Unrecognized interrupted private photo file"
            }
            // Its original source has been fully reauthenticated by caller;
            // never touch a FINAL file unless it is either absent or verified.
            val final = File(folder, photo.fileName)
            require(!Files.exists(final.toPath(), LinkOption.NOFOLLOW_LINKS) ||
                (Files.isRegularFile(final.toPath(), LinkOption.NOFOLLOW_LINKS) &&
                    final.length() == newSources.getValue(photo.id).sizeBytes &&
                    photo.sha256Hex == java.security.MessageDigest.getInstance("SHA-256")
                        .digest(final.readBytes()).joinToString("") {
                            "%02x".format(it)
                        })) {
                "Unverified final photo cannot be replaced by recovery"
            }
            Files.delete(pending.toPath())
            FileChannel.open(folder.toPath(), StandardOpenOption.READ).use {
                it.force(true)
            }
        }
    }

    private fun releaseVerifiedJournal(journal: Phase1V2RestoreWriteAheadJournal) {
        require(journal.inspectAfterRestart()?.step == Phase1V2RestoreStep.VERIFYING) {
            "Only a fully verified v2 restore journal may be archived"
        }
        check(completed.isDirectory || completed.mkdirs()) {
            "Cannot create completed recovery history"
        }
        val checked = checkNotNull(journal.inspectAfterRestart())
        val destination = File(completed, checked.transactionId)
        require(!Files.exists(destination.toPath(), LinkOption.NOFOLLOW_LINKS)) {
            "Completed v2 transaction identity already archived"
        }
        Files.move(v2Root.toPath(), destination.toPath(), StandardCopyOption.ATOMIC_MOVE)
        FileChannel.open(completed.toPath(), StandardOpenOption.READ).use {
            it.force(true)
        }
    }

    private fun ensureNewTransactionDirectory(id: String): File {
        check(transactions.isDirectory || transactions.mkdirs()) {
            "Could not create encrypted v2 owner recovery storage"
        }
        val folder = File(transactions, id)
        check(folder.mkdir() && !Files.isSymbolicLink(folder.toPath())) {
            "Cannot reserve encrypted owner recovery transaction"
        }
        return folder
    }

    private fun requireKnownTransactionDirectory(id: String): File {
        require(UUID.fromString(id).toString() == id) { "Invalid recovery identity" }
        val root = File(transactions, id)
        require(Files.isDirectory(root.toPath(), LinkOption.NOFOLLOW_LINKS) &&
            !Files.isSymbolicLink(root.toPath())) {
            "Encrypted owner escrow or imported source is missing"
        }
        return root
    }

    private fun copyVerifiedInputDurably(source: InputStream, destination: File) {
        val max = 560L * 1024 * 1024
        var count = 0L
        FileOutputStream(destination).use { output ->
            val buffer = ByteArray(32 * 1024)
            while (true) {
                val n = source.read(buffer)
                if (n < 0) break
                if (n == 0) continue
                count += n
                check(count <= max) { "Encrypted source exceeds supported archive size" }
                output.write(buffer, 0, n)
            }
            output.fd.sync()
        }
        require(count > 0) { "The selected encrypted backup is empty" }
        FileChannel.open(destination.parentFile.toPath(), StandardOpenOption.READ).use {
            it.force(true)
        }
    }

    private fun writeExactOwnerEscrow(
        baseline: V301LocalRecoverySnapshot,
        destination: File,
        passphrase: CharArray,
    ) {
        val sources = baseline.photos.map { photo ->
            val checked = V290PhotoTimelinePolicy.validate(photo)
            val folder = File(photos, checked.spaceId)
            val source = File(folder, checked.fileName)
            require(folder.canonicalFile.parentFile == photos.canonicalFile &&
                source.canonicalFile.parentFile == folder.canonicalFile &&
                Files.isRegularFile(source.toPath(), LinkOption.NOFOLLOW_LINKS)) {
                "Owner's original private JPEG is missing or unsafe"
            }
            V300PhotoSource(checked, source.length()) { source.inputStream() }
        }
        FileOutputStream(destination).use { output ->
            V300EncryptedPersonalArchive.write(
                baseline.components, sources, passphrase, output,
            )
        }
        FileChannel.open(destination.toPath(), StandardOpenOption.READ).use {
            it.force(true)
        }
        FileChannel.open(destination.parentFile.toPath(), StandardOpenOption.READ).use {
            it.force(true)
        }
    }
}
