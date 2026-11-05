package com.framebynavin.app.personal

import java.security.MessageDigest
import java.time.LocalDate
import java.util.UUID
import org.json.JSONArray
import org.json.JSONObject

/** Private, owner-authored planning data. Never an alarm or reminder receipt. */
internal data class V297ChallengeDay(
    val activity: String = "",
    val completedAtMillis: Long? = null,
) {
    fun checked(): V297ChallengeDay {
        require(activity.length <= 500) { "Daily activity too long" }
        require(completedAtMillis == null || (activity.isNotBlank() && completedAtMillis > 0L)) {
            "A completed day requires a saved activity"
        }
        return this
    }
}

internal data class V297Challenge(
    val id: String = UUID.randomUUID().toString(),
    val title: String,
    val startDate: LocalDate,
    val days: List<V297ChallengeDay> = List(21) { V297ChallengeDay() },
    val createdAtMillis: Long = System.currentTimeMillis(),
    val archivedAtMillis: Long? = null,
) {
    fun checked(): V297Challenge {
        require(id.isNotBlank() && id.length <= 100) { "Invalid plan ID" }
        require(title.isNotBlank() && title.length <= 100) { "Name your challenge (max 100 characters)" }
        require(days.size == 21) { "A 21-day plan must contain exactly 21 days" }
        require(createdAtMillis > 0) { "Invalid creation time" }
        require(archivedAtMillis == null || archivedAtMillis > 0) { "Invalid archive time" }
        days.forEach { it.checked() }
        return this
    }

    fun activityDate(dayIndex: Int): LocalDate {
        require(dayIndex in 0..20)
        return startDate.plusDays(dayIndex.toLong())
    }

    fun editDay(dayIndex: Int, text: String): V297Challenge {
        require(archivedAtMillis == null) { "Restore this challenge to edit it" }
        require(dayIndex in 0..20)
        val trimmed = text.trim()
        require(trimmed.length <= 500) { "Activity too long" }
        // Editing an already completed activity resets Done: don't claim the
        // owner completed text they have just substantially changed.
        return copy(days = days.mapIndexed { index, previous ->
            if (index == dayIndex && previous.activity != trimmed)
                V297ChallengeDay(trimmed)
            else previous
        }).checked()
    }

    fun markDone(dayIndex: Int, nowMillis: Long): V297Challenge {
        require(archivedAtMillis == null) { "Restore this challenge to log progress" }
        require(dayIndex in 0..20)
        require(nowMillis > 0L)
        require(days[dayIndex].activity.isNotBlank()) { "Add and save an activity first" }
        return copy(days = days.mapIndexed { index, previous ->
            if (index == dayIndex) previous.copy(completedAtMillis = nowMillis) else previous
        }).checked()
    }

    fun undoDone(dayIndex: Int): V297Challenge {
        require(archivedAtMillis == null)
        require(dayIndex in 0..20)
        return copy(days = days.mapIndexed { index, previous ->
            if (index == dayIndex) previous.copy(completedAtMillis = null) else previous
        }).checked()
    }

    fun archive(nowMillis: Long): V297Challenge {
        require(nowMillis > 0)
        return copy(archivedAtMillis = nowMillis).checked()
    }
    fun restore(): V297Challenge = copy(archivedAtMillis = null).checked()
}

/** Import is additive by identity. Never replace or delete owner's current plans. */
internal object V297ChallengeMergePolicy {
    fun addMissing(
        existing: List<V297Challenge>,
        incoming: List<V297Challenge>,
    ): List<V297Challenge> {
        existing.forEach { it.checked() }
        incoming.forEach { it.checked() }
        require(existing.map { it.id }.distinct().size == existing.size)
        require(incoming.map { it.id }.distinct().size == incoming.size)
        val currentIds = existing.mapTo(HashSet()) { it.id }
        return existing + incoming.filter { it.id !in currentIds }
    }
}

internal object V297ChallengeCodec {
    private const val FORMAT = "backlot.personal.challenges21"
    private const val VERSION = 1
    private const val MAX_PLANS = 40
    private const val MAX_CHARS = 1_000_000

    fun encode(challenges: List<V297Challenge>): String {
        require(challenges.size <= MAX_PLANS) { "Too many challenges" }
        require(challenges.map { it.id }.distinct().size == challenges.size) {
            "Duplicate challenge ID"
        }
        val items = JSONArray()
        challenges.forEach { plan ->
            val valid = plan.checked()
            items.put(JSONObject()
                .put("id", valid.id)
                .put("title", valid.title)
                .put("startDate", valid.startDate.toString())
                .put("createdAtMillis", valid.createdAtMillis)
                .put("archivedAtMillis", valid.archivedAtMillis ?: JSONObject.NULL)
                .put("days", JSONArray().apply {
                    valid.days.forEach { day ->
                        put(JSONObject().put("activity", day.activity)
                            .put("completedAtMillis", day.completedAtMillis ?: JSONObject.NULL))
                    }
                }))
        }
        return JSONObject()
            .put("format", FORMAT)
            .put("version", VERSION)
            .put("items", items)
            .put("itemsSha256", sha256(items.toString()))
            .toString().also { require(it.length <= MAX_CHARS) { "Challenge data too large" } }
    }

    fun decode(raw: String): List<V297Challenge> {
        require(raw.length <= MAX_CHARS) { "Challenge data too large" }
        val root = JSONObject(raw)
        require(root.getString("format") == FORMAT && root.getInt("version") == VERSION) {
            "Unsupported challenge data"
        }
        val items = root.getJSONArray("items")
        require(items.length() <= MAX_PLANS) { "Too many challenges" }
        require(root.getString("itemsSha256") == sha256(items.toString())) {
            "Challenge data checksum mismatch"
        }
        val ids = HashSet<String>()
        return buildList {
            for (index in 0 until items.length()) {
                val item = items.getJSONObject(index)
                val rawDays = item.getJSONArray("days")
                require(rawDays.length() == 21) { "Invalid 21-day plan" }
                val days = (0 until rawDays.length()).map { position ->
                    val day = rawDays.getJSONObject(position)
                    V297ChallengeDay(
                        activity = day.getString("activity"),
                        completedAtMillis = if (day.isNull("completedAtMillis")) null
                            else day.getLong("completedAtMillis"),
                    ).checked()
                }
                val plan = V297Challenge(
                    id = item.getString("id"),
                    title = item.getString("title"),
                    startDate = LocalDate.parse(item.getString("startDate")),
                    days = days,
                    createdAtMillis = item.getLong("createdAtMillis"),
                    archivedAtMillis = if (item.isNull("archivedAtMillis")) null
                        else item.getLong("archivedAtMillis"),
                ).checked()
                require(ids.add(plan.id)) { "Duplicate challenge ID" }
                add(plan)
            }
        }
    }

    private fun sha256(raw: String): String =
        MessageDigest.getInstance("SHA-256").digest(raw.toByteArray(Charsets.UTF_8))
            .joinToString("") { "%02x".format(it) }
}
