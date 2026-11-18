# Backlot UX07D — Connected Reminders and Groups QA gate, AFTER UX10

**Date:** 2026-11-17. **Status:** DEFERRED TEST PLAN — NOT EXECUTED.
**Exact parent:** UX07C PR #413 head `be8f00bb86d8e9dbdf4c07bed5dbac326731fa52`.
**Accepted stable:** V337 P0 `815ba69775f75b6e5f25bb838c2c17b4f09c203e`. Independent P0 ancestry reconciliation and original P0 Snooze/device regression still required.
**Scope:** new read-only connection projection; existing Space LINK chooser and Group UI only. No store/scheduler/schema/manifest/account changes.
**Execution agreement:** one consolidated UX01–UX10 Gradle/JVM, instrumented Android, physical device, privacy, backup/upgrade and owner acceptance campaign.

| ID | Priority | Condition / user action | Required observation |
|---|---|---|---|
| D01 | P0 | V337 user with Space links, multiple groups and overlapping reminder memberships upgrades | Exact IDs, links, schedules, pause state and Done history preserved |
| D02 | P0 | Group A and Group B reference same original Personal reminder | One canonical reminder; editing/pausing from either shows same state |
| D03 | P0 | Group link/unlink and Group delete | Original Personal reminder records, pending alarms, recovery receipts and other group memberships not deleted or duplicated |
| D04 | P0 | Space link/unlink and Space archive/restore | Personal reminder still owned by Personal store; no alarm change on link alone |
| D05 | P1 | Search Group names with mixed case, leading spaces or no match | Trimmed case-insensitive local search and honest no-result state |
| D06 | P1 | Group library empty with healthy source | Actual "No groups yet" and New Group option, not a fabricated error |
| D07 | P0 | Group DataStore corrupt/unreadable | Source error state, no invented empty groups or progress, no update/delete/bulk |
| D08 | P0 | Personal reminder source fails while opening Groups | Explain unavailable, no 0-Done or unsafe group actions |
| D09 | P0 | Personal completion history source fails while opening Groups | No implied 0 confirmations; group actions correctly gated |
| D10 | P1 | Group linked row due upcoming, paused, no upcoming | Real canonical next due and zone, never a fabricated delivery or Done |
| D11 | P1 | One linked reminder appears in 2+ Group records | Accurate count of other groups with original ID, no duplicate reminder |
| D12 | P0 | Group references a deleted Personal reminder | "No longer available"/stale-link state; safe Unlink, no new record |
| D13 | P0 | Bulk Pause All when some members already paused | Impact counts only changing originals; unchanged members not rewritten |
| D14 | P0 | Bulk Resume All when some already active | Same; only original pause states are updated after explicit confirmation |
| D15 | P0 | Bulk action on a Group with missing linked original IDs | Confirm disabled; original command also rejects, no partial write |
| D16 | P0 | All selected members already in requested state | No misleading pending change; confirmation disabled |
| D17 | P0 | Change Group membership while bulk confirmation open | Original `setAllPaused` expected IDs reject; no partial change |
| D18 | P0 | Delete linked Personal while bulk confirmation open | Original command rejects unavailable ID; no other Personal reminder changed |
| D19 | P0 | Group member also linked to another Group | Confirmation explicitly states shared effects; source updates visible to both groups |
| D20 | P0 | Bulk Pause/Resume confirmed, owner write succeeds, Android reconciliation fails | Error reports saved states may need attention; no claim alerts succeeded |
| D21 | P0 | Bulk Pause/Resume succeeds including widget/alert reconciliation | Saved-state feedback, actual pause states and schedules, no false Done |
| D22 | P1 | Group import/export on local device | Original format/checksum and owner confirmations preserved; no cloud full-reminder claim |
| D23 | P0 | Space Link chooser on a Space with 30+ saved Personal reminders | Filter/search finds original ID past first 24, no entire list rendering penalty |
| D24 | P1 | Space chooser with paused reminder | Paused original is linkable and remains paused after linking |
| D25 | P0 | Personal reminder deleted while Space link dialog is open | Canonical Personal IDs re-read before save; reject stale selection without a phantom link |
| D26 | P0 | Space archived or removed while Space link dialog is open | Fresh original Space state blocks new link; no accidental cross-Space write |
| D27 | P0 | Space links/Personal reminders unreadable | Chooser unavailable and original links unchanged; not "all linked" |
| D28 | P1 | Existing Space link chooser reopens or user switches Space | Query scoped/reset to exact Space; link choices exclude existing linked IDs |
| D29 | P0 | Space Linked Reminder Edit and Unlink existing actions | Exact owner record, safe unlink only, no unwanted time change |
| D30 | P1 | Group create/edit/rename/icon/color/export/import/delete, then navigate back | Every existing action and original test tag remains reachable |
| D31 | P0 | Activity focus and challenge manager coexist with Space & Group screens | No cross-domain Done, Snooze, alarm or group member leakage |
| D32 | P1 | 320dp screen, 200% font, landscape/tablet, TalkBack, switch/keyboard | Search, long titles, stale-link notices and confirmation actions accessible |
| D33 | P0 | UX06F P0 Snooze actual occurrence regression after cumulative UX07D | Millisecond matching and challenge v3 upgrade preserved on integrated candidate |
| D34 | P0 | Compare integrated candidate to approved V337 P0 fixes | All accepted baseline paths accounted for; no silent reset of ledger/restore protections |
| D35 | P1 | App process death with Space link chooser or group bulk modal open | No automatic link, pause/resume or other persisted mutation without explicit re-confirm |
| D36 | P1 | Search with non-Latin text, very long titles, shared groups and 5k max links | Stable local filtering, bounded dialogs, no data leakage or ANR |

## Deferred source-level test entries

`app/src/test/java/com/framebynavin/app/ui/UX07DPersonalConnectionPolicyTest.kt` stages four unit tests for group-name query, searched Space reminder eligibility, actual changing vs already-correct vs missing/shared bulk counts, and resume preview. They must be executed in the post-UX10 campaign with all existing P0, Personal Group and Space tests.

## Explicit stop-the-line / NO-GO

- Any lost, overwritten, duplicated or re-IDed Personal reminder/Space/Group data.
- Link/unlink accidentally schedules, deletes or completes the original reminder.
- Bulk Pause/Resume changes a linked source record without owner confirmation, or works with stale memberships/missing IDs.
- A failed Android reconciliation is falsely shown as a fully working alarm.
- Corrupt/unreadable Group source shown as an empty successful account.
- UX06F P0 Snooze fix, 21-day receipts or private photo recovery are lost.
- Cloud sync, photo upload, account deletion, billing, release signing or new permissions sneak into a UI-only package.

## Test evidence rules

Each test must have candidate exact Git SHA, CI run / Android version, PASS/FAIL/BLOCKED and repro; owner physical certification of the exact signed build is required before promotion. Do not treat authored test source as executed results.

**Current status:** 0/36 UX07D gates executed. Source staging and GitHub compare can verify file scope only, not Kotlin compilation, device performance or safe alarm delivery.

**Next handoff:** UX07E Personal/Challenge real progress and reflections. Preserve the source distinctions: manual Space log, Personal accepted Done, Challenge confirmed day, and Group link membership.
