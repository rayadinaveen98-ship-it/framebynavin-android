# FrameByNavin v1.8.3 RC5 — Verification Record

Status: internal foundation candidate; not a public-store release.

- VersionName: `1.8.3-foundation-rc5`
- VersionCode: `70`
- Package: `com.framebynavin.app`
- Canonical base: `43953d7ad3826dd1035ee2c7cf2dc5ceda4f42c4`
- Verified application source commit: `f1a986c54e2a26a67dc3cf3231923e58dc82626c`
- CI run: `34307894007`
- APK artifact: `10087478488`
- APK ZIP SHA-256: `375e19594c4e9f849297cd5eb4b671236140e18acf27efb65dfa9f8552c68cd1`
- APK size: `22212266` bytes
- Exact APK SHA-256: `ededd8980d9dd273f2ac7ecd723e4a7af0ff7ac7c19f44d54d71dbc77cc4db73`
- Source artifact: `10087478801`
- Source ZIP SHA-256: `f3cea1478c6ab6ef90e410b3b03aee6cf9a0ab0b77eb519ff547d145e57e06de`
- Source tar SHA-256: `0b391ae064cb8e54b4ae61d2b893d04e52e430bbc82a2a82b9891e295f5221db`
- Diagnostics artifact: `10087479201`
- Pilot signing certificate SHA-256: `f0ddec789172ec9f83a037a95b14e90293126f61936d8bab056d3a57e0d472a0`
- Backup schema: `5`

All configured CI gates passed: exact source and payload verification, unit tests, lint, instrumentation compilation, APK build, version/signature verification, and the complete emulator regression suite. The application source was committed and artifacts uploaded only after those gates passed. Downloaded ZIPs passed CRC and SHA-256 verification; inner APK and source tar match their CI manifest hashes. The source archive and Gradle configuration confirm versionCode 70. The recorded certificate matches the existing pilot signing identity.

Scope: invalidate stale YouTube authorization/sync work on disconnect, account switching, credential revocation and restore; isolate derived channel caches; preserve creator-owned video/project links and milestone records. Six synthetic lifecycle regressions were added. No live backend migration or real-user data mutation was performed.

The verified source was fast-forwarded to `integration/v1.8.3-foundation` without force-pushing. The frozen v1.8.2 rollback remains untouched. Physical-device acceptance, broader foundation audit, and production signing are not complete.
