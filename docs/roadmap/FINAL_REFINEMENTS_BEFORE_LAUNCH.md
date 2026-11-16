# Final Refinements Before Launch — Backlot

**Owner:** Founder
**Record opened:** 2026-11-16
**Purpose:** A living, ordered register of founder-approved Backlot refinements to be implemented and verified before a future launch.
**Status:** Planning/approval register. This file does **not** represent implemented, tested, integrated, or released work.
**Change control:** Documentation-only record on a separate branch based on the preserved V337 baseline `678502f4626150ea1945b81eed1593a362377454`. Do not merge, change production source, delete features, build an APK, or release based solely on this record. Integration requires separate review and authorization.

## Refinement 01 — Four selectable welcome animations

- **Area:** Launch experience → Settings → Appearance & sound → Welcome animation
- **Decision:** **APPROVED FOR FUTURE IMPLEMENTATION** by founder (2026-11-16). No implementation in this documentation change.
- **Design reference:** The Living Frame seven-shot, five-second storyboard developed in conversation on 2026-11-16, explicitly as design exploration. Its generated board is a concept reference, **not** a functional animation.
- **Preserve:** The existing `V199ParticleWelcome` as the **factory default**, not a replacement. V337 source sets this animation to **3,000 ms**, uses a multicolor particle convergence into the Backlot wordmark, the `Built for Creators` tagline, and optional generated launch sound.
- **Offer four selectable styles:**
  1. **Particle Signature (original)** — existing V199 animation; remains default/fallback.
  2. **The Living Frame** — premium ribbon of light draws a radiant cyan/sapphire/violet/coral/gold cinematic frame; resolves into the Backlot brand and transitions to app.
  3. **Prismatic Bloom** — sculpted, translucent, colorful prismatic ribbons bloom and resolve into the Backlot identity; restrained, not cluttered.
  4. **Spark's First Light** — the approved Spark companion discovers/activates a light and launches a warm, colorful reveal. Use only approved Spark character assets and proportions; no unapproved redesign.
- **Selection UX:** Four visually coherent cards, selected indicator, separate full-screen preview, and immediate selection persistence across app restarts/device restarts/upgrades. User-selected style remains selected until the user changes it. Preview is not a commit action.
- **Settings navigation:** Add a dedicated **Welcome animation** destination under the existing **Appearance & sound** section, separate from app theme, companion choice, reminder scenes and launch sound preference.
- **Shared experience:** Preserve the approved Backlot wordmark and `Built for Creators` tagline; polished, coherent animation/audio identity. Respect the existing launch-sound toggle. Avoid network-dependent launch assets. Support reduced motion and a safe, rapid fallback to original/static branding if an alternative fails.
- **Launch routing:** Preserve the current direct routing for notification/widget/deep-link targeted launches, unless separately reviewed; do not block time-sensitive actions with an ident.
- **Compatibility:** No changes to reminders, 21-day challenges, user content, onboarding, companion preferences, settings data or backup/erase behavior as part of this feature.
- **Timing:** Original V199 = **3 seconds** in verified baseline. New sequences are conceptually **~5 seconds**. Whether returning normal launches play the full 5 seconds or a shortened version remains **OPEN**; do not silently impose a policy without founder approval.

### The Living Frame — proposed five-second visual/motion storyboard

| Time | Shot | Visual/motion |
|---|---|---|
| 0.0s | Black stage | Midnight navy atmosphere, tiny cyan-gold ignition |
| 0.8s | First spark | A single fluid ribbon of luminous light awakens and moves |
| 1.6s | Frame begins | Ribbon traces the first edge/corner of a cinematic rectangular frame |
| 2.4s | Living outline | Luminous, richly colored rectangular outline completes with restrained particles |
| 3.3s | Brand reveal | Backlot wordmark + `Built for Creators` resolved inside the living frame |
| 4.2s | Transition opens | The frame becomes a visual doorway toward the existing app experience |
| 5.0s | Landing | Brand-aware but calm finish and clean handoff to onboarding or Home |

**Art direction:** High fidelity, richly saturated but controlled; cinematic, distinctive, minimal composition; polished lighting, no jarring bursts. Suggested colors: electric cyan, sapphire, violet, coral and champagne gold. **Motion:** smooth continuous path; controlled acceleration/deceleration; logo legibility; avoid visual clutter. **Audio:** optional soft ignition, harmonic rise, elegant resolving note; obey launch-sound and device behavior.

### Definition of done (not yet met)

1. Four distinct, consistent animations accessible through Settings; current original remains default for existing/no-preference users.
2. Selection, persisted value, clear active state and preview function work after app/device restart and upgrades.
3. All four versions render/read clearly on phone/tablet sizes and follow light/dark system integration as appropriate; motion reduction supported.
4. Audio behavior respects launch-sound preference, silent mode/volume policy and failure resilience.
5. Existing widget, deep-link, notification and reminder routing stays immediate and correct.
6. Smoothness, memory, startup latency and low-end device behavior are measured; animation has a fail-safe exit.
7. Current V337 baseline is preserved and regression tests pass; physical-device acceptance completed before any merge/release.
8. Final review of storyboard, assets, timing policy and implementation plan approved by founder before production-source edits.

### Approval and implementation ledger

| Step | State | Evidence / note |
|---|---|---|
| Collection concept (existing original + three alternatives) | **APPROVED** | Founder approval in conversation, 2026-11-16 |
| The Living Frame design as first implementation candidate | **APPROVED FOR FUTURE IMPLEMENTATION** | Founder request to note as first refinement, 2026-11-16 |
| Three concept refinements | **PROPOSED / TO FINALIZE** | The Living Frame, Prismatic Bloom, Spark's First Light |
| Animation assets and motion files | **NOT IMPLEMENTED** | No production changes |
| Settings selector and persistence | **NOT IMPLEMENTED** | No production changes |
| Tests, APK, physical validation | **NOT TESTED** | Separate future stage |
| Merge or release | **NOT AUTHORIZED** | Requires review and approval |

---

## Refinement 02 — Account choice, personalized first-run setup, and companion-guided onboarding

- **Area:** First normal launch after Welcome animation through Home.
- **Founder decision (2026-11-16):** **APPROVED FOR FUTURE IMPLEMENTATION** after review of the proposed user journey. Open design decisions still require resolution before source implementation. No app code changes authorized by this entry.
- **Intent:** A welcoming, premium identity -> personalization -> guided learning experience for new/guest users, with a direct Home route for existing signed-in users and no forced re-onboarding.

### Founder-requested journey

1. Welcome animation (respects chosen style and existing direct-launch bypass).
2. Initial choice: **Create a new account**, **Log in**, or **Use without an account**.
3. Sign up -> Google authentication -> new Backlot profile setup with **name** and unique **username**. Existing-account Google sign-in -> recognize returning identity.
4. Guest -> local-only entry with clear data-persistence and optional account connection later.
5. New-user profile -> choose approved companion (**Spark or Mira**), with previously designed expressive poses continuing consistently through setup and tour.
6. Ask guided questions: **What do you create? Where do you create/publish? Why do you create? How do you create?** Personal-only and Both users should have context-appropriate questions, rather than mandatory Creator-only questions.
7. Permission explanation and setup for notifications, full-screen reminder/alert behavior, microphone, etc.
8. Companion-guided in-app onboarding with optional concise voice announcements.
9. Companion closes the tour and user lands on genuine Home. Remove tour overlays only; user-selected companion preference remains intact.
10. Existing returning signed-in user with completed onboarding goes directly Home following account verification and any necessary honest data-reconciliation steps. Resume incomplete setup without destroying recorded progress.

### Experience refinements to review with founder

- Distinguish **identity** (Google account + Backlot name/username) from **work setup** (Creator/Personal/Both choice and four questions). Keep the existing focus segmentation introduced in Stage 6; do not force publishing questions onto Personal users.
- Treat sign up and login as separate initial user intents but allow a shared secure Google authentication mechanism. Handle account-already-exists and no-profile states without duplicating user profiles.
- Guest flow should remain functional and account-linkable later with deliberate, safe data-reconciliation/merge behavior and no unintended overwrites.
- Avoid collecting all permissions as a blocking wall. Provide one readable permissions explainer, then ask each Android permission in context (notifications when creating reminders, microphone when recording, special full-screen/alarm access only where platform rules allow). Handle denial, postponement and Settings re-entry gracefully.
- Proposed six-tour destinations (order for Creator/Both): **Home & Daily Brief**, **New Project**, **New Idea**, **Reminder Hub (Personal and Creator)**, **21-Day Challenge**, **Insights**. For Personal-only users, prioritize Personal Reminders, 21-Day Challenge and Home; Creator lessons can be optional/replayed later.
- Guided lessons should show real screens with carefully placed companion overlays, speak only if opted in, provide visible text/subtitles, and offer Next, Back, Skip and Replay from Settings.
- Tour should **not** create fake records or require first project creation. Finish to Home with companion exit animation, no blocking overlay. Companion remains selectable elsewhere.
- Returning-user login can skip onboarding **only** if authenticated identity and saved completion state can be verified. Account recognition is separate from the availability/coverage of cloud-restored data; disclose which Creator and Personal information is actually restored versus local-only.
- Do not lose existing users' setup state, data, current V337 behavior or draft Stage 6 work; reconcile new proposed first-run model with PR #377/PR #389 before future implementation.

### Open design decisions — founder approval required

1. Whether guest users must enter a local name or can defer profile identity.
2. Final question copy, optional answers and the conditional Creator/Personal/Both paths.
3. Which permission explanations appear during setup versus just in time; no forced OS permission approval.
4. Voice guide default OFF (recommended opt-in) versus ON; speech languages, replay and mute behavior.
5. Whether tour is fully skippable (recommended) and how Home and six-tour steps adapt to focus.
6. Account data restoration coverage, incomplete-profile resumption and guest-to-account merge UX, based on verified backend capabilities.

### Acceptance gates (all pending)

- New Google signup, existing Google login, local guest, account switch/reinstall, incomplete setup, offline/cancel/retry and mismatched account all have deterministic paths.
- Returning completed user never repeats setup; fresh and partial users do not get stuck or lose progress.
- Account profile inputs validated; unique username handling and authentication tokens secure. No mock or invented restored data.
- Appropriate companion assets and poses remain visually consistent; choice persisted.
- Conditional personalization works for Creator, Personal and Both, with no forced irrelevant fields.
- OS permission requests respect Android versions and user's denial; core non-permission features still work.
- Accessible six-destination tour with optional voice, text equivalents, skip/back/replay and correct Home landing.
- Usability, accessibility, physical phone/tablet tests, upgrade/data integrity and regression evidence pass.
- No source implementation, PR merge or release until founder approves final scope and integration gate.

**Status:** Founder-approved direction, **NOT IMPLEMENTED**. Some listed design decisions remain open for specification and testing.

---


## Refinement 03 — Meaningful personalization and professional Home identity/menu

- **Area:** Setup answer propagation; Home greeting/header; top-right companion/profile menu. Keep existing Home slideshow, Projects area and Reminder Hub UI unchanged.
- **Founder request (2026-11-16):** **PROPOSED; detailed feasibility reviewed in conversation**. Not implementation authorization. Do not change application files, production V337, merge or release.
- **Dependency:** Refinement #02's first-run choices must become durable, strongly typed profile preferences before being used in Home/Projects/Insights. Maintain Creator/Personal/Both segmentation.

### 03-A — Meaningful personalization, not decorative survey questions

- Capture selected answers with explicit meanings and optional/skip behavior:
  - **What do you create?** → primary creator mode and optional secondary modes.
  - **Where do you create/publish?** → destinations/platforms, distinguish personal/independent projects from published platforms.
  - **Why do you create?** → primary goal and optional goals used to suggest actionable project progress, not promises of performance.
  - **How do you create?** → solo/team workflow, production style, planning preference; ask only what is useful and support editing later.
  - Personal-only: contextual life/reminder/challenge preferences; avoid mandatory creator questions.
- Use these answers in **New Project** (appropriate defaults/templates), **project progress** (relevant stages, progress lens), **Ideas** (optional classifications), **Insights** (goal-aware, evidence-based summaries), **Weekly Focus** (goal/target), and **reminder suggestions** only as recommendations, never automatic scheduling or permission grants.
- User selection must never rewrite existing projects or records without explicit action. Separate identity and creator-mode preferences. Safe fallback for missing/changed setup answers.
- Verified current source: `CreatorPersonalizationEngine.snapshot` already derives goal/platform/week target from `CreatorProfile`; `V18TodayScreen` supplies `creatorProfile.safeDisplayName` to `V18CinematicHomeHero`. Refine this integration instead of creating duplicate parallel logic.

### 03-B — Greeting uses Backlot username

- Desired copy: **Good morning / afternoon / evening, <Backlot username>** based on local time and locale.
- Use the account's **Backlot username entered/claimed during setup**, **not Google first name, Google surname or Google display name**. Handle guest with deliberate guest display name or neutral greeting, and account-not-loaded / offline securely without sudden name flicker.
- Username is distinct from `CreatorProfile.displayName` in existing code; use a single scoped identity resolver shared by the greeting and My Profile, without confusing the creator name with the unique account handle.
- Clarify presentation of an optional `@`, capitalization as stored/allowed, Unicode, ellipsis and large accessibility font.
- Do not redesign Home hero or slideshow. Change text source and any necessary safe layout constraints only.

### 03-C — Turn the top-right V339 three-dot menu into a premium companion-avatar disclosure

- Preserve a **single top-right access point**; replace/decorate the present 48dp vertical-dot trigger with the **selected, approved Spark/Mira portrait** and a subtle three-dot affordance; no generic placeholder or changed companion identity. Keep adequate contrast, touch-target size, TalkBack/keyboard semantics and action indicator accessibility.
- Proposed menu order:
  1. **My Profile** → existing Settings → Account/Profile destination; do not open edit flow automatically.
  2. **Notification Center** → inline read-only preview of up to **3** true recorded activity entries from existing V306 Creator/Personal activity feed. When more than 3 entries, show **See more** → existing full Notification/Activity Center. Distinguish needs-action from history; do not call an action 'unread' unless a real read model exists. Support empty/loading/errors, no duplicate notification store and no auto-acknowledgement from opening.
  3. **Spark / Companion** → reuse the current V221.4 companion bottom sheet, suggestions and offline chat with no behavioral change. Menu text may identify chosen companion visually (Spark/Mira), while preserving existing functionality.
  4. **Gallery** → existing Home slideshow photo editor (unchanged artwork, selection, persistence and actions).
  5. **My Spaces** → existing V289 Personal Spaces destination, using current V340 safe route. Never duplicate Space databases or create fake Space items.
  6. **Weekly Focus** → existing `V20WeeklyFocusDialog` currently local to `V18TodayScreen`; do not confuse with `POverlay.WEEK` / Weekly Plan (a separate page). Route through explicit safe callback so the same dialog is opened.
  7. **Reminders** → expand inline to **Personal** → V2232 Personal Reminder Hub, and **Creator** → V131 Creator Reminder Center. This is navigation only, not a new scheduler or duplicate Hub.
  8. **Settings** → existing V144 Settings Hub.
- Group menu visually: Identity / Activity / Workspaces / Preferences. Prefer a scroll-safe, anchored expanded menu or compact bottom sheet at larger font sizes rather than a fragile multilevel flyout. One expandable section active at a time; proper Back / outside-tap behavior; no menu flashes that reset action state.
- **Optional additions for review only:** Sign in / Connect account for guests (through My Profile rather than crowding menu); Replay guide in existing Settings (not another header item). Avoid an unrequested premium, backup or destructive-action shortcut.

### 03-D — My Spaces and Weekly Focus relocation feasibility

- **My Spaces is feasible** as a menu shortcut immediately: existing Home `V340HomeSpacesEntry` calls the Personal Spaces owner workspace, read-only snapshot, no separate database. Removing its Home preview would be a noticeable information-architecture change, because the card currently displays featured Spaces and availability messages. Recommendation: **add menu shortcut first; retain Home card** until founder explicitly authorizes removal after reviewing discoverability, especially Personal-first users.
- **Weekly Focus is feasible with a small UI navigation refactor:** `V18TodayScreen` holds `showWeeklyFocus` and uses `V20WeeklyFocusDialog` with real profile, task and personalization state. Menu action should open **this same dialog**; not route to the separate `POverlay.WEEK` weekly planner. Recommendation: **add shortcut first; retain Home focus card** until founder confirms its removal.
- No changes to existing slideshow, Projects rendering, Reminder Hub section or original destination behavior at this stage.

### Technical / security / accessibility constraints

- Default selection for existing users and guest state must be safe. Avoid using Google account name, leaking another account's cached username after sign-out, or invalid cross-account data in the greeting/preview.
- Avatar must follow the saved companion identity and legacy preference behavior. Preserve current Spark/Mira assets; no unapproved artwork substitutions.
- Notification preview uses **read-only V306 activity policy**. Badge means attention/action pending, not necessarily 'unread'; Android posted != seen, and reminder action requires existing original action handlers.
- Menu routes use original existing navigation callbacks and maintain Back-stack/Back dismiss. Support text scaling (200%), TalkBack, landscape/tablet, focus order, empty states, dark/light appearance, performance and process restarts.
- No background photo/library reads, new scheduling, network calls on every menu open or changes to notification permissions.

### Proposed QA gates (all pending)

1. Creator/Personal/Both and guest/account username greeting with auth switching, offline, mixed case and long handles.
2. Every menu destination opens original destination; reminder subchoices and Weekly Focus/Weekly Plan distinguished.
3. Preview shows maximum three accurate, timestamped entries; >3 reveals See more, 0/partial/error handled honestly.
4. No accidental acknowledgement, reminder mutation, photo data change, Space database creation, project change or altered notifications from menu.
5. Companion avatar reflects selection and stays within 48dp or greater accessible hit target; open indicator semantics preserved.
6. Homescreen slideshow, projects and reminder hub screenshot/behavior regressions unchanged.
7. Source and physical-device testing across phone/tablet and accessibility font scales before any merge.
8. Review dependency/conflicts with V337 and Stage 6 onboarding work; no production change until separate approval.

**Status:** Founder-approved direction, not implemented or tested. Keep My Spaces and Weekly Focus Home cards until separate layout review.

---

## Refinement 04 — New Project identity, expressive reminder voices, and optional five-repeat speaking

- **Area:** New Project wizard / Quick Create / Edit project, advanced Creator reminder settings and voice playback.
- **Founder request (2026-11-16):** **PROPOSED; UNDER REVIEW**. Keep current reminders strong and stable. Improve project first-screen wording, make eight reminder voice personas more expressive and distinguishable (particularly Funny), add optional five spoken repeats. Do not implement from this note.
- **Source of truth inspected:** Frozen V337 `678502f4626150ea1945b81eed1593a362377454`, including `V20NewProjectWizard`, `V180ReminderTimingControls`, `VoicePersonaEngine`, `VoiceReminderService`, `CreatorTask`, `CreatorViewModel`, `TaskStore`, and `V178PlaybackLifecyclePolicy`.

### 04-A — New Project creation

- Standardize first section in **full seven-step wizard** and **Quick Create** to:
  1. **What are you making?** as the main heading.
  2. **Project title** required, one-line input.
  3. **Project notes (Optional)**, multiline input with meaningful prompt and no requirement to fill it.
- The full wizard **already** has `What are you making?` followed by title and notes; preserve its underlying workflow and refine labeling/presentation rather than rebuilding. Quick Create currently uses `PROJECT NAME` and `What are you making?` as a placeholder, and does **not** show notes in its minimal section; align copy and make optional notes accessible without abandoning quickness.
- Preserve following sections and their behavior: Type → Platform → Format → Due date → Reminders → Review/Create. Existing validation, editor draft state, project save, edit, stage progression and reminders are regression-protected.
- Continue relying on onboarding selections from refinement #02/#03 for **suggested** creator mode, content type, platform, format, production style and goal; show alternative options and allow explicit overrides; never change existing projects automatically.

### 04-B — Voice persona characterization: improve identity without pretending Android TTS is studio acting

- Current eight persona enum IDs (WARM, WOMAN, MAN, YOUNG, FUNNY, CARTOON, ALIEN, ROBOT) and existing stored settings **must be retained**. Current engine uses device offline voices, static per-persona pitch/rate and persona-specific text. Differentiation is currently limited by installed TTS.
- Refine UX/descriptions and the audible persona design with device-appropriate, locale-aware voice choice, concise persona-specific scripts, rhythm/segmented pauses where feasible, small unobtrusive sonic signatures, and honest fallbacks:
  - **Warm Human / WARM:** calm, reassuring, gentle confidence, low-pressure phrasing.
  - **Clear Human / WOMAN:** crisp, welcoming, conversational, precisely paced. Do not promise particular gender if device voice cannot deliver.
  - **Deep Human / MAN:** grounded, steady, authoritative, not booming or distorted.
  - **Bright Human / YOUNG:** upbeat, optimistic, slightly faster, still understandable.
  - **Funny / FUNNY:** warm, playful, natural setup/punchline cadence and subtle humor *only for low-priority content*, not just pitch increase; no jokes or delay for critical reminders.
  - **Cartoon / CARTOON:** bright, exaggerated expressive energy with restrained bouncy motion/accent and no harsh childish audio.
  - **Alien / ALIEN:** mysterious sci-fi phrasing with subtle spatial sonic character, intelligible and low volume.
  - **Robot / ROBOT:** precise, systematic diction and intentionally measured timing with a subtle electronic cue, while preserving intelligibility.
- Every voice must **preview with a consistent reference script plus an optional persona-specific line**, and preview must reflect actual device voice capability. Clearly disclose unavailable voice assets/language, avoid misleading claims of 'human' or native funny voices.
- Native offline-first delivery is the low-cost default. Better phonetic timing/voice selection and lightweight authored sonic signatures are options; professional pre-recorded voices or neural TTS need separate explicit licensing, privacy, download-size and operating-cost approval. **No paid speech service, data upload or subscription without founder approval.**
- Keep speech content exact for title, stage and priority: fun styling must never obscure actionable information. For urgent reminders, automatically prefer clarity over comedy, preserving selected persona identity when possible. Provide neutral fallback if a style cannot be honored.
- Ensure personal/challenge reminder systems are not unintentionally changed: first scope is **Creator New Project** reminders; any parity rollout requires separate review.

### 04-C — Speak this many times: 1, 2, 3, 4, 5

- Add options **Once, 2 times, 3 times, 4 times, 5 times** to advanced Voice & Alarm Timing, keeping **3 as the default** for new and existing projects unless founder later directs otherwise.
- Current V337 limit 1–3 is enforced at **UI policy** (`V180ReminderTimingPolicy`), **new/edit save** (`CreatorViewModel`), **persistent JSON read** (`TaskStore`), **voice service runtime** (`VoiceReminderService`), and its **watchdog budget** (`totalWindowMillis`). Changing only the visible selector is not functional.
- Preserve one reminder occurrence ID, no duplicate notifications, playback service and acknowledgment semantics. Next repeat must start **after previous complete utterance**, maintain selected gap, and STOP promptly on Done, Snooze, dismissal, playback failure or occurrence invalidation. A completed repeated voice cycle should preserve actionability if no response.
- Five repeats require an explicit **time budget** review: worst-case 5 × 45s utterance budget + 4 × 60s gap + 15s grace = **480s**, while current service caps its watchdog at 300s and wake lock about 310s. Therefore a naive switch to 5 would silently terminate long announcements. Rework bounded lifecycle safely or constrain repeat/gap/message length with transparent UI explanations; do not unboundedly retain CPU/foreground service. Test long title/notes, TTS stalls, duplicate alarms, audio interruptions, background limits and late OS delivery.
- **Repeat count != automatic re-alert**. The existing separate post-playback retry, Smart escalation, snooze and reminder schedules must remain logically independent.

### 04-D — Small, optional product improvements for founder review

- Add a concise pre-save **summary** of title/type/platform/deadline/reminder persona/repeat choice, reusing the existing Review stage; avoid extra wizard steps.
- Make project notes optional and nonintrusive; don't automatically read very long notes aloud by default after expanding repeat counts. Consider separate "Include notes in spoken reminder" opt-in after user review.
- On the voice picker, make listening before committing easy, and show a brief, accurate descriptor rather than marketing a capability the phone may not provide.
- When selected voices are unavailable in the device language, degrade to neutral, intelligible TTS or existing notification fallback; no made-up success status.

### Definition of done / acceptance gates (pending)

1. Both full wizard and Quick Create show requested first-section hierarchy, with title required and notes optional; no unrelated redesign.
2. Type, Platform, Format, Due date, reminder plan and Review unchanged except explicit enhancements; no lost drafts or wrong defaults.
3. Each of the eight stable persona IDs has a genuinely differentiated, intelligible preview on supported device/locale tests; Funny is expressive when speech resources allow, never false-promised.
4. 1–5 repeat choices persist and execute exactly the chosen number under ordinary successful conditions; default remains 3; 1–3 existing projects migrate unchanged.
5. Watchdog, foreground service, duration and wake locks safely handle 5 repeats, all gap values and long speech; no service hang, duplicate occurrence, premature cut-off or unintended auto retry.
6. Done/Snooze/Stop/interruption instantly cancel further repeats; notification and missed reminder remain correctly actionable as currently.
7. Run unit, instrumentation and actual physical phone tests under locked/unlocked, silent/focus, voice engine missing, 1.5x/2x font, locale changes, app restart and Android version restrictions.
8. Existing reminder scheduling, voice+music blending, Snooze, receipt truthfulness and Smart escalation regression suites pass, with V337 frozen.
9. Final founder approval of scope and isolated implementation before any code change/merge/release.

**Status:** Proposed only. Review complete at code-reading level, not an audio/performance or physical-device validation.

---

## Refinement queue

| No. | Refinement | Founder decision | Implementation status |
|---|---|---|---|
| 01 | Four selectable welcome animations; The Living Frame storyboard as first visual implementation reference | Approved for future implementation | Not started |
| 02 | Account choice, identity/profile, companion-guided adaptive first run and six-screen tour | Approved for future implementation; details pending | Not started |
| 03 | Meaningful personalization across projects/progress and a premium companion-avatar Home menu | Approved for future implementation; retain Home sections until further review | Not started |
| 04 | New Project first-section polish, expressive voice personas, and 1–5 spoken repeats | Proposed; under founder review | Not started |

**Next entries:** Add #05 and subsequent items in the order the founder provides them. Keep the same distinction between PROPOSED, APPROVED, IMPLEMENTED, TESTED and RELEASED. Never assume a documented item has shipped.
