package com.framebynavin.app.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.framebynavin.app.ui.theme.BacklotAccent
import com.framebynavin.app.ui.theme.BacklotBorder
import com.framebynavin.app.ui.theme.BacklotPrimaryText
import com.framebynavin.app.ui.theme.BacklotSecondaryText
import com.framebynavin.app.ui.theme.BacklotSurface
import com.framebynavin.app.ui.theme.BacklotSurfaceSelected

/**
 * Shared presentation-only chrome for Personal and Creator reminders.
 *
 * DIRECTOR'S CUT is Backlot's primary appearance. Semantic theme tokens keep
 * REC-red selection and neutral typography consistent, while retaining the
 * supported Light appearance; no amber/legacy MutedGold references here.
 *
 * No storage, reminder, alarm, navigation state or business logic is owned by
 * this file. The existing screen provides all callbacks and test tags.
 */
internal enum class BacklotReminderWorkspace { PERSONAL, CREATOR }

@Composable
internal fun BacklotReminderHubHeading(
    title: String,
    modifier: Modifier = Modifier,
) {
    Column(modifier, verticalArrangement = Arrangement.spacedBy(3.dp)) {
        Text(
            "BACKLOT / REMINDERS",
            color = BacklotAccent,
            fontSize = 10.sp,
            fontWeight = FontWeight.Black,
            letterSpacing = 1.sp,
            maxLines = 1,
        )
        Text(
            title,
            color = BacklotPrimaryText,
            fontSize = 20.sp,
            lineHeight = 24.sp,
            fontWeight = FontWeight.Black,
            maxLines = 1,
        )
    }
}

@Composable
internal fun BacklotReminderHubWorkspaceTabs(
    selected: BacklotReminderWorkspace,
    onOpenPersonal: () -> Unit,
    onOpenCreator: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(9.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        BacklotReminderHubTab(
            title = "Personal",
            selected = selected == BacklotReminderWorkspace.PERSONAL,
            onClick = onOpenPersonal,
            tag = "v2232-open-personal",
            modifier = Modifier.weight(1f),
        )
        BacklotReminderHubTab(
            title = "Creator",
            selected = selected == BacklotReminderWorkspace.CREATOR,
            onClick = onOpenCreator,
            tag = "v2232-open-creator",
            modifier = Modifier.weight(1f),
        )
    }
}

@Composable
private fun BacklotReminderHubTab(
    title: String,
    selected: Boolean,
    onClick: () -> Unit,
    tag: String,
    modifier: Modifier,
) {
    val colors = BacklotHubTabPalette(
        surface = BacklotSurface,
        selectedSurface = BacklotSurfaceSelected,
        border = BacklotBorder,
        accent = BacklotAccent,
        foreground = BacklotPrimaryText,
        secondary = BacklotSecondaryText,
    )
    val shape = RoundedCornerShape(13.dp)
    val content: @Composable () -> Unit = {
        Row(
            Modifier.fillMaxWidth().height(43.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center,
        ) {
            if (selected) {
                Surface(color = colors.accent, shape = CircleShape,
                    modifier = Modifier.size(6.dp)) {}
                Spacer(Modifier.size(8.dp))
            }
            Text(
                title,
                color = if (selected) colors.foreground else colors.secondary,
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold,
            )
        }
    }
    if (selected) {
        Surface(
            modifier = modifier,
            color = colors.selectedSurface,
            shape = shape,
            border = BorderStroke(1.dp, colors.accent),
            content = content,
        )
    } else {
        Surface(
            onClick = onClick,
            modifier = modifier.testTag(tag),
            color = colors.surface,
            shape = shape,
            border = BorderStroke(1.dp, colors.border),
            content = content,
        )
    }
}

internal data class BacklotHubTabPalette(
    val surface: Color,
    val selectedSurface: Color,
    val border: Color,
    val accent: Color,
    val foreground: Color,
    val secondary: Color,
)

/** FC13: presentation-only Director's Cut filters reused by both live reminder hubs. */
@Composable
internal fun BacklotReminderHubFilterChip(
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    FilterChip(
        selected = selected,
        onClick = onClick,
        label = { Text(label, fontSize = 11.sp, maxLines = 1) },
        modifier = modifier,
        shape = RoundedCornerShape(18.dp),
        colors = FilterChipDefaults.filterChipColors(
            containerColor = BacklotSurface,
            labelColor = BacklotSecondaryText,
            selectedContainerColor = BacklotSurfaceSelected,
            selectedLabelColor = BacklotPrimaryText,
        ),
        border = BorderStroke(1.dp, if (selected) BacklotAccent else BacklotBorder),
    )
}

/** Same section hierarchy, preserving each screen's independently computed count. */
@Composable
internal fun BacklotReminderHubSectionHeading(
    label: String,
    count: Int,
    urgent: Boolean = false,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Text(
            label,
            color = if (urgent) BacklotAccent else BacklotSecondaryText,
            fontSize = 12.sp,
            fontWeight = FontWeight.Black,
            modifier = Modifier.weight(1f),
        )
        Text(
            count.toString(),
            color = BacklotSecondaryText,
            fontSize = 12.sp,
            fontWeight = FontWeight.Bold,
        )
    }
}
