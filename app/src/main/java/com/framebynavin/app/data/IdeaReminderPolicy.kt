package com.framebynavin.app.data

import java.time.Instant
import java.time.ZoneId

enum class IdeaReminderCadence {
    ONCE,
    DAILY,
}

object IdeaReminderPolicy {
    fun isActionable(idea: CreatorIdea): Boolean =
        idea.reminderAtMillis > 0L &&
            idea.status != IdeaStatus.CONVERTED &&
            idea.status != IdeaStatus.ARCHIVED &&
            idea.projectTaskId.isBlank()

    fun normalize(idea: CreatorIdea, now: Long = System.currentTimeMillis(), zone: ZoneId = ZoneId.systemDefault()): CreatorIdea {
        if (!isActionable(idea)) {
            return if (idea.reminderAtMillis == 0L && idea.reminderCadence == IdeaReminderCadence.ONCE) idea
            else idea.copy(reminderAtMillis = 0L, reminderCadence = IdeaReminderCadence.ONCE)
        }
        if (idea.reminderAtMillis > now) return idea
        return when (idea.reminderCadence) {
            IdeaReminderCadence.ONCE -> idea.copy(reminderAtMillis = 0L)
            IdeaReminderCadence.DAILY -> idea.copy(reminderAtMillis = nextDailyOccurrence(idea.reminderAtMillis, now, zone))
        }
    }

    fun afterFired(idea: CreatorIdea, firedAt: Long = System.currentTimeMillis(), zone: ZoneId = ZoneId.systemDefault()): CreatorIdea =
        when (idea.reminderCadence) {
            IdeaReminderCadence.ONCE -> idea.copy(reminderAtMillis = 0L)
            IdeaReminderCadence.DAILY -> idea.copy(reminderAtMillis = nextDailyOccurrence(idea.reminderAtMillis, firedAt, zone))
        }

    fun nextDailyOccurrence(previousAt: Long, now: Long, zone: ZoneId = ZoneId.systemDefault()): Long {
        val previous = Instant.ofEpochMilli(previousAt.coerceAtLeast(1L)).atZone(zone)
        val nowZoned = Instant.ofEpochMilli(now).atZone(zone)
        var candidate = nowZoned.toLocalDate().atTime(previous.toLocalTime()).atZone(zone)
        if (!candidate.isAfter(nowZoned)) candidate = candidate.plusDays(1)
        return candidate.toInstant().toEpochMilli()
    }
}
