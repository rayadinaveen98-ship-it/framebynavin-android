package com.framebynavin.app.data

import android.content.Context
import android.util.AtomicFile
import java.io.File
import java.io.FileOutputStream
import java.nio.file.Files
import java.nio.file.LinkOption
import java.security.MessageDigest
import org.json.JSONObject

/**
 * Durable, non-destructive outcome evidence prepared ONLY after V377's
 * independent both-domain/media proof. This ledger does not mark the
 * combined restore complete, close any writer gate, delete source/escrow,
 * archive a recovery journal, or schedule alarms.
 */
internal class V378UnifiedVerifiedEvidenceJournal(context: Context) {
    private val app = context.applicationContext
    private val root = File(app.filesDir, "backlot-unified-verified-outcome-v1")
    private val active get() = File(root, "verified.json")
    private val keys = setOf(
        "format", "transactionId", "checkpointSha256",
        "ownerDirectionSha256", "outcome", "expectedCreatorRecordSha256",
        "liveCreatorRecordSha256", "expectedPersonalSha256",
        "livePersonalSha256", "voiceIdeaCount", "bestFrameCount",
        "originalPhotoCount", "selectedPhotoCount",
        "preservedImportedJpegFiles", "archivedNativePersonalVerified", "sha256",
    )

    fun hasEvidence(): Boolean =
        Files.exists(root.toPath(), LinkOption.NOFOLLOW_LINKS)

    fun inspect(): V377UnifiedFinalReadOnlyProof? {
        if (!hasEvidence()) return null
        check(root.isDirectory && !Files.isSymbolicLink(root.toPath()) &&
            root.canonicalFile.parentFile == app.filesDir.canonicalFile &&
            root.list()?.toSet() == setOf(active.name) &&
            Files.isRegularFile(active.toPath(), LinkOption.NOFOLLOW_LINKS) &&
            !Files.isSymbolicLink(active.toPath()) &&
            active.length() in 1L..4096L) {
            "Unknown or interrupted final verification evidence cannot be released"
        }
        val data = JSONObject(active.readText())
        check(data.keys().asSequence().toSet() == keys &&
            data.getString("format") == FORMAT) {
            "Unrecognized final verification evidence format"
        }
        val result = V377UnifiedFinalReadOnlyProof(
            transactionId = data.getString("transactionId"),
            checkpointSha256 = data.getString("checkpointSha256"),
            ownerDirectionSha256 = data.getString("ownerDirectionSha256"),
            outcome = V377VerifiedOwnerOutcome.valueOf(
                data.getString("outcome"),
            ),
            expectedCreatorRecordSha256 =
                data.getString("expectedCreatorRecordSha256"),
            liveCreatorRecordSha256 =
                data.getString("liveCreatorRecordSha256"),
            expectedPersonalSha256 = data.getString("expectedPersonalSha256"),
            livePersonalSha256 = data.getString("livePersonalSha256"),
            voiceIdeaCount = data.getInt("voiceIdeaCount"),
            bestFrameCount = data.getInt("bestFrameCount"),
            originalPhotoCount = data.getInt("originalPhotoCount"),
            selectedPhotoCount = data.getInt("selectedPhotoCount"),
            preservedImportedJpegFiles = data.getInt("preservedImportedJpegFiles"),
            archivedNativePersonalVerified =
                data.getBoolean("archivedNativePersonalVerified"),
        )
        check(data.getString("sha256") == sha256(canonical(result))) {
            "Final outcome evidence digest changed"
        }
        return result
    }

    /** Never overwrite a prior/torn completion receipt, even on owner retry. */
    fun prepareUnderBothOwnerReadLocks(
        ownerConfirmed: Boolean,
        checkpoint: V344UnifiedRestoreCheckpoint,
        decision: V347OwnerRecoveryDecision,
        proof: V377UnifiedFinalReadOnlyProof,
    ): V377UnifiedFinalReadOnlyProof {
        require(ownerConfirmed && !proof.mayReleaseJournal &&
            !proof.mayDeleteOriginalEscrow && !proof.mayRearmAlarms &&
            proof.transactionId == checkpoint.transactionId &&
            proof.checkpointSha256 == sha256(
                V344UnifiedRestoreCheckpointCodec.encode(checkpoint),
            ) &&
            proof.ownerDirectionSha256 == sha256(
                V347OwnerRecoveryDecisionCodec.encode(decision),
            ) &&
            (if (proof.outcome == V377VerifiedOwnerOutcome.ORIGINAL_RESTORED)
                decision.choice == V347OwnerRecoveryChoice.REQUEST_REVERSE_ORIGINAL
            else decision.choice == V347OwnerRecoveryChoice.REQUEST_FORWARD_TARGET &&
                checkpoint.step == V344UnifiedRestoreStep.BOTH_VERIFIED)) {
            "Outcome proof is not bound to the exact owner-approved final checkpoint"
        }
        check(!hasEvidence() &&
            root.canonicalFile.parentFile == app.filesDir.canonicalFile &&
            root.mkdir() && !Files.isSymbolicLink(root.toPath())) {
            "Another or torn combined outcome receipt exists; owner review required"
        }
        val payload = JSONObject()
            .put("format", FORMAT)
            .put("transactionId", proof.transactionId)
            .put("checkpointSha256", proof.checkpointSha256)
            .put("ownerDirectionSha256", proof.ownerDirectionSha256)
            .put("outcome", proof.outcome.name)
            .put("expectedCreatorRecordSha256", proof.expectedCreatorRecordSha256)
            .put("liveCreatorRecordSha256", proof.liveCreatorRecordSha256)
            .put("expectedPersonalSha256", proof.expectedPersonalSha256)
            .put("livePersonalSha256", proof.livePersonalSha256)
            .put("voiceIdeaCount", proof.voiceIdeaCount)
            .put("bestFrameCount", proof.bestFrameCount)
            .put("originalPhotoCount", proof.originalPhotoCount)
            .put("selectedPhotoCount", proof.selectedPhotoCount)
            .put("preservedImportedJpegFiles", proof.preservedImportedJpegFiles)
            .put("archivedNativePersonalVerified",
                proof.archivedNativePersonalVerified)
            .put("sha256", sha256(canonical(proof)))
            .toString().toByteArray(Charsets.UTF_8)
        check(payload.size in 1..4096) { "Invalid combined owner outcome receipt size" }
        val file = AtomicFile(active)
        var stream: FileOutputStream? = null
        try {
            stream = file.startWrite()
            stream.write(payload)
            stream.fd.sync()
            file.finishWrite(stream)
            stream = null
        } catch (error: Throwable) {
            stream?.let(file::failWrite)
            throw error
        }
        check(inspect() == proof) {
            "Owner's final combined outcome was not durably recorded"
        }
        return proof
    }

    companion object {
        private const val FORMAT = "backlot.unified.verified-outcome.v1"
        private fun canonical(p: V377UnifiedFinalReadOnlyProof): ByteArray =
            listOf(
                FORMAT, p.transactionId, p.checkpointSha256,
                p.ownerDirectionSha256, p.outcome.name,
                p.expectedCreatorRecordSha256, p.liveCreatorRecordSha256,
                p.expectedPersonalSha256, p.livePersonalSha256,
                p.voiceIdeaCount.toString(), p.bestFrameCount.toString(),
                p.originalPhotoCount.toString(), p.selectedPhotoCount.toString(),
                p.preservedImportedJpegFiles.toString(),
                p.archivedNativePersonalVerified.toString(),
            ).joinToString("\n", postfix = "\n")
                .toByteArray(Charsets.UTF_8)

        internal fun sha256(bytes: ByteArray): String =
            MessageDigest.getInstance("SHA-256")
                .digest(bytes).joinToString("") {
                    "%02x".format(it.toInt() and 255)
                }
    }
}
