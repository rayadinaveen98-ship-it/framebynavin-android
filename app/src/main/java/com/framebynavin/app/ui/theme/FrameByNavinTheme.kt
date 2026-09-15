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
 * Existing enum ids are preserved where possible so upgrades keep old preferences safe.
 * v138 expands Backlot to eight genuinely different visual worlds instead of recolours.
 */
enum class FrameTheme(val displayName: String, val tagline: String, val palette: FramePalette) {
    DIRECTORS_CUT(
        "Director's Cut",
        "Cinematic black · warm gold · editorial confidence",
        FramePalette(
            background = Color(0xFF080B0B),
            surface = Color(0xFF111311),
            surfaceRaised = Color(0xFF191A18),
            line = Color(0xFF2A2A2A),
            foreground = Color(0xFFF4F0E8),
            muted = Color(0xFFA69F95),
            primary = Color(0xFFF4C430),
            primaryDeep = Color(0xFF3A2C00),
            secondary = Color(0xFFD9A441),
            success = Color(0xFF6FA382),
            tertiary = Color(0xFFA66E3F),
        ),
    ),
    MIDNIGHT(
        "Mono Ink",
        "Graphite black · silver · disciplined minimalism",
        FramePalette(
            background = Color(0xFF0A0A0A),
            surface = Color(0xFF141414),
            surfaceRaised = Color(0xFF1D1D1D),
            line = Color(0xFF343434),
            foreground = Color(0xFFF2F2F2),
            muted = Color(0xFFA0A0A0),
            primary = Color(0xFFF2F2F2),
            primaryDeep = Color(0xFF292929),
            secondary = Color(0xFF6B6B6B),
            success = Color(0xFF7E9A87),
            tertiary = Color(0xFFBDBDBD),
        ),
    ),
    EMBER(
        "Terracotta Calm",
        "Terracotta · sand · warm artistic studio",
        FramePalette(
            background = Color(0xFF2B1B16),
            surface = Color(0xFF3B241C),
            surfaceRaised = Color(0xFF4A2E24),
            line = Color(0xFF6A4335),
            foreground = Color(0xFFF8E7D8),
            muted = Color(0xFFC5A999),
            primary = Color(0xFFC65F3B),
            primaryDeep = Color(0xFF5C2615),
            secondary = Color(0xFFE8D3C4),
            success = Color(0xFF7EA07E),
            tertiary = Color(0xFFB88873),
            surfacePersonality = FrameSurfacePersonality.EDITORIAL,
        ),
    ),
    VIOLET_NEON(
        "Night Bloom",
        "Deep plum · rose · indigo · quiet artistry",
        FramePalette(
            background = Color(0xFF140E1D),
            surface = Color(0xFF1E132B),
            surfaceRaised = Color(0xFF28193A),
            line = Color(0xFF45305B),
            foreground = Color(0xFFF4EAF7),
            muted = Color(0xFFB7A8C3),
            primary = Color(0xFFFF6EA8),
            primaryDeep = Color(0xFF4E1430),
            secondary = Color(0xFF6D5AB9),
            success = Color(0xFF79A88D),
            tertiary = Color(0xFFC185D8),
        ),
    ),
    LUMEN_FLOW(
        "Lumen Flow",
        "Soft ivory · charcoal · ultra-minimal calm",
        FramePalette(
            background = Color(0xFFFAF9F6),
            surface = Color(0xFFF1F1EF),
            surfaceRaised = Color(0xFFFFFFFF),
            line = Color(0xFFD7D1C8),
            foreground = Color(0xFF1F1F1F),
            muted = Color(0xFF6D6963),
            primary = Color(0xFF1F1F1F),
            primaryDeep = Color(0xFFEAE8E2),
            secondary = Color(0xFFD7D1C8),
            success = Color(0xFF607C68),
            tertiary = Color(0xFFC45B5B),
            surfacePersonality = FrameSurfacePersonality.LUMEN,
            isLight = true,
        ),
    ),
    AURORA_GLASS(
        "Aurora Glass",
        "Midnight navy · teal · cool glassy calm",
        FramePalette(
            background = Color(0xFF0B1B2E),
            surface = Color(0xFF10283A),
            surfaceRaised = Color(0xFF163247),
            line = Color(0xFF28465B),
            foreground = Color(0xFFE6F7F6),
            muted = Color(0xFF9FB6C4),
            primary = Color(0xFF22D3C5),
            primaryDeep = Color(0xFF063C3A),
            secondary = Color(0xFF6BCED1),
            success = Color(0xFF78A98D),
            tertiary = Color(0xFF8B6BB7),
            surfacePersonality = FrameSurfacePersonality.GLASS,
        ),
    ),
    PAPER_QUIET(
        "Paper Quiet",
        "Warm paper · ink · muted clay · notebook calm",
        FramePalette(
            background = Color(0xFFF4E9D7),
            surface = Color(0xFFEFE0C9),
            surfaceRaised = Color(0xFFFAF2E4),
            line = Color(0xFFD3BFA5),
            foreground = Color(0xFF1E1E1E),
            muted = Color(0xFF6C6258),
            primary = Color(0xFFC97B63),
            primaryDeep = Color(0xFFE8CDBD),
            secondary = Color(0xFFA8A08F),
            success = Color(0xFF657F68),
            tertiary = Color(0xFF6A7A6B),
            surfacePersonality = FrameSurfacePersonality.EDITORIAL,
            isLight = true,
        ),
    ),
    MOSS_STUDIO(
        "Moss Studio",
        "Deep moss · stone · muted mint · natural calm",
        FramePalette(
            background = Color(0xFF102017),
            surface = Color(0xFF183021),
            surfaceRaised = Color(0xFF21402C),
            line = Color(0xFF355744),
            foreground = Color(0xFFF3F2E7),
            muted = Color(0xFFA9B8A8),
            primary = Color(0xFFA7DAB0),
            primaryDeep = Color(0xFF294632),
            secondary = Color(0xFF7C9A80),
            success = Color(0xFF83B492),
            tertiary = Color(0xFFE8E5D9),
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
            onPrimary = if (palette.primary == Color(0xFF1F1F1F)) Color.White else Color(0xFF171513),
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

private fun currentThemeNeedsDarkOnPrimary(): Boolean = VisualExperiencePrefs.currentTheme in setOf(
    FrameTheme.DIRECTORS_CUT,
    FrameTheme.MIDNIGHT,
    FrameTheme.AURORA_GLASS,
    FrameTheme.MOSS_STUDIO,
)