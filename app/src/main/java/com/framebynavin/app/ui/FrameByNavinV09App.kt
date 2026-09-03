package com.framebynavin.app.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Bolt
import androidx.compose.material.icons.outlined.CalendarMonth
import androidx.compose.material.icons.outlined.Lightbulb
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.framebynavin.app.data.CreatorViewModel
import com.framebynavin.app.ui.theme.*

private enum class V09Overlay { NONE, WEEK, RELEASE, IDEAS }

@Composable
fun FrameByNavinV09App(vm: CreatorViewModel = viewModel()) {
    var overlay by rememberSaveable { mutableStateOf(V09Overlay.NONE) }

    Box(Modifier.fillMaxSize()) {
        FrameByNavinV07App(vm)

        if (overlay == V09Overlay.NONE) {
            Row(
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .navigationBarsPadding()
                    .padding(start = 18.dp, end = 88.dp, bottom = 100.dp),
                horizontalArrangement = Arrangement.spacedBy(7.dp),
            ) {
                V09DockButton(
                    label = "WEEK",
                    icon = Icons.Outlined.CalendarMonth,
                    container = MutedGold,
                    foreground = CinemaBlack,
                ) { overlay = V09Overlay.WEEK }
                V09DockButton(
                    label = "LIVE",
                    icon = Icons.Outlined.Bolt,
                    container = RecRed,
                    foreground = ProjectorIvory,
                ) { overlay = V09Overlay.RELEASE }
                V09DockButton(
                    label = "IDEAS",
                    icon = Icons.Outlined.Lightbulb,
                    container = CinemaSurfaceRaised,
                    foreground = MutedGold,
                    border = CinemaLine,
                ) { overlay = V09Overlay.IDEAS }
            }
        }

        when (overlay) {
            V09Overlay.NONE -> Unit
            V09Overlay.WEEK -> V08WeeklyScheduleScreen(
                slots = vm.weeklySlots,
                tasks = vm.tasks,
                onClose = { overlay = V09Overlay.NONE },
                onToggle = vm::setWeeklySlotEnabled,
                onSave = vm::saveWeeklySlot,
                onDelete = vm::deleteWeeklySlot,
                onRefresh = vm::refreshWeeklySchedule,
                onReset = vm::resetWeeklySchedule,
            )
            V09Overlay.RELEASE -> V09ReleaseDayScreen(
                onClose = { overlay = V09Overlay.NONE },
                onLaunch = vm::createReleaseBurst,
            )
            V09Overlay.IDEAS -> V09IdeaVaultScreen(
                ideas = vm.ideas,
                onClose = { overlay = V09Overlay.NONE },
                onSave = vm::saveIdea,
                onDelete = vm::deleteIdea,
                onArchive = vm::archiveIdea,
                onConvert = vm::convertIdeaToProject,
            )
        }
    }
}

@Composable
private fun V09DockButton(
    label: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    container: Color,
    foreground: Color,
    border: Color? = null,
    onClick: () -> Unit,
) {
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(15.dp),
        color = container,
        border = border?.let { BorderStroke(1.dp, it) },
        shadowElevation = 8.dp,
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(icon, label, tint = foreground, modifier = Modifier.size(16.dp))
            Spacer(Modifier.width(5.dp))
            Text(label, color = foreground, fontSize = 8.8.sp, fontWeight = FontWeight.Black)
        }
    }
}
