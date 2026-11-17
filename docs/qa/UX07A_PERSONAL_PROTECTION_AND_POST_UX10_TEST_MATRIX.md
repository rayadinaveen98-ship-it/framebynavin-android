# Backlot UX07A — Personal protection, migration and post-UX10 QA matrix

**Date:** 2026-11-17.
**State:** DEFERRED TEST SPECIFICATION / NOT RUN. This is not an emulator run, Gradle build, physical review, backup certification or Play release.
**Source:** UX07A stacked on UX06F draft #409. Protected stable V337 P0 `815ba69775f75b6e5f25bb838c2c17b4f09c203e` unchanged.
**Founder sequencing:** stage UX07A–F and UX08–10 source changes first; run one consolidated UX01–10 campaign afterward. Existing individual UX06A–E and UX06F F01–F30 gates remain required.

## 1. Core risks to test (source-identified)

- **P0 data corruption:** Room `V289SpacesDatabase` schema v2, its v1→v2 private-photo migration, Persona DataStore, 21-day v3 and groups JSON must be preserved across upgrade/restore; no reset to empty when corruption/read fails.
- **P0 destructive actions:** deleting a Space can cascade Room-owned entries and link rows; it must NOT delete Personal reminders. Saved/interrupted private photos are explicitly resolved first. Group deletion must never cascade to reminder records; bulk Pause/Resume **does** intentionally mutate source reminders and requires separate owner confirmation.
- **P0 privacy and backups:** photo JPEGs are app-private, EXIF stripped, excluded from metadata JSON/cloud; confirmed Done receipts are rejected from owner-imported Space metadata. Lost/revoked photo bytes must not be advertised as cloud restored.
- **P0 completion truth:** Personal Done is accepted by owner-issued token/occurrence; 21-day Done is manual in challenge owner; note, photo, delivery, Snooze, Dismiss, navigation and routine preview never auto-earn credit.
- **P1 navigation:** Home Space preview exact-ID deep links; Personal Activity and Challenge focus; old Today/Settings toolbar/filters; no stale multi-select after scope switching.
- **P1 presentation:** 320dp width, 200% text, TalkBack, focus, landscape/tablet and permission degradation.
- **P0 integration:** UX06F is a draft not identical to accepted V337 P0 stable; full previously accepted P0 lineage must be reconciled before accepting any integrated candidate.

## 2. Deferred UX07A–F acceptance matrix

| ID | Priority | Setup / action | Required result |
|---|---|---|---|
| P01 | P0 | Upgrade with existing V337 Spaces, old archived Space, links, manual entries, notes, photo rows and files | IDs, order, timeline, exact file checksums, archive state survive |
| P02 | P0 | Open Home with no saved Spaces DB | Home preview doesn't accidentally create DB; first-create opt-in |
| P03 | P0 | Open a Space with linked reminder, then edit reminder elsewhere | Link remains ID-based, same canonical item, no duplicated schedule |
| P04 | P0 | Delete a Group with linked Personal reminders | Group removed only; reminders and schedules unchanged |
| P05 | P0 | Pause All/Resume All in group with shared members across groups | Confirmation names cross-group effects; original source reminders change once each, no corruption |
| P06 | P0 | Re-open group with missing/stale ID | Missing link displayed, unsafe bulk action blocked, unlink safe |
| P07 | P0 | Delete Space with saved photo rows | Blocked until explicit removal; don't orphan private files or delete Personals |
| P08 | P0 | Delete Space with interrupted photo file | Requires resolution; no silent auto-discard; unknown files remain untouched |
| P09 | P0 | Photo insert → crash before Room row → owner RECOVER/DISCARD | Exact UUID/path/checksum checks, successful recovery or confirmed discard only |
| P10 | P0 | Original photo with EXIF orientation/GPS | Correct display, re-encoded app-private JPEG with source metadata absent |
| P11 | P0 | Photo larger than supported bounds/corrupt/revoked file | No partial metadata claim, preserved originals, actionable error |
| P12 | P0 | Export local Spaces metadata and import it to synthetic existing profile | SHA/schema verified, append/merge conflict fail-closed, no duplicate IDs, no false Done receipts |
| P13 | P0 | Try to restore photo bytes from metadata-only export | Clear notice bytes are excluded, no successful-photo claim |
| P14 | P0 | Group JSON export/import with nonexistent reminder IDs | Verified group metadata-only semantics, safe missing-link state, no fabricated reminders |
| P15 | P0 | Personal Done vs Snooze vs Dismiss; then inspect Space, Group, Progress Journey | Only accepted Done receipt reflected as Done; notes/logs tagged separately |
| P16 | P0 | Current 21-day Voice delivery, Challenge Snooze, explicit Day Done/Undo | No automatic Done, preserve UX06F millisecond Snooze matching and v3 codec |
| P17 | P0 | Legacy V297 challenge + existing current Challenge in one profile | Both remain separate, no format rewriting or loss |
| P18 | P1 | Home Space exact ID reference removed before tap | Show unavailable notice and actual overview; no wrong Space selected |
| P19 | P1 | Personal Activity link points to stale reminder | No wrong record or inferred action |
| P20 | P1 | Personal 4-step create/edit, cancel/reopen and back navigation | Original validation/save owner intact; no phantom alarm |
| P21 | P1 | My Day: Today, Needs Action, Next 7 Days, All, old selector | Counts/rows match actual canonical stores; no ghost entries |
| P22 | P1 | Bulk selection then change Personal agenda filter/Activity focus | Hidden selection cleared; confirm applies only reviewed Personal IDs |
| P23 | P1 | My Routines create/edit existing recurring reminder | Same Personal owner; no new routine source or altered repeat |
| P24 | P1 | Space manual check-in, note, optional numeric metric | Note doesn't add active-day metric; invalid numeric entries rejected |
| P25 | P1 | Space link creation when Reminder store unavailable | Existing state retained, linked ID not invented, suitable error |
| P26 | P1 | Force Space list, entries, photo rows or reminder history read failure | Unavailable/error distinct from truthful zero; no primary reset |
| P27 | P1 | Owner metadata backup write failure/partial file | No successful export message; existing data untouched |
| P28 | P1 | Photo baseline comparison with one/two photos | No fake improvement, no inferred health outcome, correct dates/space IDs |
| P29 | P1 | 320dp screen, 200% font, landscape/tablet, TalkBack, keyboard/switch | All preserved controls reachable, no destructive accidental press |
| P30 | P1 | No permissions/low power/notifications blocked; open Personal | Real status, safe access navigation, no claims of delivered alert |
| P31 | P1 | Personal progress opt-in OFF, then ON, review Creator/Progress Journey | Private titles/photos not leaked; owner preferences respected |
| P32 | P1 | Space archive/reopen, text/goal edits, view notes/photos | Archive doesn't delete previous data or linked reminders |
| P33 | P1 | Rapid Save/Back/repeated tap in Space create/edit/log/photo | No duplicate insert, no partial overwrite; source flags guard work |
| P34 | P1 | No Spaces/reminders/challenges/routines yet | Clear, accessible first-action and no invented activity |
| P35 | P1 | Cross-device sign-in without UX09 sync and local-only Space/photo assets | UI does not promise second-device parity; explicit scope disclosure |
| P36 | P0 | Consolidated candidate includes UX06A–F and accepted V337 P0 corrections | All 22 previously P0-changed paths dispositioned, P0 actual Snooze/device/ledger regressions pass |
| P37 | P1 | Multi-domain Personal + Creator Reminder Center + Space links navigation | No cross-store completion, duplicated alarms or rewards |
| P38 | P1 | Existing notification/Personal locked screen with Space notes/private photos | No private Space content on keyguard/notifications/diagnostics |

These P01–P38 cases are a summary. Full integration also requires UX06F F01–F30, existing P0 checks, all prior UX01–UX05 contract tests and owner-device visual certification.

## 3. Phase-specific implementation evidence to retain

**UX07A:** Exact parent SHA, inventory of every existing action and canonical owner, risk ledger, no production changes.

**UX07B:** Screenshot and UI-source evidence for Home hierarchy; navigation event list for Today/Settings/Activity/Creator/Create/My Spaces/Challenge/Groups; no new data writer.

**UX07C:** Space list/detail affordances, journal/photo privacy and recovery, notes/check-in distinction, archive/delete gate.

**UX07D:** Reminder-group Pause/Resume confirmation, shared group members, link/unlink; no hidden reminder deletion.

**UX07E:** Source-provenance for personal Done vs manual check-in vs challenge completion, real receipt timelines, no arbitrary gamification.

**UX07F:** Diff inventory, read-only/protected owners, UX08 handoff, frozen post-UX10 candidate and test selection.

## 4. Defect/no-go policy

- P0 loss, overwriting/remapping owner ID, private-photo exposure, destructive cascade, silent space import, false completed day or lost P0 Snooze fix: **STOP / NO-GO**, not waived by a polished UI.
- A P1 visual/readability defect is not silently accepted; record severity/repro and obtain explicit founder acceptance if a deferral is reasonable.
- Only report CI/test PASS with exact run link/SHA and artifacts; no green check merely from source inspection.
- Physical owner visual test must match exact built APK source, signer and package; keep V337 historical provenance and do not replace stable without approval.

## 5. Current evidence

- UX07A: file-by-file source inspection performed on verified UX06F head; action inventory and contract written.
- No implementation, compiled candidate, Gradle, CI, emulated/physical QA, import/export execution, photos read from a device, data migration, merge or release.
- **Executed cases: 0 / 38.** The remaining regression work is deliberately deferred until UX10.

**Next:** UX07B Personal Home source staging in its own stacked draft; this test matrix remains open until the founder-directed integrated QA campaign.
