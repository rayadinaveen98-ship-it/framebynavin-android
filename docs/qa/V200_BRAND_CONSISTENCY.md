# Backlot V200 — Brand consistency

Preview: versionCode 220 / `2.7.0-v200-brand-consistency-preview`.
Parent: V199 `6aed478f05e8b0a24f1fdbac0d10be950f1bec79`.

- Settings > Appearance > App Icon now previews only the approved vibrant B; historical four gold/ivory choices removed from display. Existing launcher aliases and stored choices are intentionally preserved but all render V198 art.
- All routine, alarm, voice, idea and next-stage notification producers now use the approved monochrome small icon already used by project reminders and contextual nudges.
- Adds whole-app source guard, UI instrumentation check, and inherits V199 unit/lint/build/full emulator suite.

No changes to application ID, signer, existing notification channel IDs, PendingIntent action IDs, foreground-service policy, reminder delivery, data persistence, old user preferences, or 3-second V199 welcome.

Legacy unused art is retained in source for compatibility but cannot be reactivated on the current launcher/Home/welcome/notification routes.

Acceptance: green exact-commit CI + physical-phone in-place upgrade; inspect Settings, launcher, actual OS splash, 3s intro, status bar/shade, lockscreen, each notification type and its action, all projects/settings. Do not merge or mark stable without explicit approval.
