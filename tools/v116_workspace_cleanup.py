from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
PATH = ROOT / "app/src/main/java/com/framebynavin/app/ui/V19ContentWorkspaceAlpha6Ui.kt"
text = PATH.read_text(encoding="utf-8")

replacements = {
    '"Embedded · S${it.revision} · ${it.beats.size} beats · backup/restore ready"': '"${it.beats.size} script beats ready"',
    '"Legacy-compatible · embeds automatically on open/save/export"': '"Start or continue your script"',
    'Text("CONTENT PROJECT 2.0 · RC2", color = RecRed, fontSize = 8.5.sp, fontWeight = FontWeight.Black)': 'Text("PROJECT WORKSPACE", color = RecRed, fontSize = 8.5.sp, fontWeight = FontWeight.Black)',
    'Text("R${workspace.revision}", color = MutedGold, fontSize = 9.sp, fontWeight = FontWeight.Bold)': 'Text("${workspace.productionProgressPercent()}%", color = MutedGold, fontSize = 9.sp, fontWeight = FontWeight.Bold)',
    'Text("STABILIZATION DATA", color = ProjectorIvory, fontSize = 11.sp, fontWeight = FontWeight.Black)': 'Text("PROJECT SAVED", color = ProjectorIvory, fontSize = 11.sp, fontWeight = FontWeight.Black)',
    'Text("CREATOR WORKSPACE", color = ProjectorIvory, fontSize = 24.sp, fontWeight = FontWeight.Black)': 'Text("Your project", color = ProjectorIvory, fontSize = 24.sp, fontWeight = FontWeight.Black)',
    'Text("One project from planning to publishing. RC2 is stabilization-only: publication, script and reminder authority are being consolidated before stable.", color = MutedText, fontSize = 10.sp)': 'Text("Plan, write and publish from one place.", color = MutedText, fontSize = 10.sp)',
    'eyebrow = "CONTENT DNA · PERSONALIZED",': 'eyebrow = "PROJECT STYLE",',
    'body = "Recommended workflow, project prompts, writing structure, quality checks and useful working tools for this project.",': 'body = "A simple guide based on how you make this kind of content.",',
    'eyebrow = "PROJECT COMMAND CENTER",': 'eyebrow = "PROJECT",',
    'body = "Brief, research, assets, supporting checklist, templates, deliverables and learnings.",': 'body = "Brief, research, assets, checklist and outputs.",',
    'meta = "${workspace.productionProgressPercent()}% checklist · ${workspace.deliverables.size} outputs",': 'meta = "${workspace.productionProgressPercent()}% ready · ${workspace.deliverables.size} outputs",',
    'eyebrow = "SINGLE SCRIPT AUTHORITY",': 'eyebrow = "SCRIPT",',
    'body = "Hooks, title ideas, beats, narration, visuals, B-roll, notes and recording readiness.",': 'body = "Write your hook, narration, beats and visual notes.",',
    'eyebrow = "SINGLE PUBLISH AUTHORITY",': 'eyebrow = "PUBLISH",',
    'body = "Final metadata, title/cover variants, pre-publish gates, publication history and repurposing.",': 'body = "Prepare the title, cover, description and final publish check.",',
    '"Main workflow controls project stages/reminders. Script Studio controls writing. Publish Studio controls live output state."': '"Everything stays connected to this project."',
}

for old, new in replacements.items():
    if old not in text:
        raise RuntimeError(f"workspace cleanup marker missing: {old[:80]}")
    text = text.replace(old, new, 1)

# Keep the saved-state card visually quiet; it should reassure, not read like diagnostics.
text = text.replace(
    'Spacer(Modifier.height(18.dp))\n                Text("Your project"',
    'Spacer(Modifier.height(14.dp))\n                Text("Your project"',
    1,
)

PATH.write_text(text, encoding="utf-8")
print("v116 creator workspace simplified")
