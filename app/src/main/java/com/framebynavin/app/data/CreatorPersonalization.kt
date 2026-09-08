package com.framebynavin.app.data

import java.time.DayOfWeek
import java.time.Instant
import java.time.ZoneId
import java.time.temporal.TemporalAdjusters

data class CreatorPersonalizationSnapshot(
    val category: String,
    val primaryPlatform: String,
    val platformSummary: String,
    val goal: String,
    val weeklyTarget: Int,
    val publishedThisWeek: Int,
    val weeklyProgress: Float,
    val focusTitle: String,
    val focusBody: String,
    val insightTitle: String,
    val insightBody: String,
    val emptyProjectBody: String,
)

/**
 * Turns the creator-selected onboarding profile into small, deterministic product guidance.
 * This remains local and rule-based in v1.8; it is not an AI recommendation system.
 */
object CreatorPersonalizationEngine {
    private val platformPriority = listOf(
        "YouTube",
        "Instagram",
        "X",
        "Facebook",
        "LinkedIn",
        "Podcast",
        "Blog / Newsletter",
        "Other",
    )

    fun snapshot(
        profile: CreatorProfile,
        tasks: List<CreatorTask>,
        nowMillis: Long = System.currentTimeMillis(),
        zoneId: ZoneId = ZoneId.systemDefault(),
    ): CreatorPersonalizationSnapshot {
        val normalized = profile.normalized()
        val category = normalized.category.ifBlank { "Creator" }
        val platforms = orderedPlatforms(normalized.platforms)
        val primaryPlatform = platforms.firstOrNull() ?: "your main platform"
        val platformSummary = platforms.take(3).joinToString(" · ").ifBlank { "Your platforms" }
        val goal = normalized.primaryGoal.ifBlank { "Publish consistently" }
        val target = normalized.weeklyPublishingTarget.coerceIn(1, 14)
        val weekStart = weekStartMillis(nowMillis, zoneId)
        val published = tasks.count { task ->
            task.publishedAtMillis >= weekStart &&
                task.publishedAtMillis in 1L..nowMillis
        }
        val progress = (published.toFloat() / target.toFloat()).coerceIn(0f, 1f)

        val focus = focusCopy(goal, primaryPlatform, category, published, target)
        val insight = insightCopy(goal, primaryPlatform, published, target)
        val categoryForSentence = category.replace("&", "and").lowercase()
        val emptyBody = "Start a $categoryForSentence project for $primaryPlatform, or capture an idea first."

        return CreatorPersonalizationSnapshot(
            category = category,
            primaryPlatform = primaryPlatform,
            platformSummary = platformSummary,
            goal = goal,
            weeklyTarget = target,
            publishedThisWeek = published,
            weeklyProgress = progress,
            focusTitle = focus.first,
            focusBody = focus.second,
            insightTitle = insight.first,
            insightBody = insight.second,
            emptyProjectBody = emptyBody,
        )
    }

    private fun orderedPlatforms(platforms: Set<String>): List<String> {
        val clean = platforms.map { it.trim() }.filter { it.isNotBlank() }.distinct()
        return clean.sortedWith(
            compareBy<String> { value ->
                platformPriority.indexOfFirst { it.equals(value, ignoreCase = true) }.let { if (it < 0) Int.MAX_VALUE else it }
            }.thenBy { it.lowercase() },
        )
    }

    private fun weekStartMillis(nowMillis: Long, zoneId: ZoneId): Long {
        val date = Instant.ofEpochMilli(nowMillis)
            .atZone(zoneId)
            .toLocalDate()
            .with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY))
        return date.atStartOfDay(zoneId).toInstant().toEpochMilli()
    }

    private fun focusCopy(
        goal: String,
        platform: String,
        category: String,
        published: Int,
        target: Int,
    ): Pair<String, String> = when (goal.lowercase()) {
        "publish consistently" -> "Protect your publishing rhythm." to ""
        "grow an audience" -> "Create for reach, then learn." to
            "On $platform, lead with a clear hook and one idea your ${category.replace("&", "and").lowercase()} audience will want to share."
        "improve content quality" -> "Raise one craft bar today." to
            "Use the next $platform project to improve one thing on purpose: story, clarity, sound, visuals or delivery."
        "build a creator business" -> "Create toward an outcome." to
            "Make the next $platform project serve the audience and the business instead of publishing just to stay busy."
        "launch a project" -> "Protect the launch path." to
            "Keep the next launch-critical step visible and move it forward before opening another project."
        "stay organized" -> "Keep the pipeline clear." to
            "Capture loose ideas, finish the next active step and keep $platform work out of your head."
        else -> "Keep your goal visible." to
            "$goal is the priority. Use $platform as the clearest lane for the next meaningful move."
    }

    private fun insightCopy(
        goal: String,
        platform: String,
        published: Int,
        target: Int,
    ): Pair<String, String> = when (goal.lowercase()) {
        "publish consistently" -> "Are you keeping your publishing promise?" to
            "$published of $target planned publishes are complete this week. Use the numbers below to protect the rhythm."
        "grow an audience" -> "Is your work creating audience momentum?" to
            "Read $platform performance alongside your $published / $target weekly publishing rhythm, then repeat what earns attention."
        "improve content quality" -> "Is better work becoming repeatable?" to
            "Use performance as feedback, not a score. Compare what you shipped with the craft choices you changed."
        "build a creator business" -> "Is your content supporting the bigger goal?" to
            "Track output and $platform response together so effort stays connected to useful creator outcomes."
        "launch a project" -> "Is the launch moving forward?" to
            "Use creator metrics as context while keeping launch progress and weekly output in view."
        "stay organized" -> "Is the system reducing creator friction?" to
            "Watch active work, finished work and $platform results together so the pipeline stays understandable."
        else -> "Is your creator system serving the goal?" to
            "$goal stays the lens. Compare output, active work and $platform response before deciding what to make next."
    }
}
