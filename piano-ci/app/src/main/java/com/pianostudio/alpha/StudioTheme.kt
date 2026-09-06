package com.pianostudio.alpha

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

val StudioBlack = Color(0xFF0F0F0D)
val StudioCharcoal = Color(0xFF191917)
val StudioCarbon = Color(0xFF24231F)
val StudioIvory = Color(0xFFF5EFE3)
val StudioWhite = Color(0xFFFFFCF6)
val StudioGold = Color(0xFFC8A76A)
val StudioGoldSoft = Color(0xFFE4C98F)
val StudioMuted = Color(0xFFA7A39B)
val StudioSuccess = Color(0xFF78A982)
val StudioError = Color(0xFFC97870)

@Composable
fun QuietConcertStudioTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = darkColorScheme(
            primary = StudioGold,
            onPrimary = StudioBlack,
            background = StudioBlack,
            onBackground = StudioWhite,
            surface = StudioCharcoal,
            onSurface = StudioWhite,
            surfaceVariant = StudioCarbon,
            onSurfaceVariant = StudioMuted,
            error = StudioError,
        ),
        content = content,
    )
}
