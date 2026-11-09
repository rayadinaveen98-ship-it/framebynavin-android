package com.framebynavin.app.cloud

import android.content.Context
import java.io.File
import java.nio.file.Files
import java.nio.file.StandardCopyOption
import java.util.UUID
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * READ-ONLY owner review of a selected encrypted cloud revision.
 *
 * Opening a remote backup must never replace the phone's Creator projects,
 * Personal reminders, 21-day receipts, Groups, Spaces, photos or alarm leases.
 * The exact remote head is checked before and after copying, the full file is
 * verified twice (stream + private disk), and each encrypted part is decrypted
 * and authenticated in disposable staging before reporting an inventory.
 *
 * This is source coverage inspection, NOT owner-authorized restore. No preview
 * grants mutation rights and no previously approved local review is overwritten.
 */
internal class TrustOwnerCloudReview(
    context: Context,
    private val accounts: CloudSyncManager = CloudSyncManager(context),
    private val transport: TrustUniversalCloudTransport = TrustUniversalCloudTransport(),
) {
    private val app = context.applicationContext
    private val root = File(app.noBackupFilesDir, "trust-owner-cloud-ciphertext-review-v1")
    private val uuid = Regex(
        "[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}",
    )

    internal data class OwnerHeadReview(
        val ownerId: String,
        val head: TrustCloudRevisionHead?,
        val storedEncryptedBytes: Long,
    ) {
        val hasEncryptedBackup: Boolean get() = head != null
    }

    internal data class SourceCoverageReview(
        val ownerId: String,
        val remoteRevision: Long,
        val encryptedLocalReviewId: String,
        val encryptedBytes: Long,
        val verifiedParts: Int,
        val counts: Map<String, Int>,
        val missingRequiredParts: Set<String>,
        val knownUncoveredDomains: Set<String>,
    ) {
        // Explicitly protects legacy source previews from becoming a Restore
        // button just because their password and ciphertext are valid.
        val mayReplaceOwnerData: Boolean get() = false
        val mayUploadAsCompleteBackup: Boolean get() = false
    }

    internal data class UnifiedRestoreReview(
        val ownerId: String,
        val remoteRevision: Long,
        val creator: TrustCreatorRestoreBridge.Review,
        val personal: com.framebynavin.app.personal.Phase1V2ArchiveDryRunReport,
        val uncoveredPreferenceKeys: List<String>,
    ) {
        // A combined preview is not a durable cross-store restore transaction.
        val mayCommitRestore: Boolean get() = false
    }

    suspend fun inspectCurrentRemote(): OwnerHeadReview =
        accounts.withFreshSession { session ->
            require(transport.phase(session) == "active") {
                "Cloud backup deletion is pending; do not open a stale remote revision"
            }
            val head = transport.head(session)
            OwnerHeadReview(
                ownerId = session.userId,
                head = head,
                storedEncryptedBytes = head?.encryptedBytes ?: 0L,
            )
        }

    suspend fun inspectEncryptedSource(
        ownerReviewed: OwnerHeadReview,
        passphrase: CharArray,
    ): SourceCoverageReview {
        require(passphrase.size in 12..512) {
            "Enter the private backup passphrase to review its contents"
        }
        val selectedHead = checkNotNull(ownerReviewed.head) {
            "No verified encrypted owner revision was selected"
        }
        val reviewId = UUID.randomUUID().toString()
        val pending = File(ensurePrivateRoot(), "pending-$reviewId.enc")
        val verified = File(root, "review-$reviewId.enc")
        try {
            return accounts.withFreshSession { session ->
                check(session.userId == ownerReviewed.ownerId) {
                    "The signed-in Backlot account changed; owner data was not opened"
                }
                check(transport.phase(session) == "active") {
                    "Owner backup deletion is in progress"
                }
                val actualHead = transport.head(session)
                TrustT1C4CloudRevisionPolicy.requireUnchanged(
                    selectedHead, actualHead, session.userId,
                )
                val descriptor = TrustCloudCiphertextDescriptor(
                    session.userId,
                    selectedHead.objectPath,
                    selectedHead.encryptedSha256,
                    selectedHead.encryptedBytes,
                )
                transport.downloadVerifiedToPrivateFile(session, descriptor, pending)
                val verifiedSource = withContext(Dispatchers.IO) {
                    pending.inputStream().buffered().use { input ->
                        TrustEncryptedStagingCodec.stageAndVerify(
                            session.userId, passphrase, input, app.cacheDir,
                        ).use { staged ->
                            TrustRestorableSourceReview.inspect(app, staged)
                        }
                    }
                }
                // If another phone published a newer revision while this
                // password/decryption inspection ran, the old one is not a
                // current-owner restore candidate.
                check(transport.phase(session) == "active")
                TrustT1C4CloudRevisionPolicy.requireUnchanged(
                    selectedHead, transport.head(session), session.userId,
                )
                check(!verified.exists()) { "Private encrypted review ID collided" }
                Files.move(
                    pending.toPath(), verified.toPath(), StandardCopyOption.ATOMIC_MOVE,
                )
                SourceCoverageReview(
                    ownerId = session.userId,
                    remoteRevision = selectedHead.revision,
                    encryptedLocalReviewId = reviewId,
                    encryptedBytes = selectedHead.encryptedBytes,
                    verifiedParts = verifiedSource.sourcePartsValidated,
                    counts = verifiedSource.counts,
                    missingRequiredParts = verifiedSource.missingRequiredSources,
                    knownUncoveredDomains = (
                        verifiedSource.uncoveredPreferenceKeys.map {
                            "Uncaptured saved preference: " + it
                        } + listOf(
                            "Some independent local settings/Creator assets still lack restore adapters",
                            "Cross-store restore rollback, delivery rearming and device permissions",
                        )
                    ).toSet(),
                )
            }
        } catch (failure: Throwable) {
            if (pending.exists() && !pending.delete()) {
                failure.addSuppressed(IllegalStateException(
                    "Could not erase the unverified private ciphertext file",
                ))
            }
            if (verified.exists() && !verified.delete()) {
                failure.addSuppressed(IllegalStateException(
                    "Could not erase the stale owner review after account change",
                ))
            }
            throw failure
        }
    }

    /**
     * One owner-authenticated preview of BOTH Creator and Personal domains
     * from a single verified encrypted cloud revision. No live import occurs.
     */
    suspend fun previewUnifiedRestore(
        selected: SourceCoverageReview,
        passphrase: CharArray,
    ): UnifiedRestoreReview {
        require(uuid.matches(selected.encryptedLocalReviewId)) {
            "Invalid owner encrypted review identifier"
        }
        require(passphrase.size in 12..512)
        return accounts.withFreshSession { session ->
            check(session.userId == selected.ownerId &&
                transport.phase(session) == "active") {
                "Owner or cloud backup state changed since the preview"
            }
            val head = checkNotNull(transport.head(session)) {
                "The selected cloud backup no longer exists"
            }
            check(head.revision == selected.remoteRevision &&
                head.encryptedBytes == selected.encryptedBytes) {
                "Another device updated the backup; review the new revision first"
            }
            val file = File(
                ensurePrivateRoot(),
                "review-" + selected.encryptedLocalReviewId + ".enc",
            )
            require(file.isFile &&
                !Files.isSymbolicLink(file.toPath())) {
                "The private encrypted owner copy was not found"
            }
            val descriptor = TrustCloudCiphertextDescriptor(
                session.userId, head.objectPath, head.encryptedSha256,
                head.encryptedBytes,
            )
            withContext(Dispatchers.IO) {
                TrustCloudVerifiedReadback.verify(
                    file.inputStream().buffered(), descriptor,
                )
                file.inputStream().buffered().use { encrypted ->
                    TrustEncryptedStagingCodec.stageAndVerify(
                        session.userId, passphrase, encrypted, app.cacheDir,
                    ).use { verified ->
                        val review = TrustRestorableSourceReview.inspect(app, verified)
                        check(review.missingRequiredSources.isEmpty()) {
                            "This encrypted owner archive lacks required Backlot source parts"
                        }
                        val creator = TrustCreatorRestoreBridge(app).preview(verified)
                        val personal = TrustPersonalRestoreBridge(app).previewAdditions(verified)
                        UnifiedRestoreReview(
                            ownerId = session.userId,
                            remoteRevision = head.revision,
                            creator = creator,
                            personal = personal,
                            uncoveredPreferenceKeys = review.uncoveredPreferenceKeys,
                        )
                    }
                }
            }.also {
                // Owner and exact remote revision must still be current after
                // the local database comparison, never only before it.
                check(transport.phase(session) == "active")
                TrustT1C4CloudRevisionPolicy.requireUnchanged(
                    head, transport.head(session), session.userId,
                )
            }
        }
    }

    /**
     * Reopen the EXACT owner-reviewed ciphertext for the native restore
     * coordinator, keeping both the cloud-account generation guard and the
     * temporary authenticated source alive for the entire owner operation.
     *
     * Caller must create and verify a cross-domain pre-restore escrow BEFORE
     * entering this callback; no account lock may be nested inside it.
     */
    internal suspend fun <T> withSameReviewedRestoreSource(
        selected: SourceCoverageReview,
        agreedDiff: UnifiedRestoreReview,
        passphrase: CharArray,
        action: suspend (
            verified: TrustVerifiedStagedBundle,
            actualHead: TrustCloudRevisionHead,
            diff: UnifiedRestoreReview,
        ) -> T,
    ): T {
        check(TrustUnifiedRestoreJournal.inspect(app) == null) {
            "A previous unified restore has an unresolved owner journal"
        }
        require(uuid.matches(selected.encryptedLocalReviewId) &&
            passphrase.size in 12..512 &&
            selected.ownerId == agreedDiff.ownerId &&
            selected.remoteRevision == agreedDiff.remoteRevision) {
            "The selected encrypted owner archive does not match the reviewed plan"
        }
        return accounts.withFreshSession { session ->
            check(session.userId == selected.ownerId &&
                transport.phase(session) == "active") {
                "Signed-in owner or cloud deletion state changed"
            }
            val currentHead = checkNotNull(transport.head(session)) {
                "Selected universal cloud revision is missing"
            }
            check(currentHead.revision == selected.remoteRevision &&
                currentHead.encryptedBytes == selected.encryptedBytes)
            val ownerFile = File(
                ensurePrivateRoot(),
                "review-" + selected.encryptedLocalReviewId + ".enc",
            )
            check(ownerFile.isFile &&
                !Files.isSymbolicLink(ownerFile.toPath())) {
                "Private reviewed ciphertext was removed or replaced"
            }
            val sourceDescriptor = TrustCloudCiphertextDescriptor(
                session.userId, currentHead.objectPath,
                currentHead.encryptedSha256, currentHead.encryptedBytes,
            )
            withContext(Dispatchers.IO) {
                TrustCloudVerifiedReadback.verify(
                    ownerFile.inputStream().buffered(), sourceDescriptor,
                )
                ownerFile.inputStream().buffered().use { stream ->
                    TrustEncryptedStagingCodec.stageAndVerify(
                        session.userId, passphrase, stream, app.cacheDir,
                    ).use { stage ->
                        val coverage = TrustRestorableSourceReview.inspect(app, stage)
                        check(coverage.missingRequiredSources.isEmpty() &&
                            coverage.uncoveredPreferenceKeys.isEmpty()) {
                            "Backup source is incomplete or cannot be restored losslessly"
                        }
                        val creatorDiff = TrustCreatorRestoreBridge(app).preview(stage)
                        val personalDiff = TrustPersonalRestoreBridge(app).previewAdditions(stage)
                        check(agreedDiff.creator == creatorDiff &&
                            agreedDiff.personal == personalDiff) {
                            "The phone changed since the Creator + Personal review"
                        }
                        check(!creatorDiff.hasExistingOwnerWork &&
                            creatorDiff.projectIdConflicts == 0 &&
                            creatorDiff.ideaIdConflicts == 0 &&
                            personalDiff.conflicts == 0) {
                            "Occupied or conflicting phone data cannot be replaced automatically"
                        }
                        val result = action(stage, currentHead, agreedDiff)
                        check(transport.phase(session) == "active") {
                            "Owner cloud backup deletion began during recovery"
                        }
                        TrustT1C4CloudRevisionPolicy.requireUnchanged(
                            currentHead, transport.head(session), session.userId,
                        )
                        result
                    }
                }
            }
        }
    }

    /**
     * Reconstruct a previously reviewed source after process death. The
     * journal's immutable revision, owner and SHA are the sole admissible
     * source identity. Reuses the original private ciphertext when present;
     * if missing, re-downloads the EXACT same revision into a new private
     * file without replacing any other owner review.
     */
    suspend fun reopenInterruptedRestoreSource(
        pending: TrustUnifiedRestoreJournal.Checkpoint,
        passphrase: CharArray,
    ): SourceCoverageReview {
        require(passphrase.size in 12..512 &&
            uuid.matches(pending.reviewedSourceId) &&
            TrustUnifiedRestoreJournal.inspect(app) == pending) {
            "Interrupted restore journal changed; owner must inspect recovery"
        }
        return accounts.withFreshSession { session ->
            check(session.userId == pending.ownerId &&
                transport.phase(session) == "active") {
                "Same signed-in owner and active cloud revision required"
            }
            val head = checkNotNull(transport.head(session)) {
                "The interrupted backup no longer has a cloud head"
            }
            check(head.revision == pending.remoteRevision &&
                head.encryptedSha256 == pending.encryptedSourceSha256)
            val descriptor = TrustCloudCiphertextDescriptor(
                session.userId, head.objectPath,
                head.encryptedSha256, head.encryptedBytes,
            )
            val rootDir = ensurePrivateRoot()
            val reviewedFile = File(
                rootDir, "review-" + pending.reviewedSourceId + ".enc",
            )
            if (!reviewedFile.exists()) {
                val temporary = File(
                    rootDir, "pending-" + pending.reviewedSourceId + ".enc",
                )
                try {
                    transport.downloadVerifiedToPrivateFile(
                        session, descriptor, temporary,
                    )
                    check(!reviewedFile.exists())
                    Files.move(
                        temporary.toPath(), reviewedFile.toPath(),
                        StandardCopyOption.ATOMIC_MOVE,
                    )
                } catch (failure: Throwable) {
                    if (temporary.exists() && !temporary.delete()) {
                        failure.addSuppressed(IllegalStateException(
                            "A failed owner re-download could not be discarded",
                        ))
                    }
                    throw failure
                }
            }
            require(reviewedFile.isFile &&
                !Files.isSymbolicLink(reviewedFile.toPath()))
            val coverage = withContext(Dispatchers.IO) {
                TrustCloudVerifiedReadback.verify(
                    reviewedFile.inputStream().buffered(), descriptor,
                )
                reviewedFile.inputStream().buffered().use { encrypted ->
                    TrustEncryptedStagingCodec.stageAndVerify(
                        session.userId, passphrase, encrypted, app.cacheDir,
                    ).use { staged ->
                        TrustRestorableSourceReview.inspect(app, staged)
                    }
                }
            }
            check(coverage.missingRequiredSources.isEmpty() &&
                coverage.uncoveredPreferenceKeys.isEmpty()) {
                "Interrupted backup source lacks authenticated restore coverage"
            }
            check(transport.phase(session) == "active")
            TrustT1C4CloudRevisionPolicy.requireUnchanged(
                head, transport.head(session), session.userId,
            )
            SourceCoverageReview(
                ownerId = session.userId,
                remoteRevision = head.revision,
                encryptedLocalReviewId = pending.reviewedSourceId,
                encryptedBytes = head.encryptedBytes,
                verifiedParts = coverage.sourcePartsValidated,
                counts = coverage.counts,
                missingRequiredParts = coverage.missingRequiredSources,
                knownUncoveredDomains = emptySet(),
            )
        }
    }

    /**
     * Owner re-authentication of the exact ciphertext used by an INTERRUPTED
     * unified recovery. Creator data may already have been imported, so an
     * ordinary new-restore conflict preview would be inappropriate here.
     *
     * The persisted native journal, signed-in owner, SHA and source revision
     * are verified BEFORE any caller may resume the unfinished native stage.
     */
    internal suspend fun <T> withPendingReviewedRestoreSource(
        selected: SourceCoverageReview,
        checkpoint: TrustUnifiedRestoreJournal.Checkpoint,
        passphrase: CharArray,
        action: suspend (
            verified: TrustVerifiedStagedBundle,
            actualHead: TrustCloudRevisionHead,
        ) -> T,
    ): T {
        require(passphrase.size in 12..512 &&
            selected.ownerId == checkpoint.ownerId &&
            selected.remoteRevision == checkpoint.remoteRevision &&
            selected.encryptedLocalReviewId == checkpoint.reviewedSourceId &&
            uuid.matches(selected.encryptedLocalReviewId)) {
            "Interrupted restore source does not match its owner journal"
        }
        check(checkpoint.stage.ordinal >=
            TrustUnifiedRestoreJournal.Stage.CREATOR_COMMITTED.ordinal &&
            TrustUnifiedRestoreJournal.inspect(app) == checkpoint) {
            "Creator must have committed before Personal recovery can resume"
        }
        return accounts.withFreshSession { session ->
            check(session.userId == checkpoint.ownerId &&
                transport.phase(session) == "active") {
                "Owner identity or cloud backup deletion state changed"
            }
            val head = checkNotNull(transport.head(session))
            check(head.revision == checkpoint.remoteRevision &&
                head.encryptedSha256 == checkpoint.encryptedSourceSha256 &&
                head.encryptedBytes == selected.encryptedBytes) {
                "Interrupted restore references a cloud backup that changed"
            }
            val file = File(
                ensurePrivateRoot(),
                "review-" + checkpoint.reviewedSourceId + ".enc",
            )
            check(file.isFile && !Files.isSymbolicLink(file.toPath()))
            val descriptor = TrustCloudCiphertextDescriptor(
                session.userId, head.objectPath,
                head.encryptedSha256, head.encryptedBytes,
            )
            withContext(Dispatchers.IO) {
                TrustCloudVerifiedReadback.verify(
                    file.inputStream().buffered(), descriptor,
                )
                file.inputStream().buffered().use { encrypted ->
                    TrustEncryptedStagingCodec.stageAndVerify(
                        session.userId, passphrase, encrypted, app.cacheDir,
                    ).use { staged ->
                        val coverage = TrustRestorableSourceReview.inspect(app, staged)
                        check(coverage.missingRequiredSources.isEmpty() &&
                            coverage.uncoveredPreferenceKeys.isEmpty()) {
                            "An incomplete source cannot resume a live restore"
                        }
                        val result = action(staged, head)
                        check(transport.phase(session) == "active")
                        TrustT1C4CloudRevisionPolicy.requireUnchanged(
                            head, transport.head(session), session.userId,
                        )
                        result
                    }
                }
            }
        }
    }

    internal data class PreparedUnifiedSource(
        val ownerId: String,
        val remoteRevision: Long,
        val creatorPortableBytes: Long,
        val personalEncryptedBytes: Long,
        val existingCreatorRecords: Int,
        val personalProposedAdditions: Int,
    ) {
        // The prepared ZIPs are deliberately destroyed before returning.
        // Source checks do not commit, schedule or replace any owner data.
        val committed: Boolean get() = false
    }

    /**
     * Owner-approved, non-mutating RESTORE PREPARATION using the two EXISTING
     * native import formats. Both candidates are independently verified before
     * the source is considered well-formed, then erased from disposable private
     * cache even if conversion fails. An occupied Creator workspace and any
     * Personal ID collision are rejected: the legacy Creator importer replaces
     * rather than safely merges those records.
     */
    suspend fun prepareUnifiedRestoreSource(
        selected: SourceCoverageReview,
        reviewedDiff: UnifiedRestoreReview,
        trustPassphrase: CharArray,
        personalZipPassphrase: CharArray,
        exactConfirmation: String,
    ): PreparedUnifiedSource {
        check(TrustUnifiedRestoreJournal.inspect(app) == null) {
            "A previous Creator/Personal restore requires owner recovery first"
        }
        require(exactConfirmation == "PREPARE VERIFIED RESTORE") {
            "The owner must explicitly approve private restore preparation"
        }
        require(trustPassphrase.size in 12..512 &&
            personalZipPassphrase.size in 12..512 &&
            uuid.matches(selected.encryptedLocalReviewId)) {
            "Both encrypted source passphrases and a verified owner review are required"
        }
        check(reviewedDiff.ownerId == selected.ownerId &&
            reviewedDiff.remoteRevision == selected.remoteRevision &&
            reviewedDiff.uncoveredPreferenceKeys.isEmpty() &&
            !reviewedDiff.creator.hasExistingOwnerWork &&
            reviewedDiff.creator.projectIdConflicts == 0 &&
            reviewedDiff.creator.ideaIdConflicts == 0 &&
            reviewedDiff.personal.conflicts == 0 &&
            reviewedDiff.creator.missingCreatorSources.isEmpty()
        ) {
            "This device has conflicting Creator/Personal data or missing source " +
                "coverage. Nothing will be replaced"
        }
        return accounts.withFreshSession { session ->
            check(session.userId == selected.ownerId &&
                transport.phase(session) == "active") {
                "Owner signed out, switched accounts or cloud deletion is pending"
            }
            val head = checkNotNull(transport.head(session)) {
                "The reviewed encrypted cloud backup no longer exists"
            }
            check(head.revision == selected.remoteRevision &&
                head.encryptedBytes == selected.encryptedBytes) {
                "Another device advanced the encrypted backup revision"
            }
            val file = File(
                ensurePrivateRoot(),
                "review-" + selected.encryptedLocalReviewId + ".enc",
            )
            check(file.isFile && !Files.isSymbolicLink(file.toPath()))
            val descriptor = TrustCloudCiphertextDescriptor(
                session.userId, head.objectPath, head.encryptedSha256,
                head.encryptedBytes,
            )
            withContext(Dispatchers.IO) {
                TrustCloudVerifiedReadback.verify(
                    file.inputStream().buffered(), descriptor,
                )
                file.inputStream().buffered().use { encrypted ->
                    TrustEncryptedStagingCodec.stageAndVerify(
                        session.userId, trustPassphrase, encrypted, app.cacheDir,
                    ).use { staged ->
                        val coverage = TrustRestorableSourceReview.inspect(app, staged)
                        check(coverage.missingRequiredSources.isEmpty() &&
                            coverage.uncoveredPreferenceKeys.isEmpty()) {
                            "Cannot convert a partial source to a usable restore"
                        }
                        val creatorBridge = TrustCreatorRestoreBridge(app)
                        val personalBridge = TrustPersonalRestoreBridge(app)
                        val currentCreator = creatorBridge.preview(staged)
                        val currentPersonal = personalBridge.previewAdditions(staged)
                        check(currentCreator == reviewedDiff.creator &&
                            currentPersonal == reviewedDiff.personal) {
                            "Owner data changed after restore conflict review"
                        }
                        var creatorFile: File? = null
                        var personalFile: File? = null
                        try {
                            creatorFile = creatorBridge
                                .preparePortableCandidateAfterOwnerApproval(
                                    staged, ownerApprovedPreparation = true,
                                )
                            personalFile = personalBridge.prepareEncryptedV2Candidate(
                                staged, personalZipPassphrase,
                            )
                            check(creatorFile.isFile &&
                                creatorFile.length() in 1L..(1024L * 1024 * 1024) &&
                                personalFile.isFile &&
                                personalFile.length() in 1L..(1024L * 1024 * 1024)
                            ) {
                                "One native Backlot restore candidate is missing"
                            }
                            // Recheck the current phone and remote head after
                            // preparation, before returning any success report.
                            check(creatorBridge.preview(staged) == reviewedDiff.creator &&
                                personalBridge.previewAdditions(staged) ==
                                    reviewedDiff.personal) {
                                "Device data changed during restore preparation"
                            }
                            PreparedUnifiedSource(
                                ownerId = session.userId,
                                remoteRevision = head.revision,
                                creatorPortableBytes = creatorFile.length(),
                                personalEncryptedBytes = personalFile.length(),
                                existingCreatorRecords =
                                    currentCreator.existingProjects +
                                        currentCreator.existingIdeas,
                                personalProposedAdditions =
                                    currentPersonal.proposedAdditions,
                            )
                        } finally {
                            // Files contain unencrypted native Creator JSON or
                            // a separate Personal ZIP. Never retain them after
                            // owner preview, even when conversion throws.
                            listOfNotNull(creatorFile, personalFile).forEach { candidate ->
                                check(candidate.delete() || !candidate.exists()) {
                                    "Sensitive private restore staging requires cleanup"
                                }
                            }
                        }
                    }
                }
            }.also {
                check(transport.phase(session) == "active")
                TrustT1C4CloudRevisionPolicy.requireUnchanged(
                    head, transport.head(session), session.userId,
                )
            }
        }
    }

    /** Existing callers asking only for Personal receive the same shared review. */
    suspend fun previewPersonalRestore(
        selected: SourceCoverageReview,
        passphrase: CharArray,
    ): com.framebynavin.app.personal.Phase1V2ArchiveDryRunReport =
        previewUnifiedRestore(selected, passphrase).personal

    private fun ensurePrivateRoot(): File {
        check(root.isDirectory || root.mkdirs()) {
            "Unable to prepare private owner ciphertext review directory"
        }
        check(!Files.isSymbolicLink(root.toPath())) {
            "Refusing symlinked private owner review directory"
        }
        // Encrypted owner reviews stay on device unless the owner explicitly
        // exports them. Refuse new reviews rather than deleting another one.
        check(root.listFiles().orEmpty().count {
            it.isFile && it.name.matches(Regex("review-$uuid[.]enc"))
        } < 3) {
            "Review storage is full. Resolve an existing encrypted copy first"
        }
        return root
    }
}
