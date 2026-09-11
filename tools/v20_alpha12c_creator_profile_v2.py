from pathlib import Path


def replace_once(path: Path, old: str, new: str, label: str) -> None:
    text = path.read_text()
    if old not in text:
        raise SystemExit(f"{label}: expected source contract not found")
    path.write_text(text.replace(old, new, 1))


onboarding = Path("app/src/main/java/com/framebynavin/app/ui/V18CreatorOnboarding.kt")
fixes = [
    (
        'leadingIcon = if (primaryMode == item) {{ Icon(Icons.Outlined.Star, null, modifier = Modifier.size(15.dp)) }} else null,',
        'leadingIcon = if (primaryMode == item) { { Icon(Icons.Outlined.Star, null, modifier = Modifier.size(15.dp)) } } else null,',
    ),
    (
        'leadingIcon = if (selected) {{ Icon(Icons.Outlined.Check, null, modifier = Modifier.size(16.dp)) }} else null,',
        'leadingIcon = if (selected) { { Icon(Icons.Outlined.Check, null, modifier = Modifier.size(16.dp)) } } else null,',
    ),
    (
        'leadingIcon = if (selected) {{ Icon(Icons.Outlined.Check, null, modifier = Modifier.size(16.dp)) }} else null,',
        'leadingIcon = if (selected) { { Icon(Icons.Outlined.Check, null, modifier = Modifier.size(16.dp)) } } else null,',
    ),
    (
        'item == primaryGoal && selected -> {{ Icon(Icons.Outlined.Star, null, modifier = Modifier.size(15.dp)) }}',
        'item == primaryGoal && selected -> { { Icon(Icons.Outlined.Star, null, modifier = Modifier.size(15.dp)) } }',
    ),
    (
        'selected -> {{ Icon(Icons.Outlined.Check, null, modifier = Modifier.size(15.dp)) }}',
        'selected -> { { Icon(Icons.Outlined.Check, null, modifier = Modifier.size(15.dp)) } }',
    ),
    (
        'leadingIcon = if (primaryGoal == item) {{ Icon(Icons.Outlined.Star, null, modifier = Modifier.size(15.dp)) }} else null,',
        'leadingIcon = if (primaryGoal == item) { { Icon(Icons.Outlined.Star, null, modifier = Modifier.size(15.dp)) } } else null,',
    ),
]
for index, (old, new) in enumerate(fixes, start=1):
    replace_once(onboarding, old, new, f"Compose leadingIcon fix {index}")


gradle = Path("app/build.gradle.kts")
build = gradle.read_text()
if 'versionCode = 89' not in build or 'versionName = "2.0.0-alpha1.2b.1-drive-gate-hotfix"' not in build:
    raise SystemExit("Unexpected accepted Drive-vault foundation build identity")
build = build.replace('versionCode = 89', 'versionCode = 90', 1)
build = build.replace(
    'versionName = "2.0.0-alpha1.2b.1-drive-gate-hotfix"',
    'versionName = "2.0.0-alpha1.2c-creator-profile-v2"',
    1,
)
gradle.write_text(build)
