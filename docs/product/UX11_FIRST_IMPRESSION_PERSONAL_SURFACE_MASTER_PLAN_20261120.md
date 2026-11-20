# Backlot UX11 — First Impression and Personal Experience
Date: 2026-11-20
Status: UX11-A source-staged. UX11-B..F planned only, not implemented. NO final integration/test/Android APK/release authorized until founder says ALL DONE.

## Founder requirement and screenshot evidence

The owner supplied two current Android screenshots:
- Home: BACKLOT eyebrow, “Good Morning, Creator,” a full-body companion appearing detached and too low on the right, empty project card, Reminder Hub, My Spaces, Weekly Focus and bottom navigation.
- New Personal Reminder: “Make it yours” followed by numbered Purpose/Schedule/Alert/Review step pills, generic dark surfaces and a large bottom Continue/Cancel footer. The owner wants a warmer personally owned experience, and no numbers in its step navigation.

The six explicitly approved changes:
1. Replace particle-dominant welcome with richer premium cinematic backgrounds.
2. Header companion must be properly aligned at the top-right with a **face-only** crop/asset rather than the full-body figure.
3. Restore the **old setup**, not invent a brand-new setup.
4. Premium sign-in/sign-up consistent with previously discussed direction, without changing credential semantics.
5. Make Personal Reminder Home feel like a distinct private personal sanctuary rather than a generic utility.
6. Remove the numeric 1/2/3/4 display from New Personal Reminder while preserving the existing real four-step form logic.

## Current source mapping (verified read-only on source stack)
- UX11-A: `ui/V401WelcomeCollection.kt`, `ui/V199ParticleWelcome.kt`, `ui/V401WelcomePicker.kt`, `ui/V131LaunchGate.kt`, `ui/theme/FrameByNavinTheme.kt`, `ui/V127AppearanceSettings.kt`. The existing four selectable welcome style IDs and 3,000ms classic timeline remain valid; the launch gate bypasses branding for widget/external launch.
- UX11-B: `ui/V18TodayScreen.kt` calls `V18CinematicHomeHero(..., companionAvatar)`. The live home composes `V2214HomeCompanion` via `ui/FrameByNavinV101BApp.kt`; companion behavior/selected assets are in `ui/V2214HomeCompanion.kt` and related guide art. The header slot should be redesigned, NOT a floating full-body overlay.
- UX11-C: existing real first-run routing lives in `ui/FrameByNavinV101BApp.kt` and `ui/V393FirstRunQuickStart.kt`. New Creator/Both journeys use `V393QuickCreatorSetup`, while the older five-page `V18CreatorOnboarding` is **still referenced** for existing partially set-up users. Historical pre-UX02 source in PR #394 parent `f63e6a1f17101088b859f64d788d7eda95039a81` also references this exact older five-page setup. The rollback must preserve complete accounts, Personal-first bypass, permissions and storage; pin the exact old presentation/copy before altering routing. Do not infer that unrelated old Drive recovery screens should return.
- UX11-D: `ui/V23AccountOnboarding.kt` uses Credential Manager / Google ID token and the existing cloud account policy. App routing remains in `FrameByNavinV101BApp.kt`. Existing sign-in, returning-account, username-claim and continue-locally paths must remain correct. The *exact earlier approved auth visual specification* was not located in the reviewed source, so UX11-D needs comparison of existing auth UI with historical design/owner-approved requirements before editing. Never invent a password sign-up backend or claim cloud backup/sync that is not implemented.
- UX11-E: `ui/V2232PersonalReminderHub.kt` is the active Personal reminder world with PersonalReminderStore and Phase1HabitStore, its private My Spaces and Groups links, and UX07B rhythm/UX07E progress sections. Redesign only layout/presentation/navigation shells while reusing real loaded data and owner-created actions, no new persistence, wellness tracking or private data leakage.
- UX11-F: in `ui/V2232PersonalReminderHub.kt`, `V2232PersonalEditor` renders `V406PersonalComposerProgress` and uses `composerStep`, `V296PersonalStepActions` and the existing draft `PersonalReminderDraft.checked()` before save. Replace numbered step visual, not the underlying 4-step validation, notes, schedule, alert mode, saved reminder action or footer.

## Approved sequential source-stage plan
A. **Premium welcome collection (now source-staged).** Three original Compose Canvas directions: Cinematic Ember Reveal (recommended/default for installs without an explicitly stored welcome preference), Aperture Light Bloom and Velvet Glass. Keep all four previously selected styles selectable and stored IDs unchanged. New premium timeline 2.4s, old classic 3s. Previews are silent and never persist. The launch gate still bypasses for external launches and keeps original sound handling.
B. **Companion header correction.** Rebuild header as one aligned Row: greeting left, consistently sized 44–48dp top-right face-only avatar right, Android insets respected and large-font resilient. Crop/render approved original Spark/Mira assets without facial distortion or changing their whole-body appearances elsewhere. Preserve existing avatar click menu/navigation, notifications and assistant behavior; full characters remain available in dedicated companion views.
C. **Old setup restoration.** Diff original five-page setup from the verified historic pre-UX02 source against current new QuickCreator flow; replace only the new setup presentation/branch for Creator/Both as approved. Keep established Creator/Personal/Both routing semantics, existing account/session, previously completed setup and permissions; no second mandatory setup gate.
D. **Sign-in/sign-up polish.** Keep Google Credential Manager, local continuation, returning account and username claim transaction, all true error/loading/offline states and no fake backup promises. Source-stage only UI design after matching prior owner-approved auth brief; no backend/auth provider rewrite.
E. **Personal Sanctuary Home.** Warm restrained red/rose/amber surfaces, a real next-action hero, Today/Upcoming/Needs Attention, meaningful links to My Spaces and the single-activity Challenge, clear create action, private empty/loading/error states, no misleading made-up streak or progress data. One navigation ownership, no duplicate summary cards.
F. **Personal creation polish.** Remove the `1 Purpose` etc numbered chips, switch to legible text-only progression with current-step emphasis and subtle progress line; preserve complete four-step state, validation, optional notes/suggestions, back/continue/save behaviors and Android safe insets/IME handling. Confirm the owner screenshot's footer on 320dp phones.

## Non-negotiable design and engineering acceptance
- Premium distinctive art continuity: Backlot charcoal/red studio signature; Personal has warmer, intimate atmosphere but the same tokens/spacing/accessibility.
- No distorted or improperly cropped companion faces; do not replace Spark/Mira character art across unrelated surfaces.
- All new UI must respect keyboard/IME, system safe insets, 320dp phone, tablet/landscape, 200% text and 48dp touch targets.
- Preserve real sign-in/Google account behavior, local account choice, routing after returning auth and existing profile data.
- Preserve reminder scheduling, Voice/Alarm/21-Day owners, Done/Snooze, Personal/Creator privacy and all UX09 separation.
- Draft PR per source stage, stacked on predecessor and never merged into approved main/stable until later explicit authorisation.
- Once the founder says ALL DONE: reconcile UX11 A..F together with RX01 A..F, resolve conflicts; run unit, regression, lint, Android emulator and physical QA; supply an exact-SHA QA APK for multi-day owner testing. No Play release without separate authorisation.

## UX11-A exact technical scope
1. `ui/UX11PremiumWelcomeExperience.kt`: original three native draw-only backgrounds with typography, deterministic 2.4s timeline, zero new dependency, preview no sound, existing optional launch sound, animator disabled static presentation.
2. `ui/theme/FrameByNavinTheme.kt`: add three stable IDs; only the **null/missing** stored welcome selection defaults to new Cinematic Ember; all previously explicitly selected four styles remain as saved.
3. `ui/V401WelcomeCollection.kt`: dispatch three new styles to new renderer. Existing V199 particle and V401 three other opening paths remain unchanged.
4. `ui/V401WelcomePicker.kt`: three premium choices first, four classics preserved with the same Preview/Choose behavior and silent demo.
5. This source-mapping and acceptance document.

**No RX01 or live reminder code is modified by UX11-A.** Stage is source-only. No screenshot, compilation, instrumentation, physical device test or final APK has been certified.

## UX11-A deferred QA (post ALL DONE)
- On fresh installation no welcome preference -> new Ember default; when old Particle/Living Frame/Prismatic Bloom/Spark First Light explicitly selected, remain selected through upgrade, process restart and rollback.
- All three new styles show a distinct high-contrast brand mark and smooth composed backgrounds, with no overlapping text at 320dp/200% font or tablet.
- New 2.4s timeline finishes exactly once, no stuck splash, app content visible, normal launch sound follows old preference, preview silent.
- External deep links/widgets skip welcome; no reminder or notification deep link delay.
- Reduced motion shows static/legible brand with no flashing.
- Real phone 60Hz/low-power screenshot/video review: no layer clipping, severe jank, overdraw or unexpected animation running after departure.
- UI test selectors cover every saved style, choosing and previewing without preference mutation.
