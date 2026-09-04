package com.framebynavin.app.data

data class PostPublishFollowUpSpec(
    val key: String,
    val title: String,
    val platform: String,
    val contentType: String,
    val dueOffsetMinutes: Long,
    val priority: TaskPriority,
)

object CreatorPostPublishEngine {
    fun specs(parent: CreatorTask): List<PostPublishFollowUpSpec> {
        if (parent.sourceRefId.startsWith("post-publish:")) return emptyList()
        return when (parent.platform.trim().lowercase()) {
            "youtube" -> listOf(
                PostPublishFollowUpSpec(
                    key = "cross-promote",
                    title = "Cross-promote · ${parent.title}",
                    platform = "X",
                    contentType = "Post",
                    dueOffsetMinutes = 30,
                    priority = TaskPriority.IMPORTANT,
                ),
                PostPublishFollowUpSpec(
                    key = "24h-review",
                    title = "24h performance check · ${parent.title}",
                    platform = "YouTube",
                    contentType = "Update",
                    dueOffsetMinutes = 24 * 60,
                    priority = TaskPriority.IMPORTANT,
                ),
                PostPublishFollowUpSpec(
                    key = "7d-review",
                    title = "7d performance review · ${parent.title}",
                    platform = "YouTube",
                    contentType = "Update",
                    dueOffsetMinutes = 7 * 24 * 60,
                    priority = TaskPriority.NORMAL,
                ),
            )
            "instagram" -> listOf(
                PostPublishFollowUpSpec(
                    key = "24h-review",
                    title = "24h performance check · ${parent.title}",
                    platform = "Instagram",
                    contentType = "Post",
                    dueOffsetMinutes = 24 * 60,
                    priority = TaskPriority.NORMAL,
                ),
            )
            else -> emptyList()
        }
    }

    fun sourceRef(parentId: String, key: String): String = "post-publish:$parentId:$key"
}
