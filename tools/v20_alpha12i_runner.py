from pathlib import Path

materializer = Path("tools/v20_alpha12i_content_dna_projects.py")
source = materializer.read_text()
old = "'    val acknowledgedCheckpointDueAtMillis: Long = 0L,\\n    val workspace: CreatorContentWorkspace = CreatorContentWorkspace(),'"
new = "'    val acknowledgedCheckpointDueAtMillis: Long = 0L,\\n    /** Content Project 2.0 workspace. Missing legacy data decodes to an empty workspace. */\\n    val workspace: CreatorContentWorkspace = CreatorContentWorkspace(),'"
if old not in source:
    raise SystemExit("CreatorTask anchor patch target missing in materializer")
materializer.write_text(source.replace(old, new, 1))
exec(compile(materializer.read_text(), str(materializer), "exec"), {"__name__": "__main__"})
