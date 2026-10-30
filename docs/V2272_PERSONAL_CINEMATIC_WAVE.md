# Backlot V227.2 — Personal Reminder cinematic particle refinement

## Audit
Creator Voice/Alarm (V218) already uses a unified particle field where wave dots become the clock/microphone shape without separate sticker artwork, at 30 fps (20 low power), honoring the system animator-disabled setting. Personal Reminder screens still used an unrelated, endlessly rotating **100-dot orbit** regardless of system reduced-motion preference or battery saver. This undermined the consistent premium design and could keep rendering during accessibility motion disablement.

## V227.2 changes
- Replace the Personal alert artwork with the **same flowing pigment and geometries** as Creator's V218 wave. The clock for ALARM and microphone for VOICE are formed naturally from ambient particles, not pasted as an independent icon. Notification-mode Personal alerts use the clock/time motif.
- Deterministically sample the existing Creator particle geometry into 728 normal / 329 low-power Personal points within the existing 252dp illustration viewport; 243 / 110 of those points participate in the icon trajectory. This avoids increasing draw load to match a full-screen effect.
- Limit update cadence to ~30fps normally and ~20fps under Android Power Saver or on low-RAM devices; do not create an always-running animation when Android's animator setting is disabled. Static reduced-motion view displays a formed glyph and retains user actions.
- Keep the established `v2233-particle-wave` UI test tag and Personal Alert activity structure; no changes to notification permission, privacy, sound playback, reminder lease, Done, Snooze, Dismiss, recovery logic or Creator reminders.
- Introduce unit tests for glyph mapping, deterministic geometry, counts, fps policy and reduced-motion formed state; Android UI smoke checks static voice and alarm rendering without persistence side effects, plus the two original Personal alert response/regression tests.
- Full Android app build 251, prototype signed same app ID/certificate as 250 for normal in-place sideload. Not Play Store signed.

## QA / release boundary
- Full JVM/lint, signed complete APK and signer/version continuity.
- Android 11 and Android 15 run new Personal waveform render checks and inherited existing 54 Idea, Personal, backup, widget, reminder tests including actual Android 15 gesture navigation keyboard screenshots.
- Device review remains mandatory for low-RAM/battery saver actual behavior, accessibility (TalkBack, system animation scale 0), animation smoothness, screen heat and different resolutions.
- Legacy widgets are **not removed** in V227.2. Later safe picker retirement/migration must preserve already pinned instances and not touch projects/reminders. V226 physical backup provider checks and reminder Doze/audio acceptance also remain open.
