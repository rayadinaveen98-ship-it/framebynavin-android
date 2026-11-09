package com.framebynavin.app.cloud

import android.content.Context
import com.framebynavin.app.data.CreatorBackupManager
import com.framebynavin.app.data.PortableBackupArchive
import com.framebynavin.app.data.PortableBackupMedia
import com.framebynavin.app.data.PortableBestFrame
import com.framebynavin.app.data.PortableBestFrameMedia
import com.framebynavin.app.data.PortableCreatorBackupManager
import java.io.File
import org.json.JSONArray

/**
 * Convert already authenticated Trust Creator sources into the ORIGINAL
 * Backlot .fbnbackup archive, including mapped Voice Idea recordings and
 * original Best Frames, without installing any new owner data.
 *
 * This is intended ONLY after deliberate owner restore authorization. The
 * existing native ZIP importer remains the write authority; no second Creator
 * restore implementation, custom path rewrites or silent empty placeholders.
 */
internal class TrustCreatorPortableCandidate(context: Context) {
    private val app = context.applicationContext
    private val required = setOf(
        "creator/projects", "creator/ideas", "creator/focus",
        "creator/config/weekly-slots", "creator/config/persisted",
        "creator/history/workflow", "creator/history/pulse",
        "creator/history/post-publish", "creator/history/rewards",
        "creator/config/automation", "creator/history/youtube-links",
        "creator/history/youtube-milestones",
        "creator/history/youtube-checkpoints", "creator/voice-index",
        "creator/best-frame-order",
    )

    fun makeEncryptedSourceToNative(
        verified: TrustVerifiedStagedBundle,
        ownerApprovedNativeConversion: Boolean,
    ): File {
        require(ownerApprovedNativeConversion) {
            "Owner must explicitly approve native Creator recovery preparation"
        }
        val review = TrustRestorableSourceReview.inspect(app, verified)
        check(review.missingRequiredSources.isEmpty() &&
            review.uncoveredPreferenceKeys.isEmpty()) {
            "Incomplete owner source: Creator restoration requires every saved preference"
        }
        val parts = verified.parts.associateBy { it.id }
        require(parts.keys.containsAll(required))
        fun json(name: String): String {
            val part = requireNotNull(parts[name])
            require(part.sizeBytes <= 24L * 1024 * 1024)
            return part.file.readText(Charsets.UTF_8)
        }
        val nativeSettings = TrustCreatorSettingsTranslator.translate(
            json("creator/config/persisted"),
        )
        val framesOrder = JSONArray(json("creator/best-frame-order"))
        val frames = (0 until framesOrder.length()).map { index ->
            val name = framesOrder.getString(index)
            val part = checkNotNull(parts[
                "creator/best-frame-" + index.toString().padStart(4, '0')
            ]) { "Original Best Frame part is missing" }
            PortableBestFrame(name, part.sha256, part.file)
        }
        val preparedFrames =
            PortableBestFrameMedia.prepareFromVerifiedSourceFiles(frames)
        val voiceOrder = JSONArray(json("creator/voice-index"))
        val recordings = (0 until voiceOrder.length()).map { index ->
            val row = voiceOrder.getJSONObject(index)
            val expectedPartId =
                "creator/voice-" + index.toString().padStart(4, '0')
            require(row.getString("partId") == expectedPartId)
            PortableBackupMedia(
                row.getString("ideaId"),
                checkNotNull(parts[expectedPartId]).file,
            )
        }

        val nativeSections = required.filterNot {
            it == "creator/config/persisted" ||
                it == "creator/best-frame-order" ||
                it == "creator/voice-index"
        }.associateWith(::json)
        val seal = CreatorBackupManager(app).assembleTrustedSourceEnvelope(
            nativeSections,
            nativeSettings.creatorSettingsJson,
            nativeSettings.smartEscalationJson,
            preparedFrames,
        )
        val output = File.createTempFile(
            "trust-verified-creator-", ".fbnbackup", app.cacheDir,
        )
        try {
            output.outputStream().buffered().use {
                PortableBackupArchive.write(
                    seal, recordings, it, preparedFrames.frames,
                )
            }
            // The *original* production Creator restore validator checks the
            // complete ZIP incl. metadata, named media and original image bytes.
            val preview = PortableCreatorBackupManager(app).validate(
                output.inputStream().buffered(),
            )
            check(preview.projectCount == review.counts["creator/projects"] &&
                preview.ideaCount == review.counts["creator/ideas"] &&
                preview.personalFrameCount == frames.size) {
                "Native Creator ZIP changed after verified Trust repacking"
            }
            return output
        } catch (failure: Throwable) {
            if (!output.delete() && output.exists()) {
                failure.addSuppressed(IllegalStateException(
                    "Failed to delete unverified native Creator restore file",
                ))
            }
            throw failure
        }
    }
}
