package com.framebynavin.app.data

data class WorkflowStage(
    val id: String,
    val label: String,
    val action: String,
)

data class WorkflowTemplate(
    val id: String,
    val label: String,
    val stages: List<WorkflowStage>,
)

object CreatorWorkflowEngine {
    fun templateFor(task: CreatorTask): WorkflowTemplate = templateFor(task.platform, task.contentType)

    fun templateFor(platform: String, contentType: String): WorkflowTemplate {
        val p = platform.trim().lowercase()
        val type = contentType.trim().lowercase()

        return when {
            p == "youtube" && type == "long-form" -> WorkflowTemplate(
                id = "youtube_longform",
                label = "YouTube Long-form",
                stages = listOf(
                    stage("idea", "Idea", "Lock the angle and core promise"),
                    stage("research", "Research", "Collect references, scenes and evidence"),
                    stage("script", "Script", "Write and tighten the narration"),
                    stage("voice", "Voice", "Record the final narration"),
                    stage("edit", "Edit", "Build and finish the video edit"),
                    stage("thumbnail", "Thumbnail", "Create thumbnail and metadata"),
                    stage("upload", "Upload", "Upload, QC and schedule"),
                    stage("published", "Published", "Publish and verify the live video"),
                ),
            )

            p == "youtube" && type == "cinematic moment" -> WorkflowTemplate(
                id = "youtube_cinematic_moment",
                label = "Cinematic Moment",
                stages = listOf(
                    stage("select", "Select Clips", "Lock the strongest cinematic moments"),
                    stage("edit", "Edit", "Build the visual rhythm and sequence"),
                    stage("sound_grade", "Sound + Grade", "Polish sound, grade and transitions"),
                    stage("thumbnail", "Thumbnail", "Create thumbnail and final metadata"),
                    stage("upload", "Upload", "Upload, QC and schedule"),
                    stage("published", "Published", "Publish and verify the live video"),
                ),
            )

            p == "youtube" && type == "short" -> shortVideoTemplate("youtube_short", "YouTube Short")
            p == "instagram" && type == "reel" -> shortVideoTemplate("instagram_reel", "Instagram Reel")

            p == "instagram" && type == "post" -> WorkflowTemplate(
                id = "instagram_post",
                label = "Instagram Post",
                stages = listOf(
                    stage("idea", "Idea", "Lock the post idea and visual direction"),
                    stage("create", "Create", "Create the final visual or carousel"),
                    stage("caption", "Caption", "Write caption, tags and CTA"),
                    stage("review", "Review", "Check crop, spelling and final presentation"),
                    stage("published", "Published", "Publish and verify the post"),
                ),
            )

            p == "instagram" && type == "story" -> WorkflowTemplate(
                id = "instagram_story",
                label = "Instagram Story",
                stages = listOf(
                    stage("idea", "Idea", "Lock the story message"),
                    stage("create", "Create", "Create the story frame or clip"),
                    stage("review", "Review", "Check text, crop and links"),
                    stage("published", "Published", "Publish and verify the story"),
                ),
            )

            p == "x" && type == "post" -> WorkflowTemplate(
                id = "x_post",
                label = "X Post",
                stages = listOf(
                    stage("idea", "Idea", "Lock the thought or angle"),
                    stage("draft", "Draft", "Write the post clearly and tightly"),
                    stage("review", "Review", "Verify wording, names and context"),
                    stage("published", "Published", "Publish and verify the post"),
                ),
            )

            p == "x" && type == "video" -> WorkflowTemplate(
                id = "x_video",
                label = "X Video",
                stages = listOf(
                    stage("idea", "Idea", "Lock the video angle"),
                    stage("script", "Script", "Prepare the message or narration"),
                    stage("edit", "Edit", "Finish the video edit"),
                    stage("caption", "Caption", "Write the post copy and context"),
                    stage("upload", "Upload", "Upload and final-check playback"),
                    stage("published", "Published", "Publish and verify the post"),
                ),
            )

            p == "x" && type == "update" -> WorkflowTemplate(
                id = "x_update",
                label = "X Update",
                stages = listOf(
                    stage("draft", "Draft", "Write the update"),
                    stage("verify", "Verify", "Confirm the facts and wording"),
                    stage("published", "Published", "Publish and verify the update"),
                ),
            )

            else -> WorkflowTemplate(
                id = "creator_default",
                label = "$platform $contentType".trim(),
                stages = listOf(
                    stage("idea", "Idea", "Lock what you are making"),
                    stage("create", "Create", "Create the content"),
                    stage("review", "Review", "Review and finish the content"),
                    stage("published", "Published", "Publish and verify it"),
                ),
            )
        }
    }

    fun stageIndex(task: CreatorTask): Int {
        val stages = templateFor(task).stages
        if (stages.isEmpty()) return 0
        if (task.status == TaskStatus.DONE) return stages.lastIndex
        if (task.workflowStageIndex >= 0) return task.workflowStageIndex.coerceIn(0, stages.lastIndex)
        return stageIndexFromProgress(task.progress, stages.size)
    }

    fun currentStage(task: CreatorTask): WorkflowStage =
        templateFor(task).stages[stageIndex(task)]

    fun nextStage(task: CreatorTask): WorkflowStage? {
        val template = templateFor(task)
        val index = stageIndex(task)
        return template.stages.getOrNull(index + 1)
    }

    fun progressForStage(index: Int, stageCount: Int): Int {
        if (stageCount <= 1) return if (index > 0) 100 else 0
        return ((index.coerceIn(0, stageCount) * 100f) / stageCount.toFloat()).toInt().coerceIn(0, 99)
    }

    fun progress(task: CreatorTask): Int {
        if (task.status == TaskStatus.DONE) return 100
        val template = templateFor(task)
        return progressForStage(stageIndex(task), template.stages.size)
    }

    fun stageIndexFromProgress(progress: Int, stageCount: Int): Int {
        if (stageCount <= 1) return 0
        if (progress >= 100) return stageCount - 1
        return ((progress.coerceIn(0, 99) / 100f) * stageCount)
            .toInt()
            .coerceIn(0, stageCount - 1)
    }

    fun completedStageCount(task: CreatorTask): Int =
        if (task.status == TaskStatus.DONE) templateFor(task).stages.size else stageIndex(task)

    fun nextAction(task: CreatorTask): String =
        if (task.status == TaskStatus.DONE) "Published and complete"
        else currentStage(task).action

    private fun shortVideoTemplate(id: String, label: String) = WorkflowTemplate(
        id = id,
        label = label,
        stages = listOf(
            stage("idea", "Idea", "Lock the hook and core idea"),
            stage("script", "Script", "Write the short script or beats"),
            stage("voice", "Voice", "Record voice or final dialogue"),
            stage("edit", "Edit", "Finish the vertical edit"),
            stage("cover", "Cover", "Create cover, caption and metadata"),
            stage("upload", "Upload", "Upload, QC and schedule"),
            stage("published", "Published", "Publish and verify the live post"),
        ),
    )

    private fun stage(id: String, label: String, action: String) = WorkflowStage(id, label, action)
}
