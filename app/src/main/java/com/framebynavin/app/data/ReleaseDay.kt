package com.framebynavin.app.data

enum class ReleaseEventType {
    MOVIE,
    TRAILER,
    TEASER,
    POSTER,
    SONG,
    ANNOUNCEMENT,
    REVIEW,
    BREAKING,
}

enum class ReleaseUrgency {
    NOW,
    TODAY,
    LATER,
}

enum class ReleaseOutput {
    X_POST,
    INSTAGRAM_STORY,
    INSTAGRAM_REEL,
    YOUTUBE_SHORT,
}

data class ReleaseBurstRequest(
    val topic: String,
    val eventType: ReleaseEventType,
    val details: String,
    val urgency: ReleaseUrgency,
    val outputs: Set<ReleaseOutput>,
    val saveDeepDiveIdea: Boolean,
)

data class ReleaseProjectSpec(
    val output: ReleaseOutput,
    val label: String,
    val platform: String,
    val contentType: String,
    val dueOffsetMinutes: Long,
    val reminderMode: ReminderMode,
    val priority: TaskPriority,
    val startStageIndex: Int,
)

data class ReleaseLaunchResult(
    val createdProjects: Int,
    val ideaSaved: Boolean,
)

object ReleaseDayEngine {
    fun specs(request: ReleaseBurstRequest): List<ReleaseProjectSpec> = request.outputs.map { output ->
        val offset = offsetMinutes(request.urgency, output)
        val urgentPriority = when {
            request.urgency == ReleaseUrgency.NOW && (output == ReleaseOutput.INSTAGRAM_REEL || output == ReleaseOutput.YOUTUBE_SHORT) -> TaskPriority.CRITICAL
            request.urgency == ReleaseUrgency.NOW -> TaskPriority.IMPORTANT
            else -> TaskPriority.IMPORTANT
        }
        when (output) {
            ReleaseOutput.X_POST -> ReleaseProjectSpec(
                output = output,
                label = "X Reaction",
                platform = "X",
                contentType = "Post",
                dueOffsetMinutes = offset,
                reminderMode = ReminderMode.SIMPLE,
                priority = urgentPriority,
                startStageIndex = 1,
            )
            ReleaseOutput.INSTAGRAM_STORY -> ReleaseProjectSpec(
                output = output,
                label = "Story Reaction",
                platform = "Instagram",
                contentType = "Story",
                dueOffsetMinutes = offset,
                reminderMode = ReminderMode.SIMPLE,
                priority = urgentPriority,
                startStageIndex = 1,
            )
            ReleaseOutput.INSTAGRAM_REEL -> ReleaseProjectSpec(
                output = output,
                label = "Reaction Reel",
                platform = "Instagram",
                contentType = "Reel",
                dueOffsetMinutes = offset,
                reminderMode = ReminderMode.SMART,
                priority = urgentPriority,
                startStageIndex = 1,
            )
            ReleaseOutput.YOUTUBE_SHORT -> ReleaseProjectSpec(
                output = output,
                label = "Reaction Short",
                platform = "YouTube",
                contentType = "Short",
                dueOffsetMinutes = offset,
                reminderMode = ReminderMode.SMART,
                priority = urgentPriority,
                startStageIndex = 1,
            )
        }
    }.sortedBy { it.dueOffsetMinutes }

    fun eventLabel(type: ReleaseEventType): String = when (type) {
        ReleaseEventType.MOVIE -> "Movie"
        ReleaseEventType.TRAILER -> "Trailer"
        ReleaseEventType.TEASER -> "Teaser"
        ReleaseEventType.POSTER -> "Poster"
        ReleaseEventType.SONG -> "Song"
        ReleaseEventType.ANNOUNCEMENT -> "Announcement"
        ReleaseEventType.REVIEW -> "Review"
        ReleaseEventType.BREAKING -> "Breaking"
    }

    fun outputLabel(output: ReleaseOutput): String = when (output) {
        ReleaseOutput.X_POST -> "X Post"
        ReleaseOutput.INSTAGRAM_STORY -> "IG Story"
        ReleaseOutput.INSTAGRAM_REEL -> "IG Reel"
        ReleaseOutput.YOUTUBE_SHORT -> "YT Short"
    }

    private fun offsetMinutes(urgency: ReleaseUrgency, output: ReleaseOutput): Long = when (urgency) {
        ReleaseUrgency.NOW -> when (output) {
            ReleaseOutput.X_POST -> 10L
            ReleaseOutput.INSTAGRAM_STORY -> 15L
            ReleaseOutput.INSTAGRAM_REEL -> 90L
            ReleaseOutput.YOUTUBE_SHORT -> 150L
        }
        ReleaseUrgency.TODAY -> when (output) {
            ReleaseOutput.X_POST -> 30L
            ReleaseOutput.INSTAGRAM_STORY -> 45L
            ReleaseOutput.INSTAGRAM_REEL -> 180L
            ReleaseOutput.YOUTUBE_SHORT -> 240L
        }
        ReleaseUrgency.LATER -> when (output) {
            ReleaseOutput.X_POST -> 120L
            ReleaseOutput.INSTAGRAM_STORY -> 120L
            ReleaseOutput.INSTAGRAM_REEL -> 480L
            ReleaseOutput.YOUTUBE_SHORT -> 600L
        }
    }
}
