package com.framebynavin.app.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.framebynavin.app.ui.theme.CinemaSurfaceRaised
import com.framebynavin.app.ui.theme.ProjectorIvory
import com.framebynavin.app.ui.theme.RecRed

/**
 * Never squeeze a long Save Voice Idea label between Cancel and the system edge.
 * Stack the full-width primary action on small phones and at accessibility font
 * scales; keep the familiar horizontal controls on standard viewports.
 */
internal object V227IdeaFooterPolicy {
    fun stacked(availableWidthDp: Float, fontScale: Float): Boolean =
        availableWidthDp < 380f || fontScale >= 1.3f
}

@Composable
internal fun V227IdeaEditorActionFooter(
    saveEnabled: Boolean,
    isVoiceIdea: Boolean,
    onSave: () -> Unit,
    onCancel: () -> Unit,
) {
    Surface(color = CinemaSurfaceRaised, tonalElevation = 8.dp) {
        BoxWithConstraints(Modifier.fillMaxWidth().testTag("v227-edit-footer")) {
            val stacked = V227IdeaFooterPolicy.stacked(
                maxWidth.value,
                LocalDensity.current.fontScale,
            )
            if (stacked) {
                Column(
                    Modifier.fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 10.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    V227SaveIdeaAction(
                        modifier = Modifier.fillMaxWidth(),
                        enabled = saveEnabled,
                        isVoiceIdea = isVoiceIdea,
                        onClick = onSave,
                    )
                    V227CancelIdeaAction(
                        modifier = Modifier.fillMaxWidth(),
                        onClick = onCancel,
                    )
                }
            } else {
                Row(
                    Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 10.dp),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    V227CancelIdeaAction(
                        modifier = Modifier.weight(1f),
                        onClick = onCancel,
                    )
                    V227SaveIdeaAction(
                        modifier = Modifier.weight(1.55f),
                        enabled = saveEnabled,
                        isVoiceIdea = isVoiceIdea,
                        onClick = onSave,
                    )
                }
            }
        }
    }
}

@Composable
private fun V227SaveIdeaAction(
    modifier: Modifier,
    enabled: Boolean,
    isVoiceIdea: Boolean,
    onClick: () -> Unit,
) {
    Button(
        onClick = onClick,
        enabled = enabled,
        modifier = modifier.heightIn(min = 52.dp).testTag("v216-edit-save"),
        colors = ButtonDefaults.buttonColors(containerColor = RecRed),
        shape = RoundedCornerShape(15.dp),
    ) {
        Text(
            if (isVoiceIdea) "SAVE VOICE IDEA" else "SAVE",
            fontSize = 13.sp,
            fontWeight = FontWeight.Black,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

@Composable
private fun V227CancelIdeaAction(modifier: Modifier, onClick: () -> Unit) {
    TextButton(
        onClick = onClick,
        modifier = modifier.heightIn(min = 52.dp).testTag("v216-edit-cancel"),
    ) {
        Text(
            "CANCEL",
            color = ProjectorIvory,
            fontSize = 13.sp,
            maxLines = 2,
        )
    }
}
