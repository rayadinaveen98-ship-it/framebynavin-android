from pathlib import Path

gradle = Path("app/build.gradle.kts")
build = gradle.read_text()
if 'versionCode = 91' not in build or 'versionName = "2.0.0-alpha1.2d-creator-modes"' not in build:
    raise SystemExit("Unexpected Creator Modes build identity")
build = build.replace('versionCode = 91', 'versionCode = 92', 1)
build = build.replace(
    'versionName = "2.0.0-alpha1.2d-creator-modes"',
    'versionName = "2.0.0-alpha1.2e-content-archetypes"',
    1,
)
gradle.write_text(build)
