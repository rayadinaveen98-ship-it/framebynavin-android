# Backlot Lab R2 S7 — Verified signed-startup Firebase App Check fix QA

Date: 2026-11-24. Owner reports previously delivered signed `personalStable` versionCode 337 crashes every time it opens.

## Actual bug reproduction and stack evidence
- Exact signed owner APK `Backlot-Lab-R2-Stabilization-S6-Owner-Preview.apk`, SHA256 `9f819619b88dc8eea44e38eb839302cd85d1a68a45257f4785a80c3d52f964b6` was downloaded unchanged on disposable Android15 emulator.
- Run `45369021502`, startup smoke job `138358319651` reproduced the crash with `FATAL EXCEPTION: main`.
- Root stack: `java.lang.IllegalStateException: Default FirebaseApp is not initialized in this process com.framebynavin.app.lab` -> `FirebaseAppCheck.getInstance` -> `CreatorAppCheck.install(CreatorAppCheck.kt:20)` -> `MainActivity.onCreate(MainActivity.kt:57)`.
- Build variant difference: `app/src/debug/java/com/framebynavin/app/CreatorAppCheck.kt` already returns early when `BuildConfig.BACKLOT_LAB`; `app/src/personalStable/java/.../CreatorAppCheck.kt` did not. Lab intentionally has no `google-services.json`, and uses its own Supabase environment. The shipped signed personalStable variant therefore crashes even though the old debug Android15 tests pass.
- No evidence of data corruption, service/DB reset, or corrupted downloaded file. Do NOT uninstall user installation or clear data.

## Minimal applied fix
- Application change commit `28a7000159554f254ab1ecc1aecc7792a1385ac2` adds the same `if (BuildConfig.BACKLOT_LAB) return` guard before Firebase initialization ONLY in the `personalStable` CreatorAppCheck.
- Production Firebase/App Check path (when `BACKLOT_LAB=false`) is unchanged; debug and release source sets unchanged. No Gradle, permissions, signing, backend, database or unrelated features changed.

## QA gates for safe replacement
1. Exact app source-diff fence proves a single variant file changed. No broad unit test rerun.
2. ONE `personalStable` compile in separate Kotlin JVM, then full Android vital Lint assembly on same runner; no lint bypass, no cloud writes.
3. **Upload the APK as a PRIVATE unverified checkpoint immediately after successful assembly.** Later verifier or emulator failure must NEVER force recompilation of this identical APK.
4. Separate quick signer/manifest/sha/package verification, no Gradle. Requires nondebuggable Lab package `com.framebynavin.app.lab`, versionCode 337, expected public prototype certificate.
5. Run exact signed APK on disposable Android15 emulator: fresh start, restart and in-place guest onboarding upgrade from earlier debug; gather FATAL EXCEPTION logs/screens. Final owner-ready APK uploaded ONLY if this signed runtime gate passes.
6. User physical tablet upgrade and 18-condition reminder/audio/privacy acceptance remain pending after signed QA, as does stable promotion (no automatic release).

Release STOP if startup still crashes, signature incompatibility, package/manifest mismatch, user data loss, or real locked-screen reminder privacy regression. No uninstall/pm clear on owner device.
