package com.framebynavin.app.data

import android.content.Context
import com.framebynavin.app.personal.StabilizationPersonalMutationGate
import com.framebynavin.app.personal.V301LocalRecoveryReader
import java.io.File
import java.nio.channels.Channels
import java.nio.channels.FileChannel
import java.nio.file.Files
import java.nio.file.LinkOption
import java.nio.file.StandardOpenOption
import java.security.MessageDigest
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject

/**
 * H3 TASK 2: narrowly scoped missing-only media replay under an interrupted
 * BEFORE_CREATOR journal, WITHOUT re-running Creator's JSON restore.
 *
 * An owner may have chosen FORWARD_TARGET after a crash between V365's durable
 * Voice Idea allocation and the native Creator pending.json. Original data
 * must still match the encrypted original escrow in BOTH Creator and Personal.
 * This procedure can safely finish MISSING preallocated TARGET audio copies:
 *
 * - a previously COMPLETE matching file remains unchanged;
 * - an absent file is created using CREATE_NEW and synced, never overwritten;
 * - a partial, modified, symlinked or nonregular destination BLOCKS the entire
 *   replay BEFORE any copy, and all incomplete evidence remains untouched.
 *
 * H4 remains BEFORE_CREATOR, Creator JSON/Best Frames are not imported, and
 * the immutable V365 allocation and owner decision remain intact. A crash
 * while copying may leave an incomplete file; that is deliberately preserved
 * as evidence for later owner-reviewed repair rather than silently deleted.
 *
 * INTERNAL ONLY. Not wired into UI, alarms, background jobs or Restore All.
 */
internal object V370MissingVoicePolicy {
    fun mayReplayMissing(audit: V365VoiceCopyAudit, originalStateVerified: Boolean): Boolean =
        originalStateVerified && audit.noWriteAuthority &&
            audit.unsafeCopies == 0 &&
            audit.alteredOrPartialCopies == 0 &&
            audit.matchingCopies + audit.missingCopies == audit.planned
}

internal data class V370MissingVoiceReceipt(
    val transactionId: String,
    val planned: Int,
    val newlyCopied: Int,
    val preservedMatching: Int,
    val finalMatching: Int,
    val originalDomainsUntouched: Boolean = true,
) {
    init {
        require(transactionId.isNotBlank() && planned >= 0 &&
            newlyCopied >= 0 && preservedMatching >= 0 &&
            finalMatching == planned &&
            newlyCopied + preservedMatching == planned &&
            originalDomainsUntouched) {
            "Media-only replay receipt cannot imply cross-domain restore success"
        }
    }
    val mayAdvanceH4: Boolean get() = false
    val mayDeleteAnyMedia: Boolean get() = false
    val mayWriteCreatorRecords: Boolean get() = false
    val mayScheduleReminders: Boolean get() = false
    val mayReleaseOwnerRecovery: Boolean get() = false
}

internal class V370UnifiedCreatorMissingVoiceReplay(context: Context) {
    private val app = context.applicationContext
    private val journal = V344UnifiedRestoreJournal(app)
    private val allocations = V365UnifiedCreatorVoiceInstallJournal(app)
    private val decisions = V347OwnerRecoveryDecisionJournal(app)
    private val native = V364UnifiedCreatorPendingRecoveryProof(app)

    internal suspend fun replayMissingOnlyInternal(
        freshOwnerConsent: Boolean,
        passphrase: CharArray,
    ): V370MissingVoiceReceipt = withContext(Dispatchers.IO) {
        require(freshOwnerConsent && passphrase.size >= 12) {
            "Explicit owner confirmation and encrypted archive passphrase required"
        }
        val cp = journal.requireCheckpointReadOnly()
        require(cp.step == V344UnifiedRestoreStep.BEFORE_CREATOR) {
            "Missing Voice replay requires interrupted BEFORE_CREATOR"
        }
        // Expensive authenticated review happens BEFORE holding domain mutexes.
        val report = V369UnifiedCreatorPreNativeOriginalInspector(app)
            .inspectInternalOwnerReviewed(
                freshOwnerConsent = true, passphrase = passphrase,
                expectedDirection = V347OwnerRecoveryChoice.REQUEST_FORWARD_TARGET,
            )
        check(report.transactionId == cp.transactionId &&
            V370MissingVoicePolicy.mayReplayMissing(
                report.allocatedTargetCopies, originalStateVerified = true,
            )) {
            "Unsafe or altered target Voice Idea copies require separate owner review"
        }
        val originalAudio = V366UnifiedCreatorVoiceRecoveryInspector(app)
            .inspectOwnerReviewed(ownerConfirmed = true, passphrase = passphrase)
        check(originalAudio.transactionId == cp.transactionId &&
            originalAudio.installedCopies == report.allocatedTargetCopies) {
            "Authenticated original Voice Idea proof differs from owner media review"
        }
        val sealed = V345UnifiedAuthenticatedRestoreParts(app)
            .authenticateForInterruptedRecovery(
                ownerConfirmedReview = true, passphrase = passphrase,
            )
        check(V345UnifiedAuthenticatedPairPolicy.matchesRecoveryCheckpoint(sealed, cp)) {
            "Encrypted target or original escrow no longer matches H4"
        }
        val target = exactTarget()
        check(sha256(target) == cp.sourceSha256) {
            "Owner selected encrypted target has changed"
        }
        target.inputStream().buffered().use { encrypted ->
            V341UnifiedOwnerArchive.stageAndVerify(
                encrypted, passphrase, app.cacheDir,
            ).use { unpacked ->
                check(sha256(unpacked.creator) == sealed.targetCreatorSha256) {
                    "Authenticated target Creator ZIP differs from H4 evidence"
                }
                // Verify EVERY ZIP entry, audio reference and frame byte before
                // selecting any app-private destination for writing.
                unpacked.creator.inputStream().buffered().use {
                    PortableCreatorBackupManager(app).validate(it)
                }
                val staged = unpacked.creator.inputStream().buffered().use {
                    PortableBackupArchive.stage(it, app.cacheDir)
                }
                try {
                    check(staged.isArchive) {
                        "Unified Creator recovery requires the owned-media ZIP"
                    }
                    val plan = allocations.inspectReadOnly()
                        ?: error("Immutable Creator Voice Idea allocation missing")
                    check(plan.transactionId == cp.transactionId &&
                        plan.sourceSha256 == cp.sourceSha256 &&
                        plan.escrowSha256 == cp.escrowSha256 &&
                        matchesStagedTarget(plan, staged.mediaByIdeaId)) {
                        "Allocated Voice Idea destinations differ from authenticated TARGET"
                    }
                    val originalDirection = (decisions.readOnlyStatus() as?
                        V347OwnerRecoveryDecisionJournal.ReadState.Recorded)?.choice
                        ?: error("No intact owner recovery choice")
                    check(originalDirection.choice ==
                        V347OwnerRecoveryChoice.REQUEST_FORWARD_TARGET &&
                        V347OwnerRecoveryDecisionPolicy.remainsBound(
                            originalDirection, cp,
                        )) { "No owner approval for forward-target media recovery" }
                    StabilizationPersonalMutationGate.withCombinedCreatorMissingVoiceFence(
                        app, cp, ownerConfirmed = true,
                    ) {
                        CreatorDataGate.withJournalBoundCreatorWrite(
                            app, cp, ownerConfirmed = true,
                        ) {
                            check(!IdeaProjectConversion(app).hasPendingRecovery() &&
                                journal.requireCheckpointReadOnly() == cp &&
                                native.inspectReadOnly() is
                                    V364UnifiedCreatorPendingRecoveryProof.Assessment
                                        .VoiceAllocatedBeforeNativeCreatorJournal &&
                                allocations.inspectReadOnly() == plan &&
                                decisions.readOnlyStatus() ==
                                    V347OwnerRecoveryDecisionJournal.ReadState.Recorded(
                                        originalDirection,
                                    )) {
                                "Creator native or owner recovery evidence changed before replay"
                            }
                            // Recheck the actual owner records under Personal
                            // FIRST, Creator SECOND mutex, just before a file
                            // can be created. Only read Personal; never write.
                            val creatorJson = CreatorBackupManager(app)
                                .createCloudBackupForUnifiedReadOnlyBaselineInsideOwnerGate()
                            val personal = V301LocalRecoveryReader(app)
                                .readForInterruptedV2Recovery()
                            val baseline = V343UnifiedBaselineReader(app)
                                .fingerprintUnverifiedInterrupted(creatorJson, personal)
                            val photos = V354UnifiedLiveSpacePhotoAudit(app)
                                .inspect(personal.photos)
                            val verifiedOriginalVoice =
                                V367UnifiedCreatorOriginalVoiceSafety(app)
                                    .inspectOriginalIdeaReferencesReadOnly(
                                        ideasJson = JSONObject(creatorJson).getString("ideas"),
                                        expectedAudio = originalAudio.originalMediaHashes,
                                        voiceDir = File(app.filesDir, "voice_ideas"),
                                    )
                            check(baseline.creatorSha256 == cp.creatorOriginalSha256 &&
                                baseline.personalSha256 == cp.personalOriginalSha256 &&
                                photos.currentPhotoFilesMatchTheirLiveRows &&
                                verifiedOriginalVoice.ideaCount == report.originalIdeaCount &&
                                verifiedOriginalVoice.verifiedOriginalVoiceCount ==
                                    report.originalVoiceFileCount &&
                                sha256(target) == cp.sourceSha256) {
                                "Owner original data changed before missing Voice replay"
                            }
                            val before = allocations.auditReadOnly(plan)
                            check(V370MissingVoicePolicy.mayReplayMissing(
                                before, originalStateVerified = true,
                            )) { "Partial/altered Voice Idea media must never be overwritten" }
                            val newCount = copyMissingOnly(
                                plan, staged.mediaByIdeaId,
                            )
                            val after = allocations.auditReadOnly(plan)
                            check(after.matchingCopies == plan.entries.size &&
                                after.missingCopies == 0 &&
                                after.alteredOrPartialCopies == 0 &&
                                after.unsafeCopies == 0 &&
                                newCount == before.missingCopies &&
                                journal.requireCheckpointReadOnly() == cp &&
                                decisions.readOnlyStatus() ==
                                    V347OwnerRecoveryDecisionJournal.ReadState.Recorded(
                                        originalDirection,
                                    ) &&
                                native.inspectReadOnly() is
                                    V364UnifiedCreatorPendingRecoveryProof.Assessment
                                        .VoiceAllocatedBeforeNativeCreatorJournal) {
                                "Voice copy or recovery journal verification failed; preserve evidence"
                            }
                            V370MissingVoiceReceipt(
                                transactionId = cp.transactionId,
                                planned = plan.entries.size,
                                newlyCopied = newCount,
                                preservedMatching = before.matchingCopies,
                                finalMatching = after.matchingCopies,
                            )
                        }
                    }
                } finally {
                    staged.cleanup()
                }
            }
        }
    }

    private fun matchesStagedTarget(
        plan: V365VoiceCopyPlan,
        staged: Map<String, File>,
    ): Boolean =
        V366CreatorVoiceRecoveryPolicy.matchesTarget(
            plan, staged.mapValues { (_, file) ->
                require(file.isFile &&
                    !Files.isSymbolicLink(file.toPath()) &&
                    file.length() in 1L..(512L * 1024L * 1024L)) {
                    "Unsafe staged target audio"
                }
                V366VoiceBytes(file.length(), sha256(file))
            },
        )

    /** CREATE_NEW means no existing file, even a partial one, can be replaced. */
    private fun copyMissingOnly(
        plan: V365VoiceCopyPlan,
        staged: Map<String, File>,
    ): Int {
        val root = File(app.filesDir, "voice_ideas")
        check(!Files.isSymbolicLink(root.toPath()) &&
            root.canonicalFile.parentFile == app.filesDir.canonicalFile) {
            "Untrusted app-private Voice Idea directory"
        }
        if (!root.exists()) check(root.mkdir()) {
            "Could not create app-private Voice Idea folder"
        }
        check(root.isDirectory && !Files.isSymbolicLink(root.toPath())) {
            "Voice Idea directory is not regular"
        }
        var copied = 0
        for (entry in plan.entries) {
            val source = requireNotNull(staged[entry.ideaId])
            check(source.length() == entry.sizeBytes &&
                sha256(source) == entry.sha256) {
                "Target Voice Idea staged bytes changed"
            }
            val destination = File(root, entry.fileName)
            check(destination.canonicalFile.parentFile == root.canonicalFile) {
                "Unsafe preallocated Voice Idea destination"
            }
            if (Files.exists(destination.toPath(), LinkOption.NOFOLLOW_LINKS)) {
                check(Files.isRegularFile(destination.toPath(), LinkOption.NOFOLLOW_LINKS) &&
                    !Files.isSymbolicLink(destination.toPath()) &&
                    destination.length() == entry.sizeBytes &&
                    sha256(destination) == entry.sha256) {
                    "Existing partial or changed Voice Idea file must be preserved for review"
                }
                continue
            }
            FileChannel.open(
                destination.toPath(),
                StandardOpenOption.CREATE_NEW,
                StandardOpenOption.WRITE,
            ).use { channel ->
                source.inputStream().buffered().use { input ->
                    input.copyTo(Channels.newOutputStream(channel), 64 * 1024)
                }
                channel.force(true)
            }
            check(destination.length() == entry.sizeBytes &&
                sha256(destination) == entry.sha256) {
                "Incomplete Voice Idea copy preserved; stop recovery"
            }
            copied++
        }
        return copied
    }

    private fun exactTarget(): File {
        val folder = File(
            app.filesDir, "backlot-unified-owner-restore-preparation-v1",
        )
        check(folder.isDirectory && !Files.isSymbolicLink(folder.toPath()) &&
            folder.canonicalFile.parentFile == app.filesDir.canonicalFile) {
            "Untrusted selected encrypted owner folder"
        }
        val file = File(folder, "incoming-unified-encrypted.zip")
        check(file.isFile && !Files.isSymbolicLink(file.toPath()) &&
            file.canonicalFile.parentFile == folder.canonicalFile) {
            "Untrusted selected encrypted owner archive"
        }
        return file
    }

    private fun sha256(file: File): String {
        val digest = MessageDigest.getInstance("SHA-256")
        file.inputStream().buffered().use { input ->
            val buffer = ByteArray(64 * 1024)
            while (true) {
                val count = input.read(buffer)
                if (count < 0) break
                if (count > 0) digest.update(buffer, 0, count)
            }
        }
        return digest.digest().joinToString("") {
            "%02x".format(it.toInt() and 255)
        }
    }
}
