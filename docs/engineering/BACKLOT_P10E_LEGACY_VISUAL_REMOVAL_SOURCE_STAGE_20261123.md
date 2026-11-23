# Backlot P10E — Retirement of Old Reminder Looks (source-stage ledger)
**Date:** 2026-11-23  
**Founder approval:** "ok bro continue" following P10D. Owner explicitly requested removal of old/default reminder main screen, older looks, Scenes (aperture / sunrise / projector), Voice/Alarm backgrounds and Premiere alternative, while keeping Living Bloom / Pulse Studio / Rise Core plus per-scope local gallery images, animated media and video.

**Verified status:** Deleted and replaced obsolete source on an isolated cumulative branch. **NOT COMPILED / NOT TESTED / NOT CERTIFIED / NOT MERGED / NO APK.** GitHub compare is evidence of source changes only.

## Repository controls
- Repo: `rayadinaveen98-ship-it/framebynavin-android`
- P10E branch: `feature/backlot-v337-point10e-legacy-reminder-cleanup-after-p10d-20261123`
- Parent P10D: `feature/backlot-v337-point10d-gallery-visuals-after-p10c-20261123`
- Owner-accepted V337 stable SHA: `815ba69775f75b6e5f25bb838c2c17b4f09c203e`. Main and stable untouched.
- No signing/build/release, Gradle/JVM/CI/emulator/physical QA, hosted data update, schema/auth changes, migrations, cloud, user media deletion, backup/reset or changed reminder alarm/voice/music ownership.

## Removed: historical reminder-only visuals (actual files deleted)
1. `reminders/V391ReminderSceneEngine.kt`: obsolete Classic/Director/Aurora/Glasswave etc. voice/alarm picker, scene engine, independent visual preferences UI.
2. `reminders/V394PremiereExperience.kt`: old opt-in Premiere full-screen controls and renderers, aperture/sunrise/progress scene visualizations and experimental prefs UI.
3. `reminders/V336DirectorReminderVisuals.kt`: old full-screen crimson Director backdrop, particle signal clock/microphone and V336 motion clock.
4. `reminders/V195ReminderParticleScene.kt`: old one-tap particle microphone/clock entrance and floating dots.
5. Retired associated visual-only geometry/policies: `V195ParticleGeometry.kt`, `V197AmbientGeometry.kt`, `V198ParticleMotion.kt`, `V202PremiumParticleMotion.kt`, `V218UnifiedWavePolicy.kt`.

**These files are deleted from the P10E candidate branch.** Git history and historical documentation naturally remain as provenance; this is not a claim that no historic mention exists in Markdown or commits.

## Repaired active paths
- `P1ReminderEntranceHosts.kt`: removed unused P1 Personal gallery-only entrance and its private fallback renderer (which had embedded old V336 art). Kept exact `P1GalleryGatePolicy.challengeEligible` and real `P1ChallengeNotificationEntrance` after locked/current/pause/date checks.
- `P1EntranceBackdrop.kt`: removed P1 Creator particle underlay used only by retired V195 renderer; preserved on-device Photo Picker imports, private media decoding, muted Media3 loop, lifecycle disposal, framing, Android keyguard check and the P10 living-family fallback. Restored necessary Android Context/KeyguardManager imports after source review.
- Creator `ReminderCheckInScreen` now uses `P10ReminderVisualPolicy.startRevealed` for Android animation-off accessibility. It no longer relies on old V197 ambient-geometry class for this policy.
- Preserved P10A–D architecture, family-based Creator/Personal/21-Day presentation, double-tap, gallery selection, crop, custom video continuity and action ownership.

## Obsolete checks and test replacements
- Removed old unit-only V394 Premiere policy and V195/V197/V198/V202/V218 particle tests that only certified retired looks.
- Removed obsolete instrumented UI tests: V195 scene, V197 particle, V218 unified wave and V336 Director screenshot test. Their checks actively expected the deleted visuals and would be incorrect to retain as acceptance gates.
- Added `app/src/androidTest/java/com/framebynavin/app/reminders/P10LivingReminderVisualUiTest.kt`, which stages synthetic checks for Voice/Alarm Creator and Personal main-screen controls, P10 emblem presence, integrated information stages, and an explicit accessibility Open reminder action; no live delivery/service calls.
- Augmented `P10ReminderVisualContractTest.kt` to verify reduced-motion immediate access using the NEW policy.
- Added a deferred source gate at `tools/check_p10_retired_reminder_visuals.sh`; it asserts absent old renderer source files, absent old active Kotlin references and presence of new P10 mounts. **Not executed.** Must audit any failed source guard at consolidated QA.

## Old isolated proof/preview infrastructure retired, not production CI
- Removed 5 workflow definitions, specifically `backlot-premiere-native-proof.yml`, `backlot-v336-director-native-visuals.yml`, `backlot-v195-living-reminder-scenes.yml`, `backlot-v195-2-immersive-particles.yml` and `backlot-v218-unified-wave.yml`. They only targeted superseded isolated branches or manual old-look proof, some explicitly requiring V195/V336 source and old versionCodes.
- Removed old source-only shell checks `check_v195_living_scenes.sh` and `check_v197_immersive_particles.sh` because they would necessarily fail against approved P10.
- Unregistered **only** `:v218review` in `settings.gradle.kts` and deleted its Gradle config, manifest and V218-only Activity. It was a separate offline preview that copied and ran the deleted V195 particle sources; not a production Backlot reminder engine.
- Retained `:app` and independent `:v217review`. Normal Backlot GitHub workflows, build configuration, signing and release operations were deliberately not modified.

## Data and compatibility principles
- Old SharedPreferences files/keys (`backlot_reminder_scenes_v391` and `backlot_premiere_experience_v394`) are NOT deleted or migrated. Nothing reads them to choose a live P10 background. This preserves historical backup safety and avoids unapproved data loss; those records are now inert.
- The app-wide **Director's Cut / Backlot Light** application appearance choice is unrelated to the removed reminder-only Director visuals and must remain.
- Saved Creator/Personal/21-Day private gallery assets, crop settings, challenge status, snooze, voice and music preferences stay in existing stores; no data purge.
- Some historical UI test tags in current screens remain for stable action semantics, **not** to render the old visual code.

## Source review evidence and remaining risks
- GitHub compare with P10D parent shows branch is ahead, zero behind. Core active paths (MainActivity, PersonalAlertActivity, ReminderCheckInScreen, P1Backdrop, P1Challenge host, Settings UI and P10 presentation) were fetched and manually scanned for removed V195/V336/V391/V394 symbols; none found in that limited audited group.
- Full repository-wide symbol/linkage proof is **NOT possible from the current connector-only source inspection** (code search did not return indexed results). The new static source gate and the Android compiler are the definitive next checks. Do not call the whole codebase reference-free or "APK-clean" based only on limited source review.
- The removed dedicated old-review CI checks are not evidence of P10 quality. Newly staged P10 UI tests do not substitute for real Android screenshots, FPS measurements, entry gestures, privacy, sound and response regressions.
- Deferral is deliberate under founder instruction: stage changes without launching a full JVM/Gradle/test/APK run after each point.

## Consolidated P10F gate (requires new explicit founder approval)
1. Verify exact P1–P10E branch ancestry, diff boundary, successful source guard and no stable/main change.
2. Compile Kotlin/Compose and AndroidTest sources without running full test suites first; fix all unresolved imports/references (including old V195/V336/V391/V394 consumers, if any) in focused commits.
3. Run targeted new P10 policy and UI cases, P1 gallery import policy, notification/occurrence/locked-screen negative cases; then original Creator/Personal/21-Day reminder callbacks and voice/music integration.
4. Conduct real Android video/GIF/HEIF/photo/crop/preview lifecycle tests, all three families, accessibility off/on, lockscreen privacy, static/reduced-power compositor and GPU/rendering checks.
5. Verify no retired scene collections, old Premiere/Director visual content or old fallback when using any reminder scope or preview on physical devices; inspect signed APK for old resources and user data safety.
6. Capture **real** physical/emulator screenshots or screen recordings, compare against founder's premium-motion direction; no visual acceptance from source inspection alone.
7. Create owner-preview APK only after the founder says staging is finished and explicitly requests consolidated QA/build; production release remains a separate approval.

**Status at the end of P10E source stage: source removal candidate prepared, compilation and actual user experience still unverified.**
