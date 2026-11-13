package com.framebynavin.app.data

import android.content.Context
import com.framebynavin.app.personal.Phase1V2OriginalReverseJournal
import com.framebynavin.app.personal.Phase1V2PhotoByteForensics
import com.framebynavin.app.personal.Phase1V2RestoreFingerprint
import com.framebynavin.app.personal.Phase1V2RestoreStep
import com.framebynavin.app.personal.Phase1V2ReverseStep
import com.framebynavin.app.personal.Phase1V2RestoreTargetPolicy
import com.framebynavin.app.personal.Phase1V2RecoveryRoots
import com.framebynavin.app.personal.Phase1V2RestoreWriteAheadJournal
import com.framebynavin.app.personal.StabilizationPersonalMutationGate
import com.framebynavin.app.personal.V300EncryptedPersonalArchive
import com.framebynavin.app.personal.V300PersonalComponents
import com.framebynavin.app.personal.V301LocalRecoveryReader
import com.framebynavin.app.personal.V301LocalRecoverySnapshot
import com.framebynavin.app.personal.spaces.V290SpacePhotoEntity
import java.io.File
import java.nio.file.Files
import java.nio.file.LinkOption
import java.security.MessageDigest
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject

internal enum class V377VerifiedOwnerOutcome { ORIGINAL_RESTORED, SELECTED_TARGET_RESTORED }

/**
 * Evidence only: never a journal-release, delete-media, alarm or success UI
 * permission. Records+private media are compared against independently
 * authenticated ORIGINAL and selected TARGET owner ZIPs under both domain
 * read-only process mutexes. The original ZIPs are NEVER deleted.
 */
internal data class V377UnifiedFinalReadOnlyProof(
    val transactionId: String,
    val checkpointSha256: String,
    val ownerDirectionSha256: String,
    val outcome: V377VerifiedOwnerOutcome,
    val expectedCreatorRecordSha256: String,
    val liveCreatorRecordSha256: String,
    val expectedPersonalSha256: String,
    val livePersonalSha256: String,
    val voiceIdeaCount: Int,
    val bestFrameCount: Int,
    val originalPhotoCount: Int,
    val selectedPhotoCount: Int,
    val preservedImportedJpegFiles: Int,
    val archivedNativePersonalVerified: Boolean,
) {
    init {
        val hex = Regex("[a-f0-9]{64}")
        require(transactionId.isNotBlank() &&
            listOf(checkpointSha256, ownerDirectionSha256,
                expectedCreatorRecordSha256, liveCreatorRecordSha256,
                expectedPersonalSha256, livePersonalSha256).all(hex::matches) &&
            expectedCreatorRecordSha256 == liveCreatorRecordSha256 &&
            expectedPersonalSha256 == livePersonalSha256 &&
            listOf(voiceIdeaCount, bestFrameCount, originalPhotoCount,
                selectedPhotoCount, preservedImportedJpegFiles).all { it >= 0 })
    }
    val mayReleaseJournal: Boolean get() = false
    val mayDeleteImportedMedia: Boolean get() = false
    val mayDeleteOriginalEscrow: Boolean get() = false
    val mayRearmAlarms: Boolean get() = false
}

internal class V377UnifiedFinalVerification(context: Context) {
    private val app = context.applicationContext
    private val h4 = V344UnifiedRestoreJournal(app)
    private val decisions = V347OwnerRecoveryDecisionJournal(app)

    internal suspend fun inspectOwnerApproved(
        freshOwnerConsent: Boolean,
        passphrase: CharArray,
    ): V377UnifiedFinalReadOnlyProof =
        verifyOwnerOutcome(freshOwnerConsent, passphrase, persistReceipt = false)

    /**
     * The SAME complete read-only forensics runs under BOTH owner locks.
     * Only after Creator/Personal/media are checked inside the locks may
     * the separate non-executable immutable outcome receipt be fsynced.
     * Does not advance H4, archive journals, delete media or schedule alarms.
     */
    internal suspend fun recordVerifiedOwnerOutcomeInternalOnly(
        freshOwnerConsent: Boolean,
        passphrase: CharArray,
    ): V377UnifiedFinalReadOnlyProof =
        verifyOwnerOutcome(freshOwnerConsent, passphrase, persistReceipt = true)

    private suspend fun verifyOwnerOutcome(
        freshOwnerConsent: Boolean,
        passphrase: CharArray,
        persistReceipt: Boolean,
    ): V377UnifiedFinalReadOnlyProof = withContext(Dispatchers.IO) {
        require(freshOwnerConsent && passphrase.size >= 12) {
            "Complete combined verification requires fresh owner consent and password"
        }
        val cp = h4.requireCheckpointReadOnly()
        check(cp.step != V344UnifiedRestoreStep.PREPARED_NO_WRITES) {
            "No live restore began; no full-domain release can be inferred"
        }
        val pair = V345UnifiedAuthenticatedRestoreParts(app)
            .authenticateForInterruptedRecovery(
                ownerConfirmedReview = true, passphrase = passphrase,
            )
        check(V345UnifiedAuthenticatedPairPolicy.matchesRecoveryCheckpoint(pair, cp)) {
            "Encrypted ORIGINAL/selected TARGET owner archive no longer matches H4"
        }
        val choice = decisions.inspectAuthenticatedForFinalReview(cp, pair)
        val outcome = if (choice.choice ==
            V347OwnerRecoveryChoice.REQUEST_REVERSE_ORIGINAL)
            V377VerifiedOwnerOutcome.ORIGINAL_RESTORED
        else V377VerifiedOwnerOutcome.SELECTED_TARGET_RESTORED
        check(if (outcome == V377VerifiedOwnerOutcome.SELECTED_TARGET_RESTORED)
            cp.step in setOf(
                V344UnifiedRestoreStep.PERSONAL_WRITTEN,
                V344UnifiedRestoreStep.VERIFYING_BOTH,
                V344UnifiedRestoreStep.BOTH_VERIFIED,
            )
        else true) {
            "A partially written selected TARGET cannot claim a completed restore"
        }
        val originalPersonal = section(
            "backlot-unified-owner-recovery-escrow-v1",
            "original-owner-encrypted-unified.zip", passphrase,
            cp.escrowSha256,
        )
        val incomingPersonal = section(
            "backlot-unified-owner-restore-preparation-v1",
            "incoming-unified-encrypted.zip", passphrase,
            cp.sourceSha256,
        )
        val merged = Phase1V2RestoreTargetPolicy.propose(
            V301LocalRecoverySnapshot(
                originalPersonal.first, originalPersonal.second, true,
            ),
            incomingPersonal.first, incomingPersonal.second,
        )
        val recipe = V356UnifiedPersonalRecoveryRecipeReader(app).inspectVerified(
            V346UnifiedInterruptedRecoveryReview(app).inspect(
                ownerConfirmedReview = true, passphrase = passphrase,
            ),
            passphrase,
        ).personalRecipe
        check(merged.baselineDigest == recipe.originalDigest &&
            merged.targetDigest == recipe.mergedTargetDigest &&
            recipe.allOriginalPhotoReferencesPreserved) {
            "Original/selected Personal sections do not reconstruct merged backup"
        }
        val expectedPhotos = if (outcome ==
            V377VerifiedOwnerOutcome.ORIGINAL_RESTORED)
            originalPersonal.second else merged.photos
        val expectedPersonal = if (outcome ==
            V377VerifiedOwnerOutcome.ORIGINAL_RESTORED)
            recipe.originalDigest else recipe.mergedTargetDigest

        // Original Voice media remains on-device even after a selected
        // target restore. Authenticate its independent archive bytes so
        // the live private Voice folder can be checked against the UNION
        // of approved old/selected SHA values (including orphan copies).
        val originalVoiceHashes = originalVoiceArchiveHashes(passphrase)

        // Both sealed archives were authenticated above. Stage only ONE
        // Creator section here; its media manifest independently SHA-checks
        // every recording and every Best Frame before live comparison.
        val selectedCreatorFolder = if (outcome ==
            V377VerifiedOwnerOutcome.ORIGINAL_RESTORED)
            "backlot-unified-owner-recovery-escrow-v1"
        else "backlot-unified-owner-restore-preparation-v1"
        val selectedCreatorFile = if (outcome ==
            V377VerifiedOwnerOutcome.ORIGINAL_RESTORED)
            "original-owner-encrypted-unified.zip"
        else "incoming-unified-encrypted.zip"
        val selectedCreatorSha = if (outcome ==
            V377VerifiedOwnerOutcome.ORIGINAL_RESTORED)
            pair.originalCreatorSha256 else pair.targetCreatorSha256
        val source = ownerArchive(selectedCreatorFolder, selectedCreatorFile)
        source.inputStream().buffered().use { encrypted ->
            V341UnifiedOwnerArchive.stageAndVerify(
                encrypted, passphrase, app.cacheDir,
            ).use { owner ->
                check(sha(owner.creator) == selectedCreatorSha) {
                    "Exact owner Creator inner ZIP has changed"
                }
                val staged = owner.creator.inputStream().buffered().use {
                    PortableBackupArchive.stage(it, app.cacheDir)
                }
                try {
                    check(staged.isArchive) {
                        "Full-domain verification requires portable Creator media"
                    }
                    val manager = CreatorBackupManager(app)
                    manager.verifyPortableJsonEnvelope(staged.backupJson)
                    val compact = PortableBestFrameMedia.verifyCompactPreview(
                        staged.backupJson, staged.bestFrames,
                        staged.hasFrameManifest,
                    )
                    if (compact != null)
                        manager.validatePortableCompact(staged.backupJson, compact)
                    else {
                        check(staged.bestFrames.isEmpty() &&
                            JSONObject(staged.backupJson)
                                .getString("personalFrames") == "[]") {
                            "Unverified legacy Best Frame content cannot be released"
                        }
                        manager.validate(staged.backupJson)
                    }
                    val stagedVoice = staged.mediaByIdeaId.mapValues { (_, file) ->
                        check(Files.isRegularFile(
                            file.toPath(), LinkOption.NOFOLLOW_LINKS,
                        ) && !Files.isSymbolicLink(file.toPath()) &&
                            file.length() in 1L..(512L * 1024L * 1024L)) {
                            "Invalid Creator Voice Idea media file in verified archive"
                        }
                        V366VoiceBytes(file.length(), sha(file))
                    }
                    StabilizationPersonalMutationGate.withPendingCombinedReadInspection(
                        app, cp,
                    ) {
                        CreatorDataGate.withPendingCombinedReadInspection(app, cp) {
                            val currentChoice =
                                decisions.inspectAuthenticatedForFinalReview(cp, pair)
                            check(currentChoice == choice &&
                                h4.requireCheckpointReadOnly() == cp &&
                                sha(ownerArchive(
                                    selectedCreatorFolder, selectedCreatorFile,
                                )) == (if (outcome ==
                                    V377VerifiedOwnerOutcome.ORIGINAL_RESTORED)
                                    cp.escrowSha256 else cp.sourceSha256) &&
                                !IdeaProjectConversion(app).hasPendingRecovery()) {
                                "Owner choice, encrypted source or conversion journal changed"
                            }
                            val creator = manager
                                .createCloudBackupForUnifiedReadOnlyBaselineInsideOwnerGate()
                            val personal = V301LocalRecoveryReader(app)
                                .readForInterruptedV2Recovery()
                            val h4Fingerprint = V343UnifiedBaselineReader(app)
                            val creatorExpected = if (outcome ==
                                V377VerifiedOwnerOutcome.ORIGINAL_RESTORED)
                                cp.creatorOriginalSha256
                            else h4Fingerprint.creatorRecordFingerprintIgnoringLocalVoicePaths(
                                staged.backupJson,
                            )
                            val creatorCurrent = if (outcome ==
                                V377VerifiedOwnerOutcome.ORIGINAL_RESTORED)
                                h4Fingerprint.fingerprintUnverifiedInterrupted(
                                    creator, personal,
                                ).creatorSha256
                            else h4Fingerprint.creatorRecordFingerprintIgnoringLocalVoicePaths(
                                creator,
                            )
                            val personalCurrent = if (outcome ==
                                V377VerifiedOwnerOutcome.ORIGINAL_RESTORED)
                                h4Fingerprint.personalFingerprintForInterrupted(personal)
                            else Phase1V2RestoreFingerprint.calculate(
                                personal.components, personal.photos,
                            )
                            check(creatorExpected == creatorCurrent &&
                                personalCurrent == expectedPersonal) {
                                "Live Creator or Personal records do not match selected owner outcome"
                            }
                            val voice = V367UnifiedCreatorOriginalVoiceSafety(app)
                                .inspectOriginalIdeaReferencesReadOnly(
                                    ideasJson = JSONObject(creator).getString("ideas"),
                                    expectedAudio = stagedVoice,
                                    voiceDir = File(app.filesDir, "voice_ideas"),
                                )
                            check(voice.verifiedOriginalVoiceCount == stagedVoice.size) {
                                "Voice Idea references are not byte-identical to selected archive"
                            }
                            // Unreferenced files are not silently
                            // ignored: either their bytes match an ORIGINAL
                            // or SELECTED owner archive recording, or Task 4
                            // refuses to release the recovery journal. No
                            // media is deleted or renamed in this audit.
                            auditRetainedVoiceFiles(
                                originalVoiceHashes.values.toSet() +
                                    stagedVoice.values.toSet(),
                            )
                            val originalFrameOrder =
                                CreatorHeroArchive.captureOriginalOrder(app)
                            check(V373RecoveredCreatorMediaPolicy
                                .liveFramesMatchVerifiedTarget(
                                    originalFrameOrder,
                                    staged.bestFrames, compact,
                                )) {
                                "Live Best Frames order or image SHA differs from owner ZIP"
                            }
                            val photos = Phase1V2PhotoByteForensics.inspect(
                                if (outcome ==
                                    V377VerifiedOwnerOutcome.ORIGINAL_RESTORED)
                                    Phase1V2RestoreStep.BEFORE_PHOTOS
                                else Phase1V2RestoreStep.VERIFYING,
                                File(app.filesDir, "backlot-personal-space-photos"),
                                originalPersonal.second, merged.photos,
                                personal.photos,
                            )
                            check(photos.pendingCopies == 0 &&
                                photos.verifiedFinalFiles >= expectedPhotos.size &&
                                personal.photos.sortedBy { it.id } ==
                                    expectedPhotos.sortedBy { it.id }) {
                                "Live Space photos, imported orphans or JPEG bytes are unsafe"
                            }

                            val handoff = V359UnifiedPersonalV2HandoffJournal(app)
                                .inspectReadOnly()
                            val archivedNative = if (outcome ==
                                V377VerifiedOwnerOutcome.SELECTED_TARGET_RESTORED) {
                                val record = (handoff as?
                                    V359UnifiedPersonalV2HandoffJournal.Inspection.Bound)
                                    ?.handoff ?: error(
                                    "Completed selected Personal needs durable v2 handoff",
                                )
                                check(V359PersonalV2HandoffCodec
                                    .matchesCompletedCombined(record, cp) &&
                                    record.originalPersonalDigest ==
                                        recipe.originalDigest &&
                                    record.mergedPersonalDigest ==
                                        recipe.mergedTargetDigest) {
                                    "Selected Personal native handoff is not owner bound"
                                }
                                val active = Phase1V2RecoveryRoots.alongsideLegacy(
                                    StabilizationPersonalMutationGate.journalRootFor(app),
                                )
                                val archived = File(
                                    File(app.filesDir,
                                        "backlot-personal-v2-completed-journals"),
                                    record.personalV2TransactionId,
                                )
                                check(!Files.exists(active.toPath(),
                                    LinkOption.NOFOLLOW_LINKS) &&
                                    archived.isDirectory &&
                                    !Files.isSymbolicLink(archived.toPath())) {
                                    "Native v2 restore is active, torn or absent"
                                }
                                val native = Phase1V2RestoreWriteAheadJournal(archived)
                                    .inspectAfterRestart()
                                check(native != null &&
                                    native.step == Phase1V2RestoreStep.VERIFYING &&
                                    V359PersonalV2HandoffCodec.matchesV2(
                                        record, native,
                                    )) {
                                    "No verified archived Personal import matches owner target"
                                }
                                true
                            } else {
                                val inverse = Phase1V2OriginalReverseJournal(app)
                                    .inspect()
                                if (inverse != null) {
                                    check(inverse.step ==
                                        Phase1V2ReverseStep.ORIGINAL_RECORDS_VERIFIED &&
                                        inverse.combinedId == cp.transactionId &&
                                        inverse.originalDigest == recipe.originalDigest &&
                                        inverse.targetDigest == recipe.mergedTargetDigest) {
                                        "Original Personal rollback action is not completed"
                                    }
                                } else {
                                    check(handoff ==
                                        V359UnifiedPersonalV2HandoffJournal
                                            .Inspection.NotPrepared &&
                                        personalCurrent ==
                                            cp.personalOriginalSha256) {
                                        "Original Personal claim has unknown pending native v2 evidence"
                                    }
                                }
                                false
                            }
                            check(h4.requireCheckpointReadOnly() == cp &&
                                decisions.inspectAuthenticatedForFinalReview(cp, pair) ==
                                    choice) {
                                "Owner source or H4 changed during verification"
                            }
                            val proof = V377UnifiedFinalReadOnlyProof(
                                transactionId = cp.transactionId,
                                checkpointSha256 =
                                    V359PersonalV2HandoffCodec.sha256(
                                        V344UnifiedRestoreCheckpointCodec.encode(cp),
                                    ),
                                ownerDirectionSha256 =
                                    V359PersonalV2HandoffCodec.sha256(
                                        V347OwnerRecoveryDecisionCodec.encode(choice),
                                    ),
                                outcome = outcome,
                                expectedCreatorRecordSha256 = creatorExpected,
                                liveCreatorRecordSha256 = creatorCurrent,
                                expectedPersonalSha256 = expectedPersonal,
                                livePersonalSha256 = personalCurrent,
                                voiceIdeaCount = stagedVoice.size,
                                bestFrameCount = staged.bestFrames.size,
                                originalPhotoCount = originalPersonal.second.size,
                                selectedPhotoCount = merged.photos.size,
                                preservedImportedJpegFiles = photos.verifiedNewFiles,
                                archivedNativePersonalVerified = archivedNative,
                            )
                            if (persistReceipt) {
                                check(h4.requireCheckpointReadOnly() == cp &&
                                    decisions.inspectAuthenticatedForFinalReview(cp, pair) ==
                                        choice) {
                                    "Owner decision changed before completion receipt fsync"
                                }
                                V378UnifiedVerifiedEvidenceJournal(app)
                                    .prepareUnderBothOwnerReadLocks(
                                        ownerConfirmed = true,
                                        checkpoint = cp,
                                        decision = choice,
                                        proof = proof,
                                    )
                            }
                            proof
                        }
                    }
                } finally {
                    staged.cleanup()
                }
            }
        }
    }

    private fun originalVoiceArchiveHashes(
        passphrase: CharArray,
    ): Map<String, V366VoiceBytes> {
        val original = ownerArchive(
            "backlot-unified-owner-recovery-escrow-v1",
            "original-owner-encrypted-unified.zip",
        )
        return original.inputStream().buffered().use { input ->
            V341UnifiedOwnerArchive.stageAndVerify(
                input, passphrase, app.cacheDir,
            ).use { verified ->
                val staged = verified.creator.inputStream().buffered().use {
                    PortableBackupArchive.stage(it, app.cacheDir)
                }
                try {
                    check(staged.isArchive) {
                        "Retained original Creator archive must carry a voice media manifest"
                    }
                    staged.mediaByIdeaId.mapValues { (_, file) ->
                        check(Files.isRegularFile(
                            file.toPath(), LinkOption.NOFOLLOW_LINKS,
                        ) && !Files.isSymbolicLink(file.toPath()) &&
                            file.length() in 1L..(512L * 1024L * 1024L)) {
                            "Invalid ORIGINAL encrypted Voice Idea file"
                        }
                        V366VoiceBytes(file.length(), sha(file))
                    }
                } finally { staged.cleanup() }
            }
        }
    }

    private fun auditRetainedVoiceFiles(
        expectedMedia: Set<V366VoiceBytes>,
    ): Int {
        val root = File(app.filesDir, "voice_ideas")
        if (!Files.exists(root.toPath(), LinkOption.NOFOLLOW_LINKS)) {
            check(expectedMedia.isEmpty()) {
                "Original or restored Voice Idea folder is missing"
            }
            return 0
        }
        check(Files.isDirectory(root.toPath(), LinkOption.NOFOLLOW_LINKS) &&
            !Files.isSymbolicLink(root.toPath()) &&
            root.canonicalFile.parentFile == app.filesDir.canonicalFile) {
            "Private Voice Idea folder is unsafe"
        }
        val files = requireNotNull(root.listFiles()) {
            "Could not inspect original and newly copied Voice Idea media"
        }
        check(files.size <= 10_000) {
            "Unbounded unknown Voice Idea media prevents final release"
        }
        files.forEach { file ->
            check(Files.isRegularFile(file.toPath(), LinkOption.NOFOLLOW_LINKS) &&
                !Files.isSymbolicLink(file.toPath()) &&
                file.canonicalFile.parentFile == root.canonicalFile &&
                file.length() in 1L..(512L * 1024L * 1024L) &&
                V366VoiceBytes(file.length(), sha(file)) in expectedMedia) {
                "Unknown, altered or partial Voice Idea copy must remain blocked for owner review"
            }
        }
        return files.size
    }

    private fun section(
        folder: String,
        filename: String,
        passphrase: CharArray,
        expectedSha: String,
    ): Pair<V300PersonalComponents, List<V290SpacePhotoEntity>> {
        val archive = ownerArchive(folder, filename)
        check(sha(archive) == expectedSha) {
            "Personal archive changed after owner authentication"
        }
        return archive.inputStream().buffered().use { encrypted ->
            V341UnifiedOwnerArchive.stageAndVerify(
                encrypted, passphrase, app.cacheDir,
            ).use { verified ->
                verified.personal.inputStream().buffered().use { personal ->
                    V300EncryptedPersonalArchive.stageAndVerify(
                        personal, passphrase, app.cacheDir,
                    ).use { inner ->
                        inner.components to inner.photos.map { it.photo }
                    }
                }
            }
        }
    }

    private fun ownerArchive(folder: String, filename: String): File {
        val root = File(app.filesDir, folder)
        val file = File(root, filename)
        check(root.isDirectory &&
            !Files.isSymbolicLink(root.toPath()) &&
            root.canonicalFile.parentFile == app.filesDir.canonicalFile &&
            Files.isRegularFile(file.toPath(), LinkOption.NOFOLLOW_LINKS) &&
            !Files.isSymbolicLink(file.toPath()) &&
            file.canonicalFile.parentFile == root.canonicalFile) {
            "Owner original or selected encrypted ZIP is missing or unsafe"
        }
        return file
    }

    private fun sha(file: File): String {
        val hash = MessageDigest.getInstance("SHA-256")
        file.inputStream().buffered().use { input ->
            val bytes = ByteArray(64 * 1024)
            while (true) {
                val n = input.read(bytes)
                if (n < 0) break
                if (n > 0) hash.update(bytes, 0, n)
            }
        }
        return hash.digest().joinToString("") {
            "%02x".format(it.toInt() and 255)
        }
    }
}
