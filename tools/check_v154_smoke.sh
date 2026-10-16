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
grep -Fq 'versionCode = 154' app/build.gradle.kts
grep -Fq 'versionName = "2.0.4-v154-insights-project-choice-preview"' app/build.gradle.kts
grep -Fq 'var showAdvancedSetup by rememberSaveable(startWithQuickCreate) { mutableStateOf(!startWithQuickCreate) }' "$BASE/V20NewProjectWizard.kt"
grep -Fq 'onQuickCreate = { openComposer(quick = true) }' "$BASE/FrameByNavinV101BApp.kt"
grep -Fq 'Text("QUICK CREATE"' "$BASE/V131PolishUi.kt"
grep -Fq 'PROCESSING_SAFETY_DAYS = 2L' app/src/main/java/com/framebynavin/app/youtube/YouTubeAnalyticsQueryPolicy.kt
grep -Fq 'YouTubeAnalyticsQueryPolicy.shouldRefresh(' "$BASE/V11YouTubeInsights.kt"
echo "V154 visible-language, project entry, and Insights smoke checks passed"
