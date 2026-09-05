from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]


def replace_required(rel_path: str, replacements: list[tuple[str, str]]) -> None:
    path = ROOT / rel_path
    text = path.read_text(encoding="utf-8")
    for old, new in replacements:
        if old not in text:
            raise RuntimeError(f"Expected follow-up copy not found in {rel_path}: {old!r}")
        text = text.replace(old, new)
    path.write_text(text, encoding="utf-8")


# Studio's expanded project view is shared by the current v1.7.5 Studio screen.
replace_required(
    "app/src/main/java/com/framebynavin/app/ui/V07StudioScreen.kt",
    [
        ("PRODUCTION WORKFLOW", "YOUR PROJECTS"),
        ("Tap a project to open its pipeline right where it lives.", "Open a project to see what’s done and what comes next."),
        ("production pipeline", "project steps"),
        ("CURRENT · ${current.label.uppercase()}", "NOW · ${current.label.uppercase()}"),
        ("STAGE ${currentIndex + 1}/${template.stages.size}", "STEP ${currentIndex + 1} OF ${template.stages.size}"),
        ("NEXT ACTION", "NEXT"),
        ("FOCUS · ${currentStage.label.uppercase()}", "WORK ON · ${currentStage.label.uppercase()}"),
        ("COMPLETE STAGE", "MARK STEP DONE"),
        ("✓ Published workflow complete", "✓ Project published"),
    ],
)

replace_required(
    "app/src/main/java/com/framebynavin/app/ui/FrameByNavinV101BApp.kt",
    [
        ("Open a project only when you need its full pipeline.", "Open a project when you want to see all its steps."),
        ("STAGE DONE", "STEP DONE"),
        ("Studio holds the full pipeline.", "Studio holds all your project steps."),
        ("Precise timing", "Exact reminder timing"),
        ("Background reliability", "Allow background reminders"),
    ],
)

replace_required(
    "app/src/main/java/com/framebynavin/app/ui/V16CreatorIntelligenceUi.kt",
    [
        ("${video.title} has a ${YouTubeMilestonePolicy.label(24)} 24-hour result saved.", "${video.title} has a 24-hour result saved."),
    ],
)

print("v1.7.5 UX copy follow-up applied")
