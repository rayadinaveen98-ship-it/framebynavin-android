package com.framebynavin.app.ui

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
import com.framebynavin.app.ui.theme.ProjectorIvory
import com.framebynavin.app.ui.theme.RecRed

/** Personal reminder actions must remain reachable at large fonts and on narrow phones. */
internal object V2271PersonalFooterPolicy {
    fun vertical(availableWidthDp: Float, fontScale: Float): Boolean =
        availableWidthDp < 380f || fontScale >= 1.3f
}

@Composable
internal fun V2271PersonalEditorFooter(
    saving: Boolean,
    onCancel: () -> Unit,
    onSave: () -> Unit,
) {
    BoxWithConstraints(Modifier.fillMaxWidth().testTag("v2271-personal-footer")) {
        val vertical = V2271PersonalFooterPolicy.vertical(
            maxWidth.value, LocalDensity.current.fontScale,
        )
        if (vertical) {
            Column(
                modifier = Modifier.fillMaxWidth().padding(top = 10.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                PersonalSaveButton(
                    saving, onSave,
                    Modifier.fillMaxWidth(),
                )
                PersonalCancelButton(onCancel, Modifier.fillMaxWidth())
            }
        } else {
            Row(
                modifier = Modifier.fillMaxWidth().padding(top = 10.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                PersonalCancelButton(onCancel, Modifier.weight(1f))
                PersonalSaveButton(saving, onSave, Modifier.weight(1f))
            }
        }
    }
}

@Composable
private fun PersonalSaveButton(
    saving: Boolean,
    onSave: () -> Unit,
    modifier: Modifier,
) {
    Button(
        onClick = onSave,
        enabled = !saving,
        modifier = modifier.heightIn(min = 52.dp)
            .testTag("v2232-personal-save"),
        colors = ButtonDefaults.buttonColors(containerColor = RecRed),
        shape = RoundedCornerShape(15.dp),
    ) {
        Text(
            if (saving) "SAVING" else "SAVE",
            fontSize = 13.sp,
            fontWeight = FontWeight.Bold,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

@Composable
private fun PersonalCancelButton(onCancel: () -> Unit, modifier: Modifier) {
    OutlinedButton(
        onClick = onCancel,
        modifier = modifier.heightIn(min = 52.dp).testTag("v2271-personal-cancel"),
        shape = RoundedCornerShape(15.dp),
    ) {
        Text("CANCEL", color = ProjectorIvory, fontSize = 13.sp, maxLines = 2)
    }
}
