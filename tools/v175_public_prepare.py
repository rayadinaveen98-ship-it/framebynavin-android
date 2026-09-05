from pathlib import Path
import re

ROOT = Path(__file__).resolve().parents[1]

# Fix Compose compatibility in the generated Best Frames source.
frames_path = ROOT / "app/src/main/java/com/framebynavin/app/ui/V175BestFramesUi.kt"
if frames_path.exists():
    text = frames_path.read_text(encoding="utf-8")
    text = text.replace("import androidx.compose.foundation.layout.matchParentSize\n", "")
    text = text.replace(".matchParentSize()", ".fillMaxSize()")
    frames_path.write_text(text, encoding="utf-8")

# Remove committed signing credentials/config. Public CI will use standard debug signing
# until a private keystore is supplied through environment/secrets later.
gradle_path = ROOT / "app/build.gradle.kts"
gradle = gradle_path.read_text(encoding="utf-8")
gradle = re.sub(
    r"\n\s*signingConfigs \{\n\s*create\(\"dev\"\) \{.*?\n\s*\}\n\s*\}\n",
    "\n",
    gradle,
    flags=re.S,
)
gradle = re.sub(
    r"\n\s*buildTypes \{\n\s*getByName\(\"debug\"\) \{\n\s*signingConfig = signingConfigs\.getByName\(\"dev\"\)\n\s*\}\n\s*\}\n",
    "\n",
    gradle,
    flags=re.S,
)
gradle_path.write_text(gradle, encoding="utf-8")

# Ensure private signing material can never be committed again.
gitignore = ROOT / ".gitignore"
existing = gitignore.read_text(encoding="utf-8") if gitignore.exists() else ""
entries = [
    "keystore/",
    "*.jks",
    "*.keystore",
    "local.properties",
    ".env",
    ".env.*",
]
for entry in entries:
    if entry not in existing.splitlines():
        existing += ("" if existing.endswith("\n") or not existing else "\n") + entry + "\n"
gitignore.write_text(existing, encoding="utf-8")

print("v1.7.5 public-safe preparation applied")
