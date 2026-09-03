package com.framebynavin.app.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

val CinemaBlack = Color(0xFF070707)
val CinemaSurface = Color(0xFF101010)
val CinemaSurfaceRaised = Color(0xFF151515)
val CinemaLine = Color(0xFF292929)
val ProjectorIvory = Color(0xFFF3EFE7)
val MutedText = Color(0xFF918C85)
val RecRed = Color(0xFFFF3D3D)
val RecRedDeep = Color(0xFF311010)
val MutedGold = Color(0xFFD8B56B)

private val FrameByNavinColors = darkColorScheme(
    primary = RecRed,
    onPrimary = ProjectorIvory,
    background = CinemaBlack,
    onBackground = ProjectorIvory,
    surface = CinemaSurface,
    onSurface = ProjectorIvory,
    outline = CinemaLine,
)

@Composable
fun FrameByNavinTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = FrameByNavinColors,
        content = content
    )
}
