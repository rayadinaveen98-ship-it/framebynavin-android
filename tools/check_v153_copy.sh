#!/usr/bin/env bash
set -euo pipefail
BASE="app/src/main/java/com/framebynavin/app/ui"
for file in V144SettingsHub.kt V19ContentWorkspaceAlpha3Ui.kt V19ContentWorkspaceAlpha4Ui.kt V19ContentWorkspaceAlpha5Ui.kt V19ContentWorkspaceAlpha6Ui.kt V19ContentWorkspaceUi.kt; do
  if grep -nE 'Funny and Cute|CONTENT PROJECT 2.0|SCRIPT STUDIO · RC2|PUBLISH STUDIO · RC2|DNA BLUEPRINT' "$BASE/$file"; then
    echo "Obsolete copy remains in $file" >&2
    exit 1
  fi
done
grep -Fq 'PBottomNavItem(PTab.CREATE, Icons.Outlined.MovieEdit, "Projects"' "$BASE/FrameByNavinV101BApp.kt"
grep -Fq 'V144CategoryCard("Spark", "Your creative companion"' "$BASE/V144SettingsHub.kt"
grep -Fq 'Text("VIDEO REVIEW"' "$BASE/V20VideoPostmortemUi.kt"
grep -Fq 'versionCode = 153' app/build.gradle.kts
grep -Fq 'versionName = "2.0.3-v153-simple-creator-copy-preview"' app/build.gradle.kts
echo "V153 visible-language smoke checks passed"
