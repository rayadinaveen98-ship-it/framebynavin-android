package com.framebynavin.app.data

import java.util.UUID

data class CreatorProjectPreset(
    val id: String,
    val label: String,
    val platform: String,
    val contentType: String,
    val priority: TaskPriority = TaskPriority.IMPORTANT,
    val suggestedNotes: String = "",
)

data class CreatorDeliverableTemplate(
    val platform: String,
    val format: String,
)

data class CreatorContentTemplate(
    val id: String,
    val name: String,
    val checklist: List<String>,
    val deliverables: List<CreatorDeliverableTemplate>,
)

object CreatorProjectTemplates {
    /** Existing new-project presets remain available to the rest of the app. */
    val presets: List<CreatorProjectPreset> = listOf(
        CreatorProjectPreset(
            id = "youtube_analysis",
            label = "YouTube Analysis",
            platform = "YouTube",
            contentType = "Long-form",
            priority = TaskPriority.IMPORTANT,
            suggestedNotes = "Angle • Research • Script • Record • Edit • Thumbnail • Upload • Publish • Promote",
        ),
        CreatorProjectPreset(
            id = "movie_review_short",
            label = "Movie Review Short",
            platform = "YouTube",
            contentType = "Short",
            priority = TaskPriority.IMPORTANT,
            suggestedNotes = "Hook • Core opinion • 60-sec script • Voice • Edit • Cover • Upload • Promote",
        ),
        CreatorProjectPreset(
            id = "cinematic_compilation",
            label = "Cinematic Compilation",
            platform = "YouTube",
            contentType = "Cinematic Moment",
            priority = TaskPriority.IMPORTANT,
            suggestedNotes = "Select moments • Build sequence • Sound + grade • Thumbnail • Upload • Publish • Promote",
        ),
        CreatorProjectPreset(
            id = "instagram_reel",
            label = "Instagram Reel",
            platform = "Instagram",
            contentType = "Reel",
            priority = TaskPriority.NORMAL,
            suggestedNotes = "Hook • Script/beats • Voice • Vertical edit • Cover/caption • Upload • Promote",
        ),
        CreatorProjectPreset(
            id = "x_post",
            label = "X Post",
            platform = "X",
            contentType = "Post",
            priority = TaskPriority.NORMAL,
            suggestedNotes = "Draft • Verify wording/context • Publish • Follow up if needed",
        ),
    )

    val contentTemplates: List<CreatorContentTemplate> = listOf(
        CreatorContentTemplate(
            id = "youtube_long",
            name = "YouTube Long",
            checklist = listOf("Research", "Angle + hook", "Script", "Record voice", "Edit", "Thumbnail + title", "Publish", "Review performance"),
            deliverables = listOf(CreatorDeliverableTemplate("YouTube", "Long video")),
        ),
        CreatorContentTemplate(
            id = "youtube_short",
            name = "YouTube Short",
            checklist = listOf("Hook", "Select clip / visual", "Edit", "Caption + title", "Publish"),
            deliverables = listOf(CreatorDeliverableTemplate("YouTube", "Short")),
        ),
        CreatorContentTemplate(
            id = "instagram_reel",
            name = "Instagram Reel",
            checklist = listOf("Hook", "Select clip / visual", "Edit", "Caption + hashtags", "Cover", "Publish"),
            deliverables = listOf(CreatorDeliverableTemplate("Instagram", "Reel")),
        ),
        CreatorContentTemplate(
            id = "movie_review",
            name = "Movie Review",
            checklist = listOf("Watch + raw notes", "Verdict", "Performance notes", "Writing / screenplay notes", "Technical notes", "Script", "Record", "Edit", "Thumbnail + title", "Publish"),
            deliverables = listOf(
                CreatorDeliverableTemplate("YouTube", "Long video"),
                CreatorDeliverableTemplate("Instagram", "Reel"),
            ),
        ),
        CreatorContentTemplate(
            id = "cinematic_analysis",
            name = "Cinematic Analysis",
            checklist = listOf("Research", "Collect frames", "Structure argument", "Write hook", "Script", "Record voice", "Edit", "Sound polish", "Thumbnail + title", "Publish", "Extract derivatives"),
            deliverables = listOf(
                CreatorDeliverableTemplate("YouTube", "Long video"),
                CreatorDeliverableTemplate("YouTube", "Short"),
                CreatorDeliverableTemplate("Instagram", "Reel"),
            ),
        ),
        CreatorContentTemplate(
            id = "recommendation_reel",
            name = "Recommendation Reel",
            checklist = listOf("Choose title", "Pick strongest visual", "Write 1-line reason", "Edit", "OTT / availability check", "Caption", "Publish"),
            deliverables = listOf(
                CreatorDeliverableTemplate("Instagram", "Reel"),
                CreatorDeliverableTemplate("YouTube", "Short"),
            ),
        ),
        CreatorContentTemplate(
            id = "news_reaction",
            name = "News / Reaction",
            checklist = listOf("Verify source", "Confirm second source", "Define what changed", "Write context", "Record", "Edit", "Publish", "Update if story changes"),
            deliverables = listOf(
                CreatorDeliverableTemplate("YouTube", "Short / explainer"),
                CreatorDeliverableTemplate("Instagram", "Reel"),
            ),
        ),
    )

    fun find(id: String): CreatorProjectPreset? = presets.firstOrNull { it.id == id }

    /**
     * Content templates are deliberately additive. Applying a template never deletes, resets,
     * reorders, or republishes existing creator work.
     */
    fun applyContentTemplate(
        workspace: CreatorContentWorkspace,
        template: CreatorContentTemplate,
        projectTitle: String,
    ): CreatorContentWorkspace {
        val existingChecklistKeys = workspace.checklist.map { it.title.trim().lowercase() }.toSet()
        val addedChecklist = template.checklist
            .filterNot { it.trim().lowercase() in existingChecklistKeys }
            .map { CreatorChecklistItem(id = UUID.randomUUID().toString(), title = it) }

        val existingDeliverableKeys = workspace.deliverables
            .filter { it.parentDeliverableId.isBlank() }
            .map { "${it.platform.trim().lowercase()}|${it.format.trim().lowercase()}" }
            .toSet()
        val addedDeliverables = template.deliverables
            .filterNot { "${it.platform.trim().lowercase()}|${it.format.trim().lowercase()}" in existingDeliverableKeys }
            .map {
                CreatorDeliverable(
                    id = UUID.randomUUID().toString(),
                    platform = it.platform,
                    format = it.format,
                    title = projectTitle,
                )
            }

        return workspace.copy(
            checklist = workspace.checklist + addedChecklist,
            deliverables = workspace.deliverables + addedDeliverables,
        )
    }
}

fun CreatorContentWorkspace.productionProgressPercent(): Int {
    val actionable = checklist.filter { it.status != CreatorChecklistStatus.SKIPPED }
    if (actionable.isEmpty()) return 0
    val done = actionable.count { it.status == CreatorChecklistStatus.DONE }
    return ((done * 100.0) / actionable.size).toInt().coerceIn(0, 100)
}

fun CreatorContentWorkspace.nextProductionStep(): CreatorChecklistItem? =
    checklist.firstOrNull { it.status == CreatorChecklistStatus.TODO }
