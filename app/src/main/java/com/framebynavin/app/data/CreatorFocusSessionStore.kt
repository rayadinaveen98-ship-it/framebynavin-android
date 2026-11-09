package com.framebynavin.app.data

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject

/**
 * Phase 3C recovery adapter; intentionally NOT invoked by the live Focus UI yet.
 *
 * Active session and history are single-writer (same app process), synchronously committed,
 * and exported as one versioned document. Never turn unreadable data into empty history.
 * The eventual UI must first add this section to the Creator backup/restore manifest before
 * it can write Focus sessions, or older Creator backups would silently omit them.
 */
internal data class CreatorFocusLedger(
    val active: CreatorFocusSession? = null,
    val history: List<CreatorFocusSession> = emptyList(),
) {
    fun checked(): CreatorFocusLedger {
        active?.checked()
        require(active == null || active.state in setOf(
            CreatorFocusState.READY, CreatorFocusState.RUNNING, CreatorFocusState.PAUSED
        )) { "Active Focus record has a terminal state" }
        require(history.size <= 100)
        require(history.all {
            it.checked().state == CreatorFocusState.FINISHED ||
                it.state == CreatorFocusState.DISCARDED
        }) { "Focus history contains unfinished or unvalidated records" }
        val ids = history.map { it.id } + listOfNotNull(active?.id)
        require(ids.distinct().size == ids.size) { "Duplicate Focus session identity" }
        return this
    }
}

internal object CreatorFocusSessionCodec {
    private const val VERSION = 1

    fun encode(ledger: CreatorFocusLedger): String {
        ledger.checked()
        return JSONObject()
            .put("version", VERSION)
            .put("active", ledger.active?.let(::encodeSession) ?: JSONObject.NULL)
            .put("history", JSONArray().apply { ledger.history.forEach { put(encodeSession(it)) } })
            .toString()
    }

    fun decode(raw: String): CreatorFocusLedger {
        val root = JSONObject(raw)
        require(root.getInt("version") == VERSION) { "Unsupported Focus archive version" }
        require(root.keys().asSequence().toSet() == setOf("version", "active", "history")) {
            "Unexpected Focus archive fields"
        }
        val active = if (root.isNull("active")) null else decodeSession(root.getJSONObject("active"))
        val records = root.getJSONArray("history")
        require(records.length() <= 100) { "Focus history is too large" }
        return CreatorFocusLedger(active, List(records.length()) {
            decodeSession(records.getJSONObject(it))
        }).checked()
    }

    private fun encodeSession(s: CreatorFocusSession): JSONObject = JSONObject().apply {
        s.checked()
        put("id", s.id)
        put("projectId", s.projectId)
        put("stageId", s.stageId)
        put("targetMillis", s.targetMillis)
        put("createdAtWallMillis", s.createdAtWallMillis)
        put("state", s.state.name)
        put("accruedActiveMillis", s.accruedActiveMillis)
        put("segmentStartedElapsedMillis", s.segmentStartedElapsedMillis)
        put("segmentBootCount", s.segmentBootCount)
        put("endedAtWallMillis", s.endedAtWallMillis)
        put("recoveredAfterReboot", s.recoveredAfterReboot)
    }

    private fun decodeSession(o: JSONObject): CreatorFocusSession {
        require(o.keys().asSequence().toSet() == setOf(
            "id", "projectId", "stageId", "targetMillis", "createdAtWallMillis",
            "state", "accruedActiveMillis", "segmentStartedElapsedMillis",
            "segmentBootCount", "endedAtWallMillis", "recoveredAfterReboot"
        )) { "Unexpected Focus session properties" }
        return CreatorFocusSession(
            id = o.getString("id"),
            projectId = o.getString("projectId"),
            stageId = o.getString("stageId"),
            targetMillis = o.getLong("targetMillis"),
            createdAtWallMillis = o.getLong("createdAtWallMillis"),
            state = CreatorFocusState.valueOf(o.getString("state")),
            accruedActiveMillis = o.getLong("accruedActiveMillis"),
            segmentStartedElapsedMillis = o.getLong("segmentStartedElapsedMillis"),
            segmentBootCount = o.getInt("segmentBootCount"),
            endedAtWallMillis = o.getLong("endedAtWallMillis"),
            recoveredAfterReboot = o.getBoolean("recoveredAfterReboot"),
        ).checked()
    }
}

/**
 * Local durable store, not a project TaskStore replacement or a notification scheduler.
 * Import remains explicitly callable only from an owner-approved, versioned restore flow.
 */
internal class CreatorFocusSessionStore(context: Context) {
    private val preferences = context.applicationContext
        .getSharedPreferences("creator_focus_sessions_v1", Context.MODE_PRIVATE)

    fun load(): CreatorFocusLedger = synchronized(lock) { loadLocked() }

    fun exportJson(): String = synchronized(lock) { CreatorFocusSessionCodec.encode(loadLocked()) }

    /**
     * Portable Creator backups must not carry a device-local monotonic clock anchor.
     * Pause exported copies at their last durable active-time checkpoint; never infer
     * any time from another phone's uptime or device wall clock.
     * This changes only the exported copy, never pauses the local live session.
     */
    fun exportPortableJson(): String = synchronized(lock) {
        val saved = loadLocked()
        val safeActive = saved.active?.let { active ->
            if (active.state == CreatorFocusState.RUNNING) {
                active.copy(
                    state = CreatorFocusState.PAUSED,
                    segmentStartedElapsedMillis = -1L,
                    segmentBootCount = -1,
                    recoveredAfterReboot = true,
                ).checked()
            } else active
        }
        CreatorFocusSessionCodec.encode(saved.copy(active = safeActive))
    }

    /** Before restoring any older backup with no Focus section, prevent silent data loss. */
    fun hasOwnerData(): Boolean = synchronized(lock) {
        val saved = loadLocked()
        saved.active != null || saved.history.isNotEmpty()
    }

    fun validateJson(raw: String): CreatorFocusLedger = CreatorFocusSessionCodec.decode(raw)

    /** Explicit owner-approved restore only. Never merges two live sessions. */
    fun importAfterApprovedRestore(raw: String): CreatorFocusLedger = synchronized(lock) {
        val checked = validateJson(raw)
        // Never trust a portable running anchor from another device/boot. Even if
        // it matches by coincidence, count only durably recorded active time.
        val safe = checked.copy(active = checked.active?.let { current ->
            if (current.state == CreatorFocusState.RUNNING) {
                current.copy(
                    state = CreatorFocusState.PAUSED,
                    segmentStartedElapsedMillis = -1L,
                    segmentBootCount = -1,
                    recoveredAfterReboot = true,
                ).checked()
            } else current
        }).checked()
        saveLocked(safe)
        safe
    }

    fun create(session: CreatorFocusSession): CreatorFocusLedger = change { ledger ->
        require(ledger.active == null) { "Finish or discard the existing Focus session first" }
        require(session.state == CreatorFocusState.READY) { "Focus must start explicitly" }
        ledger.copy(active = session.checked())
    }

    fun update(id: String, action: (CreatorFocusSession) -> CreatorFocusSession): CreatorFocusLedger =
        change { ledger ->
            val current = ledger.active ?: error("No active Focus session")
            require(current.id == id) { "Stale Focus session identity" }
            val next = action(current).checked()
            require(next.id == current.id &&
                next.projectId == current.projectId &&
                next.stageId == current.stageId &&
                next.targetMillis == current.targetMillis
            ) { "Cannot silently retarget a Focus session" }
            when (next.state) {
                CreatorFocusState.FINISHED, CreatorFocusState.DISCARDED ->
                    ledger.copy(active = null, history = (ledger.history + next).takeLast(100))
                else -> ledger.copy(active = next)
            }
        }

    private fun change(update: (CreatorFocusLedger) -> CreatorFocusLedger): CreatorFocusLedger =
        synchronized(lock) {
            val previous = loadLocked()
            val next = update(previous).checked()
            if (next != previous) saveLocked(next)
            next
        }

    private fun loadLocked(): CreatorFocusLedger {
        val primary = preferences.getString(PRIMARY, null)
        if (primary == null) {
            check(preferences.getString(PREVIOUS, null) == null) {
                "Focus recovery copy exists but primary is missing. Refusing empty replacement."
            }
            return CreatorFocusLedger()
        }
        // Invalid JSON must block writes; never replace or reset unknown owner data.
        return CreatorFocusSessionCodec.decode(primary)
    }

    private fun saveLocked(next: CreatorFocusLedger) {
        val before = preferences.getString(PRIMARY, null)
        val editor = preferences.edit()
        if (before != null) editor.putString(PREVIOUS, before)
        check(editor.putString(PRIMARY, CreatorFocusSessionCodec.encode(next)).commit()) {
            "Could not durably save Focus session. No success should be displayed."
        }
    }

    companion object {
        private const val PRIMARY = "ledger"
        private const val PREVIOUS = "last_verified_ledger"
        private val lock = Any()
    }
}
