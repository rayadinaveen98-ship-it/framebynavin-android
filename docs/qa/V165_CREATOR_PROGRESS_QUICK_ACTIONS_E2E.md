# Backlot V165 — Phase 6 Creator Progress + Quick Actions acceptance

Base: V164 `a4dfd91bd46664bb66e906fdb4b4ce6cfac24ffc`.
Purpose: bring the six locked Quick Actions phases through one explicit end-to-end acceptance gate without merging into stable or altering Settings.

## Creator Progress product contract
- Default landing is **Your creative work**: level, recorded points, weekly actions, next actual milestone. It must be legible with no reward activity.
- The next step uses the recorded achievement state and existing navigation (Idea Vault or Projects); it never creates an award or marks a project published.
- Achievements, creator traits and recent rewards are optional disclosures, not four dense mandatory sections. They remain fully accessible.
- The summary explains that points are **recorded in-app activity**, not public YouTube analytics, monetization, streak penalties or a personal-worth score.
- XP, level, milestone order, reward event keys, source timestamps, reward ledger, and backup schema stay unchanged. No fake streaks, auto-awards or destructive migrations.

## Cross-phase checks
| Phase | Real user journey | Acceptance |
|---|---|---|
| P1 V160 | Existing customized work and weekly plan after upgrade | Project IDs, creator choices, backup and original media survive |
| P2 V161 | Quick Actions → Quick Capture → Idea Vault | Correct destination, save only after accepted write, original audio retained |
| P3 V162 | Vault → move voice idea to Recently Deleted → restore → convert | Recording playable, no active deleted reminders, original project link retained, no duplicate conversion |
| P4 V163 | Daily Brief → focus project; Calendar → date → project/weekly template | One-tap navigation, correct source distinction, no ghost events, overdue projects separate |
| P5 V164 | Preview missing projects, cancel, confirm; switch Auto Plan; reset template | Preview inert, confirm required, existing/skipped/archived projects stay untouched |
| P6 V165 | New Creator Progress → next milestone action → relevant workspace → back | Awards grounded, accessible details, consistent navigation, no unintended reward writes |

## Automated gates
1. Run inherited V161, V162, V163 and V164 source guards, then the V165 guide/link guard.
2. Run all JVM tests, Android lint, AndroidTest compilation and assemble a separate V165 preview APK.
3. Android 11 emulator suite executes V161 Quick Actions, V162 Idea Vault recovery, V163 Brief/Calendar, V164 Weekly Plan confirmation and V165 Creator Progress tests.
4. Review CI logs and artifacts; do not assume queued or compile-only checks passed.
5. Recheck earlier V162/V163/V164 isolated workflow results and explain any failures rather than treating a new preview as stable.

## Physical-device sign-off: not automated
- Upgrade in place from last working release with a backup first; verify old projects, customized weekly slots, ideas and recordings.
- Capture real audio; delete/restore; play before/after. Try failed save and app restart to check ownership.
- Test Today, Quick Actions, Idea Vault, Daily Brief, Calendar, Weekly Plan, Automation, Creator Progress and native Back.
- Test offline mode, Android notification permission disabled, canceled and enabled reminders, and a completed background automation run.
- Test portrait on a compact phone, large font 1.3x/2x, TalkBack and touch targets. Confirm bottom navigation and dialogs are usable.
- No data reset, silent backup overwrite, forced reminders, default-schedule replacement, phantom activity or assumed publish.
- The creator must explicitly approve the tested APK before stable promotion.

## Release decision
V165 is a **preview candidate**, not a guaranteed stable version. Settings overhaul remains separate and follows release acceptance.
