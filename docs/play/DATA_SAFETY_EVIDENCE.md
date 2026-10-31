# Backlot — Data Safety and Permissions evidence gate

**NOT A PLAY CONSOLE DECLARATION.** Source audit at Build272 (31 Oct 2026) plus read-only inspection of Backlot's connected Supabase project. Do not infer legal answers from this draft.

## Confirmed code/backend facts

- Application `com.framebynavin.app`, targetSdk 36, minSdk 26. Public prototype owner-preview APK is NOT Play-signed.
- Core Creator data and separate Personal Reminders persist locally. Exported Personal JSON can contain private titles and notes and is not encrypted. Backup files outside Backlot's storage remain user controlled.
- Optional Creator Google authentication/Supabase cloud backup can send account information, project snapshots and app-owned media off device.
- Optional YouTube authorization / analytics, Calendar access and Google/Firebase AI may transfer user-authorized information to those providers.
- Voice Idea and supported on-device speech recognition use microphone permission when invoked. Speech providers can vary by device.
- Android requests notification, calendar read/write, record audio, exact alarms, full screen intent, boot recovery, wake locks, vibration and media-playback foreground-service permissions.
- Live Backlot Supabase project read-only inspection on Oct 31: public `creator_profiles`, `creator_sync_snapshots`, `creator_sync_heads` tables have RLS enabled; **zero deployed Edge Functions**. Existing disabled website deletion request preview does not implement real account deletion.
- Supabase security advisor reports leaked-password protection disabled. Verify the actual sign-in methods and address where applicable; not necessarily a Google-only OAuth blocker.

## Data Safety fields needing evidence from device, backend, SDK and account owner
| Data flow | Action required before Play Console submission |
| --- | --- |
| Google login / email / user identifier | Record purpose, optionality, encryption in transit, retention and account deletion |
| Creator cloud snapshots/media | Inventory exact fields and Storage buckets; test user export and reliable complete deletion |
| YouTube channel/analytics | Record OAuth scopes, connected permissions, sharing and handling/retention |
| Calendar read/write | Confirm only contextual consent, chosen event scope and externally synced events |
| Voice Ideas/microphone | Verify recording/storage locality and external transfers (if any) |
| Firebase/Google SDKs and AI | Audit SDK telemetry, diagnostics and request data sent to Google |
| Personal Reminders | Confirm no network sync of private titles/notes; export/restore behavior and local deletion |
| App analytics/crash/identifiers | Verify runtime third-party SDK automatic collection, not just direct app code |
| Data deletion | Test complete authentication account + Supabase Creator tables + Storage/media cleanup, with verified receipt and consistent rollback/retry |

## Play permissions review required
- `USE_FULL_SCREEN_INTENT`: **high rejection risk** for multipurpose creator organizer; Google generally auto-grants only core alarm/call use. Document opt-in access and blocked/denied notification fallback; decide whether removing this permission is safer, then regression-test real device. Do not claim core alarm-app exemption without proof.
- `FOREGROUND_SERVICE_MEDIA_PLAYBACK`: three audio services must satisfy policy and Play declarations. Prepare demonstration videos for each, user-visible purpose, interruption impact and Android 15+ boot restrictions.
- `SCHEDULE_EXACT_ALARM`: special access may be revoked/denied; prove permission education and fallback without exaggerating timing reliability.
- `READ_CALENDAR`/`WRITE_CALENDAR`/`RECORD_AUDIO`: verify contextual, user-selected feature prompting and runtime use.

## Verified official references (checked 31 Oct 2026)
- Android API 36 deadline: https://support.google.com/googleplay/android-developer/answer/11926878
- Play privacy-policy requirements: https://support.google.com/googleplay/android-developer/answer/9859455
- User data and deletion: https://support.google.com/googleplay/android-developer/answer/13327111
- Foreground service / full-screen intent: https://support.google.com/googleplay/android-developer/answer/13392821
- Data safety: https://support.google.com/googleplay/android-developer/answer/10787469

## Non-negotiable release condition

**Do not claim Play review readiness or submit a Data Safety form** until developer identity/support contact, exact data handling, signed production package, public policy URL, functional in-app and external account-deletion experience, runtime evidence, and owner signoff exist.
