# Backlot Lab R2 — owner-preview build request

Requested 2026-11-24, following owner's instruction to continue and share the APK if ready.

**Evidence:** Android 15 first-run navigation and Lab debug/test build passed in Actions run 45312742080, two independent fresh-app passes, test app source SHA `64ced7c21f0de30c82926505985d81ddcaf5a5f3`.

**Exact scope:** One standalone Backlot **Lab** `personalStable` owner-preview APK with `-PbacklotLab=true`, produced from identical tested application sources. Verify `com.framebynavin.app.lab`, `Backlot Lab`, non-debuggable application, existing development-preview signing certificate for in-place upgrade, versionCode and SHA-256. Refuse to share on any verification failure.

**Prohibited:** production app package or original backend, private Play signing, new features, R2 code modifications, backend/database mutations, destructive resets, extra full test campaign, merge or stable promotion. Only owner physical test, not release.