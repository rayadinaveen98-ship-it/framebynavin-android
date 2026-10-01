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

## Verified green milestone

GitHub Actions run `36869639524` verified the earlier cross-platform slice through:

- shared Android compilation
- shared common tests
- Android/shared parity tests
- Android lint
- Android APK regression build
- iOS Simulator `BacklotShared.framework` link
- SwiftUI type-check against `BacklotShared.framework`

A later CI revision adds generation/build of the actual unsigned iOS Simulator `.app` target; treat its latest branch run as authoritative before starting authentication work.

## Next execution slice

1. Validate generated Xcode project + unsigned iOS Simulator app bundle.
2. Add native iOS secure session storage (Keychain) and an identity repository boundary.
3. Add Sign in with Apple native flow.
4. Add Google iOS OAuth once the iOS OAuth client configuration exists.
5. Exchange native provider identity with the existing Supabase creator account/session model without creating duplicate creator accounts.
6. Add platform-neutral creator snapshot/Idea serialization contract so iOS can read the exact Android cloud workspace.
7. Build real iOS Ideas list/editor against local-first storage and cloud sync.
8. Prove Android Idea → iOS restore → edit → Android round trip, including Voice Idea remote media reference.

## External configuration still required before real iOS authentication / TestFlight

- Apple Developer Program team/account.
- Final iOS Bundle ID (currently development placeholder `com.backlot.app.dev`).
- Sign in with Apple capability for that App ID.
- Google OAuth iOS client ID / reversed client scheme for the chosen Bundle ID.
- App Store Connect record later for TestFlight.

No production secrets should be committed to the repository. Supabase publishable configuration is public-client configuration; provider credentials and signing material remain outside source control.
