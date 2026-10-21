package com.framebynavin.app.ui

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import com.framebynavin.app.ui.theme.CinemaBlack

/**
 * Activity-owned, modal-feeling screen for the remaining full-screen app overlays.
 *
 * MainActivity already uses edge-to-edge. Avoid a separate Dialog window, which can apply
 * OEM-specific decor/navigation padding independently of the Activity and clip footer controls.
 * Consume safe drawing and keyboard insets once at the Activity content boundary.
 *
 * The opaque click shield intercepts taps on otherwise empty parts of the screen so that
 * underlying navigation cannot be accidentally triggered while this overlay is visible.
 * Keep confirmation AlertDialogs modal and preserve each caller's existing onDismiss behavior.
 */
@Composable
internal fun CreatorAdaptiveFullScreen(
    onDismissRequest: () -> Unit,
    content: @Composable ColumnScope.() -> Unit,
) {
    BackHandler(onBack = onDismissRequest)
    Surface(Modifier.fillMaxSize(), color = CinemaBlack) {
        Box(Modifier.fillMaxSize()) {
            val shieldInteraction = remember { MutableInteractionSource() }
            Box(
                Modifier.matchParentSize().clickable(
                    interactionSource = shieldInteraction,
                    indication = null,
                    onClick = {},
                ),
            )
            Column(
                modifier = Modifier.fillMaxSize()
                    .windowInsetsPadding(WindowInsets.safeDrawing)
                    .imePadding(),
                content = content,
            )
        }
    }
}
