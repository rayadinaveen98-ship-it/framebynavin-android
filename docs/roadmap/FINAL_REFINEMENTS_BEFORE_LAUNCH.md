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

## Refinement queue

| No. | Refinement | Founder decision | Implementation status |
|---|---|---|---|
| 01 | Four selectable welcome animations; The Living Frame storyboard as first visual implementation reference | Approved for future implementation | Not started |

**Next entries:** Add #02, #03, etc. in the order the founder provides them. Keep the same distinction between PROPOSED, APPROVED, IMPLEMENTED, TESTED and RELEASED. Never assume a documented item has shipped.
