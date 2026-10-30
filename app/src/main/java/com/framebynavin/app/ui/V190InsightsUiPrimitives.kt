package com.framebynavin.app.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.ExpandLess
import androidx.compose.material.icons.outlined.ExpandMore
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.framebynavin.app.ui.theme.CinemaLine
import com.framebynavin.app.ui.theme.CinemaSurface
import com.framebynavin.app.ui.theme.MutedText
import com.framebynavin.app.ui.theme.ProjectorIvory

/**
 * V190 UI contract: visible titles and action labels only, with concise
 * accuracy, permission, error, and loading states when essential.
 * Keep advanced feature access behind explicit disclosure, not hidden forever.
 */
@Composable
internal fun V190Disclosure(title: String, expanded: Boolean, onToggle: () -> Unit) {
    Surface(
        modifier = Modifier.fillMaxWidth().testTag("v190-disclosure-${title.lowercase().replace(Regex("[^a-z]+"), "-")}"),
        onClick = onToggle,
        shape = RoundedCornerShape(14.dp),
        color = CinemaSurface,
        border = BorderStroke(1.dp, CinemaLine),
    ) {
        Row(Modifier.padding(horizontal = 14.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically) {
            Text(title, color = ProjectorIvory, fontSize = 13.sp,
                fontWeight = FontWeight.Medium, modifier = Modifier.weight(1f))
            Icon(if (expanded) Icons.Outlined.ExpandLess else Icons.Outlined.ExpandMore,
                contentDescription = if (expanded) "Collapse $title" else "Expand $title",
                tint = MutedText, modifier = Modifier.size(18.dp))
        }
    }
}
