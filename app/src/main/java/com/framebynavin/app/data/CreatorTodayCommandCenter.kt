package com.framebynavin.app.data

import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId

data class CreatorTodayItem(
    val task: CreatorTask,
    val recommendation: CreatorRecommendation,
)

data class CreatorTodayCommitment(
    val task: CreatorTask,
    val dueAtMillis: Long,
)

data class CreatorTodayCommandCenter(
    val continueItem: CreatorTodayItem?,
    val upNext: List<CreatorTodayItem>,
    val commitments: List<CreatorTodayCommitment>,
    val activeCount: Int,
    val dueTodayCount: Int,
    val overdueCount: Int,
)

/**
 * Deterministic execution view for v2.0 Today.
 *
 * This engine is intentionally read-only. It ranks the existing CreatorTask source of truth and
 * never changes workflow stage, publication state, reminder state, or persistence on its own.
 */
object CreatorTodayCommandCenterEngine {
    fun build(
        tasks: List<CreatorTask>,
        nowMillis: Long = System.currentTimeMillis(),
        zone: ZoneId = ZoneId.systemDefault(),
    ): CreatorTodayCommandCenter {
        val ranked = CreatorPriorityEngine.rankActive(tasks, nowMillis)
        val today = Instant.ofEpochMilli(nowMillis).atZone(zone).toLocalDate()

        val continueItem = ranked.firstOrNull()?.let { task ->
            CreatorTodayItem(task, CreatorPriorityEngine.recommendation(task, nowMillis))
        }

        val upNext = ranked
            .drop(1)
            .take(3)
            .map { task -> CreatorTodayItem(task, CreatorPriorityEngine.recommendation(task, nowMillis)) }

        val commitments = ranked
            .asSequence()
            .filter { it.dueAtMillis > 0L }
            .sortedWith(compareBy<CreatorTask> { it.dueAtMillis }.thenBy { it.title.lowercase() })
            .take(4)
            .map { CreatorTodayCommitment(task = it, dueAtMillis = it.dueAtMillis) }
            .toList()

        return CreatorTodayCommandCenter(
            continueItem = continueItem,
            upNext = upNext,
            commitments = commitments,
            activeCount = ranked.size,
            dueTodayCount = ranked.count { it.dueAtMillis > 0L && localDate(it.dueAtMillis, zone) == today },
            overdueCount = ranked.count { it.dueAtMillis in 1 until nowMillis },
        )
    }

    private fun localDate(millis: Long, zone: ZoneId): LocalDate =
        Instant.ofEpochMilli(millis).atZone(zone).toLocalDate()
}
