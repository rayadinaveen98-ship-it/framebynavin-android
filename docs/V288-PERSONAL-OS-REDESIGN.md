# Backlot V288+ — Personal OS / Spaces Redesign

**Status:** UX + architecture proposal, not implemented UI or a signed APK.
**Baseline:** V287 verified owner-preview (nap wake, delivery health, 21-day
planner, existing reminder engine). Protect existing installed owner data.

## Product thesis

One place to **plan → act → log → reflect**. A Reminder is a time-based action;
a Personal Space is the ongoing context, progress, journal, photos and goals
behind many related reminders. Do not create one Space per alert by default.

**Home must be for action, not setup**. Keep the current Creator/Personal
switch (or compact equivalent) and existing active reminders. No large setup
cards above the day's list.

## IA

Personal mode's 4 top-level destinations:
- **Today**: one next action, compact daily progress, chronological reminder
  agenda, compact Spaces row, clear create action.
- **Spaces**: all active and archived personal journeys. Preset icons, colors,
  goals and reminders for each Space. Spaces can be deleted/archived without
  deleting linked Personal reminders by default.
- **Progress**: day/week/month confirmed Done activity, scoped streaks,
  notes and optional measures. A missed notification is NOT evidence the
  user failed to perform a habit.
- **Settings**: all setup/configuration and diagnostics.

## Home hierarchy (small-screen first)

1. Header: 'Personal' / compact Creator switch, notification health icon
   **only when actionable**, '+' quick add, settings gear.
2. Today/weekday, optional daily intention line.
3. One primary 'Next up' card, named icon, exact due-time context,
   **Done / Snooze** actions only when a live due/posted receipt permits them;
   never fake acknowledgement of a future recurrence.
4. Thin 'Today' progress line derived from confirmed Done events and defined
   schedule eligibility; pending != failed; add clear numerator/denominator.
5. Chronological reminder list with Today, 7 Days, All filter. Pending and
   needs-attention rows must remain visible. Minimal row: icon, action title,
   time, state, trailing overflow or valid actions.
6. Compact horizontal 'Your Spaces' row for 2–4 favorites, then 'See all'.
7. No long explanations, no permission API paragraphs, no confirmation buttons
   for unrelated installers, no horizontal strip of all 21 dates on Home.

## Settings migration (no data changes)

- Life Plan: 21-day reusable cycle templates, install/edit, start date,
  quiet alerts and flexible calendar planning. Do not keep an immutable Nov
  2026 hard-coded scheduler as the only future workflow.
- Routines: Diet & Fitness V1, nap schedule, personal daily repeat settings.
- Alerts & delivery: precise alarms, notifications, full-screen access,
  permission shortcuts, battery guidance, recheck and quiet hours.
- Follow-ups, voice & music, backup / restore and privacy settings.
- Keep actionable permission problems summarized in a *small* Home icon; full
  diagnostics live in Settings.
- Preserve existing V285/V286/V287 installers and current PersonalReminder
  IDs. Rehousing composables must not auto-install, delete or reschedule.

## New model (separate from the current reminder DataStore)

Use Room to hold structured local-first objects:
- Space(id, title, type, icon, accent, description, createdAt, archived, order)
- SpaceReminder(spaceId, reminderId, role) link table (many reminders per
  Space; simple enough to migrate existing categories without changing IDs)
- SpaceGoal(id, spaceId, metricType, target, period, displayUnit)
- SpaceEntry(id, spaceId, date/time, type, note, metricPayload, source)
- SpaceMedia(id, entryId, privateFileId, thumbnailId, capturedAt, consent)
- SpaceTemplate(id, recommendedHabitSpecs, checkInFields, icon)
- SpaceActivityLink: explicit reference to one PersonalCompletionEvent's
  immutable (reminderId, occurrenceMillis) stable key; never count a dismiss or
  automatic timeout as completion.

Room owns records; app-private internal storage owns image bytes. Keep existing
PersonalReminderStore + PersonalCompletionJournal for notification delivery.
Use schema versions and migration tests; no wiping old records.

Current PersonalDone history is capped and deleting a reminder removes linked
history. V288 Spaces must preserve **independent journal entries** and
check-in observations so a deleted notification does not silently erase a
long-term habit timeline. Never retroactively fabricate Done.

## Space examples

- **Skin care**: grouped AM cleanser/moisturizer/SPF, PM serum/moisturizer,
  simple Done checkboxes, photos (baseline, optional day 14/28), notes,
  side-by-side or drag-to-compare before/after, privacy-first. Lighting/time/
  angle instructions; visual difference != proven product efficacy or medical
  diagnosis. Optional 14-day check-in reminder, OFF by default.
- **Fitness**: workouts, sets/reps/load or activity minutes, walking minutes,
  recovery notes; display trends using user-entered values.
- **Reading**: book currently reading, 10PM reminder, pages/minutes logged,
  quotes and finishing dates; leave content owned by user.
- **Editing / filmmaking**: sessions, time spent, skill practiced, learning
  notes and milestones. Optional explicit link to a Creator project; never
  write into CreatorTask implicitly.
- **Game development / gaming**: separate templates for coding/prototyping
  and recreational gaming. Sessions, milestone, project notes.
- **Custom**: category, icon, activity, measurements, notes, files,
  reminders; all optional.

## Quick Add

Tap '+' -> compact bottom sheet:
- **Reminder** / **Start a Space** / **Log Progress**.
- Suggestions from explicit preset catalog and local title keywords only
  (zero cost, offline, no deceptive remote 'AI' claim).
- A 'sunscreen' title suggests Skin Care with 'AM SPF' routine; owner can
  create a standalone reminder instead. Never auto-create a Space.
- A 'read novel' title suggests Reading with default 10 PM; owner can edit.
- Each template's default alert type, snooze and repeat are visible before
  save; new routine install remains opt-in.
- Advanced controls (recurrence, voice music, sound and retries) collapsed by
  default, expanded on demand, never silently reset during edit.
- Icons are meaningful and accessible with contentDescription and 48dp+ touch
  targets; text and large font scaling must remain legible.

## Photo / media design

Use Android **system Photo Picker** (no broad gallery permissions).
Copy only the approved photo to app-private storage and generate bounded
thumbnails. Strip or avoid preserving location EXIF. Offer optional notes.
Delete media with its entry only after deliberate confirmation.
Create side-by-side and draggable split-compare with dates; both images retain
normal zoom/crop independently. No automatic face detection or sensitive
classification. Permission for camera only if an in-app camera is explicitly
added later.

**Backups:** existing PersonalCompletionBackupCodec has a bounded JSON payload;
do not embed image bytes/base64 inside it. Versioned metadata export and
a separately opted-in image export/import with verified checksums, size
limits and authenticated encryption/secure user-mediated transport. Show
what is/is not backed up, and no silent remote image upload. Enforce
restoration of references only when corresponding media verified.
No mandatory paid API.

## Progress and trust

- 'Done' and logged metrics count only after user confirmation.
- Future reminders, notified events, dismissed and unanswered are separate.
- Daily progress denominator includes only eligible planned events that
  occurred by the evaluation moment, not all future events as failures.
- Milestones are user-chosen, not shame-based. Streaks optional; a missed day
  must not label the person as a failure.
- Sensitive photos hidden from lock-screen notifications and share previews.
- One-tap archive, export, delete, restore/recovery controls and predictable
  failure responses.

## Visual specification

Elegant cinema-inspired dark surfaces; warm restrained accent, meaningful
category icons (outline or filled consistent set), no giant warning-colored
buttons for ordinary actions, no tiny gray explanations as core content,
one obvious primary action per screen. Card height targets:
- Next Action hero 110–150 dp
- reminder row 56–72 dp
- space tile about 112–140 dp
- short settings tiles with chevron and status
Preserve contrast, screen-reader order, dynamic type and 48dp targets.
Design for 360dp Android phone first and then tablets.

## Engineering release plan

### V288 — Clear Today / Settings split
- Move giant V285–V287 setup cards behind Settings > Personal.
- Real action-facing Today dashboard & chronological list.
- Keep existing backend, notification receivers, IDs, alarm leases.
- Snapshot/Compose tests in phone widths and 200% font scaling.
- Prove no migration or reminder count changes.
- **Gate:** user-visible UI review + Android 15 test PASS + phone QA.

### V289 — Personal Spaces foundation
- Room Space + ReminderLink + entry journal, migrations, templates,
  archive/deletion semantics and independent log history.
- Space browser, detail screens, scoped progress.
- **Gate:** cold start, low-storage, database migration, restore and no
  orphan references.

### V290 — Skin Care progress vertical slice
- Preset grouping, reminder linkage, private Photo Picker, day-14 comparison,
  user notes, deletion and export. No broad media permissions.
- **Gate:** photo picked, rotated, duplicate, permission revoked, backup
  roundtrip and Android lifecycle test on two devices.

### V291 — Fitness/Reading/Creative tracks
- Metric form schemas, optional progress charts, habits/templates and
  explicit Creator links, offline use.
- **Gate:** independent counts, no fake streaks, accessibility.

### V292 — Guided Reminder Builder + release hardening
- Contextual preset suggestions, compact form, inline preview and advanced
  settings. Reliability/permissions and cloud/backup coverage, privacy,
  Play policy, battery/OEM and signed bundle release checks.
- **Gate:** no regression in V284–V287 alert, snooze, recurrence, completion,
  backup, and media export. Real-device owner approval required.

## Immediate acceptance checks for V288

1. Personal tab opens into Today with no setup card above agenda.
2. Reminder cards appear within one viewport at normal device height.
3. User can see their upcoming action, active alert state, and click '+'.
4. Settings holds all existing 21-day/diet/nap/follow-up/permissions panels.
5. No reminders are reinstalled, renamed, automatically completed,
   deleted, rescheduled or duplicated when switching between tabs.
6. Back navigation, Creator switch, old notifications, snooze and full-screen
   alert routes still work.
7. Fix contradictory counts such as '0 of 38 diet slots saved' shown near
   '38 ACTIVE' by giving each count clear scope/label.

**V288 is a proposed implementation phase until code, tests and APK exist.**
