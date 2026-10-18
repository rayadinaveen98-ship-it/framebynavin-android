# Backlot V163 — Quick Actions Phase 4: Daily Brief + Content Calendar

Base: V162 Phase 3 safe Idea Vault. Isolated Phase 4 preview; no stable merge without creator approval.

## Product contract
- Daily Brief presents a ranked **existing project** as the main focus with an actionable **Open Project** target.
- Every item needing attention links to its actual existing project. Never invent a completed action or create a task on tapping an advisory.
- "Reminder count" is accurately described as **project reminders in the next 24 hours**, not "reminders today."
- Brief calendar rows link by source: real project to its workspace; weekly schedule preview to Weekly Plan. "View Calendar" connects Brief to the full calendar.
- Calendar shows actual upcoming dates for 14 local calendar days and offers selectable day chips. It distinguishes "Project deadline" from "Weekly plan · not a project yet." A planned weekly slot is **not** proof of a created project or a scheduled Android notification.
- Recent, incomplete, unarchived projects past their deadline appear in a **Past-due projects** section (last 14 local days); no skipped, done or archived tasks there.
- Do not count past unfinished deadlines as future calendar entries; completed projects may remain in today's chronology.
- Preserve unique project ownership, original weekly slot preferences, original project IDs and timestamps. No database/schema migration or new job schedule.
- All controls use plain-language copy and the current navigation architecture. Reminders and Settings stay out of scope.

## QA
1. Static V161/V162/V163 contracts, JVM tests, Android lint, AndroidTest compilation and debug APK.
2. Device/emulator: opening focus project, nudge project, calendar project, weekly-plan preview and back navigation; date chip filtering, no items, completed/skipped/archived/overdue handling; local date boundaries.
3. Physical-phone: small screen, large font, TalkBack and midnight rollover; ensure old Idea Vault and existing reminders remain intact.
4. Signoff is required before merging any preview branches.

## Known release gate
V162 initially failed androidTest compilation because older UI tests called a now-expanded Idea Vault API. Commit af5fdbbf653580e6857cdc39410c2d3628a1a9b5 restores compatible default parameters; its CI and later V163 tests must pass. Do not interpret successful JVM tests as full release acceptance.
