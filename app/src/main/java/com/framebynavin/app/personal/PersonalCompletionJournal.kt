package com.framebynavin.app.personal

import org.json.JSONArray
import org.json.JSONObject

/**
 * User-confirmed Personal DONE receipts only.
 *
 * Stored solely in PersonalReminderStore's private DataStore transaction alongside
 * the acknowledged occurrence. Never written to CreatorTask, Creator cloud, or
 * analytics. No title/notes are persisted in this log.
 */
internal data class PersonalCompletionEvent(
    val reminderId: String,
    val occurrenceMillis: Long,
    val doneAtMillis: Long,
) {
    val stableKey: String get() = "$reminderId:$occurrenceMillis"
}

internal object PersonalCompletionJournal {
    private const val MAX_RECORDS = 5_000
    private const val MAX_CHARS = 1_000_000

    fun append(
        existing: List<PersonalCompletionEvent>,
        receipt: PersonalCompletionEvent,
    ): List<PersonalCompletionEvent> {
        require(receipt.reminderId.isNotBlank())
        require(receipt.occurrenceMillis > 0L && receipt.doneAtMillis > 0L)
        if (existing.any { it.stableKey == receipt.stableKey }) return existing
        return (existing + receipt).takeLast(MAX_RECORDS)
    }

    fun encode(events: List<PersonalCompletionEvent>): String {
        require(events.size <= MAX_RECORDS)
        require(events.map { it.stableKey }.distinct().size == events.size)
        return JSONArray().apply {
            events.forEach {
                require(it.reminderId.isNotBlank() && it.occurrenceMillis > 0L && it.doneAtMillis > 0L)
                put(JSONObject()
                    .put("reminderId", it.reminderId)
                    .put("occurrenceMillis", it.occurrenceMillis)
                    .put("doneAtMillis", it.doneAtMillis))
            }
        }.toString().also { require(it.length <= MAX_CHARS) }
    }

    fun decode(raw: String): List<PersonalCompletionEvent> {
        require(raw.length <= MAX_CHARS)
        val array = JSONArray(raw)
        require(array.length() <= MAX_RECORDS)
        val result = buildList {
            for (i in 0 until array.length()) {
                val item = array.getJSONObject(i)
                add(PersonalCompletionEvent(
                    reminderId = item.getString("reminderId"),
                    occurrenceMillis = item.getLong("occurrenceMillis"),
                    doneAtMillis = item.getLong("doneAtMillis"),
                ))
            }
        }
        encode(result) // strict validation: no stale, corrupt or duplicate keys
        return result
    }

    /** Only explicit current-lease DONE may add a receipt; snooze etc. never count. */
    fun acceptedDone(
        before: PersonalReminder?,
        after: PersonalReminder?,
        action: String,
        result: PersonalApplyResult,
        occurrenceMillis: Long,
        token: String,
    ): PersonalCompletionEvent? {
        if (action != PersonalDeliveryContract.ACTION_DONE || result != PersonalApplyResult.ACCEPTED)
            return null
        if (before == null || after == null || before.id != after.id) return null
        if (token.isBlank() || before.pendingToken != token ||
            before.presentedToken != token || before.pendingOccurrenceMillis != occurrenceMillis) return null
        if (after.lastResponse != PersonalResponse.DONE ||
            after.lastResponseAtMillis <= 0L ||
            after.lastAcknowledgedOccurrenceMillis < occurrenceMillis ||
            after.pendingToken.isNotEmpty()) return null
        return PersonalCompletionEvent(before.id, occurrenceMillis, after.lastResponseAtMillis)
    }
}
