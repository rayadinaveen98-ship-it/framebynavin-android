package com.framebynavin.app.ui.theme

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

enum class FrameSurfacePersonality { SOLID, EDITORIAL, GLASS, LUMEN }

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
    val tertiary: Color = secondary,
    val surfacePersonality: FrameSurfacePersonality = FrameSurfacePersonality.SOLID,
    val isLight: Boolean = false,
)

/**
 * Existing enum ids are preserved so upgrades keep old preferences and widget logic safe.
 * The visible v137 themes are intentionally different moods, not simple recolours.
 */
enum class FrameTheme(val displayName: String, val tagline: String, val palette: FramePalette) {
    DIRECTORS_CUT(
        "Director's Cut",
        "Bold cinema black · REC red · warm gold",
        FramePalette(
            background = Color(0xFF070707),
            surface = Color(0xFF101010),
            surfaceRaised = Color(0xFF171717),
            line = Color(0xFF2B2927),
            foreground = Color(0xFFF4F0E8),
            muted = Color(0xFF938E86),
            primary = Color(0xFFE94A45),
            primaryDeep = Color(0xFF321111),
            secondary = Color(0xFFD2AE67),
            success = Color(0xFF6FA382),
            tertiary = Color(0xFF8D7857),
        ),
    ),
    MIDNIGHT(
        "Midnight Ink",
        "Deep navy · soft steel blue · quiet focus",
        FramePalette(
            background = Color(0xFF0A0E14),
            surface = Color(0xFF111823),
            surfaceRaised = Color(0xFF172231),
            line = Color(0xFF29394A),
            foreground = Color(0xFFEAF0F4),
            muted = Color(0xFF8996A3),
            primary = Color(0xFF6E90B3),
            primaryDeep = Color(0xFF18283A),
            secondary = Color(0xFFA8BAC8),
            success = Color(0xFF6D9C86),
            tertiary = Color(0xFF7898A9),
        ),
    ),
    EMBER(
        "Gallery Sand",
        "Warm paper · clay · editorial calm",
        FramePalette(
            background = Color(0xFFF0E7DA),
            surface = Color(0xFFF9F3E9),
            surfaceRaised = Color(0xFFE7DACB),
            line = Color(0xFFD1C0AD),
            foreground = Color(0xFF292621),
            muted = Color(0xFF786E63),
            primary = Color(0xFF9B614A),
            primaryDeep = Color(0xFFE3CCBC),
            secondary = Color(0xFF8B7556),
            success = Color(0xFF64806D),
            tertiary = Color(0xFFC28A6C),
            surfacePersonality = FrameSurfacePersonality.EDITORIAL,
            isLight = true,
        ),
    ),
    VIOLET_NEON(
        "Moss Atelier",
        "Forest ink · sage · crafted warmth",
        FramePalette(
            background = Color(0xFF0F140F),
            surface = Color(0xFF171D16),
            surfaceRaised = Color(0xFF1F281E),
            line = Color(0xFF32402F),
            foreground = Color(0xFFEDF0E8),
            muted = Color(0xFF96A08E),
            primary = Color(0xFF7D9A6B),
            primaryDeep = Color(0xFF243020),
            secondary = Color(0xFFC5B887),
            success = Color(0xFF79A788),
            tertiary = Color(0xFF9EAD7A),
        ),
    ),
    LUMEN_FLOW(
        "Monochrome Mono",
        "Black · ivory · disciplined minimalism",
        FramePalette(
            background = Color(0xFF090909),
            surface = Color(0xFF121212),
            surfaceRaised = Color(0xFF1B1B1B),
            line = Color(0xFF30302F),
            foreground = Color(0xFFF2F2EF),
            muted = Color(0xFF8D8D88),
            primary = Color(0xFFE8E8E4),
            primaryDeep = Color(0xFF262626),
            secondary = Color(0xFFB6B6B0),
            success = Color(0xFF7E9A87),
            tertiary = Color(0xFF73736E),
        ),
    ),
    AURORA_GLASS(
        "Ivory Studio",
        "Soft ivory · graphite · restrained gold",
        FramePalette(
            background = Color(0xFFF6F1E7),
            surface = Color(0xFFFFFCF6),
            surfaceRaised = Color(0xFFEEE5D8),
            line = Color(0xFFD7CABB),
            foreground = Color(0xFF211F1B),
            muted = Color(0xFF776F65),
            primary = Color(0xFF8C6845),
            primaryDeep = Color(0xFFE4D6C3),
            secondary = Color(0xFFB08B58),
            success = Color(0xFF607C68),
            tertiary = Color(0xFF8A7A65),
            surfacePersonality = FrameSurfacePersonality.EDITORIAL,
            isLight = true,
        ),
    ),
}

/** Visual preferences stay separate from creator/business settings. */
object VisualExperiencePrefs {
    // Retain this private preference file id so existing installs keep their selected visual state.
    private const val PREFS = "framebynavin_visual_experience"
    private const val KEY_THEME = "theme"
    private const val KEY_LAUNCH_SOUND = "launch_sound"
    private var appContext: Context? = null

    var currentTheme by mutableStateOf(FrameTheme.DIRECTORS_CUT)
        private set
    var launchSoundEnabled by mutableStateOf(true)
        private set

    fun initialize(context: Context) {
        if (appContext != null) return
        appContext = context.applicationContext
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

    fun updateLaunchSound(enabled: Boolean) {
        launchSoundEnabled = enabled
        appContext?.getSharedPreferences(PREFS, Context.MODE_PRIVATE)?.edit()?.putBoolean(KEY_LAUNCH_SOUND, enabled)?.apply()
    }

    val palette: FramePalette get() = currentTheme.palette
    val isGlass: Boolean get() = palette.surfacePersonality == FrameSurfacePersonality.GLASS
    val isEditorial: Boolean get() = palette.surfacePersonality == FrameSurfacePersonality.EDITORIAL
    val isLumen: Boolean get() = palette.surfacePersonality == FrameSurfacePersonality.LUMEN
}

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
val FrameTertiary: Color get() = VisualExperiencePrefs.palette.tertiary

private tailrec fun Context.frameActivity(): Activity? = when (this) {
    is Activity -> this
    is ContextWrapper -> baseContext.frameActivity()
    else -> null
}

@Composable
fun FrameByNavinTheme(content: @Composable () -> Unit) {
    val palette = VisualExperiencePrefs.palette
    val view = LocalView.current
    SideEffect {
        val window = view.context.frameActivity()?.window ?: return@SideEffect
        window.statusBarColor = palette.background.toArgb()
        window.navigationBarColor = palette.background.toArgb()
        WindowCompat.getInsetsController(window, view).apply {
            isAppearanceLightStatusBars = palette.isLight
            isAppearanceLightNavigationBars = palette.isLight
        }
    }
    val colors = if (palette.isLight) {
        lightColorScheme(
            primary = palette.primary,
            onPrimary = Color.White,
            secondary = palette.secondary,
            tertiary = palette.tertiary,
            background = palette.background,
            onBackground = palette.foreground,
            surface = palette.surface,
            onSurface = palette.foreground,
            surfaceVariant = palette.surfaceRaised,
            outline = palette.line,
            error = Color(0xFFB34D49),
        )
    } else {
        darkColorScheme(
            primary = palette.primary,
            onPrimary = if (currentThemeNeedsDarkOnPrimary()) Color(0xFF151515) else palette.foreground,
            secondary = palette.secondary,
            tertiary = palette.tertiary,
            background = palette.background,
            onBackground = palette.foreground,
            surface = palette.surface,
            onSurface = palette.foreground,
            surfaceVariant = palette.surfaceRaised,
            outline = palette.line,
            error = Color(0xFFE65F5A),
        )
    }
    MaterialTheme(colorScheme = colors, content = content)
}

private fun currentThemeNeedsDarkOnPrimary(): Boolean = VisualExperiencePrefs.currentTheme == FrameTheme.LUMEN_FLOW
