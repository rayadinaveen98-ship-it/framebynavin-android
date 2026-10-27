package com.framebynavin.app.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.Refresh
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.framebynavin.app.ui.theme.*

/** Presentation only: the existing Week screen owns all confirmation and schedule mutations. */
@Composable
internal fun V197WeeklyPlanActions(
    previewCount: Int,
    previewEnabled: Boolean,
    onPreview: () -> Unit,
    onAdd: () -> Unit,
) {
    val stacked = LocalConfiguration.current.screenWidthDp < 390 ||
        LocalDensity.current.fontScale >= 1.35f
    if (stacked) {
        Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            V197PreviewWeeklyButton(previewCount, previewEnabled, onPreview, Modifier.fillMaxWidth())
            V197AddWeeklyButton(onAdd, Modifier.fillMaxWidth())
        }
    } else {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            V197PreviewWeeklyButton(previewCount, previewEnabled, onPreview, Modifier.weight(1f))
            V197AddWeeklyButton(onAdd, Modifier.weight(1f))
        }
    }
}

@Composable
private fun V197PreviewWeeklyButton(count: Int, enabled: Boolean, onClick: () -> Unit, modifier: Modifier) {
    Button(
        onClick = onClick,
        enabled = enabled,
        modifier = modifier.heightIn(min = 48.dp).testTag("v164-create-preview"),
        colors = ButtonDefaults.buttonColors(containerColor = RecRed),
        shape = RoundedCornerShape(14.dp),
    ) {
        Icon(Icons.Outlined.Refresh, null, modifier = Modifier.size(17.dp))
        Spacer(Modifier.width(6.dp))
        Text("PREVIEW $count PROJECTS", fontSize = 10.sp, fontWeight = FontWeight.Bold)
    }
}

@Composable
private fun V197AddWeeklyButton(onClick: () -> Unit, modifier: Modifier) {
    OutlinedButton(
        onClick = onClick,
        modifier = modifier.heightIn(min = 48.dp).testTag("v197-add-weekly-post"),
        border = BorderStroke(1.dp, CinemaLine),
        shape = RoundedCornerShape(14.dp),
    ) {
        Icon(Icons.Outlined.Add, null, modifier = Modifier.size(17.dp))
        Spacer(Modifier.width(6.dp))
        Text("ADD WEEKLY POST", color = ProjectorIvory, fontSize = 9.5.sp, fontWeight = FontWeight.Bold)
    }
}
