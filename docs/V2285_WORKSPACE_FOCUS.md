# Backlot V228.5 — Workspace Focus (first slice)

## Design rationale
Backlot already has full Plan, Script, Publish and Guide editors plus a truthful workflow stage and four workspace tools. The missing layer is concise, specific guidance from **already saved** content. This version adds a maximum of two prominent "Next useful moves" between those tools and the Project Steps, without creating a second editor or altering app navigation.

## Behavior
- A pure `V2285WorkspaceFocusPolicy.hints(task)` reads the canonical CreatorTask/workspace fields: audience, promise, hook, script, structured beats, checklist status and deliverable publishing status.
- Suggests a missing viewer promise (Plan), opening/script (Script), unfinished production checks (Plan) or missing/planned output (Publish). Prefers the editor corresponding to the current workflow stage.
- Each hint links to an existing editor. No writes, backend, cloud calls, stage completion, rewards, auto-publication or reminders. Never claims a deliverable was published when only planned.
- Completed/skipped projects do not receive work nags. If core materials are present, shows a modest informational message rather than "ready to publish".
- Two recommendations max, distinct editor destinations. No new schema, keys, backup migration or permissions.

## QA and rollout
- Build 257 stacked on Build 256, same app ID and signing.
- Pure JVM tests cover empty, partial, ready, published, completed/skipped and stage-priority cases; Android 11/15 workspace UI tests prove cards and route callbacks without editing project records.
- Physical phone QA remains required for small screens, fonts, scroll and avoiding conflicting draft saves. Keep PR draft until proven.
