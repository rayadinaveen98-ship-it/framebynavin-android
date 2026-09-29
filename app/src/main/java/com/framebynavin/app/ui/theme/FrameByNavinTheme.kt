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

private fun directorsCutProfile() = FrameThemeProfile(
    visualLanguage = FrameVisualLanguage.CINEMATIC,
    cardRadius = 15.dp,
    buttonRadius = 13.dp,
    smallRadius = 10.dp,
    navigationRadius = 22.dp,
    pagePadding = 18.dp,
    sectionGap = 16.dp,
    borderWidth = 1.dp,
    headlineScale = 1.00f,
    guideScrimAlpha = .58f,
    atmosphereAlpha = 1.00f,
    compact = true,
)

private fun directorsCutPalette() = FramePalette(
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
)

private fun backlotLightPalette() = FramePalette(
    background = Color(0xFFF7F4EE),
    surface = Color(0xFFFFFCF7),
    surfaceRaised = Color(0xFFFFFFFF),
    line = Color(0xFFDDD6CC),
    foreground = Color(0xFF1C1B19),
    muted = Color(0xFF68635D),
    primary = Color(0xFFC83F3A),
    primaryDeep = Color(0xFFF6DFDC),
    secondary = Color(0xFF8A6A2F),
    success = Color(0xFF4F765D),
    tertiary = Color(0xFF765C3B),
    isLight = true,
)

/**
 * V148 reduces Backlot to one design language with two appearances.
 *
 * Legacy enum ids are deliberately retained so upgrades from older builds can read a previously
 * saved preference safely. They are not selectable and are normalized to Director's Cut during
 * initialization. This avoids both migration crashes and accidental resurrection of retired looks.
 */
enum class FrameTheme(
    val displayName: String,
    val tagline: String,
    val palette: FramePalette,
    val profile: FrameThemeProfile,
    val selectable: Boolean,
) {
    DIRECTORS_CUT(
        "Director's Cut",
        "Dark · Default · cinema black · REC red · warm ivory",
        directorsCutPalette(),
        directorsCutProfile(),
        true,
    ),
    BACKLOT_LIGHT(
        "Light",
        "Bright production workspace · same Backlot identity",
        backlotLightPalette(),
        directorsCutProfile(),
        true,
    ),

    // Legacy preference ids. Keep for upgrade compatibility only.
    STUDIO_GOLD("Studio Gold", "Retired appearance", directorsCutPalette(), directorsCutProfile(), false),
    MIDNIGHT("Mono Ink", "Retired appearance", directorsCutPalette(), directorsCutProfile(), false),
    LUMEN_FLOW("Ivory Atelier", "Retired appearance", directorsCutPalette(), directorsCutProfile(), false),
    PAPER_QUIET("Paper Quiet", "Retired appearance", directorsCutPalette(), directorsCutProfile(), false),
    MOSS_STUDIO("Moss Studio", "Retired appearance", directorsCutPalette(), directorsCutProfile(), false),
    EMBER("Terracotta Calm", "Retired appearance", directorsCutPalette(), directorsCutProfile(), false),
    VIOLET_NEON("Night Bloom", "Retired appearance", directorsCutPalette(), directorsCutProfile(), false),
    AURORA_GLASS("Blue Hour", "Retired appearance", directorsCutPalette(), directorsCutProfile(), false),
    STORYBOARD("Storyboard", "Retired appearance", directorsCutPalette(), directorsCutProfile(), false),
}

val BacklotSelectableAppearances: List<FrameTheme> = listOf(
    FrameTheme.DIRECTORS_CUT,
    FrameTheme.BACKLOT_LIGHT,
)

private fun FrameTheme.supportedAppearance(): FrameTheme = when (this) {
    FrameTheme.DIRECTORS_CUT, FrameTheme.BACKLOT_LIGHT -> this
    else -> FrameTheme.DIRECTORS_CUT
}

/** Visual preferences stay separate from creator/business settings. */
object VisualExperiencePrefs {
    // Retain this private preference file id so existing installs keep their visual preference.
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
        val storedName = prefs.getString(KEY_THEME, FrameTheme.DIRECTORS_CUT.name).orEmpty()
        val restored = runCatching { FrameTheme.valueOf(storedName) }.getOrDefault(FrameTheme.DIRECTORS_CUT)
        currentTheme = restored.supportedAppearance()
        if (storedName != currentTheme.name) {
            prefs.edit().putString(KEY_THEME, currentTheme.name).apply()
        }
        launchSoundEnabled = prefs.getBoolean(KEY_LAUNCH_SOUND, true)
    }

    fun setTheme(theme: FrameTheme) {
        val supported = theme.supportedAppearance()
        currentTheme = supported
        appContext?.getSharedPreferences(PREFS, Context.MODE_PRIVATE)?.edit()?.putString(KEY_THEME, supported.name)?.apply()
    }

    fun updateLaunchSound(enabled: Boolean) {
        launchSoundEnabled = enabled
        appContext?.getSharedPreferences(PREFS, Context.MODE_PRIVATE)?.edit()?.putBoolean(KEY_LAUNCH_SOUND, enabled)?.apply()
    }

    val palette: FramePalette get() = currentTheme.palette
    val profile: FrameThemeProfile get() = currentTheme.profile
    val isLight: Boolean get() = palette.isLight
    val isGlass: Boolean get() = palette.surfacePersonality == FrameSurfacePersonality.GLASS
    val isEditorial: Boolean get() = palette.surfacePersonality == FrameSurfacePersonality.EDITORIAL
    val isLumen: Boolean get() = palette.surfacePersonality == FrameSurfacePersonality.LUMEN
}

// V148 semantic appearance tokens. New UI should use these names instead of assuming a color.
val BacklotBackground: Color get() = VisualExperiencePrefs.palette.background
val BacklotSurface: Color get() = VisualExperiencePrefs.palette.surface
val BacklotSurfaceRaised: Color get() = VisualExperiencePrefs.palette.surfaceRaised
val BacklotBorder: Color get() = VisualExperiencePrefs.palette.line
val BacklotPrimaryText: Color get() = VisualExperiencePrefs.palette.foreground
val BacklotSecondaryText: Color get() = VisualExperiencePrefs.palette.muted
val BacklotAccent: Color get() = VisualExperiencePrefs.palette.primary
val BacklotAccentContainer: Color get() = VisualExperiencePrefs.palette.primaryDeep
val BacklotSecondaryAccent: Color get() = VisualExperiencePrefs.palette.secondary
val BacklotSuccess: Color get() = VisualExperiencePrefs.palette.success
val BacklotTertiary: Color get() = VisualExperiencePrefs.palette.tertiary
val BacklotSurfaceSelected: Color get() = if (VisualExperiencePrefs.isLight) Color(0xFFF6E8E4) else Color(0xFF241615)
val BacklotSurfacePressed: Color get() = if (VisualExperiencePrefs.isLight) Color(0xFFEDE8E0) else Color(0xFF20201F)
val BacklotNavigationSurface: Color get() = if (VisualExperiencePrefs.isLight) Color(0xFFFFFDF9) else Color(0xFF0F0F0F)
val BacklotInputSurface: Color get() = if (VisualExperiencePrefs.isLight) Color(0xFFFFFFFF) else Color(0xFF141414)
val BacklotScrim: Color get() = Color.Black.copy(alpha = if (VisualExperiencePrefs.isLight) .38f else .62f)
val BacklotWarning: Color get() = if (VisualExperiencePrefs.isLight) Color(0xFF8A5B18) else Color(0xFFD8A657)
val BacklotError: Color get() = if (VisualExperiencePrefs.isLight) Color(0xFFB33C38) else Color(0xFFE65F5A)

// Compatibility aliases. Existing screens continue to follow the selected appearance while V148
// gradually moves them to semantic names.
val CinemaBlack: Color get() = BacklotBackground
val CinemaSurface: Color get() = BacklotSurface
val CinemaSurfaceRaised: Color get() = BacklotSurfaceRaised
val CinemaLine: Color get() = BacklotBorder
val ProjectorIvory: Color get() = BacklotPrimaryText
val MutedText: Color get() = BacklotSecondaryText
val RecRed: Color get() = BacklotAccent
val RecRedDeep: Color get() = BacklotAccentContainer
val MutedGold: Color get() = BacklotSecondaryAccent
val SuccessGreen: Color get() = BacklotSuccess
val FrameTertiary: Color get() = BacklotTertiary

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
            onSurfaceVariant = palette.muted,
            outline = palette.line,
            error = BacklotError,
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
            onSurfaceVariant = palette.muted,
            outline = palette.line,
            error = BacklotError,
        )
    }
    MaterialTheme(
        colorScheme = colors,
        shapes = themeShapes(profile),
        typography = themeTypography(profile),
        content = content,
    )
}
