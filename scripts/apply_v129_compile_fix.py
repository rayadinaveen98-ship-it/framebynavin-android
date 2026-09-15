from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]

theme_path = ROOT / "app/src/main/java/com/framebynavin/app/ui/theme/FrameByNavinTheme.kt"
theme = theme_path.read_text()
theme = theme.replace(
    "enum class FrameSurfacePersonality { SOLID, GLASS, LUMEN }",
    "enum class FrameSurfacePersonality { SOLID, EDITORIAL, GLASS, LUMEN }",
)
if "val isEditorial:" not in theme:
    theme = theme.replace(
        "    val isGlass: Boolean get() = palette.surfacePersonality == FrameSurfacePersonality.GLASS\n",
        "    val isGlass: Boolean get() = palette.surfacePersonality == FrameSurfacePersonality.GLASS\n"
        "    val isEditorial: Boolean get() = palette.surfacePersonality == FrameSurfacePersonality.EDITORIAL\n",
    )
theme_path.write_text(theme)

spot_path = ROOT / "app/src/main/java/com/framebynavin/app/ui/V20GuidedFirstRun.kt"
spot = spot_path.read_text()
if "import androidx.compose.runtime.getValue" not in spot:
    spot = spot.replace(
        "import androidx.compose.runtime.Composable\n",
        "import androidx.compose.runtime.Composable\nimport androidx.compose.runtime.getValue\n",
    )
spot_path.write_text(spot)

print("v129 compile compatibility fix applied")
