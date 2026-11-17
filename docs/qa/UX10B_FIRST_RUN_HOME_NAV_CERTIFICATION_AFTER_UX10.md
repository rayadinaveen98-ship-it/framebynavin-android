# Backlot UX10B — First run, Home & bottom navigation certification

**Date:** 2026-11-17.
**STATUS:** QA cases AUTHORING ONLY; **0 TESTS RUN**. No Gradle/Compose/JVM, Android emulator, physical-device screenshot, signed APK, CI, merge, production release or stable promotion.
**Exact parent:** UX10A draft PR #423 `c9cba45e6d57f1c12df7f153547f6d5f453addf5`.
**Protected stable:** V337 accepted P0 `815ba69775f75b6e5f25bb838c2c17b4f09c203e`, not changed.
**Outstanding stable integration:** owner-approved 19 P0 commits affecting 22 paths are outside the staged UX lineage and must be reviewed before release-level certification.
**Founder's revised order:** UX10 now, UX09 deferred. A final feature-complete multi-device release requires later UX09 integration; UX10-only source work cannot certify sync.

| ID | Priority | Screen/action | Mandatory result/evidence |
|---|---|---|---|
| B01 | P0 | Normal app launch | Original persisted choice of 4 Backlot welcome styles, 3-second timeline, original companion and branding |
| B02 | P0 | Widget/deep-link reminder launch | Existing direct route; cinematic ident must not intercept action |
| B03 | P1 | Welcome Particle Signature with Android Remove Animations | Original static rendered ident, no transient light effects, no crash |
| B04 | P1 | Welcome Living Frame/Prismatic Bloom/Spark's First Light in reduced motion | Original static identity and accessible finish; saved style unchanged |
| B05 | P0 | Welcome preview and Choose in Settings | Preview silent and not persisted; chosen style persists only after original Choose |
| B06 | P0 | First install with no existing focus | First-run Creator/Personal/Both shows all three equally without falsely preselected Both |
| B07 | P0 | Focus tap then process restart | Exactly selected focus retained only after original preference store confirms save |
| B08 | P0 | Failed first-run focus save | Original error path and ability to retry, no silent transition |
| B09 | P0 | Personal-first selection | Original Personal Reminders route available without Creator profile mandate |
| B10 | P0 | Creator or Both focus | Original account Google/local, saved companion and quick/five-page Creator setup gates |
| B11 | P0 | Google sign-in, token cancel/error and username claim | No changed credential/session/account route, no private token leak |
| B12 | P0 | Continue without Google | Original local account route; no forced cloud or new permission |
| B13 | P1 | Account entry wording | Explicit limits of supported cloud backup; no claim UX09 automatic two-device parity |
| B14 | P1 | 320dp, font 1.0 on quick Creator setup | Single-column option chips >=48dp, no cropped labels |
| B15 | P1 | 360dp/393dp, font 1.0 on quick Creator setup | Original two-column options fit, accurate selection visible |
| B16 | P1 | 393dp, font 1.5/2.0 | Single-column options, all goal/platform/style options scrollable |
| B17 | P0 | Quick Creator setup orientation change | Original rememberSaveable name/mode/platform/goal/style retained |
| B18 | P0 | Quick Creator profile submit with omitted required field | Disabled CTA, no fabricated default Creator mode or saved state |
| B19 | P0 | Quick Creator setup valid save | Original normalized profile accepted once, Home opens only upon owner confirmation |
| B20 | P0 | Existing incomplete Creator account | Original five-page V18CreatorOnboarding remains, no reset or bypass |
| B21 | P1 | Legacy five-page large font and long mode choices | Larger eyebrow text, scroll/FlowRow, readable active labels |
| B22 | P0 | Optional Android permission guidance | All four exact original permission Review buttons, no forced grant, Continue remains available |
| B23 | P1 | Permissions large font/narrow phone | Detail text + Review button stack; no overlap or controls behind system bars |
| B24 | P0 | Personal guide 3-page next, voice toggle and Skip | Original selected companion, user voice preference and callback, no skipped save |
| B25 | P1 | Personal guide 200% font and 320dp | Last CTA, voice toggle and Skip button fully accessible; 48dp/56dp min targets |
| B26 | P0 | Actual Today Home first load | Do not show false empty before vm.tasksLoaded, no lost saved projects |
| B27 | P0 | Home no Creator projects | Original Create Project action intact, no fake XP or reminder |
| B28 | P0 | Home Up Next project swipe/cards | Exact stable saved IDs, no state mutation by swipe |
| B29 | P0 | Home Continue Work | Same original onContinueWork route, not direct workflow stage mutation |
| B30 | P0 | Home Focus | Same onFocus route and original haptic; no duplicated stage completion |
| B31 | P1 | Home Continue/Focus at 320dp or 200% font | Full-width stacked readable text, >=56dp targets, original test tags |
| B32 | P1 | Home Continue/Focus at normal font wide layout | Balanced two-button row, preserved original labels and IDs |
| B33 | P0 | Home original section order | Header/companion, Top Performers when linked, Up Next, Reminder Center, Spaces, Journey, Weekly Focus |
| B34 | P1 | Home read-only stats and Weekly Focus | 28 formerly <10sp labels readable; no fake published status |
| B35 | P0 | Bottom nav routes | Home/Ideas/+Create Project/Projects/Insights all match original destinations; Calendar separate |
| B36 | P1 | Bottom nav >=1.5–2.0 font 320dp | No 6.5sp label or clipped word; icon-only when needed, full TalkBack label and selected state |
| B37 | P1 | Bottom nav 393dp/normal and tablet | Readable >=10sp labels wherever space permits, 56dp items and full + button |
| B38 | P1 | Keyboard, TalkBack, gesture Back, screen rotation | Correct focus order, active item identity, measured nav clearance, no blocked Home content |
| B39 | P0 | Protected V337 accepted alarms, Snooze, reminder tokens, backup, account erase | No regression; stable lineage reconciliation before integrated test |
| B40 | P0 | Real owner-device integrated APK and all UX01–08 + UX10 surfaces | Physical screenshot/visual/functional sign-off tied to exact source SHA and APK hash, then founder release authorization |

## Authored policy tests, not executed

`app/src/test/java/com/framebynavin/app/ui/UX10BPresentationPolicyTest.kt` defines **6 unexecuted tests**: first-run single-column boundaries, Home adaptive controls, Projects/Insights readability at font scale, icon-only accessibility fallback thresholds, malformed geometry handling and deterministic viewport thresholds. **0/6 executed**; pure geometry tests cannot prove real 320dp/200% font screenshots, external-launch behavior, TalkBack or Android notification reliability.

## Stop-the-line: outstanding issues

- A missing/switched/unsaved first-run choice, disabled Google/local access or reordered account recovery is P0 NO-GO.
- A Home button directly changing project status (instead of routing to original owner) is P0 NO-GO.
- Missing/incorrect bottom nav tab or unreadable 6.5sp labels is release-blocking.
- Premature claim of full Cloud auto-sync or false backup coverage is P0 NO-GO; UX09 remains deferred.
- Regressed accepted V337 P0 full-screen/Android 15 alarms, Snooze, voice or local restore is P0 NO-GO.
- Designer tests must check dark/light, large font, Android accessibility, system bars and physical tablet. Neither this source nor QA matrix is executable proof.

**Current stage:** source-staged, no compiled or physical evidence, draft PR only.

**Next stage:** UX10C Creator projects, workflow stages, advanced Content Workspace and Idea Vault design density — preserve all actual source operations; author tests but do not execute until agreed integration readiness.
