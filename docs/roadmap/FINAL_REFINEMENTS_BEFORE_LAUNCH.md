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

**Status:** Founder-approved for future implementation; **NOT IMPLEMENTED**. Audio/performance/physical-device validation remains open.

---


## Refinement 05 — Professional Settings architecture, account connectivity, scenes, voice, automation clarity and help

- **Status:** **PROPOSED; founder review pending** (2026-11-16). Architecture, code and read-only service-log audit only. Do not modify Backlot source, live data, cloud configuration, user permissions, or produce APKs under this documentation-only request.
- **Dependency:** #01 (welcome animations), #02 (account/onboarding), #03 (companion portrait and profile), #04 (eight reminder voices). Preserve frozen V337 and independent P0 draft/reliability certification.
- **Primary goal:** Keep useful Settings behavior and improve organization, comprehensibility, authenticity and professionalism. Do not add backup/restore options back into Settings after previous owner request.

### 05-A Account

- Keep signed-in user details, Backlot handle, identity edit, creator profile edit and connected-account states. Use founder-selected companion portrait (Spark/Mira) as the Account DP; this is an avatar/visual reference, **not** a new uploaded profile-photo store.
- Connection entries: Google account and existing YouTube integration, with accurate disconnected/connecting/connected/error states and direct existing routes. No misleading promise of complete data restoration.
- Make visible identity consistent with #03: Home greeting uses Backlot username, not Google name. Preserve separate user display name and Backlot handle.
- Remove stale profile copy referring to **Settings → Backup & Data** when that destination was deliberately retired. Avoid resurrecting prior backup/restore/delete settings.

### 05-A1 P0 external service blockage — Google sign-in / Supabase egress

- V337 `CloudConfig` routes Backlot to project `kukkqgpzxnfanynbddiw` (FrameByNavin Creator OS). Supabase read-only project metadata reports `ACTIVE_HEALTHY`; nevertheless, read-only last-24h unified logs show **six HTTP 402 responses for edge path `/auth/v1/token`**. User sees **restricted due to exceeded egress quota**.
- Root cause assessment: Restriction-class response at the Supabase Auth token endpoint is **verified**. The quoted **egress quota** is the user-observed restriction reason and is consistent with Supabase documentation; exact bandwidth source, organization quota consumption and billing-cycle end **not yet verified** by usage/billing access.
- Auth implementation: Android Google ID-token sign-in calls `/auth/v1/token?grant_type=id_token` through `CloudApiClient`, and non-2xx messages are surfaced as `CloudHttpException`. Repeated Google sign-in cannot bypass a Supabase service restriction.
- Action plan (read-only until separately approved): inspect Supabase org Usage → Egress, billing cycle / restrictions, largest contributing service, repeated full-snapshot reads, high-frequency requests and possible storage downloads; preserve tokens and user data. Fix excessive transfer only after scoped approval and tests. Add accessible, actionable error copy for restriction, retry only when service recovers, preserve local guest behavior. **No plan upgrade, spend-cap change, credit-card change or purchase without founder approval.**
- Supabase documentation: https://supabase.com/docs/guides/platform/manage-your-usage/egress and https://supabase.com/kb/troubleshooting/http-status-codes.
- **Release blocker:** account sign-up/login must pass on a real device with correct organization quota and account restoration expectations; absence of dashboard restriction alone is not a test result.

### 05-B Appearance & Sound information architecture

- Retain current **Director's Cut** and **Light** appearances only (V337 already has only these two selectable; keep retired IDs for backward compatibility).
- Keep and clarify **Launch sound**; add **Welcome animations** selector from #01.
- Keep **Home Photos** behavior/editor intact.
- Add genuinely distinctive **App icon** candidates for explicit selection: approved original plus cohesive cinematic, minimal, and light variants. Existing four `BacklotAppIcon` entries all currently use the **same** approved icon drawable; do not present aliases as visual choices before generating and testing different production assets. Use safe launcher-alias toggling and verify icon caching/upgrade.
- Consolidate **Reminder scenes** into exactly one menu destination, not a duplicate entry routing back to Appearance. Keep independent Voice and Alarm background selection.
- V337 `V391ReminderScene` includes **Director's Cut, Aurora Silk, Liquid Chrome, Celestial Drift, Crimson Pulse, Glasswave, Ember Field**. Each needs a differentiated original art/motion spec, rigorous visual preview, correct text/control contrast and battery/reduced-motion fallback. No mere recoloring with unconvincing name.
- **Retire `V394` Premiere / Full-Screen Experience visual presentation completely** as requested, *not* platform full-screen alarm/notification access, voice playback, locked-screen delivery, Done/Snooze or other essential alert routes. Audit all `V394` production/test references first, migrate selected/persisted Premiere preferences to the normal reminder scene, remove obsolete settings/preview/renderer/asset paths only after separate approval and QA evidence. Keep safe rollback plan.

### 05-C Companion and voice

- Companion screen stays as-is, including approved Spark/Mira preference and helpful suggestion toggle. Account DP reuses selected companion artwork.
- Voice page gets a clear hierarchy: **Default reminder voice**, **Background music**, **Preview**, **scope/explanation**. Cross-reference #04's eight persona refinement; avoid duplicating per-project configuration.
- Existing `V273ReminderMusicPicker` offers Soft piano, Choose music, No music; Play background music; Test voice + music; and **Stop preview always shown**, even when not playing. Replace with a stateful mutually exclusive Play/Pause-or-Stop control: Stop appears only during actual preview; live loading/playing/stopped/failed states, clear error when file unavailable, automatic stop on leaving page.
- Trim excessive explanatory copy and border cards. Keep concise contextual info only: global default vs per-project overrides, and where locally selected custom music is stored. Preserve SAF read permissions and do not scan device files or upload media without consent.
- No paid TTS or music service without separate pricing/license/privacy review and founder approval.

### 05-D Notifications & Alarms

- **Presentation-only restructure**: first Status/Health and permission readiness, then Reminder defaults (snooze/voice/alarm options), Quiet Hours, Unanswered Follow-ups, Helpful project reminders with link to Automation details, Android advanced access under a clear expandable section.
- Preserve current actual scheduling, service permissions, Snooze, Voice, Alarm, Smart escalation, retry/acknowledgement and delivery history, without claiming exact OS guarantees or making a permission read an acknowledgement.
- Clear statuses (granted/missing/system restricted) and corrective navigation, with accessibility at large font sizes.

### 05-E Planning & Integrations education (preserve existing functionality)

- **Weekly Auto Plan:** approved weekly templates can generate missing draft Creator projects over the next 14 days; background WorkManager checks roughly every **6 hours**, not an exact scheduler; never automatically publishes videos or modifies owner-edited existing projects. Existing schedules can create project reminders when configured.
- **Helpful reminders / context nudges:** optional project-aware notification suggestions chosen from existing task/activity evidence; approximate WorkManager checks every **6 hours**, subject to eligibility/cooldown, notification permission and pause/disable; **not** an alarm, project mutation, or guaranteed arrival time. Explain how to pause and what triggers them.
- **Regular reminders/routines:** separate optional Daily Brief, Weekly Creator Review and Idea Vault Review notifications checked by WorkManager roughly **hourly** and gated to configured time windows. Current displayed defaults: Daily Brief after 8:00 AM IST; Weekly Review Sunday after 7:00 PM IST; Idea Review Wednesday after 7:00 PM IST. Android timing approximate.
- **Normal project reminders** are distinct scheduled due-date/reminder alerts managed by the Creator/Personal reminder engines, not the automation routine digest. Explain these four concepts with one practical example each and add relevant status/last-run details only when verified.
- Preserve existing weekly planning and Google Calendar integration and its actual permission/export semantics. Do not add new automation behavior without owner review.

### 05-F Help & About

- Rebuild mostly empty current page (V337 currently only **Show app tour** + **Version**) as useful, searchable/contextual **Help Center**:
  - Getting started / replay companion onboarding
  - Projects and stages, Idea Vault, Personal and Creator Reminders, Scenes/Voice/Music, 21-Day Challenge and Insights
  - Planning, automation explanation and sample workflow
  - Account/sign-in, cloud limitations and offline use
  - Permissions / missed alarms troubleshooting and device-specific readiness
  - FAQ / issue-report support channel **only if verified configured**; no fake contact/chat guarantees
  - App version/build, What's New / changelog, privacy/terms links when verified
- Keep help content concise, accessible and available offline for essential FAQs; truthful about what is and isn't backed up.

### 05-G Privacy & Permissions and Advanced

- Privacy & Permissions is stable; no broad redesign now. Only clarify navigation/duplicated permission rows when needed by other settings.
- Source audit of **Start fresh on this phone**: `FreshStartActivity` requires two checkbox acknowledgements, exact phrase **ERASE THIS PHONE**, a second confirmation, then Android `ActivityManager.clearApplicationUserData()`. This is **device-only app-private data** erasure, not a deletion of cloud accounts. Not executed; **no real-device destructive test**.
- Fix obviously stale copy such as **BACK TO BACKUPS**, and instructions that imply a Backups Settings destination still exists. Explicitly warn about local-only media/challenges/Spaces possibly not being recoverable, especially while Supabase login is restricted. Preserve owner-approved Erase local data capability and confirmation gates.
- Physical QA of clear-data functionality must use a fully disposable test install/account and **separate explicit owner approval**; never exercise it on their current data.

### Quality and release acceptance gates (pending)

1. Account DP matches chosen companion and Google account-state screens, offline errors and reconnect flow are honest.
2. Supabase API restriction diagnosed, transfer causes quantified from usage/billing and login restored; no payment incurred without owner consent.
3. Exactly two selectable themes; distinct real icons; no duplicate scene entry; welcome selector integrated from #01.
4. Premiere-specific presentation retired with all calling code/asset/preferences audited and compatibility-safe fallback, while OS full-screen reminder actions remain untouched.
5. Every scene convincingly different, previews accurate for voice/alarm, all controls legible and reachable, works in reduced motion.
6. Voice/music preview lifecycle never shows an idle Stop action, no simultaneous/missing playback, preserves audio focus and user assets.
7. Notifications/Alarms and Planning/Integrations UI reorganized with no changes to schedules, permission semantics, existing data or reminder delivery.
8. Help content matches actual app behavior and version, replay works, accessibility tested.
9. Device reset remains protected and cloud-safe, with stale/backups text removed; destructive behavior not called in live environment.
10. Before release: full unit/instrumentation, actual Android test, performance/contrast/talkback/back-stack verification and owner sign-off; integrate only after P0 baseline gate.

**Record state:** Founder-approved for future implementation; **NOT IMPLEMENTED**. Source/code and read-only log evidence available; visual/device acceptance pending.

---

## Refinement 06 — Format-aware professional Project Stages and Content Workspace 3.0

- **Founder request (2026-11-16):** Investigate whether stage lists and Creator Content Workspace are accurate, professional, genuinely format-aware and better than generic repeated layouts; deep-research creator needs and competing products; propose full redesign if evidence requires it.
- **State:** **RESEARCHED / APPROVED FOR FUTURE IMPLEMENTATION** (pilot-first, approved by founder 2026-11-16); NOT IMPLEMENTED or TESTED.
- **Report:** [CREATOR_WORKSPACE_DEEP_AUDIT_2026-11-16.md](CREATOR_WORKSPACE_DEEP_AUDIT_2026-11-16.md) (complete baseline code audit, creator needs, competitor comparison, five sample workflows, architecture, risks, stage plan and acceptance criteria).
- **Finding:** `CreatorWorkflowV2` provides stage diversity across different types, but current `V19ContentWorkspaceAlpha6Hub`, shared Plan/Script/Publish editors and `CW3StageDesk` present mostly universal controls. `StageWorkspaceRoutingPolicy` maps stage IDs to a small number of shared destinations and some V2 templates interpolate platform/format only in labels rather than production requirements. This matches founder-observed sameness. `V2285WorkspaceFocusPolicy` can suggest scripts for formats that don't need them.
- **Protect:** Existing CreatorContentWorkspace / TaskStore authority, CreatorContentDna, Legacy template fallback, stage-owned data scoped by project/template/stage IDs, revision-fenced drafts, distinct Stage approval / checklist / Script readiness / Cut review / publication facts, independent derivative outputs and real reminder scheduling.
- **Recommended direction:** **Workspace 3.0 as progressive, additive redesign**—one cohesive Project Cockpit with true contextual working modules and versioned recipe packs for different formats. Start with five pilots: cinematic analysis long-form, Short/Reel, podcast, tutorial, and carousel. Later expand true original short-film, music, news etc. Do not build many cosmetic clones or claim Backlot edits media.
- **Core UX requirement:** Each stage must expose its actual purpose, real expected artifact, relevant editor/links, status/readiness, next action, and honest completion evidence. Inputs and validation depend on mode+archetype+production style+platform+delivery format, not a stage name alone. User override remains allowed without losing source history.
- **Missing capability proposals:** structured scene/claim-evidence maps, format-specific beat/shot/take/guest/slide editors, output revision and device media availability, platform-aware packaging and truthful release checks, better Next Move prompts. No mandatory paid cloud/AI services.
- **Risk and gates:** Version-pin template IDs, avoid silent migration of existing stage records, preserve output/reminder epochs, guard old export/import, physical phone/tablet and multiple-project testing, accessibility/performance checks. Current study is static code and documented industry research, **not** a physical UI certification or live rival app trial.
- **Approval boundary:** Founder has approved this pilot-first direction for future work. This record is not authorization to modify production source, merge, release or deliver a signed APK without a separate controlled implementation gate.

---

## Refinement 07 — Four selectable cinematic reminder intro animations, companion-aware main screens, and notification icon quality

- **Founder request (2026-11-16):** Shared reminder visual pattern across **Personal, Creator and 21-Day Challenge** Voice/Alarm: pre-reveal animation holding a visually formed microphone/alarm icon; user double-taps to reveal existing main reminder controls over the separately selected animated background; chosen companion visible on main screen; three new high-quality intro animation alternatives in Settings in addition to original; improve corresponding notification icons.
- **Decision:** **PROPOSED / AWAITING REVIEW**. Full source audit and design direction documented. No implementation, device testing or production edits authorized.
- **Dedicated specification:** [REMINDER_VISUAL_EXPERIENCE_REFINEMENT_07.md](REMINDER_VISUAL_EXPERIENCE_REFINEMENT_07.md).
- **Baseline:** Creator V337 currently has one-tap V195 colored particle Mic/Clock intro with V391 background after reveal. Personal V2233 currently displays reminder controls directly with V336 signal art and V391 background; 21-Day uses a distinct Phase1Habit notification and voice service with no shared intro. Claiming all three already support double-tap would be false.
- **Four intro choices:** **Original Particle Formation** (original/preserved default), **Prism Forge** (refracted luminous crystalline ribbon assembly), **Signal Observatory** (precision orbital/scanning geometry), **Ember Pulse** (sculptural warm light and atmospheric ember assembly). Each must draw an authentic Mic silhouette for Voice or Clock/Alarm silhouette for Alarm, not merely reuse a colored central sticker.
- **Settings:** Under Appearance & Sound → Reminder Scenes, add **Voice opening animation** and **Alarm opening animation** selectors shared consistently among Personal/Creator/Challenge; retain independent **Voice background** and **Alarm background** scenes from #05. No duplicate Premiere experience.
- **Main UI:** use existing actual chosen Spark/Mira identity and approved companion artwork, smaller anchored interactive-safe pose, without obscuring titles, privacy or primary actions. Creator/Personal/Challenge keep their own source-of-truth action handlers; appearance alone never acknowledges/delivers/completes/advances.
- **Interaction:** Double-tap as preferred artwork gesture with clearly visible single-tap accessible **Open reminder** alternative to avoid motor/TalkBack barrier, and OS notification Done/Snooze immediately actionable. User action chooses reveal; animation does not automatically claim task completion. Respect reduced motion, battery saver, locked-screen and background timeout behavior.
- **Notification icons:** Native monochrome Backlot small icons, distinct category affordance within OS limits (Creator, Personal, 21-Day) and Voice/Alarm status identified clearly in notification text/channel. Existing Creator/Personal small icon is Backlot; 21-Day currently uses generic Android alarm icon. Reuse chosen companion as meaningful private large art (existing V390), no animated/full-color status-bar icon, no private challenge/activity content exposed on locked screens.
- **Android/platform constraints:** Android 14+ limits FSI entitlement to qualifying alarm/call functionality. No automatic full-screen for regular quiet notifications/ordinary speech. Challenge currently routes to MainActivity/notification, so a genuine valid, permission-scoped challenge alert surface requires a new carefully authorized phase, not an unsafe route substitution.
- **Priority:** Visually ambitious but reliability-first, preserve delivery/occurrence tokens, playback, Done/Snooze/Stage Done, reminder backdrops, Personal lock-screen redaction, 21-Day scheduling/completion, follow-ups and all existing watchdogs. Physical device, accessibility, animation performance and regression acceptance gates specified in dedicated document.
- **Status:** Founder-approved for future implementation; NOT IMPLEMENTED or tested. Preserve OS/reminder delivery requirements.

---

## Refinement 08 — Personal Care, My Spaces and a coherent Personal Home

- **Founder request (2026-11-16):** Preserve all meaningful My Spaces capabilities and links to reminders, professionally restructure its UI, audit Personal Reminders, Reminder Groups, 21-Day Challenges and any other Personal features, and suggest a dedicated welcoming Personal place focused on meaningful self-care.
- **State:** **RESEARCHED / PROPOSED, awaiting founder review; NOT IMPLEMENTED or TESTED**. Only read-only source audit/public research and documentation.
- **Full report:** [PERSONAL_CARE_AND_MY_SPACES_AUDIT_2026-11-16.md](PERSONAL_CARE_AND_MY_SPACES_AUDIT_2026-11-16.md).
- **Baseline:** Personal already has a truthful My Day preview, offline reminder routines (My Routines), eight optional personal-purpose categories, recurrence/follow-ups and action records; Reminder Groups with linked reminders and batch Pause/Resume; 21-Day Challenges with one repeated action, manual Done, snooze and true daily progress; My Spaces as a Room-backed notes, goals, activity, metric and private-photo journal with links to actual reminders; a unified Personal reminder/challenge agenda. The core features and data must be retained.
- **Product strategy:** Create a **Personal Home / My Care** overview as a calm entry to the four pillars, not as a new database or duplicate planner. Show next actionable Personal item, optional gentle reflection, a curated preview of real Spaces, current Challenge and My Routines, and non-judgmental weekly history. Stay consistent with Director's Cut and Light. A user must never be punished for resting, skipping, or missing a day.
- **My Spaces redesign:** Preserve existing entries/photos/links/Room IDs. Improve main cards with actual recent activity and useful CTA. Within Space: Overview, Journal, Photos, Reminders and Progress, with one prominent Log progress and secondary Add Note/Add Photo. Preserve archive/restore/delete gates. No new automatic schedules or synthetic wellness scores. Add optional Space starter templates for Rest, Relationships, Learning, Nutrition, Wellbeing, etc. without migrating existing enum records or forced goal creation.
- **New feature proposals:** Optional 15-second daily self-check-in with minimal feeling/energy and optional reflection; truthful weekly reflection; personal chronological calendar and meaningful metrics. No clinical diagnosis, therapy claims, AI health analysis or cloud upload. A health-data privacy/schema/export/deletion approval gate is mandatory before implementation.
- **Critical data trust finding:** Space photos live only in app-private files; normal Backlot cloud does **not** restore photos or Spaces. Spaces' current metadata-only local export is plaintext and excludes private images and actual Personal reminder records. Preserve user safety, disclose device-only limits, review conflict with prior Settings backup/restore removal; do not claim reinstall restores Spaces or silently remove the Spaces utility.
- **Architecture/QA:** One read-only composition over existing PersonalReminderStore/CompletionJournal, Phase1HabitStore, V298ReminderGroupStore and V289SpacesDatabase, never merge underlying sources of truth. No cloned reminders, automatic challenge confirmation or fake receipt. Verify old links/photos/reminder actions, actual Android UI, offline, accessibility, account switching, upgrade/migration and dummy-data photo recovery before integration.
- **Recommendation:** Approve a progressive Personal Home and My Spaces usability redesign; prototype and validate with real users before introducing sensitive new tracking. No production code edits, cloud changes, merges or release based on this record.

---

## Refinement queue

| No. | Refinement | Founder decision | Implementation status |
|---|---|---|---|
| 01 | Four selectable welcome animations; The Living Frame storyboard as first visual implementation reference | Approved for future implementation | Not started |
| 02 | Account choice, identity/profile, companion-guided adaptive first run and six-screen tour | Approved for future implementation; details pending | Not started |
| 03 | Meaningful personalization across projects/progress and a premium companion-avatar Home menu | Approved for future implementation; retain Home sections until further review | Not started |
| 04 | New Project first-section polish, expressive voice personas, and 1–5 spoken repeats | Approved for future implementation | Not started |
| 05 | Settings restructuring, account quota P0, authentic scenes, voice polish, automation clarity and Help Center | Approved for future implementation | Not started |
| 06 | Evidence-based format-aware Project Stages and Content Workspace 3.0 | Approved for future implementation (pilot-first) | Not started |
| 07 | Selectable Voice/Alarm intro animation collection across Creator, Personal & 21-Day, selected companion, notification icons | Approved for future implementation | Not started |
| 08 | Personal Care, My Spaces and professional Personal Home | Researched; proposed for founder review | Not started |

**Next entries:** Add #09 and subsequent items in the order the founder provides them. Keep the same distinction between PROPOSED, APPROVED, IMPLEMENTED, TESTED and RELEASED. Never assume a documented item has shipped.
