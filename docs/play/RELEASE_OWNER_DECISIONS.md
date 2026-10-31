# Backlot Play release — owner decisions and production gates

Status: **blocked pending user-controlled credentials and production behavior**; no production service deployed and no Play submission authorized by this document. Build272 is the fully built and emulator-tested *prototype-signed owner APK*, not a production Play package.

## Owner-only inputs that must never enter public GitHub or this chat

1. An enrolled Google Play Console developer account and its approved developer identity/contact information. The Play Console connection, payment, identity verification and store setup are owner operations.
2. Production Play App Signing choice and a **private** upload signing identity. Keep keystore and passwords OUT of the public GitHub repository and issue logs. Existing Build272 certificate is public prototype material. Never distribute its key as a production signing identity.
3. **Choose application identity and data migration strategy**:
   - Preferred for protecting existing prototype data: a separate Play `applicationId` with production signer installed side-by-side, and tested explicit import of independently exported Creator and Personal records. Requires OAuth/redirect/package/deep-link changes, backup restoration testing and final listing identity choice.
   - Same `applicationId` with a new Play signer **cannot normally update the existing prototype-installed app in place**. Migration would require a separately approved, verified backup/recovery procedure; never casually uninstall or clear user data.
4. Approved public, reachable HTTPS privacy policy URL and verified developer support email. The existing `release-site/` pages in PR #213 are disabled drafts, not eligible Play privacy/account-deletion URLs.
5. Live, verified, authenticated cloud-account deletion flow including Supabase Auth, profiles, sync heads/snapshots, storage objects and fencing of future writes. The Supabase project currently has no deployed Edge Functions. No user should be promised a submitted request until this is actually operational.
6. Real handset acceptance: backup export/readback, cold relaunch, 3x voice alerts, custom Files music grants, Done/Snooze, hourly/Doze/boot recovery, small/large font and OEM widgets.
7. Play Console data safety, foreground-service types and full-screen intent declaration videos, target audience/content rating, Google OAuth brand verification as applicable, and production-signed AAB.

## Production gate order

**Gate A:** owner provides non-secret decisions (package ID plan, developer contact, site host, Play Console status).  
**Gate B:** test privacy and account deletion *end to end* using separate disposable test user with explicit non-production data; verify Storage deletion.  
**Gate C:** protect `main` and audit full stacked PR ancestry, then freeze approved source.  
**Gate D:** provision private upload key securely outside repo; bundle release AAB, inspect signatures and exact package, perform Play internal testing.  
**Gate E:** physical-phone/signed-Play upgrade & restoration acceptance.  
**Gate F:** Play closed test if applicable (personal accounts created after Nov 13 2023 generally need 12 opted-in testers for 14 continuous days), then apply for production access and publish only after policy/review gates complete.

## Stop conditions

Do not merge historical draft stack, expose service-role secret to browser or app, send deletion requests from an unsigned/public page, wipe preview data, upload prototype-signed package to Play, mark publication successful or claim Play approval in absence of actual Play response.

## Parent release references
- Exact Build272 QA: https://github.com/rayadinaveen98-ship-it/framebynavin-android/actions/runs/41198817937
- Release gate: https://github.com/rayadinaveen98-ship-it/framebynavin-android/issues/174
- Key migration: https://github.com/rayadinaveen98-ship-it/framebynavin-android/issues/180
- Main ruleset: https://github.com/rayadinaveen98-ship-it/framebynavin-android/issues/185
- Play policies: https://github.com/rayadinaveen98-ship-it/framebynavin-android/issues/187
