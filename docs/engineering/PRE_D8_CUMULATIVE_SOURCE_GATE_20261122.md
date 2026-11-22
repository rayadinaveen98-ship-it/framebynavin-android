# Backlot — Pre-D8 cumulative source gate and integration readiness ledger

Date: 2026-11-22

**SOURCE-REVIEWED / NOT CERTIFIED.** This document records repository inspection and bounded source fixes. It is not proof of compilation, successful CI, instrumentation, real-device ringing, stable packaging or release quality.

## Strict founder gate

The founder instructed that source stages can be implemented together, but test execution, Gradle, lint, integration, CI, APK assembly and physical-device QA are deferred until an explicit final command such as **"Done bro, start next steps and give APK"**. Generic "continue / go ahead" after an individual stage is not automatic authorization to run the cumulative build.

**DO NOT** merge/rebase into main or stable, run a build/test/CI dispatch, modify any hosted backend, create a signed APK or activate subscriptions under this branch. Separate final owner acceptance and stable promotion are required after D8 evidence.

## Verified read-only comparison

- Repository: rayadinaveen98-ship-it/framebynavin-android
- Protected owner baseline / main at review: abbe88984bac16cfafbd990d9cb00ed54cfdd07e (read-only comparison showed main identical).
- Latest stage parent: feature/backlot-rx03c-personalized-greetings-20261122 @ 4ec9e0d7dd3abeb36777e4bdb9f173a339294d46
- Bounded source hardening branch: feature/backlot-pre-d8-source-hardening-20261122 (stacked directly on parent; no behind commits)
- Parent → hardening review: exactly three source files, three source commits.
- Cumulative owner-baseline → reviewed source branch: 176 commits ahead, 0 behind, 92 changed files at read-only comparison before adding this document.
- On the closure branch itself, read-only GitHub Actions run listing returned 0 runs; this does NOT prove other branches or earlier stages had no runs, nor certify source correctness.

## Closed in source during this review

1. RX02EntranceMediaStore.selected(): a malformed or wrong-type saved file preference previously could throw during the rendering of an actual live reminder. It now returns null on read error, causing existing RX02/RX01 default scene fallback. No media file or preference is silently reset.
2. RX03VoiceIdentityRegistry.saved(): malformed or wrong-type offline voice preference previously could throw during actual reminder TTS startup. It now returns null on read error, preserving the prior Android offline voice fallback. All existing speaker identity keys retained.
3. VoiceGreetingBuilder.firstName(): a cached email/URL/handle-like token can no longer be announced as a first name. When no credible known name is found, retain existing neutral "Hey there" greeting; existing morning/afternoon/evening and 00:00–04:59 neutral overnight greeting policy preserved.

No delivery scheduler, foreground service lifecycle, reminder DB schema, saved project data, cloud/Supabase tables, notification receiver, Android exact-alarm policies, OS full-screen permissions or auth routes were modified by this three-file hardening stage.

## Source stages included in the cumulative ancestry

| Stage | Source status | Purpose |
|---|---|---|
| UX12 D0 | Approved plan | Simple cinematic companion design and scope |
| D1–D3 | Staged | App ident, authentication/first run and integrated companion arrival |
| D4–D6 | Staged | Personal home, Idea Vault and Settings/appearance |
| D7 + D7 review closure | Staged | Three distinct reminder experiences, verified-identity double tap, optional companion shown consistently |
| RX02 | Staged | Entrance-only app-private gallery image/animated image/<=15-second video + consolidated Reminder Appearance Settings, preserving saved Classic/Premiere selection and delivery permission route |
| RX03 | Staged | Enumerate/select actually installed Android offline voice IDs for eight Creator styles; zero guarantee of eight unique speakers |
| RX03B | Staged | Three domain script sets (24 templates), separate opt-in default styles for Personal and Challenge, Creator revised scripts, unchanged reminder recurrence/action owner |
| RX03C | Staged | First-name daypart greetings on all spoken domains, only first utterance per cycle; Creator Alarm and preview alignment |
| Pre-D8 safety closure | Staged | Defensive preference reads and name privacy |

All statuses are **STAGED**, not **TESTED**, **APPROVED FOR MERGE**, **CERTIFIED**, or **RELEASED**.

## Remaining source or product risks

- Most important P0: Personal Reminder voice reliability under long idle, OEM restrictions, auto-retry, repeat, music, Snooze and true observed playback **not certified** (a notification posted is not audible success).
- RX02 source in-app video trimmer is NOT present. Gallery must supply a <=15s clip; imported motion photos may be still-only and live wallpapers need exported compatible files. Crop/reframe controls NOT staged. Decoder availability, player overlay, locked/unlocked privacy and 40 MB import behavior require physical testing. Custom media only local-device, not synced.
- RX03/RX03B cannot promise eight genuinely distinct speaker timbres on devices with one installed TTS voice. Premium original neural speaker licensing/provider/privacy/recurring fees need separate founder decision.
- Personal and Challenge packs are **per-domain defaults**, not independent per-reminder choices. Per-item fields require additional approved backward-compatible Personal/Phase1 data-model, codec, editor, backup/restore and downstream sync scope. Do not covertly expand scope.
- English domain scripts are source staged, not independently professional Telugu localization. Mixed-language pronunciation needs listening.
- User-visible copy uses first name from locally cached authenticated Google identity, matching cached profile or Creator settings; no network request during live announcement. Do not confuse user identity with CreatorTask project title.
- D8 must check Compose compilation, Gradle artifacts, Media3 1.11.1 availability, ActivityResultContract, APIs 26–36, custom media decoding/battery, Kotlin getters/annotations and source-level assumptions. A read-only static scan is not a substitute for the tests.
- QA and APK must derive from *the final integration SHA* to avoid testing an old stage and shipping another. Keep hosted UX09/sync/backup integration explicitly separately gated.

## Exact D8 execution gate if founder authorizes later

1. Verify base/head SHA, source file inventory, current PRs and status of protected branches. Perform explicit cumulative integration in an isolated candidate; preserve all unrelated stable behavior.
2. Run full Gradle compilation, release-configuration sanity, Kotlin/JVM/unit source policy tests, Android lint, security/privacy checks; capture commands, statuses, logs, final source SHA and verify no workflow from untrusted PR executes with secrets.
3. Instrumented Android UI gate for D1–D7 flows; double tap single vs two taps, TalkBack action, saved visual modes, no keyguard title exposure, user preference, owner-verified occurrence/receipts, unlock and stale notification.
4. RX02 gallery import/cancel/size/format/duration/corrupt and low-memory/power-save paths. Compare 3 domains and both Voice/Alarm icon visuals; verify silent <15s video looping and player release, no accidental overlays of original CTA.
5. RX03 speaker identity inventory and optional/invalid saved choice, all 24 scripts and English/Telugu fallback, same sample speaker audition, actual greeting local times 04:59/05:00/11:59/12:00/16:59/17:00/23:59/00:00 and no leaked account email.
6. Observe Creator Voice+Alarm, Personal Voice+Alarm and 21-Day Voice+eligible Alarm across 1–5 repeat counts, music, Snooze, Done/Working/Close/Dismiss/reschedule, lifecycles and device sleep. Separate P0 overnight test evidence from fast emulator/smoke.
7. Test first-run, Google login, legacy Project/Personal user data, privacy/local storage and service OS restrictions on at least representative physical phone + Redmi Pad 2/Android OEM. Report Android OS/build numbers and real outcomes; do not infer certification from app starting.
8. Package and verify only authorized signed **Lab APK** with checksum and build provenance, share for founder acceptance. **Never** promote stable or production-release without distinct approval.

## Decision state

No newly authorised design/features beyond the reviewed source closure. RX04 premium neural voices, per-item Voice packs and advanced in-app video editing remain proposals. D8 integration/build/QA/physical APK held until explicit founder instruction.
