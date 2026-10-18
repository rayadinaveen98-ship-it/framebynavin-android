# Backlot V174 — New Project Phase 4: Quick Create simplification

**Base:** V173 P3 visual preview (which stacks V172 draft continuity and V171 safety).
**Branch:** `feature/backlot-v174-new-project-p4-quick-create`.
**Status:** Draft preview only. No stable/main merges or database schema changes.

## Product contract

- Keep the full **seven-step wizard** the default New Project experience. Quick Create is an explicit independent shortcut from Quick Actions. Its one-screen path retains all project fields and uses the same `PProjectDraft` and `saveTaskConfiguration` as full creation.
- Quick Create starts with just the **project name, platform, format, deadline and a single reminder summary**.
- The reminder summary opens all **five plan choices** and advanced delivery configuration; it starts collapsed. A CUSTOM plan always exposes the time picker even after choices are collapsed, so an invalid custom time is never hidden.
- New Quick Create projects default to **24 hours from creation** instead of just one hour. Only new Quick Create drafts use this default; existing projects and the standard seven-step wizard keep their prior behavior. The deadline is always visible and editable.
- A Create Project button and More Options link remain in a persistent footer while the content scrolls. The shortcut does **not** bypass validation for blank title, selected type/platform/format, future due date, or valid CUSTOM reminder time.
- More Options opens the original wizard including creator mode, content archetype, production styles, priority, notes and all advanced reminder settings. Back to Quick Create retains already-chosen fields.
- Existing project reminders, saved data, creator profile, App Settings return/draft safety, voice choices and V171/V172/V173 behaviors remain untouched.

## Automated acceptance
- Run V161–V173 inherited source-preservation guards and V174 quick-create guard.
- JVM unit tests confirm same readiness policy as the seven-step wizard, 24-hour quick default, and custom reminder validation.
- Compile Android Compose quick reminder summary test; check changed label and clickable disclosure.
- Android lint, assemble Android UI test APK and V174 debug preview APK.

## Real Android device sign-off
- Back up user data; test the preview separately or upgrade in place only with compatible signing. **Never uninstall** your data-bearing version.
- Open New Project normally: full seven-step wizard is still default, unchanged initial deadline.
- Open Quick Actions > Quick Create: first screen has five essentials and a visible footer action.
- Verify new quick project is due 24 hours ahead and may be changed using the date/time picker.
- Switch platform; ensure formats remain valid. Select a format and create a project; verify project type and all default creator DNA persist.
- Open the reminder summary; verify Off/Light/Guided/Urgent/Custom plans all remain selectable. Pick Custom, choose an invalid time, confirm Create is disabled and the date picker stays available even after collapsing choices; then set a valid time.
- Open advanced delivery, pick Voice or Alarm, visit reminder Settings and return. Verify selections persist and no accidental save is performed.
- Use More Options to change type/style/notes, return to Quick, verify all values remain, and create. Edit the saved project to confirm correct values.
- Test 100%, 130%, 200% font scaling and a small phone; sticky footer must remain tappable with keyboard shown or hidden.
- Confirm V171 overdue editing/snooze preservation, V172 keep/discard/draft continuation and V173 visual consistency on physical Android device.
- Creator sign-off required before stable promotion.

**Not provided:** durable autosave after process force-stop, verified OEM reminder deliveries, app-store production signing, and final release acceptance.

**Next:** P5 reminder/project details clarity, then P6 device acceptance and release.
