package com.framebynavin.app.data

import android.content.Context
import java.io.File
import java.nio.file.Files
import java.nio.file.LinkOption
import java.security.MessageDigest
import org.json.JSONObject

/**
 * H3 Task 2 — NO-WRITE inspection of Creator's already-existing standalone
 * pre-restore checkpoint while the combined H4 journal is BEFORE_CREATOR.
 *
 * PortableCreatorBackupManager can install Voice Idea media BEFORE it calls
 * CreatorBackupManager.restorePortableCompact(), so an H4 BEFORE_CREATOR
 * checkpoint without a native Creator marker is NOT proof that no media was
 * copied. Refuse a Creator replay in that case until its media provenance
 * is reconciled. Orphan files are always preserved, never auto-deleted.
 *
 * When Creator's native marker DOES exist, verify its independently retained
 * canonical JSON, exact SHA, original Best Frame order and image bytes, then
 * compare the original canonical Creator fingerprint to the immutable H4
 * original. An unrelated Creator marker can never be replayed or rolled back
 * under a different owner transaction.
 *
 * This proof does NOT grant a Creator write permit, resume an import,
 * rewrite Voice Idea paths, change Best Frame order, delete media, release H4
 * or schedule project alarms.
 */
internal class V364UnifiedCreatorPendingRecoveryProof(context: Context) {
    private val app = context.applicationContext
    private val h4 = V344UnifiedRestoreJournal(app)

    sealed interface Assessment {
        data object WrongCombinedStage : Assessment
        data object NoNativeCreatorJournalMediaMayExist : Assessment
        data object UnreadableOrUnboundNativeEvidence : Assessment
        data class BoundOriginal(
            val combinedTransactionId: String,
            val nativeOriginalFileName: String,
            val nativeOriginalSha256: String,
            val originalCreatorFingerprint: String,
            val verifiedOriginalBestFrameCount: Int,
            val preexistingVoiceOrphansCanBeRuledOut: Boolean = false,
        ) : Assessment {
            val mayReplayCreator: Boolean get() = false
            val mayRollbackCreator: Boolean get() = false
            val mayDeleteOwnedMedia: Boolean get() = false
            val mayReleaseRecovery: Boolean get() = false
        }
    }

    fun inspectReadOnly(): Assessment {
        val checkpoint = runCatching {
            h4.requireCheckpointReadOnly()
        }.getOrNull() ?: return Assessment.UnreadableOrUnboundNativeEvidence
        if (checkpoint.step != V344UnifiedRestoreStep.BEFORE_CREATOR) {
            return Assessment.WrongCombinedStage
        }
        val root = File(app.filesDir, "creator_restore_recovery_v181")
        val pending = File(root, "pending.json")
        val allEvidence = listOf(
            pending,
            File(root, "pending.json.new"),
            File(root, "pending.json.bak"),
        )
        val presence = allEvidence.map {
            Files.exists(it.toPath(), LinkOption.NOFOLLOW_LINKS)
        }
        if (!presence[0]) {
            return if (!presence[1] && !presence[2] &&
                (!Files.exists(root.toPath(), LinkOption.NOFOLLOW_LINKS) ||
                    (root.isDirectory &&
                        !Files.isSymbolicLink(root.toPath()))))
                Assessment.NoNativeCreatorJournalMediaMayExist
            else Assessment.UnreadableOrUnboundNativeEvidence
        }
        if (presence[1] || presence[2]) {
            return Assessment.UnreadableOrUnboundNativeEvidence
        }

        return runCatching {
            check(root.isDirectory &&
                !Files.isSymbolicLink(root.toPath()) &&
                root.canonicalFile.parentFile == app.filesDir.canonicalFile) {
                "Creator recovery directory is untrusted"
            }
            check(pending.isFile && !Files.isSymbolicLink(pending.toPath()) &&
                pending.length() in 1L..4096L) {
                "Creator native owner marker is torn or oversized"
            }
            val j = JSONObject(pending.readText())
            val keys = j.keys().asSequence().toSet()
            check(keys == setOf("file", "sha256", "originalFrames",
                "originalFramesSha256")) {
                "Combined portable Creator recovery must carry original frame-byte proof"
            }
            val name = j.getString("file")
            val savedSha = j.getString("sha256")
            val originalFrames = j.getString("originalFrames")
            check(name.matches(Regex("pre-restore-[a-zA-Z0-9_-]+\\.fbnbackup")) &&
                savedSha.matches(Regex("[a-f0-9]{64}")) &&
                j.getString("originalFramesSha256") == sha(originalFrames)) {
                "Creator native recovery identity, digest or Best Frames proof invalid"
            }
            val copy = File(root, name)
            check(copy.isFile && !Files.isSymbolicLink(copy.toPath()) &&
                copy.canonicalFile.parentFile == root.canonicalFile &&
                copy.length() in 1L..(128L * 1024L * 1024L)) {
                "Untrusted Creator native recovery backup"
            }
            val raw = copy.readText()
            check(sha(raw) == savedSha) {
                "Creator pre-restore backup differs from its durable native checkpoint"
            }
            CreatorBackupManager(app).validate(raw)
            val originals = CreatorHeroArchive.verifyOriginalOrderSourceFiles(
                app, originalFrames,
            )
            val digest = V343UnifiedBaselineReader(app)
                .creatorFingerprintFromNativeOriginal(raw, originalFrames)
            check(digest == checkpoint.creatorOriginalSha256 &&
                h4.requireCheckpointReadOnly() == checkpoint) {
                "Native Creator recovery original does not match the H4 original"
            }
            Assessment.BoundOriginal(
                combinedTransactionId = checkpoint.transactionId,
                nativeOriginalFileName = name,
                nativeOriginalSha256 = savedSha,
                originalCreatorFingerprint = digest,
                verifiedOriginalBestFrameCount = originals.size,
            )
        }.getOrElse {
            Assessment.UnreadableOrUnboundNativeEvidence
        }
    }

    private fun sha(value: String): String =
        MessageDigest.getInstance("SHA-256")
            .digest(value.toByteArray(Charsets.UTF_8))
            .joinToString("") { "%02x".format(it.toInt() and 255) }
}
