# Backlot UX07C — Spaces and private journal certification, after UX10

**Date:** 2026-11-17. **Status: DEFERRED TEST SPECIFICATION; 0 tests executed.**
**Source:** UX07C stacked on UX07B draft #412 at `df75d09a539a7f825d0dbfaaf8891b327fd6ce69`.
**Protected:** accepted V337 P0 `815ba69775f75b6e5f25bb838c2c17b4f09c203e`; complete baseline P0 reconciliation remains a separate post-UX10 release gate.
**Sequence:** owner-approved one integrated UX01–UX10 compilation, JVM, instrumented, Android physical and restore/upgrade acceptance campaign.

## 1. Core cross-stage scenarios

| ID | Priority | Scenario | Pass criterion |
|---|---|---|---|
| C01 | P0 | Upgrade existing Spaces with all seven original persisted enum types | Original IDs, goals, types, order, archive flags and links unchanged |
| C02 | P1 | Spaces list with goal, no goal, custom and archived types | Correct title/category/goal/status; no invented activity count or user content |
| C03 | P1 | Tap Space card / Back / exact Home-deep-linked Space | Original corresponding detail, no stale journal/other Space data shown |
| C04 | P0 | Open My Spaces with no DB on first launch | No silent Room initialization until original owner opens/creates Space |
| C05 | P0 | Space entries load interrupted or fail | Unavailable, not "No notes" or zero logs; original records untouched |
| C06 | P0 | Space reminder links fail to read | No zero-links claim; linking/unlinking disabled; source reminders unchanged |
| C07 | P0 | Space photo rows fail to read | No misleading "No photos yet", private file actions hidden, records not deleted |
| C08 | P0 | Interrupted photo recovery scan fails | Recover/Discard blocked; Space delete blocked; unknown files left intact |
| C09 | P0 | Space detail reads initially loading while owner taps destructive menu | Delete disabled until ALL independent sources READY and original photo preflight passes |
| C10 | P0 | Space has existing saved photo | Delete blocked until verified photo ownership and explicit removal |
| C11 | P0 | Space has interrupted temporary JPEG | Owner must Recover/Discard; no silent automated destructive cleanup |
| C12 | P0 | Explicit confirmed Space deletion after valid safety preflight | Deletes only original Space-owned rows; linked Personal reminders unaffected; original disk recheck preserved |
| C13 | P0 | Save new private JPEG from gallery | Original orientation/metadata stripping, bounded JPEG/checksum and private storage unchanged |
| C14 | P0 | Photo picker cancelled, save cancelled, save fails, owner Delete cancelled | No surprise photo row, file deletion or extra permissions |
| C15 | P0 | Recover original interrupted photo, then reopen | Correct timestamp, filename, row and verified photo bytes; no duplicate |
| C16 | P0 | Owner Discard interrupted photo | Only exact original safe private copy removed, never source gallery |
| C17 | P1 | One/two/many saved photos and less/more than 14 days | Baseline comparison unchanged, new neutral EARLIER/LATER copy; no inferred improvement |
| C18 | P0 | Spaces metadata-only export/import | Same version/checksum and explicit owner confirmation; no imported Done receipts or photo bytes |
| C19 | P0 | Restore metadata JSON to new device | Explicitly no claim that private JPEGs, alarms or original reminders were restored |
| C20 | P1 | Space notes with 0, 3, 4+, long and non-Latin entries | Private Note views, Add, Show All/Less and real timestamps preserved |
| C21 | P0 | Add note then inspect progress | No false check-in, no increase in active-day/metric count |
| C22 | P0 | Save manual activity with optional validated metric | Original manual source, optional metric value/unit and history preserved |
| C23 | P1 | Activity history with source MANUAL/CONFIRMED_REMINDER_DONE | Accurate source label, no false Done/linked achievement |
| C24 | P0 | Linked reminder Done history still loading or unreadable | "Unavailable/loading", not fabricated zero confirmed receipts |
| C25 | P0 | Add/link/unlink/edit reminder in Space | Original Personal reminder owner and schedule; no clones, duplicate alarm or removed authorizations |
| C26 | P1 | Archive/restore Space, edit name/goal, old archived Space | Original actions and journal/history preserved |
| C27 | P1 | 320dp width, 200% font, landscape and tablet | Space cards, metrics, photo ADD and notes fit/scroll, primary actions accessible |
| C28 | P1 | TalkBack/switch, keyboard, reduced motion | Real action labels and tests tags, no interactive decorative art |
| C29 | P1 | Personal My Day embedded Spaces preview + full Spaces index | Same ID and source owner; no repeated photo decoding on Home |
| C30 | P0 | Integrated Android 15 P0 Snooze and Challenge v3 upgrade | Accepted V337 corrections, 21-day day receipts and source-owned actions survive |
| C31 | P1 | Package process death mid-load/mid-photo save | No false empty contents; original recovery offers owner choice |
| C32 | P1 | Repeated taps during owner write / owner mutation gate conflict | No double saves, duplicate journal IDs, lost saved entries, or false success |

## 2. Defer source test execution

Staged but not run: `app/src/test/java/com/framebynavin/app/ui/UX07CSpacePresentationPolicyTest.kt` validates saved enum label preservation, distinct manual note/check-in/metric provenance and explicit Done receipt label. Also require original `V289`/`V290` data and photo recovery test suites and new device UI coverage at integration, with exact source SHA and execution artifact.

## 3. Scope and no-go

**NO GO:** V337 owner stable regression, false confirmed Done, changed group/reminder identity, Space cascade erases Personal reminder, photo auto-upload, unsanitized JPEG, unguarded delete after read error, discarded interrupted bytes without confirmation, Room migration, backup/restore data loss or keyguard privacy exposure.

**Current recorded evidence:** GitHub source-only diff review and code inspection; **C01–C32 not executed**, no compile/CI/emulator/owner physical APK. Declared status remains **SOURCE STAGED / QA DEFERRED**.

**Next:** UX07D original links and Reminder Groups source refinement with confirmed Pause All/Resume All preserved.
