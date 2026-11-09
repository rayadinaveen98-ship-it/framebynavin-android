package com.framebynavin.app.cloud

import android.content.Context
import com.framebynavin.app.data.CreatorFocusSessionCodec
import com.framebynavin.app.data.CreatorPostPublishStore
import com.framebynavin.app.data.CreatorWorkflowTimelineStore
import com.framebynavin.app.data.IdeaVaultStore
import com.framebynavin.app.data.ProjectPulseHistoryStore
import com.framebynavin.app.data.TaskStore
import com.framebynavin.app.personal.PersonalCompletionBackupCodec
import com.framebynavin.app.personal.Phase1HabitCodec
import com.framebynavin.app.personal.V297ChallengeCodec
import com.framebynavin.app.personal.V298ReminderGroupCodec
import com.framebynavin.app.personal.V303SpaceReceiptArchiveCodec
import com.framebynavin.app.personal.spaces.V290PhotoTimelinePolicy
import com.framebynavin.app.personal.spaces.V290SpacePhotoEntity
import org.json.JSONArray
import org.json.JSONObject

/**
 * Authenticate the *relationships*, not merely the encrypted ZIP checksums:
 * every Space photo row, private JPEG, Voice Idea original and Best Frames slot
 * must map to exactly one encrypted source part. This validation is READ ONLY.
 * It does not install files, mutate DataStores, resume interrupted restores,
 * rewrite old original paths, or schedule a single alarm.
 */
internal object TrustRestorableSourceReview {
    private val required = setOf(
        "creator/projects", "creator/ideas", "creator/focus",
        "creator/config/persisted", "creator/config/weekly-slots",
        "creator/config/weekly-presence",
        "creator/history/workflow", "creator/history/pulse",
        "creator/history/post-publish",
        "creator/best-frame-order", "creator/voice-index",
        "personal/reminders-history", "personal/legacy-challenges",
        "personal/reminder-groups", "personal/spaces-journal-links",
        "personal/single-activity-receipts", "personal/photo-metadata",
    )

    internal data class Result(
        val counts: Map<String, Int>,
        val missingRequiredSources: Set<String>,
        val uncoveredPreferenceKeys: List<String>,
        val referencedVoiceOriginals: Int,
        val referencedBestFrames: Int,
        val referencedSpacePhotos: Int,
    ) {
        val sourcePartsValidated: Int get() = counts.size
        // A verified source manifest is NOT a validated full-app restore plan.
        val mayInstall: Boolean get() = false
    }

    fun inspect(
        context: Context,
        staged: TrustVerifiedStagedBundle,
    ): Result {
        val app = context.applicationContext
        val entries = staged.parts.associateBy { it.id }
        check(entries.size == staged.parts.size) { "Duplicate encrypted source identifier" }
        val counts = staged.parts.associate { it.id to it.records }
        val missing = required - entries.keys
        val allowedIndexed = Regex(
            "(personal/photo|creator/voice|creator/best-frame)-[0-9]{4}",
        )
        require(entries.keys.all { it in required || allowedIndexed.matches(it) }) {
            "Unsupported or unexpected owner backup source"
        }
        if (missing.isNotEmpty()) {
            return Result(counts, missing, emptyList(), 0, 0, 0)
        }
        fun json(id: String): String {
            val part = requireNotNull(entries[id])
            require(part.sizeBytes <= 24L * 1024 * 1024) {
                "Portable JSON section exceeds bounded readback limit"
            }
            return part.file.readText(Charsets.UTF_8)
        }

        val tasks = TaskStore(app)
        check(tasks.validateJson(json("creator/projects")) ==
            counts["creator/projects"]) { "Creator projects manifest count differs" }
        val ideas = IdeaVaultStore(app)
        check(ideas.validateJson(json("creator/ideas")) ==
            counts["creator/ideas"]) { "Creator ideas manifest count differs" }
        val focus = CreatorFocusSessionCodec.decode(json("creator/focus"))
        check(focus.history.size + (if (focus.active == null) 0 else 1) ==
            counts["creator/focus"]) { "Creator focus history is incomplete" }
        val timeline = CreatorWorkflowTimelineStore(app)
        check(timeline.validateJson(json("creator/history/workflow")) ==
            counts["creator/history/workflow"]) { "Workflow events don't match" }
        val pulses = ProjectPulseHistoryStore(app)
        check(pulses.validateJson(json("creator/history/pulse")) ==
            counts["creator/history/pulse"]) { "Creator pulse history doesn't match" }
        val publishing = CreatorPostPublishStore(app)
        check(publishing.validateJson(json("creator/history/post-publish")) ==
            counts["creator/history/post-publish"]) { "Publishing history doesn't match" }

        val config = TrustPortablePreferenceCapture.validateRaw(
            json("creator/config/persisted"),
        )
        check(config.includedKeys == counts["creator/config/persisted"]) {
            "Saved configuration has a different verified record count"
        }
        val weeklyPresence = JSONObject(json("creator/config/weekly-presence"))
        check(weeklyPresence.length() == 1 &&
            weeklyPresence.get("hasSavedSlots") is Boolean) {
            "Saved weekly-plan presence was not preserved"
        }
        val weeklySlots = JSONArray(json("creator/config/weekly-slots"))
        if (!weeklyPresence.getBoolean("hasSavedSlots")) {
            require(weeklySlots.length() == 0 &&
                counts["creator/config/weekly-slots"] == 0) {
                "Absent weekly plan cannot be fabricated as saved schedule"
            }
        } else {
            require(weeklySlots.length() == counts["creator/config/weekly-slots"]) {
                "Saved weekly slots do not match the frozen owner schedule"
            }
        }

        val personal = PersonalCompletionBackupCodec.decode(
            json("personal/reminders-history"),
        )
        check(personal.reminders.size + personal.completions.size ==
            counts["personal/reminders-history"]) {
            "Personal reminder records or Done receipts are incomplete"
        }
        val legacy = V297ChallengeCodec.decode(json("personal/legacy-challenges"))
        require(legacy.size == counts["personal/legacy-challenges"])
        val groups = V298ReminderGroupCodec.decode(json("personal/reminder-groups"))
        require(groups.size == counts["personal/reminder-groups"])
        val spaces = V303SpaceReceiptArchiveCodec.decode(
            json("personal/spaces-journal-links"), personal,
        )
        check(spaces.spaces.size + spaces.entries.size + spaces.links.size ==
            counts["personal/spaces-journal-links"]) {
            "Space notes, journal entries or reminder links are incomplete"
        }
        val challenges = Phase1HabitCodec.decode(
            json("personal/single-activity-receipts"),
        )
        check(challenges.size + challenges.sumOf { it.confirmations.size } ==
            counts["personal/single-activity-receipts"]) {
            "Exact 21-day challenge Done receipts are incomplete"
        }

        val images = JSONArray(json("personal/photo-metadata"))
        check(images.length() == counts["personal/photo-metadata"]) {
            "Personal photo metadata count mismatch"
        }
        val photos = ArrayList<V290SpacePhotoEntity>(images.length())
        repeat(images.length()) { index ->
            val row = images.getJSONObject(index)
            require(row.keys().asSequence().toSet() == setOf(
                "id", "spaceId", "fileName", "sha256Hex", "takenAtMillis",
                "addedAtMillis", "widthPx", "heightPx", "note",
            )) { "Unsupported photo metadata record" }
            photos += V290PhotoTimelinePolicy.validate(
                V290SpacePhotoEntity(
                    id = row.getString("id"),
                    spaceId = row.getString("spaceId"),
                    takenAtMillis = row.getLong("takenAtMillis"),
                    addedAtMillis = row.getLong("addedAtMillis"),
                    fileName = row.getString("fileName"),
                    sha256Hex = row.getString("sha256Hex"),
                    widthPx = row.getInt("widthPx"),
                    heightPx = row.getInt("heightPx"),
                    note = row.getString("note"),
                ),
            )
        }
        require(photos.map { it.id }.distinct().size == photos.size)
        require(photos.map { it.spaceId }.all {
            spaceId -> spaces.spaces.any { it.id == spaceId }
        }) { "Private photo has no saved Space" }
        photos.sortedBy { it.id }.forEachIndexed { index, photo ->
            val name = "personal/photo-" + index.toString().padStart(4, '0')
            val photoSource = requireNotNull(entries[name]) {
                "Private Space photo bytes omitted from encrypted archive"
            }
            require(photoSource.records == 1 &&
                photoSource.sha256 == photo.sha256Hex) {
                "Space photo file does not match its restored Room metadata"
            }
        }
        require(entries.keys.count { it.startsWith("personal/photo-") &&
            it != "personal/photo-metadata" } == photos.size) {
            "Unreferenced private photo ciphertext part"
        }

        val frameOrder = JSONArray(json("creator/best-frame-order"))
        check(frameOrder.length() == counts["creator/best-frame-order"])
        require(frameOrder.length() <= 50)
        repeat(frameOrder.length()) { index ->
            val fileName = frameOrder.getString(index)
            require(fileName.isNotBlank() && fileName.length <= 255 &&
                '/' !in fileName && '\\' !in fileName && ".." !in fileName) {
                "Unsafe original Best Frames file label"
            }
            val frame = entries[
                "creator/best-frame-" + index.toString().padStart(4, '0')
            ] ?: error("Original Best Frames JPEG missing from owner archive")
            require(frame.records == 1 && frame.sizeBytes > 0)
        }
        require(entries.keys.count { it.startsWith("creator/best-frame-") &&
            it != "creator/best-frame-order" } == frameOrder.length()) {
            "Unreferenced Best Frames original"
        }

        val voiceIndex = JSONArray(json("creator/voice-index"))
        check(voiceIndex.length() == counts["creator/voice-index"])
        val voiceNames = HashSet<String>()
        val voiceIdeas = HashSet<String>()
        repeat(voiceIndex.length()) { index ->
            val row = voiceIndex.getJSONObject(index)
            require(row.keys().asSequence().toSet() == setOf("ideaId", "partId"))
            val ideaId = row.getString("ideaId")
            val expectedPart = "creator/voice-" + index.toString().padStart(4, '0')
            require(ideaId.isNotBlank() && voiceIdeas.add(ideaId) &&
                row.getString("partId") == expectedPart &&
                voiceNames.add(expectedPart) &&
                entries[expectedPart]?.records == 1) {
                "Voice Idea original cannot be mapped to its saved idea"
            }
        }
        require(entries.keys.count { it.startsWith("creator/voice-") &&
            it != "creator/voice-index" } == voiceIndex.length()) {
            "Unexpected unreferenced Voice Idea original"
        }

        return Result(
            counts = counts,
            missingRequiredSources = emptySet(),
            uncoveredPreferenceKeys = config.uncoveredKeys,
            referencedVoiceOriginals = voiceIndex.length(),
            referencedBestFrames = frameOrder.length(),
            referencedSpacePhotos = photos.size,
        )
    }
}
