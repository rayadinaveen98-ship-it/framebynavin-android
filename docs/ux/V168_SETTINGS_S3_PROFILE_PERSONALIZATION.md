# Backlot V168 — Settings S3: Profile & Personalization

Stacked on V167 Settings S2. Draft preview only; no stable merge, data migration or account changes.

## Creator Profile editing
- **Edit Creator Details** opens a focused full-screen editor in My Profile, not the five-step first-run onboarding.
- Existing mode, secondary modes, posting platforms, production styles, main and secondary goals and weekly publishing target are prefilled. The editor preserves unfamiliar historical/custom labels.
- Essential choices appear first; secondary creator types, styles and extra goals remain accessible under **More options**.
- **Cancel/Back makes no data write**. Save is enabled only for a valid creator type, at least one platform, a main goal, and weekly target in 1–14.
- Saving is synchronous and verified using `CreatorOsSettingsStore.saveCreatorProfileEdits`; failed writes show an error, not a false success.
- Keep the original display name, cloud creator ID, logged-in account, secondary choices, existing creator projects, project reminders, customized weekly templates, audio and reward history.
- Do **not** change `accountOnboardingComplete`, `onboardingComplete`, guided tour completion, account identity, or the creator setup schema. This is an edit, not a migration.
- Legacy `onEditCreatorSetup` callback is retained for compatibility with historical UI tests but deliberately not invoked or wired to reset onboarding.

## Personalization
- **Appearance** retains all existing themes, icons and launch-sound controls. Small text labels are enlarged and each option has clearer explanation.
- **Spark** uses the current companion picker and stored selection. The text describes the actual choice, with no claim that it changes projects.
- **Voice** retains every existing `VoicePersona` and device Text-to-Speech preview. Cards and preview controls have more legible labels and 40dp minimum preview-row height. Changing defaults does not rewrite existing projects.
- Existing preferences, backup format, cloud state and app launcher aliases remain unchanged.

## Automated and device acceptance
1. Inherited checks V161–V167 + S3 source policy test.
2. JVM tests: draft/cancel leaves profile intact, edit preserves name and schema, unusual legacy labels survive normalization, invalid inputs rejected.
3. Android tests: cancel → no save, save → callback exactly once, My Profile editor opens without invoking old onboarding callback.
4. Android lint, AndroidTest compile and V168 APK; Android 11 emulator checks.
5. Physical phone: edit a profile and reopen; confirm it persists after app restart, same local/cloud identity, no forced onboarding and no changes to existing projects, recordings or weekly plan. Try 1.3x and 2x system font scaling and TalkBack.
6. Re-test theme, app icon change, launch sound, Spark and TTS preview across available language packs.
7. S1 Calendar consent and S2 navigation remain mandatory regression checks; stable release and Settings S4/S5 are separate steps.

## Known limits
- Profile changes alter future suggested defaults. They do not retroactively change existing creator projects or your previously customized weekly slots.
- Android emulator and phone acceptance must pass before release; a successful compile alone is not acceptance.
