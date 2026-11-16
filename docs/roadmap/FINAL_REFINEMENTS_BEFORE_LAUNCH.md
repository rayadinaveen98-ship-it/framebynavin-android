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
- **Founder request (2026-11-16):** **PROPOSED; UNDER REVIEW**, not yet approved as final specification. Founder asked to understand and improve the journey before implementation. No app code changes authorized by this item.
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

**Status:** Proposed only. Record captures founder intent and suggested improvements, not an implemented or approved final flow.

---

## Refinement queue

| No. | Refinement | Founder decision | Implementation status |
|---|---|---|---|
| 01 | Four selectable welcome animations; The Living Frame storyboard as first visual implementation reference | Approved for future implementation | Not started |
| 02 | Account choice, identity/profile, companion-guided adaptive first run and six-screen tour | Proposed, awaiting founder review | Not started |

**Next entries:** Add #02, #03, etc. in the order the founder provides them. Keep the same distinction between PROPOSED, APPROVED, IMPLEMENTED, TESTED and RELEASED. Never assume a documented item has shipped.
