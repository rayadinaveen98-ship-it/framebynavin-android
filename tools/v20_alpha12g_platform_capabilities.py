from pathlib import Path


gradle = Path("app/build.gradle.kts")
build = gradle.read_text()
if 'versionCode = 93' not in build or 'versionName = "2.0.0-alpha1.2f-production-styles"' not in build:
    raise SystemExit("Unexpected Production Styles build identity")
build = build.replace('versionCode = 93', 'versionCode = 94', 1)
build = build.replace(
    'versionName = "2.0.0-alpha1.2f-production-styles"',
    'versionName = "2.0.0-alpha1.2g-platform-capabilities"',
    1,
)
gradle.write_text(build)
