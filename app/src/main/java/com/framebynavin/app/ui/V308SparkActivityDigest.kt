package com.framebynavin.app.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.ChevronRight
import androidx.compose.material.icons.outlined.NotificationsActive
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.framebynavin.app.ui.theme.*
import com.framebynavin.app.reminders.FC13SparkNotificationMood
import java.security.MessageDigest

/**
 * Spark reads V306's existing evidence-based feed. These are preview rows, not
 * stored push notifications and NEVER proof the user viewed a delivered alert.
 */
internal object V308SparkActivityPolicy {
    private const val LIMIT = 3
    /** Hashed, domain-scoped key: no reminder title or raw ID in Spark preferences. */
    fun dismissalId(item: V306ActivityItem): String {
        val digest = MessageDigest.getInstance("SHA-256")
            .digest(item.id.toByteArray(Charsets.UTF_8))
        return "spark-activity:" + digest.joinToString("") { "%02x".format(it) }
    }

    fun top(
        items: List<V306ActivityItem>,
        suppressed: Set<String> = emptySet(),
    ): List<V306ActivityItem> {
        val filtered = items.filterNot { dismissalId(it) in suppressed }
        val actions = filtered.filter { it.bucket == V306ActivityBucket.NEEDS_ACTION }
        // Real actionable events are more important than historical confirmation.
        // When there is no outstanding action, surface at most two recent receipts.
        return (if (actions.isNotEmpty()) actions.take(LIMIT)
            else filtered.filter { it.bucket == V306ActivityBucket.HISTORY }.take(2))
            .distinctBy { it.id }
    }

    fun displayedTitle(item: V306ActivityItem): String =
        if (item.domain == V306ActivityDomain.PERSONAL)
            if (item.bucket == V306ActivityBucket.NEEDS_ACTION)
                "Personal reminder needs attention"
            else "Personal activity confirmed"
        else item.title

    fun displayedDetail(item: V306ActivityItem): String =
        if (item.domain == V306ActivityDomain.PERSONAL)
            if (item.bucket == V306ActivityBucket.NEEDS_ACTION)
                "Open Personal to review"
            else "Completed activity saved privately"
        else item.detail
}

@Composable
internal fun V308SparkActivityDigest(
    feed: List<V306ActivityItem>,
    onOpenActivity: () -> Unit,
    onOpenCreator: (String) -> Unit,
    onOpenPersonal: () -> Unit,
    dismissed: Set<String> = emptySet(),
    onDismiss: ((V306ActivityItem) -> Unit)? = null,
) {
    val preview = V308SparkActivityPolicy.top(feed, dismissed)
    if (preview.isEmpty()) return
    val actionCount = feed.count { it.bucket == V306ActivityBucket.NEEDS_ACTION }
    Surface(
        modifier = Modifier.fillMaxWidth().testTag("v308-spark-activity"),
        color = CinemaSurface,
        shape = RoundedCornerShape(18.dp),
        border = BorderStroke(1.dp, CinemaLine),
    ) {
        Column(Modifier.padding(13.dp), verticalArrangement = Arrangement.spacedBy(7.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(9.dp)) {
                FC13SparkPose(
                    mood = if (actionCount > 0)
                        FC13SparkNotificationMood.THINK
                    else FC13SparkNotificationMood.SUCCESS,
                    size = FC13SparkPoseSize.COMPACT,
                    modifier = Modifier.testTag("fc13-spark-digest-expression"),
                )
                Column(Modifier.weight(1f)) {
                    Text("YOUR ACTIVITY", color = ProjectorIvory,
                        fontSize = 13.sp, fontWeight = FontWeight.Bold)
                    Text(
                        if (actionCount > 0) "$actionCount items need attention"
                        else "Recent confirmed activity",
                        color = MutedText, fontSize = 11.sp,
                    )
                }
            }
            preview.forEach { item ->
                Surface(
                    onClick = {
                        if (item.bucket == V306ActivityBucket.HISTORY) onOpenActivity()
                        else if (item.domain == V306ActivityDomain.CREATOR)
                            onOpenCreator(item.targetId)
                        else onOpenPersonal()
                    },
                    modifier = Modifier.fillMaxWidth()
                        .testTag("v308-spark-activity-row-${item.domain.name.lowercase()}"),
                    color = CinemaSurfaceRaised,
                    shape = RoundedCornerShape(11.dp),
                ) {
                    Row(Modifier.padding(11.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        Column(Modifier.weight(1f),
                            verticalArrangement = Arrangement.spacedBy(3.dp)) {
                            Text(V308SparkActivityPolicy.displayedTitle(item),
                                color = ProjectorIvory, fontSize = 12.sp,
                                fontWeight = FontWeight.SemiBold,
                                maxLines = 1, overflow = TextOverflow.Ellipsis)
                            Text(V308SparkActivityPolicy.displayedDetail(item),
                                color = MutedText, fontSize = 11.sp,
                                maxLines = 2, overflow = TextOverflow.Ellipsis)
                        }
                        if (onDismiss != null) {
                            IconButton(
                                onClick = { onDismiss(item) },
                                modifier = Modifier.size(40.dp)
                                    .testTag("v308-spark-dismiss"),
                            ) {
                                Icon(Icons.Outlined.Close, "Hide this Spark update",
                                    tint = MutedText)
                            }
                        } else {
                            Icon(Icons.Outlined.ChevronRight, null, tint = RecRed)
                        }
                    }
                }
            }
            TextButton(
                onClick = onOpenActivity,
                modifier = Modifier.fillMaxWidth().testTag("v308-spark-see-all"),
            ) { Text("SEE ALL ACTIVITY", color = RecRed, fontSize = 11.sp) }
        }
    }
}
