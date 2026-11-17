package com.framebynavin.app.cloud.ux09rpc

import com.framebynavin.app.cloud.ux09.Ux09Contract
import com.framebynavin.app.cloud.ux09.Ux09Domain
import com.framebynavin.app.cloud.ux09.Ux09Intent
import org.json.JSONArray
import org.json.JSONObject

/** Exact verified UX09-01E server API response contract, for Backlot LAB only. */
internal object Ux09LabRpcContract {
    private const val LAB_ENDPOINT = "https://vasleafrmsovdbdlrcsw.supabase.co"
    private val APPLY_KEYS = setOf("status", "sequence", "revision", "domain", "entity_id", "deleted")
    private val CONFLICT_KEYS = setOf("status", "current_revision", "domain", "entity_id", "deleted")
    private val PULL_KEYS = setOf("changes", "next_cursor", "has_more")
    private val CHANGE_KEYS = setOf("sequence", "domain", "entity_id", "revision", "payload", "deleted")

    internal fun requireLabEndpoint(url: String) {
        require(url.trimEnd('/') == LAB_ENDPOINT) {
            "UX09 Lab-only RPC must not run against original production Supabase"
        }
    }

    internal sealed interface Apply {
        data class Applied(
            val operationId: String,
            val domain: Ux09Domain,
            val entityId: String,
            val revision: Long,
            val sequence: Long,
            val tombstone: Boolean,
        ) : Apply
        data class Conflict(
            val operationId: String,
            val domain: Ux09Domain,
            val entityId: String,
            val currentRevision: Long,
            val remoteDeleted: Boolean,
        ) : Apply
    }

    internal data class Change(
        val sequence: Long,
        val domain: Ux09Domain,
        val entityId: String,
        val revision: Long,
        val canonicalPayload: String,
        val deleted: Boolean,
    )
    internal data class Page(
        val changes: List<Change>,
        val nextCursor: Long,
        val hasMore: Boolean,
    )

    internal fun parseApply(raw: String, sent: Ux09Intent): Apply {
        Ux09Contract.validate(sent)
        val o = JSONObject(raw)
        return when (o.getString("status")) {
            "applied" -> {
                exactKeys(o, APPLY_KEYS)
                val domain = domainOf(o.getString("domain"))
                val entityId = Ux09Contract.requireUuid(o.getString("entity_id"))
                require(domain == sent.domain && entityId == sent.entityId) {
                    "Applied response entity mismatch; keep original outbox entry"
                }
                val revision = o.getLong("revision")
                val sequence = o.getLong("sequence")
                val deleted = o.getBoolean("deleted")
                require(revision == sent.expectedRevision + 1L &&
                    sequence > 0L && deleted == sent.tombstone) {
                    "Invalid applied sequence, revision or tombstone: preserve original operation"
                }
                Apply.Applied(sent.operationId, domain, entityId, revision, sequence, deleted)
            }
            "conflict" -> {
                exactKeys(o, CONFLICT_KEYS)
                val domain = domainOf(o.getString("domain"))
                val entityId = Ux09Contract.requireUuid(o.getString("entity_id"))
                require(domain == sent.domain && entityId == sent.entityId) {
                    "Conflicted response entity mismatch"
                }
                val current = o.getLong("current_revision")
                require(current >= 0L) { "Conflict revision must be nonnegative" }
                Apply.Conflict(sent.operationId, domain, entityId, current,
                    o.getBoolean("deleted"))
            }
            else -> throw IllegalArgumentException("Unknown signed Lab apply status")
        }
    }

    internal fun parsePull(raw: String, afterSequence: Long, limit: Int): Page {
        require(afterSequence >= 0L && limit in 1..100) {
            "Invalid requested Lab pull cursor or page size"
        }
        val o = JSONObject(raw)
        exactKeys(o, PULL_KEYS)
        val items = o.getJSONArray("changes")
        require(items.length() <= limit) {
            "Server returned more entries than the requested bounded page"
        }
        var previousSequence = afterSequence
        val results = (0 until items.length()).map { index ->
            val event = items.getJSONObject(index)
            exactKeys(event, CHANGE_KEYS)
            val sequence = event.getLong("sequence")
            require(sequence > previousSequence) {
                "Nonmonotonic or repeated delta sequence: do not advance cursor"
            }
            val revision = event.getLong("revision")
            require(revision > 0L) {
                "Invalid server entity revision"
            }
            val domain = domainOf(event.getString("domain"))
            val entityId = Ux09Contract.requireUuid(event.getString("entity_id"))
            val deleted = event.getBoolean("deleted")
            val payload = Ux09Contract.canonicalPayload(event.getJSONObject("payload").toString())
            if (deleted) require(payload == "{}") {
                "Remote permanent tombstone must not contain user data"
            }
            previousSequence = sequence
            Change(sequence, domain, entityId, revision, payload, deleted)
        }
        val cursor = o.getLong("next_cursor")
        require(cursor == previousSequence) {
            "Remote cursor differs from last verified delta: preserve old cursor"
        }
        val more = o.getBoolean("has_more")
        require(!more || results.isNotEmpty()) {
            "An empty page claiming more data would stall synchronization"
        }
        return Page(results, cursor, more)
    }

    internal fun pullBody(afterSequence: Long, limit: Int): String {
        require(afterSequence >= 0L && limit in 1..100)
        return JSONObject()
            .put("p_after_sequence", afterSequence)
            .put("p_limit", limit)
            .toString()
    }

    private fun domainOf(wire: String): Ux09Domain =
        Ux09Domain.entries.singleOrNull { it.wire == wire }
            ?: throw IllegalArgumentException("Unknown UX09 sync domain")

    private fun exactKeys(value: JSONObject, required: Set<String>) {
        require(value.keys().asSequence().toSet() == required) {
            "Unexpected or omitted signed Lab sync fields"
        }
    }
}
