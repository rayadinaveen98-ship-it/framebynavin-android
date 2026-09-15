package com.framebynavin.app.ui.theme

import android.content.Context
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.Color

data class FramePalette(
    val background: Color,
    val surface: Color,
    val surfaceRaised: Color,
    val line: Color,
    val foreground: Color,
    val muted: Color,
    val primary: Color,
    val primaryDeep: Color,
    val secondary: Color,
    val success: Color,
    val isLight: Boolean = false,
)

enum class FrameTheme(val displayName: String, val tagline: String, val palette: FramePalette) {
    DIRECTORS_CUT(
        "Director's Cut",
        "Cinema black · REC red · projector gold",
        FramePalette(
            background = Color(0xFF070707), surface = Color(0xFF101010), surfaceRaised = Color(0xFF151515),
            line = Color(0xFF292929), foreground = Color(0xFFF3EFE7), muted = Color(0xFF918C85),
            primary = Color(0xFFFF3D3D), primaryDeep = Color(0xFF311010), secondary = Color(0xFFD8B56B),
            success = Color(0xFF6BAF83),
        ),
    ),
    MIDNIGHT(
        "Midnight",
        "Deep navy · electric blue · cool cyan",
        FramePalette(
            background = Color(0xFF050812), surface = Color(0xFF0C1220), surfaceRaised = Color(0xFF111A2C),
            line = Color(0xFF26324C), foreground = Color(0xFFEAF2FF), muted = Color(0xFF8A96AA),
            primary = Color(0xFF4BA3FF), primaryDeep = Color(0xFF0D2540), secondary = Color(0xFF62E2FF),
            success = Color(0xFF68D391),
        ),
    ),
    EMBER(
        "Ember",
        "Charcoal · warm orange · amber light",
        FramePalette(
            background = Color(0xFF0A0705), surface = Color(0xFF17100B), surfaceRaised = Color(0xFF20150E),
            line = Color(0xFF3A2A20), foreground = Color(0xFFFBF2E8), muted = Color(0xFFA39182),
            primary = Color(0xFFFF7A30), primaryDeep = Color(0xFF3A1607), secondary = Color(0xFFF7C35F),
            success = Color(0xFF71B886),
        ),
    ),
    VIOLET_NEON(
        "Violet Neon",
        "Ink black · violet · magenta energy",
        FramePalette(
            background = Color(0xFF07050C), surface = Color(0xFF110D19), surfaceRaised = Color(0xFF181122),
            line = Color(0xFF312343), foreground = Color(0xFFF7EFFF), muted = Color(0xFF9B8DAA),
            primary = Color(0xFFB86BFF), primaryDeep = Color(0xFF2D123E), secondary = Color(0xFFFF66C4),
            success = Color(0xFF70D6B1),
        ),
    ),
    IVORY_STUDIO(
        "Ivory Studio",
        "Warm paper · ink · editorial red",
        FramePalette(
            background = Color(0xFFF6F1E8), surface = Color(0xFFFFFCF7), surfaceRaised = Color(0xFFF0E9DE),
            line = Color(0xFFD8CFC0), foreground = Color(0xFF1D1A17), muted = Color(0xFF6F675E),
            primary = Color(0xFFB9232F), primaryDeep = Color(0xFFF2D8DA), secondary = Color(0xFF9A6A25),
            success = Color(0xFF3A7D55), isLight = true,
        ),
    ),
}

/**
 * Tiny visual-preference runtime deliberately separate from creator/business settings.
 * Existing semantic color aliases below resolve through this state, so legacy Compose surfaces
 * switch themes immediately instead of becoming a half-themed Material3 shell.
 */
object VisualExperiencePrefs {
    private const val PREFS = "framebynavin_visual_experience"
    private const val KEY_THEME = "theme"
    private const val KEY_LAUNCH_SOUND = "launch_sound"
    private var appContext: Context? = null

    var currentTheme by mutableStateOf(FrameTheme.DIRECTORS_CUT)
        private set
    var launchSoundEnabled by mutableStateOf(true)
        private set

    fun initialize(context: Context) {
        if (appContext == null) appContext = context.applicationContext
        val prefs = context.applicationContext.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        currentTheme = runCatching {
            FrameTheme.valueOf(prefs.getString(KEY_THEME, FrameTheme.DIRECTORS_CUT.name).orEmpty())
        }.getOrDefault(FrameTheme.DIRECTORS_CUT)
        launchSoundEnabled = prefs.getBoolean(KEY_LAUNCH_SOUND, true)
    }

    fun setTheme(theme: FrameTheme) {
        currentTheme = theme
        appContext?.getSharedPreferences(PREFS, Context.MODE_PRIVATE)?.edit()?.putString(KEY_THEME, theme.name)?.apply()
    }

    fun setLaunchSoundEnabled(enabled: Boolean) {
        launchSoundEnabled = enabled
        appContext?.getSharedPreferences(PREFS, Context.MODE_PRIVATE)?.edit()?.putBoolean(KEY_LAUNCH_SOUND, enabled)?.apply()
    }

    val palette: FramePalette get() = currentTheme.palette
}

// Stable semantic aliases used by the existing app. They now resolve through the active palette.
val CinemaBlack: Color get() = VisualExperiencePrefs.palette.background
val CinemaSurface: Color get() = VisualExperiencePrefs.palette.surface
val CinemaSurfaceRaised: Color get() = VisualExperiencePrefs.palette.surfaceRaised
val CinemaLine: Color get() = VisualExperiencePrefs.palette.line
val ProjectorIvory: Color get() = VisualExperiencePrefs.palette.foreground
val MutedText: Color get() = VisualExperiencePrefs.palette.muted
val RecRed: Color get() = VisualExperiencePrefs.palette.primary
val RecRedDeep: Color get() = VisualExperiencePrefs.palette.primaryDeep
val MutedGold: Color get() = VisualExperiencePrefs.palette.secondary
val SuccessGreen: Color get() = VisualExperiencePrefs.palette.success

@Composable
fun FrameByNavinTheme(content: @Composable () -> Unit) {
    val palette = VisualExperiencePrefs.palette
    val colors = if (palette.isLight) {
        lightColorScheme(
            primary = palette.primary,
            onPrimary = Color.White,
            secondary = palette.secondary,
            background = palette.background,
            onBackground = palette.foreground,
            surface = palette.surface,
            onSurface = palette.foreground,
            surfaceVariant = palette.surfaceRaised,
            outline = palette.line,
            error = palette.primary,
        )
    } else {
        darkColorScheme(
            primary = palette.primary,
            onPrimary = palette.foreground,
            secondary = palette.secondary,
            background = palette.background,
            onBackground = palette.foreground,
            surface = palette.surface,
            onSurface = palette.foreground,
            surfaceVariant = palette.surfaceRaised,
            outline = palette.line,
            error = palette.primary,
        )
    }
    MaterialTheme(colorScheme = colors, content = content)
}
