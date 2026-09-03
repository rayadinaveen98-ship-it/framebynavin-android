package com.framebynavin.app.data

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.first
import org.json.JSONArray
import org.json.JSONObject
import java.time.DayOfWeek

private val Context.weeklyScheduleDataStore by preferencesDataStore(name = "weekly_schedule_v08")

class WeeklyScheduleStore(private val context: Context) {
    private val slotsKey = stringPreferencesKey("weekly_slots_json")

    suspend fun loadOrSeed(): List<WeeklyScheduleSlot> {
        val prefs = context.weeklyScheduleDataStore.data.first()
        val raw = prefs[slotsKey]
        if (raw == null) {
            val defaults = WeeklyScheduleEngine.defaultSlots()
            save(defaults)
            return defaults
        }
        return runCatching { decode(raw) }.getOrElse { WeeklyScheduleEngine.defaultSlots() }
    }

    suspend fun save(slots: List<WeeklyScheduleSlot>) {
        context.weeklyScheduleDataStore.edit { prefs -> prefs[slotsKey] = encode(slots) }
    }

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
                add(
                    WeeklyScheduleSlot(
                        id = item.optString("id").ifBlank { "weekly-$i" },
                        title = item.optString("title", "Creator Slot"),
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
