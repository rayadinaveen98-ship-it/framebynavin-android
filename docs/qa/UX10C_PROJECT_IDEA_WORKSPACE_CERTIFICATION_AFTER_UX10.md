# Backlot UX10C — Creator projects, stages, Workspace & Idea Vault certification AFTER agreed source stages

**Date:** 2026-11-17
**STATE:** FUTURE QA SPECIFICATION ONLY — **0/40 checks executed; 0/7 new source unit tests executed**.
**No evidence:** no Kotlin/Gradle compile, JVM/CI, Compose instrumentation, Android/emulator, signed APK, device screenshots, TalkBack physical output, merge, release.
**Exact source base:** UX10B PR #424 head `9ddfe99fb1e63c145c53ba325cc7cfbc61a4773f`.
**Protected owner stable:** V337 P0 `815ba69775f75b6e5f25bb838c2c17b4f09c203e`; must reconcile original accepted 19-commit/22-path protective delta before any releasable candidate.
**Roadmap order:** UX09 account-level two-device sync explicitly postponed. UX10C is source-only, not a replacement for UX09 privacy, RLS, conflict or two-device tests.

| ID | Priority | Scenario | Must verify on exact candidate |
|---|---|---|---|
| C01 | P0 | Open original Quick Create from Home, Quick Actions or Projects | Same one original wizard, no duplicated draft or stage |
| C02 | P0 | Original Quick Create valid title/platform/format/due | Same normalized canonical CreatorTask saved once |
| C03 | P0 | Missing title / past deadline / invalid reminder time | Original save disabled/errors visible and no phantom task |
| C04 | P1 | Quick Create compact mode at 320dp, font scale 1.0 | All labels ≥10sp, correct gutters, 48dp chip / 56dp CTA |
| C05 | P1 | Quick Create at 200% font, large display size | Primary CTA and More Options reachable above system bars/IME |
| C06 | P0 | Quick Create More Options and return | Same seven-stage original form and entered draft, no reset |
| C07 | P0 | Seven-stage wizard review/change buttons | Original section numbers, validation and active red state, no automatic stage |
| C08 | P0 | Creator category/production style choice | Same original saved normalized source ID and options |
| C09 | P0 | Reminder plan, custom time, voice count and priority | Existing scheduling/permissions, no incorrect generated alarm |
| C10 | P1 | Wizard neutral card vs current/invalid step | Neutral hairline quieter; current and errors visually strong |
| C11 | P0 | Existing project notes edit and cancel/save | Same original content, no stale overwrite, same note |
| C12 | P0 | Project resume/reopen after process death | Original project/draft reload and all saved fields survive |
| C13 | P0 | Content Workspace entry from project | Original project ID and current stage chosen; not a cloned workspace |
| C14 | P1 | Workspace four tools at 393dp normal font | Exactly Plan/Script/Publish/Guide in original order as two rows |
| C15 | P1 | Workspace four tools 320dp and font scale 2.0 | One-column, readable descriptions, correct original actions |
| C16 | P0 | Plan click from adaptive tool card | Original Plan, original task/Workspace revision |
| C17 | P0 | Script click from adaptive tool card | Original Script Studio and no unexpected save or new script |
| C18 | P0 | Publish click from adaptive tool card | Original Publish Studio, explicit receipt required for publication |
| C19 | P0 | Guide click from adaptive tool card | Original Blueprint/Guide; no project step advancement |
| C20 | P0 | Stage Desk notes, tasks, evidence and links | Original IDs, expected workspace revision and source details retained |
| C21 | P0 | Stage Desk save versus advance | Saving stage materials never auto-approves or changes stage status |
| C22 | P0 | Unreviewed stage, revised project in other view | Original stale-revision guard, no silent overwrite on save |
| C23 | P0 | Original Stage approval/cancel and Creator XP | Only confirmed persisted receipt can award once |
| C24 | P1 | Stage Desk neutral/active surfaces | Quiet materials borders but visible errors, focus and authoritative stage |
| C25 | P0 | Creative Board / Output Craft / Output Learning | Source-backed links and journal/cut IDs unchanged |
| C26 | P0 | Real saved output marked ready vs published | No false external publication claim or status mutation |
| C27 | P0 | Legacy Content Workspace old project and stored version | Original editor remains usable; revision/restore not changed |
| C28 | P1 | Plan/Script/Publish Studios high-font / small device | 10sp+ labels readable with keyboard, action footer reachable |
| C29 | P1 | WorkspaceAdaptiveFrame edge-to-edge and IME | Single safeDrawing inset, save/cancel remain above system keyboard |
| C30 | P0 | Save/secondary action footer on narrow and large font | Exactly original writer; no duplicate save, cancel remains cancel |
| C31 | P0 | Idea Vault search, All/Inbox/Ready/Converted/Deleted, advanced filters | Original matching records, counts, title/content and filter state |
| C32 | P1 | Voice/Momentum/Converted idea badges at 200% font | Flow-wrapped tags, no 7sp compressed text or lost badges |
| C33 | P1 | Idea card with project link/convert/archive actions | Original actions remain fully accessible with 48dp targets |
| C34 | P0 | Open linked project from Idea Vault | Correct original saved project ID; nonexistent project error unchanged |
| C35 | P0 | Convert idea to project | Original durable conversion, linked identity, original idea retained |
| C36 | P0 | Archive/restore and Recently Deleted | No accidental removal/undelete or retention reset |
| C37 | P0 | Idea editor new Text/Voice and draft cancellation | Original voice/audio, discard and Save/Cancel semantics |
| C38 | P1 | Creator/Idea visual dark+Light, 320dp/393dp/tablet/landscape/TalkBack | Readable text, logical focus, correct contrast and no cropped CTA |
| C39 | P0 | Accepted V337 P0 alarm/voice/Snooze/ledger/backup restore + user privacy | Preserve 19 corrective commits/22 paths; no unrelated scheduler/data changes |
| C40 | P0 | Founder physical Android acceptance + all UX01–UX10 combined | Exact SHA/APK hash, actual successful Kotlin, JVM, CI, instrumented and real-device evidence; explicit owner authorization before stable |

## Authored source tests, execution deferred

`app/src/test/java/com/framebynavin/app/ui/UX10CWorkspacePresentationPolicyTest.kt` adds **7 unexecuted** source policy cases:
1. Narrow Creator Workspace -> one original tool per row.
2. Normal phone/tablet -> two columns, four original tools.
3. Accessibility text -> one column regardless of device size.
4. Invalid font-scale values -> safe single-column fallback.
5. Project wizard action min 56dp and 48dp touch floor.
6. Existing Creator project wizard supportive text readability.
7. Existing Idea editor save/cancel footer responsive breakpoint.

All prior original `V173`, `V227`, stage revision, `V405`, Writer, Creator workflow and Idea Vault tests remain expected in the cumulative campaign. These tests are **not compiled or run** now. Test authoring does NOT prove that Compose visual code compiles, that FlowRow layouts have no clipping, or that Android apps save correctly.

## Stop-the-line criteria

- Project/Idea/stage data lost, duplicate CreatorTask/Idea ID, unintended publish/stage transition or XP, stale-revision write bypass, detached media/voice recording, wrong linked project/Personal privacy exposure.
- Unreadable tiny text, clipped Save/Cancel controls at 200% font, mis-ordered focus, date/time picker behind keyboard, changed reminder permission/alarm or user action semantics.
- Cumulative missing V337 accepted P0 fixes, unreconciled backup/restore, Android 15 notification/Snooze delivery or user privacy requirements.
- New paid provider, secret in Android, unreviewed Supabase switch, false multi-device synchronization promise or release without owner signoff.

**Evidence to collect later:** exact integrated source SHA and APK hash, JVM/instrumented logs, original source validation/migration evidence, 320dp/393dp/tablet dark+light screenshots with 1.0/1.5/2.0 fonts, screen-reader focus, real sound/alert, app lifecycle/recovery and founder physical acceptance.

**CURRENT:** Source-staged only. UX10D Personal/Spaces/Challenge/Reminder visual audit is next. UX09 remains separately deferred and incomplete.
