# Backlot UX12 D6 — Practical Settings & Appearance
Date: 2026-11-22
Status: SOURCE STAGED. NOT COMPILED, TESTED, MERGED, APK-BUILT OR RELEASED.

## Approved direction and branch ancestry

- UX12 Simple Cinematic Companion System: restrained Film Editorial structure, Living Spatial motion only where useful, warm original Spark/Mira Storybook Companion touches.
- Approved D0 blueprint and D1–D5 staged source are the basis. Do not substitute generated promotional UI or redesign existing characters.
- Parent: `feature/backlot-ux12-d5-idea-vault-orb-20261122` at `f685b8bc7ec2886b9d1887232a90d89f3a61a2a0`.
- D6: `feature/backlot-ux12-d6-settings-appearance-20261122` stacked on D5, not on main.
- Protected owner-approved stable/main remained `abbe88984bac16cfafbd990d9cb00ed54cfdd07e` before source staging. No approved stable promotion.
- Stage without running per-stage CI, unit/instrumented tests, lint, emulator, APK build or physical-device exercises. Founder authorizes cumulative QA only after: **"Done bro, start next steps and give APK"**, or equally explicit final-stage instruction.

## Source audit before edits

`V144SettingsHub` already owns working Settings search (V404), navigation hierarchy (V167), personalisation routing, notification/exact-alarm access, Snooze/quiet-hours/follow-ups, Creator Auto Plan confirmation, Calendar, privacy, advanced erase and about. Prior Personalization was five large category cards without showing saved selections. The Home search input must remain discoverable, including empty/no-results and retained test tags.

`V127AppearanceSettings` uses `VisualExperiencePrefs` and only two approved selectable themes (Director's Cut and Light); several legacy theme identifiers are normalized for compatibility. Individual cards contained bordered cards around bordered previews. Saved app icon and launch sound have existing independent owners.

`V401WelcomePicker` already has 3 premium and 4 classic style IDs, explicit Preview vs Choose, silent preview, optional launch sound and external-launch routing. No stored welcome ID may be deleted or rewritten by this visual stage.

Companion source uses the existing `V144GuidePrefs` and artwork-backed `V144GuideCharacter`; Spark/Mira choice must never be overwritten simply by opening Settings.

## Exact D6 source changes

1. **`app/src/main/java/com/framebynavin/app/ui/V144SettingsHub.kt`**
   - Adds `UX12D6StudioProfileCard`: a simple editorial, no-border Settings identity surface showing the *actual* current theme, selected Spark/Mira and saved welcome style, plus a small original THINK pose. Tap uses the existing Personalization route; display does not write preferences.
   - Organizes Home headings into **You & Your Studio**, **Reminders & Workflow**, **Privacy & Support**. Search and all existing callback routes remain.
   - Replaces large Personalization category cards with compact grouped quiet rows: look/appearance, Home photos, Reminder scenes, Companion, Reminder voice. Descriptions show saved choices, not invented account/sync/delivery states.
   - All real Settings destinations, permission buttons, notification defaults, data/advanced controls, YouTube/Calendar handlers, backstack and search mapping remain unchanged.

2. **`app/src/main/java/com/framebynavin/app/ui/V127AppearanceSettings.kt`**
   - Keeps only approved Director's Cut and Light choices, their same `VisualExperiencePrefs.setTheme` callbacks, app icon picker, welcome picker and launch sound.
   - Smaller visual previews; only selected theme gets one subtle outline, instead of multiple nested borders. Theme selection declares radio-button semantics for accessibility.
   - Keeps the `V148AppearancePreview` palette/sample-rendering mechanism, rather than fake screenshot art.

3. **`app/src/main/java/com/framebynavin/app/ui/V401WelcomePicker.kt`**
   - Shows the three premium welcomes first. The four original choices are still available via one explicit **See all four original openings** button.
   - If the user has a saved classic welcome, its collection is expanded when Settings first opens, so the saved choice is not hidden.
   - Preserves all seven stored IDs, distinct full-screen preview, existing choose/write, preview-no-write, preview silence and external launch behavior. Toggle has test tag `ux12-d6-toggle-classics`.
   - Introduces no new animation, notifications, permissions, icon change, paid asset or platform API.

No repository production settings stores, auth/cloud data, reminders/alarms/auto-retry/Snooze, creator models, project editing, Supabase or backup/restore/delete implementations were modified.

## Deferred mandatory cumulative QA (only after owner final command)

- Fresh/returning local and Google account: Settings opens, back/search/direct search routes correct, no profile/session or legacy data reset.
- Appearance: Director's Cut and Light change true live palette; saved theme survives process restart/update. Legacy themes still normalize.
- Classic welcome saved before upgrade: it is revealed immediately; three premium styles and four originals each can be previewed without saving; Choose alone persists; launch sound stays independent.
- App icon picker and Home photo editor remain usable; companion picker retains Spark/Mira choice and guide artwork; a selected guide remains selected on re-entry.
- At 320–360dp, Redmi Pad 2, landscape, 1.5×/2× fonts and TalkBack: no clipping, tapped summary goes to Personalization, theme radio semantics and welcome expand/collapse are intelligible.
- Search terms/theme/guide/results and no-results remain operable, including `v404-settings-search`, `v404-settings-results` and `v404-settings-no-results`.
- Reminder scenes, Voice, quiet hours, Snooze, alarm auto-stop, follow-ups, Calendar, YouTube, privacy, only Advanced erase, guided tour all retain original callbacks.
- RX01/UX11 integrated Android and regression QA, P0 Personal Reminder long-idle/Snooze/repeat reliability, physical screenshots and APK provenance are separate mandatory gates. Source-read review is NOT a pass.

## Roadmap checkpoint

- D0: APPROVED blueprint.
- D1–D6: SOURCE STAGED on the cumulative UX12 branch, not stable.
- D7: NEXT — three reminder experiences, bounded visual refinement only; preserve RX01 behavior and P0 reliability investigation.
- D8: HELD — one cumulative compile/test/lint/instrumentation/physical QA, then traceable Lab APK and owner acceptance, **not automatically now**.
