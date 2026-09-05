from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
gradle_path = ROOT / "app/build.gradle.kts"
gradle = gradle_path.read_text(encoding="utf-8")

gradle = gradle.replace("versionCode = 38", "versionCode = 39")
gradle = gradle.replace(
    'versionName = "1.7.5-ux-copy-polish-rc2"',
    'versionName = "1.7.5-youtube-connection-fix-rc3"',
)

gradle_path.write_text(gradle, encoding="utf-8")
print("v1.7.5 RC3 version applied")
