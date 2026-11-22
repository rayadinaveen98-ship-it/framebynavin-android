# Backlot UX12 D1 — Approved brand mark and practical signature ident

Date: 2026-11-22
Stage: D1 SOURCE-IMPLEMENTED on isolated draft branch; not merged or released.
Founder direction: Simple Cinematic Companion System — practical Film Editorial structure, Living Spatial motion and Personal Storybook warmth. This stage implements **only** brand mark fidelity and premium welcome composition. No shiny fake concept art, no new characters.

## Source-of-truth and branch fence

- Repository: rayadinaveen98-ship-it/framebynavin-android
- Stable `main`: `abbe88984bac16cfafbd990d9cb00ed54cfdd07e` (untouched).
- RX01 + UX11 draft integration: PR #460, source `5fb8c288c0ceea70db03504b0188cddddd6873d0` (untouched, base for D1).
- D1 source branch: `feature/backlot-ux12-d1-signature-ident-20261122`.
- This is a stacked proposal, not direct changes to production/stable.
- Existing `UX12 Practical Hybrid Design Blueprint` is planning authority. No unrelated UX12 work is implemented by D1.

## Initial verified issue

In `UX11PremiumWelcomeExperience.kt` the premium welcome used only Compose `Text("BACKLOT")`, plus three Canvas background styles. It never drew the existing approved B brand symbol.

The repository already contains the original `res/drawable/backlot_v198_brand_mark.xml` Android VectorDrawable, while the launcher uses a separate WebP asset. This stage **reuses the approved vector**; no geometry or character art is altered.

## Exact D1 changes

1. `app/src/main/java/com/framebynavin/app/ui/UX11PremiumWelcomeExperience.kt`
   - Displays the approved B via `painterResource(R.drawable.backlot_v198_brand_mark)` in the current premium welcome renderer; keeps the wordmark and tagline as real typeset Compose `Text`.
   - Changes decorative offset text overlays to a simple centered `Column` with one mark, one title and one tagline. This is intended to scale cleanly and feel intentional on phones and tablets.
   - Adds pure `UX12D1SignatureTimeline` with ordered B reveal, wordmark and tagline reveals, and exit by the existing 2400ms duration. Android reduced-motion preference uses static fully visible mark/wordmark, without moving the image.
   - Simplifies only the default Cinematic Ember Reveal background to two faint diffuse light wells and one quiet optical line, rather than repeated diagonal streaks.
   - Preserves the original Aperture Light Bloom and Velvet Glass Canvas drawing functions and all four classic welcome routes.
   - Keeps optional launch sound, no-sound previews, and the `onFinished` navigation contract unchanged. No added navigation gate or permission.

2. `app/src/test/java/com/framebynavin/app/ui/UX12D1SignatureTimelineTest.kt`
   - Verifies reveal ordering, full mark/wordmark visibility before exit, normal finish, reduced-motion static values, and legacy route scope.

3. `.github/workflows/backlot-ux12-d1-signature-isolated-qa.yml`
   - Narrow branch-only source fence, Gradle build/unit/lint and Lab APK candidate. This does **not** certify the design on physical tablets; CI outcomes must be read separately.

## Explicit preservation and QA

Unchanged: `VisualExperiencePrefs` saved welcome style IDs, welcome selection UI, external reminder/widget/notification launch bypass, theme selection, app icon, splash/welcome sound preference, UI first-run/auth, companion art, Personal/Creator/Challenge reminder scheduling, snooze/repeat/notification/voice playback, cloud/account config, UX09.

### Acceptance gates

- Gradle compile + all JVM unit tests + lint must pass on the **actual D1 commit**. An older APK is NOT reusable after source changes.
- QA APK must have `com.framebynavin.app.lab` identity and report the source SHA before install. APK generation is not proof of release readiness.
- Actual visual review on at least one narrow phone (including large text) and the owner's Redmi Pad 2 (Android 16 / HyperOS 3): icon edges, exact B shape and palette, text clarity/cropping, frame-time smoothness, no keyboard/system bar overlap.
- Preview must be silent and must not alter saved selection. Selected classic animations must still play.
- Normal launch ident completes once. Widget/notification/deep-links bypass it.
- Reduced-motion and Battery Saver scenarios require physical verification. The current renderer does not add any heavy GPU shader or video dependency.
- P0 Personal Reminder reliability incident remains a separate gate; the visual stage cannot claim Alarm/Snooze certification.
- Any design changes beyond D1 (first-run, companion, personal/idea/settings, RX01 reminders) require their separately planned UX12 stages.

## Current evidence classification

- SOURCE-IMPLEMENTED: approved vector asset insertion and quiet Cinematic Ember treatment.
- TEST SOURCE ADDED: JVM timeline tests, not evidence they executed successfully.
- CI / APK / REAL-DEVICE QA: not asserted until the corresponding independent evidence exists.
- MERGE/RELEASE: not approved. Keep the draft PR isolated until founder visual and QA approval.
