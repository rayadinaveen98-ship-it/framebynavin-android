# Backlot RC16 — Private Google Play upload signing gate

**Scope:** Build269 production code and UI remain frozen; only `app/build.gradle.kts` receives a release-signing preflight. No Play upload, app installation, deletion, key provision or change to the current public preview key occurs.

## Why this matters

Backlot's `personalStable` APKs use the publicly committed `dev-signing/framebynavin-public-debug.jks` for continuity with existing sideload installations. That key is **not private** and must never be used for Google Play's upload or app-signing identity.

Google Play App Signing ordinarily separates an owner-held **upload key** for signing the AAB from Google's **app-signing key** used on APKs installed by users. This branch only validates the owner-held upload identity; it cannot choose Google's key on the owner's behalf.

Official references:
- https://support.google.com/googleplay/android-developer/answer/9842756
- https://developers.google.com/android/guides/client-auth
- https://developer.android.com/build/building-cmdline

## Implemented runtime-free checks

`verifyPrivateReleaseSigning` fails closed when any of these conditions hold:

1. Any of the four `FRAMEBYNAVIN_RELEASE_*` values are missing or empty.
2. Keystore file is missing or inside the checked-out repository (committing a private key is forbidden).
3. Keystore cannot be opened, alias isn't a key entry, certificate is unavailable, or private key password cannot unlock the key.
4. Certificate is **identical to the checked-in public prototype certificate**, even when the public JKS was copied to a different path outside the repository.
5. Certificate is not RSA or uses RSA shorter than 2048 bits.

`assembleRelease`, `bundleRelease`, `packageReleaseBundle`, and `signReleaseBundle` depend on this preflight. `debug` and `personalStable` remain unaffected. The app ID `com.framebynavin.app`, versionCode 269 and all production Kotlin/Compose sources are unchanged.

## Non-secret CI proof

The workflow checks missing-credentials failure, **rejecting an external copy of the publicly committed keystore**, then creates a throwaway RSA 3072-bit identity **outside the repository**, signs a CI-only release AAB and verifies it with `jarsigner`. These ephemeral test certificates are not release credentials and **must not** be uploaded to Play.

The same workflow assembles a new `personalStable` APK solely for a signer-continuity comparison against RC13's known-good owner preview; it must have the same certificate as the prior prototype. No owner-installable APK or Play AAB is published as an RC16 artifact.

## External owner decisions required before a real release

- Choose between a **new Play package ID side by side** (safer for preserving preview installation) or retaining the existing package ID with a separately approved and verified manual migration process. Do not silently change the package ID.
- Generate/store a real unique upload keystore under owner control **outside GitHub**, using a secure secret manager or other private method. Never post keystore, aliases/passwords, signed AAB, backup files or full signing logs in issues.
- Register the correct SHA-1/SHA-256 certificates for Google Sign-In, Firebase and any affected OAuth/Play identity after the package and signing strategy is selected.
- On disposable device/profile, validate verified Creator and separate Personal backup restores including original media, projects and reminders.
- Configure protected main, approve stacked PR consolidation, legal privacy URLs, authenticated deletion backend, Data Safety, alarm/foreground service permissions and Play Console declarations.

**No merge or Play publication is authorized by passing this test.** Release gates: #174 #180 #185 #187. RC15 cross-install proof confirms backup portability only, not Android signature compatibility.
