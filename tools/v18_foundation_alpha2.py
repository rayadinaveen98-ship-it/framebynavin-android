from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
path = ROOT / "app/build.gradle.kts"
text = path.read_text(encoding="utf-8")

if "versionCode = 41" not in text:
    raise SystemExit("Expected foundation alpha1 versionCode before alpha2 bump")
if 'versionName = "1.8.0-foundation-alpha1"' not in text:
    raise SystemExit("Expected foundation alpha1 versionName before alpha2 bump")

text = text.replace("versionCode = 41", "versionCode = 42", 1)
text = text.replace('versionName = "1.8.0-foundation-alpha1"', 'versionName = "1.8.0-foundation-alpha2"', 1)
path.write_text(text, encoding="utf-8")
print("v1.8 foundation alpha2 version applied")
