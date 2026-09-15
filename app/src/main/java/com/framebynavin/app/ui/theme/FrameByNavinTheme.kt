package com.framebynavin.app.ui.theme

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.Typography
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
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.view.WindowCompat

enum class FrameSurfacePersonality { SOLID, EDITORIAL, GLASS, LUMEN }
enum class FrameVisualLanguage { CINEMATIC, LUXE, MONO, GALLERY, PAPER, ORGANIC, POSTER, BLOOM, HORIZON, STORYBOARD }

data class FrameThemeProfile(
    val visualLanguage: FrameVisualLanguage,
    val cardRadius: Dp,
    val buttonRadius: Dp,
    val smallRadius: Dp,
    val navigationRadius: Dp,
    val pagePadding: Dp,
    val sectionGap: Dp,
    val borderWidth: Dp,
    val headlineScale: Float,
    val guideScrimAlpha: Float,
    val atmosphereAlpha: Float,
    val compact: Boolean = false,
)

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
    val primaryContentDark: Boolean = false,
)

/**
 * v139 finalizes ten real visual languages. Existing enum ids are retained for upgrade safety;
 * new looks get new ids instead of silently replacing Director's Cut again.
 */
enum class FrameTheme(
    val displayName: String,
    val tagline: String,
    val palette: FramePalette,
    val profile: FrameThemeProfile,
) {
    DIRECTORS_CUT(
        "Director's Cut",
        "Classic Backlot · cinema black · REC red · warm ivory",
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
        FrameThemeProfile(FrameVisualLanguage.CINEMATIC, 15.dp, 13.dp, 10.dp, 22.dp, 18.dp, 16.dp, 1.dp, 1.00f, .58f, 1.00f, compact = true),
    ),
    STUDIO_GOLD(
        "Studio Gold",
        "Charcoal · precise gold · restrained editorial luxury",
        FramePalette(
            background = Color(0xFF080B0B),
            surface = Color(0xFF111311),
            surfaceRaised = Color(0xFF191A18),
            line = Color(0xFF302E28),
            foreground = Color(0xFFF4F0E8),
            muted = Color(0xFFA69F95),
            primary = Color(0xFFF4C430),
            primaryDeep = Color(0xFF3A2C00),
            secondary = Color(0xFFD9A441),
            success = Color(0xFF6FA382),
            tertiary = Color(0xFFA66E3F),
            primaryContentDark = true,
        ),
        FrameThemeProfile(FrameVisualLanguage.LUXE, 20.dp, 16.dp, 12.dp, 26.dp, 22.dp, 20.dp, 1.dp, 1.03f, .54f, .88f),
    ),
    MIDNIGHT(
        "Mono Ink",
        "Graphite · ivory · typography-first discipline",
        FramePalette(
            background = Color(0xFF090909),
            surface = Color(0xFF121212),
            surfaceRaised = Color(0xFF1B1B1B),
            line = Color(0xFF343434),
            foreground = Color(0xFFF2F2EF),
            muted = Color(0xFF929292),
            primary = Color(0xFFF2F2EF),
            primaryDeep = Color(0xFF2B2B2B),
            secondary = Color(0xFF737373),
            success = Color(0xFF7E9A87),
            tertiary = Color(0xFFBDBDBD),
            primaryContentDark = true,
        ),
        FrameThemeProfile(FrameVisualLanguage.MONO, 6.dp, 5.dp, 3.dp, 8.dp, 18.dp, 13.dp, 1.dp, .96f, .61f, .55f, compact = true),
    ),
    LUMEN_FLOW(
        "Ivory Atelier",
        "Gallery ivory · charcoal · quiet negative space",
        FramePalette(
            background = Color(0xFFF8F5EE),
            surface = Color(0xFFF0ECE3),
            surfaceRaised = Color(0xFFFFFDF8),
            line = Color(0xFFD8D0C4),
            foreground = Color(0xFF20201F),
            muted = Color(0xFF746E66),
            primary = Color(0xFF20201F),
            primaryDeep = Color(0xFFE5E0D7),
            secondary = Color(0xFFB7A58A),
            success = Color(0xFF607C68),
            tertiary = Color(0xFF9D8061),
            surfacePersonality = FrameSurfacePersonality.LUMEN,
            isLight = true,
        ),
        FrameThemeProfile(FrameVisualLanguage.GALLERY, 2.dp, 3.dp, 2.dp, 4.dp, 26.dp, 24.dp, 1.dp, 1.08f, .44f, .58f),
    ),
    PAPER_QUIET(
        "Paper Quiet",
        "Warm paper · ink · ruled editorial calm",
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
            primaryContentDark = true,
        ),
        FrameThemeProfile(FrameVisualLanguage.PAPER, 10.dp, 8.dp, 6.dp, 12.dp, 23.dp, 18.dp, 1.dp, 1.02f, .46f, .80f),
    ),
    MOSS_STUDIO(
        "Moss Studio",
        "Deep moss · sage · soft organic studio rhythm",
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
            primaryContentDark = true,
        ),
        FrameThemeProfile(FrameVisualLanguage.ORGANIC, 28.dp, 24.dp, 18.dp, 30.dp, 22.dp, 21.dp, 1.dp, 1.00f, .50f, .82f),
    ),
    EMBER(
        "Terracotta Calm",
        "Clay · sand · asymmetric poster-like composition",
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
        FrameThemeProfile(FrameVisualLanguage.POSTER, 4.dp, 3.dp, 2.dp, 6.dp, 24.dp, 22.dp, 2.dp, 1.12f, .52f, .95f),
    ),
    VIOLET_NEON(
        "Night Bloom",
        "Deep plum · rose · elegant curved linework",
        FramePalette(
            background = Color(0xFF140E1D),
            surface = Color(0xFF1E132B),
            surfaceRaised = Color(0xFF28193A),
            line = Color(0xFF45305B),
            foreground = Color(0xFFF4EAF7),
            muted = Color(0xFFB7A8C3),
            primary = Color(0xFFE9739E),
            primaryDeep = Color(0xFF4E1430),
            secondary = Color(0xFF8C74B8),
            success = Color(0xFF79A88D),
            tertiary = Color(0xFFC185D8),
        ),
        FrameThemeProfile(FrameVisualLanguage.BLOOM, 24.dp, 20.dp, 16.dp, 28.dp, 22.dp, 20.dp, 1.dp, 1.05f, .54f, .88f),
    ),
    AURORA_GLASS(
        "Blue Hour",
        "Slate blue · fog grey · wide cinematic quiet",
        FramePalette(
            background = Color(0xFF0D1723),
            surface = Color(0xFF142233),
            surfaceRaised = Color(0xFF1B2D41),
            line = Color(0xFF31455A),
            foreground = Color(0xFFE8EEF3),
            muted = Color(0xFF9DABB8),
            primary = Color(0xFF88A9C2),
            primaryDeep = Color(0xFF26394B),
            secondary = Color(0xFFB9C4CD),
            success = Color(0xFF769789),
            tertiary = Color(0xFF6D859A),
            surfacePersonality = FrameSurfacePersonality.GLASS,
            primaryContentDark = true,
        ),
        FrameThemeProfile(FrameVisualLanguage.HORIZON, 18.dp, 14.dp, 10.dp, 20.dp, 24.dp, 18.dp, 1.dp, 1.00f, .52f, .74f),
    ),
    STORYBOARD(
        "Storyboard",
        "Frame grids · shot labels · clean production notation",
        FramePalette(
            background = Color(0xFFF0EFEA),
            surface = Color(0xFFFAF9F5),
            surfaceRaised = Color(0xFFE5E3DC),
            line = Color(0xFFB8B5AC),
            foreground = Color(0xFF1A1B1D),
            muted = Color(0xFF686A6E),
            primary = Color(0xFF2D5D7B),
            primaryDeep = Color(0xFFD1DEE6),
            secondary = Color(0xFF8A6748),
            success = Color(0xFF5F7E68),
            tertiary = Color(0xFFB4453F),
            surfacePersonality = FrameSurfacePersonality.EDITORIAL,
            isLight = true,
        ),
        FrameThemeProfile(FrameVisualLanguage.STORYBOARD, 1.dp, 1.dp, 1.dp, 2.dp, 20.dp, 16.dp, 1.dp, .98f, .47f, .86f, compact = true),
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
    val profile: FrameThemeProfile get() = currentTheme.profile
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

val BacklotCardRadius: Dp get() = VisualExperiencePrefs.profile.cardRadius
val BacklotButtonRadius: Dp get() = VisualExperiencePrefs.profile.buttonRadius
val BacklotSmallRadius: Dp get() = VisualExperiencePrefs.profile.smallRadius
val BacklotNavigationRadius: Dp get() = VisualExperiencePrefs.profile.navigationRadius
val BacklotPagePadding: Dp get() = VisualExperiencePrefs.profile.pagePadding
val BacklotSectionGap: Dp get() = VisualExperiencePrefs.profile.sectionGap
val BacklotBorderWidth: Dp get() = VisualExperiencePrefs.profile.borderWidth

private tailrec fun Context.frameActivity(): Activity? = when (this) {
    is Activity -> this
    is ContextWrapper -> baseContext.frameActivity()
    else -> null
}

private fun themeShapes(profile: FrameThemeProfile) = Shapes(
    extraSmall = RoundedCornerShape(profile.smallRadius),
    small = RoundedCornerShape(profile.buttonRadius),
    medium = RoundedCornerShape(profile.cardRadius),
    large = RoundedCornerShape(profile.cardRadius + 4.dp),
    extraLarge = RoundedCornerShape(profile.navigationRadius),
)

private fun themeTypography(profile: FrameThemeProfile): Typography {
    val scale = profile.headlineScale
    val displayWeight = when (profile.visualLanguage) {
        FrameVisualLanguage.CINEMATIC, FrameVisualLanguage.POSTER -> FontWeight.Black
        FrameVisualLanguage.LUXE, FrameVisualLanguage.GALLERY, FrameVisualLanguage.BLOOM -> FontWeight.SemiBold
        FrameVisualLanguage.MONO, FrameVisualLanguage.STORYBOARD -> FontWeight.Bold
        else -> FontWeight.Medium
    }
    return Typography(
        displayLarge = TextStyle(fontSize = (42f * scale).sp, lineHeight = (46f * scale).sp, fontWeight = displayWeight),
        headlineLarge = TextStyle(fontSize = (30f * scale).sp, lineHeight = (34f * scale).sp, fontWeight = displayWeight),
        headlineMedium = TextStyle(fontSize = (24f * scale).sp, lineHeight = (28f * scale).sp, fontWeight = displayWeight),
        titleLarge = TextStyle(fontSize = 20.sp, lineHeight = 24.sp, fontWeight = FontWeight.Bold),
        titleMedium = TextStyle(fontSize = 16.sp, lineHeight = 20.sp, fontWeight = FontWeight.SemiBold),
        bodyLarge = TextStyle(fontSize = 15.sp, lineHeight = 21.sp, fontWeight = FontWeight.Normal),
        bodyMedium = TextStyle(fontSize = 13.sp, lineHeight = 19.sp, fontWeight = FontWeight.Normal),
        labelLarge = TextStyle(fontSize = 12.sp, lineHeight = 16.sp, fontWeight = FontWeight.Bold),
    )
}

@Composable
fun FrameByNavinTheme(content: @Composable () -> Unit) {
    val palette = VisualExperiencePrefs.palette
    val profile = VisualExperiencePrefs.profile
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
            onPrimary = if (palette.primaryContentDark) Color(0xFF171513) else Color.White,
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
            onPrimary = if (palette.primaryContentDark) Color(0xFF151515) else Color.White,
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
    MaterialTheme(
        colorScheme = colors,
        shapes = themeShapes(profile),
        typography = themeTypography(profile),
        content = content,
    )
}
