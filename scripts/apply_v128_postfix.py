from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]

def p(rel): return ROOT / rel

def replace_once(rel, old, new, required=True):
    path = p(rel)
    text = path.read_text()
    if old not in text:
        if required:
            raise SystemExit(f"Missing expected block in {rel}: {old[:140]!r}")
        return
    path.write_text(text.replace(old, new, 1))

# Widget compile support for the new sixth theme. Keep the known-green v127 updater shape;
# only extend its exhaustive theme mapping and clean obsolete experimental imports if present.
widget = p("app/src/main/java/com/framebynavin/app/widget/CreatorWidgetUpdater.kt")
text = widget.read_text()
text = text.replace("import androidx.core.graphics.toColorInt\n", "")
if "FrameTheme.AURORA_GLASS" not in text:
    text = text.replace(
        "        FrameTheme.IVORY_STUDIO -> R.drawable.widget_bg_ivory\n",
        "        FrameTheme.IVORY_STUDIO -> R.drawable.widget_bg_ivory\n        FrameTheme.AURORA_GLASS -> R.drawable.widget_bg_aurora\n",
    )
widget.write_text(text)

# Fix the most visible Ivory leftover in Today: no fixed dark capture card.
replace_once(
    "app/src/main/java/com/framebynavin/app/ui/V20TodayCommandCenter.kt",
    "                color = Color(0xFF171310),\n",
    "                color = CinemaSurfaceRaised,\n",
    required=False,
)

# Older creator-home fallback follows the active theme instead of forcing brown/black surfaces.
home = p("app/src/main/java/com/framebynavin/app/ui/V133CreatorHome.kt")
text = home.read_text()
text = text.replace("import com.framebynavin.app.ui.theme.SuccessGreen\n", "import com.framebynavin.app.ui.theme.SuccessGreen\nimport com.framebynavin.app.ui.theme.FrameTertiary\nimport com.framebynavin.app.ui.theme.VisualExperiencePrefs\n")
text = text.replace(
    "        Modifier.fillMaxSize().background(\n            Brush.radialGradient(\n                colors = listOf(Color(0xFF231410), Color(0xFF101012), CinemaBlack),\n                radius = 1100f,\n            )\n        )\n",
    "        Modifier.fillMaxSize().background(\n            when {\n                VisualExperiencePrefs.isGlass -> Brush.linearGradient(listOf(CinemaBlack, FrameTertiary.copy(alpha = .20f), CinemaSurface))\n                VisualExperiencePrefs.isEditorial -> Brush.verticalGradient(listOf(CinemaBlack, CinemaSurfaceRaised))\n                else -> Brush.radialGradient(listOf(RecRed.copy(alpha = .10f), CinemaSurface, CinemaBlack), radius = 1100f)\n            }\n        )\n",
)
text = text.replace("border = BorderStroke(1.dp, Color(0xFF3B3029))", "border = BorderStroke(1.dp, CinemaLine)")
text = text.replace("color = if (overdue) RecRed.copy(alpha = .13f) else Color(0xFF1B1713)", "color = if (overdue) RecRed.copy(alpha = .13f) else CinemaSurfaceRaised")
text = text.replace("background(Color(0xFF242429), RoundedCornerShape(10.dp))", "background(CinemaLine, RoundedCornerShape(10.dp))")
home.write_text(text)

# Motion polish: slower optical settling, gentler haptic visual rhythm.
motion = p("app/src/main/java/com/framebynavin/app/ui/V127MotionUi.kt")
text = motion.read_text()
text = text.replace("progress.animateTo(1f, tween(620, easing = FastOutSlowInEasing))", "progress.animateTo(1f, tween(760, easing = FastOutSlowInEasing))")
text = text.replace("delay(620L)\n        opacity.animateTo(0f, tween(260))", "delay(590L)\n        opacity.animateTo(0f, tween(280, easing = FastOutSlowInEasing))")
text = text.replace("pulse.animateTo(1f, tween(820, easing = FastOutSlowInEasing))", "pulse.animateTo(1f, tween(1040, easing = FastOutSlowInEasing))")
motion.write_text(text)

# Dynamic system-bar contrast is critical for Ivory Studio.
theme = p("app/src/main/java/com/framebynavin/app/ui/theme/FrameByNavinTheme.kt")
text = theme.read_text()
if "WindowCompat" not in text:
    text = text.replace("import android.content.Context\n", "import android.app.Activity\nimport android.content.Context\nimport android.content.ContextWrapper\n")
    text = text.replace("import androidx.compose.runtime.setValue\n", "import androidx.compose.runtime.setValue\nimport androidx.compose.runtime.SideEffect\n")
    text = text.replace("import androidx.compose.ui.graphics.Color\n", "import androidx.compose.ui.graphics.Color\nimport androidx.compose.ui.platform.LocalView\nimport androidx.core.view.WindowCompat\n")
    text = text.replace(
        "@Composable\nfun FrameByNavinTheme(content: @Composable () -> Unit) {\n    val palette = VisualExperiencePrefs.palette\n",
        "private tailrec fun Context.frameActivity(): Activity? = when (this) {\n    is Activity -> this\n    is ContextWrapper -> baseContext.frameActivity()\n    else -> null\n}\n\n@Composable\nfun FrameByNavinTheme(content: @Composable () -> Unit) {\n    val palette = VisualExperiencePrefs.palette\n    val view = LocalView.current\n    SideEffect {\n        val window = view.context.frameActivity()?.window ?: return@SideEffect\n        WindowCompat.getInsetsController(window, view).apply {\n            isAppearanceLightStatusBars = palette.isLight\n            isAppearanceLightNavigationBars = palette.isLight\n        }\n    }\n",
    )
theme.write_text(text)

print("v128 postfix applied")
