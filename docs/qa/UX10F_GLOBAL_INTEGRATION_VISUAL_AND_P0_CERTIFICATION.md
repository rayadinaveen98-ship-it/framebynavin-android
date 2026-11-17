# UX10F — Global integration, visual continuity and P0 certification

**Date:** 2026-11-17.
**STATUS:** FUTURE ACCEPTANCE SPECIFICATION ONLY. **0/40 scenarios executed and 0/8 new UX10F source tests run.** No Gradle/JVM/Compose/CI/instrumented emulator, real-device APK, merge, release or publication.
**Parent:** UX10E draft PR #427, head `411b733dae308dcd3948d99c39faf840d443d059`.
**Protected stable:** accepted V337 P0 SHA `815ba69775f75b6e5f25bb838c2c17b4f09c203e`; 19 accepted corrective commits across 22 files absent from cumulative UX ancestry.
**Owner ordering:** UX10 before UX09. UX09 per-entity multi-device sync remains unimplemented and deferred, so this source-only checklist cannot be marked a full ten-point release certification.

| ID | Priority | Test case | Required outcome |
|---|---|---|---|
| F01 | P0 | V337 19 corrective commit ancestry | All 22 accepted files accounted for at exact integrated SHA; protected stable untouched |
| F02 | P0 | Phase1HabitLiveDelivery direct overlap | Corrected millisecond Snooze occurrence identity and no false stale suppression |
| F03 | P0 | Missing notification channel on Android 15 | App marks alert channel unready, never silently ready |
| F04 | P0 | Challenge voice failure/start evidence chronology | Later observed genuine status wins per exact occurrence, failure tie unsafe |
| F05 | P0 | Malformed restore archive | Fail closed, no destructive restore or cross-owner data |
| F06 | P0 | All tracked Android UI test sources | Compile them all; do not quarantine stale V339–V343 files or revive removed Backup UI |
| F07 | P0 | Real Android 15 10-minute Snooze | OS alarm/notification rearm and ledger evidence, no forged Done |
| F08 | P0 | Locked screen audible reminder and music | Real voice repeats, background music, permissions, accurate intent token |
| F09 | P0 | Creator and Personal source isolation | No wrong owner, account, reminder, project or photo visibility |
| F10 | P0 | Project/idea stage writes and XP | Original validated revisions, publish receipts, one event key, no false credit |
| F11 | P1 | Director's Cut and Light launch/Settings | Approved palettes and saved theme/companion/welcome style preserved |
| F12 | P1 | First launch normal vs deep link | Welcome timing/reduced-motion and notification/widget bypass preserved |
| F13 | P1 | Personal-first and Creator onboarding | All three focus choices, Google/local, saved form, permission choices valid |
| F14 | P1 | Home 320dp and 2x font | Continue/Focus/Quick Actions readable, no inaccessible last content under bottom nav |
| F15 | P1 | Bottom Home/Ideas/+/Projects/Insights | Same original tabs and TalkBack selected/full labels, no 6.5sp text |
| F16 | P1 | New Project Quick and seven-step wizard | Real date/reminder validation, editable fields, no clipped action |
| F17 | P1 | Advanced Content Workspace four tools | Original Plan/Script/Publish/Guide destinations, 1/2 columns responsive |
| F18 | P1 | Stage Desk and idea editor IME | Save/Cancel reachable, no implicit save or change in original revision |
| F19 | P1 | Idea Vault cards at 200% font | Voice/Momentum badges and original edit/archive/convert/project link accessible |
| F20 | P1 | Progress Journey historical real status | No false-empty view, consent-based Personal Done, real source IDs |
| F21 | P1 | Creator XP Spark/Mira celebration | Only persisted unique award, selected companion 3-second and reduced motion |
| F22 | P1 | Daily Brief source loading/error | No 0 fabricated before weekly load or task read, open original task |
| F23 | P1 | Content Calendar Week/Month/Agenda | Date source, 80-page, 90-day bound, selected date and readable grid |
| F24 | P0 | Quick Actions Skip confirmation | Correct owner/current task ID and reminder token, no unrelated skip |
| F25 | P1 | My Spaces metrics, notes and photos | True logs/active-days/linked count, private media metadata stripped |
| F26 | P1 | 21-day Challenge compact calendar | All 21 real dates, 4/7 columns, day/date TalkBack and truthful Done |
| F27 | P1 | Reminder Groups in high-font mode | Edit/Delete/Pause/Resume targets with original confirm/read guards |
| F28 | P1 | Personal Done/Snooze row at 320dp | Original delivery contract, no duplicate acknowledgement, actions stack |
| F29 | P1 | Settings navigation and search | Account/Voice/Theme/Privacy/Advanced all present, erase confirmation protected |
| F30 | P1 | Insights four original tabs | Overview/Content/Revenue/Creator, readable 108dp+ scroll or equal width |
| F31 | P1 | YouTube four metrics on small/large text | Same real source values, one column at <400dp or large font |
| F32 | P1 | Creator five metrics/drilldowns | Same five source values and exact target enum links; responsive grouping |
| F33 | P0 | YouTube channel/period source identity | No cross-channel stale figures, missing data not shown as true zero |
| F34 | P1 | Notification Center missing Creator read | Unavailable message, not loading or genuine empty, warning visible |
| F35 | P1 | Notification Center filters and Spark | Needs Action/All/History original IDs, refresh and never auto-acknowledge |
| F36 | P1 | Widget preview and pinned launcher actions | Synthetic preview only, no automatic pin/remove, 174/208dp source |
| F37 | P1 | Android screen sizes | 320dp, 393dp, tablet portrait/landscape, split-screen and edge-to-edge |
| F38 | P1 | Accessibility and motion | Text 1.0/1.5/2.0, TalkBack, keyboard/focus, reduced motion and high contrast |
| F39 | P0 | UX09 deferred sync promise | Release description does not claim multi-device parity without secure implemented tested UX09 |
| F40 | P0 | Exact candidate and owner acceptance | Successful complete build/JVM/Android/CI, real APK SHA/hash, founder physical signoff and separate release authorization |

## Test packages and sequencing

**Source authored:** `app/src/test/java/com/framebynavin/app/ui/UX10FContinuityPolicyTest.kt` contains 8 new **UNEXECUTED** pure policy tests covering narrow/normal/large-text metric layout, invalid scale, Creator acknowledgement read-error precedence, 21-day date count and common typography/touch target minimums. Cumulative UX10A–F: 44 authored / 0 executed source tests; 240 authored / 0 executed QA scenarios (40 each stage). No quality, code compilation or physical PASS follows from this math.

**Before testing:** specifically review all 22 V337 P0 missing paths, 1 direct overlapping `Phase1HabitLiveDelivery.kt`, 4 production corrections, 11 tests, 5 workflows/scripts and 2 QA documents. Preserve protected stable. Obtain founder approval for the isolated integration/reconciliation action.

**When founder authorizes end-to-end campaign after UX09 and P0 incorporation:** compile all Kotlin/Compose + Android instrumentation sources, complete JVM/unit/Room/recovery/Creator/Personal suites and CI XML, Android 15 notification-channel and real 10-minute OS Snooze experiment, verify actual voice+music/full-screen/lockscreen/reboot, then inspect phone+tablet dark/light with high font/TalkBack/reduced motion; certify exact integrated SHA, reproducible APK hash and owner physical acceptance.

## Source-level caveats

- UX10F has not been built; `RowScope.weight`, icon semantics and increased 10sp legacy cards must be verified in real Compose. Device shape and font selection can still clip a label even with clean code.
- Current widget migration/provider state remains an unresolved separate founder decision. Do not remove installed widgets or restore removed backup/settings UI to appease tests.
- No service-role credentials, Supabase project swap, Android account schema migration, private photo cloud upload or new service expense is approved.
- Known UX08 progress/weekly/error/Skip/celebration gaps still require focused integration tests; UX10 polish does not resolve these automatically.
- The real stable APK is NOT available from these source-only PRs. Any claim of physical tests passing without device logs/screenshots is invalid.

**Go/No-go:** Any P0 failure or unapproved server/widget/release change is an automatic NO-GO. UX10F source stage may be complete while the full Backlot project remains NOT READY FOR STABLE RELEASE.
