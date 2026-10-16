package com.framebynavin.app.data

/** Human-readable guide over authoritative reward events. No invented events or persistence. */
enum class CreatorProgressDestination { IDEAS, PROJECTS }

data class CreatorProgressNextStep(
    val title: String,
    val explanation: String,
    val actionLabel: String,
    val destination: CreatorProgressDestination,
)

object CreatorProgressV165Policy {
    fun nextStep(snapshot: CreatorRewardProgressSnapshot): CreatorProgressNextStep? =
        when (snapshot.nextAchievement?.id) {
            CreatorAchievementId.FIRST_SPARK -> CreatorProgressNextStep(
                "Save your first idea", "Start with a thought you want to explore.",
                "OPEN IDEA VAULT", CreatorProgressDestination.IDEAS)
            CreatorAchievementId.FIRST_PUBLISH -> CreatorProgressNextStep(
                "Publish a project", "Complete a project and record when it went live.",
                "OPEN PROJECTS", CreatorProgressDestination.PROJECTS)
            CreatorAchievementId.LEARNING_LOOP -> CreatorProgressNextStep(
                "Review your published work", "Finish a post-publish performance check.",
                "OPEN PROJECTS", CreatorProgressDestination.PROJECTS)
            CreatorAchievementId.SHIPPING_RHYTHM -> CreatorProgressNextStep(
                "Keep shipping your work", "Publish three projects in total.",
                "OPEN PROJECTS", CreatorProgressDestination.PROJECTS)
            CreatorAchievementId.CREATOR_100, CreatorAchievementId.CREATOR_500 ->
                CreatorProgressNextStep(
                    "Keep creating", "Points come from recorded creator actions, not screen visits.",
                    "OPEN PROJECTS", CreatorProgressDestination.PROJECTS)
            null -> null
        }

    fun weeklySummary(snapshot: CreatorRewardProgressSnapshot): String =
        if (snapshot.weeklyEventCount == 0) "No rewarded actions recorded this week."
        else "${snapshot.weeklyEventCount} rewarded action${if (snapshot.weeklyEventCount == 1) "" else "s"} this week."

    fun progressMeaning(): String =
        "Only actions recorded in Backlot count here. Points are not YouTube views, earnings or upload statistics."
}
