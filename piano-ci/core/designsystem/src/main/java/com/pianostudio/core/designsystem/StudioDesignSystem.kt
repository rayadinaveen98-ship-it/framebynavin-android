package com.pianostudio.core.designsystem

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

object StudioColors {
    val Ink = Color(0xFF10100E)
    val InkRaised = Color(0xFF181815)
    val Surface = Color(0xFF20201C)
    val SurfaceRaised = Color(0xFF2A2924)
    val Ivory = Color(0xFFF4EFE6)
    val IvoryStrong = Color(0xFFFFFBF4)
    val Muted = Color(0xFFB8B1A7)
    val Subtle = Color(0xFF918B82)
    val Champagne = Color(0xFFC7A96B)
    val ChampagneSoft = Color(0xFFE1C993)
    val Walnut = Color(0xFF6E5543)
    val Success = Color(0xFF7FA987)
    val Warning = Color(0xFFD5A75A)
    val Error = Color(0xFFC87A73)
    val Hairline = Color(0xFF3A3832)
}

@Immutable
data class StudioSpacing(
    val xxs: androidx.compose.ui.unit.Dp = 4.dp,
    val xs: androidx.compose.ui.unit.Dp = 8.dp,
    val sm: androidx.compose.ui.unit.Dp = 12.dp,
    val md: androidx.compose.ui.unit.Dp = 16.dp,
    val lg: androidx.compose.ui.unit.Dp = 24.dp,
    val xl: androidx.compose.ui.unit.Dp = 32.dp,
    val xxl: androidx.compose.ui.unit.Dp = 48.dp,
)

val LocalStudioSpacing = staticCompositionLocalOf { StudioSpacing() }

private val StudioTypography = Typography(
    displaySmall = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.SemiBold,
        fontSize = 36.sp,
        lineHeight = 42.sp,
        letterSpacing = (-0.5).sp,
    ),
    headlineLarge = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.SemiBold,
        fontSize = 30.sp,
        lineHeight = 36.sp,
        letterSpacing = (-0.3).sp,
    ),
    headlineMedium = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.SemiBold,
        fontSize = 26.sp,
        lineHeight = 32.sp,
        letterSpacing = (-0.2).sp,
    ),
    titleLarge = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.SemiBold,
        fontSize = 21.sp,
        lineHeight = 27.sp,
    ),
    titleMedium = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.Medium,
        fontSize = 17.sp,
        lineHeight = 23.sp,
    ),
    bodyLarge = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.Normal,
        fontSize = 16.sp,
        lineHeight = 24.sp,
    ),
    bodyMedium = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.Normal,
        fontSize = 14.sp,
        lineHeight = 21.sp,
    ),
    bodySmall = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.Normal,
        fontSize = 12.sp,
        lineHeight = 18.sp,
    ),
    labelLarge = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.SemiBold,
        fontSize = 14.sp,
        lineHeight = 18.sp,
        letterSpacing = 0.15.sp,
    ),
    labelMedium = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.SemiBold,
        fontSize = 12.sp,
        lineHeight = 16.sp,
        letterSpacing = 0.55.sp,
    ),
    labelSmall = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.Medium,
        fontSize = 11.sp,
        lineHeight = 14.sp,
        letterSpacing = 0.45.sp,
    ),
)

private val StudioShapes = Shapes(
    extraSmall = androidx.compose.foundation.shape.RoundedCornerShape(8.dp),
    small = androidx.compose.foundation.shape.RoundedCornerShape(12.dp),
    medium = androidx.compose.foundation.shape.RoundedCornerShape(16.dp),
    large = androidx.compose.foundation.shape.RoundedCornerShape(22.dp),
    extraLarge = androidx.compose.foundation.shape.RoundedCornerShape(28.dp),
)

private val StudioScheme = darkColorScheme(
    primary = StudioColors.Champagne,
    onPrimary = StudioColors.Ink,
    primaryContainer = StudioColors.SurfaceRaised,
    onPrimaryContainer = StudioColors.IvoryStrong,
    background = StudioColors.Ink,
    onBackground = StudioColors.IvoryStrong,
    surface = StudioColors.InkRaised,
    onSurface = StudioColors.IvoryStrong,
    surfaceVariant = StudioColors.Surface,
    onSurfaceVariant = StudioColors.Muted,
    outline = StudioColors.Hairline,
    error = StudioColors.Error,
    onError = StudioColors.IvoryStrong,
)

@Composable
fun PianoStudioTheme(content: @Composable () -> Unit) {
    @Suppress("UNUSED_VARIABLE")
    val dark = isSystemInDarkTheme()
    androidx.compose.runtime.CompositionLocalProvider(LocalStudioSpacing provides StudioSpacing()) {
        MaterialTheme(
            colorScheme = StudioScheme,
            typography = StudioTypography,
            shapes = StudioShapes,
            content = content,
        )
    }
}

object StudioType {
    val MusicalTitle = TextStyle(
        fontFamily = FontFamily.Serif,
        fontWeight = FontWeight.Medium,
        fontSize = 28.sp,
        lineHeight = 34.sp,
        color = StudioColors.IvoryStrong,
    )

    val PlayerInstruction = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.SemiBold,
        fontSize = 20.sp,
        lineHeight = 26.sp,
        color = StudioColors.IvoryStrong,
    )
}
