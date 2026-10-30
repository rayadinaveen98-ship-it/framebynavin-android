# Backlot V227.1 — Personal Reminder editor inset and accessibility hardening

## Why this tranche
The original Personal editor used a separate Android Dialog window with default decor fitting, a 92% height Surface and another imePadding inside. This can apply gesture/navigation offsets differently than the Activity's full-screen editor, while the fixed two-column SAVE/CANCEL row can become cramped on narrow phones or at large text scaling. The earlier V216 Idea Editor already uses a full-window edge-to-edge dialog with the safeDrawing and IME insets consumed in one place. This tranche applies that existing proven contract to Personal.

## Implementation
- Personal editor Dialog opts out of platform decor fitting, uses full-window Surface and a content Column with WindowInsets.safeDrawing + imePadding before normal padding. The form body remains weighted and vertically scrollable so action buttons are fixed above the real keyboard.
- V2271PersonalEditorFooter uses full-width primary SAVE above CANCEL for width under 380dp or system fontScale at least 1.3; standard wide/default-font devices keep the old horizontal row. Both buttons have at least 52dp touch height, 13sp text and stable SAVE test ID; CANCEL gains its own test tag.
- Original PersonalReminderDraft.checked(), validation, recurrence, schedules and onSave behavior are unchanged. This does not cross into Creator, YouTube or backups.
- JVM breakpoints and Android instrumentation test the *real Personal Editor dialog*, fixed action bounds, large-font IME behavior, and save of a Telugu reminder through a synthetic in-memory callback.
- Full app versionCode **250** developer-signed test APK for in-place update over V227 249; not a Play Store release.

## QA gates
- Current V227 full JVM/lint, signer continuity, preexisting V226 28 Android regression cases and Android 11/15 New/Edit Idea UI matrix.
- New Personal editor tests on Android 11 and Android 15 with gestural navigation. No user data modifications are performed in synthetic keyboard tests.
- Physical phone tests on OEM keyboard, 1x to 2x text, gesture navigation and font/display scale remain mandatory. V226 backup export real-provider acceptance and reminder Doze/boot testing still open. Legacy widget providers remain intact.
