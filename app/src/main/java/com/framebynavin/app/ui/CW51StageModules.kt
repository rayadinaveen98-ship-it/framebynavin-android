package com.framebynavin.app.ui

import com.framebynavin.app.data.CreatorTask
import com.framebynavin.app.data.CreatorWorkflowEngine
import com.framebynavin.app.data.WorkflowStage

/** UX05.1C: stage-aware prompts; no new editors or persisted parallel records. */
internal data class CW51StageModule(
    val name: String,
    val guidance: String,
    val questions: List<String>,
    val suggestedTasks: List<String>,
)

internal object CW51StageModulePolicy {
    fun forStage(task: CreatorTask, stage: WorkflowStage): CW51StageModule? {
        val workflowId = CreatorWorkflowEngine.templateFor(task).id
        val mode = task.contentDna.creatorModeId
        val id = stage.id

        return when {
            workflowId.startsWith("v150_film_") && id in setOf("scene_evidence", "watch_notes", "evidence") ->
                CW51StageModule(
                    "Scene evidence map",
                    "Connect what you observed to a specific, supportable interpretation.",
                    listOf(
                        "Which scene and timecode supports your strongest claim?",
                        "What framing, sound, blocking or edit choice can the audience actually see?",
                        "Which sources or usage permissions still need review?",
                    ),
                    listOf("Link a scene observation to its original material",
                        "Check the difference between observation and interpretation"),
                )
            workflowId.startsWith("v150_film_") && id in setOf("voice", "record", "edit", "structure") ->
                CW51StageModule(
                    "Filmmaker production desk",
                    "Keep creative intent connected to the actual footage or narration.",
                    listOf(
                        "Which planned moment must be visible or audible?",
                        "Which missing take, voice line or reference blocks the edit?",
                    ),
                    listOf("Compare the rough sequence with the central argument"),
                )
            workflowId == "v150_podcast" && id in setOf("promise", "research", "outline", "record") ->
                CW51StageModule(
                    "Podcast run of show",
                    "Plan the conversation structure without pretending the dialogue is scripted.",
                    listOf(
                        "What central question makes this episode useful?",
                        "Which segments or guest questions should come first?",
                        "Which sources, consent or recording checks are still outstanding?",
                    ),
                    listOf("Confirm episode opening and closing", "Prepare transitions and guest prompts"),
                )
            (mode == "tech" || workflowId.startsWith("v150_tech_")) &&
                id in setOf("test_plan", "evidence", "benchmarks", "setup", "test") ->
                CW51StageModule(
                    "Repeatable test matrix",
                    "Document measurable conditions so comparisons stay credible.",
                    listOf(
                        "What exact result and baseline are being compared?",
                        "Which environment, version and method produced the observation?",
                        "Which claim is still unsupported or needs another run?",
                    ),
                    listOf("Record test conditions", "Link results to their primary evidence"),
                )
            (mode == "food" || workflowId == "v150_food_recipe") &&
                id in setOf("ingredients", "method", "shot_plan", "capture") ->
                CW51StageModule(
                    "Recipe and shot sequence",
                    "Keep quantities, food-safety notes and demonstrable visual steps together.",
                    listOf(
                        "Which quantities and times must be correct for the viewer?",
                        "Which preparation or result needs a close-up?",
                        "What food-safety condition should be checked independently?",
                    ),
                    listOf("Check ingredient quantities", "Capture the key transformation and final result"),
                )
            (mode == "travel_outdoors" || workflowId.startsWith("v150_travel_")) &&
                id in setOf("trip_plan", "locations", "route", "shot_plan", "research", "capture") ->
                CW51StageModule(
                    "Location and route story",
                    "Link places, travel decisions and shots without assuming current accessibility.",
                    listOf(
                        "Which location best serves the story or guide decision?",
                        "What route, access, cost or permission requires fresh confirmation?",
                        "What establishing, detail or transition shot will orient the audience?",
                    ),
                    listOf("Verify route and access details", "List essential shots by location"),
                )
            (mode == "education" || workflowId.startsWith("v150_teach_")) &&
                id in setOf("learning_goal", "prerequisites", "outline", "example", "review", "visuals") ->
                CW51StageModule(
                    "Learning experience map",
                    "Design for what the audience should be able to do, not merely what is said.",
                    listOf(
                        "What can the learner do after this?",
                        "Which prerequisite or demonstration makes the lesson understandable?",
                        "How could a learner check their own understanding?",
                    ),
                    listOf("Document the learner outcome", "Verify the supporting example"),
                )
            (mode == "news_commentary" || workflowId.startsWith("v150_report_")) &&
                id in setOf("sources", "verify", "known_unknown", "context", "follow_up") ->
                CW51StageModule(
                    "Verified versus developing",
                    "Separate directly supported facts from interpretation and unresolved details.",
                    listOf(
                        "Which original source supports the central assertion?",
                        "What is known, contradicted or still developing?",
                        "What change would require an update or correction?",
                    ),
                    listOf("Link a primary source", "Label unresolved details before publication"),
                )
            id in setOf("shot_plan", "session_plan", "record", "capture", "visuals_capture", "process_capture") ->
                CW51StageModule(
                    "Capture plan",
                    "Turn the stage into a usable preparation and take log.",
                    listOf(
                        "What must be captured before this session is complete?",
                        "Which equipment, timing, consent or quality check remains?",
                    ),
                    listOf("List essential captures", "Confirm the usable takes"),
                )
            id in setOf("edit", "process_edit", "sound_grade", "mix", "refine") ->
                CW51StageModule(
                    "Version review",
                    "Document feedback and version references in the existing cut log.",
                    listOf(
                        "What changed in this version and why?",
                        "Which unresolved issue would prevent a satisfactory result?",
                    ),
                    listOf("Review current cut", "Verify final picture, sound or presentation"),
                )
            else -> null
        }
    }
}
