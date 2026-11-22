# Backlot UX12 D5 — Idea Vault writing surface and living voice orb

**Date:** 2026-11-22  
**Classification:** SOURCE STAGED; NOT COMPILED; NOT TESTED; NOT MERGED; NOT RELEASED.

## Authority and continuity

- Product direction: **Simple Cinematic Companion System**, combining Film Editorial structure, Living Spatial motion and Personal Storybook warmth.
- Repo: `rayadinaveen98-ship-it/framebynavin-android`
- Stable `main` at source audit: `abbe88984bac16cfafbd990d9cb00ed54cfdd07e`; untouched by D5.
- Prior source baseline UX12 D4: `27968789ca04a01fc6e0763f86ffbbbe2f211713`.
- D5 branch: `feature/backlot-ux12-d5-idea-vault-orb-20261122`, stacked on D4.
- Per founder instruction, NO per-stage tests, lint, APK assembly, GitHub Actions runs or merges; accumulate all UX12 stages, then perform complete QA only when the founder explicitly says **“Done bro, start next steps and give APK.”**

## Source audit

New Idea used `V09NewIdeaCleanEditor`, a bordered raised `Surface` containing an already outlined `OutlinedTextField`, producing two large nested rectangles. A separate explanatory sentence and long editorial header crowded the page before the useful Voice capture controls, optional Notes, Reminder and Organize sections. Those sections already contain significant real feature behavior and must not be removed.

The dictation component `V117VoiceIdeaInput` places `FC13ReactiveDotField` behind an explicit user tap and microphone permission request. Its animation phase advances at rest, but `reaction = intensity * traveling * attenuation` is zero when `active = false`, meaning all dot positions remain stationary. This is a presentation bug, not a microphone or audio-storage defect.

## Implemented source changes

### V09IdeaVaultUi.kt

- Replaced the new-idea editor's nested `OutlinedTextField` with a Compose `BasicTextField` within ONE simple writing surface. The only visible outline belongs to the outer pad. No fake overlays/cinematic poster renders.
- Kept the exact saveable draft `title`, prompt choices and `writingPrompt.placeholder`, related-title detection, and original `v2203-new-idea-title` test tag.
- Improved writing text size and line spacing, with normal keyboard focus, caret, selection and multiline editing from Compose.
- Simplified the headline to **New idea**. Added a small approved Spark/Mira thoughtful pose (`CinePulseState.THINK`) in the header rather than another large illustration or floating animated panel.
- Preserved separately selectable **RECORD** (stores original audio) and **DICTATE** (speech-to-text; no original audio saved). Existing toggle callbacks, permissions, saved voice audio, cancel/discard semantics and transcription-to-title callback are unchanged.
- Kept all optional Notes, Reminder, Organize, platform, format, status and potential controls, plus the anchored Save button and `CreatorIdea` persistence callback.

### V117VoiceQuickIdeaUi.kt

- Added `UX12D5DotMotionPolicy` with independently calculated idle expansion and RMS-driven microphone reaction.
- The dot lattice has a slow native periodic outward breathing component plus tiny positional wander whenever animations and device power settings permit; **its idle motion no longer depends on active microphone input**.
- Real microphone RMS continues to add extra displacement, radius and brightness only during actual listening. The mic is activated or stopped only by explicit tap and existing `IdeaVoiceTranscriber` methods; no automatic listening, permission or audio recording was added.
- Reduced motion and low-power devices remain static. No flashing, rotating blur-heavy shaders, new dependencies, graphics/video assets or custom signal analysis.
- Changed idle caption to **Microphone off** so the natural motion does not falsely imply active listening; the dot field retains its Start/Stop voice listening accessibility semantics.
- Increased previously very small supporting labels and removed the nested bordered partial-transcript box while preserving interim recognized words and error messaging.
- The `FC13ReactiveDotField` API is unchanged, including callers in other Backlot conversation experiences.

### Deferred source-only policy test

File: `app/src/test/java/com/framebynavin/app/ui/UX12D5DotMotionPolicyTest.kt`. This test was **added, not executed**. It checks idle movement with zero RMS, static behavior when motion is off, and bounds on real speech reaction. Do not count it as a green test.

## Functional safety invariants

- Save writes through existing `onSave(CreatorIdea)` handler; no new idea database, cloud or auto-saving.
- User exact typed words remain persisted; writing-prompt chip changes only the prompt text and helper.
- No destructive edit/dismiss behavior changes. Original recording cleanup happens only via existing explicit dismiss/save controls.
- The microphone is OFF until intentional tap and Android permission approval. Ambient motion is strictly visual and does not indicate listening, and tapping still toggles listening.
- Dictation is not confused with **RECORD** audio capture; the latter retains its original audio file.
- No changes to existing-idea edit, voice playback, conversion to projects, Recent Deleted, search, filters or Idea Vault status/organization engine.
- D5 does not affect personal reminders, alarm delivery, snooze, challenge completion, account/auth, sync or data migration.
- The recurring Personal Reminder P0 reliability issue is still an independent release gate.

## Cumulative final QA acceptance when founder authorizes

1. Open New Idea: one writing border only, placeholder visible when empty, caret/focus/selection work, multi-line text and keyboard behave correctly on phone/tablet.
2. Choose each prompt: `IDEA`, `HOOK`, `SCENE`, `TITLE`, `QUESTION`; helper/placeholder change without losing previously typed text.
3. Enter large/long text: scroll correctly, anchored Save button not covered by IME or Android navigation.
4. Duplicate-title advisory remains read-only and saving still requires existing explicit Save action.
5. Record voice: original audio stored and cleared/discarded through original callbacks; Dictate still converts speech to text, not into a saved audio file.
6. Open dictation with mic off: dots noticeably but gently breathe; idle says **Microphone off**, does not trigger permission/mic/capture.
7. Explicit tap: permission is requested only if needed; listening starts/stops; detected language, partial text, transcript and error states remain accurate.
8. Real speech RMS modulates dots more than idle motion. Stop listening returns to idle without stale RMS.
9. Animation disabled / Battery Saver / low-RAM: static field and working tap action. No false mic-active visuals or performance stutter.
10. Verify companion is original Spark or Mira approved art in a small thoughtful position with no cropped faces or obstructed close button.
11. Navigate Notes, Reminder, Organize, topic/category/potential/status/platform/format and edit-existing Idea flow. All original data and actions preserved.
12. Inspect TalkBack labels, minimum touch targets, 1.5×/2× text, compact phones, landscape and owner's Redmi Pad 2.
13. Validate after all UX12 stages with Gradle compile, unit/instrumentation tests, lint and Lab APK provenance check. Physical-device approval and release remain separately gated.

## Evidence classification

**Source staged** means the expected code and plan were written to a stacked GitHub branch. It is NOT evidence of successful compilation, unit/connected tests, responsive layouts or real-world microphone interaction. Source-only review is permitted now; no per-stage QA run, PR-triggered CI or APK.
