# Backlot Build272 — complete owner test APK, voice + music preview

Owner-only draft branch based on PR #221 SHA `b3e70f52f0355346b11593dc1b02987f3ea7c134`. The previous Build271 owner APK remains unchanged and has passed 17/17 Android11 phone + 17/17 Android16 tablet integration cases; SHA-256 `b93684e1dbebdd99b4047e0c9f8ff6181d9fb73e22e94c89ae73d7d5082370ae`.

## Build272 increment

- Full Android Backlot app `com.framebynavin.app`, versionCode 272, versionName `2.16.1-rc27-reminder-voice-preview-owner-test`.
- Contains Build271's integrated Personal/Creator reminder features plus V277 editor Preview control for real TTS+music sample, background-only preview, Stop control, 30-second auto-stop and composable ON_STOP lifecycle handling.
- No new Android permissions, no changed Personal or Creator data schema, no cloud/project sync changes, no live Supabase or account deletion, no Play private signing/Google release. Personal/Creator existing local data must be preserved when updating.
- Uses checked-in public **prototype preview signing key**, not Play release signer. CI downloads exact SHA-verified Build271 owner APK and checks the two APK X.509 signer fingerprints are identical before uploading Build272 for owner testing.
- Full Android Gradle/JUnit/lint, synthetic Android11 phone + Android16 tablet instrumented integration 20/20 test cases each with explicit CI-emulator data-safety guard required before distribution.

## Honest release boundary

- This is for **testing** only; physical owner handset must verify TTS voice, music levels, notification visibility, missed/overlapping reminders, 3 actual repeated speech announcements and Android battery management. CI cannot confirm sound quality.
- Back up BOTH Personal and Creator independently before updating. Never uninstall Build271 or clear data to install. If Android reports signature mismatch, cancel.
- Custom picked song URI is not portable across devices; reselect locally on a new phone. Avoid restoring local music URIs as guaranteed audio sources.
- Widget previews should be compared to actual home-screen widget pinning and launcher behavior.
- PR remains draft; Play private signing, policy website, domain and production Supabase deletion stay separately gated.

CI: https://github.com/rayadinaveen98-ship-it/framebynavin-android/actions/workflows/backlot-v278-build272-owner-qa.yml
