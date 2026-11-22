# Backlot RX02 — Reminder Appearance Consolidation & Custom Gallery Entrance Media

Date: 2026-11-22  
**Status: SOURCE STAGED, NOT COMPILED, TESTED, OWNER-ACCEPTED, MERGED, APK-BUILT OR RELEASED.**

## Founder-approved change request and bounds

A. User gallery media replaces **ONLY** the full-screen **double-tap ENTRANCE background** for Creator, Personal or 21-Day Challenge reminders, selected independently from Settings; images, supported animated images and compatible videos <=15 seconds. All existing text, Voice/Alarm optic, double-tap accessibility gate and the separate MAIN background/control system remain in place. Nothing changes reminder delivery, start/stop, Snooze, Done, Repeat, acknowledgment, service/notification privacy or Android fullscreen policy.

B. Consolidate previously scattered reminder visual selectors under a single **Reminder Appearance** Settings page rather than deleting existing choice IDs. Preserve the old V391 (12 scene IDs, independent Voice/Alarm selections), V394 Premiere, V407 optional companion preference, RX01 domain enablement/precedence and original per-domain palettes. Advanced Android full-screen permissions belong under **Notifications & Reminders** delivery settings, not Appearance.

C. Founder expressly forbids per-source-stage test runs/APK builds. Only an explicit final **D8** authorization permits cumulative compilation, JVM/test/lint/emulator/physical QA, APK artifact creation, and founder acceptance. **Do not merge or modify protected stable/main.** Voice-engine redesign is a separate proposal not yet approved for a particular provider/cost.

## Git lineage

- Protected main and owner-stable at entry: `abbe88984bac16cfafbd990d9cb00ed54cfdd07e`.
- D7 source-review closure head: `b0d667565bd1c27546be895a6956edde245ded75`, branch `feature/backlot-ux12-d7-review-closure-20261122`.
- RX02 isolated branch: `feature/backlot-rx02-custom-entrance-media-20261122`, stacked directly from D7 closure.
- RX01/UX11 draft QA PR #460 is separate and **not evidence that RX02 passed QA**. No UX09/Supabase/Cloud/backups/hosted data changes.

## Implemented source locations

1. `app/src/main/java/com/framebynavin/app/reminders/RX02EntranceMediaStore.kt`:
   - New per-domain saved file preference, app-private `filesDir/rx02_entrance_media`, vetted filename and MIME/size/duration checks, bounded stream copy, old selection retained when import fails, on-success replacement cleanup, separate remove/reset.
   - Whitelist JPEG, PNG, GIF, WebP, HEIC/HEIF API28+, MP4, WebM and 3GPP; 40 MB cap and **MAX 15,000ms on VIDEO**. For now, longer videos produce a clear instruction to trim in Gallery; **no in-app video segment editor or transformer was added.**
   - Local-only; no app-wide storage/camera permission requested. The existing Android manifest has `android:allowBackup="false"`. Do not promise any image is synced to another device.
   - Video poster JPG is generated where supported for battery-saver/decoder failure.

2. `RX02EntranceBackdrop.kt` and `res/layout/rx02_entrance_video_texture.xml`:
   - Decorative-only background. Locked-device policy always shows original generic RX01 fluid art (not privately selected gallery media), including Creator Activities that may appear over the keyguard.
   - Photo center-crop; GIF/animated WebP on API28+ via ImageDecoder, static image fallback on earlier versions/reduced motion; video via Media3 ExoPlayer+PlayerView TextureView, silent loop, audio tracks disabled, pause on lifecycle stop and release on composable disposal; poster on reduced motion/power saver; built-in stock underlay on decode/player error. High contrast dark scrim behind untouched foreground.
   - No touch handler, new warning/alarm, new playback audio, FSI entitlement, network or data/receipt mutation.
   - Media3 open-source dependencies `media3-exoplayer:1.11.1` and `media3-ui:1.11.1` pinned in `app/build.gradle.kts`. License/cost: no subscription/API. Check AndroidX Media3 release/version against target Gradle during final authorized D8.

3. `RX01ReminderExperienceGate.kt`:
   - Replace **only** `RX01FluidReminderBackdrop(ENTRANCE)` behind existing `RX01ReminderEntranceHero` with `RX02EntranceBackdrop`. Original gate's occurrence recheck, touch/accessible double tap, MAIN separate artwork and main response closures unchanged.

4. `RX02ReminderAppearanceSettings.kt`:
   - One Settings page with Creator/Personal/21-Day selection, saved Fluid enable switch, per-domain default original artwork, system Photo Picker (Image+Video), local import, replace/reset, full-screen preview showing original unchanged Voice/Alarm optic and labels, and an optional expandable section for V391/V394/V407 saved old look choices.
   - Explicit explanation that long clips require trimming in Gallery and system live wallpapers / iOS Live Photos need compatible video export; Motion Photo/JPEG may import as **still**. No direct third-party animated wallpaper access claimed.
   - Preview is UI-only and does not create a live reminder, notification or user response.

5. `V144SettingsHub.kt`, `V404SettingsIndex.kt`, `RX01ReminderSceneSettings.kt`:
   - Existing `REMINDER_SCENES` navigation now shows `RX02ReminderAppearanceSettings` as **REMINDER APPEARANCE**; all previous V391/V394 options remain discoverable within the one location.
   - 21-Day Android FSI owner-opt-in/settings moved into the advanced **Notifications & Reminders** delivery/permissions section without mutating its existing preference key or policy. Settings search directs it there. Old RX01 settings implementation retained in source for compatibility/reference, no longer surfaced as a duplicate screen.
   - Normal Account/App Theme/Welcome/Companion/Voice/Privacy routes and preference files untouched.

## Technical risk/limitation ledger — do NOT hide

- **Not a system wallpaper importer.** Third-party running live wallpapers can't necessarily be extracted; supported exported video/animated image works. Motion Photos and HEIC+/MOV dual-file Live Photos are not guaranteed as animated; image still fallback or manual video export.
- **No in-app trimming** for videos >15s at this stage; only already-short clips. Media3 playback supports loop; film export/transcoding is not part of this source stage.
- **No user crop/position editing** yet; centerCrop is used. Confirm whether fit/reposition is required before product acceptance.
- **Custom media local-only**, not synchronized by UX09 or Google sign-in. Reinstall/erase local data loses it.
- **Runtime not verified**: Gradle/Media3 resolution, compile, UnstableApi lint, rendering order/TextureView over Compose, lower-memory device battery/codec availability, partial media import failover, 40MB asset, Android13–16 Gallery picker, GIF performance, HEIF orientation, aspect ratio, scroll/large font.
- **Privacy**: source lockscreen fallback uses Android KeyguardManager and Lifecycle events; must test locked/unlocked lifecycle and no gallery frame leaks during resume/rotation. Android backup disabled by existing manifest; do not add network upload.
- **Delivery P0** remains separately open: unattended Personal Voice, repeat/retry, Snooze and Android OEM background restrictions. Do not conflate a visually opening screen with successful audio.

## D8 acceptance matrix — authored, not executed

1. Stable lineage and exact file diff; RX01 D7 closure vs RX02 changes, no modifications to Android alarm/Voice services, OS policy, notification receivers, cloud or account.
2. Picker permissions: gallery photo PNG/JPEG/HEIF, GIF, WebP (static/animated), MP4/WebM/3GP <=15 seconds; cancel, expired URI, unsupported MIME, corrupted image/video, >40MB, 15,001ms, missing device decoder; previous selection preserved on error.
3. Each of three domains saves/reloads its own asset independently, replaces and removes without orphan media or cross-domain bleed; restart, upgrade, local erase, low disk, locked device and tablet landscape verified.
4. Entrance: same actual mode-matched microphone/alarm optic and all exact text; one tap must NOT advance; double tap is the ONLY touch opening, accessible equivalent; second occurrence check before MAIN; MAIN always uses original calm scene; no media playback after EXIT/MAIN.
5. Video loop silent and audio tracks disabled, player stops on lifecycle pause/exit; still poster on lowPower/animation scale off/error; GIF stops on pause; no sound-focus contention with actual voice/alarm.
6. Bright white photo/GIF and high contrast scrim legibility at 320dp and 2x fonts; compare native screenshots for Creator/Personal/Challenge, TalkBack, power saver, notch, and large phone/tablet.
7. Settings: One Reminder Appearance location, old V391 12 scene IDs / per-Voice+Alarm choices, V394 Premiere and V407 companion persisted; RX01 domain fallback choices unchanged; Android 21-day full-screen opt-in only under delivery/permissions, no hidden account or reset actions.
8. P0 long-idle/repeat/Snooze real audio evidence on Redmi Pad 2 and other Android OEMs, independent of graphics QA. Physical install/acceptance and artifact hash/certificate only after explicit final D8 approval.

## Stage checkpoint

- UX12 D0–D7 + review closure: Source staged, NOT certified.
- **RX02 custom media + Settings consolidation**: SOURCE STAGED ON ISOLATED FEATURE BRANCH. No tests/build run.
- **Voice engine quality upgrade**: still a proposed decision, not implemented; founder has not approved paid TTS, samples/provider or billing.
- D8 cumulative QA and Lab APK: **HELD** pending founder explicit "Done bro, start next steps and give APK" or equivalent.
