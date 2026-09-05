from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
gradle_path = ROOT / "app/build.gradle.kts"
gradle = gradle_path.read_text(encoding="utf-8")

gradle = gradle.replace("versionCode = 38", "versionCode = 39")
gradle = gradle.replace(
    'versionName = "1.7.5-ux-copy-polish-rc2"',
    'versionName = "1.7.5-youtube-connection-fix-rc3"',
)

# Public prototype builds intentionally use one fixed DEBUG certificate so
# Google OAuth sees the same package + SHA-1 on every GitHub-hosted build.
# This is not a production signing key.
if 'create("prototypeStable")' not in gradle:
    marker = "    compileSdk = 35\n"
    signing = '''    compileSdk = 35\n\n    signingConfigs {\n        create("prototypeStable") {\n            storeFile = rootProject.file("dev-signing/framebynavin-public-debug.jks")\n            storePassword = "android"\n            keyAlias = "androiddebugkey"\n            keyPassword = "android"\n        }\n    }\n'''
    if marker not in gradle:
        raise RuntimeError("Could not find compileSdk insertion point")
    gradle = gradle.replace(marker, signing, 1)

if 'signingConfig = signingConfigs.getByName("prototypeStable")' not in gradle:
    marker = "    buildFeatures {\n"
    build_types = '''    buildTypes {\n        getByName("debug") {\n            signingConfig = signingConfigs.getByName("prototypeStable")\n        }\n    }\n\n'''
    if marker not in gradle:
        raise RuntimeError("Could not find buildFeatures insertion point")
    gradle = gradle.replace(marker, build_types + marker, 1)

gradle_path.write_text(gradle, encoding="utf-8")
print("v1.7.5 RC3 version + fixed prototype signing applied")
