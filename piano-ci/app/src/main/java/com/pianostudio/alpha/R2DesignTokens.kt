package com.pianostudio.alpha

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

val R2Black = Color(0xFF0F0F0D)
val R2Charcoal = Color(0xFF191916)
val R2Carbon = Color(0xFF23221E)
val R2Raised = Color(0xFF2C2A25)
val R2Ivory = Color(0xFFF6F1E7)
val R2White = Color(0xFFFFFDF8)
val R2Gold = Color(0xFFC6A768)
val R2Muted = Color(0xFFA9A49B)
val R2Subtle = Color(0xFF747069)
val R2Success = Color(0xFF7FA886)
val R2Error = Color(0xFFC7766D)
val R2Amber = Color(0xFFD5A85C)

@Composable
fun R2Theme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = darkColorScheme(
            primary = R2Gold,
            onPrimary = R2Black,
            background = R2Black,
            onBackground = R2White,
            surface = R2Charcoal,
            onSurface = R2White,
            surfaceVariant = R2Carbon,
            onSurfaceVariant = R2Muted,
            error = R2Error,
        ),
        content = content,
    )
}
