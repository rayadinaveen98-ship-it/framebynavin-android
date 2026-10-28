# Backlot V224 — Widget Trio (engineering preview)

## Audited baseline
V223.4 has nine legacy registered widget providers: Creator Compact, Creator Desk, Quick Idea, New Project, Current Project, Next Reminder, Content Calendar, Daily Brief, Creator Insights. Several duplicate actions and visual treatment. They are **not** removed from user devices in this first V224 milestone, to avoid silently breaking pinned widgets on an Android launcher. No CreatorTask, PersonalReminder or Ideas data model is deleted.

## Three replacement widgets
1. **Quick Capture (compact):** vibrant surface, separate large +IDEA and +PROJECT controls. +IDEA opens existing real QuickIdeaActivity (text/voice capture); +PROJECT opens existing new-project flow. The widget itself never silently creates records.
2. **Today Focus (medium):** actual currently active project title, today's due count, next Creator reminder *time*, and independent Today/Creator Reminders destinations. It deliberately doesn't fabricate a project if none exists.
3. **My Day (medium):** actual upcoming Creator reminder count / next time and Personal reminder count / next time. **Private Personal reminder titles and notes NEVER enter RemoteViews**, including error/empty states. An unreadable Personal store displays "Open to check", never "zero". Personal button opens existing Personal Hub via its established deep-link action; Creator button opens Creator reminders.

## Update model
- Existing CreatorWidgetUpdater calls the V224 renderer whenever Creator tasks refresh.
- Personal commands, responses, received notifications and missed recovery best-effort refresh widget data. Widget failure is never a prerequisite for saving a reminder, nor will it reset the Personal DataStore.
- Android AppWidgetProvider updates and 30min periodic fallback ensure launcher recovery. Widgets use Android RemoteViews, no unsupported embedded Compose or silent direct actions.
- All actions are explicit immutable PendingIntents with V224 URI namespacing; no direct Done/Snooze interaction on a non-private launcher.
- UI uses scalable sp, independent 45–48dp tap targets, maxLines/ellipsize for large fonts and darker high-contrast surfaces; final real-device and tablet legibility remain owner QA.

## Migration safety
- First deliver trio and test visuals. Legacy 9 providers remain until owner validates trio on actual launcher.
- Next release: offer a transition guide and remove old provider definitions only after handling existing pinned instances; never delete project/idea/reminder data or uninstall app.
- Keep prototype personal sideload signer, package `com.framebynavin.app`; V224 preview versionCode 245 only, not Play signing.

## Verification gates
- Full JVM, lint, APK builds and signed in-place upgrade from V223.4 244 to V224 245.
- JVM privacy summary tests: counts, next time, paused and posted/expired filters; corrupted Personal store shows "Open to check".
- Android 11 provider manifest and layout registration test; prior V2234 missed recovery, V2233 voice/alarm, V2232 Personal UI, V2231 response and V222 project-cold-start regression.
- Android 12–16 widget resizing/OEM launcher, high font scaling, talkback, app-in-background widget refresh and physical phone visual approval pending. This PR is draft until then.
