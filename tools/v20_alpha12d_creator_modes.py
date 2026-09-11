from pathlib import Path


def replace_once(path: Path, old: str, new: str, label: str) -> None:
    text = path.read_text()
    if old not in text:
        raise SystemExit(f"{label}: expected source contract not found")
    path.write_text(text.replace(old, new, 1))


ui = Path("app/src/main/java/com/framebynavin/app/ui/V18CreatorOnboarding.kt")
text = ui.read_text()

text = text.replace(
    "import com.framebynavin.app.data.CreatorProfile\nimport com.framebynavin.app.data.CreatorPlatformRegistry\n",
    "import com.framebynavin.app.data.CreatorModeRegistry\nimport com.framebynavin.app.data.CreatorProfile\nimport com.framebynavin.app.data.CreatorPlatformRegistry\n",
    1,
)

creator_modes_block = '''private val creatorModes = listOf(
    "Film & Entertainment",
    "Gaming",
    "Education",
    "Tech",
    "Lifestyle",
    "Business & Career",
    "Music & Audio",
    "Art & Design",
    "News & Commentary",
    "Food",
    "Travel & Outdoors",
    "Health & Fitness",
    "Other / Hybrid",
)

'''
if creator_modes_block not in text:
    raise SystemExit("hardcoded creator modes block not found")
text = text.replace(creator_modes_block, "", 1)
text = text.replace("creatorModes.forEach { item ->", "CreatorModeRegistry.labels.forEach { item ->", 1)
text = text.replace("creatorModes.filter { it != primaryMode }.forEach { item ->", "CreatorModeRegistry.labels.filter { it != primaryMode }.forEach { item ->", 1)

marker = '''    val canContinue = when (page) {
        0 -> primaryMode.isNotBlank()
        1 -> platforms.isNotEmpty()
        2 -> styles.isNotEmpty()
        3 -> selectedGoals.isNotEmpty() && primaryGoal in selectedGoals
        else -> true
    }
'''
replacement = marker + '''
    val selectedModeDefinition = CreatorModeRegistry.definition(primaryMode)
    val orderedProductionStyles = remember(primaryMode) {
        (selectedModeDefinition.suggestedProductionStyles + productionStyles).distinct()
    }
'''
if marker not in text:
    raise SystemExit("onboarding state marker not found")
text = text.replace(marker, replacement, 1)

subtitle = '''                        Text("Your primary mode sets smart defaults. Secondary modes keep FrameByNavin flexible when your work crosses niches.", color = MutedText, fontSize = 12.sp, lineHeight = 18.sp)
'''
subtitle_new = subtitle + '''                        if (primaryMode.isNotBlank()) {
                            Spacer(Modifier.height(7.dp))
                            Text(selectedModeDefinition.description, color = MutedGold.copy(alpha = .82f), fontSize = 10.5.sp, lineHeight = 15.sp)
                        }
'''
if subtitle not in text:
    raise SystemExit("creator mode subtitle not found")
text = text.replace(subtitle, subtitle_new, 1)

style_copy = '''                        Text("This describes production, not your niche. It will let workflows distinguish gameplay capture from screen recording, voiceover, live work and more.", color = MutedText, fontSize = 12.sp, lineHeight = 18.sp)
                        Spacer(Modifier.height(20.dp))
'''
style_copy_new = '''                        Text("This describes production, not your niche. Suggestions for ${selectedModeDefinition.label} appear first, but every production style stays available.", color = MutedText, fontSize = 12.sp, lineHeight = 18.sp)
                        Spacer(Modifier.height(20.dp))
'''
if style_copy not in text:
    raise SystemExit("production style copy not found")
text = text.replace(style_copy, style_copy_new, 1)
text = text.replace("productionStyles.forEach { item ->", "orderedProductionStyles.forEach { item ->", 1)

ui.write_text(text)

gradle = Path("app/build.gradle.kts")
build = gradle.read_text()
if 'versionCode = 90' not in build or 'versionName = "2.0.0-alpha1.2c-creator-profile-v2"' not in build:
    raise SystemExit("Unexpected Creator Profile V2 build identity")
build = build.replace('versionCode = 90', 'versionCode = 91', 1)
build = build.replace(
    'versionName = "2.0.0-alpha1.2c-creator-profile-v2"',
    'versionName = "2.0.0-alpha1.2d-creator-modes"',
    1,
)
gradle.write_text(build)
