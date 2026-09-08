package com.framebynavin.app.youtube

import com.framebynavin.app.data.CreatorTask
import java.util.Locale

/**
 * Creator-neutral YouTube format classification.
 *
 * Structured project metadata wins whenever it is meaningful. Title inference is only a fallback
 * for generic project types such as "Video" so the analytics layer does not depend on one niche,
 * channel name, language, or creator-specific naming convention.
 */
object YouTubeContentClassifier {
    fun label(task: CreatorTask): String = label(task.title, task.contentType)

    fun label(title: String, contentType: String): String {
        val rawType = contentType.trim()
        val type = rawType.lowercase(Locale.ROOT)

        when {
            type.contains("short") || type.contains("reel") -> return "Short-form"
            type.contains("long-form") || type.contains("long form") -> return "Long-form"
            type.contains("live") || type.contains("stream") -> return "Live"
            type.contains("podcast") -> return "Podcast"
            type.contains("interview") -> return "Interview"
            rawType.isNotBlank() && type !in GENERIC_TYPES -> return rawType
        }

        val normalizedTitle = " " + title
            .lowercase(Locale.ROOT)
            .replace(Regex("[^\\p{L}\\p{M}\\p{N}#]+"), " ")
            .trim() + " "

        return when {
            " #shorts " in normalizedTitle || " short " in normalizedTitle -> "Short-form"
            " live " in normalizedTitle || " stream " in normalizedTitle -> "Live"
            " podcast " in normalizedTitle -> "Podcast"
            " interview " in normalizedTitle -> "Interview"
            " tutorial " in normalizedTitle || " how to " in normalizedTitle -> "Tutorial / How-to"
            " review " in normalizedTitle -> "Review"
            " breakdown " in normalizedTitle || " analysis " in normalizedTitle || " explained " in normalizedTitle -> "Analysis"
            " recommend " in normalizedTitle || " recommendation " in normalizedTitle -> "Recommendation"
            " vlog " in normalizedTitle -> "Vlog"
            rawType.isNotBlank() -> rawType
            else -> "Video"
        }
    }

    private val GENERIC_TYPES = setOf(
        "video",
        "youtube video",
        "youtube",
        "content",
        "production",
        "other",
    )
}
