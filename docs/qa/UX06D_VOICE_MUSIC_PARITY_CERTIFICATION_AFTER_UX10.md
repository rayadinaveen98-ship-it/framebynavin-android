# UX06D — 21-Day Challenge Voice/Music Parity Certification (POST-UX10, NOT RUN)

**Status:** TESTS SPECIFIED AND SOME UNIT TEST SOURCE UPDATED; NO TESTS EXECUTED.
**UX06D source parent:** UX06C draft #406 at 5355ece419828ecc45a7f71c36c6c6838a634b00.
**Protected baseline:** V337 unchanged. No merger, deployment, APK or release authorized at this stage.

## Invariant ledger

1. Existing v1/v2 Challenge Voice = 3 announcements, 5-second gaps, no music; exact completed-day timestamps preserved.
2. v3 explicitly stores music mode/URI, count 1..5 and exact selected allowed gap; all new fields covered by checksum.
3. V297 legacy 21-activity planner remains disjoint from the single-activity 21-day Challenge.
4. No spoken alert, preview, musical tail, scheduled broadcast, notification or elapsed timer creates Done, Undo, Snooze, Stage Done, reward, or inferred completion.
5. Only reviewed Edit can change Voice settings on a saved challenge; progress, snooze and day notes cannot edit audio.
6. Quiet and Alarm never start background music. Custom audio stays a SAF URI, no file/media bytes are included in backups.
7. Per-challenge setting never changes Creator global music, Personal music, other challenges or current device's stream volume.
8. A stopped/changed challenge cannot keep old music playing or claim new notification/Done. Posted notification remains separately actionable.

## Required post-UX10 cases

| ID | Trigger / fixture | Acceptance |
|---|---|---|
| D01 | V1 archive decode after upgrade | Voice mode OFF/3/5s, original confirmations and notes unchanged, no phantom music |
| D02 | V2 archive with archived and active challenges | Same historical defaults; archived stays unscheduled |
| D03 | V3 encode/decode with music OFF | Exact settings, IDs, schedule, links and receipts survive |
| D04 | V3 encode/decode with Piano and three repeats | Exact round trip, checksum matches |
| D05 | V3 with custom content URI, count five, gap 60s | Exact full round trip, no media content embedded |
| D06 | Corrupt v3 payload/checksum, missing new key, invalid enum | Import fails closed; original data remains intact |
| D07 | Unknown future v4+, old v2 app reading v3 | Fail closed instead of losing audio data; documented downgrade gate |
| D08 | Custom `file://` or remote `https://` URI | Rejected before write, no unauthorized path read |
| D09 | Missing/revoked custom audio URI | Piano fallback, still valid notification/TTS; no crash or silent data reset |
| D10 | Fresh challenge Off (default) | No reminder armed; no music, no delivery receipt |
| D11 | New challenge Voice with Piano | Selected visible; settings committed once after Save; not before |
| D12 | Voice with No Music | Voice still speaks; no Piano/MediaPlayer |
| D13 | Voice with Custom SAF audio and persistable grant | Foreground audio plays locally, no broad storage permission |
| D14 | Select one of 1/2/3/4/5 announcements | Exactly N completed TTS utterances; same original occurrence |
| D15 | Select 5/10/15/30/60-second repeat gaps | Actual requested gap between utterances; no truncation or accelerated repeats |
| D16 | Long 5x45sec TTS plus 4x60sec gaps | Watchdog allows expected duration and bounded final tail |
| D17 | TTS init failure / unsupported locale | Record TTS unavailable, stop music/service; posted notification remains |
| D18 | Duplicate callback, failed utterance, stale generation | No additional speech, no stale error receipt or service interruption |
| D19 | Rapid switch between two challenge voice alerts | New generation owns sound, old callback cannot stop new alert |
| D20 | Snooze current challenge while music plays | Stops exact occurrence's speech/music; one correct snooze rearm |
| D21 | Change title/Voice options while sound plays | Old service/music stopped, new preferences saved; older sound not resumed |
| D22 | Archive/delete active challenge | Stop only matching challenge audio; other scheduled reminders untouched |
| D23 | Done / Undo / note edit while sound plays | Only explicit user action writes completion; no extra musical receipt |
| D24 | Change alert Voice → Quiet/Alarm/Off | No background music; original channel semantics preserved |
| D25 | Switch Quiet/Alarm → Voice after previous stored choice | Voice uses visible saved choice only; no silent mutation of Done history |
| D26 | Edit existing V2 Voice challenge | Editor starts Music OFF, count3 gap5s; can opt into Piano then save |
| D27 | Edit Cancel or permission denial in music picker | Original saved media setting and challenge progress untouched |
| D28 | Edit stale snapshot after simultaneous Done/snooze | Reject stale edit; preserve newer exact owner event |
| D29 | Change start date with existing Done or note | Still blocked, no rebase/erase due to audio schema |
| D30 | Challenge on phone locked, DND/silent, exact denied, notifications denied | Truthful existing fallback; no forced full-screen activation |
| D31 | 21-day Voice playback paired with Creator/Personal Voice | No duplicated or leaked music, handled audio focus/route and service lifecycle |
| D32 | Android process death, boot, timezone change while v3 saved | Exact schedule reconciliation, no synthetic progress or repeated audible late alerts |
| D33 | Restore v3 archive on different phone without Custom file | Metadata restored, missing file falls back predictably, content bytes not assumed |
| D34 | 320dp phone, 200% font, landscape, tablet | Voice settings scrollable, Save/Cancel reachable, no hidden destructive controls |
| D35 | TalkBack and screen reader on Create/Edit voice choices | Count, gap, Piano/Custom/Off and sample buttons semantically accessible |
| D36 | Preview Voice + Music then navigate away | Only sample phrase used, audio stops; never speaks private challenge title |
| D37 | Device in Battery Saver and no TTS | No runaway foreground service, ringtone lock or audio loop |
| D38 | Repeat custom file remove/move during active playback | Safe piano fallback, correct cleanup and no thrown unhandled exception |
| D39 | Other challenge records with same optional settings | Edit one does not modify another; merge/import still detects collisions |
| D40 | Recovery-copy restore and V337 before/after original data fixtures | Original owner snapshot and receipts bit-for-bit preserved in semantic fields |

## Deferred unit test source staged (not executed)

- `Phase1HabitCodecTest` updated v3 version assumption and v1/v2/v3 regression fixtures.
- `Phase1HabitVoicePlaybackPolicyTest` adds count five, gap 60s and watchdog bounds.
- `Phase1HabitManagementPolicyTest` adds exact receipt/notes/links preservation under Voice edit.

**No tests were executed.** Existing source tests are not a certification substitute.

## Evidence required to mark tested

- One cumulative post-UX10 Gradle compile + unit / instrumentation / UI / backup & restore CI with exact SHA/logs.
- Synthetic fixtures v1/v2/v3, corrupt JSON, missing/revoked SAF media, archived/off/quiet/alarm/voice.
- Distinct Android test device and API versions; screen capture of Create/Edit/multi-step, audio test log with real TTS, DND and Bluetooth.
- Measured service duration and battery/performance, background restriction errors, accessibility and selected settings continuity after app restart.
- Review of full music privacy grant and application backup semantics; output must never claim music files sync just because URI string is stored.
- Explicit owner acceptance of the default Piano for *new* Voice challenges and existing voice-only grandfathering.
- UX06A R01–R35, UX06B B01–B25, UX06C C01–C30, and later UX06E/F gates.

**Certification outcome currently:** UNTESTED / NO APK / NOT RELEASED.
