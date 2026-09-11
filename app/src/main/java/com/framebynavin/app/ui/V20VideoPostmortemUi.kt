package com.framebynavin.app.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.framebynavin.app.data.CreatorPostmortemTimingQuality
import com.framebynavin.app.data.CreatorTask
import com.framebynavin.app.data.CreatorVideoPostmortem
import com.framebynavin.app.data.CreatorVideoPostmortemEngine
import com.framebynavin.app.data.CreatorWorkflowTimelineStore
import com.framebynavin.app.ui.theme.*
import com.framebynavin.app.youtube.YouTubeAnalyticsStore
import com.framebynavin.app.youtube.YouTubeInsightsFoundationStore
import com.framebynavin.app.youtube.YouTubePublishCheckpointPolicy
import com.framebynavin.app.youtube.YouTubePublishCheckpointStore
import com.framebynavin.app.youtube.YouTubeReachStore
import com.framebynavin.app.youtube.YouTubeVideoSnapshot
import java.util.Locale

@Composable
internal fun V20VideoPostmortemCard(
    task: CreatorTask,
    video: YouTubeVideoSnapshot,
    windowDays: Int,
) {
    val context = LocalContext.current.applicationContext
    val postmortem = remember(task, video, windowDays) {
        val analytics = YouTubeAnalyticsStore(context).load(windowDays)
        val foundation = analytics?.let {
            YouTubeInsightsFoundationStore(context).load(windowDays, it.channel.channelId)
        }
        val retention = foundation?.retention?.firstOrNull { it.videoId == video.videoId }
        val reach = analytics?.let { base ->
            YouTubeReachStore(context)
                .summary(base.startDate, base.endDate)
                ?.videos
                ?.firstOrNull { it.videoId == video.videoId }
        }
        CreatorVideoPostmortemEngine.build(
            task = task,
            video = video,
            timeline = CreatorWorkflowTimelineStore(context).load(task.id),
            retention = retention,
            reach = reach,
            checkpoints = YouTubePublishCheckpointStore(context).load(video.videoId),
        )
    }

    Surface(
        Modifier.fillMaxWidth(),
        RoundedCornerShape(16.dp),
        Color(0xFF171719),
        border = BorderStroke(1.dp, MutedGold.copy(alpha = .30f)),
    ) {
        Column(Modifier.padding(13.dp)) {
            Text("CREATOR POSTMORTEM", color = MutedGold, fontSize = 7.7.sp, fontWeight = FontWeight.Black, letterSpacing = .9.sp)
            Spacer(Modifier.height(3.dp))
            Text("Creation → publish → audience response", color = ProjectorIvory, fontSize = 11.sp, fontWeight = FontWeight.Black)
            Text(
                "${postmortem.evidence.capturedCount}/${postmortem.evidence.totalCount} evidence layers connected · missing data stays missing",
                color = MutedText,
                fontSize = 7.8.sp,
            )

            Spacer(Modifier.height(10.dp))
            PostmortemSection("CONTENT DNA", dnaLine(postmortem))

            val creation = creationLine(postmortem)
            if (creation.isNotBlank()) {
                Spacer(Modifier.height(8.dp))
                PostmortemSection("CREATION", creation)
            }

            Spacer(Modifier.height(8.dp))
            PostmortemSection("PROCESS", processLine(postmortem))

            Spacer(Modifier.height(8.dp))
            PostmortemSection("PACKAGING", packagingLine(postmortem))

            Spacer(Modifier.height(8.dp))
            PostmortemSection("RESULT", resultLine(postmortem))

            Spacer(Modifier.height(8.dp))
            PostmortemSection(
                "CREATOR LEARNING",
                postmortem.creatorLearning.ifBlank { "No learning note captured yet. This stays blank rather than being guessed." },
                muted = postmortem.creatorLearning.isBlank(),
            )

            Spacer(Modifier.height(8.dp))
            Text(
                "Project/stage duration is elapsed residence time, not active editing or research time.",
                color = MutedText.copy(alpha = .72f),
                fontSize = 7.2.sp,
                lineHeight = 10.sp,
            )
        }
    }
}

@Composable
private fun PostmortemSection(label: String, value: String, muted: Boolean = false) {
    Text(label, color = MutedText, fontSize = 6.8.sp, fontWeight = FontWeight.Black, letterSpacing = .55.sp)
    Spacer(Modifier.height(2.dp))
    Text(
        value,
        color = if (muted) MutedText else ProjectorIvory,
        fontSize = 8.5.sp,
        lineHeight = 12.sp,
        maxLines = 5,
        overflow = TextOverflow.Ellipsis,
    )
}

private fun dnaLine(value: CreatorVideoPostmortem): String {
    val creation = value.creation
    val parts = buildList {
        creation.creatorModeId.takeIf { it.isNotBlank() }?.let { add(prettyKey(it)) }
        creation.archetypeId.takeIf { it.isNotBlank() }?.let { add(prettyKey(it)) }
        if (creation.productionStyles.isNotEmpty()) add(creation.productionStyles.joinToString(" + ") { prettyKey(it) })
        creation.platform.takeIf { it.isNotBlank() }?.let { add(it) }
        creation.deliveryFormat.takeIf { it.isNotBlank() }?.let { add(it) }
    }
    return parts.joinToString(" · ").ifBlank { "Legacy project identity only" }
}

private fun creationLine(value: CreatorVideoPostmortem): String {
    val c = value.creation
    return when {
        c.hook.isNotBlank() -> "Hook: ${c.hook} · ${scriptEvidence(c.scriptPresent, c.scriptCharacterCount, c.referenceCount)}"
        c.angle.isNotBlank() -> "Angle: ${c.angle} · ${scriptEvidence(c.scriptPresent, c.scriptCharacterCount, c.referenceCount)}"
        c.promise.isNotBlank() -> "Promise: ${c.promise} · ${scriptEvidence(c.scriptPresent, c.scriptCharacterCount, c.referenceCount)}"
        c.scriptPresent || c.referenceCount > 0 -> scriptEvidence(c.scriptPresent, c.scriptCharacterCount, c.referenceCount)
        else -> ""
    }
}

private fun scriptEvidence(script: Boolean, chars: Int, refs: Int): String = buildList {
    if (script) add("script captured${if (chars > 0) " (${chars} chars)" else ""}")
    if (refs > 0) add("$refs reference${if (refs == 1) "" else "s"}")
}.joinToString(" · ").ifBlank { "no script/reference evidence" }

private fun processLine(value: CreatorVideoPostmortem): String {
    val timing = when (value.timingQuality) {
        CreatorPostmortemTimingQuality.EXACT_FROM_PROJECT_CREATION -> "Project → publish ${postmortemDuration(value.projectToPublishMillis)}"
        CreatorPostmortemTimingQuality.LOWER_BOUND_FROM_FIRST_OBSERVATION -> "Observed project → publish ≥ ${postmortemDuration(value.projectToPublishMillis)}"
        CreatorPostmortemTimingQuality.UNAVAILABLE -> "Project-to-publish timing unavailable"
    }
    val longest = value.stageSpans.maxByOrNull { it.residenceMillis }
    return if (longest == null) "$timing · stage history still learning"
    else "$timing · longest captured stage: ${longest.stageLabel} ${if (longest.lowerBound) "≥ " else ""}${postmortemDuration(longest.residenceMillis)}"
}

private fun packagingLine(value: CreatorVideoPostmortem): String {
    val p = value.packaging
    val details = buildList {
        add(p.titleSnapshot.ifBlank { "Title snapshot unavailable" })
        add(if (p.thumbnailSnapshot.isNotBlank()) "thumbnail captured" else "thumbnail concept unavailable")
        if (p.publicationRevisionCount > 0) add("${p.publicationRevisionCount} publication event${if (p.publicationRevisionCount == 1) "" else "s"}")
        if (p.descriptionPresent) add("description")
        if (p.tagsPresent) add("tags")
    }
    return details.joinToString(" · ")
}

private fun resultLine(value: CreatorVideoPostmortem): String {
    val p = value.performance
    val details = buildList {
        p.impressions?.let { add("${compactPostmortem(it)} impressions") }
        p.ctrPercent?.let { add(String.format(Locale.US, "%.2f%% CTR", it)) }
        p.retentionAt50Percent?.let { add(String.format(Locale.US, "%.0f%% watching near midpoint", it * 100.0)) }
        if (value.checkpoints.isNotEmpty()) {
            add(value.checkpoints.joinToString(prefix = "release curve ", separator = " → ") { YouTubePublishCheckpointPolicy.label(it.targetMinutes) })
        }
    }
    return details.joinToString(" · ").ifBlank {
        "Views/watch/subscriber results are connected above; reach, retention and release-curve evidence are not available yet."
    }
}

private fun prettyKey(raw: String): String = raw
    .replace('-', ' ')
    .replace('_', ' ')
    .trim()
    .split(Regex("\\s+"))
    .joinToString(" ") { word -> word.lowercase().replaceFirstChar { it.uppercase() } }

private fun postmortemDuration(millis: Long): String {
    if (millis <= 0L) return "unavailable"
    val minutes = millis / 60_000L
    val days = minutes / 1_440L
    val hours = (minutes % 1_440L) / 60L
    val mins = minutes % 60L
    return when {
        days > 0 -> "${days}d${if (hours > 0) " ${hours}h" else ""}"
        hours > 0 -> "${hours}h${if (mins > 0) " ${mins}m" else ""}"
        else -> "${mins}m"
    }
}

private fun compactPostmortem(value: Long): String = when {
    value >= 1_000_000L -> String.format(Locale.US, "%.1fM", value / 1_000_000.0)
    value >= 1_000L -> String.format(Locale.US, "%.1fK", value / 1_000.0)
    else -> value.toString()
}
