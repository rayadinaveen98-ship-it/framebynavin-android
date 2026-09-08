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
                    stage("voice", "Record", "Record the final narration"),
                    stage("edit", "Edit", "Build and finish the video edit"),
                    stage("thumbnail", "Thumbnail", "Create thumbnail and metadata"),
                    stage("upload", "Upload", "Upload, QC and schedule"),
                    stage("published", "Publish", "Publish and verify the live video"),
                    stage("promote", "Promote", "Share the release across Shorts, Reels or X and capture the live link"),
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
                    stage("published", "Publish", "Publish and verify the live video"),
                    stage("promote", "Promote", "Share the compilation and capture the live link"),
                ),
            )

            p == "youtube" && type == "short" -> shortVideoTemplate("youtube_short", "YouTube Short")
            p == "youtube" && type == "video" -> videoTemplate("youtube_video", "YouTube Video")
            p == "instagram" && type == "reel" -> shortVideoTemplate("instagram_reel", "Instagram Reel")

            p == "instagram" && type == "post" -> WorkflowTemplate(
                id = "instagram_post",
                label = "Instagram Post",
                stages = listOf(
                    stage("idea", "Idea", "Lock the post idea and visual direction"),
                    stage("create", "Create", "Create the final visual or carousel"),
                    stage("caption", "Caption", "Write caption, tags and CTA"),
                    stage("review", "Review", "Check crop, spelling and final presentation"),
                    stage("published", "Publish", "Publish and verify the post"),
                    stage("promote", "Promote", "Share to Story or cross-post where useful"),
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
                    stage("published", "Publish", "Publish and verify the post"),
                    stage("promote", "Promote", "Reuse or link the video where it helps the main release"),
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

            p == "facebook" && type == "reel" -> shortVideoTemplate("facebook_reel", "Facebook Reel")
            p == "facebook" && type == "video" -> videoTemplate("facebook_video", "Facebook Video")
            p == "facebook" && type == "post" -> socialPostTemplate("facebook_post", "Facebook Post", includePromote = true)

            p == "linkedin" && type == "post" -> WorkflowTemplate(
                id = "linkedin_post",
                label = "LinkedIn Post",
                stages = listOf(
                    stage("angle", "Angle", "Lock the useful idea and audience takeaway"),
                    stage("draft", "Draft", "Write the post with a clear opening and structure"),
                    stage("proof", "Proof", "Check claims, links and wording"),
                    stage("published", "Publish", "Publish and verify the post"),
                    stage("engage", "Engage", "Reply to useful comments and capture learning"),
                ),
            )
            p == "linkedin" && type == "article" -> articleTemplate("linkedin_article", "LinkedIn Article")
            p == "linkedin" && type == "video" -> videoTemplate("linkedin_video", "LinkedIn Video")

            p == "podcast" && type == "episode" -> WorkflowTemplate(
                id = "podcast_episode",
                label = "Podcast Episode",
                stages = listOf(
                    stage("idea", "Idea", "Lock the episode promise and audience"),
                    stage("research", "Research", "Collect references, guests or talking points"),
                    stage("outline", "Outline", "Build the episode flow"),
                    stage("record", "Record", "Record the final episode"),
                    stage("edit", "Edit", "Edit audio and complete quality control"),
                    stage("metadata", "Metadata", "Prepare title, description and artwork"),
                    stage("published", "Publish", "Publish and verify the episode"),
                    stage("promote", "Promote", "Share the episode and useful clips"),
                ),
            )
            p == "podcast" && type == "clip" -> WorkflowTemplate(
                id = "podcast_clip",
                label = "Podcast Clip",
                stages = listOf(
                    stage("select", "Select", "Choose the strongest moment"),
                    stage("edit", "Edit", "Finish the clip and captions"),
                    stage("copy", "Copy", "Write the supporting post copy"),
                    stage("published", "Publish", "Publish and verify the clip"),
                ),
            )

            p == "blog / newsletter" && type == "article" -> articleTemplate("creator_article", "Article")
            p == "blog / newsletter" && type == "newsletter" -> WorkflowTemplate(
                id = "creator_newsletter",
                label = "Newsletter",
                stages = listOf(
                    stage("idea", "Idea", "Lock the main promise for this edition"),
                    stage("outline", "Outline", "Structure the edition and supporting sections"),
                    stage("draft", "Draft", "Write the full newsletter"),
                    stage("edit", "Edit", "Tighten writing, links and formatting"),
                    stage("send", "Send", "Send or schedule the newsletter"),
                    stage("review", "Review", "Review response and capture what to repeat"),
                ),
            )

            else -> WorkflowTemplate(
                id = "creator_default",
                label = "$platform $contentType".trim(),
                stages = listOf(
                    stage("idea", "Idea", "Lock what you are making"),
                    stage("create", "Create", "Create the content"),
                    stage("review", "Review", "Review and finish the content"),
                    stage("published", "Publish", "Publish and verify it"),
                    stage("promote", "Promote", "Share it where it supports your creator plan"),
                ),
            )
        }
    }

    /** Send is a publication event only when the creator confirms it was actually sent. */
    fun isPublicationStage(stage: WorkflowStage): Boolean = stage.id == "published" || stage.id == "send"

    fun hasPublicationStage(task: CreatorTask): Boolean = templateFor(task).stages.any(::isPublicationStage)

    fun publicationStageIndex(task: CreatorTask): Int = templateFor(task).stages.indexOfFirst(::isPublicationStage)

    fun stageActionLabel(task: CreatorTask): String {
        if (task.status == TaskStatus.DONE) return "PROJECT COMPLETE"
        val stage = currentStage(task)
        return when {
            isPublicationStage(stage) && task.publishedAtMillis <= 0L -> "MARK PUBLISHED"
            isPublicationStage(stage) -> "COMPLETE STEP"
            stageIndex(task) == templateFor(task).stages.lastIndex -> "FINISH PROJECT"
            else -> "COMPLETE STEP"
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
        if (task.status == TaskStatus.DONE) "Project complete"
        else currentStage(task).action

    private fun socialPostTemplate(id: String, label: String, includePromote: Boolean) = WorkflowTemplate(
        id = id,
        label = label,
        stages = buildList {
            add(stage("idea", "Idea", "Lock the post idea and audience takeaway"))
            add(stage("create", "Create", "Create the final post or visual"))
            add(stage("caption", "Caption", "Write the copy, context and call to action"))
            add(stage("review", "Review", "Check presentation, wording and links"))
            add(stage("published", "Publish", "Publish and verify the post"))
            if (includePromote) add(stage("promote", "Promote", "Share it where it supports the creator plan"))
        },
    )

    private fun videoTemplate(id: String, label: String) = WorkflowTemplate(
        id = id,
        label = label,
        stages = listOf(
            stage("idea", "Idea", "Lock the video angle and audience promise"),
            stage("script", "Script", "Prepare the script or talking points"),
            stage("record", "Record", "Record the final video or narration"),
            stage("edit", "Edit", "Finish the video edit"),
            stage("metadata", "Metadata", "Prepare title, caption and cover"),
            stage("published", "Publish", "Publish and verify the video"),
            stage("promote", "Promote", "Share the video where it supports the main goal"),
        ),
    )

    private fun articleTemplate(id: String, label: String) = WorkflowTemplate(
        id = id,
        label = label,
        stages = listOf(
            stage("idea", "Idea", "Lock the article promise and reader"),
            stage("research", "Research", "Collect evidence, references and examples"),
            stage("outline", "Outline", "Structure the argument or story"),
            stage("draft", "Draft", "Write the complete draft"),
            stage("edit", "Edit", "Tighten the writing and verify details"),
            stage("published", "Publish", "Publish and verify the article"),
            stage("promote", "Promote", "Share the article where it supports the creator plan"),
        ),
    )

    private fun shortVideoTemplate(id: String, label: String) = WorkflowTemplate(
        id = id,
        label = label,
        stages = listOf(
            stage("idea", "Idea", "Lock the hook and core idea"),
            stage("script", "Script", "Write the short script or beats"),
            stage("voice", "Record", "Record voice or final dialogue"),
            stage("edit", "Edit", "Finish the vertical edit"),
            stage("cover", "Cover", "Create cover, caption and metadata"),
            stage("upload", "Upload", "Upload, QC and schedule"),
            stage("published", "Publish", "Publish and verify the live post"),
            stage("promote", "Promote", "Cross-post or point viewers to the main creator goal"),
        ),
    )

    private fun stage(id: String, label: String, action: String) = WorkflowStage(id, label, action)
}
