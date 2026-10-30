# Backlot V224.1 — Widget resizing and local-day accuracy (engineering preview)

## Audit of V224
- My Day counted every future Creator and Personal reminder, not today's events.
- A pending Personal lease that was already overdue could fall through to a calculated next recurrence before the Personal coordinator had actually recovered it. The launcher showed a potentially fictitious upcoming item.
- V224 widgets had fixed-height text despite Android allowing users to resize them and increase system fonts. AppWidgetProvider did not react to APPWIDGET_OPTIONS_CHANGED.
- V224 widget provider updated every legacy widget as a side effect of refreshing one new widget.

## V224.1 implementation
- My Day counts remaining today in the user's device-local time zone. Every count and next time uses the same exclusive local-tomorrow boundary (handles DST correctly). Old unanswered or already-posted Personal tokens do not generate false future recurrences.
- Each new widget reads its own AppWidgetManager min-width/min-height options. Shorter than 175dp, narrower than 225dp or Android font scale at least 1.3 chooses compact mode: hides secondary copy, keeps paired action buttons, uses smaller but still scalable-sp typography. Widget minimum dimensions reduced to 170dp width and 112dp (Quick Capture) or 145dp (Today Focus/My Day) height.
- V224 provider handles widget-options change and uses bounded 9-second goAsync broadcast lifecycle. It reads data to update only the new trio; the nine legacy providers stay separate and existing pinned launcher widgets remain untouched until user-approved migration.
- Personal titles/notes are never passed to RemoteViews; counts and times only. Personal data errors show Open to check.
- Preview build 246, same prototype sideload signing identity as V224 build 245, same Android package com.framebynavin.app.

## Automated checks
- JVM: compact/full breakpoints and font scaling, today counts, overdue lease, local next-day cutoff and DST transition, privacy tests.
- Android: all three widget providers and RemoteViews still inflate; compact hide/show IDs exist; legacy provider stays registered.
- Full JVM tests + lint, signed personalStable APK + debug/test APK, Android emulator regression, signature/version comparison against exact V224 artifact.

## Still requiring owner phone confirmation
- Widget cells at 2x2/3x2/4x2 on multiple launchers; font scaling, TalkBack, visual clipping, real taps, process-death refresh. Automated green does not equal Play-ready.
- Only after new widgets are approved: plan gradual legacy widget picker retirement with pinned-widget migration. Never wipe projects, ideas or reminders or silently break pinned widgets.
