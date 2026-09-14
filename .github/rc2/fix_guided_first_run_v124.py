from pathlib import Path

ROOT = Path("app/src/main/java/com/framebynavin/app/ui/FrameByNavinV101BApp.kt")


def replace_once(text: str, old: str, new: str, label: str) -> str:
    count = text.count(old)
    if count != 1:
        raise SystemExit(f"{label}: expected exactly one anchor, found {count}")
    return text.replace(old, new, 1)


text = ROOT.read_text()

text = replace_once(
    text,
    '''    BackHandler(enabled = guidedTourStepName != null || focusTaskId != null || showComposer || showQuickCapture || showReminders || showControl || overlay != POverlay.NONE || tab != PTab.TODAY) {\n        when {\n            guidedTourStepName != null -> {\n                settingsStore.markGuidedTourComplete()\n                settings = settingsStore.snapshot()\n                guidedTourStepName = null\n            }\n            focusTaskId != null -> focusTaskId = null\n            showComposer -> showComposer = false\n            showQuickCapture -> showQuickCapture = false\n            showReminders -> showReminders = false\n            showControl -> { showControl = false; controlExpanded = false }\n            overlay != POverlay.NONE -> overlay = POverlay.NONE\n            else -> {\n                externalStudioId = null\n                tab = PTab.TODAY\n            }\n        }\n    }\n''',
    '''    BackHandler(enabled = guidedTourStepName != null || focusTaskId != null || showComposer || showQuickCapture || showReminders || showControl || overlay != POverlay.NONE || tab != PTab.TODAY) {\n        when {\n            // Real surfaces always get first chance to close. A Back press inside Quick Capture or\n            // New Project must not silently count as skipping the entire guided journey.\n            focusTaskId != null -> focusTaskId = null\n            showComposer -> showComposer = false\n            showQuickCapture -> showQuickCapture = false\n            showReminders -> showReminders = false\n            showControl -> { showControl = false; controlExpanded = false }\n            overlay != POverlay.NONE -> overlay = POverlay.NONE\n            guidedTourStepName != null -> {\n                settingsStore.markGuidedTourComplete()\n                settings = settingsStore.snapshot()\n                guidedTourStepName = null\n            }\n            else -> {\n                externalStudioId = null\n                tab = PTab.TODAY\n            }\n        }\n    }\n''',
    "back priority",
)

text = replace_once(
    text,
    '''                if (guidedTourStepName == CreatorGuidedTourStep.IDEAS.name) {\n                    guidedTourStepName = CreatorGuidedTourStep.PROJECT.name\n                }\n''',
    '''                if (guidedTourStepName == CreatorGuidedTourStep.IDEAS.name) {\n                    externalStudioId = null\n                    tab = PTab.CREATE\n                    guidedTourStepName = CreatorGuidedTourStep.PROJECT.name\n                }\n''',
    "captured idea to project context",
)

text = replace_once(
    text,
    '''            onSecondary = {\n                if (guidedTourStep == CreatorGuidedTourStep.IDEAS) {\n                    guidedTourStepName = CreatorGuidedTourStep.PROJECT.name\n                }\n            },\n''',
    '''            onSecondary = {\n                if (guidedTourStep == CreatorGuidedTourStep.IDEAS) {\n                    externalStudioId = null\n                    tab = PTab.CREATE\n                    guidedTourStepName = CreatorGuidedTourStep.PROJECT.name\n                }\n            },\n''',
    "skipped idea to project context",
)

ROOT.write_text(text)
print("Applied guided first-run interaction fixes")
