# Backlot six-stage implementation audit and PR conflict matrix
**As-of:** 2026-11-16. **Scope:** evidence-led review and planning only; NO merge, refactor, deletion, production update, feature implementation, release or owner-data mutation.  
**Reference:** owner-tested frozen V337 `678502f4626150ea1945b81eed1593a362377454`; approved P0 candidate `815ba69775f75b6e5f25bb838c2c17b4f09c203e` (draft PR #389).

## Executive decision
**Do not merge or cherry-pick the old six-stage pull requests on top of V337.** Their chosen implementations are already present in V337 and the integrated P0 candidate. The older PR branches remain draft for review/traceability and have base branches stacked against one another, not the current V337. An indiscriminate merge risks duplicate resources, overwriting newer Premiere changes or reverting P0 Snooze safety.

The initial concern that “Stage 6 is versionCode 336 but P0 is 337, therefore all six features must be ported” is **incorrect**: the old Stage 6 *branch* is 336, while V337 is a newer successor containing its implementation. Evidence:
- PR [#377](https://github.com/rayadinaveen98-ship-it/framebynavin-android/pull/377) Stage 6 head `5f320f93...`, versionCode `336`.
- PR [#378](https://github.com/rayadinaveen98-ship-it/framebynavin-android/pull/378) Premiere **head equals frozen V337** `678502f462...`, base equals PR #377 head `5f320f93...`, V337 versionCode `337`.
- Exact blob identities between PR #377 branch, V337 and P0 for `V393FirstRunQuickStart.kt` (`143cf9bf...`), `V392WidgetSystem.kt` (`342f4d30...`), `V144GuideSystem.kt` (`648a60be...`) and `V144SettingsHub.kt` (`5a54fd87...`).
- V337 `Phase1HabitVoicePlaybackPolicy.kt` matches Stage 1; AndroidManifest registers **exactly three V392 replacement widgets**; no old AppWidgetProvider registrations. Later-stage changes intentionally supersede Stage 2's manifest and Stage 4's earlier scene-engine blobs.
- V337 `V391ReminderSceneEngine.kt` differs from Stage 4 head, because subsequent V394 Premiere work touched it. Do not replace it with an earlier blob.

## Selected historical six-stage path (all PRs remain open/draft)
| PR | Scope | Source/branch state in V337 | Disposition |
| --- | --- | --- | --- |
| [#368](https://github.com/rayadinaveen98-ship-it/framebynavin-android/pull/368) | Hide Settings Backup/Restore; retain local erasure | Settings source present, no Backup/Restore Settings entries; Advanced “Start fresh on this phone” remains | **Retain, do not replay** |
| [#369](https://github.com/rayadinaveen98-ship-it/framebynavin-android/pull/369) | Stage 1: 21-day TTS cadence/watchdog | Voice playback policy blob matches current V337 | **In source; audible QA open** |
| [#370](https://github.com/rayadinaveen98-ship-it/framebynavin-android/pull/370) | Stage 2: retire 12 prior widgets | Manifest subsequently replaced by three V392 widgets; removed providers remain absent | **In source; launcher upgrade QA open** |
| [#371](https://github.com/rayadinaveen98-ship-it/framebynavin-android/pull/371) | Stage 3: Spark/Mira art, preferences, notification icons | Companion system blob matches; art present per stage inventory | **In source; artwork/identity QA open** |
| [#375](https://github.com/rayadinaveen98-ship-it/framebynavin-android/pull/375) | Selected Stage 4: six Voice/Alarm scene backgrounds | Engine is present and later extended by V394; verify active appearance selectors | **In source; visuals/performance QA open** |
| [#376](https://github.com/rayadinaveen98-ship-it/framebynavin-android/pull/376) | Selected Stage 5: 3 redesigned widgets | V392 widget system exact blob matches and manifest has 3 providers | **In source; launcher/device QA open** |
| [#377](https://github.com/rayadinaveen98-ship-it/framebynavin-android/pull/377) | Selected Stage 6: Creator/Personal/Both local-first quick start | `V393FirstRunQuickStart` matches V337; first-run flows not physically certified on fresh installs | **In source; onboarding/device QA open** |
| [#378](https://github.com/rayadinaveen98-ship-it/framebynavin-android/pull/378) | Experimental native Premiere scenes, opt-in, V394 | V337 head equals #378 head | **In source; experimental, not approved final UX** |

**Alternate/double implementation branch — do not combine blindly**
| PR | Alternative | Major shared edit surfaces / conflict risk | Decision |
| --- | --- | --- | --- |
| [#372](https://github.com/rayadinaveen98-ship-it/framebynavin-android/pull/372) | Earlier Stage 4 scene engine, `V391ReminderScenes.kt` | Same Creator `ReminderCheckInScreen`, Personal `PersonalAlertActivity`, Settings and navigation as selected #375; distinct scene-engine implementation | **Superseded candidate; no merge** |
| [#373](https://github.com/rayadinaveen98-ship-it/framebynavin-android/pull/373) | Earlier Stage 5 widget family, `V392WidgetGalleryActivity` and other layout names | Manifest, TaskStore, Phase1HabitStore, Settings, V392WidgetSystem overlap with chosen #376 | **Superseded candidate; no merge** |
| [#374](https://github.com/rayadinaveen98-ship-it/framebynavin-android/pull/374) | Earlier focus-first onboarding, `V393FirstRunJourney.kt` | `FrameByNavinV101BApp.kt`, guide tests, Settings/navigation and account entry points overlap with chosen #377 | **Superseded candidate; no merge** |

## P0 and verification PR lineage — no duplication
| PR(s) | Meaning | Status at audit |
| --- | --- | --- |
| [#379](https://github.com/rayadinaveen98-ship-it/framebynavin-android/pull/379) | Fail-closed recovery tests and normalized malformed-archive error | Included in P0 candidate |
| [#380](https://github.com/rayadinaveen98-ship-it/framebynavin-android/pull/380), [#381](https://github.com/rayadinaveen98-ship-it/framebynavin-android/pull/381), [#382](https://github.com/rayadinaveen98-ship-it/framebynavin-android/pull/382), [#383](https://github.com/rayadinaveen98-ship-it/framebynavin-android/pull/383) | Actual alarm, reboot rearm, forced Doze, denied-permission diagnostics | Isolated test-only CI success, **not** evidence for every OEM on integrated SHA |
| [#384](https://github.com/rayadinaveen98-ship-it/framebynavin-android/pull/384) | Reproduced old Alert Health defects | Green *diagnostics* mean old behavior reproduced, **not repaired** |
| [#385](https://github.com/rayadinaveen98-ship-it/framebynavin-android/pull/385) | Recovery + Alert Health fixes | Included in #389 |
| [#386](https://github.com/rayadinaveen98-ship-it/framebynavin-android/pull/386) | Fix 5 old AndroidTest compilation errors | Included in #389 |
| [#387](https://github.com/rayadinaveen98-ship-it/framebynavin-android/pull/387) | Real 10m Snooze synthetic-device test and diagnostic records | Included in #389 |
| [#388](https://github.com/rayadinaveen98-ship-it/framebynavin-android/pull/388) | Millisecond-accurate Snooze occurrence validation | Included in #389. Isolated #388 JVM failure was inherited two unrepaired V337 recovery tests; #389 resolves them. |
| [#389](https://github.com/rayadinaveen98-ship-it/framebynavin-android/pull/389) | Integrated P0 corrections and all test compiler repairs | Exact SHA `815ba697`: [JVM+Android 15 CI run 43855917050](https://github.com/rayadinaveen98-ship-it/framebynavin-android/actions/runs/43855917050) SUCCESS; **draft/unmerged** |

## Scope collision map and risk ranking
| Risk | Hot paths | What could go wrong | Required handling |
| --- | --- | --- | --- |
| **P0 critical** — Snooze receiver regression | `Phase1HabitLiveDelivery.kt` touched #369/#371/#388/#389 | Applying older Stage 1/3 version overwrites exact-millisecond validation; silently loses delayed notification | Treat #389 as candidate canonical; diff and tests required for any future changes to this file |
| **High** — Split Stage 4 alternatives | `ReminderCheckInScreen.kt`, `PersonalAlertActivity.kt`, `V144SettingsHub.kt`, V391 engine | Competing APIs/art paths, break Done/Snooze callback, screen presentation or Premiere opt-in | Audit V337 active composition/callbacks before approving new artwork |
| **High** — Split Stage 5 alternatives | `AndroidManifest.xml`, V392 widgets, stores, XML/layouts | Duplicate providers or wrong widget IDs, stale launcher placeholders, incorrect private labels | Do not cherry-pick #373; test exactly 3 registered providers + launcher upgrade |
| **High** — Split Stage 6 alternatives | `FrameByNavinV101BApp.kt`, `V393FirstRunQuickStart`, guide tests | Forced account/login, fake creator profile, stale partial setup overwrite | Keep #377-compatible semantics; test clean/returning/partial scenarios |
| **High** — Settings / internal backup surfaces | `V144SettingsHub.kt`, manifest `BackupActivity` and `PersonalBackupActivity` | Accidental UI reactivation or unsafe deletion of internal dependencies | Keep user-facing links absent and Advanced local erase. No cleanup without separate scope |
| **High** — 21-day audiovisual parity | Native V394 Challenge Premiere **preview only**; challenge audio unchanged | Mistakenly ship demo “Day 08” as actual progress; assume challenge voice + music exists | Explicitly **not implemented**; separate scoped design+implementation approval required |
| **Medium** — Stage 3 art and accessibility | Mira WebP, guide prefs, notification large icon | Mixed Spark/Mira identity, malformed cutouts or lock-screen privacy | Visual review at 64/96/144dp, 1.5x, TalkBack, locked-device public notification |
| **Medium** — Scene/performance budget | `V391ReminderSceneEngine.kt`, V394 Premiere | High GPU/battery usage, text clipping, gesture interference | Reduced motion, power save, low/mid device fps/memory and callback validation |
| **Medium** — APK upgrade identity | V336 branch, V337 installed QA preview, public preview signer | Android rejects version downgrade or signer mismatch; potential data loss if uninstalled | Version 338+ proposal; exact signer continuity and non-destructive upgrade checks |

## Functionality / experience status as of audit
**Verified as shipped in owner V337 source:** original six-stage implementations, 3 V392 widgets, Spark/Mira selection, Creator/Personal/Both onboarding code, classic cinematic V391/V394 opt-in scenes, hidden backup/restore Settings actions, retained local erase. This is **source/inventory** evidence only, not proof all features are usable and polished on a physical device.

**Verified on P0 combined emulator:** actual 10-minute 21-day Snooze, missing-channel health contract, full JVM suite, all instrumented tests *compiled*. This does NOT mean every instrumented test *ran*. Owner replied “ok all good” about the owner-testing APK; scenario-level physical evidence absent.

**Not accepted as production complete:** fully integrated challenge cinematic alert screen, 21-day voice+music parity, device-by-device launcher migration, Mira art quality, TalkBack/reduced-motion UI, low-power performance, fresh onboarding matrix, secure upgrade/rollback, private release signing, release/publishing.

## Proposed authority and workflow
- Treat **V337 frozen** as unchanged rollback/reference; treat **#389** as P0-tested implementation candidate, **not** merged or released.
- Prepare an inventory/QA-focused next candidate **based on #389**, and only after reviewing this documentation. No speculative “re-integration of all six stages”.
- Require acceptance criteria per domain and explicitly authorized production scope before modifying reminder visuals or 21-day audio behavior.
- Keep all current PRs open/draft during this audit; no cleanup, squash, merge, deletion, version bump or APK generation authorized by this document alone.
