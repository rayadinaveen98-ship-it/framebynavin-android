package com.framebynavin.app.personal

import java.time.Instant
import java.time.LocalTime
import java.time.ZoneId

/** Offline suggestions; nothing scheduled until owner saves the form. */
internal enum class V288ReminderSuggestion(
    val label: String,
    val title: String,
    val purpose: PersonalPurpose,
    val time: LocalTime,
    val repeat: PersonalRepeat,
) {
    SKINCARE("Skincare", "Skin care routine", PersonalPurpose.PERSONAL_GOAL, LocalTime.of(8, 0), PersonalRepeat.DAILY),
    SUNSCREEN("Sunscreen", "Apply sunscreen", PersonalPurpose.PERSONAL_GOAL, LocalTime.of(8, 0), PersonalRepeat.DAILY),
    READING("Reading", "Reading time", PersonalPurpose.PERSONAL_GOAL, LocalTime.of(22, 0), PersonalRepeat.DAILY),
    WORKOUT("Workout", "Movement session", PersonalPurpose.MOVEMENT, LocalTime.of(7, 15), PersonalRepeat.WEEKDAYS),
    EDITING("Learning", "Practice a skill", PersonalPurpose.PERSONAL_GOAL, LocalTime.of(11, 15), PersonalRepeat.WEEKDAYS),
    GAME_DEV("Hobbies", "Make time for a hobby", PersonalPurpose.PERSONAL_GOAL, LocalTime.of(17, 45), PersonalRepeat.WEEKDAYS);

    fun nextOccurrenceMillis(nowMillis: Long, zone: ZoneId): Long {
        val now = Instant.ofEpochMilli(nowMillis).atZone(zone)
        var candidate = now.toLocalDate().atTime(time).atZone(zone)
        if (candidate.toInstant().toEpochMilli() <= nowMillis) candidate = candidate.plusDays(1)
        if (repeat == PersonalRepeat.WEEKDAYS)
            while (candidate.dayOfWeek.value > 5) candidate = candidate.plusDays(1)
        return candidate.toInstant().toEpochMilli()
    }
}
