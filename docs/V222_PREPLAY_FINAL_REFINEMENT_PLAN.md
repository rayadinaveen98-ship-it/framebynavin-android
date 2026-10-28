# Backlot V222 — final personal experience refinements before Play candidate
Status: **Design checkpoint + one implemented cold-start fix**. All six requests are scoped here; DO NOT treat visual concepts or personal reminders as implemented merely because this plan exists. Parent release is V221.5 personal stable, 2.9.0 versionCode 240. Never modify production/personal-stable branches while these are in development.

## 1. Approved icon: stronger saturation, same geometry
- Canonical artwork is `res/drawable/backlot_v198_brand_mark.xml` (dark B spine with electric blue/violet, lavender and coral rounded panels); launcher itself uses `res/drawable-nodpi/backlot_v198_approved_icon.webp` in the adaptive icon foreground.
- Preserve **exact emblem, B silhouette, panel arrangement, proportions, icon masks, and brand identity**. Improve only gradient vibrance, local contrast and restrained highlights; keep crisp at 48, 72, 96, 192 px in dark/light launchers. Do not add typography, neon bloom on edges, or unapproved redesign.
- Confirm both current approved WebP/adaptive icon and vector source match visually before modifying. Make a side-by-side approval screenshot and verify all four launcher aliases, Settings icon preview and splash. Avoid swapping only the vector while leaving launcher foreground unchanged.

## 2. Cold-start missing projects
- **Confirmed code issue:** `CreatorViewModel.tasks` is a Compose mutableStateListOf that starts empty while DataStore loads. `PTodayScreen` previously memoized `remember(tasks)` and `remember(creatorProfile, tasks)` against the stable list instance: empty project queue/weekly focus could remain cached until navigating off/on Home.
- Implemented on this branch: snapshot list during composition; use `remember(visibleTasks)` for active queue and `remember(creatorProfile, visibleTasks)` for personalization. No storage mutation.
- Added `V222ColdStartProjectsUiTest` with real same-instance StateList initially empty, then insert and edit after screen first composition; run Android emulator and existing V220 Home regressions. Owner still needs cold-launch/relaunch check with existing populated projects and restore.

## 3. Circular selected companion: **plan first, implementation after design checkpoint**
- Place a **48–52 dp circular portrait at top right of the Home greeting**, directly BEFORE the existing gallery/slideshow add/manage button. Keep existing gallery button and photo controls untouched. The greeting must wrap rather than overlap on narrow phones; at >=1.5x font provide a responsive secondary row if needed.
- Use the **approved original** Frame/Navi/Cute V217 raster art and Spark raster art; crop inside circle with a soft border and subtle shadow. No redraws or generic robot.
- Idle state: visible portrait with no distracting blinking and no fabricated "insight". When V221.5 evidence engine returns a real actionable item and companion is enabled, show a subtle single pulse / small status ring (respect reduce-motion and battery saver).
- Tap: a compact, accessible ModalBottomSheet opens the **same read-only V221.5 suggestion card** (action, Why?, Not now), plus the current selected companion. If no suggestion, show a brief neutral status such as "Nothing needs your attention right now", not invented facts. The same stored dismissal and preference apply.
- When sheet passes real-device approval, remove the old inline Home card and its spacing. Do not keep duplicated suggestions or reduce priority of Top 3 / Up Next. Companion off in Settings hides the avatar or neutralizes its notification affordance, consistent with the opt-out decision. Use a single shared decision and callbacks to prevent card and avatar disagreement.
- Verify screen-reader labels, minimum 48 dp hit target, no overlap with gallery +, font up to 200%, Home on short/narrow screens, tab return, force close/reopen.

## 4. Voice and alarm visuals: deepen pigments, not childish animation
- Current shared scene is `V195ReminderParticleScene`: multicolor ambient wave forming CLOCK/MIC from the same particles. Its alpha drops to 40% after reveal and a further 26% black overlay is applied. This strongly dims the field after controls appear and likely explains perceived dullness.
- Retain existing unified clock/mic shape, choreography and response buttons. Tune hue vibrance, midtone contrast and layered selective radial glow; reduce double-dimming only while maintaining readable WCAG-level action contrast. Voice: electric cyan/blue/violet with restrained magenta. Alarm: amber/coral/pink/violet, not flat red.
- Profile performance on low/mid-range Android: cap particles/glow draw calls, keep 30/20fps and battery saver/no-motion fallback; never alter reminder scheduling, timeout, snooze or exact-alarm semantics. Test locked/unlocked screens, 200% fonts, color contrast and notification/action accessibility.
- Compare before/after screenshot in reveal and unrevealed states.

## 5. Replace the nine legacy widgets, don't remove user data
- Manifest currently registers 9 widget providers: Compact, Large, Quick Idea, New Project, Current Project, Next Reminder, Calendar, Daily Brief, Insights.
- User requests to delete all old widget options. After design approval, remove **only obsolete provider declarations and obsolete widget rendering assets** in the replacement build, not projects/reminders/recordings/account data. Warn that existing placed legacy widgets will disappear on update; do not quietly break user home screens while withholding replacements.
- New intentional widget set proposal:
  1. **Quick Capture** small: Add idea, New project, Personal reminder.
  2. **Today** medium: top active project/next reminder (personal and creator visibly distinguished) plus Open button. No fake live updates.
  3. **My Day** large: small agenda of real reminders and project tasks, actionable Done/Snooze only via secure explicit action/confirm, else deep link to app.
- User picks final 2–3 widgets and chooses personal/creator filters in widget setup. Use size-adaptive, battery-conscious Glance/RemoteViews, Android-specified touch and vertical-scroll gestures; never background-loop for UI updates. Persist config per widget ID and preserve privacy on lock screen.
- QA new installs, upgrades with old widgets, widget resize, phones/tablets/foldables, dark/light, app force stop and reboot.

## 6. Personal reminders: separate mode on one Backlot foundation
- Favor in-app **Personal** space, not second APK (for now). Creator projects/ideas remain independent, exactly as before.
- Shared reminder delivery engine/notification/audio reliability only; **separate personal reminder entities and storage/IDs**, explicit category and privacy boundaries, independent search, completion, recurring rules, and backup schema. Don't map personal reminders to fake CreatorTask entries: this would pollute projects, Insights, YouTube stats, smart nudges and on-screen work context.
- Personal setup entry in Reminders Hub: segmented **Creator | Personal**; Quick create from Home/Quick Capture and planned widget. Personal fields: name, date/time, optional repeat, priority and alert method (gentle notification / voice / alarm), Snooze/Done/Reschedule.
- Distinct personal naming on notifications/lock screen, user-controlled privacy toggles and quiet hours; no personal content sent to connected YouTube/cloud features without separate consent. Reuse verified alarm/voice system with namespaced occurrence IDs and explicit type routing, safe for boot/reboot/time zone changes and duplicate scheduling.
- Assess recurring schedules, overdue logic, backup/restore, locale and time-zone, accessibility, battery optimization and Android/Play exact-alarm permission compliance before feature launch.

## Release sequencing
1. **Fix and verify issue #2** (already patched; automated test pending).
2. Present compact-avatar placement to owner first; review logo and reminder before/after visuals.
3. Implement approved icon/particle/companion updates with safe defaults; test phone and large fonts.
4. Remove/replace nine widgets as one migration (not a period of broken widgets).
5. Build Personal Reminders as a separately staged, schema-versioned feature; release only after backup+delivery regression matrix.
6. Device matrix: compact/default/large phone; Android 11/13/15/16; 7-11 inch tablet; orientation/IME; low power and low RAM; restore and in-place upgrade. New Play signing bundle and policy declarations after complete owner QA.

## Don't claim completion until
- Source builds and all tests pass; physical-phone cold-launch Home state verified with preexisting data.
- Visual changes approved with current canonical artwork preserved.
- Reminder delivery, acknowledgment, backup, Restore and widgets cross-tested after migration.
- No Play Store production keys present in repository; original V221.5 stable remains rollback checkpoint.
