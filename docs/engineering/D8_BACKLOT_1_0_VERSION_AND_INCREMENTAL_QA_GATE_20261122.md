# Backlot 1.0 — D8 version identity and safe incremental release gate

Date: 2026-11-22
Status: D8 AUTHORIZED by founder; initial isolated candidate prepared; compile/test evidence pending.

## Version identity decision

- Public app label: **Backlot** for both package variants; never "Backlot Lab" in app drawer.
- Public versionName: **1.0**. This is a product-facing name and is not Android's upgrade ordering signal.
- Internal versionCode: **338**, monotonically above existing v337's code 337, preserving upgrade eligibility.
- **Isolation is by package ID, not display name:** QA mode with \`-PbacklotLab=true\` remains \`com.framebynavin.app.lab\` and is side-by-side installable. Calling it "Backlot" does NOT make it an upgrade of \`com.framebynavin.app\`.
- Same-package sideload owner preview (later, only after gates): \`com.framebynavin.app\`, \`personalStable\` build with the existing PUBLIC owner-preview signature. Android upgrades require identical package ID, matching signing certificate, nondecreasing versionCode. This signer is NOT suitable for Play production signing; do not call the preview production.
- Current stable/main source and app data remain untouched. No overwriting or uninstalling existing owner APK from CI.
- Signed personalStable owner preview must NOT be distributed until current source compile, unit/lint, identity and signing checks are green; device-side upgrade still requires owner approval.

## Efficient incremental validation approved

1. Freeze exact isolated source SHA, compare with stable baseline and check no UX09/cloud/backups/credentials mutation.
2. Run initial \`compileDebugKotlin\` for a lab package. Collect full error diagnostics; don't schedule a full integration repeat until compile is cleared.
3. Fix exact compile errors in *this isolated branch*, preserving unrelated source and retaining previous diagnostic logs. Each fix creates a new SHA; rerun only compile to see if fixed. Gradle incremental/local caching may reduce time, but GitHub hosted ephemeral runners do not guarantee persistent caches.
4. After compile succeeds, run full unit regressions, Android lint, instrumentation compilation and signed-owner packaging against a selected, immutable source head. For an isolated workflow, use separate jobs and rerun only failed jobs when source has NOT changed. When source changes, dependent results must be renewed. 
5. Android emulator startup and physical phone/Redmi tablet smoke/long-idle still require device evidence; runner emulation is NOT physical reliability certification.
6. Verify source-to-APK SHA, application-label, versionName, versionCode, signer, package identity and SHA256 of final owner-preview APK.
7. Final sign-off/stable promotion is a separate founder action. Never automatically release/merge.

## Version-check and compatibility notes

- Existing Gradle generated \`@string/backlot_install_label\` provides manifest application label AND launcher alias labels; changing this one resValue updates both.
- Both \`versionCode\` and \`versionName\` live in \`app/build.gradle.kts\`.
- \`versionCode=338\` is an internal sequence; users should see version **1.0**.
- Local Lab builds are not eligible to update existing \`com.framebynavin.app\` installs despite the identical Backlot label; inform founder and prefer owner-signed preview only for upgrade evaluation.
- The D8 test package could still present an internal diagnostics indication in an About/testing page later, but do not change the user's requested visible app name or rebrand major UX beyond scope.

## Known gates

- D0–D7, RX02 entrance media + Settings consolidation, RX03 offline speaker selection, RX03B scripts, RX03C localized time greetings and pre-D8 hardening are staged but NOT yet test-certified.
- Prior Personal long-idle voice P0 remains separately uncertified; a passing compile or emulator cannot certify it.
- No paid original voices or individual Personal/Challenge voice schema change is included.
