package com.framebynavin.app.ui

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.CalendarMonth
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.framebynavin.app.data.CreatorViewModel
import com.framebynavin.app.ui.theme.CinemaBlack
import com.framebynavin.app.ui.theme.MutedGold
import com.framebynavin.app.ui.theme.ProjectorIvory

@Composable
fun FrameByNavinV08App(vm: CreatorViewModel = viewModel()) {
    var showWeekly by rememberSaveable { mutableStateOf(false) }

    Box(Modifier.fillMaxSize()) {
        FrameByNavinV07App(vm)

        if (!showWeekly) {
            Surface(
                onClick = { showWeekly = true },
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .navigationBarsPadding()
                    .padding(start = 24.dp, bottom = 100.dp),
                shape = RoundedCornerShape(18.dp),
                color = MutedGold,
                shadowElevation = 10.dp,
            ) {
                androidx.compose.foundation.layout.Row(
                    modifier = Modifier.padding(horizontal = 13.dp, vertical = 11.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Icon(Icons.Outlined.CalendarMonth, "Weekly schedule", tint = CinemaBlack, modifier = Modifier.size(18.dp))
                    androidx.compose.foundation.layout.Spacer(Modifier.size(6.dp))
                    Text("WEEK", color = CinemaBlack, fontSize = 9.5.sp, fontWeight = FontWeight.Black)
                }
            }
        }

        if (showWeekly) {
            V08WeeklyScheduleScreen(
                slots = vm.weeklySlots,
                tasks = vm.tasks,
                onClose = { showWeekly = false },
                onToggle = vm::setWeeklySlotEnabled,
                onSave = vm::saveWeeklySlot,
                onDelete = vm::deleteWeeklySlot,
                onRefresh = vm::refreshWeeklySchedule,
                onReset = vm::resetWeeklySchedule,
            )
        }
    }
}
