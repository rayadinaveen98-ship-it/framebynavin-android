package com.framebynavin.app.ui

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.layout.size
import com.framebynavin.app.reminders.FC13SparkNotificationMood

/**
 * Same original approved Spark art in small cards, a medium alert and a full
 * reminder screen. Android's native monochrome status icon is separate.
 *
 * Spark remains the ONLY selectable new companion until the other character
 * pose packs are approved. These five states are existing, real WebP masters:
 * point, present, welcome, thinking and celebrate.
 */
internal enum class FC13SparkPoseSize(val width: Dp, val height: Dp) {
    COMPACT(42.dp, 50.dp),
    MEDIUM(74.dp, 82.dp),
    FULL(96.dp, 104.dp),
}

internal fun FC13SparkNotificationMood.guideState(): CinePulseState = when (this) {
    FC13SparkNotificationMood.ALERT -> CinePulseState.POINT
    FC13SparkNotificationMood.SPEAK -> CinePulseState.PRESENT
    FC13SparkNotificationMood.CALM -> CinePulseState.IDLE
    FC13SparkNotificationMood.THINK -> CinePulseState.THINK
    FC13SparkNotificationMood.SUCCESS -> CinePulseState.CELEBRATE
}

@Composable
internal fun FC13SparkPose(
    mood: FC13SparkNotificationMood,
    size: FC13SparkPoseSize,
    modifier: Modifier = Modifier,
    pointRight: Boolean = true,
) {
    V144GuideCharacter(
        guide = V144GuideIdentity.FUNNY,
        state = mood.guideState(),
        pointRight = pointRight,
        modifier = modifier.size(size.width, size.height)
            .testTag("fc13-spark-${size.name.lowercase()}-${mood.name.lowercase()}"),
    )
}
