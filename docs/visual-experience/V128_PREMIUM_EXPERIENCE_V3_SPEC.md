# FrameByNavin v128 — Premium Experience V3

Status: LOCKED implementation contract
Base: v127 Visual Experience V2
Branch: `feature/v128-premium-experience-v3`

## Product principle
Less text, less clutter, more context, richer motion, stronger visual identity. v128 must preserve all v126/v127 functionality while upgrading presentation and interaction quality.

## 1. Launch ident
- Cold-launch ident target: ~5.0 seconds; widget/deep-link launches remain instant.
- Replace the static center divider with a living thread that behaves like every other thread.
- Threads use smoother eased travel, subtle curvature, depth/lane variance, stagger and convergence.
- Increase density without visual noise.
- FRAME BY NAVIN wordmark is ~15% larger than v127, brighter, more vibrant and held long enough to read.
- Keep launch sound optional and theme-aware.

## 2. Creator setup copy + layout
- Frame Guide gives one short contextual instruction.
- Screen heading asks the question.
- Remove duplicate explanatory paragraphs unless needed for a decision.
- Page 1: remove primary-mode smart-default paragraph and selected-mode description from main flow. Secondary modes remain optional and compact.
- Page 2: remove platform-format explanation.
- Page 3: remove niche/production explanation; keep only a tiny contextual hint if useful.
- Page 4: shorten goals copy; rename REALISTIC PUBLISHING TARGET -> WEEKLY TARGET; primary focus appears only when needed.
- Page 5: remove duplicate Android-permissions explanation; creator-profile summary becomes denser.
- Long pages must scroll correctly under header/guide and above bottom CTA on compact devices.

## 3. Premium first-run tour
- Replace the plain coach-card feeling with a spotlight experience.
- Non-target content is dimmed/softened; target area remains visually dominant.
- Coach card becomes smaller, more translucent and context-positioned.
- Frame Guide gains context-specific poses/placement rather than repeating setup movement.
- Keep Skip and replay from Settings.
- Never block critical navigation or hide the highlighted control.

## 4. Frame Pulse orb V2
- Retain RMS-driven live audio response.
- Add richer layered glow, fluid inner field, orbiting filaments, subtle bloom and depth.
- Smooth RMS values so motion feels organic instead of jittery.
- States: idle, listening, active speech, finishing/success, error.
- Active theme controls orb palette.
- Original design language; do not clone Siri/Gemini artwork.

## 5. Ivory Studio correction
- Light-theme audit across core app, setup, Today, Ideas, Create, Insights, Control sheet, reminders, settings, dialogs, Quick Capture and widgets.
- Replace dark-only assumptions with semantic visual tokens.
- System bars must use correct light/dark icon contrast.
- Dark editorial contrast cards are allowed only intentionally.

## 6. Aurora Glass theme
- New theme is a visual personality, not only a palette.
- Deep soft gradient background with translucent drifting color fields.
- Frosted/semi-transparent cards, luminous outlines, translucent selected pills and floating glass navigation treatment where practical.
- Palette: violet / electric blue / aqua / coral-pink with high-contrast foreground text.
- Motion character is soft, calm and fluid.
- Existing themes remain stable.

## 7. Google Calendar
- Deferred to v128.1 after v128 visual QA.
- Preferred first implementation: Android Calendar Provider/local calendar integration, not a paid backend.
- User chooses writable calendar; app-owned events track event IDs to prevent duplicates.
- FrameByNavin reminders remain separate from calendar events.

## 8. New Project — Content Type redesign
- Preserve mode-aware recommendation intelligence and mode switching.
- Normal card shows a maximum of 4 recommended content types.
- “Browse all types” opens an organized dedicated picker instead of expanding the complete taxonomy inline.
- Picker supports grouped/browseable presentation and preserves selected value.
- Creative Style remains collapsible.
- No capability regression from v126 full project setup.

## 9. Motion polish
- Preserve v127 completion/achievement feedback.
- Refine easing, settle, glow and haptic timing; avoid noisy particles.
- Stage completion target feel: ~700–900ms, smooth and restrained.
- Achievement feedback may be slightly stronger but remains elegant.

## Regression guards
- New Project must retain Off/Light/Guided/Urgent/Custom and all reminder delivery modes.
- Theme switching must persist and not remove user data/preferences.
- Widget/deep-link launch remains instant.
- Quick Idea voice transcript behavior remains functional.
- Guided tour remains skippable and replayable.
- No feature may be deferred to Edit Project merely to simplify creation.
