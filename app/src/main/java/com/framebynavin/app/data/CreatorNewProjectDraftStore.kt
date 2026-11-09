package com.framebynavin.app.data

import android.content.Context
import com.framebynavin.app.reminders.ReminderMusicMode
import org.json.JSONArray
import org.json.JSONObject

/**
 * One LOCAL, recoverable NEW-project composer draft. This is never a created
 * project, a cloud backup, an active reminder or a publication record.
 *
 * A restore-generation mismatch never overwrites or replays earlier owner
 * input. The old raw draft remains untouched for explicit future recovery.
 */
internal data class CreatorNewProjectFormDraft(
    val title: String,
    val creatorModeId: String,
    val archetypeId: String,
    val productionStyles: List<String>,
    val platform: String,
    val contentType: String,
    val dueAt: Long,
    val attentionPlan: ProjectAttentionPlan,
    val deliveryPreference: ReminderDeliveryPreference,
    val customReminderAt: Long,
    val priority: TaskPriority,
    val notes: String,
    val soundUri: String,
    val voiceMusicMode: ReminderMusicMode,
    val voiceMusicUri: String,
    val voice: VoicePersona,
    val repeatCount: Int,
    val repeatGap: Int,
    val alarmTimeout: Int,
    val snoozeMinutes: Int,
    val autoRetryEnabled: Boolean,
    val autoRetryMinutes: Int,
    val autoRetryMaxAttempts: Int,
) {
}

internal class CreatorNewProjectDraftStore(context: Context) {
    private val prefs = context.applicationContext.getSharedPreferences(
        "backlot_creator_new_project_draft_p4_v1", Context.MODE_PRIVATE,
    )

    fun save(generation: Long, draft: CreatorNewProjectFormDraft) {
        require(generation >= 0 && draft.title.length <= 800 &&
            draft.notes.length <= 30_000 && draft.productionStyles.size <= 24) {
            "New project draft exceeds recovery limits"
        }
        val payload = JSONObject()
            .put("schema", 1)
            .put("generation", generation)
            .put("savedAt", System.currentTimeMillis())
            .put("title", draft.title)
            .put("creatorModeId", draft.creatorModeId)
            .put("archetypeId", draft.archetypeId)
            .put("productionStyles", JSONArray(draft.productionStyles.sorted()))
            .put("platform", draft.platform)
            .put("contentType", draft.contentType)
            .put("dueAt", draft.dueAt)
            .put("attentionPlan", draft.attentionPlan.name)
            .put("deliveryPreference", draft.deliveryPreference.name)
            .put("customReminderAt", draft.customReminderAt)
            .put("priority", draft.priority.name)
            .put("notes", draft.notes)
            .put("soundUri", draft.soundUri)
            .put("voiceMusicMode", draft.voiceMusicMode.name)
            .put("voiceMusicUri", draft.voiceMusicUri)
            .put("voice", draft.voice.name)
            .put("repeatCount", draft.repeatCount)
            .put("repeatGap", draft.repeatGap)
            .put("alarmTimeout", draft.alarmTimeout)
            .put("snoozeMinutes", draft.snoozeMinutes)
            .put("autoRetryEnabled", draft.autoRetryEnabled)
            .put("autoRetryMinutes", draft.autoRetryMinutes)
            .put("autoRetryMaxAttempts", draft.autoRetryMaxAttempts)
        check(prefs.edit().putString(keyFor(generation), payload.toString()).commit()) {
            "Could not preserve new project draft"
        }
    }

    /** Null = absent, unsupported, corrupt, or from a different restore epoch. */
    fun load(generation: Long): CreatorNewProjectFormDraft? {
        val raw = prefs.getString(keyFor(generation), null) ?: return null
        return runCatching {
            require(raw.length <= 75_000)
            val j = JSONObject(raw)
            require(j.getInt("schema") == 1 &&
                j.getLong("generation") == generation)
            val styles = j.getJSONArray("productionStyles")
            require(styles.length() <= 24)
            val draft = CreatorNewProjectFormDraft(
                title = j.getString("title"),
                creatorModeId = j.getString("creatorModeId"),
                archetypeId = j.getString("archetypeId"),
                productionStyles = (0 until styles.length()).map(styles::getString),
                platform = j.getString("platform"),
                contentType = j.getString("contentType"),
                dueAt = j.getLong("dueAt"),
                attentionPlan = ProjectAttentionPlan.valueOf(j.getString("attentionPlan")),
                deliveryPreference = ReminderDeliveryPreference.valueOf(j.getString("deliveryPreference")),
                customReminderAt = j.getLong("customReminderAt"),
                priority = TaskPriority.valueOf(j.getString("priority")),
                notes = j.getString("notes"),
                soundUri = j.getString("soundUri"),
                voiceMusicMode = ReminderMusicMode.valueOf(j.getString("voiceMusicMode")),
                voiceMusicUri = j.getString("voiceMusicUri"),
                voice = VoicePersona.valueOf(j.getString("voice")),
                repeatCount = j.getInt("repeatCount"),
                repeatGap = j.getInt("repeatGap"),
                alarmTimeout = j.getInt("alarmTimeout"),
                snoozeMinutes = j.getInt("snoozeMinutes"),
                autoRetryEnabled = j.getBoolean("autoRetryEnabled"),
                autoRetryMinutes = j.getInt("autoRetryMinutes"),
                autoRetryMaxAttempts = j.getInt("autoRetryMaxAttempts"),
            )
            require(draft.title.length <= 800 && draft.notes.length <= 30_000 &&
                draft.dueAt > 0 && draft.productionStyles.distinct().size ==
                    draft.productionStyles.size)
            draft
        }.getOrNull()
    }

    /**
     * Opaque fingerprint excludes the save timestamp: closing the composer
     * may re-commit identical fields while its queued project write finishes.
     * A DIFFERENT subsequent draft must never be cleared by that older save.
     */
    fun contentFingerprint(generation: Long): String? =
        prefs.getString(keyFor(generation), null)?.let { raw ->
            runCatching {
                val payload = JSONObject(raw)
                require(payload.getInt("schema") == 1 &&
                    payload.getLong("generation") == generation)
                payload.remove("savedAt")
                val bytes = payload.toString().toByteArray(Charsets.UTF_8)
                java.security.MessageDigest.getInstance("SHA-256").digest(bytes)
                    .joinToString("") { "%02x".format(it) }
            }.getOrNull()
        }

    fun clearIfUnchanged(generation: Long, expectedFingerprint: String): Boolean {
        if (!expectedFingerprint.matches(Regex("[0-9a-f]{64}"))) return false
        if (contentFingerprint(generation) != expectedFingerprint) return false
        clear(generation)
        return true
    }

    fun clear(generation: Long) {
        check(prefs.edit().remove(keyFor(generation)).commit()) {
            "Could not clear completed new-project draft"
        }
    }

    fun hasUnreviewedDifferentGeneration(generation: Long): Boolean =
        prefs.all.keys.any { it.startsWith("draft:") && it != keyFor(generation) }

    private fun keyFor(generation: Long): String {
        require(generation >= 0)
        return "draft:$generation"
    }
}
