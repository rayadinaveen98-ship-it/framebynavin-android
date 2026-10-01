# Backlot V151 Cross-Platform Foundation — Status

Updated: 2026-10-01
Branch: `feature/backlot-v151-cross-platform-foundation`
Stable Android baseline: `stable/backlot-v150` at `444d54b98f7322d3c7f57ffcfead4f2a57693a30`

## Completed

- V150 promoted to `main` and frozen at `stable/backlot-v150`.
- Kotlin Multiplatform `:shared` module added.
- Shared targets configured for Android, iOS x64, iOS arm64 and iOS Simulator arm64.
- Static `BacklotShared.framework` export configured.
- Workflow V2 rules moved into shared common code.
- Android `CreatorWorkflowV2` converted into a compatibility adapter that delegates to shared core.
- Legacy Android projects without Content DNA still fall back to the existing V150 workflow engine.
- Shared/Android workflow parity tests added.
- Swift-friendly `BacklotSharedApi` facade added.
- Shared common tests added for Workflow V2.
- Cross-platform identity contract added for Google and Apple identities; provider secrets/tokens are intentionally excluded from shared product models.
- Creator cloud reconciliation policy moved to shared common code; Android delegates to it.
- Shared reconciliation tests lock fresh-install restore, local-only upload, cloud-only restore and two-device conflict behavior.
- Platform-neutral `BacklotIdea` domain record added.
- Device-local audio paths are intentionally excluded from the shared Idea contract.
- Android Idea Vault ↔ shared Idea adapter added with round-trip/local-path safety test.
- Native SwiftUI shell added with Today, Ideas, Create, Calendar and Insights tabs.
- Swift UI proof calls `BacklotSharedApi` directly to resolve the same Workflow V2 engine used by Android.
- XcodeGen project spec added for an iOS 17+ Backlot 0.1 simulator application target.
- iOS Simulator app build constrained to arm64 to match the Kotlin/Native simulator framework.
- A real unsigned `Backlot.app` simulator bundle now builds successfully in Xcode CI.

## Verified green milestone

GitHub Actions run `36871573640` is the authoritative V151 foundation gate and passed end-to-end:

### Android
- shared Android compilation
- shared common tests
- Android/shared Workflow V2 parity tests
- Idea Vault shared-domain bridge tests
- Android unit tests
- Android lint
- installable Android APK regression build

### iOS
- iOS Simulator `BacklotShared.framework` link
- framework verification
- native SwiftUI type-check while calling `BacklotSharedApi`
- XcodeGen project generation
- real unsigned iOS Simulator application build
- `Backlot.app` bundle verification

## Next execution slice — Identity V2

1. Add native iOS secure session storage using Keychain.
2. Add a native identity repository boundary so provider tokens never enter shared product models.
3. Add Sign in with Apple using Authentication Services and a cryptographic nonce.
4. Exchange Apple's ID token + raw nonce with Supabase Auth using the native ID-token flow.
5. Capture Apple's first-sign-in full name and persist it to creator/user metadata when available.
6. Add Google iOS OAuth after the iOS client configuration exists for the final Bundle ID.
7. Add account-linking protection so Google and Apple do not silently create duplicate Backlot creator identities.
8. Then connect iOS to the existing creator snapshot/Ideas cloud and prove Android ↔ iOS round-trip sync.

## External configuration still required before real iOS authentication / TestFlight

- Apple Developer Program team/account.
- Final iOS Bundle ID (currently development placeholder `com.backlot.app.dev`).
- Sign in with Apple capability for that App ID.
- Register the native App ID in Supabase Authentication → Apple provider Client IDs.
- Google OAuth iOS client ID / reversed client scheme for the chosen Bundle ID.
- App Store Connect record later for TestFlight.

No production secrets should be committed to the repository. Supabase publishable configuration is public-client configuration; provider credentials and signing material remain outside source control.
