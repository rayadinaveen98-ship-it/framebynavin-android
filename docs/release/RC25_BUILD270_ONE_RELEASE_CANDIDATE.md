# Backlot RC25 · ONE consolidated owner preview candidate · Build 270

**Purpose:** Stop issuing standalone RC security prototypes and consolidate the exact current Android application into **one installable full owner-preview APK** with a repeatable device acceptance gate. No new end-user features in RC25.

## Source freeze and integration

- Release branch: `release/backlot-rc25-build270-consolidated-preview`, based on exact successful RC24 SHA `2f158f887f985580b0c6dd6873b69889892e8a77`.
- The branch descends directly from frozen `main` SHA `aa9e1aab939b7b075b50f9d5a952c6fb86b901eb`. At the start it was **1,349 commits ahead, zero behind** (GitHub compare). This does NOT mean 1,349 PRs, nor that all changes are individually reviewed.
- Exactly one production file changes: `app/build.gradle.kts` bumps owner preview from `versionCode=269` to **270** and versionName to `2.15.7-rc25-consolidated-owner-preview`. Source semantics, permissions and UI are unchanged.
- A new CI-only `RC25InPlaceUpgrade269To270Test` checks synthetic Creator and private Personal Reminder data **before and after** a same-signature in-place upgrade with no uninstall/clear, including a post-launch check.
- The full APK is the existing non-debuggable `personalStable` target signed using the public prototype preview key from checked-in dev-signing. **It is NOT a private Play Store release signer**. RC16's `verifyPrivateReleaseSigning` continues to fail if no private key is provided.
- Full Gradle JVM tests, Android Lint, compiled instrumentation, Android 11 UI smoke, Android 16 real in-place upgrade, signer/manifest/package inspection and artifact SHA-256 are automated in RC25.

## Very important: no data-loss guarantee for owner's physical phone

CI uses disposable synthetic emulator records. Before installing on an existing owner's phone, preserve separate Creator portable backup, separate Personal Reminder backup and external original recordings/files. Verify that both exports open and contain real expected data. Install only as an **in-place update with no uninstall**, using the same prototype signing identity. Do not sign with any different key for this upgrade. If the installer rejects the update, STOP; do not uninstall or clear application data. `android:allowBackup=false` means system backup cannot be treated as a fallback.

## Account deletion and Play Console blockers

Data & Privacy intentionally continues to disclose: **Full Backlot account deletion is not available in this preview.** RC17–24 are offline/local staging proof only. They have not deployed the persistent queue, real storage deletion backend, public deletion-request page or complete privacy policy.

To distribute on Play, owner must: (1) decide on safe private signing identity and existing-preview migration (#180); (2) protect `main` and review entire stacked integration history (#185); (3) approve real backend account deletion, public privacy and data-safety declarations, full-screen alarm/foreground-service eligibility (#187); and (4) run real owner-device feature/backup QA (#174). Neither a Play AAB nor any user-data deletion is generated or run here.

## Release instructions

1. Open RC25 GitHub Actions and download `Backlot-RC25-Build270-Full-App-Owner-Preview`, then verify `SHA256SUMS.txt`.
2. The artifact contains **one complete** `Backlot-RC25-Build270-Full-App.apk` (not patch/delta) built from this exact frozen branch. Its same public preview signer is verified against Build269.
3. For the owner's current phone, perform Creator + Personal backup verification first. Apply the APK as an update only. Do not uninstall any previous Backlot app to resolve signing issues.
4. Test Companion, slideshow, Save Idea, Workspace, Creator backup/restore, Personal reminders backup/restore, reminder notification/full-screen permission fallback, close/reopen persistence and Google account sign-in if configured. Report failures with screenshots and Android version; do not upload personal data to public GitHub.
5. Keep PR DRAFT. No unreviewed merge, paid hosting, production Supabase SQL migration, key publishing or Play submission.

## Definition of RC25 success

- Green source freeze/provenance, Gradle build+lint, signed full owner APK with matching package/cert, successful Android 11 smoke and Android 16 Build269→270 upgrade, fail-closed Play private signer.
- Owner's physical-device acceptance and external Play release blockers are **separate later gates**, not silently marked complete by automated QA.
