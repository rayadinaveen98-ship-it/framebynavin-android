package com.framebynavin.app.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.framebynavin.app.ui.theme.*

/**
 * Activity-owned Content Workspace viewport.
 *
 * Edge-to-edge is enabled by ContentWorkspaceActivity. Consume safe drawing insets once at
 * the actual Activity content root (not in a separate full-screen Dialog window). IME padding
 * consumes only the remaining keyboard inset after safeDrawing, so footers move above the
 * keyboard rather than becoming obscured by the navigation/gesture area.
 *
 * All Workspace destinations share this contract; local screens own their scroll content and
 * save callbacks. Scaffold has zero additional insets to prevent double-padding.
 */
@Composable
internal fun WorkspaceAdaptiveFrame(content: @Composable ColumnScope.() -> Unit) {
    Surface(Modifier.fillMaxSize(), color = CinemaBlack) {
        Box(
            Modifier.fillMaxSize()
                .windowInsetsPadding(WindowInsets.safeDrawing)
                .imePadding(),
        ) {
            Scaffold(
                modifier = Modifier.fillMaxSize(),
                containerColor = CinemaBlack,
                contentWindowInsets = WindowInsets(0, 0, 0, 0),
            ) { scaffoldPadding ->
                Column(
                    modifier = Modifier.fillMaxSize().padding(scaffoldPadding),
                    content = content,
                )
            }
        }
    }
}

/**
 * The important action remains first and full width on narrow phones / large accessibility
 * text, while standard-width phones retain the familiar paired footer.
 */
@Composable
internal fun WorkspaceAdaptiveSaveActions(
    primaryLabel: String,
    secondaryLabel: String = "SAVE",
    onPrimary: () -> Unit,
    onSecondary: () -> Unit,
) {
    BoxWithConstraints(Modifier.fillMaxWidth()) {
        val largeText = LocalDensity.current.fontScale >= 1.3f
        val stacked = maxWidth < 380.dp || largeText
        if (stacked) {
            Column(
                Modifier.fillMaxWidth().padding(horizontal = 18.dp, vertical = 10.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Button(
                    onClick = onPrimary,
                    modifier = Modifier.fillMaxWidth().heightIn(min = 50.dp),
                    shape = RoundedCornerShape(15.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = RecRed),
                ) { Text(primaryLabel, fontWeight = androidx.compose.ui.text.font.FontWeight.Bold) }
                OutlinedButton(
                    onClick = onSecondary,
                    modifier = Modifier.fillMaxWidth().heightIn(min = 50.dp),
                    shape = RoundedCornerShape(15.dp),
                ) { Text(secondaryLabel, fontWeight = androidx.compose.ui.text.font.FontWeight.Bold) }
            }
        } else {
            Row(
                Modifier.fillMaxWidth().padding(horizontal = 18.dp, vertical = 11.dp),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                OutlinedButton(
                    onClick = onSecondary,
                    modifier = Modifier.weight(1f).heightIn(min = 50.dp),
                    shape = RoundedCornerShape(15.dp),
                ) { Text(secondaryLabel, fontWeight = androidx.compose.ui.text.font.FontWeight.Bold) }
                Button(
                    onClick = onPrimary,
                    modifier = Modifier.weight(1f).heightIn(min = 50.dp),
                    shape = RoundedCornerShape(15.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = RecRed),
                ) { Text(primaryLabel, fontSize = 12.sp, fontWeight = androidx.compose.ui.text.font.FontWeight.Bold) }
            }
        }
    }
}
