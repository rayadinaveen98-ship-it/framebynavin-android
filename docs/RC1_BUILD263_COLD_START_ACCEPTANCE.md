# Backlot RC1 — Build 263 preview, cold-start integrity

## Scope
Freeze from V229.5 Build262 QA-green SHA `8d7e9b5e5e39ace90a877b91a67eebd9e0762ef2`. The candidate is an **unmerged stacked branch**, not a consolidated production release and not a Play Store binary.

## Observed cause and fix
The earlier V222 fix already snapshots `CreatorViewModel.tasks` during Home composition, preventing stale `remember(tasks)` state when DataStore loads into the **same** `mutableStateListOf`. Existing V222 UI tests cover that.

Another startup gap remained: `PTodayScreen` received an initially empty task list before the first authoritative `TaskStore.tasksFlow` emission and displayed the empty-account CTA in the meantime. `CreatorViewModel.tasksLoaded` existed but was a private non-observable Boolean. In RC1:
- Expose an observable, read-only-to-UI `tasksLoaded` Compose state. It becomes true **only** once the canonical initial project snapshot has been loaded; no fake timer or guessed completion.
- Pass it through `FrameByNavinV101BApp` into `PTodayScreen`.
- While the queue is empty and the first project snapshot has not loaded, render *Opening saved projects…* instead of a claim that no work exists or an empty-project CTA. Once loaded, the real project or real empty-state appears normally.
- Keep the existing mutable StateList snapshot logic, event routing, reminders, alert playback, project editing, workflows, media, backups and persistence untouched. No data migration or seed/import.

## QA gates
- [ ] Full JVM tests, lint and owner APK build for Build263.
- [ ] Same package id, signing certificate and upgraded `versionCode=263` vs QA-green Build262.
- [ ] Android 11 and Android 15 emulators: prior V222 cold-start StateList reactivity test and new RC1 empty-before-load/project-after-load tests.
- [ ] Android 11 and Android 15 prior Personal Reminders, Spark voice, Workspace Publish and other regression tests.
- [ ] Cold-install empty account on physical phone: loading indicator then empty Home; no fake preloaded projects.
- [ ] Existing installed account: force-stop, launch, projects appear automatically without tab switching; ensure stable layout at 1.5x/2x font sizes.
- [ ] Upgrade Build262 to Build263 over an **existing installation** and verify projects, personal reminders, ideas, backup exports and widgets, with a backup beforehand.
- [ ] Confirm no Play Store submission using public prototype signing key.

## Remaining release risk
GitHub compare previously reported development head >1,200 commits ahead of main. This RC branch captures the current development stack but does not by itself audit every migration or grant stable release approval. See release tracker issue #174. Keep this PR draft until physical-device results and stack audit are complete.
