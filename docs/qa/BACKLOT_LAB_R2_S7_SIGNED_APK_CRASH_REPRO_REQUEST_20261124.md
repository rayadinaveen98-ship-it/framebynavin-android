# Backlot Lab R2 Stage 7 — Owner-installed APK crashes on launch

Owner report 2026-11-24: `Backlot-Lab-R2-Stabilization-S6-Owner-Preview.apk` installs but repeatedly closes when opened.

CRITICAL: Do NOT suggest clearing owner data, uninstalling or resetting Lab or cloud database. No deletion or stable promotion.

## Reproduce before altering code
- Reuse byte-identical owner-preview APK from successful signed and full-Lint owner build GitHub run `45359117194` artifact `14906434939`, expected file SHA256 `9f819619b88dc8eea44e38eb839302cd85d1a68a45257f4785a80c3d52f964b6`.
- Debug navigation in run `45343275501` (2/2) does NOT certify `personalStable`; inspect the exact user-installed release variant.
- Stage 5 app source commit `379a3412ca84bf4fe70671fe14cec0bb56b0d316` must remain unchanged for reproduction.
- Run disposable Android 15 emulator with owner **signed** APK: fresh launch, process alive and fatal AndroidRuntime logcat capture, cold restart and stateful in-place upgrade from earlier debug APK after guest onboarding.
- Do not run Gradle or rebuild the APK to collect logs. Preserve logcat, Android activity state, UI dumps, screenshot and SHA verification.
- If emulator reproduces, identify **first FATAL EXCEPTION/root cause** in captured stack before proposing a minimal fix. Do not add speculative catch blocks, reset data or implement unrelated changes.
- If only owner hardware crashes, ask for limited scoped ADB AndroidRuntime logcat using user's Windows PC without collecting personal data, and preserve owner data.
- Signed variant must pass its own cold-start smoke and in-place install acceptance before any further owner-preview distribution.
