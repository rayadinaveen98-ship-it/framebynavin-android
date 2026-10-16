# Backlot V167 — Settings S2: Structure & Navigation

**Base:** V166 S1 safety commit `2f29dcf9233917ae93e6d724912ad006c8050ef7`.
**Rule:** This is a structural UX-only update; it must not reset projects, creator profiles, settings, calendar sync or backups. No merge to stable without creator approval.

## Six Settings destinations
1. **My Profile** — existing account identity, creator details editor entry, connected Google account, YouTube results and sign-out.
2. **Personalization** — child pages Appearance (theme/app icon/launch sound), Spark (companion), Voice (default project voice and preview). Subpage Back returns to Personalization, then Settings home.
3. **Reminders & Alerts** — existing permissions, snooze/auto-stop, quiet hours and helpful reminders. S1 reminder safety untouched.
4. **Planning & Calendar** — existing preview-before-Auto-Plan, consent-first Google Calendar export and calendar selection.
5. **Backup & Data** — one cloud backup account destination, portable export/restore, recovery copies and existing backup exclusions.
6. **About & Help** — app tour and version.

## Eliminated duplicate destinations
- The old top-level **Connections** category is removed; its YouTube destination remains in My Profile, and its Google account/backup destination remains under My Profile and Backup & Data respectively.
- The repeated **Google Drive Vault** card is removed from My Profile and replaced by a readable pointer to Backup & Data. Google Drive operations remain available in the existing Cloud Sync screen. No data is deleted or disconnected.
- All customization controls remain, but only the Personalization category appears on Settings home.
- Native Android back and visible back arrow agree on parent-child navigation. A stale saved section named CONNECTIONS resolves safely to Settings home.

## Acceptance
- V161–V166 inherited checks, V167 navigation guard, JVM tests, Android lint, Android test compile and debug APK.
- Android 11 UI tests: six categories, Profile, Personalization → Voice → Personalization → home, Backup & Data links, Reminders and Calendar.
- S1 consent dialogs still need explicit confirmation and Calendar provider-side tests on device.
- Confirm that a user returning from Profile or Google backup lands in a sensible Settings state, and that font scaling/vertical scrolling supports smaller devices.
- No settings preference or storage schema migration, no changes to actual reminder mechanisms, voice audio, app icon assets, YouTube data or Google sign-in.

## Next
- **S3:** Profile & Personalization: inline editing instead of re-entering onboarding; polish themes, Spark and voice, maintain stored values.
- **S4:** Reminders & Planning UX clarity without changing reminder reliability.
- **S5:** Backup/Sync reporting, device acceptance and release decision.
