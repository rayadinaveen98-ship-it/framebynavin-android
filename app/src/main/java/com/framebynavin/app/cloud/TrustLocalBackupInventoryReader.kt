package com.framebynavin.app.cloud

import android.content.Context
import com.framebynavin.app.data.CreatorDataGate
import com.framebynavin.app.data.CreatorPostPublishStore
import com.framebynavin.app.data.CreatorWorkflowTimelineStore
import com.framebynavin.app.data.ProjectPulseHistoryStore
import com.framebynavin.app.data.WeeklyScheduleStore
import com.framebynavin.app.data.CreatorHeroArchive
import com.framebynavin.app.data.PortableBestFrameMedia
import com.framebynavin.app.personal.StabilizationPersonalMutationGate
import java.io.File
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import com.framebynavin.app.data.CreatorFocusSessionCodec
import com.framebynavin.app.data.CreatorFocusSessionStore
import com.framebynavin.app.data.IdeaVaultStore
import com.framebynavin.app.data.TaskStore
import com.framebynavin.app.personal.PersonalCompletionBackupCodec
import com.framebynavin.app.personal.Phase1HabitCodec
import com.framebynavin.app.personal.V297ChallengeCodec
import com.framebynavin.app.personal.V298ReminderGroupCodec
import com.framebynavin.app.personal.V301LocalRecoveryReader
import com.framebynavin.app.personal.V303SpaceReceiptArchiveCodec
import org.json.JSONArray
import org.json.JSONObject

/**
 * T1B1: safe, incomplete inventory of persisted local owner records.
 *
 * Only validated counts, byte sizes and SHA-256 evidence escape this call.
 * NO cloud connection, upload, settings seeding, alarm changes or restore.
 *
 * The two store authorities remain different: this preview must NEVER be
 * treated as one atomic Creator + Personal revision or a complete Back Up.
 */
internal class TrustLocalBackupInventoryReader(context: Context) {
    private val app = context.applicationContext

    suspend fun inspectReadOnly(): TrustBackupInventory = withContext(Dispatchers.IO) {
        // CreatorDataGate prevents concurrent ordinary Creator mutations, but
        // its lock is NOT the Personal/Space recovery lock.
        val creator = CreatorDataGate.transaction {
            val tasks = TaskStore(app)
            val ideas = IdeaVaultStore(app)
            val focus = CreatorFocusSessionStore(app)
            val taskJson = tasks.exportJson()
            val ideaJson = ideas.exportJson()
            val voiceOriginals = TrustVoiceIdeaInventory.inspect(
                app.filesDir, ideas.load(),
            )
            val savedSparkPrefs = TrustSparkPreferenceInventory.inspect(app)
            val savedEscalation = TrustCreatorEscalationInventory.inspect(app)
            val focusJson = focus.exportPortableJson()
            val focusLedger = CreatorFocusSessionCodec.decode(focusJson)
            val weeklyStore = WeeklyScheduleStore(app)
            val savedSettings = TrustSavedSettingsInventory.inspect(app)
            val routinePreferences = TrustRoutinePreferencesInventory.inspect(app)
            val appearancePreferences = TrustAppearancePreferencesInventory.inspect(app)
            val slideshowPlayback = TrustSavedSettingsInventory.homePlayback(app)
            // Null is an unseeded plan, not a verified empty backup.
            // This bypasses loadOrSeed so inventory never edits the schedule.
            val persistedWeekly = weeklyStore.inspectSavedForInventory()
            val transitions = CreatorWorkflowTimelineStore(app)
            val transitionJson = transitions.exportJson()
            val pulses = ProjectPulseHistoryStore(app)
            val pulseJson = pulses.exportJson()
            val publishing = CreatorPostPublishStore(app)
            val publishingJson = publishing.exportJson()
            // Active Best Frames ARE the saved Home slideshow collection.
            // Voice recordings and rollback-only frame files remain separate
            // pending categories; do not pretend they are included.
            val activeFrameNames = CreatorHeroArchive.orderedSourceNames(app)
            val frames = PortableBestFrameMedia.prepareFromFiles(
                app.filesDir, activeFrameNames,
            )
            val bestFramesProof = TrustMediaByteInventory.verify(
                domain = TrustBackupDomain.CREATOR_HERO_ORIGINAL_BYTES,
                root = File(app.filesDir, "best_frames_original"),
                sources = frames.frames.map { media ->
                    TrustMediaSource(media.name, media.sha256)
                },
                maxEachBytes = PortableBestFrameMedia.MAX_FILE_BYTES,
                maxTotalBytes = PortableBestFrameMedia.MAX_TOTAL_BYTES,
                // The existing archive retains older frame files for rollback.
                // This proof covers ACTIVE images only. Broad Creator media
                // remains pending until orphan/recording policy is reviewed.
                requireExactFileInventory = false,
            )
            listOf(
                TrustBackupInventoryPolicy.evidence(
                    TrustBackupDomain.CREATOR_PROJECTS,
                    taskJson, tasks.validateJson(taskJson),
                ),
                TrustBackupInventoryPolicy.evidence(
                    TrustBackupDomain.CREATOR_IDEAS,
                    ideaJson, ideas.validateJson(ideaJson),
                ),
                TrustBackupInventoryPolicy.evidence(
                    TrustBackupDomain.CREATOR_FOCUS,
                    focusJson, focusLedger.history.size +
                        if (focusLedger.active == null) 0 else 1,
                ),
                bestFramesProof,
                // Home slideshow uses the very same active CreatorHeroArchive
                // originals; reuse the VERIFIED bytes rather than a second
                // read or an invented independent collection.
                bestFramesProof.copy(domain = TrustBackupDomain.HOME_SLIDESHOW_MEDIA),
                TrustBackupInventoryPolicy.evidence(
                    TrustBackupDomain.CREATOR_WORKFLOW_TIMELINE,
                    transitionJson, transitions.validateJson(transitionJson),
                ),
                TrustBackupInventoryPolicy.evidence(
                    TrustBackupDomain.CREATOR_PULSE_HISTORY,
                    pulseJson, pulses.validateJson(pulseJson),
                ),
                TrustBackupInventoryPolicy.evidence(
                    TrustBackupDomain.CREATOR_POST_PUBLISH,
                    publishingJson, publishing.validateJson(publishingJson),
                ),
            ) + listOfNotNull(
                persistedWeekly?.let { (raw, count) ->
                    TrustBackupInventoryPolicy.evidence(
                        TrustBackupDomain.CREATOR_WEEKLY_SLOTS, raw, count,
                    )
                },
                savedSettings,
                routinePreferences,
                appearancePreferences,
                slideshowPlayback,
                voiceOriginals,
                savedSparkPrefs,
                savedEscalation,
            )
        }

        // Stable Personal lock and photo integrity preflight; any corrupt store,
        // interrupted restore journal or untracked JPEG throws rather than
        // silently returning zero. No Room database is opened writable.
        val (local, originalPhotoProof) = StabilizationPersonalMutationGate
            .withStableSnapshot(app) {
                val snapshot = V301LocalRecoveryReader(app).readV2()
                check(snapshot.localIntegrityVerified) {
                    "Cannot inventory Personal records during pending recovery"
                }
                // Keep original JPEG verification within the same owner lock
                // used by Space/Personal writers. A photo row by itself is
                // never accepted as proof of an existing JPEG.
                val files = snapshot.photos.map { photo ->
                    TrustMediaSource(
                        relativePath = "${photo.spaceId}/${photo.fileName}",
                        expectedSha256 = photo.sha256Hex,
                    )
                }
                val proof = TrustMediaByteInventory.verify(
                    domain = TrustBackupDomain.PERSONAL_PHOTO_BYTES,
                    root = File(app.filesDir, "backlot-personal-space-photos"),
                    sources = files,
                    maxEachBytes = 12L * 1024 * 1024,
                    maxTotalBytes = 500L * 1024 * 1024,
                    requireExactFileInventory = true,
                )
                snapshot to proof
            }
        val payload = local.components
        val reminders = PersonalCompletionBackupCodec.decode(payload.personal)
        val habits = Phase1HabitCodec.decode(requireNotNull(payload.singleActivityChallenges))
        val legacy = V297ChallengeCodec.decode(payload.challenges)
        val groups = V298ReminderGroupCodec.decode(payload.groups)
        val spaces = V303SpaceReceiptArchiveCodec.decode(payload.spaces, reminders)

        // Photo rows and exact user note/byte fingerprints require an
        // independent manifest. Merely hashing Space JSON could OMIT real
        // Room photo metadata and incorrectly claim coverage.
        val photoMetadata = JSONArray().apply {
            local.photos.sortedBy { it.id }.forEach { p ->
                put(JSONObject()
                    .put("id", p.id)
                    .put("spaceId", p.spaceId)
                    .put("fileName", p.fileName)
                    .put("sha256Hex", p.sha256Hex)
                    .put("takenAtMillis", p.takenAtMillis)
                    .put("addedAtMillis", p.addedAtMillis)
                    .put("widthPx", p.widthPx)
                    .put("heightPx", p.heightPx)
                    .put("note", p.note))
            }
        }.toString()
        val evidence = creator + listOf(
            TrustBackupInventoryPolicy.evidence(
                TrustBackupDomain.PERSONAL_REMINDERS,
                payload.personal, reminders.reminders.size,
            ),
            TrustBackupInventoryPolicy.evidence(
                TrustBackupDomain.PERSONAL_DONE_RECEIPTS,
                payload.personal, reminders.completions.size,
            ),
            TrustBackupInventoryPolicy.evidence(
                TrustBackupDomain.PERSONAL_SINGLE_ACTIVITY,
                requireNotNull(payload.singleActivityChallenges), habits.size,
            ),
            TrustBackupInventoryPolicy.evidence(
                TrustBackupDomain.PERSONAL_SINGLE_ACTIVITY_DONE,
                requireNotNull(payload.singleActivityChallenges),
                habits.sumOf { it.confirmations.size },
            ),
            TrustBackupInventoryPolicy.evidence(
                TrustBackupDomain.PERSONAL_LEGACY_CHALLENGES,
                payload.challenges, legacy.size,
            ),
            TrustBackupInventoryPolicy.evidence(
                TrustBackupDomain.PERSONAL_GROUPS,
                payload.groups, groups.size,
            ),
            TrustBackupInventoryPolicy.evidence(
                TrustBackupDomain.PERSONAL_SPACES,
                payload.spaces, spaces.spaces.size,
            ),
            TrustBackupInventoryPolicy.evidence(
                TrustBackupDomain.PERSONAL_JOURNALS,
                payload.spaces, spaces.entries.size,
            ),
            TrustBackupInventoryPolicy.evidence(
                TrustBackupDomain.PERSONAL_SPACE_LINKS,
                payload.spaces, spaces.links.size,
            ),
            TrustBackupInventoryPolicy.evidence(
                TrustBackupDomain.PERSONAL_PHOTO_METADATA,
                photoMetadata, local.photos.size,
            ),
            originalPhotoProof,
        )
        // Proves active Best Frames and inventoried Space originals. Voice
        // originals and saved Spark identity/tips are included ONLY when all
        // their local source checks pass; broader Creator media, notifications,
        // other Spark state and cross-store atomicity remain unproven.
        TrustBackupInventoryPolicy.assemble(evidence)
    }
}
