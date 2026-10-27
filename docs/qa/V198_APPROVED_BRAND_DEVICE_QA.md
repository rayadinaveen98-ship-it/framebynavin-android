# Backlot V198 — Approved Brand Integration QA

Source identity: the user's supplied Option 2 visual, blue / lavender / peach **B** assembled from three offset rounded creator panels. Approved name and tagline: **Backlot — Built for Creators**. No gold spotlight variant.

## Art and code provenance

- An optimized 256×256 WebP launcher/splash image was cropped from the **exact user-uploaded image**. `app/src/main/res/drawable-nodpi/backlot_v198_approved_icon.webp`, Git blob SHA-1 `d02fcce620543960d4276aeefcc39323e3b936d0`. Background #111227; mark scaled into adaptive icon safe region.
- `@mipmap/backlot_v198_launcher` uses that approved artwork plus a separate flat monochrome B mask. It is an Android adaptive icon; rounded/squircle/circle masks are controlled by the launcher.
- `backlot_v198_brand_mark.xml` is a **simplified vector recreation** of the same stacked-panel geometry and colors, used when transparent branding is needed. It is not a pixel-for-pixel equivalent of the approved 3D image crop.
- Small notification icon is a flat white alpha mask to comply with Android status-bar rendering. The color branding remains in the app icon, not in the status bar.
- `V174CinematicWelcome` currently retains the old animation timing until V199. Its visible B and tagline now follow V198 branding; do **not** classify the 3-second welcome as implemented under V198.
- No package ID, reminder schedule engine, notification channel IDs, action PendingIntents, backup format, or storage model changes. Personal sideload signing remains the validated prototype certificate, not a Play-production signing identity.

## Release test matrix

1. Build `assemblePersonalStable` (versionCode 218), unit tests, lint, inherited Android emulator acceptance, plus V198 asset guards and signer certificate hash.
2. Launcher: check circle / squircle / rounded square adaptive icon masks, Android themed icon in monochrome, and shortcuts. No clipped B.
3. System splash: navy background and the approved colorful B, no unexpected old mark. Android 12+ OS splash may appear before native app initialization.
4. Welcome: inspect approved B and `Built for Creators` label, consistent color. Existing 4.6-second animation is **known temporary behavior** until V199.
5. Notifications: regular reminder + optional contextual nudge show a legible single-color B at 24dp and in status bar. Verify actions unchanged and quiet channels untouched.
6. App: onboarding brand signature and Settings About/version footer; no data loss on in-place V197 RC1 → V198 upgrade.
7. App remains accessible and interactive after an external widget or reminder deep link without imposing a welcome delay.
8. Rollback: V197 RC1 and V195.5 stay untouched. **Do not uninstall a populated app** on a signer mismatch.

## Next phase

V199 will introduce the user's requested batched ~4,000 moving colorful circles that converge into the **Backlot** wordmark with tagline in a strict 3,000ms visible timeline; replace the existing 4,600ms visual/audio synchronization, preserve deep link instant launch, and guard 60Hz/120Hz devices and reduced-motion behavior.
