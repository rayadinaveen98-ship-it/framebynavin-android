from pathlib import Path

materializer = Path("tools/v20_alpha12i_content_dna_projects.py")
source = materializer.read_text()

patches = [
    (
        "'    val acknowledgedCheckpointDueAtMillis: Long = 0L,\\n    val workspace: CreatorContentWorkspace = CreatorContentWorkspace(),'",
        "'    val acknowledgedCheckpointDueAtMillis: Long = 0L,\\n    /** Content Project 2.0 workspace. Missing legacy data decodes to an empty workspace. */\\n    val workspace: CreatorContentWorkspace = CreatorContentWorkspace(),'",
        "CreatorTask anchor",
    ),
    (
        "'        contentType: String,\\n        dueLabel: String,'",
        "'    fun saveTaskConfiguration(\\n        id: String?,\\n        title: String,\\n        platform: String,\\n        contentType: String,\\n        dueLabel: String,'",
        "ViewModel old signature anchor",
    ),
    (
        "'        contentType: String,\\n        contentDna: CreatorContentDna = CreatorContentDna(),\\n        dueLabel: String,'",
        "'    fun saveTaskConfiguration(\\n        id: String?,\\n        title: String,\\n        platform: String,\\n        contentType: String,\\n        contentDna: CreatorContentDna = CreatorContentDna(),\\n        dueLabel: String,'",
        "ViewModel new signature anchor",
    ),
    (
        'CreatorTask("v96", "V96", "YouTube", "Long-form", "Today", 1L, contentDna = dna)',
        'CreatorTask(id = "v96", title = "V96", platform = "YouTube", contentType = "Long-form", dueLabel = "Today", dueAtMillis = 1L, contentDna = dna)',
        "instrumented CreatorTask constructor",
    ),
]
for old, new, label in patches:
    if old not in source:
        raise SystemExit(f"{label} patch target missing in materializer")
    source = source.replace(old, new, 1)

materializer.write_text(source)
exec(compile(materializer.read_text(), str(materializer), "exec"), {"__name__": "__main__"})
