#!/usr/bin/env python3
from pathlib import Path

root = Path(__file__).resolve().parents[1]
app = root / "app/src/main/java/com/framebynavin/app/ui/FrameByNavinV101BApp.kt"
gradle = root / "app/build.gradle.kts"

text = app.read_text(encoding="utf-8")
old = '''            onSecondary = {
                if (guidedTourStep == CreatorGuidedTourStep.IDEAS) {
                    externalStudioId = null
                    tab = PTab.CREATE
                    guidedTourStepName = CreatorGuidedTourStep.PROJECT.name
                }
            },'''
new = '''            onSecondary = {
                when (guidedTourStep) {
                    CreatorGuidedTourStep.IDEAS -> {
                        externalStudioId = null
                        tab = PTab.CREATE
                        guidedTourStepName = CreatorGuidedTourStep.PROJECT.name
                    }
                    CreatorGuidedTourStep.PROJECT -> {
                        // A tour must never force the creator to create real data. If they do not
                        // want a project yet, skip the Workspace-only lesson and continue to Insights.
                        externalStudioId = null
                        tab = PTab.INSIGHTS
                        guidedTourStepName = CreatorGuidedTourStep.INSIGHTS.name
                    }
                    else -> Unit
                }
            },'''
if old not in text:
    raise SystemExit("Expected guided-tour secondary-action block was not found; refusing unsafe patch")
app.write_text(text.replace(old, new, 1), encoding="utf-8")

g = gradle.read_text(encoding="utf-8")
if 'versionCode = 124' not in g:
    raise SystemExit("Expected versionCode 124 baseline not found")
if 'versionName = "2.0.0-rc2-guided-first-run"' not in g:
    raise SystemExit("Expected v124 versionName baseline not found")
g = g.replace('versionCode = 124', 'versionCode = 125', 1)
g = g.replace(
    'versionName = "2.0.0-rc2-guided-first-run"',
    'versionName = "2.0.0-rc2-progressive-project-tour-fix"',
    1,
)
gradle.write_text(g, encoding="utf-8")

print("Applied v125 progressive-project + guided-tour integration patch")
