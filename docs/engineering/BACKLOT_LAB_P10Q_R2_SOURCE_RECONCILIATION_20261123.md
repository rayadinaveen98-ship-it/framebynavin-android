# Backlot Lab × P10Q — R2 isolated source reconciliation

**Date:** 2026-11-23
**Current stage:** **SOURCE-STAGED / NOT COMPILED / NOT TESTED / NOT RELEASED**
**Founder-approved scope:** R2 isolated source integration; do not merge into production/stable, mutate Supabase, run CI/JVM/emulator after every edit, create/release an owner APK, or promote stable. The founder separately gates final consolidated tests and owner APK after reviewing the staged package.

## 1. Branches / exact source of truth

| Source | Ref | SHA |
|---|---|---|
| Lab owner-tested Nov 17 | `stable/backlot-lab-v337-owner-tested-20261117` | `4ba1263124d118d3917f988228aec2248068466f` |
| **Approved Lab + UX owner-accepted Nov 20 integration (R2 base)** | `stable/backlot-ux01-08-ux10-owner-accepted-20261120` | `abbe88984bac16cfafbd990d9cb00ed54cfdd07e` |
| P10Q newer verified Creator OS UX, reminder visuals, first-run test | `feature/backlot-v337-point10q-handoff-qa-trigger-20261123` | `4ad7d994252f2d342df9e54340d6b33233bf8ab3` |
| **R2 isolated integration, never main/stable** | `integration/backlot-lab-v337-p10q-reconciled-20261123` | See HEAD; at completion of this document's precursor: `f8bb5aeeae4bc0d031136e5a501c79d5fc0580a7` |
| Protected P0 fallback | `stable/backlot-v337-p0-owner-accepted-20261116` | `815ba69775f75b6e5f25bb838c2c17b4f09c203e` |

GitHub comparison relative to exact November 20 base at the end of source staging: **11 new R2 commits, 150 changed paths: 85 added, 38 removed, 27 modified**. These are *source paths*, NOT 150 newly tested features. Of those, 67 are production `app/src/main` paths and 28 are test files. All 18 shared-file changed paths from the independently developed P10Q and Lab branches have now been deliberately dispositioned. No merge into stable/main performed.

## 2. Reconciliation performed

1. **Base:** create a NEW isolated R2 branch from November 20 accepted Lab+UX source; carry forward all Lab-exclusive modules, P0 fixes, Creator project/idea/content workspace, progress/rewards, daily brief/calendar/quick actions, Personal spaces/journal and the original Lab cloud/build configuration.
2. **95 clean source imports:** port P10Q-exclusive non-destructive files, art and QA records by immutable Git blob SHA; do NOT replace the Lab application tree wholesale.
3. **Shared files — non-overlap merges:** `app/build.gradle.kts`, `BacklotReminderHubChrome.kt`, `VoicePersonaEngine.kt`, `VoiceReminderService.kt` receive disjoint three-way changes, retaining Lab signing/cloud setup and both voice features.
4. **Shared Personal/welcome:** `PersonalAlertActivity.kt` combines Lab disclosure/companion with P10 artwork; `V2232PersonalReminderHub.kt` preserves Lab Personal improvements; `V127AppearanceSettings.kt` and `V131LaunchGate.kt` use P10 look and signature welcome instead of the old selector.
5. **Unified Creator navigation:** `FrameByNavinV101BApp.kt` combines Lab-exclusive Creator/Personal routes and protected cloud recovery with P10Q `P6aUnifiedFirstRunPolicy`, one account choice, companion choice, **original FIVE-page Creator setup** and P6 real entry. Eliminated orphan Lab returning-account callback/obsolete first-run flag references. `V23AccountOnboarding.kt` uses the new P10Q sign-in/sign-up UI but retains the existing Google + CloudSyncManager authentication stack (the Lab backend is still selected separately via `CloudConfig`).
6. **Owner-requested Reminder Hub default:** `personalReminderSpace` starts `true`. Backlot's primary Home remains Creator OS. Explicit Creator deep links still set Creator, and Personal/Creator switching remains available in the shared Hub. The first-run Android UI test source was updated to expect **Personal initially**, then Creator and Personal roundtrip. This merged test is **STAGED, NOT RUN**.
7. **Settings without old collections:** `V144SettingsHub.kt` integrates Lab settings index/support and P10Q one-gallery design, Voice & Sound, Home photos, and Personalization. The P10Q compact `V140VoiceStudioPicker.kt` is paired with Lab `V403VoicePreviewPlayer` preview and cleanup. `V167SettingsNavigationPolicy.kt` redirects old saved REMINDER_SCENES section to Personalization rather than resurrecting removed collections. `V404SettingsIndex.kt` now searches for approved reminder backgrounds rather than Classic/Cinematic gallery.
8. **Idea Vault:** P10Q `P8IdeaVaultOverview`, empty/search responsiveness, layout and accessible cards, restored Lab UX05G **OPEN PROJECT** action on a canonical linked Idea, with a wrapping action row for small phones.
9. **Creator reminder surface:** P10Q authored Voice/Alarm Identity Emblem and palette become primary, while Lab Smart-stage Snooze `REMIND IN …` semantics/help and optional V407 companion moment remain. The accepted reminder action callbacks and keyguard safeguards are not replaced.
10. **38 intentional source removals:** all 32 P10Q-deleted legacy particle/scene files, retired test drivers/previews, plus six Lab-only old collection/quick setup files: `V391ReminderSceneEngine.kt`, `V394PremiereExperience.kt`, `V393FirstRunQuickStart.kt`, `V406CinematicReminderScenes.kt`, `V401WelcomeCollection.kt`, `V401WelcomePicker.kt`. The Personal alert's stray V336 palette reference was replaced by the current look. No old extra setup or Classic/Cinematic/Premiere collection should be user-reachable in the intended candidate.
11. **Future verification fence, not run:** `tools/qa/backlot_lab_p10q_recovery_contract_gate.sh` asserts Lab identity, newer UX, Personal Hub default, linked-project and Smart Snooze semantics, absence of retired source files and old first-run callbacks.

## 3. Critical Lab preservation contracts

- With opt-in `-PbacklotLab=true`, Android Gradle package is `com.framebynavin.app.lab` and user-facing title `Backlot Lab`; normal `com.framebynavin.app` remains the separate original application.
- `CloudConfig.kt` retains `BuildConfig.BACKLOT_LAB` switching. Lab target: `vasleafrmsovdbdlrcsw`, **NOT** original `kukkqgpzxnfanynbddiw`. Never build owner-preview with the original package/backend accidentally.
- Preserve original public development preview signing certificate for in-place Lab update (not a private Google Play upload key); confirm real APK signer before any device installation.
- **Live Supabase verification at diagnosis (read-only):** Backlot Lab project `vasleafrmsovdbdlrcsw` was `ACTIVE_HEALTHY`, had four RLS-enabled public application tables and installed migrations for Lab fresh install, private voice Storage policies and UX09 Creator Project/Idea records. No user rows were read and no backend changed in R2.
- A UX09 migration being installed **does not certify** complete per-record Android sync, conflict resolution, offline journal, backup/restore or full account deletion. Keep these as independently gated.
- No original backend, Google client secret/service-role key, user-owned data, account identity, deployment or paid service was changed. The Lab public publishable key remains in existing source; no private credentials staged.

## 4. Gate failures/risks NOT yet resolved by source staging

- **Kotlin compilation NOT executed.** Large independent UI trees were reconciled by source review/targeted three-way composition; hidden references to intentionally retired classes, duplicate symbols, missing imports, Kotlin type errors or behavioral regressions may exist. Do not call this implementation tested or owner-installable.
- **Android instrumentation/JVM NOT executed on merged SHA.** Previous P10Q 2/2 Android 15 smoke applies ONLY to standalone P10Q source SHA, not to R2 combined SHA or Lab package ID.
- **Lab OAuth not exercised** against the combined P10Q account UI; credential management, package/SHA registration and actual Google sign-in remain a release gate.
- **P0 regressions:** exact Android Snooze token/millisecond identity, notification channels, action receipts, privacy/lockscreen, process death, offline data, Home/Personal buttons, Idea linking, Creator stage transitions and cloud authorization still need a single consolidated campaign.
- **Retired engines:** run a full source-symbol reference/compile fence when QA begins. Old Lab test classes may still reference retired scene choices; tests should be migrated or explicitly retired only with evidence, never hidden from CI to force a green run.
- **UX09 scope:** do not silently activate unfinished auto-sync, full backup/restore or account erasure.
- **Visual acceptance:** new welcome/companion/Voice Orb/Reminder Hub and the Lab's additional Creator/Personal feature modules must be checked on physical Android. No physical acceptance from an emulator alone.
- **Owner data:** never clear/uninstall an existing populated Backlot or Backlot Lab to force an upgrade; use fresh disposable emulator/data for the first verification pass, then owner-approved upgrade test.

## 5. Next stage: founder-gated consolidated verification, then owner APK

1. Founder reviews this **R2 source stage**, the exact branch, and remaining scope. Founder previously requested **NO automatic per-change tests**; run only when the founder says all staged work is done / start next steps.
2. Once authorized, **one consolidated CI campaign** on exact frozen R2 SHA: source contract gate; compile Lab variant `:app:assembleDebug :app:assembleDebugAndroidTest -PbacklotLab=true`; full JVM/lint + Android test compilation, diagnose and fix any failures narrowly (avoid blanket repeated builds).
3. Test Android 15 Lab installed app identity and first-run navigation, account local/Google path (real OAuth with owner), original five-page setup, Creator Home, Personal-default Reminder Hub, Creator switch, Input/Idea Vault linked project, Stage Desk/Output Learning, Personal spaces, music/voice/actual reminder actions, P0 Snooze, lock-screen disclosure, camera/photos and offline conflict gates as approved.
4. Verify Lab-only Supabase target and signing certificate from compiled APK; prevent old-backend writes. Apply no database migration unless separately approved and signed RLS/security gate passes.
5. After CI and emulator evidence, produce separately identified **non-debuggable personalStable Lab preview APK**, verify signer/package/hash and only then share with owner for physical testing. Do not promote stable automatically.
6. Keep old stable SHA and rollback path, explicitly obtain owner device acceptance before any new stable promotion. Play Store release and full UX09 activation remain separately gated.

**R2 exit:** all requested source stages reconciled on isolated branch, but **build/test/owner-preview/release remain PENDING**. No PR merge, backend mutation or release action is authorized in this stage.
