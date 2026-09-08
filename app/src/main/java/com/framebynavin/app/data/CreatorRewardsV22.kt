package com.framebynavin.app.data

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.first
import java.time.Instant
import java.time.ZoneId
import java.time.temporal.ChronoUnit
import java.time.temporal.TemporalAdjusters
import java.time.DayOfWeek

private val Context.creatorRewardsV22DataStore by preferencesDataStore(name = "creator_rewards_v22")

enum class CreatorAchievementId {
    FIRST_SPARK,
    FIRST_PUBLISH,
    LEARNING_LOOP,
    SHIPPING_RHYTHM,
    CREATOR_100,
    CREATOR_500,
}

data class CreatorAchievementSnapshot(
    val id: CreatorAchievementId,
    val title: String,
    val description: String,
    val current: Int,
    val target: Int,
    val unlockedAtMillis: Long = 0L,
) {
    val unlocked: Boolean get() = current >= target
    val progress: Float get() = (current.toFloat() / target.coerceAtLeast(1).toFloat()).coerceIn(0f, 1f)
}

enum class CreatorTraitId {
    IDEATOR,
    BUILDER,
    FINISHER,
    LEARNER,
    CONSISTENT,
}

data class CreatorTraitSnapshot(
    val id: CreatorTraitId,
    val title: String,
    val level: String,
    val body: String,
    val score: Int,
)

data class CreatorRewardProgressSnapshot(
    val totalXp: Int,
    val level: Int,
    val levelProgress: Float,
    val xpToNextLevel: Int,
    val weeklyMomentum: Int,
    val weeklyEventCount: Int,
    val achievements: List<CreatorAchievementSnapshot>,
    val traits: List<CreatorTraitSnapshot>,
    val recentRewards: List<CreatorRewardLedgerEntry>,
) {
    val unlockedAchievementCount: Int get() = achievements.count { it.unlocked }
    val nextAchievement: CreatorAchievementSnapshot? get() = achievements.firstOrNull { !it.unlocked }
}

object CreatorRewardsV22Engine {
    fun snapshot(
        entries: List<CreatorRewardLedgerEntry>,
        nowMillis: Long = System.currentTimeMillis(),
        zoneId: ZoneId = ZoneId.systemDefault(),
    ): CreatorRewardProgressSnapshot {
        val clean = entries.filter { it.occurredAtMillis > 0L }.sortedBy { it.occurredAtMillis }
        val totalXp = clean.sumOf { it.xp.coerceAtLeast(0) }
        val currentLevelBase = (totalXp / XP_PER_LEVEL) * XP_PER_LEVEL
        val weekStartMillis = Instant.ofEpochMilli(nowMillis)
            .atZone(zoneId)
            .toLocalDate()
            .with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY))
            .atStartOfDay(zoneId)
            .toInstant()
            .toEpochMilli()
        val weekly = clean.filter { it.occurredAtMillis in weekStartMillis..nowMillis }
        val weeklyMomentum = weekly.sumOf { it.momentum.coerceAtLeast(0) }

        return CreatorRewardProgressSnapshot(
            totalXp = totalXp,
            level = (totalXp / XP_PER_LEVEL) + 1,
            levelProgress = ((totalXp - currentLevelBase).toFloat() / XP_PER_LEVEL.toFloat()).coerceIn(0f, 1f),
            xpToNextLevel = XP_PER_LEVEL - (totalXp - currentLevelBase),
            weeklyMomentum = weeklyMomentum,
            weeklyEventCount = weekly.size,
            achievements = achievements(clean, totalXp),
            traits = traits(clean, nowMillis, zoneId),
            recentRewards = clean.asReversed().take(6),
        )
    }

    /**
     * Conservative Alpha22 backfill. Only actions with reliable stored evidence are credited:
     * idea creation dates, completed published projects, and completed post-publish checkpoints.
     * Workflow stages are intentionally not reconstructed because older versions did not store
     * trustworthy completion timestamps for each individual stage.
     */
    fun backfillCandidates(
        tasks: List<CreatorTask>,
        ideas: List<CreatorIdea>,
        checkpoints: List<PostPublishCheckpoint>,
    ): List<CreatorRewardLedgerEntry> {
        val byKey = linkedMapOf<String, CreatorRewardLedgerEntry>()

        ideas.filter { it.createdAtMillis > 0L && it.title.isNotBlank() }
            .sortedBy { it.createdAtMillis }
            .forEach { idea ->
                val entry = CreatorRewardEngine.ideaDailyCapture(idea.id, idea.createdAtMillis)
                byKey.putIfAbsent(entry.eventKey, entry)
            }

        tasks.filter { task ->
            task.publishedAtMillis > 0L &&
                task.id != "starter-frame-breakdown" &&
                !CreatorPostPublishEngine.isLegacyTask(task) &&
                !(task.origin == CreatorTaskOrigin.WEEKLY && WeeklyScheduleEngine.isLegacySeedSlot(task.scheduleSlotId))
        }.sortedBy { it.publishedAtMillis }.forEach { task ->
            val entry = CreatorRewardEngine.projectPublished(task.id, task.title, task.publishedAtMillis)
            byKey.putIfAbsent(entry.eventKey, entry)
        }

        tasks.mapNotNull(CreatorPostPublishEngine::fromLegacyTask)
            .filter { it.status == PostPublishCheckpointStatus.DONE && it.completedAtMillis > 0L }
            .forEach { checkpoint ->
                val entry = CreatorRewardEngine.postPublishCompleted(checkpoint, checkpoint.completedAtMillis)
                byKey.putIfAbsent(entry.eventKey, entry)
            }

        checkpoints.filter { it.status == PostPublishCheckpointStatus.DONE && it.completedAtMillis > 0L }
            .forEach { checkpoint ->
                val entry = CreatorRewardEngine.postPublishCompleted(checkpoint, checkpoint.completedAtMillis)
                byKey.putIfAbsent(entry.eventKey, entry)
            }

        return byKey.values.sortedBy { it.occurredAtMillis }
    }

    private fun achievements(
        entries: List<CreatorRewardLedgerEntry>,
        totalXp: Int,
    ): List<CreatorAchievementSnapshot> {
        val ideaEntries = entries.filter { it.type == CreatorRewardEventType.IDEA_DAILY_CAPTURE }
        val publishEntries = entries.filter { it.type == CreatorRewardEventType.PROJECT_PUBLISHED }
        val learningEntries = entries.filter { it.type == CreatorRewardEventType.POST_PUBLISH_COMPLETED }

        fun firstAt(type: CreatorRewardEventType): Long = entries.firstOrNull { it.type == type }?.occurredAtMillis ?: 0L
        fun xpThresholdAt(target: Int): Long {
            var running = 0
            entries.forEach { entry ->
                running += entry.xp
                if (running >= target) return entry.occurredAtMillis
            }
            return 0L
        }

        return listOf(
            CreatorAchievementSnapshot(
                CreatorAchievementId.FIRST_SPARK,
                "First Spark",
                "Capture your first idea.",
                ideaEntries.size.coerceAtMost(1),
                1,
                firstAt(CreatorRewardEventType.IDEA_DAILY_CAPTURE),
            ),
            CreatorAchievementSnapshot(
                CreatorAchievementId.FIRST_PUBLISH,
                "First Publish",
                "Ship your first project.",
                publishEntries.size.coerceAtMost(1),
                1,
                firstAt(CreatorRewardEventType.PROJECT_PUBLISHED),
            ),
            CreatorAchievementSnapshot(
                CreatorAchievementId.LEARNING_LOOP,
                "Learning Loop",
                "Complete a post-publish performance check.",
                learningEntries.size.coerceAtMost(1),
                1,
                firstAt(CreatorRewardEventType.POST_PUBLISH_COMPLETED),
            ),
            CreatorAchievementSnapshot(
                CreatorAchievementId.SHIPPING_RHYTHM,
                "Shipping Rhythm",
                "Publish 3 projects.",
                publishEntries.size.coerceAtMost(3),
                3,
                publishEntries.getOrNull(2)?.occurredAtMillis ?: 0L,
            ),
            CreatorAchievementSnapshot(
                CreatorAchievementId.CREATOR_100,
                "Creator 100",
                "Earn 100 XP from real creator actions.",
                totalXp.coerceAtMost(100),
                100,
                xpThresholdAt(100),
            ),
            CreatorAchievementSnapshot(
                CreatorAchievementId.CREATOR_500,
                "Creator 500",
                "Earn 500 XP from real creator actions.",
                totalXp.coerceAtMost(500),
                500,
                xpThresholdAt(500),
            ),
        )
    }

    private fun traits(
        entries: List<CreatorRewardLedgerEntry>,
        nowMillis: Long,
        zoneId: ZoneId,
    ): List<CreatorTraitSnapshot> {
        val counts = entries.groupingBy { it.type }.eachCount()
        val activeSince = Instant.ofEpochMilli(nowMillis).minus(29, ChronoUnit.DAYS).toEpochMilli()
        val activeDays = entries.asSequence()
            .filter { it.occurredAtMillis in activeSince..nowMillis }
            .map { Instant.ofEpochMilli(it.occurredAtMillis).atZone(zoneId).toLocalDate() }
            .distinct()
            .count()

        val raw = listOf(
            TraitSeed(
                CreatorTraitId.IDEATOR,
                "Ideator",
                counts.getOrDefault(CreatorRewardEventType.IDEA_DAILY_CAPTURE, 0) +
                    counts.getOrDefault(CreatorRewardEventType.IDEA_CONVERTED, 0) * 2,
                "You capture ideas and turn promising ones into projects.",
            ),
            TraitSeed(
                CreatorTraitId.BUILDER,
                "Builder",
                counts.getOrDefault(CreatorRewardEventType.PROJECT_STAGE_COMPLETED, 0) +
                    counts.getOrDefault(CreatorRewardEventType.IDEA_CONVERTED, 0),
                "You move projects through real production steps.",
            ),
            TraitSeed(
                CreatorTraitId.FINISHER,
                "Finisher",
                counts.getOrDefault(CreatorRewardEventType.PROJECT_PUBLISHED, 0) * 4,
                "You turn active projects into published work.",
            ),
            TraitSeed(
                CreatorTraitId.LEARNER,
                "Learner",
                counts.getOrDefault(CreatorRewardEventType.POST_PUBLISH_COMPLETED, 0) * 3,
                "You review published work and close the learning loop.",
            ),
            TraitSeed(
                CreatorTraitId.CONSISTENT,
                "Consistent",
                activeDays * 2,
                "You show up across multiple creator days, not only at publish time.",
            ),
        )

        return raw.filter { it.score > 0 }
            .sortedWith(compareByDescending<TraitSeed> { it.score }.thenBy { it.id.ordinal })
            .take(3)
            .map { seed ->
                CreatorTraitSnapshot(
                    id = seed.id,
                    title = seed.title,
                    level = when {
                        seed.score >= 10 -> "Strong"
                        seed.score >= 5 -> "Building"
                        else -> "Emerging"
                    },
                    body = seed.body,
                    score = seed.score,
                )
            }
    }

    private data class TraitSeed(
        val id: CreatorTraitId,
        val title: String,
        val score: Int,
        val body: String,
    )

    private const val XP_PER_LEVEL = 100
}

/** One-time, idempotent migration marker for the conservative Alpha22 reward backfill. */
class CreatorRewardBackfillStore(private val context: Context) {
    private val backfillDoneKey = booleanPreferencesKey("alpha22_verified_backfill_done")

    suspend fun runOnce(
        rewardStore: CreatorRewardStore,
        tasks: List<CreatorTask>,
        ideas: List<CreatorIdea>,
        checkpoints: List<PostPublishCheckpoint>,
    ): Int {
        val prefs = context.creatorRewardsV22DataStore.data.first()
        val candidates = CreatorRewardsV22Engine.backfillCandidates(tasks, ideas, checkpoints)
        val alreadyDone = prefs[backfillDoneKey] == true
        if (alreadyDone && (candidates.isEmpty() || rewardStore.load().isNotEmpty())) return 0

        var added = 0
        candidates.forEach { candidate ->
            if (rewardStore.record(candidate)) added++
        }
        context.creatorRewardsV22DataStore.edit { it[backfillDoneKey] = true }
        return added
    }
}
