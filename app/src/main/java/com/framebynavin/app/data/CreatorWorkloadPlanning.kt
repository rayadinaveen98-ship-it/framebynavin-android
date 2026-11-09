package com.framebynavin.app.data

import android.content.Context

/**
 * Phase 5 workload is a VIEW of owner-provided future slot estimates, never
 * an automatic scheduler. Unknown estimates remain unknown (never assumed 0h).
 */
internal data class CreatorWeeklyWorkloadSnapshot(
    val occurrences: Int,
    val estimatedOccurrences: Int,
    val unknownOccurrences: Int,
    val plannedMinutes: Int,
    val availableMinutes: Int,
) {
    val isOverloaded: Boolean get() =
        availableMinutes > 0 && plannedMinutes > availableMinutes
    val hasIncompleteEstimates: Boolean get() = unknownOccurrences > 0
    val availabilityIsSet: Boolean get() = availableMinutes > 0
    val canClaimAllWorkIsCovered: Boolean get() = false

    fun summary(): String = buildString {
        append("${plannedMinutes / 60}h ${plannedMinutes % 60}m estimated")
        if (availableMinutes > 0) append(" / ${availableMinutes / 60}h ${availableMinutes % 60}m available")
        if (unknownOccurrences > 0) append(" · $unknownOccurrences unestimated")
    }
}

internal object CreatorWorkloadPlanning {
    fun nextSevenDays(
        slots: List<WeeklyScheduleSlot>,
        availableMinutes: Int,
        nowMillis: Long = System.currentTimeMillis(),
    ): CreatorWeeklyWorkloadSnapshot {
        require(availableMinutes in 0..10080)
        val upcoming = WeeklyScheduleEngine.upcomingOccurrences(
            slots, fromMillis = nowMillis, daysAhead = 7L,
        )
        val known = upcoming.filter { it.slot.estimatedMinutes in 1..720 }
        require(upcoming.all { it.slot.estimatedMinutes in 0..720 }) {
            "A weekly slot has an invalid creator estimate"
        }
        return CreatorWeeklyWorkloadSnapshot(
            occurrences = upcoming.size,
            estimatedOccurrences = known.size,
            unknownOccurrences = upcoming.size - known.size,
            plannedMinutes = known.sumOf { it.slot.estimatedMinutes },
            availableMinutes = availableMinutes,
        )
    }
}

/**
 * Optional locally saved weekly capacity. Creator restore epochs isolate old
 * settings, so a restored account does not inherit an unrelated availability.
 * This value is NOT represented as cloud-backed.
 */
internal class CreatorWorkloadAvailabilityStore(context: Context) {
    private val app = context.applicationContext
    private val prefs = app.getSharedPreferences("creator_workload_availability_phase5_v1", Context.MODE_PRIVATE)

    fun availableMinutes(): Int {
        val currentGeneration = CreatorDataGate.generation(app)
        if (prefs.getLong("generation", -1L) != currentGeneration) return 0
        return prefs.getInt("weekly_minutes", 0).takeIf { it in 0..10080 } ?: 0
    }

    fun saveAvailableMinutes(value: Int) {
        require(value == 0 || value in 30..10080) {
            "Choose 30 minutes or more, or clear availability"
        }
        val generation = CreatorDataGate.generation(app)
        check(prefs.edit()
            .putLong("generation", generation)
            .putInt("weekly_minutes", value)
            .commit()) { "Workload availability was not saved" }
    }
}
