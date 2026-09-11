from pathlib import Path


def replace_once(path: Path, old: str, new: str, label: str) -> None:
    text = path.read_text()
    if old not in text:
        raise SystemExit(f"{label}: expected source contract not found")
    path.write_text(text.replace(old, new, 1))


ui = Path("app/src/main/java/com/framebynavin/app/ui/V18CreatorOnboarding.kt")
text = ui.read_text()

text = text.replace(
    "import com.framebynavin.app.data.CreatorPlatformRegistry\n",
    "import com.framebynavin.app.data.CreatorPlatformRegistry\nimport com.framebynavin.app.data.ProductionStyleRegistry\n",
    1,
)

production_styles_block = '''private val productionStyles = listOf(
    "Voiceover",
    "Talking Head",
    "Gameplay Capture",
    "Screen Recording",
    "Camera / B-roll",
    "Livestream",
    "Audio-only",
    "Animation / Motion",
    "Writing",
    "Mixed / Hybrid",
)

'''
if production_styles_block not in text:
    raise SystemExit("hardcoded production styles block not found")
text = text.replace(production_styles_block, "", 1)

old_order = '''    val orderedProductionStyles = remember(primaryMode) {
        (selectedModeDefinition.suggestedProductionStyles + productionStyles).distinct()
    }
'''
new_order = '''    val orderedProductionStyles = remember(primaryMode) {
        ProductionStyleRegistry.orderedForMode(primaryMode)
    }
'''
if old_order not in text:
    raise SystemExit("production style ordering contract not found")
text = text.replace(old_order, new_order, 1)
ui.write_text(text)


gradle = Path("app/build.gradle.kts")
build = gradle.read_text()
if 'versionCode = 92' not in build or 'versionName = "2.0.0-alpha1.2e-content-archetypes"' not in build:
    raise SystemExit("Unexpected Content Archetypes build identity")
build = build.replace('versionCode = 92', 'versionCode = 93', 1)
build = build.replace(
    'versionName = "2.0.0-alpha1.2e-content-archetypes"',
    'versionName = "2.0.0-alpha1.2f-production-styles"',
    1,
)
gradle.write_text(build)
