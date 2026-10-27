package com.framebynavin.app.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/** Quiet, non-blocking confirmation. Never displayed before write completion. */
@Composable
internal fun V203ReminderSavedChip(message: String, modifier: Modifier = Modifier) {
    Surface(
        modifier = modifier.semantics { liveRegion = LiveRegionMode.Polite },
        shape = RoundedCornerShape(18.dp),
        color = Color(0xF5221F35),
        shadowElevation = 7.dp,
        border = BorderStroke(1.dp, Color(0xFF7665AC).copy(alpha = .7f)),
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
            horizontalArrangement = Arrangement.spacedBy(9.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(Icons.Outlined.CheckCircle, contentDescription = null, tint = Color(0xFFB3F1D4))
            Text(message, color = Color(0xFFF8F5FF), fontSize = 13.sp,
                fontWeight = FontWeight.SemiBold, maxLines = 2)
        }
    }
}
