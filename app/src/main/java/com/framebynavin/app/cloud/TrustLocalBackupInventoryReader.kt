package com.framebynavin.app.cloud

import android.content.Context
import com.framebynavin.app.data.CreatorDataGate
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

    suspend fun inspectReadOnly(): TrustBackupInventory {
        // CreatorDataGate prevents concurrent ordinary Creator mutations, but
        // its lock is NOT the Personal/Space recovery lock.
        val creator = CreatorDataGate.transaction {
            val tasks = TaskStore(app)
            val ideas = IdeaVaultStore(app)
            val focus = CreatorFocusSessionStore(app)
            val taskJson = tasks.exportJson()
            val ideaJson = ideas.exportJson()
            val focusJson = focus.exportPortableJson()
            val focusLedger = CreatorFocusSessionCodec.decode(focusJson)
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
            )
        }

        // Stable Personal lock and photo integrity preflight; any corrupt store,
        // interrupted restore journal or untracked JPEG throws rather than
        // silently returning zero. No Room database is opened writable.
        val local = V301LocalRecoveryReader(app).readV2()
        check(local.localIntegrityVerified) {
            "Cannot inventory Personal records during pending recovery"
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
        )
        // Deliberately leaves original JPEG/photo/media bytes, alarm/app
        // preferences and missing Creator source families unproved.
        return TrustBackupInventoryPolicy.assemble(evidence)
    }
}
