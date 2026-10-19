# Backlot V188 — Revenue and Audience Accuracy

Based on passing V187 preview at `63252dc7096ae0ab4e2a963970e48c6dad1fd863`.

## Implemented candidate
- Monetary Analytics requests target the exact selected channel rather than whichever channel the token considers MINE; API must authorize it. Monetary cache is channel- and creator-generation-owned at the final write; older unowned cache is rejected.
- Revenue metric evidence distinguishes real reported zero from empty/failed summary or ad reports. Estimated revenue, ads and CPM do not fabricate missing amounts; custom revenue-per-thousand-public-views is explicitly NOT labeled Studio RPM.
- Revenue uses Pacific-local reporting dates; the first day of a month is pending instead of requesting an unprocessed current day. Range switches invalidate delayed callbacks and force a refresh.
- Reach importer reports actual daily coverage (including zero-row reports), follows Reporting API pagination with a finite safeguard, and checks request/channel/restore ownership before each local cache edit.
- Existing core syncing and unrelated creator data remain unchanged. Minimal UI copy: necessary functional states only; full visual/copy redesign is V190.

## CI test gates
Full JVM unit and lint, inherited source checks, V187 instrumentation suite and V188 owner/coverage tests on Android 11 emulator. APK is an installable **candidate**, not stable. Authorized live-channel parity and physical-phone testing remain required.

## Follow-up validation
Compare Revenue with Studio for same channel/currency/reporting calendar (including mixed Shorts and long-form); verify denied monetary scope, rapid range changes, month-start, reach report backfill and Reporting API errors on device. Do not claim parity until verified.
