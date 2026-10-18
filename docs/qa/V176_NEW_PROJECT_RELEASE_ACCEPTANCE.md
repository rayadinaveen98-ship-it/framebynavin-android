# Backlot V176 — New Project P6: End-to-end acceptance and release decision

Source branch: `feature/backlot-v176-new-project-p6-release-qa`
Stacked base: V175 P5 > V174 P4 > V173 P3 > V172 P2 > V171 P1 > V170 Settings preview.
**Release state: acceptance candidate, not stable. No main/stable merge, no user-data wipe, no schema migration.**

## Contract and acceptance evidence

| Journey / property | Automated | Manual creator/device | Gate |
|---|---|---|---|
| Full 7-step default; Quick Create independent | Static V161–V176 contracts + unit policy | Launch full vs shortcut | Both required |
| Empty title / elapsed due / invalid custom time rejected | JVM save-parity tests | Create and change date/time on a phone | Both required |
| Creation saves title/type/notes/voice/plan | JVM policy, Android Compose flow, TaskStore Android roundtrip | Create and reopen a real project | Both required |
| Notes belong beside project title | Compose V175/V176 tests | Full wizard, existing editor, long notes | Both required |
| Reminder Off, Light, Guided, Urgent, Custom + advanced modes | V156 Android UI test, JVM plan parity | Schedule and observe OS alert/voice/alarm; missing permissions | Both required |
| Unchanged overdue deadline and snoozes survive unrelated edits | V171 JVM + V176 persisted Android record | Update an actual overdue/snoozed project | Both required |
| Unsaved drafts: Keep Editing / Discard | V172 Android UI tests + JVM signature | Android Back, closing project, cancellation | Both required |
| Draft through Settings/Profile, then return | Saveable state and navigation source guards | Navigate Settings/Profile and back | Both required |
| Large fonts, scrolling, 48dp targets | V173 size contracts + Android UI tests | Compact screen at 100%/130%/200% font | Both required |
| Existing backup and Settings unaffected | V166–V170 inherited regression scripts + Android Settings UI tests | Backup, restore, check previous records | Both required |
| Disk-save failure / signed in-place upgrade | Optimistic-write error-source checks, no destructive test | Inject write failure on a test device; test signed upgrade | **Manual mandatory** |

## Automated release candidate pipeline
- V161–V175 inherited code guard scripts + V176 release guard.
- Full `:app:testDebugUnitTest`, Android lint, compile device tests and assemble a separate, signed debug preview APK.
- **Run** selected Compose and TaskStore instrumentation cases against an Android 11/API 30 emulator. Failure must fail the emulator job (no continue-on-error).
- Upload preview APK, emulator test XML/reports and log artifact even on failure.
- Preserve exact CI run ID, build commit and APK checksum in PR.

### Do not overclaim emulator coverage
Compiling instrumented tests does not run them. Android 11 emulator execution does not verify OEM alarm restrictions, battery optimization, voice engine installation, notification permissions on Android 13+, background/full-screen requirements, inaccessible locales, keyboard on small physical displays, or creator-owned data from an earlier signed stable app.

## Manual release checklist
1. **Data backup:** Export and verify existing Backlot project/idea data from the currently trusted version. Do not uninstall it.
2. **Signing identity:** Compare signing certificate for previous stable and candidate; if mismatch, use a separate test device/install. **Never uninstall a data-bearing installation to bypass a signature mismatch.**
3. **First launch:** App welcome, account/onboarding navigation, existing projects, reminders, Quick Actions and Settings all work.
4. **New Project:** Create using all 7 stages. Add title, type, style, platform, format, deadline, optional notes, plan, delivery and project voice. Reopen and check each saved field.
5. **Quick Create:** Five essentials + persistent button; new draft starts due 24 hours ahead; switch platform and format; change to Custom with invalid time, check Create disabled; correct time and save; More Options and return preserve choices.
6. **Edit:** Open overdue project, modify notes/title only; verify exact old due, existing progress, snooze, working pause, voice and acknowledgment survive app restart.
7. **Draft safety:** Keep Editing preserves values; Discard drops only unsaved draft; open Settings/Profile, return and verify same draft/step. Recreate app activity and repeat.
8. **Delivery:** Run Off/Light/Guided/Urgent/Custom on the real phone, exercise Voice, Alarm, Notification and Smart as supported, request permissions, reboot and test next occurrence. Record OS version and OEM.
9. **Backup/restore and regression:** Restore exported data in an isolated test installation, verify records, projects, ideas, schedules, insights, app Settings. No duplicate reminders or unintended overwrites.
10. **Accessibility:** Compact screen and 100%, 130%, 200% font sizes, system dark/light where relevant; keyboard, scrolling and footer actions must be usable.
11. **Failure and rollback:** Simulate storage-write errors on a test fixture; no false “saved” claim or silent data loss. Verify a safe exit/restore path.
12. **Creator sign-off:** Review screenshots and 1st-hand app behavior. Only after all gates are checked may stacked branches be considered for merging in dependency order.

## Open issues/risks
- Accepted writes are still queued optimistically; a later failure triggers existing global recovery. Durable save acknowledgment is not yet implemented.
- Saveable drafts do **not** promise recovery after reboot/force-stop.
- Reminder OS delivery depends on permissions, OEM settings, actual schedules and device testing.
- No production-signed stable build can be established from a debug preview alone.
- V170–V175 PR stack is unmerged; never label a V176 passing debug build as a stable release.

## Sign-off criteria
**Automated QA passed** = JVM, lint, compilation, emulator runtime and APK upload all success for the exact same head commit.
**Ready for creator device QA** = automated QA passed and APK checksum supplied.
**Stable approved** = creator has completed and accepted the manual checks, signing/upgrade is verified, and the relevant PRs have been reviewed and merged.
