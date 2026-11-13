package com.framebynavin.app.data

import android.content.Context
import com.framebynavin.app.personal.V301LocalRecoveryReader
import com.framebynavin.app.personal.V301LocalRecoverySnapshot
import java.security.MessageDigest
import org.json.JSONObject

/**
 * An H3 drift detector, NOT an atomic cross-store transaction.
 *
 * Capture on BOTH sides of making a rollback escrow. If Creator, its original
 * Best Frames, or any of Personal's nine domains drift during the export,
 * fail closed rather than treating a torn pair of backups as a safe baseline.
 * This cannot rule out all concurrent ABA writes; H3 cross-store write locking
 * is still required before the production restore commit is enabled.
 */
internal data class V343UnifiedBaselineFingerprint(
    val creatorSha256: String,
    val personalSha256: String,
) {
    val valid: Boolean get() =
        Regex("[0-9a-f]{64}").matches(creatorSha256) &&
            Regex("[0-9a-f]{64}").matches(personalSha256)
}

internal class V343UnifiedBaselineReader(context: Context) {
    private val app = context.applicationContext

    suspend fun capture(): V343UnifiedBaselineFingerprint {
        // CreatorDataGate protects its canonical snapshot. Best Frames are
        // separately hash-checked by their own owner archive policy.
        val creatorRoot = JSONObject(
            CreatorBackupManager(app).createCloudBackupForUnifiedReadOnlyBaseline()
        )
        val local = V301LocalRecoveryReader(app).readV2()
        return hashReadOnlySnapshots(creatorRoot, local, requireIntegrity = true)
    }

    /**
     * H3 interrupted recovery may have partially changed live Personal
     * records/photos. Its V301 forensic reader explicitly reports
     * localIntegrityVerified=false. The resulting hashes are EVIDENCE ONLY:
     * never an assertion that a partial restore can be replayed or rolled back.
     */
    internal fun fingerprintUnverifiedInterrupted(
        creatorRaw: String,
        personal: V301LocalRecoverySnapshot,
    ): V343UnifiedBaselineFingerprint =
        hashReadOnlySnapshots(JSONObject(creatorRaw), personal, requireIntegrity = false)

    /**
     * H3 Task 2: recompute the SAME original Creator domain fingerprint
     * from a separately SHA-validated native pre-restore JSON and its
     * durable original-Best-Frames checkpoint. No live reads or writes.
     */
    internal fun creatorFingerprintFromNativeOriginal(
        nativeBackupRaw: String,
        originalFrames: String,
    ): String = fingerprintCreatorRoot(JSONObject(nativeBackupRaw), originalFrames)

    /**
     * Compare exact logical Creator records before/after native recovery.
     * The Hero/Best Frames byte order is independently checked through
     * CreatorHeroArchive.captureOriginalOrder(): random new restored file
     * names must not cause a false records mismatch. Neither envelope
     * createdAtMillis nor payloadSha256 participates in owner-data hashes.
     */
    internal fun creatorRecordsFingerprintForRecovery(raw: String): String =
        fingerprintCreatorRoot(JSONObject(raw), "[]")

    private fun fingerprintCreatorRoot(
        creatorRoot: JSONObject,
        originalFrames: String,
    ): String {
        val creatorSections = listOf(
            "format", "schemaVersion", "tasks", "ideas", "weeklySchedule", "settings",
            "smartEscalationConfig", "postPublish", "rewards", "youtubeProjectLinks",
            "youtubeMilestones", "youtubePublishCheckpoints", "projectPulseHistory",
            "workflowStageTimeline", "automationPreferences", "creatorFocusSessions",
            "manifest",
        )
        require(creatorSections.all(creatorRoot::has)) {
            "Creator archive omits required currently supported sections"
        }
        // createdAtMillis and payloadSha256 are random/current export-envelope
        // metadata and deliberately NOT treated as owner-data changes.
        val creatorValues = creatorSections.map { key ->
            key to creatorRoot.get(key).toString()
        } + ("originalBestFrames" to originalFrames)
        return hashParts(creatorValues.map { (key, value) -> "$key=$value" })
    }

    /**
     * Original Personal records and photo-row fingerprint independent of
     * CURRENT Creator Best Frames. Necessary while a crashed native Creator
     * import may have replaced or corrupted its live slideshow; an otherwise
     * valid Personal original must remain inspectable before native rewind.
     */
    internal fun personalFingerprintForInterrupted(
        personal: V301LocalRecoverySnapshot,
    ): String = fingerprintPersonal(personal, requireIntegrity = false)

    private fun hashReadOnlySnapshots(
        creatorRoot: JSONObject,
        local: V301LocalRecoverySnapshot,
        requireIntegrity: Boolean,
    ): V343UnifiedBaselineFingerprint = V343UnifiedBaselineFingerprint(
        creatorSha256 = fingerprintCreatorRoot(
            creatorRoot, CreatorHeroArchive.captureOriginalOrder(app),
        ),
        personalSha256 = fingerprintPersonal(local, requireIntegrity),
    )

    private fun fingerprintPersonal(
        local: V301LocalRecoverySnapshot,
        requireIntegrity: Boolean,
    ): String {
        check((!requireIntegrity || local.localIntegrityVerified) &&
            local.components.singleActivityChallenges != null) {
            "A complete Personal v2 snapshot is required"
        }
        val d = local.components
        val personalValues = listOf(
            "personal" to d.personal, "challenges" to d.challenges,
            "groups" to d.groups, "spaces" to d.spaces,
            "singleActivityChallenges" to requireNotNull(d.singleActivityChallenges),
            "verifiedPhotoRows" to local.photos.sortedBy { it.id }
                .joinToString("\n") { it.toString() },
        )
        return hashParts(personalValues.map { (key, value) -> "$key=$value" })
    }

    private fun hashParts(values: List<String>): String {
        val md = MessageDigest.getInstance("SHA-256")
        values.forEach { value ->
            val data = value.toByteArray(Charsets.UTF_8)
            md.update(java.nio.ByteBuffer.allocate(8).putLong(data.size.toLong()).array())
            md.update(data)
        }
        return md.digest().joinToString("") { "%02x".format(it.toInt() and 0xff) }
    }
}

internal object V343UnifiedBaselinePolicy {
    fun noDrift(
        original: V343UnifiedBaselineFingerprint,
        after: V343UnifiedBaselineFingerprint,
    ): Boolean = original.valid && original == after
}
