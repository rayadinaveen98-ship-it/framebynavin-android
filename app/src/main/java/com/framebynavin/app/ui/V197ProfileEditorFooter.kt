package com.framebynavin.app.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.framebynavin.app.ui.theme.*

/** UI-only: save validation and durable profile callbacks remain with V168CreatorProfileEditor. */
@Composable
internal fun V197ProfileEditorFooter(
    enabled: Boolean,
    onSave: () -> Unit,
    onCancel: () -> Unit,
) {
    BoxWithConstraints(Modifier.fillMaxWidth()) {
        val stacked = maxWidth < 370.dp || LocalDensity.current.fontScale >= 1.35f
        if (stacked) {
            Column(
                Modifier.fillMaxWidth().padding(vertical = 10.dp),
                verticalArrangement = Arrangement.spacedBy(7.dp),
            ) {
                V197ProfileSave(enabled, onSave, Modifier.fillMaxWidth())
                V197ProfileCancel(onCancel, Modifier.fillMaxWidth())
            }
        } else {
            Row(
                Modifier.fillMaxWidth().padding(vertical = 12.dp),
                horizontalArrangement = Arrangement.spacedBy(9.dp),
            ) {
                V197ProfileCancel(onCancel, Modifier.weight(1f))
                V197ProfileSave(enabled, onSave, Modifier.weight(1.4f))
            }
        }
    }
}

@Composable
private fun V197ProfileCancel(onCancel: () -> Unit, modifier: Modifier) {
    OutlinedButton(
        onClick = onCancel,
        modifier = modifier.heightIn(min = 50.dp),
        border = BorderStroke(1.dp, CinemaLine),
    ) { Text("CANCEL", fontSize = 12.sp) }
}

@Composable
private fun V197ProfileSave(enabled: Boolean, onSave: () -> Unit, modifier: Modifier) {
    Button(
        onClick = onSave,
        enabled = enabled,
        modifier = modifier.heightIn(min = 50.dp).testTag("v168-save-profile"),
        colors = ButtonDefaults.buttonColors(containerColor = RecRed),
        shape = RoundedCornerShape(15.dp),
    ) {
        Text(
            "SAVE CHANGES",
            fontSize = 12.sp,
            fontWeight = FontWeight.Bold,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
        )
    }
}
