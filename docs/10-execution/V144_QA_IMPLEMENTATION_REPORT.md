# Backlot V144 — Implementation & QA Report

## Candidate
- Branch: `feature/v144-guide-schedule-refresh`
- Final validated app/test commit: `0658238d5519373104371fac51de08fb75473e0f`
- V143 baseline: `5fd5430584d3a72c23b78ed73bd07fa85a315b59`
- Version code: `144`
- Version name: `2.0.0-rc17-v144-workflow-insights`
- Application ID: `com.framebynavin.app`
- Debug signing: existing `prototypeStable` key, preserving in-place debug upgrade compatibility from V143.

## Scope implemented

### Guide characters
- Added Funny and Cute as first-class guide identities alongside Frame and Navi.
- Added 10 approved transparent WebP assets under `res/drawable-nodpi`.
- Existing Frame/Navi vector renderer is preserved.
- Funny/Cute render directly from approved raster artwork; Compose only applies restrained breathing, mirroring and state motion.
- Creator Setup and Guided First Run use the selected V144 guide across their semantic poses.
- Guide picker is available from Settings.

### Weekly schedule V5 naming
- `Frames of the Day · IG Reel + YT Short + X`
- `Daily Movie Recommendation · IG Reel + YT Short + X`
- `10 PM Music · Instagram`
- `Why This Scene Works`
- `Every Cinematic Moment`
- `Every Cinematic Moment Promo`
- `Friday Movie Review`
- `Review Promo`
- `Cinematic Analysis`
- `Cinematic Analysis Promo`

Migration keeps the stable `fbn_v4_*` schedule IDs so existing generated occurrences are updated rather than duplicated. Untouched old preset names migrate automatically while creator-customized slot titles/settings remain preserved.

### Settings
- Replaced the single long Settings stack with the V144 category hub.
- Categories cover Profile & Account, Appearance, Guide Character, Voice, Notifications & Reminders, Planning & Calendar, Connections, Data/Backup/Sync and About Backlot.
- Existing functionality is retained through category/sub-screen routing.

### Insights performance
- Cached Insights render immediately.
- Core YouTube analytics can publish before deeper audience/reach work finishes.
- Deep reports refresh behind the core snapshot instead of blocking the whole surface.
- Existing freshness/cache behavior remains in place.

### Ideas
- Reused the existing V117 speech-recognition component inside New/Edit Idea.
- Final speech transcripts append into editable Notes; partial speech is not duplicated into the model.
- Added reminder controls for Off, 3h, 6h, Tomorrow, Daily and Custom.
- 3h/6h/Tomorrow/Custom are one-shot. Daily is the explicit repeating option.
- Idea reminder notifications support Open, Make Project, Snooze 3h and Stop.
- Open/Make Project deep-link to the exact idea; Make Project opens conversion flow.
- Reminder reconciliation automatically removes stale alarms for deleted, archived or converted ideas.
- Recovery covers app reconciliation, boot, package replacement, time/timezone changes and exact-alarm permission changes.

### YouTube revenue
- Normal Insights keeps its existing core read-only YouTube scopes.
- Revenue uses separate incremental `yt-analytics-monetary.readonly` consent.
- Declining/cancelling revenue consent does not break normal Insights.
- Added 7D, 28D, 90D and This Month periods.
- Direct metrics include estimated revenue, estimated ad revenue and playback-based CPM.
- RPM is explicitly calculated as estimated revenue / views * 1000 rather than presented as a native YouTube metric.
- Revenue has a separate cache keyed to channel ownership and is cleared on disconnect/channel change.
- Revenue UI communicates that YouTube monetary analytics are estimated/delayed rather than real-time.

## Regression coverage
- Weekly V5 default names, stable IDs, locked publish times and custom-title preservation.
- Idea reminder one-shot/daily behavior, missed-reminder recovery and automatic stop after archive/conversion.
- YouTube revenue period/calculation behavior.
- V144 guide registry and pose mapping for every semantic pose used by Setup/Guided First Run.
- Home hero smoke assertion aligned with current Backlot branding without modifying production UI.

## Automated QA
Final workflow run: `#312` / `35730440527`

- Unit tests: PASS.
- Android lint: PASS.
- Instrumentation test APK compilation: PASS.
- Debug APK assembly: PASS.
- APK artifact upload: PASS.
- Targeted connected emulator UI smoke on `macos-15-intel`, API 35: PASS.
- Connected smoke result: `7/7` tests completed, `0 skipped`, `0 failed`.
- Earlier Linux emulator attempt is excluded as an app signal because it lacked usable hardware acceleration and crashed before discovering tests.

## Final artifact
- Artifact name: `Backlot-v144-Workflow-Insights`
- GitHub artifact ID: `10694844823`
- Artifact ZIP digest: `sha256:e8acb951badd0a687a345a9417224396ccf55cd653d88b55da204f73c1d6698b`
- Extracted APK filename: `Backlot-v144-Workflow-Insights.apk`
- Extracted APK size: `25,168,450` bytes
- Extracted APK SHA-256: `5821e7c79a594e8746bd4078ded6256ed339105ad096c8f8166ed17866c22122`

## Release decision
V144 is QA-complete on the validated candidate. Unit, lint, instrumentation compilation, debug build/upload and the focused connected emulator smoke suite are all green. The generated debug APK is suitable for in-place testing over the V143 debug install because the application ID/signing configuration is preserved and versionCode advances to 144.
