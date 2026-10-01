# Backlot Cross-Platform Plan V1 — Locked

Status: ACTIVE
Baseline: Backlot V150 (`stable/backlot-v150`)
Execution branch: `feature/backlot-v151-cross-platform-foundation`

## Product contract

Backlot becomes one cross-platform creator operating system, not two independently implemented apps.

- Shared product/business logic lives in Kotlin Multiplatform (`:shared`).
- Android keeps native Jetpack Compose and Android platform services.
- iOS uses native SwiftUI and Apple platform services.
- Supabase remains the common creator cloud and media backend.
- Existing V150 Android behavior and data compatibility are protected during extraction.
- Platform limitations are handled natively instead of weakening one platform to imitate the other.

## Locked implementation order

1. Freeze/promote V150 Android stable checkpoint.
2. Introduce Kotlin Multiplatform shared core without changing Android behavior.
3. Move Content DNA, Workflow V2, creator-domain models and other pure logic into shared core incrementally.
4. Formalize platform-neutral cloud/media and identity contracts.
5. Add iOS SwiftUI shell with Today, Ideas, Create, Calendar and Insights navigation.
6. Add Apple + Google identity with safe account linking.
7. Implement Ideas and Voice Ideas with Android ↔ iOS sync.
8. Implement Projects and Workspace V2.
9. Implement Blueprint and shared Workflow/Content DNA parity.
10. Implement Script Studio and Publish Studio.
11. Implement Today, Creator Brain, Next Move and Opportunity surfaces.
12. Implement YouTube connection and Insights.
13. Implement native iOS notifications/reminders, voice, calendar and background adapters.
14. Add initial WidgetKit widgets: Quick Idea, Current Project, Next Reminder.
15. Run destructive cross-device recovery and conflict QA.
16. Ship through internal TestFlight, private creator beta, then iOS 1.0.

## First architectural milestone

The first milestone is complete only when:

- `:shared` compiles for Android.
- `BacklotShared.framework` links for iOS Simulator on macOS CI.
- Android consumes shared Workflow V2 rather than a duplicate Android resolver.
- Existing V150 workflow tests remain green.
- A direct shared-core parity test verifies Android and shared stage IDs/actions agree.
- Android lint and debug APK build remain green.

## First end-to-end cross-device milestone

Create an Idea on Android → sync → open iOS → same Idea appears → convert to Project → advance workflow on iOS → reopen Android → same project/stage appears.

Voice Idea media must also round-trip through the existing private creator cloud with integrity verification.

## Guardrails

- No independent Swift copy of Creator Brain, Content DNA or workflow rules.
- No cloud format fork between Android and iOS.
- No App Store/payment work before core sync/parity is proven.
- No attempt to reproduce Android exact-alarm/full-screen behavior on iOS; use native Apple notification patterns.
- No mass migration of Android storage/platform code into KMP. Extract only pure product logic first.
- Existing V150 backups and creator data must remain readable throughout migration.
