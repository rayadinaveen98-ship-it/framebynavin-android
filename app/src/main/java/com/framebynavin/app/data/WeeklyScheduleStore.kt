package com.framebynavin.app.data

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import org.json.JSONArray
import org.json.JSONObject
import java.time.DayOfWeek

private val Context.weeklyScheduleDataStore by preferencesDataStore(name = "weekly_schedule_v08")

private val weeklyScheduleMutationMutex = Mutex()
class WeeklyScheduleStore(private val context: Context) {
    private val mutationMutex = weeklyScheduleMutationMutex

    private val slotsKey = stringPreferencesKey("weekly_slots_json")
    private val backupKey = stringPreferencesKey("weekly_slots_json_last_good")

    private suspend fun saveUnlocked(slots: List<WeeklyScheduleSlot>) {
        val encoded = encode(slots)
        context.weeklyScheduleDataStore.edit { prefs ->
            prefs[slotsKey]?.let { previous -> if (runCatching { decode(previous) }.isSuccess) prefs[backupKey] = previous }
            prefs[slotsKey] = encoded
        }
    }

    suspend fun loadOrSeed(): List<WeeklyScheduleSlot> = CreatorDataGate.transaction {
        val prefs = context.weeklyScheduleDataStore.data.first()
        val raw = prefs[slotsKey] ?: return@transaction emptyList()
        val decoded = runCatching { decode(raw) }.getOrElse { cause ->
            val backup = prefs[backupKey] ?: throw IllegalStateException("Weekly plan is unreadable. The original has been retained.", cause)
            runCatching { decode(backup) }.getOrElse { throw IllegalStateException("Both weekly plan copies are unreadable.", it) }
        }
        val cleaned = decoded.filterNot { WeeklyScheduleEngine.isLegacySeedSlot(it.id) }
        if (cleaned.size != decoded.size) save(cleaned)
        cleaned
    }

    suspend fun save(slots: List<WeeklyScheduleSlot>)= CreatorDataGate.transaction {
        mutationMutex.withLock {
            saveUnlocked(slots)
        }
    }

    private suspend fun loadRaw(): List<WeeklyScheduleSlot> {
        val raw = context.weeklyScheduleDataStore.data.first()[slotsKey] ?: return emptyList()
        return decode(raw).filterNot { WeeklyScheduleEngine.isLegacySeedSlot(it.id) }
    }

    suspend fun applyDelta(base: List<WeeklyScheduleSlot>, desired: List<WeeklyScheduleSlot>, expectedGeneration: Long): List<WeeklyScheduleSlot> = CreatorDataGate.transaction {
        mutationMutex.withLock {
            if (CreatorDataGate.generation(context) != expectedGeneration)
                throw CreatorWriteConflict("An older schedule edit was cancelled after restore")
            val updated = CreatorDeltaEngine.merge(base, desired, loadRaw()) { it.id }
            saveUnlocked(updated)
            updated
        }
    }

    suspend fun exportJson(): String = encode(loadOrSeed())

    suspend fun importJson(raw: String): List<WeeklyScheduleSlot> {
        val decoded = decode(raw)
        save(decoded)
        return decoded
    }

    fun validateJson(raw: String): Int = decode(raw).size

    private fun encode(slots: List<WeeklyScheduleSlot>): String {
        val array = JSONArray()
        slots.forEach { slot ->
            array.put(
                JSONObject()
                    .put("id", slot.id)
                    .put("title", slot.title)
                    .put("dayOfWeek", slot.dayOfWeek.name)
                    .put("hour", slot.hour)
                    .put("minute", slot.minute)
                    .put("platform", slot.platform)
                    .put("contentType", slot.contentType)
                    .put("enabled", slot.enabled)
                    .put("cadence", slot.cadence.name)
                    .put("reminderMode", slot.reminderMode.name)
                    .put("priority", slot.priority.name)
            )
        }
        return array.toString()
    }

    private fun decode(raw: String): List<WeeklyScheduleSlot> {
        val array = JSONArray(raw)
        return buildList {
            for (i in 0 until array.length()) {
                val item = array.getJSONObject(i)
                val id = item.optString("id").ifBlank { "weekly-$i" }
                val title = item.optString("title", "Creator Slot").trim()
                require(title.isNotBlank()) { "Weekly slot $i has no title" }
                add(
                    WeeklyScheduleSlot(
                        id = id,
                        title = title,
                        dayOfWeek = runCatching { DayOfWeek.valueOf(item.optString("dayOfWeek", DayOfWeek.MONDAY.name)) }.getOrDefault(DayOfWeek.MONDAY),
                        hour = item.optInt("hour", 19).coerceIn(0, 23),
                        minute = item.optInt("minute", 0).coerceIn(0, 59),
                        platform = item.optString("platform", "Instagram"),
                        contentType = item.optString("contentType", "Reel"),
                        enabled = item.optBoolean("enabled", true),
                        cadence = runCatching { ScheduleCadence.valueOf(item.optString("cadence", ScheduleCadence.EVERY_WEEK.name)) }.getOrDefault(ScheduleCadence.EVERY_WEEK),
                        reminderMode = runCatching { ReminderMode.valueOf(item.optString("reminderMode", ReminderMode.SMART.name)) }.getOrDefault(ReminderMode.SMART),
                        priority = runCatching { TaskPriority.valueOf(item.optString("priority", TaskPriority.IMPORTANT.name)) }.getOrDefault(TaskPriority.IMPORTANT),
                    )
                )
            }
        }
    }
}
