# Backlot Point 5 — Companion Selection Source Stage Ledger

**Founder approval:** "ok bro approved go ahead" after Point 4 source stage, authorizing work on Point 5. User requests an in-chat screen preview. No testing or APK until founder explicitly opens consolidated QA.

**Status:** SOURCE-STAGED / UNCOMPILED / UNTESTED / NOT DEVICE-ACCEPTED.

## Source and boundaries
- Repo: rayadinaveen98-ship-it/framebynavin-android
- Cumulative branch: feature/backlot-v337-point05-companion-selection-source-after-p4-20261123
- Parent Point 4 SHA: 564993946c862b3a6adab5d695211035820e537b
- Stable V337 unchanged: 815ba69775f75b6e5f25bb838c2c17b4f09c203e
- main unchanged. P1–P4 retained unchanged. No merge, release, APK, CI or executed test.

## Existing grounded assets
- Exactly two selectable guides already existed: FUNNY (displayed Spark) and MIRA.
- Approved transparent WebP sprite families for Spark: welcome, present, point, thinking, celebrate.
- Approved transparent WebP sprite families for Mira: welcome, present, point, think, listen, celebrate.
- Existing saved legacy identifiers FRAME, NAVI, CUTE remain readable; do not migrate or delete them.
- Existing V144GuidePrefs preference name/key retained; no new backend, download or paid art.

## Staged implementation
1. Replaced old first-run two small card grid with one large, focused portrait stage; two equal Spark/Mira preview tabs; three expression/pose chips (Hello, Thinking, Celebrate); one unambiguous Continue with <name> action.
2. Settings companion screen uses the same portrait and pose previews but does not mutate the saved companion when a preview is tapped. An explicit Use <name> action persists the choice.
3. Original V144GuideCharacter and V144RasterGuideCharacter continue to render the EXACT approved sprites at original aspect ratio; screen preview never redraws, changes skin, retouches, or distorts character art.
4. Portrait pose changes use a bounded 220ms crossfade and existing subtle breathing/sway. Reduced-motion preference removes physical character motion.
5. V144GuidePrefs.select now durably commits to the original SharedPreferences key BEFORE publishing its Compose state. Failed saves keep the old companion and display retry feedback. No write occurs while merely previewing.
6. Original companion presence, suggestions, reminder logic, navigation and safe activity icons elsewhere in app remain unchanged. No app-wide scene redesign, Home avatar crop or full-screen reminder intervention is included here without evidence/approval.
7. Pure policy tests source-staged, not executed.

## Deferred checks and risk register
- Kotlin compilation, imports/Compose API check and new test execution.
- Device portrait source-asset bounding boxes and fully visible posture for both companions on small/large screens.
- Preview vs saved companion persistence; activity/process death during preview; failure to commit.
- Legacy stored selections remain readable and available in original profile data; if legacy current guide is not one of two approved, fallback portrait is Spark WITHOUT changing stored data.
- Settings route and first-run route stay correctly gated after Point 4 sign-in/local entry.
- Accessibility: selection names, 48dp+ targets, reduced motion, keyboard/navigation, font scaling.
- In-chat preview is a design/markup approximation using source-approved assets, NOT a rendered or screenshot-verified Android app.
- Approved owner testing after all points are source-staged; no CI/build here.

**Point 6 not started.**
