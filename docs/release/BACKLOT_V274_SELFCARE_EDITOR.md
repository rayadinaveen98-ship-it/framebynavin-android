# Backlot V274 — Personal Self-Care editor (DRAFT)

Independent of Cloudflare, Supabase and Play Store. Based on CI-green hourly Personal reminder PR #215, with no alteration to CreatorTask, cloud sync, widget definitions, Play signer or active user records.

## Functional improvement

- Existing one-screen Personal reminder entry is now divided visually into four small guided sections: **what matters**, **your reminder**, **when should it happen**, and **how should Backlot reach you**.
- At top, a compact sideways scrollable set of 8 **optional personal purpose shortcuts**: My Reminder (custom), Hydration, Movement, Rest, Mindfulness, Sleep Routine, Connection and Personal Goals. No category is a medical regimen, diagnosis or mandatory schedule.
- Choosing a category proposes a natural reminder title. An already customized title is NEVER overwritten by selecting another category. All old title/notes/time/repeat/alert controls remain and the Save/Cancel footer stays outside the scroller for IME and large-font accessibility.
- Live **Your Plan** card reflects selected cadence, scheduled first time and alert style. Hourly editor from PR #215 continues to include next five occurrence previews, day filters and active-window controls.
- The chosen optional `PersonalPurpose` is stored only in the local Personal record and Personal v1 checksum-protected JSON export. Existing v1 backups omit `purpose` and import with `CUSTOM`. Old reminders are not silently rewritten or recategorized.
- Unit tests exercise never-overwrite semantics, category defaults and cadence labels. Android11 instrumentation validates old-backup import and guided editor save using synthetic data only.

## Release blocks

This remains an independent DRAFT branch. It must first pass full Kotlin/Android build, lint and Android11 UI/emulator QA. An owner-approved full APK requires integration with V271 voice + piano, V273 Files music and V275 widget preview, testing on real small/large phones, battery restrictions, TTS/music focus, reboot and actual Personal/Creator backup/restore.

No health-related behavioral promises, cloud/YouTube writes, Google Play changes, third-party APIs or paid services. Build270 remains unaffected.
